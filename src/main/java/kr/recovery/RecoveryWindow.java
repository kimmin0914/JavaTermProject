package kr.recovery;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

final class RecoveryWindow extends JFrame {
    static final Color BACK = new Color(13,17,16);
    static final Color PANEL = new Color(20,26,24);
    static final Color INK = new Color(231,237,236);
    static final Color MUTED = new Color(181,191,184);
    static final Color ACCENT = new Color(178,199,184);
    static final Color WARN = new Color(236,171,112);
    static final long CASE_ID = 1;

    OracleRepository repository;
    Scenario scenario;
    Game game;
    private PlayPanel play;
    private AdminPanel admin;
    private boolean busy;
    private final JPanel busyLayer = new JPanel(new GridBagLayout()) {
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(12,18,22,180));
            g.fillRect(0,0,getWidth(),getHeight());
        }
    };

    RecoveryWindow() {
        super("봉쇄구역 회수반");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(975,650));
        setSize(1210,780);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { requestClose(); }
        });
        busyLayer.setOpaque(false);
        busyLayer.setFocusable(true);
        busyLayer.setFocusTraversalKeysEnabled(false);
        busyLayer.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent event) { event.consume(); }
        });
        busyLayer.addMouseListener(new java.awt.event.MouseAdapter() {});
        busyLayer.add(label("Oracle 데이터베이스 처리 중…",19,INK));
        setGlassPane(busyLayer);
        login();
    }

    private void login() {
        JPanel shell = colorPanel(new GridBagLayout(), BACK);
        JPanel form = vertical(PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(49,69,78)),
                BorderFactory.createEmptyBorder(32,38,34,38)));
        JLabel eyebrow = label("사건 기록  /  R–01",15,ACCENT);
        form.add(eyebrow);
        form.add(Box.createVerticalStrut(9));
        form.add(label("봉쇄구역 회수반",37,INK));
        form.add(Box.createVerticalStrut(8));
        form.add(label("기록을 되찾고, 닫힌 문의 진실을 확인하세요.",14,MUTED));
        form.add(Box.createVerticalStrut(25));
        JTextField url = new JTextField("jdbc:oracle:thin:@localhost:1521/xe",30);
        JTextField user = new JTextField("C##DBSOFT",30);
        JPasswordField password = new JPasswordField(30);
        field(form,"Oracle JDBC 주소",url);
        field(form,"사용자 이름",user);
        field(form,"비밀번호",password);
        JButton connect = button("Oracle 연결", true);
        form.add(connect);
        connect.addActionListener(event -> {
            if (busy) return;
            char[] secret = password.getPassword();
            password.setText("");
            String address = url.getText().strip();
            String account = user.getText().strip();
            if (secret.length == 0 || account.isBlank() || !address.startsWith("jdbc:oracle:thin:@")) {
                Arrays.fill(secret,'\0');
                message("Oracle 접속 주소, 사용자 이름과 비밀번호를 확인하세요.");
                return;
            }
            work(() -> {
                OracleRepository connected = null;
                try {
                    DriverManager.setLoginTimeout(10);
                    connected = new OracleRepository(address, account, secret);
                    return new ConnectionInfo(connected, connected.identity(), connected.load(CASE_ID));
                } catch (Exception error) {
                    if (connected != null) try { connected.close(); } catch (SQLException close) { error.addSuppressed(close); }
                    throw error;
                } finally {
                    Arrays.fill(secret,'\0');
                }
            }, this::home);
        });
        shell.add(form);
        setContentPane(shell);
        getRootPane().setDefaultButton(connect);
        password.requestFocusInWindow();
    }

    private record ConnectionInfo(OracleRepository database, String identity, Scenario scenario) {}

    private void home(ConnectionInfo info) {
        repository = info.database();
        scenario = info.scenario();
        getRootPane().setDefaultButton(null);
        JPanel base = colorPanel(new BorderLayout(), BACK);
        JPanel top = colorPanel(new BorderLayout(12,0), PANEL);
        top.setBorder(BorderFactory.createEmptyBorder(7,20,7,20));
        top.add(label("봉쇄구역 회수반   /   사건 기록",18,INK),BorderLayout.WEST);
        top.add(label("Oracle 연결됨  ·  "+info.identity(),13,ACCENT),BorderLayout.EAST);
        base.add(top,BorderLayout.NORTH);
        CardLayout screens = new CardLayout();
        JPanel cards = colorPanel(screens,BACK);
        play = new PlayPanel(this);
        admin = new AdminPanel(this);
        HistoryPanel history = new HistoryPanel(this);
        cards.add(play,"play");
        cards.add(history,"history");
        cards.add(admin,"manage");
        JPanel navigation = SceneChrome.clear(new FlowLayout(FlowLayout.CENTER,6,0));
        ButtonGroup buttons = new ButtonGroup();
        nav(navigation,buttons,"임무 플레이",true,() -> screens.show(cards,"play"));
        nav(navigation,buttons,"회차 기록",false,() -> {
            screens.show(cards,"history");
            history.refresh();
        });
        nav(navigation,buttons,"조사 장면 관리",false,() -> {
            screens.show(cards,"manage");
            admin.refresh();
        });
        top.add(navigation,BorderLayout.CENTER);
        base.add(cards,BorderLayout.CENTER);
        setContentPane(base);
        revalidate();
        repaint();
    }

    private static void nav(JPanel navigation,ButtonGroup group,String name,boolean selected,Runnable activate) {
        JToggleButton item = new JToggleButton(name);
        group.add(item);
        item.setSelected(selected);
        styleButton(item,selected);
        item.addItemListener(event -> styleButton(item,item.isSelected()));
        item.addActionListener(event -> activate.run());
        navigation.add(item);
    }

    void startGame() {
        work(() -> repository.start(scenario.caseId()), run -> {
            game = new Game(scenario,run);
            play.start();
        });
    }

    void finishGame(Scenario.Ending ending, boolean quit) {
        Game current = game;
        if (current == null) return;
        work(() -> {
            repository.finish(current,ending);
            return current.run;
        }, run -> {
            game = null;
            if (quit) shutdown();
            else play.completed(run,ending,current.area());
        });
    }

    private void requestClose() {
        if (busy) return;
        if (game == null) shutdown();
        else if (JOptionPane.showConfirmDialog(this, "임무를 중단하고 실패 이력을 저장할까요?",
                "프로그램 종료", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            finishGame(null,true);
        }
    }

    private void shutdown() {
        if (repository == null) dispose();
        else work(() -> { repository.close(); return null; }, result -> dispose());
    }

    <T> void work(Callable<T> task, Consumer<T> success) {
        if (busy) return;
        busy = true;
        busyLayer.setVisible(true);
        busyLayer.requestFocusInWindow();
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy = false;
                busyLayer.setVisible(false);
                try { success.accept(get()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { showFailure(e.getCause()); }
            }
        }.execute();
    }

    private void showFailure(Throwable cause) {
        if (cause instanceof SQLException sql) {
            String explanation = switch (sql.getErrorCode()) {
                case 1 -> "같은 번호 또는 표시 순서가 등록되어 있습니다. 목록을 다시 불러오세요.";
                case 942 -> "프로젝트 테이블을 찾을 수 없습니다. SQL Developer 접속과 테이블 생성을 확인하세요.";
                case 1017 -> "Oracle 사용자 이름 또는 비밀번호가 일치하지 않습니다.";
                case 2292 -> "다른 데이터가 이 장면을 참조하고 있어 삭제할 수 없습니다.";
                case 12514 -> "Oracle 서비스 이름을 확인하세요. 수업 환경에서는 xe를 사용합니다.";
                case 17002 -> "Oracle 주소나 리스너가 실행 중인지 확인하세요.";
                default -> sql.getErrorCode() == 0 ? sql.getMessage() : "Oracle 오류 번호 "+sql.getErrorCode()+"\n"+sql.getMessage();
            };
            message(explanation);
        } else message(cause.getMessage() == null ? "처리 중 오류가 발생했습니다." : cause.getMessage());
    }

    void message(String text) {
        JOptionPane.showMessageDialog(this,text,"봉쇄구역 회수반",JOptionPane.INFORMATION_MESSAGE);
    }

    static JPanel colorPanel(LayoutManager layout, Color color) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(color);
        return panel;
    }

    static JPanel vertical(Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBackground(color);
        return panel;
    }

    static JLabel label(String value, int size, Color color) {
        JLabel label = new JLabel(value);
        label.setForeground(color);
        label.setFont(label.getFont().deriveFont(size >= 18 ? Font.BOLD : Font.PLAIN,(float)size));
        return label;
    }

    static JButton button(String name, boolean primary) {
        JButton button = new JButton(name);
        styleButton(button,primary);
        return button;
    }

    static void styleButton(AbstractButton button, boolean primary) {
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(primary ? new Color(42,55,49) : new Color(24,32,29));
        button.setForeground(INK);
        button.setFont(button.getFont().deriveFont(Font.BOLD,14f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? new Color(111,136,122) : new Color(59,75,67)),
                BorderFactory.createEmptyBorder(8,12,8,12)));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    static void field(JPanel panel, String name, JComponent input) {
        JLabel label = label(name,13,MUTED);
        label.setLabelFor(input);
        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE,input.getPreferredSize().height));
        panel.add(input);
        panel.add(Box.createVerticalStrut(10));
    }

    static JTextArea prose(boolean editable) {
        JTextArea text = new JTextArea();
        text.setEditable(editable);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(text.getFont().deriveFont(16f));
        text.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        if (!editable) {
            text.setBackground(PANEL);
            text.setForeground(INK);
            text.setCaretColor(INK);
        }
        return text;
    }
}

