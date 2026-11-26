package kr.recovery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class OracleRepository implements AutoCloseable {
    private static final long DEMO_CASE = 900000001L;
    private static final long DEMO_AREA = 1;
    private static final String DEMO_TITLE = "자바 CRUD 시연용 사건";
    private final Connection db;
    record Run(long id, LocalDateTime started, LocalDateTime ended, String status, String exit, String ending) {}
    record Detail(List<String> puzzles, List<String> evidence) {}

    OracleRepository(String url, String user, char[] password) throws SQLException {
        db = DriverManager.getConnection(url, user, new String(password));
    }

    String identity() throws SQLException {
        return rows("SELECT USER || ' / ' || SYS_CONTEXT('USERENV','CON_NAME') FROM dual",
                rs -> rs.getString(1)).get(0);
    }

    Scenario load(long id) throws SQLException {
        return ReviewedStory.apply(transaction(() -> loadScenario(id)));
    }

    private Scenario loadScenario(long id) throws SQLException {
        List<String[]> header = rows("SELECT title, briefing FROM CASE_FILE WHERE case_id=?", r ->
                new String[]{r.getString(1), r.getString(2)}, id);
        if (header.isEmpty()) throw new SQLException("사건 1이 없습니다. SQL Developer에서 기본 데이터를 입력하세요.");
        List<Scenario.Area> areas = rows("""
                SELECT area_no,area_name,area_type,map_x,map_y FROM AREA WHERE case_id=? ORDER BY area_no
                """, r -> new Scenario.Area(r.getLong(1),r.getString(2),r.getString(3),r.getInt(4),r.getInt(5)),id);
        if (areas.stream().filter(a -> a.type().equals("START")).count() != 1) {
            throw new SQLException("사건의 시작 구역을 확인하세요.");
        }
        return new Scenario(id, header.get(0)[0], header.get(0)[1], areas, scenes(id),
                rows("""
                     SELECT puzzle_no,area_no,puzzle_name,puzzle_type,prompt,answer_key,hint_text,solved_text,
                            prerequisite_puzzle_no,required_free_hands
                     FROM PUZZLE WHERE case_id=? ORDER BY puzzle_no
                     """, r -> new Scenario.Puzzle(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),
                        r.getString(5),r.getString(6),r.getString(7),r.getString(8),nullableLong(r,9),r.getInt(10)),id),
                rows("""
                     SELECT passage_no,from_area_no,to_area_no,passage_name,min_free_hands,requires_power,unlock_puzzle_no
                     FROM PASSAGE WHERE case_id=? ORDER BY passage_no
                     """, r -> new Scenario.Passage(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),
                        r.getInt(5),"Y".equals(r.getString(6)),nullableLong(r,7)),id),
                rows("""
                     SELECT evidence_no,area_no,evidence_name,evidence_note,original_hands,copy_allowed,unlock_puzzle_no
                     FROM EVIDENCE WHERE case_id=? ORDER BY evidence_no
                     """, r -> {
                    Long hands = nullableLong(r,5);
                    return new Scenario.Evidence(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),
                            hands == null ? null : hands.intValue(),"Y".equals(r.getString(6)),nullableLong(r,7));
                },id),
                rows("""
                     SELECT ending_no,ending_title,ending_text,ending_rank FROM ENDING WHERE case_id=?
                     ORDER BY ending_rank DESC,ending_no
                     """, r -> new Scenario.Ending(r.getLong(1),r.getString(2),r.getString(3),r.getInt(4)),id),
                rows("""
                     SELECT ending_no,evidence_no,required_form FROM ENDING_REQUIREMENT WHERE case_id=?
                     """, r -> new Scenario.Requirement(r.getLong(1),r.getLong(2),r.getString(3)),id),
                rows("""
                     SELECT puzzle_no,inspection_no FROM PUZZLE_CLUE WHERE case_id=?
                     """, r -> new Scenario.Clue(r.getLong(1),r.getLong(2)),id));
    }

    private List<Scenario.Scene> scenes(long id) throws SQLException {
        return rows("""
                SELECT inspection_no,area_no,inspection_type,title,content,display_order
                FROM INSPECTION WHERE case_id=? ORDER BY area_no,display_order,inspection_no
                """, r -> new Scenario.Scene(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),
                r.getString(5),r.getInt(6)),id);
    }

    LocalDateTime now() throws SQLException {
        return rows("SELECT CAST(SYSTIMESTAMP AS TIMESTAMP(0)) FROM dual",
                r -> r.getTimestamp(1).toLocalDateTime()).get(0);
    }

    long start(long caseId) throws SQLException {
        return transaction(() -> {
            lockCase(caseId);
            long id = rows("SELECT NVL(MAX(run_no),0)+1 FROM PLAY_RUN WHERE case_id=?",
                    r -> r.getLong(1),caseId).get(0);
            update("""
                    INSERT INTO PLAY_RUN (case_id,run_no,started_at,run_status)
                    VALUES (?,?,CAST(SYSTIMESTAMP AS TIMESTAMP(0)),'ACTIVE')
                    """,caseId,id);
            return id;
        });
    }

    void finish(Game game, Scenario.Ending ending) throws SQLException {
        transaction(() -> {
            long id = game.scenario.caseId();
            int changed = update("""
                    UPDATE PLAY_RUN SET ended_at=CAST(SYSTIMESTAMP AS TIMESTAMP(0)),run_status=?,exit_area_no=?,ending_no=?
                    WHERE case_id=? AND run_no=? AND run_status='ACTIVE'
                    """,ending == null ? "FAILED" : "ESCAPED",
                    ending == null ? null : game.area().id(),ending == null ? null : ending.id(),id,game.run);
            assertOne(changed,"이미 종료된 회차이거나 회차가 존재하지 않습니다.");
            for (Game.Solved solved : game.solved()) {
                update("""
                        INSERT INTO RUN_PUZZLE (case_id,run_no,puzzle_no,attempt_count,solved_at)
                        VALUES (?,?,?,?,?)
                        """,id,game.run,solved.puzzleId(),solved.attempts(),Timestamp.valueOf(solved.at()));
            }
            if (ending != null) {
                for (Map.Entry<Long,String> recovery : game.held().entrySet()) {
                    update("""
                            INSERT INTO RUN_EVIDENCE (case_id,run_no,evidence_no,recovery_form) VALUES (?,?,?,?)
                            """,id,game.run,recovery.getKey(),recovery.getValue());
                }
            }
            return null;
        });
    }

    List<Run> runs(long id) throws SQLException {
        return rows("""
                SELECT r.run_no,r.started_at,r.ended_at,r.run_status,a.area_name,e.ending_title
                FROM PLAY_RUN r
                LEFT JOIN AREA a ON a.case_id=r.case_id AND a.area_no=r.exit_area_no
                LEFT JOIN ENDING e ON e.case_id=r.case_id AND e.ending_no=r.ending_no
                WHERE r.case_id=? ORDER BY r.run_no DESC
                """,r -> new Run(r.getLong(1),timestamp(r,2),timestamp(r,3),r.getString(4),
                r.getString(5),r.getString(6)),id);
    }

    Detail detail(long caseId, long runId) throws SQLException {
        return new Detail(rows("""
                SELECT p.puzzle_name, rp.attempt_count FROM RUN_PUZZLE rp JOIN PUZZLE p
                  ON p.case_id=rp.case_id AND p.puzzle_no=rp.puzzle_no
                WHERE rp.case_id=? AND rp.run_no=? ORDER BY rp.solved_at,rp.puzzle_no
                """,r -> r.getString(1)+" · "+r.getInt(2)+"회 제출",caseId,runId),
                rows("""
                SELECT e.evidence_name,re.recovery_form FROM RUN_EVIDENCE re JOIN EVIDENCE e
                  ON e.case_id=re.case_id AND e.evidence_no=re.evidence_no
                WHERE re.case_id=? AND re.run_no=? ORDER BY re.evidence_no
                """,r -> r.getString(1)+" · "+(r.getString(2).equals("ORIGINAL") ? "원본" : "복사본"),caseId,runId));
    }

    void prepareDemo() throws SQLException {
        transaction(() -> {
            if (!demoExists()) {
                update("INSERT INTO CASE_FILE (case_id,title,briefing) VALUES (?,?,?)",DEMO_CASE,DEMO_TITLE,
                        "INSPECTION 등록·조회·수정·삭제 연습용으로 분리한 사건입니다.");
                update("INSERT INTO AREA (case_id,area_no,area_name,area_type,map_x,map_y) VALUES (?,?,?,'START',0,0)",
                        DEMO_CASE,DEMO_AREA,"연습 구역");
            }
            lockDemo();
            if (rows("SELECT area_no FROM AREA WHERE case_id=? AND area_type='EXIT'",
                    r -> r.getLong(1),DEMO_CASE).isEmpty()) {
                update("INSERT INTO AREA (case_id,area_no,area_name,area_type,map_x,map_y) VALUES (?,2,?,'EXIT',1,0)",
                        DEMO_CASE,"연습 출구");
            }
            return null;
        });
    }

    List<Scenario.Scene> demoScenes() throws SQLException {
        return demoExists() ? scenes(DEMO_CASE).stream().filter(s -> s.areaId() == DEMO_AREA).toList() : List.of();
    }

    long addScene(String type, String title, String content) throws SQLException {
        return transaction(() -> {
            lockDemo();
            long nextId = rows("SELECT NVL(MAX(inspection_no),0)+1 FROM INSPECTION WHERE case_id=?",
                    r -> r.getLong(1),DEMO_CASE).get(0);
            List<Integer> occupied = rows("SELECT display_order FROM INSPECTION WHERE case_id=? AND area_no=?",
                    r -> r.getInt(1),DEMO_CASE,DEMO_AREA);
            int order = 1;
            while (occupied.contains(order)) order++;
            if (order > 999) throw new SQLException("시연 공간에는 장면을 999개까지 등록할 수 있습니다.");
            update("""
                    INSERT INTO INSPECTION (case_id,inspection_no,area_no,inspection_type,title,content,display_order)
                    VALUES (?,?,?,?,?,?,?)
                    """,DEMO_CASE,nextId,DEMO_AREA,type,title,content,order);
            return nextId;
        });
    }

    void editScene(long id, String type, String title, String content) throws SQLException {
        transaction(() -> {
            lockDemo();
            assertOne(update("""
                    UPDATE INSPECTION SET inspection_type=?,title=?,content=?
                    WHERE case_id=? AND area_no=? AND inspection_no=?
                    """,type,title,content,DEMO_CASE,DEMO_AREA,id),"수정할 장면이 없습니다.");
            return null;
        });
    }

    void deleteScene(long id) throws SQLException {
        transaction(() -> {
            lockDemo();
            assertOne(update("DELETE FROM INSPECTION WHERE case_id=? AND area_no=? AND inspection_no=?",
                    DEMO_CASE,DEMO_AREA,id),"삭제할 장면이 없습니다.");
            return null;
        });
    }

    private boolean demoExists() throws SQLException {
        List<String> titles = rows("SELECT title FROM CASE_FILE WHERE case_id=?",r -> r.getString(1),DEMO_CASE);
        if (titles.isEmpty()) return false;
        if (!DEMO_TITLE.equals(titles.get(0))) throw new SQLException("CRUD 시연용으로 지정한 사건 번호가 이미 사용 중입니다.");
        return true;
    }

    private void lockDemo() throws SQLException {
        lockCase(DEMO_CASE);
        if (!demoExists()) throw new SQLException("먼저 CRUD 시연 공간을 준비하세요.");
    }

    private void lockCase(long id) throws SQLException {
        if (rows("SELECT case_id FROM CASE_FILE WHERE case_id=? FOR UPDATE",r -> r.getLong(1),id).isEmpty()) {
            throw new SQLException("사건 행이 없습니다. 시연 공간을 먼저 준비했는지 확인하세요.");
        }
    }

    private static void assertOne(int n, String message) throws SQLException {
        if (n != 1) throw new SQLException(message);
    }

    private static Long nullableLong(ResultSet r, int column) throws SQLException {
        long n = r.getLong(column);
        return r.wasNull() ? null : n;
    }

    private static LocalDateTime timestamp(ResultSet r, int column) throws SQLException {
        Timestamp timestamp = r.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private <T> T transaction(Operation<T> work) throws SQLException {
        db.setAutoCommit(false);
        boolean transactionEnded = false;
        try {
            T result = work.perform();
            db.commit();
            transactionEnded = true;
            return result;
        } catch (SQLException | RuntimeException ex) {
            try {
                db.rollback();
                transactionEnded = true;
            } catch (SQLException rollbackError) {
                ex.addSuppressed(rollbackError);
                try { db.close(); } catch (SQLException closeError) { ex.addSuppressed(closeError); }
            }
            throw ex;
        } finally {
            if (transactionEnded) db.setAutoCommit(true);
        }
    }

    private PreparedStatement statement(String sql, Object... values) throws SQLException {
        PreparedStatement ps = db.prepareStatement(sql);
        try {
            for (int i=0;i<values.length;i++) {
                if (values[i] == null) ps.setNull(i+1,java.sql.Types.NUMERIC);
                else ps.setObject(i+1,values[i]);
            }
            return ps;
        } catch (SQLException e) {
            try { ps.close(); } catch (SQLException closeError) { e.addSuppressed(closeError); }
            throw e;
        }
    }

    private int update(String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = statement(sql,values)) { return ps.executeUpdate(); }
    }

    private <T> List<T> rows(String sql, Row<T> map, Object... values) throws SQLException {
        try (PreparedStatement ps = statement(sql,values); ResultSet result = ps.executeQuery()) {
            List<T> matches = new ArrayList<>();
            while (result.next()) matches.add(map.read(result));
            return List.copyOf(matches);
        }
    }

    @Override public void close() throws SQLException { db.close(); }
    @FunctionalInterface private interface Row<T> { T read(ResultSet rs) throws SQLException; }
    @FunctionalInterface private interface Operation<T> { T perform() throws SQLException; }
}

