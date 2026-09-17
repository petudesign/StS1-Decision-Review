package spirecoach.logging;

import spirecoach.domain.ActionSnapshot;
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

/** Small buffered NDJSON writer. Logging failures are intentionally non-fatal to gameplay. */
public final class NdjsonLogger implements AutoCloseable {
    private final BufferedWriter writer;
    private final Path path;
    private int linesSinceFlush;

    private NdjsonLogger(BufferedWriter writer, Path path) {
        this.writer = writer;
        this.path = path;
    }

    public static NdjsonLogger open() {
        try {
            Path directory = Paths.get(System.getProperty("user.dir"), "logs", "spirecoach");
            Files.createDirectories(directory);
            String filename = "combat-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date()) + ".ndjson";
            Path path = directory.resolve(filename);
            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new NdjsonLogger(writer, path);
        } catch (IOException | SecurityException error) {
            System.err.println("[Spire Coach] Could not open combat log: " + error.getMessage());
            return new NdjsonLogger(null, null);
        }
    }

    public Path getPath() { return path; }

    public synchronized void combatStart(TurnSnapshot state) {
        write(JsonLog.combatStart(state));
    }

    public synchronized void turnStart(TurnSnapshot state) {
        write(JsonLog.turnStart(state));
    }

    public synchronized void turnEnd(TurnSnapshot state) {
        write(JsonLog.turnEnd(state));
    }

    public synchronized void action(ActionSnapshot action) {
        write(JsonLog.action(action));
    }

    public synchronized void combatEnd(CombatSnapshot combat, List<Lesson> lessons) {
        write(JsonLog.combatEnd(combat, lessons));
        flush();
    }

    private void write(String line) {
        if (writer == null) return;
        try {
            writer.write(line);
            writer.newLine();
            if (++linesSinceFlush >= 8) flush();
        } catch (IOException error) {
            System.err.println("[Spire Coach] Combat log write failed: " + error.getMessage());
        }
    }

    private void flush() {
        if (writer == null) return;
        try {
            writer.flush();
            linesSinceFlush = 0;
        } catch (IOException error) {
            System.err.println("[Spire Coach] Combat log flush failed: " + error.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        if (writer == null) return;
        try {
            flush();
            writer.close();
        } catch (IOException error) {
            System.err.println("[Spire Coach] Combat log close failed: " + error.getMessage());
        }
    }
}
