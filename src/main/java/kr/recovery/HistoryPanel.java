package kr.recovery;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static kr.recovery.RecoveryWindow.*;

final class HistoryPanel extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RecoveryWindow host;
    private final JTable table;
    private final JTextArea details = prose(false);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"회차","시작","종료","상태","탈출 구역","결말"},0) {
        @Override public boolean isCellEditable(int row,int column) { return false; }
    };
    private List<OracleRepository.Run> runs = List.of();

    HistoryPanel(RecoveryWindow host) {
        super(new BorderLayout(0,10));
        this.host = host;
        setBackground(BACK);
        setBorder(BorderFactory.createEmptyBorder(17,20,18,20));
        JPanel header = colorPanel(new BorderLayout(),BACK);
        header.add(label("회차 기록",25,INK),BorderLayout.WEST);
        JPanel buttons = colorPanel(new FlowLayout(FlowLayout.RIGHT,8,0),BACK);
        JButton refresh = button("새로고침",false);
        refresh.addActionListener(e -> refresh());
        JButton open = button("선택 회차 상세",true);
        open.addActionListener(e -> detail());
        buttons.add(refresh); buttons.add(open);
        header.add(buttons,BorderLayout.EAST);
        add(header,BorderLayout.NORTH);
        table = new JTable(model);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {55,156,156,86,122,179};
        for (int i=0;i<widths.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        add(new JScrollPane(table),BorderLayout.CENTER);
        details.setText("회차를 선택하고 ‘선택 회차 상세’를 눌러 해결 퍼즐과 확보 증거를 확인하세요.");
        var transcript = new JScrollPane(details);
        transcript.setPreferredSize(new Dimension(600,155));
        add(transcript,BorderLayout.SOUTH);
    }

    void refresh() {
        host.work(() -> host.repository.runs(CASE_ID), rows -> {
            runs = rows;
            model.setRowCount(0);
            for (OracleRepository.Run run : rows) {
                model.addRow(new Object[]{run.id(),time(run.started()),time(run.ended()),status(run.status()),
                        run.exit(),run.ending()});
            }
            details.setText(rows.isEmpty() ? "저장된 회차가 없습니다."
                    : "회차 "+rows.size()+"건을 조회했습니다. 회차를 선택해 상세 정보를 볼 수 있습니다.");
        });
    }

    private void detail() {
        int row = table.getSelectedRow();
        if (row < 0) { host.message("조회할 회차를 먼저 선택하세요."); return; }
        long run = runs.get(table.convertRowIndexToModel(row)).id();
        host.work(() -> host.repository.detail(CASE_ID,run), detail -> {
            details.setText("회차 "+run+"\n\n해결 퍼즐  "+String.join("   /   ",
                    detail.puzzles().isEmpty() ? List.of("없음") : detail.puzzles())
                    +"\n\n확보 증거  "+String.join("   /   ",
                    detail.evidence().isEmpty() ? List.of("없음") : detail.evidence()));
            details.setCaretPosition(0);
        });
    }

    private static String time(LocalDateTime t) { return t == null ? "" : TIME.format(t); }
    private static String status(String value) {
        return switch (value) {
            case "ESCAPED" -> "탈출";
            case "FAILED" -> "실패";
            case "ACTIVE" -> "진행 중";
            default -> value;
        };
    }
}

