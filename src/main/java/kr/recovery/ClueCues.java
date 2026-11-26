package kr.recovery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ClueCues {
    record Focus(String role,List<String> phrases) {}
    record MarkedText(String text,List<String> marks) {}

    private static final Map<Long,Focus> CASE_ONE = Map.of(
            2L,new Focus("기록의 발생 순서",List.of("발생 순서")),
            3L,new Focus("시각에서 읽을 자리",List.of("표시 시각","분 일의 자리")),
            5L,new Focus("표시 시각과 실제 시각의 차이",List.of("2분 빠르다","표시된 시각은 21:17")),
            6L,new Focus("어떤 종류의 명령인지",List.of("출입 차단","일치하는 코드")),
            7L,new Focus("전력 한도와 필요한 장비",List.of("전력 한도는 6","필수 장비","필요하다","필요하지 않다")),
            8L,new Focus("회로별 부하와 입력 순서",List.of("입력 자리 순서","켜짐","꺼짐")));

    private ClueCues() {}

    static List<String> marks(long caseId,Scenario.Scene scene) {
        Focus focus=caseId==1 ? CASE_ONE.get(scene.id()) : null;
        return focus==null ? List.of() : focus.phrases();
    }

    static List<Scenario.Scene> sources(Game game,Scenario.Puzzle puzzle) {
        var linkIds=game.scenario.clues().stream().filter(link -> link.puzzle()==puzzle.id())
                .map(Scenario.Clue::scene).collect(Collectors.toSet());
        var read=game.inspected();
        return game.scenario.scenes().stream().filter(s -> linkIds.contains(s.id()) && read.contains(s.id()))
                .sorted(Comparator.comparingLong(Scenario.Scene::id)).toList();
    }

    static MarkedText prompt(Game game,Scenario.Puzzle puzzle,List<Scenario.Scene> clues,boolean retry) {
        var highlighted=new ArrayList<String>();
        var references=new ArrayList<String>();
        for (Scenario.Scene scene:clues) {
            String title="「"+scene.title()+"」";
            Focus focus=game.scenario.caseId()==1 ? CASE_ONE.get(scene.id()) : null;
            highlighted.add(title);
            if (focus==null) references.add(title);
            else {
                references.add(title+"  "+focus.role());
                highlighted.add(focus.role());
            }
        }
        String leading=(retry ? "기록과 맞지 않는다. 다시 대조해 보자.\n" : "")+puzzle.prompt();
        if (!references.isEmpty()) leading+="\n대조할 곳  ·  "+String.join("     /     ",references);
        return new MarkedText(leading,List.copyOf(highlighted));
    }

    static String exhibitText(Scenario.Scene scene) {
        String text=scene.content().replace(" / ","\n").replace(". ",".\n");
        return switch ((int)scene.id()) {
            case 3 -> text.replace("자리만 이어 ","자리만\n이어 ")
                    .replace("순서로 각 ","순서로\n각 ").replace("자리를 이어 ","자리를\n이어 ");
            case 6 -> text.replace("시각에 일치","시각에\n일치").replace("시각과 일치","시각과\n일치");
            default -> text;
        };
    }

    static String explanation(Scenario.Puzzle puzzle) {
        return switch (puzzle.type()) {
            case "CODE" -> "1. 상황판에서 사건의 발생 순서를 먼저 확인한다.\n"
                    + puzzle.hint() + "\n\n"
                    + "2. 잠금 메모의 규칙대로 각 시각에서 분의 일의 자리만 읽는다.\n"
                    + "읽어 낸 세 숫자를 발생 순서대로 이어 붙이고, 맨 앞의 0도 그대로 입력한다.";
            case "POWER" -> "1. 비상 전력 기준에서 탈출에 필요한 장비를 골라 부하의 합을 확인한다.\n"
                    + puzzle.hint() + "\n\n"
                    + "2. 부하 기록에 적힌 입력 순서대로 스위치를 설정한다.\n"
                    + "켜야 하는 회로는 1, 꺼야 하는 회로는 0이다.";
            case "VERIFY" -> "1. CCTV 점검표의 시계 오차를 반영해 화면에 표시된 시각을 실제 시각으로 바꾼다.\n"
                    + puzzle.hint() + "\n\n"
                    + "2. 출입 명령 목록에서 실제 시각과 명령 종류가 모두 일치하는 줄을 찾는다.\n"
                    + "그 줄에 적힌 출입 차단 코드를 입력한다.";
            default -> puzzle.hint();
        };
    }
}

