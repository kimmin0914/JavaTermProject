package kr.recovery;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import static kr.recovery.RecoveryWindow.*;

final class AdminPanel extends JPanel {
    private static final String[] TYPES = {"CLUE","DIALOGUE","STORY"};
    private final RecoveryWindow host;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"번호","종류","제목","표시 순서"},0) {
        @Override public boolean isCellEditable(int row,int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> type = new JComboBox<>(TYPES);
    private final JTextField title = new JTextField(24);
    private final JTextArea content = prose(true);
    private final JLabel status = label("시연 공간을 준비하면 조사 장면을 등록할 수 있습니다.",14,MUTED);
    private List<Scenario.Scene> scenes = List.of();

    AdminPanel(RecoveryWindow host) {
        super(new BorderLayout(16,12));
        this.host = host;
        setBackground(BACK);
        setBorder(BorderFactory.createEmptyBorder(17,20,17,20));
        JPanel heading = vertical(BACK);
        heading.add(label("조사 장면 관리",25,INK));
        heading.add(Box.createVerticalStrut(7));
        heading.add(label("시연용 사건의 조사 장면을 등록·조회·수정·삭제합니다.",14,MUTED));
        add(heading,BorderLayout.NORTH);

        JPanel list = colorPanel(new BorderLayout(0,8),PANEL);
        list.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel controls = colorPanel(new FlowLayout(FlowLayout.LEFT,6,0),PANEL);
        JButton prepare = button("시연 공간 준비",false);
        prepare.addActionListener(e -> host.work(() -> {
            host.repository.prepareDemo();
            return host.repository.demoScenes();
        }, rows -> display(rows,"시연 공간을 준비했습니다.")));
        controls.add(prepare);
        JButton reload = button("조회",false);
        reload.addActionListener(e -> refresh());
        controls.add(reload);
        list.add(controls,BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(29);
        table.getColumnModel().getColumn(0).setPreferredWidth(48);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(245);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || table.getSelectedRow() < 0) return;
            Scenario.Scene item = selected();
            type.setSelectedItem(item.type());
            title.setText(item.title());
            content.setText(item.content());
            content.setCaretPosition(0);
        });
        list.add(new JScrollPane(table),BorderLayout.CENTER);
        add(list,BorderLayout.CENTER);

        JPanel editor = vertical(PANEL);
        editor.setPreferredSize(new Dimension(405,400));
        editor.setBorder(BorderFactory.createEmptyBorder(12,14,12,14));
        editor.add(label("장면 편집",19,INK));
        editor.add(Box.createVerticalStrut(12));
        type.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,
                                                                    boolean selected,boolean focus) {
                return super.getListCellRendererComponent(list,
                        value == null ? "" : Scenario.sceneType(value.toString()),index,selected,focus);
            }
        });
        field(editor,"종류",type);
        field(editor,"제목",title);
        editor.add(label("내용",13,MUTED));
        editor.add(Box.createVerticalStrut(4));
        JScrollPane contents = new JScrollPane(content);
        contents.setAlignmentX(LEFT_ALIGNMENT);
        contents.setPreferredSize(new Dimension(340,185));
        editor.add(contents);
        editor.add(Box.createVerticalStrut(12));
        JPanel actions = colorPanel(new FlowLayout(FlowLayout.LEFT,5,0),PANEL);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));
        JButton reset = button("새 입력",false);
        reset.addActionListener(e -> clear());
        JButton add = button("등록",true);
        add.addActionListener(e -> save(false));
        JButton update = button("수정",false);
        update.addActionListener(e -> save(true));
        JButton delete = button("삭제",false);
        delete.addActionListener(e -> delete());
        actions.add(reset); actions.add(add); actions.add(update); actions.add(delete);
        editor.add(actions);
        add(editor,BorderLayout.EAST);
        add(status,BorderLayout.SOUTH);
    }

    void refresh() {
        host.work(host.repository::demoScenes, rows ->
                display(rows,"조회 완료 · "+rows.size()+"건  /  최초 등록 전 ‘시연 공간 준비’를 눌러주세요."));
    }

    private void save(boolean editing) {
        Scenario.Scene item = selected();
        if (editing && item == null) { host.message("수정할 장면을 먼저 선택하세요."); return; }
        String enteredTitle = title.getText().strip();
        String enteredContent = content.getText().strip();
        String enteredType = (String)type.getSelectedItem();
        int titleLength = enteredTitle.codePointCount(0,enteredTitle.length());
        int contentLength = enteredContent.codePointCount(0,enteredContent.length());
        if (titleLength < 1 || titleLength > 100 || contentLength < 1 || contentLength > 1000) {
            host.message("제목은 1~100자, 내용은 1~1000자로 입력하세요.");
            return;
        }
        host.work(() -> {
            if (editing) host.repository.editScene(item.id(),enteredType,enteredTitle,enteredContent);
            else host.repository.addScene(enteredType,enteredTitle,enteredContent);
            return host.repository.demoScenes();
        }, rows -> display(rows,(editing ? "수정" : "등록")+" 완료 · "+enteredTitle));
    }

    private void delete() {
        Scenario.Scene item = selected();
        if (item == null) { host.message("삭제할 장면을 먼저 선택하세요."); return; }
        if (JOptionPane.showConfirmDialog(host,"‘"+item.title()+"’ 장면을 삭제할까요?",
                "조사 장면 삭제",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        host.work(() -> {
            host.repository.deleteScene(item.id());
            return host.repository.demoScenes();
        }, rows -> display(rows,"삭제 완료 · "+item.title()));
    }

    private Scenario.Scene selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : scenes.get(table.convertRowIndexToModel(row));
    }

    private void clear() {
        table.clearSelection();
        type.setSelectedIndex(0);
        title.setText("");
        content.setText("");
        title.requestFocusInWindow();
    }

    private void display(List<Scenario.Scene> rows,String result) {
        clear();
        scenes = rows;
        model.setRowCount(0);
        for (Scenario.Scene s : rows) {
            model.addRow(new Object[]{s.id(),Scenario.sceneType(s.type()),s.title(),s.order()});
        }
        status.setText(result);
    }
}

