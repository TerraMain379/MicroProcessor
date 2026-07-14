package ru.terramain.microprocessor.bcbui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Width, trimming and drawing for the script editor font.
 * Supports proportional and monospace ({@link MultilineEditorSettings#MONOSPACE}) layouts.
 */
public final class EditorFontMetrics {
    private final Font font;
    private final boolean monospace;
    private final boolean nativeMonospace;
    private final float cellScale;
    private final int cellWidth;
    private final int lineHeight;
    private final int lineStep;

    public EditorFontMetrics(Font font) {
        this.font = font;
        this.monospace = MultilineEditorSettings.MONOSPACE;
        this.nativeMonospace = monospace && MultilineEditorSettings.usesBundledFont();
        this.cellScale = nativeMonospace ? MultilineEditorSettings.MONO_CELL_SCALE : 1.0f;
        int configured = MultilineEditorSettings.MONO_CELL_WIDTH;
        this.cellWidth = configured > 0 ? configured : defaultCellWidth(font, nativeMonospace, cellScale);
        this.lineHeight = font.lineHeight;
        int spacing = MultilineEditorSettings.LINE_SPACING;
        int lineGap = MultilineEditorSettings.LINE_GAP;
        this.lineStep = spacing > 0 ? spacing : lineHeight + Math.max(1, lineGap);
    }

    private static int defaultCellWidth(Font font, boolean nativeMonospace, float cellScale) {
        int base = Math.max(font.width("0"), font.width("M"));
        if (!nativeMonospace) {
            return base;
        }
        return Math.max(1, Math.round(base * cellScale));
    }

    public Font font() {
        return font;
    }

    public int lineHeight() {
        return lineHeight;
    }

    public int lineStep() {
        return lineStep;
    }

    public int cellWidth() {
        return cellWidth;
    }

    public boolean isMonospace() {
        return monospace;
    }

    /** Pixel X offset of a caret before {@code charIndex} in {@code text}. */
    public int caretX(String text, int charIndex) {
        if (text.isEmpty() || charIndex <= 0) {
            return 0;
        }
        int index = Math.min(charIndex, text.length());
        if (!monospace || nativeMonospace) {
            int width = 0;
            for (int i = 0; i < index; i++) {
                width += glyphAdvance(text.charAt(i));
            }
            return width;
        }
        return index * cellWidth;
    }

    public int textWidth(String text) {
        return caretX(text, text.length());
    }

    public int textWidth(String text, int start, int end) {
        if (start >= end || text.isEmpty()) {
            return 0;
        }
        return caretX(text.substring(Math.max(0, start), Math.min(end, text.length())), end - start);
    }

    public int visibleCharCount(int pixelWidth) {
        if (pixelWidth <= 0) {
            return 0;
        }
        return caretIndexAtPixel(markerLine(pixelWidth), pixelWidth);
    }

    public String trimToPixelWidth(String text, int pixelWidth) {
        if (text.isEmpty() || pixelWidth <= 0) {
            return "";
        }
        int end = caretIndexAtPixel(text, pixelWidth);
        return text.substring(0, end);
    }

    public int columnAtPixel(int relativeX) {
        return caretIndexAtPixel(markerLine(relativeX), relativeX);
    }

    public int charIndexAtPixel(String visibleLine, int relativeX) {
        return caretIndexAtPixel(visibleLine, relativeX);
    }

    public int caretIndexAtPixel(String text, int relativeX) {
        if (relativeX <= 0 || text.isEmpty()) {
            return 0;
        }
        if (!monospace || nativeMonospace) {
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                int glyphWidth = glyphAdvance(text.charAt(i));
                if (relativeX < width + glyphWidth / 2) {
                    return i;
                }
                width += glyphWidth;
            }
            return text.length();
        }

        int column = relativeX / cellWidth;
        if (column >= text.length()) {
            return text.length();
        }
        int cellStart = column * cellWidth;
        return relativeX < cellStart + cellWidth / 2 ? column : column + 1;
    }

    public void drawString(GuiGraphics graphics, String text, int x, int y, int color) {
        if (text.isEmpty()) {
            return;
        }
        if (!monospace || nativeMonospace) {
            drawNativeString(graphics, text, x, y, color);
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            String glyph = String.valueOf(text.charAt(i));
            int cellX = x + i * cellWidth;
            int drawX = cellX + (cellWidth - font.width(glyph)) / 2;
            graphics.drawString(font, glyph, drawX, y, color);
        }
    }

    private void drawNativeString(GuiGraphics graphics, String text, int x, int y, int color) {
        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            String glyph = String.valueOf(text.charAt(i));
            graphics.drawString(font, glyph, cursorX, y, color);
            cursorX += glyphAdvance(text.charAt(i));
        }
    }

    private int glyphAdvance(char glyph) {
        return Math.max(1, Math.round(font.width(String.valueOf(glyph)) * cellScale));
    }

    private static String markerLine(int pixelWidth) {
        return "0".repeat(Math.max(16, pixelWidth + 8));
    }
}
