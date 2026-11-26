package kr.recovery;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class RoomTargets {
    enum Kind { SCENE, PUZZLE, EVIDENCE, PASSAGE, EXIT }

    record Spot(Kind kind,long id,double x,double y,String object,String hint) {}

    private static final Map<Long,List<Spot>> ROOMS = Map.of(
        1L,List.of(
            place(Kind.SCENE,1,.444,.443,"벽면 인터폰","탈출에 필요한 단서를 찾아보자. 난간 옆 벽면 인터폰에서 무전이 들린다."),
            place(Kind.PASSAGE,1,.721,.50,"철제 출입문","불빛이 비치는 철제 문으로 들어가 보자."),
            place(Kind.PASSAGE,7,.257,.711,"외부 계단","왼쪽의 노란 계단은 옥상 탈출점으로 이어진다.")),
        2L,List.of(
            place(Kind.SCENE,11,.514,.507,"노란 무전기","난간 중앙의 노란 무전기로 마지막 통신을 확인해 보자."),
            place(Kind.PASSAGE,8,.818,.476,"고정 사다리","벽 사다리를 타면 처음 들어왔던 옥상으로 돌아갈 수 있다."),
            place(Kind.EXIT,2,.278,.592,"열린 철제 문","열린 철제 문 아래로 계단이 보인다. 가져갈 증거를 확인했다면 여기서 탈출할 수 있다.")),
        3L,List.of(
            place(Kind.SCENE,2,.159,.405,"벽의 상황판","벽의 상황판에 서로 다른 시각이 남아 있다."),
            place(Kind.SCENE,10,.091,.684,"상황판 아래 클립보드","상황판 아래 클립보드에 장치를 옮길 때의 주의사항이 남아 있다."),
            place(Kind.PASSAGE,2,.328,.478,"왼쪽 출입구","왼쪽 창으로 빗속 계단이 보인다. 옥상 진입점으로 돌아가는 길이다."),
            place(Kind.PASSAGE,3,.698,.486,"오른쪽 보안실 문","푸른 창 너머에 감시 화면이 보인다."),
            place(Kind.PASSAGE,5,.519,.515,"복도 끝 제어실 문","복도 끝 주황 불빛 아래 문으로 제어실에 들어갈 수 있다."),
            place(Kind.PASSAGE,9,.877,.507,"화물 승강기 문","오른쪽의 넓은 철제 문은 화물 승강기로 이어진다.")),
        4L,List.of(
            place(Kind.SCENE,3,.12,.45,"선반에 붙은 메모","선반에 끼워진 종이를 살펴보자. 잠금 번호를 맞추는 방법이 적혀 있을 수 있다."),
            place(Kind.SCENE,4,.344,.778,"의자 위 명단","의자 위에 펼쳐진 명단에 빠진 이름이 있다."),
            place(Kind.SCENE,5,.642,.335,"CCTV 옆 벽시계","푸른 CCTV 화면 옆에 시계 점검표가 붙어 있다."),
            place(Kind.SCENE,6,.469,.595,"책상 위 출입 기록","책상 위 붉은 집게로 고정된 종이에는 출입 기록이 있다."),
            place(Kind.PUZZLE,1,.618,.592,"숫자 키패드","서류로 잠금 번호의 규칙을 알아냈다. 책상 위 숫자 키패드를 조작해 보자."),
            place(Kind.PUZZLE,3,.746,.357,"기록 대조 모니터","벽시계 오른쪽 모니터에서 실제 시각에 해당하는 출입 차단 명령을 대조하자."),
            place(Kind.EVIDENCE,1,.227,.614,"CCTV 녹화 장치","선반 아래의 은색 장치에서 원본을 챙기거나 영상만 복사할 수 있다."),
            place(Kind.EVIDENCE,2,.772,.565,"출입 기록 단말기","책상 오른쪽 초록불이 켜진 단말기에 출입 기록 복사본이 남아 있다."),
            place(Kind.PASSAGE,4,.919,.479,"복도 쪽 출입문","오른쪽 끝 출입문으로 중앙 복도에 돌아갈 수 있다.")),
        5L,List.of(
            place(Kind.SCENE,7,.243,.416,"왼쪽 계기판","왼쪽의 두 계기에서 남아 있는 전력 한도를 확인해 보자."),
            place(Kind.SCENE,8,.551,.411,"오른쪽 회로 계기","같은 제어반의 오른쪽 계기들에 회로별 부하가 표시돼 있다."),
            place(Kind.SCENE,9,.705,.498,"벽에 걸린 수화기","오른쪽 벽에 걸린 수화기에서 남겨진 음성을 확인할 수 있다."),
            place(Kind.PUZZLE,2,.396,.586,"제어반 스위치","계기들을 읽었다면 제어반 아래의 스위치 세 개를 조작해 보자."),
            place(Kind.PASSAGE,6,.853,.495,"제어실 출입구","오른쪽 초록색 문으로 중앙 복도에 돌아갈 수 있다.")),
        6L,List.of(
            place(Kind.SCENE,12,.543,.492,"콘크리트 기둥의 인터폰","셔터 옆 기둥의 인터폰에서 마지막 통신을 확인해 보자."),
            place(Kind.PASSAGE,10,.303,.487,"화물 승강기 쪽 셔터","초록빛이 비치는 셔터는 화물 승강기로 돌아가는 길이다."),
            place(Kind.EXIT,6,.760,.608,"하역장 바깥 경사로","오른쪽 빗속 경사로가 건물 밖으로 이어진다. 증거를 챙겼다면 그곳으로 탈출할 수 있다."))
    );

    private RoomTargets() {}

    private static Spot place(Kind kind,long id,double x,double y,String object,String hint) {
        return new Spot(kind,id,x,y,object,hint);
    }

    static String arrival(long area) {
        return switch ((int)area) {
            case 1 -> "빗물이 난간 끝에서 떨어진다. 문틈으로 실내 조명이 새어 나온다.";
            case 2 -> "옥상 끝에 사다리가 걸려 있다. 아래쪽에서 바람이 올라온다.";
            case 3 -> "낡은 형광등이 깜박인다. 누군가 급히 정리한 상황판이 벽에 걸려 있다.";
            case 4 -> "어두운 방 안에서 모니터 하나만 켜져 있다. 책상과 선반에는 급히 남긴 흔적이 있다.";
            case 5 -> "바닥을 타고 낮은 진동이 전해진다. 제어반의 계기 바늘은 아직 움직인다.";
            case 6 -> "화물 승강기 문 뒤로 비 냄새가 들어온다. 경사로 너머로 바깥 불빛이 보인다.";
            default -> "천천히 주변을 살펴본다.";
        };
    }

    static List<Spot> forArea(Scenario scenario,long area) {
        return ROOMS.getOrDefault(area,List.of()).stream().filter(spot -> switch (spot.kind()) {
            case SCENE -> scenario.scenes().stream().anyMatch(s -> s.id()==spot.id() && s.areaId()==area);
            case PUZZLE -> scenario.puzzles().stream().anyMatch(p -> p.id()==spot.id() && p.areaId()==area);
            case EVIDENCE -> scenario.evidence().stream().anyMatch(e -> e.id()==spot.id() && e.areaId()==area);
            case PASSAGE -> scenario.passages().stream().anyMatch(p -> p.id()==spot.id() && p.from()==area);
            case EXIT -> scenario.area(area).type().equals("EXIT");
        }).toList();
    }

    static String hint(Game game) {
        Scenario scenario = game.scenario;
        long current = game.area().id();
        Set<Long> read = game.inspected();
        if (current==1 && !read.contains(1L)) {
            return spotHint(scenario,current,Kind.SCENE,1).orElse("옥상에서 현장 무전부터 확인해 보자.");
        }
        Optional<Scenario.Puzzle> next = scenario.puzzles().stream()
                .filter(p -> !game.solved(p.id()) && game.unlocked(p.prerequisite())).findFirst();
        if (next.isPresent()) {
            Scenario.Puzzle puzzle = next.get();
            List<Scenario.Scene> undiscovered = scenario.clues().stream()
                    .filter(clue -> clue.puzzle()==puzzle.id() && !read.contains(clue.scene()))
                    .flatMap(clue -> scenario.scenes().stream().filter(s -> s.id()==clue.scene())).toList();
            Optional<Scenario.Scene> local = undiscovered.stream().filter(scene -> scene.areaId()==current).findFirst();
            if (local.isPresent()) return spotHint(scenario,current,Kind.SCENE,local.get().id()).orElse("이 방에 연결 단서가 남아 있다.");
            if (!undiscovered.isEmpty()) return routeHint(game,undiscovered.get(0).areaId(),"아직 못 찾은 기록이 있다.");
            if (game.freeHands()<puzzle.hands()) {
                var heavy = scenario.evidence().stream().filter(e -> "ORIGINAL".equals(game.held().get(e.id()))).findFirst();
                if (heavy.isPresent() && heavy.get().areaId()!=current) {
                    return routeHint(game,heavy.get().areaId(),"양손을 비워야 한다. 원본을 발견한 곳으로 돌아가자.");
                }
                return "원본을 들면 스위치를 조작할 수 없다. 조사 수첩의 보관함에서 양손을 비우자.";
            }
            if (puzzle.areaId()!=current) return routeHint(game,puzzle.areaId(),"기록을 확인했다. 이제 조작할 장치를 찾아가자.");
            return spotHint(scenario,current,Kind.PUZZLE,puzzle.id()).orElse("발견한 기록으로 이 방의 장치를 조작할 수 있다.");
        }
        Optional<Scenario.Evidence> essential = scenario.evidence().stream().filter(e -> e.id()==1).findFirst();
        if (essential.isPresent() && !game.held().containsKey(essential.get().id())) {
            Scenario.Evidence e = essential.get();
            if (e.areaId()!=current) return routeHint(game,e.areaId(),"탈출 전에 CCTV 기록을 확보하자.");
            if (game.recovered().contains(e.id())) return "보관함에 CCTV 기록을 두고 왔다. 조사 수첩을 열어 챙기자.";
            return spotHint(scenario,current,Kind.EVIDENCE,e.id()).orElse("이 방에서 CCTV 기록을 확보할 수 있다.");
        }
        if ("EXIT".equals(game.area().type())) {
            Optional<Spot> call = forArea(scenario,current).stream()
                    .filter(s -> s.kind()==Kind.SCENE && !read.contains(s.id())).findFirst();
            if (call.isPresent()) return call.get().hint();
            return spotHint(scenario,current,Kind.EXIT,current).orElse("탈출할 지점을 찾아보자.");
        }
        if (current==4 && scenario.evidence().stream().anyMatch(e -> e.id()==2 && !game.held().containsKey(e.id()))) {
            if (game.recovered().contains(2L)) return "출입 기록은 보관함에 있다. 챙기려면 조사 수첩을 열고, 떠날 준비가 됐다면 출입문으로 가자.";
            return "CCTV를 확보했다. 출입 로그도 챙기려면 책상 오른쪽 단말기를 확인하자.";
        }
        long exit = game.freeHands()==2 ? 2 : 6;
        return routeHint(game,exit,exit==2 ? "빈 손으로 옥상 사다리를 이용할 수 있다. 하역장도 갈 수 있다."
                : "원본을 든 채로는 사다리를 오를 수 없다. 하역장으로 이어지는 승강기를 찾자.");
    }

    private static Optional<String> spotHint(Scenario scenario,long area,Kind kind,long id) {
        return forArea(scenario,area).stream().filter(s -> s.kind()==kind && s.id()==id).map(Spot::hint).findFirst();
    }

    private static String routeHint(Game game,long destination,String prefix) {
        record Step(long area,Scenario.Passage first) {}
        var queue = new ArrayDeque<Step>();
        var visited = new HashSet<Long>();
        long current = game.area().id();
        queue.add(new Step(current,null));
        visited.add(current);
        while (!queue.isEmpty()) {
            Step step = queue.removeFirst();
            if (step.area()==destination && step.first()!=null) {
                return prefix+" "+spotHint(game.scenario,current,Kind.PASSAGE,step.first().id())
                        .orElse("이동할 수 있는 출입구를 살펴보자.");
            }
            for (Scenario.Passage path : game.scenario.passages()) {
                if (path.from()==step.area() && game.passageBlock(path).isEmpty() && visited.add(path.to())) {
                    queue.addLast(new Step(path.to(),step.first()==null ? path : step.first()));
                }
            }
        }
        return prefix;
    }
}

