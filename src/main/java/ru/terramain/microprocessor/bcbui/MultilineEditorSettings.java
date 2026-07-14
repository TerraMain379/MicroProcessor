package ru.terramain.microprocessor.bcbui;

import net.minecraft.client.gui.Font;

/**
 * Script editor behaviour and presentation settings.
 */
public final class MultilineEditorSettings {
    public static int SCROLL_STEP_X = 4;
    public static int SCROLL_STEP_Y = 2;

    public static boolean MONOSPACE = true;
    /** 0 = auto from font width of "0"/"M". */
    public static int MONO_CELL_WIDTH = 0;
    /** 0 = auto from font line height + {@link #LINE_GAP}. */
    public static int LINE_SPACING = 0;
    /** Extra pixels between lines when {@link #LINE_SPACING} is 0. */
    public static int LINE_GAP = 2;

    public static boolean AUTO_INDENT = true;
    public static boolean AUTO_BRACKETS = true;

    /**
     * Bundled font id from {@code assets/microprocessor/font/<id>.json}.
     * Use {@code default} for the vanilla UI font.
     * Font size is configured only via the {@code size} field in that json.
     */
    public static String FONT = "jetbrains_mono";

    /**
     * Horizontal cell scale for monospace layout ({@code 1.0} = natural glyph width).
     */
    public static float MONO_CELL_SCALE = 1.0f;

    private MultilineEditorSettings() {
    }

    public static Font getFont() {
        return EditorFonts.getFont(FONT);
    }

    public static boolean usesBundledFont() {
        return EditorFonts.isActive(FONT);
    }
}
