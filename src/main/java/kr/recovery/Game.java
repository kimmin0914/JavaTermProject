package kr.recovery;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class Game {
    record Solved(long puzzleId, int attempts, LocalDateTime at) {}

    final Scenario scenario;
    final long run;
    private long area;
    private final Set<Long> inspected = new LinkedHashSet<>();
    private final Map<Long, Integer> attempts = new LinkedHashMap<>();
    private final Map<Long, Solved> solved = new LinkedHashMap<>();
    private final Map<Long, String> held = new LinkedHashMap<>();
    private final Set<Long> recovered = new LinkedHashSet<>();

    Game(Scenario scenario, long run) {
        this.scenario = scenario;
        this.run = run;
        this.area = scenario.startArea().id();
    }

    Scenario.Area area() { return scenario.area(area); }
    Set<Long> inspected() { return Set.copyOf(inspected); }
    List<Solved> solved() { return List.copyOf(solved.values()); }
    Map<Long, String> held() { return Map.copyOf(held); }
    Set<Long> recovered() { return Set.copyOf(recovered); }
    boolean unlocked(Long id) { return id == null || solved.containsKey(id); }
    boolean solved(long id) { return solved.containsKey(id); }
    boolean power() {
        return scenario.puzzles().stream().anyMatch(p -> p.type().equals("POWER") && solved(p.id()));
    }

    int freeHands() {
        return 2 - scenario.evidence().stream()
                .filter(e -> "ORIGINAL".equals(held.get(e.id())))
                .mapToInt(e -> e.originalHands()).sum();
    }

    String inspect(Scenario.Scene scene) {
        requireHere(scene.areaId());
        inspected.add(scene.id());
        return scene.content();
    }

    String passageBlock(Scenario.Passage passage) {
        if (!unlocked(passage.unlock())) return "잠금 해제에 필요한 퍼즐이 남아 있습니다.";
        if (passage.power() && !power()) return "전력이 필요합니다.";
        if (freeHands() < passage.hands()) return "빈 손 " + passage.hands() + "개가 필요합니다.";
        return "";
    }

    void move(Scenario.Passage passage) {
        requireHere(passage.from());
        require(passageBlock(passage).isEmpty(), passageBlock(passage));
        area = passage.to();
    }

    String puzzleBlock(Scenario.Puzzle puzzle) {
        if (!unlocked(puzzle.prerequisite())) return "선행 퍼즐을 먼저 해결하세요.";
        long missing = scenario.clues().stream().filter(clue -> clue.puzzle()==puzzle.id())
                .filter(clue -> !inspected.contains(clue.scene())).count();
        if (missing>0) return "아직 조사하지 않은 연결 단서가 "+missing+"개 있다. 화면 위의 실마리를 살펴보자.";
        if (freeHands() < puzzle.hands()) return "빈 손 " + puzzle.hands() + "개가 필요합니다.";
        return "";
    }

    boolean submit(Scenario.Puzzle puzzle, String input, LocalDateTime now) {
        requireHere(puzzle.areaId());
        require(!solved(puzzle.id()), "이미 해결했습니다.");
        require(puzzleBlock(puzzle).isEmpty(), puzzleBlock(puzzle));
        String answer = input.strip().toUpperCase(Locale.ROOT);
        require(answer.matches("[A-Z0-9]{1,50}"), "영문 또는 숫자를 입력하세요.");
        int count = attempts.getOrDefault(puzzle.id(), 0) + 1;
        require(count <= 9999, "제출 횟수를 모두 사용했습니다.");
        attempts.put(puzzle.id(), count);
        if (!puzzle.answer().equals(answer)) return false;
        solved.put(puzzle.id(), new Solved(puzzle.id(), count, now));
        return true;
    }

    void recover(Scenario.Evidence evidence, String form) {
        requireHere(evidence.areaId());
        require(unlocked(evidence.unlock()), "관련 퍼즐을 먼저 해결하세요.");
        if (form.equals("ORIGINAL")) {
            int alreadyHeld = "ORIGINAL".equals(held.get(evidence.id())) ? evidence.originalHands() : 0;
            require(evidence.originalHands() != null, "이 증거는 원본을 가져갈 수 없습니다.");
            require(freeHands() + alreadyHeld >= evidence.originalHands(), "원본을 들기 위한 빈 손이 부족합니다.");
        } else {
            require(form.equals("COPY") && evidence.copyAllowed(), "이 증거는 복사할 수 없습니다.");
        }
        held.put(evidence.id(), form);
        recovered.add(evidence.id());
    }

    void putBack(Scenario.Evidence evidence) {
        requireHere(evidence.areaId());
        held.remove(evidence.id());
    }

    Scenario.Ending escapeEnding() {
        require(area().type().equals("EXIT"), "탈출 구역에서만 종료할 수 있습니다.");
        require(solved.size() == scenario.puzzles().size(), "모든 사건 퍼즐을 해결해야 합니다.");
        return scenario.endings().stream().filter(ending -> {
            List<Scenario.Requirement> needed = scenario.requirements().stream()
                    .filter(r -> r.ending() == ending.id()).toList();
            return !needed.isEmpty() && needed.stream().allMatch(r -> {
                String carried = held.get(r.evidence());
                return carried != null && (r.form().equals("ANY") || carried.equals(r.form()));
            });
        }).max(Comparator.comparingInt(Scenario.Ending::rank)
                .thenComparing(Comparator.comparingLong(Scenario.Ending::id).reversed()))
                .orElseThrow(() -> new IllegalStateException("결말에 필요한 증거를 아직 확보하지 못했습니다."));
    }

    private void requireHere(long id) {
        require(area == id, "현재 구역에서만 할 수 있습니다.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

