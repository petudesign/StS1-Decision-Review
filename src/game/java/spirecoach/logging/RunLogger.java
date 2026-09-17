package spirecoach.logging;

import spirecoach.domain.CombatSnapshot;
import spirecoach.domain.Lesson;
import spirecoach.domain.TurnSnapshot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Local run-level event log. Raw NDJSON stays the source of truth for analysis. */
public final class RunLogger implements AutoCloseable {
    private final BufferedWriter writer;
    private final Path path;

    private RunLogger(BufferedWriter writer, Path path) {
        this.writer = writer;
        this.path = path;
    }

    public static RunLogger open() {
        try {
            Path directory = Paths.get(System.getProperty("user.dir"), "logs", "spirecoach");
            Files.createDirectories(directory);
            String filename = "run-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date()) + ".ndjson";
            Path path = directory.resolve(filename);
            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new RunLogger(writer, path);
        } catch (IOException | SecurityException error) {
            System.err.println("[Spire Coach] Could not open run log: " + error.getMessage());
            return new RunLogger(null, null);
        }
    }

    public Path getPath() { return path; }

    public synchronized void runStart(TurnSnapshot state) {
        if (state == null) return;
        StringBuilder json = new StringBuilder("{\"event\":\"run_start\"");
        field(json, "floor", state.getFloor());
        field(json, "act", state.getAct());
        field(json, "character", state.getCharacter());
        field(json, "ascension", state.getAscension());
        write(json.append('}').toString());
    }

    public synchronized void combatEnd(CombatSnapshot combat, List<Lesson> lessons) {
        if (combat == null) return;
        StringBuilder json = new StringBuilder("{\"event\":\"combat_end\"");
        field(json, "floor", combat.getFloor());
        field(json, "act", combat.getAct());
        field(json, "character", combat.getCharacter());
        field(json, "won", combat.isWon());
        field(json, "lessonCount", lessons == null ? 0 : lessons.size());
        json.append(",\"lessonRuleIds\":[");
        if (lessons != null) {
            for (int i = 0; i < lessons.size(); i++) {
                if (i > 0) json.append(',');
                json.append(quote(lessons.get(i).getRuleId()));
            }
        }
        json.append("]}");
        write(json.toString());
    }

    public synchronized void cardReward(int floor, int act, String character,
                                         List<String> offeredIds, String selectedId,
                                         String selectedName, boolean skipped,
                                         int deckSizeBefore, int deckSizeAfter,
                                         int attacks, int skills, int powers,
                                         String advice) {
        StringBuilder json = new StringBuilder("{\"event\":\"card_reward\"");
        field(json, "floor", floor);
        field(json, "act", act);
        field(json, "character", character);
        field(json, "skipped", skipped);
        field(json, "selectedId", selectedId);
        field(json, "selectedName", selectedName);
        field(json, "deckSizeBefore", deckSizeBefore);
        field(json, "deckSizeAfter", deckSizeAfter);
        field(json, "attacksBefore", attacks);
        field(json, "skillsBefore", skills);
        field(json, "powersBefore", powers);
        field(json, "advice", advice);
        json.append(",\"offeredIds\":[");
        if (offeredIds != null) {
            for (int i = 0; i < offeredIds.size(); i++) {
                if (i > 0) json.append(',');
                json.append(quote(offeredIds.get(i)));
            }
        }
        json.append("]}");
        write(json.toString());
    }

    public synchronized void runEnd(int floor, int act, String character, boolean won,
                                     int combats, int lessons, int rewards,
                                     int skipped, int cardsAdded) {
        StringBuilder json = new StringBuilder("{\"event\":\"run_end\"");
        field(json, "floor", floor);
        field(json, "act", act);
        field(json, "character", character);
        field(json, "won", won);
        field(json, "combatsReviewed", combats);
        field(json, "lessonsFound", lessons);
        field(json, "cardRewards", rewards);
        field(json, "rewardsSkipped", skipped);
        field(json, "cardsAdded", cardsAdded);
        write(json.append('}').toString());
        flush();
    }

    private void write(String line) {
        if (writer == null) return;
        try {
            writer.write(line);
            writer.newLine();
            flush();
        } catch (IOException error) {
            System.err.println("[Spire Coach] Run log write failed: " + error.getMessage());
        }
    }

    private void flush() {
        if (writer == null) return;
        try {
            writer.flush();
        } catch (IOException error) {
            System.err.println("[Spire Coach] Run log flush failed: " + error.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        if (writer == null) return;
        try {
            flush();
            writer.close();
        } catch (IOException error) {
            System.err.println("[Spire Coach] Run log close failed: " + error.getMessage());
        }
    }

    private static void field(StringBuilder json, String key, String value) {
        json.append(',').append(quote(key)).append(':').append(quote(value));
    }

    private static void field(StringBuilder json, String key, int value) {
        json.append(',').append(quote(key)).append(':').append(value);
    }

    private static void field(StringBuilder json, String key, boolean value) {
        json.append(',').append(quote(key)).append(':').append(value);
    }

    private static String quote(String value) {
        if (value == null) return "null";
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default: escaped.append(c);
            }
        }
        return escaped.append('"').toString();
    }
}
