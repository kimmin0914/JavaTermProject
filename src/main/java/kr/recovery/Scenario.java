package kr.recovery;

import java.util.List;

record Scenario(long caseId, String title, String briefing, List<Area> areas,
                List<Scene> scenes, List<Puzzle> puzzles, List<Passage> passages,
                List<Evidence> evidence, List<Ending> endings, List<Requirement> requirements,
                List<Clue> clues) {
    record Area(long id, String name, String type, int x, int y) {}
    record Scene(long id, long areaId, String type, String title, String content, int order) {}
    record Puzzle(long id, long areaId, String name, String type, String prompt, String answer,
                  String hint, String solvedText, Long prerequisite, int hands) {}
    record Passage(long id, long from, long to, String name, int hands, boolean power, Long unlock) {}
    record Evidence(long id, long areaId, String name, String note, Integer originalHands,
                    boolean copyAllowed, Long unlock) {}
    record Ending(long id, String title, String text, int rank) {}
    record Requirement(long ending, long evidence, String form) {}
    record Clue(long puzzle, long scene) {}

    Area area(long id) {
        return areas.stream().filter(area -> area.id() == id).findFirst().orElseThrow();
    }

    Area startArea() {
        return areas.stream().filter(area -> area.type().equals("START")).findFirst()
                .orElseThrow(() -> new IllegalStateException("시작 구역이 없습니다."));
    }

    static String sceneType(String type) {
        return switch (type) {
            case "CLUE" -> "단서";
            case "DIALOGUE" -> "대화";
            case "STORY" -> "기록";
            default -> type;
        };
    }
}

