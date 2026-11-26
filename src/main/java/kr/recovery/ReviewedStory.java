package kr.recovery;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class ReviewedStory {
    private static final Properties COPY = load();

    private ReviewedStory() {}

    static Scenario apply(Scenario data) {
        if (data.caseId()!=1) return data;
        return new Scenario(data.caseId(),data.title(),replace("case.1.briefing",data.briefing()),data.areas(),
                data.scenes().stream().map(scene -> new Scenario.Scene(scene.id(),scene.areaId(),scene.type(),
                        scene.title(),replace("scene."+scene.id()+".content",scene.content()),scene.order())).toList(),
                data.puzzles().stream().map(puzzle -> {
                    String key="puzzle."+puzzle.id();
                    return new Scenario.Puzzle(puzzle.id(),puzzle.areaId(),puzzle.name(),puzzle.type(),
                            replace(key+".prompt",puzzle.prompt()),puzzle.answer(),replace(key+".hint_text",puzzle.hint()),
                            replace(key+".solved_text",puzzle.solvedText()),puzzle.prerequisite(),puzzle.hands());
                }).toList(),
                data.passages(),
                data.evidence().stream().map(evidence -> new Scenario.Evidence(evidence.id(),evidence.areaId(),
                        evidence.name(),replace("evidence."+evidence.id()+".evidence_note",evidence.note()),
                        evidence.originalHands(),evidence.copyAllowed(),evidence.unlock())).toList(),
                data.endings().stream().map(ending -> new Scenario.Ending(ending.id(),ending.title(),
                        replace("ending."+ending.id()+".ending_text",ending.text()),ending.rank())).toList(),
                data.requirements(),data.clues());
    }

    private static String replace(String key,String current) {
        String before=COPY.getProperty(key+".before");
        return before!=null && before.equals(current.replace("\r\n","\n"))
                ? COPY.getProperty(key+".after",current) : current;
    }

    private static Properties load() {
        Properties data=new Properties();
        try (var source=ReviewedStory.class.getResourceAsStream("/story/case-one-revision.properties")) {
            if (source!=null) {
                try (var reader=new InputStreamReader(source,StandardCharsets.UTF_8)) { data.load(reader); }
            }
        } catch (IOException e) {
            throw new IllegalStateException("사건 문장 개정 자료를 읽을 수 없습니다.",e);
        }
        return data;
    }
}

