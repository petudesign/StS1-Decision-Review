package spirecoach.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import spirecoach.domain.Lesson;

import java.util.ArrayList;
import java.util.List;

/** A deliberately small post-combat review panel. */
public final class CombatReviewOverlay {
    private final List<Lesson> lessons = new ArrayList<Lesson>();
    private String statusMessage = "";
    private int summaryCombats;
    private int summaryLessons;
    private int summaryRewards;
    private int summarySkipped;
    private int summaryCardsAdded;
    private boolean summaryWon;
    private boolean runSummary;
    private String noticeTitle = "";
    private boolean open;

    public void open(List<Lesson> newLessons) {
        open(newLessons, "");
    }

    public void open(List<Lesson> newLessons, String message) {
        lessons.clear();
        if (newLessons != null) lessons.addAll(newLessons);
        statusMessage = message == null ? "" : message;
        noticeTitle = "";
        runSummary = false;
        open = true;
    }

    public void openNotice(String title, String message) {
        lessons.clear();
        noticeTitle = title == null ? "COACH CHECK-IN" : title;
        statusMessage = message == null ? "" : message;
        runSummary = false;
        open = true;
    }

    public void openRunSummary(int combats, int lessonCount, int rewards,
                               int skipped, int cardsAdded, boolean won) {
        lessons.clear();
        summaryCombats = combats;
        summaryLessons = lessonCount;
        summaryRewards = rewards;
        summarySkipped = skipped;
        summaryCardsAdded = cardsAdded;
        summaryWon = won;
        statusMessage = won
                ? "The run is complete. Here is what Coach noticed."
                : "The run ended. Here is what Coach noticed before the defeat.";
        runSummary = true;
        open = true;
    }

    public void close() {
        lessons.clear();
        statusMessage = "";
        noticeTitle = "";
        runSummary = false;
        open = false;
    }

    public boolean isOpen() { return open; }

    public void update() {
        if (!open || !InputHelper.justClickedLeft) return;
        float x = panelX();
        float y = panelY();
        float w = panelWidth();
        float buttonY = y + 36.0f * Settings.scale;
        if (InputHelper.mX >= x + w - 230.0f * Settings.scale
                && InputHelper.mX <= x + w - 50.0f * Settings.scale
                && InputHelper.mY >= buttonY
                && InputHelper.mY <= buttonY + 58.0f * Settings.scale) {
            close();
            InputHelper.justClickedLeft = false;
        }
    }

    public void render(SpriteBatch sb) {
        if (!open) return;
        float x = panelX();
        float y = panelY();
        float w = panelWidth();
        float h = panelHeight();
        float s = Settings.scale;

        sb.setColor(new Color(0.035f, 0.045f, 0.065f, 0.97f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, w, h);
        sb.setColor(new Color(0.65f, 0.48f, 0.25f, 1.0f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y + h - 6.0f * s, w, 6.0f * s);
        sb.setColor(Color.WHITE);

        float cursor = y + h - 38.0f * s;
        String title = runSummary ? "RUN REVIEW"
                : (noticeTitle.isEmpty()
                ? (lessons.isEmpty() ? "COACH CHECK-IN" : "COMBAT REVIEW")
                : noticeTitle);
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, title, x + w / 2.0f, cursor,
                Color.WHITE);
        cursor -= 44.0f * s;

        if (!statusMessage.isEmpty()) {
            cursor = renderWrapped(sb, statusMessage, x + 42.0f * s, cursor,
                    w - 84.0f * s, Color.LIGHT_GRAY, 24.0f * s);
            cursor -= 12.0f * s;
        }

        if (runSummary) {
            cursor = renderLabelValue(sb, "Combats reviewed", String.valueOf(summaryCombats), x, cursor);
            cursor = renderLabelValue(sb, "Lessons found", String.valueOf(summaryLessons), x, cursor);
            cursor = renderLabelValue(sb, "Card rewards", String.valueOf(summaryRewards), x, cursor);
            cursor = renderLabelValue(sb, "Rewards skipped", String.valueOf(summarySkipped), x, cursor);
            cursor = renderLabelValue(sb, "Cards added", String.valueOf(summaryCardsAdded), x, cursor);
            cursor -= 8.0f * s;
            String closing = summaryLessons == 0
                    ? "No high-confidence lessons were found yet. That does not mean every decision was perfect; it means Coach is still deliberately conservative."
                    : "Coach only shows lessons when the evidence is strong enough to be useful.";
            renderWrapped(sb, closing, x + 42.0f * s, cursor,
                    w - 84.0f * s, new Color(0.88f, 0.76f, 0.48f, 1.0f), 24.0f * s);
        } else {
            for (Lesson lesson : lessons) {
                FontHelper.renderFontLeftTopAligned(sb, FontHelper.cardTitleFont,
                        lesson.getTitle(), x + 42.0f * s, cursor, Color.WHITE);
                cursor -= 34.0f * s;
                cursor = renderWrapped(sb, lesson.getExplanation(), x + 42.0f * s, cursor,
                        w - 84.0f * s, Color.LIGHT_GRAY, 24.0f * s);

                cursor -= 8.0f * s;
                cursor = renderLabelValue(sb, "Your sequence", join(lesson.getRelevantActions()), x, cursor);
                cursor = renderLabelValue(sb, "Damage", value(lesson, "actual_damage"), x, cursor);
                cursor = renderLabelValue(sb, "Alternative", join(lesson.getAlternativeActions()), x, cursor);
                cursor = renderLabelValue(sb, "Damage", value(lesson, "alternative_damage"), x, cursor);
                cursor -= 8.0f * s;
                cursor = renderWrapped(sb, "Lesson: " + lesson.getPrinciple(), x + 42.0f * s, cursor,
                        w - 84.0f * s, new Color(0.88f, 0.76f, 0.48f, 1.0f), 24.0f * s);
                cursor -= 34.0f * s;
            }
        }

        float buttonX = x + w - 230.0f * s;
        float buttonY = y + 36.0f * s;
        sb.setColor(new Color(0.16f, 0.38f, 0.50f, 1.0f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, buttonX, buttonY, 180.0f * s, 58.0f * s);
        sb.setColor(Color.WHITE);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, "Continue",
                buttonX + 90.0f * s, buttonY + 19.0f * s, Color.WHITE);
    }

    private float renderLabelValue(SpriteBatch sb, String label, String value, float x, float y) {
        float s = Settings.scale;
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipHeaderFont, label + ":",
                x + 42.0f * s, y, Color.GRAY);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, value,
                x + 240.0f * s, y, Color.WHITE);
        return y - 26.0f * s;
    }

    private float renderWrapped(SpriteBatch sb, String text, float x, float y, float width,
                                Color color, float lineHeight) {
        // A fixed character estimate keeps this overlay dependency-free and predictable.
        int maxChars = Math.max(20, (int) (width / (11.0f * Settings.scale)));
        String[] words = (text == null ? "" : text).split(" ");
        StringBuilder line = new StringBuilder();
        float cursor = y;
        for (String word : words) {
            if (line.length() > 0 && line.length() + word.length() + 1 > maxChars) {
                FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, line.toString(), x, cursor, color);
                cursor -= lineHeight;
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, line.toString(), x, cursor, color);
            cursor -= lineHeight;
        }
        return cursor;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) return "—";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) result.append(" → ");
            result.append(values.get(i));
        }
        return result.toString();
    }

    private static String value(Lesson lesson, String key) {
        Integer value = lesson.getValues().get(key);
        return value == null ? "—" : String.valueOf(value);
    }

    private static float panelX() { return Settings.WIDTH * 0.14f; }
    private static float panelY() { return Settings.HEIGHT * 0.12f; }
    private static float panelWidth() { return Settings.WIDTH * 0.72f; }
    private static float panelHeight() { return Settings.HEIGHT * 0.72f; }
}
