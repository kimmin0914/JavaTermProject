package kr.recovery;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static kr.recovery.RecoveryWindow.*;

final class PlayPanel extends JPanel {
    private final RecoveryWindow host;
    private final JLabel state = label("",13,MUTED);
    private final JTextArea guidance = SceneChrome.transcript();
    private final JLabel noteTitle = label("임무 브리핑",17,ACCENT);
    private final JTextArea note = SceneChrome.transcript();
    private final JPanel interaction = SceneChrome.clear(new FlowLayout(FlowLayout.RIGHT,7,1));
    private final JLabel dialogueCue = label("",13,ACCENT);
    private final JPanel advanceSurface = new JPanel();
    private final JPanel notebookCard = SceneChrome.shade(new BorderLayout(0,12),216);
    private final JPanel notebookMask = new JPanel() {
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(7,12,10,106));
            g.fillRect(0,0,getWidth(),getHeight());
        }
    };
    private final RoomPhoto photo = new RoomPhoto(this::activate);
    private final Timer timer;
    private Instant start;
    private final JButton notebook;
    private final JButton stop;
    private Dialogue currentDialogue;
    private JComponent readingReturn;

    private record Dialogue(String title,List<String> lines,int line,boolean spoken,Runnable after) {}

    PlayPanel(RecoveryWindow host) {
        super(new BorderLayout());
        this.host = host;
        setBackground(BACK);

        JPanel mission = SceneChrome.shade(new GridLayout(2,1,0,4),117);
        mission.setBorder(BorderFactory.createEmptyBorder(7,12,7,12));
        mission.add(state);
        JPanel controls = SceneChrome.clear(new FlowLayout(FlowLayout.LEFT,6,0));
        notebook = SceneChrome.control("조사 수첩",false);
        notebook.addActionListener(e -> notebook());
        stop = SceneChrome.control("임무 중단",false);
        stop.addActionListener(e -> stopMission());
        controls.add(notebook);
        controls.add(stop);
        mission.add(controls);

        JPanel next = SceneChrome.shade(new BorderLayout(0,3),142);
        next.setBorder(BorderFactory.createEmptyBorder(9,13,7,11));
        next.add(label("다음 실마리",13,ACCENT),BorderLayout.NORTH);
        guidance.setFont(guidance.getFont().deriveFont(14f));
        guidance.setBorder(BorderFactory.createEmptyBorder());
        guidance.setFocusable(false);
        next.add(guidance,BorderLayout.CENTER);

        JPanel transcript = SceneChrome.shade(new BorderLayout(0,2),178);
        transcript.setBorder(BorderFactory.createEmptyBorder(12,17,8,15));
        transcript.add(noteTitle,BorderLayout.NORTH);
        JScrollPane reading = SceneChrome.scroll(note);
        note.setBorder(BorderFactory.createEmptyBorder(4,0,1,8));
        transcript.add(reading,BorderLayout.CENTER);
        JPanel footer = SceneChrome.clear(new BorderLayout(8,0));
        footer.add(dialogueCue,BorderLayout.WEST);
        footer.add(interaction,BorderLayout.CENTER);
        transcript.add(footer,BorderLayout.SOUTH);

        JLayeredPane scene = new JLayeredPane() {
            @Override public void doLayout() {
                int w=getWidth(), h=getHeight(), margin=w>=1080 ? 20 : 14;
                int hintWidth=Math.min(520,w/2), hintHeight=83;
                int transcriptWidth=w-2*margin;
                int normalHeight=h>=570 ? 140 : 125;
                int maxHeight=Math.min(h/3,h>=570 ? 184 : 155);
                int textWidth=Math.max(1,transcriptWidth-transcript.getInsets().left-transcript.getInsets().right-10);
                note.setSize(textWidth,Short.MAX_VALUE);
                int needed=note.getPreferredSize().height+noteTitle.getPreferredSize().height
                        +footer.getPreferredSize().height+transcript.getInsets().top+transcript.getInsets().bottom+8;
                int transcriptHeight=Math.max(normalHeight,Math.min(maxHeight,needed));
                photo.setBounds(0,0,w,h);
                mission.setBounds(margin,margin,331,80);
                next.setBounds(w-margin-hintWidth,margin,hintWidth,hintHeight);
                transcript.setBounds(margin,h-margin-transcriptHeight,transcriptWidth,transcriptHeight);
                photo.setClearance(margin+Math.max(80,hintHeight)+12,margin+transcriptHeight+11);
                int drawerWidth=Math.min(720,w-2*margin-24), drawerHeight=Math.min(447,h-2*margin-24);
                notebookMask.setBounds(0,0,w,h);
                notebookCard.setBounds((w-drawerWidth)/2,(h-drawerHeight)/2,drawerWidth,drawerHeight);
            }
        };
        scene.add(photo,JLayeredPane.DEFAULT_LAYER);
        scene.add(mission,JLayeredPane.PALETTE_LAYER);
        scene.add(next,JLayeredPane.PALETTE_LAYER);
        scene.add(transcript,JLayeredPane.PALETTE_LAYER);
        notebookMask.setOpaque(false);
        notebookMask.setVisible(false);
        notebookMask.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) { closeNotebook(); }
        });
        notebookCard.setBorder(BorderFactory.createEmptyBorder(15,19,14,19));
        notebookCard.setVisible(false);
        notebookCard.addMouseListener(new MouseAdapter() {});
        notebookCard.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ESCAPE"),"close");
        notebookCard.getActionMap().put("close",new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { closeNotebook(); }
        });
        scene.add(notebookMask,JLayeredPane.MODAL_LAYER);
        scene.add(notebookCard,JLayeredPane.POPUP_LAYER);

        JLayer<JLayeredPane> layer = new JLayer<>(scene);
        layer.setGlassPane(advanceSurface);
        configureAdvanceSurface();
        add(layer,BorderLayout.CENTER);

        timer = new Timer(1000,event -> updateState());
        briefing("임무 브리핑",host.scenario.briefing());
    }

    private void configureAdvanceSurface() {
        advanceSurface.setOpaque(false);
        advanceSurface.setVisible(false);
        advanceSurface.setFocusable(true);
        advanceSurface.setFocusTraversalKeysEnabled(false);
        advanceSurface.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        advanceSurface.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getButton()==MouseEvent.BUTTON1) advanceDialogue();
            }
        });
        advanceSurface.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"),"next");
        advanceSurface.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"),"next");
        advanceSurface.getActionMap().put("next",new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { advanceDialogue(); }
        });
    }

    void start() {
        start = Instant.now();
        timer.start();
        showNote("현장 도착","빗소리 사이로 인터폰 잡음이 들린다. 난간 옆 벽에서 나는 소리다.");
        render();
        photo.requestFocusInWindow();
    }

    void completed(long run,Scenario.Ending ending,Scenario.Area lastArea) {
        timer.stop();
        notebook.setEnabled(false);
        stop.setEnabled(false);
        state.setText("회차 "+run+(ending==null ? "  /  철수" : "  /  탈출 완료"));
        photo.showRoom(lastArea.id(),lastArea.name(),null,List.of());
        tip("이번 임무가 끝났다. 저장된 결과는 회차 기록에서 볼 수 있다.");
        Runnable newRun=() -> {
            dialogueCue.setText("이번 결과는 회차 기록에 저장됐다.");
            action("새 회차 시작",true,host::startGame);
        };
        if (ending==null) {
            showNote("임무 중단","증거를 모두 챙기기 전에 현장에서 철수했다. 해결한 퍼즐은 이번 회차에 기록됐다.");
            newRun.run();
        } else {
            List<String> beats=ending.text().lines().filter(line -> !line.isBlank()).toList();
            showDialogue(new Dialogue(ending.title(),beats,0,false,newRun));
        }
    }

    private void briefing(String title,String text) {
        photo.showRoom(host.scenario.startArea().id(),host.scenario.title(),null,List.of());
        showNote(title,text);
        state.setText("사건 1  /  대기 중");
        tip("새 회차를 시작하고, 보안실의 CCTV 기록을 찾아보자.");
        notebook.setEnabled(false);
        stop.setEnabled(false);
        action("새 회차 시작",true,host::startGame);
    }

    private void render() {
        Game game = host.game;
        Scenario.Area area = game.area();
        photo.showRoom(area.id(),area.name(),"EXIT".equals(area.type()) ? "탈출 지점" : null,
                RoomTargets.forArea(game.scenario,area.id()),game.inspected(),game.recovered());
        updateState();
        tip(RoomTargets.hint(game));
        notebook.setEnabled(true);
        stop.setEnabled(true);
    }

    private void tip(String text) {
        guidance.setText(text);
        guidance.setCaretPosition(0);
    }

    private void activate(RoomTargets.Spot spot) {
        Game game = host.game;
        if (game==null || currentDialogue!=null) return;
        Scenario scenario = game.scenario;
        locally(() -> {
            switch (spot.kind()) {
                case SCENE -> scenario.scenes().stream().filter(s -> s.id()==spot.id()).findFirst()
                        .ifPresent(scene -> showScene(game,scene));
                case PUZZLE -> scenario.puzzles().stream().filter(p -> p.id()==spot.id()).findFirst()
                        .ifPresent(puzzle -> puzzle(game,puzzle));
                case EVIDENCE -> scenario.evidence().stream().filter(e -> e.id()==spot.id()).findFirst()
                        .ifPresent(evidence -> evidence(game,evidence));
                case PASSAGE -> scenario.passages().stream().filter(p -> p.id()==spot.id()).findFirst()
                        .ifPresent(passage -> move(game,passage));
                case EXIT -> {
                    showNote(spot.object(),"탈출할 수 있는지 확인한다.");
                    Scenario.Ending ending = game.escapeEnding();
                    showNote(spot.object(),"이 길로 건물 밖에 나갈 수 있다. 지금 챙긴 증거를 가지고 탈출할까?");
                    action("현장에 남기",false,() -> showNote(game.area().name(),"아직 남은 기록을 살펴볼 수 있다."));
                    action("탈출하고 기록 저장",true,() -> host.finishGame(ending,false));
                }
            }
        });
    }

    private void move(Game game,Scenario.Passage passage) {
        String blocked = game.passageBlock(passage);
        if (!blocked.isEmpty()) {
            showNote(passage.name(),blocked);
            return;
        }
        game.move(passage);
        showNote("현장",RoomTargets.arrival(game.area().id()));
        render();
    }

    private void showScene(Game game,Scenario.Scene scene) {
        boolean firstRead = !game.inspected().contains(scene.id());
        String body = game.inspect(scene);
        if (scene.type().equals("DIALOGUE")) {
            showDialogue(new Dialogue(scene.title(),body.lines().filter(line -> !line.isBlank()).toList(),0,true,null));
        }
        else if (scene.type().equals("CLUE")) {
            showNote(scene.title()+(firstRead ? " · 새 단서" : ""),body);
            SceneChrome.annotate(note,body,ClueCues.marks(game.scenario.caseId(),scene),SceneChrome.NOTE_MARK);
            if (firstRead) showClue(game,List.of(scene),0,photo);
        } else showNote(scene.title(),body);
        render();
    }

    private void showDialogue(Dialogue dialogue) {
        if (dialogue.lines().isEmpty()) {
            showNote(dialogue.title(),"");
            if (dialogue.after()!=null) dialogue.after().run();
            return;
        }
        currentDialogue = dialogue;
        String entry = dialogue.lines().get(dialogue.line());
        int speaker = dialogue.spoken() ? entry.indexOf(": ") : -1;
        String heading = speaker>0 ? entry.substring(0,speaker)+"   /   "+dialogue.title() : dialogue.title();
        String body = speaker>0 ? entry.substring(speaker+2).strip() : entry;
        writeNote(heading,body);
        String next=dialogue.line()+1<dialogue.lines().size() ? (dialogue.spoken() ? "다음 대사" : "다음 장면")
                : dialogue.after()==null ? "조사 재개" : "결말 닫기";
        dialogueCue.setText((dialogue.line()+1)+"/"+dialogue.lines().size()+"   화면 클릭 · "+next);
        photo.clearTarget();
        advanceSurface.setVisible(true);
        advanceSurface.requestFocusInWindow();
    }

    private void advanceDialogue() {
        Dialogue dialogue = currentDialogue;
        if (dialogue==null) return;
        if (dialogue.line()+1<dialogue.lines().size()) {
            showDialogue(new Dialogue(dialogue.title(),dialogue.lines(),dialogue.line()+1,dialogue.spoken(),dialogue.after()));
        } else {
            currentDialogue = null;
            advanceSurface.setVisible(false);
            if (dialogue.after()!=null) dialogue.after().run();
            else dialogueCue.setText("조사 수첩에 기록됨");
            photo.requestFocusInWindow();
        }
    }

    private void updateState() {
        Game game = host.game;
        if (game==null || start==null) return;
        long seconds = Math.max(0,Duration.between(start,Instant.now()).toSeconds());
        state.setText("회차 "+game.run+"   ·   "+String.format("%02d:%02d",seconds/60,seconds%60)
                +"   ·   해결 "+game.solved().size()+"/"+game.scenario.puzzles().size()
                +"   ·   빈 손 "+game.freeHands()+"/2");
    }

    private void puzzle(Game game,Scenario.Puzzle puzzle) {
        if (game.solved(puzzle.id())) { showNote(puzzle.name(),puzzle.solvedText()); return; }
        String blocked = game.puzzleBlock(puzzle);
        if (!blocked.isEmpty()) { showNote(puzzle.name(),blocked); return; }
        List<Scenario.Scene> clues=ClueCues.sources(game,puzzle);
        showNote(puzzle.name(),puzzle.prompt());
        showPuzzleCue(game,puzzle,clues,false);
        puzzleControls(game,puzzle,clues);
    }

    private void showPuzzleCue(Game game,Scenario.Puzzle puzzle,List<Scenario.Scene> clues,boolean retry) {
        ClueCues.MarkedText guide=ClueCues.prompt(game,puzzle,clues,retry);
        SceneChrome.annotate(note,guide.text(),guide.marks(),SceneChrome.NOTE_MARK);
    }

    private void puzzleControls(Game game,Scenario.Puzzle puzzle,List<Scenario.Scene> clues) {
        JTextField answer = SceneChrome.answerInput();
        boolean power = "POWER".equals(puzzle.type());
        if (power) {
            JCheckBox lift = switchBox("승강기");
            JCheckBox lights = switchBox("조명");
            JCheckBox vent = switchBox("배기");
            for (JCheckBox box : new JCheckBox[]{lift,lights,vent}) {
                box.addActionListener(e -> answer.setText((lift.isSelected() ? "1" : "0")
                        +(lights.isSelected() ? "1" : "0")+(vent.isSelected() ? "1" : "0")));
                interaction.add(box);
            }
            answer.setText("000");
            answer.setEditable(false);
        } else {
            JLabel answerLabel = label("답안",13,MUTED);
            answerLabel.setLabelFor(answer);
            interaction.add(answerLabel);
        }
        interaction.add(answer);
        if (!clues.isEmpty()) action("관련 단서 보기",false,() -> showClue(game,clues,0,answer));
        action("정답·해설 보기",false,() -> showSolution(puzzle,answer));
        Runnable submit = () -> {
            String submitted = answer.getText();
            host.work(host.repository::now,now -> locally(() -> {
                boolean correct = game.submit(puzzle,submitted,now);
                if (correct) showNote(puzzle.name()+" · 해결",puzzle.solvedText());
                else {
                    showPuzzleCue(game,puzzle,clues,true);
                    if (!power) { answer.selectAll(); answer.requestFocusInWindow(); }
                }
                render();
            }));
        };
        action("입력",true,submit);
        answer.addActionListener(e -> submit.run());
        if (!power) answer.requestFocusInWindow();
    }

    private void showSolution(Scenario.Puzzle puzzle,JComponent returnTo) {
        JPanel body=SceneChrome.clear(new BorderLayout(0,12));
        body.setBorder(BorderFactory.createEmptyBorder(12,7,5,6));
        body.add(label("정답   "+puzzle.answer(),27,ACCENT),BorderLayout.NORTH);
        JTextArea explanation=SceneChrome.transcript();
        explanation.setText(ClueCues.explanation(puzzle));
        explanation.setCaretPosition(0);
        body.add(SceneChrome.scroll(explanation),BorderLayout.CENTER);
        openReadingCard(puzzle.name()+" · 정답과 해설",body,null,returnTo);
    }

    private void showClue(Game game,List<Scenario.Scene> scenes,int index,JComponent returnTo) {
        Scenario.Scene scene=scenes.get(index);
        JPanel footer=SceneChrome.clear(new BorderLayout(8,0));
        String location=game.scenario.area(scene.areaId()).name();
        if (scenes.size()>1) location+="  /  "+(index+1)+" · "+scenes.size();
        footer.add(label(location,13,MUTED),BorderLayout.WEST);
        if (scenes.size()>1) {
            JPanel step=SceneChrome.clear(new FlowLayout(FlowLayout.RIGHT,7,0));
            if (index>0) {
                JButton previous=SceneChrome.control("이전 단서",false);
                previous.addActionListener(e -> showClue(game,scenes,index-1,returnTo));
                step.add(previous);
            }
            if (index+1<scenes.size()) {
                JButton next=SceneChrome.control("다음 단서",true);
                next.addActionListener(e -> showClue(game,scenes,index+1,returnTo));
                step.add(next);
            }
            footer.add(step,BorderLayout.EAST);
        }
        openReadingCard(scene.title(),new ClueExhibit(game.scenario.caseId(),scene),footer,returnTo);
    }

    private static JCheckBox switchBox(String name) {
        JCheckBox box = new JCheckBox(name);
        box.setUI(new javax.swing.plaf.basic.BasicCheckBoxUI());
        box.setOpaque(false);
        box.setForeground(INK);
        box.setIcon(SceneChrome.checkMark(false));
        box.setSelectedIcon(SceneChrome.checkMark(true));
        return box;
    }

    private void evidence(Game game,Scenario.Evidence evidence) {
        if (!game.unlocked(evidence.unlock())) {
            showNote(evidence.name(),"아직 잠겨 있다. 주변 기록을 대조해 먼저 잠금을 풀어야 한다.");
            return;
        }
        showNote(evidence.name(),evidence.note());
        evidenceActions(game,evidence,false);
    }

    private void evidenceActions(Game game,Scenario.Evidence evidence,boolean fromLocker) {
        String holding = game.held().get(evidence.id());
        if (evidence.originalHands()!=null && !"ORIGINAL".equals(holding)) {
            action("원본 회수 · "+evidence.originalHands()+"손",false,() -> recover(game,evidence,"ORIGINAL"));
        }
        if (evidence.copyAllowed() && !"COPY".equals(holding)) {
            action("복사본 확보",true,() -> recover(game,evidence,"COPY"));
        }
        if (fromLocker && holding!=null) action("보관함에 두기",false,() -> {
            game.putBack(evidence);
            showNote(evidence.name(),"현장 보관함에 넣었다. 이 방에서는 조사 수첩을 열어 다시 챙길 수 있다.");
            render();
        });
    }

    private void recover(Game game,Scenario.Evidence evidence,String form) {
        locally(() -> {
            boolean first = !game.recovered().contains(evidence.id());
            game.recover(evidence,form);
            String formText = "ORIGINAL".equals(form) ? "녹화 장치 원본을 확보했다." : "복사본을 확보했다.";
            if (game.freeHands()==0) formText+=" 양손이 차서 사다리를 이용할 수 없다.";
            showNote(first ? "새 증거   /   "+evidence.name() : evidence.name(),
                    formText+" 이 방에서는 조사 수첩을 열어 보관 형태를 바꿀 수 있다.");
            render();
        });
    }

    private void notebook() {
        Game game = host.game;
        if (game==null) return;
        List<Scenario.Scene> discovered=game.scenario.scenes().stream()
                .filter(s -> game.inspected().contains(s.id())).toList();
        String records = discovered.stream()
                .map(s -> "["+game.scenario.area(s.areaId()).name()+"] "+s.title()+"\n"+s.content())
                .collect(Collectors.joining("\n\n"));
        String inventory = game.scenario.evidence().stream().filter(e -> game.recovered().contains(e.id()))
                .map(e -> e.name()+" · "+evidenceStatus(game,e))
                .collect(Collectors.joining("\n"));
        JTextArea text = SceneChrome.transcript();
        List<String> marks=discovered.stream().filter(s -> s.type().equals("CLUE"))
                .flatMap(s -> ClueCues.marks(game.scenario.caseId(),s).stream()).distinct().toList();
        SceneChrome.annotate(text,"확보한 증거\n"+(inventory.isEmpty() ? "없음" : inventory)+"\n\n조사 기록\n"
                +(records.isEmpty() ? "아직 조사한 장면이 없습니다." : records),marks,SceneChrome.NOTE_MARK);
        List<Scenario.Evidence> here = game.scenario.evidence().stream().filter(e ->
                game.recovered().contains(e.id()) && e.areaId()==game.area().id()).toList();
        JPanel locker=null;
        if (!here.isEmpty()) {
            locker=SceneChrome.clear(new FlowLayout(FlowLayout.RIGHT,7,0));
            for (Scenario.Evidence evidence : here) {
                JButton item=SceneChrome.control("보관함 · "+evidence.name(),false);
                item.addActionListener(e -> {
                    closeNotebook();
                    showNote("증거 보관함   /   "+evidence.name(),"현재 상태: "+evidenceStatus(game,evidence)+".");
                    evidenceActions(game,evidence,true);
                });
                locker.add(item);
            }
        }
        openReadingCard("조사 수첩",SceneChrome.scroll(text),locker,photo);
    }

    private void openReadingCard(String title,JComponent content,JPanel footer,JComponent returnTo) {
        readingReturn=returnTo;
        notebookCard.removeAll();
        JPanel header=SceneChrome.clear(new BorderLayout(8,0));
        header.add(label(title,20,INK),BorderLayout.WEST);
        JButton close=SceneChrome.control("닫기",false);
        close.addActionListener(e -> closeNotebook());
        header.add(close,BorderLayout.EAST);
        notebookCard.add(header,BorderLayout.NORTH);
        notebookCard.add(content,BorderLayout.CENTER);
        if (footer!=null) notebookCard.add(footer,BorderLayout.SOUTH);
        photo.clearTarget();
        notebookMask.setVisible(true);
        notebookCard.setVisible(true);
        notebookCard.revalidate();
        notebookCard.repaint();
        close.requestFocusInWindow();
    }

    private void closeNotebook() {
        notebookCard.setVisible(false);
        notebookMask.setVisible(false);
        JComponent destination=readingReturn;
        readingReturn=null;
        if (destination==null || !destination.requestFocusInWindow()) photo.requestFocusInWindow();
    }

    private static String evidenceStatus(Game game,Scenario.Evidence evidence) {
        String form = game.held().get(evidence.id());
        if (form==null) return "현장 보관함";
        return "ORIGINAL".equals(form) ? "원본 보유" : "복사본 보유";
    }

    private void stopMission() {
        if (host.game==null) return;
        showNote("임무 중단","지금 철수하면 이번 회차는 실패로 기록된다.");
        action("계속 조사하기",false,() -> showNote(host.game.area().name(),"조사를 계속한다."));
        action("철수하고 기록 저장",true,() -> host.finishGame(null,false));
    }

    private void showNote(String title,String content) {
        currentDialogue = null;
        dialogueCue.setText("");
        advanceSurface.setVisible(false);
        writeNote(title,content);
    }

    private void writeNote(String title,String content) {
        noteTitle.setText(title);
        SceneChrome.annotate(note,content,List.of(),SceneChrome.NOTE_MARK);
        interaction.removeAll();
        interaction.revalidate();
        interaction.repaint();
        revalidate();
    }

    private void locally(Runnable action) {
        try { action.run(); }
        catch (IllegalStateException e) {
            SceneChrome.annotate(note,e.getMessage(),List.of(),SceneChrome.NOTE_MARK);
        }
    }

    private void action(String label,boolean primary,Runnable callback) {
        JButton button = SceneChrome.control(label,primary);
        button.addActionListener(e -> callback.run());
        interaction.add(button);
        interaction.revalidate();
        interaction.repaint();
        revalidate();
    }
}

