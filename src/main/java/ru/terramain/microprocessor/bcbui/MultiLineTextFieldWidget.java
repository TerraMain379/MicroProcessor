package ru.terramain.microprocessor.bcbui;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringUtil;
import net.minecraft.Util;
import ru.terramain.microprocessor.mixin.EditBoxAccessor;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class MultiLineTextFieldWidget extends EditBox {
    private ScrollbarWidget scrollX, scrollY;
    private List<String> lines; // all lines
    private List<Integer> linesStartsOffsets; // lines offsets in base 1d text
    private List<Pair<Style, Integer>> colorsPairs; // swap color offsets
    private int visibleLines = 11; // viewport vertical visible lines number
    private int visibleFirstLineIndex = 0; // first line in viewport
    private int horizontalCharOffset = 0; // horizontal viewport first char offset // TODO: migrate from char to pixels for no-monospace text
    private int maxLineWidth = 0; // max line width in all text
    private Pair<Integer, Integer> cursorPosPreference;
    private boolean LShiftPressed, RShiftPressed = false;
    private boolean textModified = false;
    private SyntaxHighlighter syntaxHighlighter;
    private EditBoxAccessor accessor = (EditBoxAccessor) this;
    private float timeSinceClick = 0.0f;
    private boolean readOnly = false;

    public MultiLineTextFieldWidget(int x, int y, int width, int height, Component text) {
        super(MultilineEditorSettings.getFont(), x, y, width, height, text);
        this.lines = new LinkedList<>();
        this.linesStartsOffsets = new LinkedList<>();
        this.lines.add("");
        this.linesStartsOffsets.add(0);
        this.visibleLines = (height - 4) / lineStep();
        this.visibleFirstLineIndex = 0;
        this.scrollX = new TextFieldScrollbarWidget(x, y + height + 1, width, 10, Component.literal(""), this, true);
        this.scrollY = new TextFieldScrollbarWidget(x + width + 1, y, 10, height, Component.literal(""), this, false);
        cursorPosPreference = Pair.of(0, 0);
        updateScrollLayout();
    }

    private EditorFontMetrics metrics() {
        return new EditorFontMetrics(MultilineEditorSettings.getFont());
    }

    private int lineStep() {
        return metrics().lineStep();
    }

    private int visibleCharCount() {
        return Math.max(metrics().visibleCharCount(getInnerWidth()), 1);
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
        updateScrollLayout();
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
        updateScrollLayout();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateScrollLayout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateScrollLayout();
    }

    /**
     * Syncs scrollbar geometry with the editor bounds.
     */
    public void updateScrollLayout() {
        int x = getX();
        int y = getY();

        scrollX.setX(x);
        scrollX.setY(y + height + 1);
        scrollX.setWidth(width);

        scrollY.setX(x + width + 1);
        scrollY.setY(y);
        scrollY.setHeight(height);

        refreshScrollMetrics();
    }

    /**
     * Recomputes visible area, scroll ranges and scrollbar thumb sizes/positions.
     * Call after changing width/height if setters were bypassed.
     */
    public void refreshScrollMetrics() {
        visibleLines = Math.max((height - 4) / lineStep(), 1);
        visibleFirstLineIndex = clamp(visibleFirstLineIndex, 0, Math.max(lines.size() - visibleLines, 0));
        horizontalCharOffset = clamp(horizontalCharOffset, 0, maxHorizontalScrollOffset());

        scrollY.refreshTrackMetrics();
        scrollX.refreshTrackMetrics();
        scrollY.setScale(lines.size() / (double) visibleLines);
        scrollX.setScale((double) maxLineWidth / visibleCharCount());
        syncScrollbarPositions();
    }

    private void syncScrollbarPositions() {
        int scrollableLines = scrollableLineCount();
        scrollY.updatePos((double) visibleFirstLineIndex / scrollableLines);

        int maxHScroll = maxHorizontalScrollOffset();
        scrollX.updatePos((double) horizontalCharOffset / maxHScroll);
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta){
        //TODO: render line at x of cursor if currently at parenthesis
        timeSinceClick += delta / 20.0f;
        int color;
        if (!this.isVisible()) {
            return;
        }
        if (accessor.getDrawsBackground()) {
            color = this.isFocused() ? -1 : -6250336;
            context.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, color);
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -16777216);
        }

        color = accessor.getEditable() ? accessor.getEditableColor() : accessor.getUneditableColor();
        EditorFontMetrics metrics = metrics();

        if (!lines.isEmpty()) {
            for (int i = visibleFirstLineIndex; i < visibleFirstLineIndex + visibleLines && i < lines.size(); i++) {
                String line = lines.get(i);
                if (horizontalCharOffset < line.length()) {
                    line = line.substring(horizontalCharOffset);
                    line = metrics.trimToPixelWidth(line, getInnerWidth());
                    int y = this.getY() + lineStep() * (i - visibleFirstLineIndex) + 5;
                    if (usesColoredText()) {
                        this.drawColoredLine(context, metrics, line, this.getX() + 5, y, i);
                    } else {
                        metrics.drawString(context, line, this.getX() + 5, y, color);
                    }
                }
            }
        }

        scrollX.renderWidget(context, mouseX, mouseY, delta);
        scrollY.renderWidget(context, mouseX, mouseY, delta);

        int anchor = accessor.getSelectionStart();
        int cursor = accessor.getSelectionEnd();
        int selectionStart = Math.min(anchor, cursor);
        int selectionEnd = Math.max(anchor, cursor);

        boolean selectingBackwards = anchor > cursor;

        Pair<Integer, Integer> start = indexToLineAndOffset(selectionStart);
        Pair<Integer, Integer> end = indexToLineAndOffset(selectionEnd);

        int firstSelectedLine = start.getFirst();
        int selectionStartOffset = start.getSecond();
        int lastSelectedLine = end.getFirst();
        int selectionEndOffset = end.getSecond();

        selectionStartOffset -= horizontalCharOffset;
        selectionEndOffset -= horizontalCharOffset;

        selectionStartOffset = Math.max(selectionStartOffset, 0);
        selectionEndOffset = Math.max(selectionEndOffset, 0);

        boolean renderVerticalCursor = !readOnly && selectionStart < accessor.getText().length();
        boolean verticalCursorVisible = !readOnly && this.isFocused() && (Util.getMillis() - accessor.getLastSwitchFocusTime()) / 300L % 2L == 0L;

        context.pose().translate(0.0, 0.0, 0.1);

        for (int i = firstSelectedLine; i <= lastSelectedLine; i++) {
            if (i < visibleFirstLineIndex || i >= visibleFirstLineIndex + visibleLines) {
                continue;
            }
            int x1 = this.getX() + 5;
            int x2 = x1;
            int y = this.getY() + lineStep() * (i - visibleFirstLineIndex) + 5;

            String visibleLine = lines.get(i).substring(Math.min(horizontalCharOffset, lines.get(i).length()));
            if (i == firstSelectedLine) {
                x1 += metrics.textWidth(visibleLine.substring(0, Math.min(selectionStartOffset, visibleLine.length())));

                if (verticalCursorVisible && !selectingBackwards) {
                    if (renderVerticalCursor) {
                        context.fill(x1, y - 1, x1 + 1, y + 1 + metrics.lineHeight(), -3092272);
                    } else {
                        metrics.drawString(context, "_", x1, y, -3092272);
                    }
                }
            }

            if (i == lastSelectedLine) {
                x2 += metrics.textWidth(visibleLine.substring(0, Math.min(selectionEndOffset, visibleLine.length())));

                if (verticalCursorVisible && renderVerticalCursor && selectingBackwards) {
                    context.fill(x2, y - 1, x2 + 1, y + 1 + metrics.lineHeight(), -3092272);
                }

            } else {
                x2 += this.getInnerWidth();
            }
            accessor.invokeDrawSelectionHighlight(context, x1, y, x2, y + lineStep());
        }
    }

    private void insertNewLine() {
        if (readOnly) {
            return;
        }
        int i = Math.min(accessor.getSelectionStart(), accessor.getSelectionEnd());
        int j = Math.max(accessor.getSelectionStart(), accessor.getSelectionEnd());
        String text = accessor.getText();

        String indent = "";
        String commentSuffix = "";
        if (MultilineEditorSettings.AUTO_INDENT) {
            int lineStart = text.lastIndexOf('\n', i - 1) + 1;
            int lineEnd = text.indexOf('\n', i);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            String line = text.substring(lineStart, lineEnd);
            indent = leadingWhitespace(line);
            int commentIdx = line.indexOf("//");
            if (commentIdx >= 0 && i - lineStart >= commentIdx) {
                commentSuffix = "// ";
            }
        }

        String insertion = "\n" + indent + commentSuffix;
        if (text.length() - (j - i) + insertion.length() > accessor.invokeGetMaxLength()) {
            return;
        }
        String newText = new StringBuilder(text).replace(i, j, insertion).toString();
        if (!accessor.getTextPredicate().test(newText)) {
            return;
        }
        accessor.setTextVariable(newText);
        int cursor = i + insertion.length();
        this.moveCursorTo(cursor, false);
        this.setHighlightPos(cursor);
        this.onTextChanged(accessor.getText(), true);
        this.updateScrollPositions();
        textModified = true;
    }

    private static String leadingWhitespace(String line) {
        int end = 0;
        while (end < line.length()) {
            char c = line.charAt(end);
            if (c != ' ' && c != '\t') {
                break;
            }
            end++;
        }
        return line.substring(0, end);
    }

    private void drawColoredLine(GuiGraphics context, EditorFontMetrics metrics, String content, int x, int y, int lineIndex) {
        int renderOffset = 0;
        int startOffset = linesStartsOffsets.get(lineIndex);
        int currentOffset = 0;
        if (colorsPairs.size() > 1) {
            for (int i = 0; i < colorsPairs.size(); i++) {
                if (currentOffset >= content.length()) {
                    break;
                }
                int nextColorStart = (i + 1) < colorsPairs.size() ? colorsPairs.get(i + 1).getSecond() : (startOffset + content.length());
                nextColorStart -= startOffset;
                nextColorStart -= horizontalCharOffset;
                if (nextColorStart > currentOffset) {
                    String substring = content.substring(currentOffset, clamp(nextColorStart, currentOffset, content.length()));

                    int color;
                    try {
                        color = colorsPairs.get(i).getFirst().getColor().getValue();
                    } catch (IndexOutOfBoundsException e) {
                        color = TextColor.fromLegacyFormat(ChatFormatting.GRAY).getValue();
                    }

                    metrics.drawString(context, substring, x + renderOffset, y, color);
                    currentOffset += substring.length();
                    renderOffset += metrics.textWidth(substring);
                }
                if (currentOffset > content.length()) {
                    break;
                }
            }
        } else {
            int color;
            try {
                color = colorsPairs.get(0).getFirst().getColor().getValue();
            } catch (IndexOutOfBoundsException e) {
                color = TextColor.fromLegacyFormat(ChatFormatting.GRAY).getValue();
            }
            metrics.drawString(context, content, x, y, color);
        }
    }

    private int pointToIndex(double x, double y) {
        if (!lines.isEmpty()) {
            EditorFontMetrics metrics = metrics();
            int lineIndex = (int) Math.floor((y - (this.getY() + 5)) / lineStep()) + visibleFirstLineIndex;
            lineIndex = Math.max(Math.min(lineIndex, lines.size() - 1), 0);
            String line = lines.get(lineIndex);
            int offset = 0;
            if (!line.isEmpty()) {
                String visibleLine = line.substring(Math.min(horizontalCharOffset, line.length()));
                int relX = (int) (x - (this.getX() + 5));
                int chars = metrics.charIndexAtPixel(visibleLine, relX);
                offset = Math.min(horizontalCharOffset + chars, line.length());
            }

            return linesStartsOffsets.get(lineIndex) + Math.max(offset, 0);
        }
        return 0;
    }

    private Pair<Integer, Integer> indexToLineAndOffset(int index) {
        for (int i = 0; i < lines.size(); i++) {
            if (linesStartsOffsets.get(i) + lines.get(i).length() + 1 > index) {
                return Pair.of(i, index - linesStartsOffsets.get(i));
            }
        }
        if (!lines.isEmpty()) {
            int last = lines.size() - 1;
            return Pair.of(last, lines.get(last).length());
        }
        return Pair.of(0, 0);
    }

    private int lineOffsetEnd(String line, int offsetInLine) {
        return Math.min(Math.max(offsetInLine, 0), line.length());
    }

    private int scrollableLineCount() {
        return Math.max(lines.size() - visibleLines, 1);
    }

    private int maxHorizontalScrollOffset() {
        return Math.max(maxLineWidth - visibleCharCount(), 0);
    }

    public Pair<Integer, Integer> getCharacterPos(int index) {
        Pair<Integer, Integer> output = indexToLineAndOffset(index);
        EditorFontMetrics metrics = metrics();
        int x, y;
        try {
            String line = lines.get(output.getFirst());
            int from = Math.min(horizontalCharOffset, line.length());
            int to = lineOffsetEnd(line, output.getSecond());
            x = this.getX() + 5 + metrics.textWidth(line, from, Math.max(from, to));
        } catch (Exception e) {
            x = this.getX() + 5;
        }
        y = this.getY() + 5 + lineStep() * (output.getFirst() - visibleFirstLineIndex);
        return Pair.of(x, y);
    }

    public void setSyntaxHighlighter(SyntaxHighlighter highlighter) {
        this.syntaxHighlighter = highlighter;
        this.onChanged(accessor.getText(), true);
    }

    private boolean usesColoredText() {
        return syntaxHighlighter != null;
    }

    @Override
    public void insertText(String text) {
        if (readOnly) {
            return;
        }
        String string2;
        String string;
        int l;
        int i = Math.min(accessor.getSelectionStart(), accessor.getSelectionEnd());
        int j = Math.max(accessor.getSelectionStart(), accessor.getSelectionEnd());
        int k = accessor.invokeGetMaxLength() - accessor.getText().length() - (i - j);
        if (k < (l = (string = StringUtil.filterText(text, true)).length())) {
            string = string.substring(0, k);
            l = k;
        }
        if (!accessor.getTextPredicate().test(string2 = new StringBuilder(accessor.getText()).replace(i, j, string).toString())) {
            return;
        }
        accessor.setTextVariable(string2);
        this.moveCursorTo(i + l, false);
        this.setHighlightPos(this.getCursorPosition());
        this.onTextChanged(accessor.getText(), true);
        this.updateScrollPositions();
        textModified = true;
    }

    private void erase(int offset) {
        if (readOnly) {
            return;
        }
        if (Screen.hasControlDown()) {
            this.deleteWords(offset);
        } else {
            this.deleteChars(offset);
        }
        this.onTextChanged(accessor.getText(), true);
        this.updateScrollPositions();
        textModified = true;
    }

    private boolean isAlphanumeric(char a){
        return Character.isLetter(a) || Character.isDigit(a) || a == '_';
    }

    private int getWordLength(int offsetDir){
        /*
         * If traversing letters or numbers, keep going until reaching a non-alphanumeric character.
         * Otherwise, only traverse chunks of the same character
         */
        int startIndex = accessor.getSelectionStart() + (offsetDir < 0 ? -1 : 0);
        String text = getValue();
        if(text.isEmpty() || startIndex < 0 || startIndex >= text.length()) return 0;
        char current = text.charAt(startIndex);
        char startChar = current;
        //System.out.println("Starting at: " + startChar + ", wordOffset = " + offsetDir);
        boolean erasingAlphanumeric = isAlphanumeric(startChar);
        int endIndex = startIndex;
        do{
            endIndex += offsetDir;
            if (endIndex < 0 || endIndex >= text.length()) break;
            current = text.charAt(endIndex);
        } while (
                (erasingAlphanumeric && isAlphanumeric(current))
                        || (!erasingAlphanumeric && current == startChar)
        );
        endIndex -= offsetDir;
        int min = Math.min(startIndex, endIndex);
        int max = Math.max(startIndex, endIndex);
        //System.out.println("Word: " + text.substring(min, max + 1));
        return ((max-min) + 1) * offsetDir;
    }

    private void selectWord(){
        int forward = getWordLength(1);
        int backward = getWordLength(-1);
        accessor.setSelectionStart(accessor.getSelectionStart()+forward);
        accessor.setSelectionEnd((accessor.getSelectionStart()-forward)+backward);
    }

    @Override
    public void deleteWords(int wordOffset) {
        if (readOnly) {
            return;
        }
        if (accessor.getText().isEmpty()) {
            return;
        }
        if (accessor.getSelectionEnd() != accessor.getSelectionStart()) {
            this.insertText("");
            return;
        }
        deleteChars(getWordLength(wordOffset));
    }

    @Override
    public void deleteChars(int characterOffset) {
        if (readOnly) {
            return;
        }
        int k;
        if (accessor.getText().isEmpty()) {
            return;
        }
        if (accessor.getSelectionEnd() != accessor.getSelectionStart()) {
            this.insertText("");
            return;
        }
        int i = accessor.invokeGetCursorPosWithOffset(characterOffset);
        int j = Math.min(i, accessor.getSelectionStart());
        if (j == (k = Math.max(i, accessor.getSelectionStart()))) {
            return;
        }
        String string = new StringBuilder(accessor.getText()).delete(j, k).toString();
        if (!accessor.getTextPredicate().test(string)) {
            return;
        }
        accessor.setTextVariable(string);
        this.moveCursorTo(j, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == 340){
            LShiftPressed = true;
        }
        if(keyCode == 344){
            RShiftPressed = true;
        }

        if (!this.isFocused()) {
            return false;
        }

        if ((keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_A && Screen.hasControlDown())) {
            this.moveCursorToEnd(Screen.hasShiftDown());
            this.setHighlightPos(0);
            return true;
        }
        if ((keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_C && Screen.hasControlDown())) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
            return true;
        }
        if ((keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_V && Screen.hasControlDown())) {
            if (accessor.getEditable()) {
                this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
            }
            return true;
        }
        if ((keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_X && Screen.hasControlDown())) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
            if (accessor.getEditable()) {
                this.insertText("");
            }
            return true;
        }
        switch (keyCode) {
            case 263: {
                if (Screen.hasControlDown()) {
                    //this.setCursorPos(this.getWordSkipPosition(-1), Screen.hasShiftDown());
                    this.setCursor(getCursorPosition() + getWordLength(-1), Screen.hasShiftDown());
                    updateScrollPositions();
                } else {
                    this.moveCursor(-1, Screen.hasShiftDown());
                }
                return true;
            }
            case 264:{//DOWN
                this.moveCursorVertical(1);
                return true;
            }
            case 265:{//UP
                this.moveCursorVertical(-1);
                return true;
            }
            case 262: {
                if (Screen.hasControlDown()) {
                    //this.setCursorPos(this.getWordSkipPosition(1), Screen.hasShiftDown());
                    this.setCursor(getCursorPosition() + getWordLength(1), Screen.hasShiftDown());
                    updateScrollPositions();
                } else {
                    this.moveCursor(1, Screen.hasShiftDown());
                }
                return true;
            }
            case 259: {
                if (accessor.getEditable()) {
                    this.erase(-1);
                }
                return true;
            }
            case 261: {
                if (accessor.getEditable()) {
                    this.erase(1);
                }
                return true;
            }
            case 268: {
                this.moveCursorToStart(Screen.hasShiftDown());
                updateScrollPositions();
                return true;
            }
            case 269: {
                this.moveCursorToEnd(Screen.hasShiftDown());
                updateScrollPositions();
                return true;
            }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER:
            case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER: {
                if (accessor.getEditable()) {
                    this.insertNewLine();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(keyCode == 340){
            LShiftPressed = false;
        }
        if(keyCode == 344){
            RShiftPressed = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!this.isFocused()) {
            return false;
        }
        if (!isAllowedEditorCharacter(chr)) {
            return false;
        }
        if (!accessor.getEditable()) {
            return false;
        }

        Character closing = bracketPair(chr);
        if (MultilineEditorSettings.AUTO_BRACKETS && closing != null) {
            this.insertText(chr + String.valueOf(closing));
            this.moveCursorTo(this.getCursorPosition() - 1, false);
            textModified = true;
            return true;
        }

        this.insertText(Character.toString(chr));
        textModified = true;
        return true;
    }

    private static boolean isAllowedEditorCharacter(char chr) {
        return StringUtil.isAllowedChatCharacter(chr) || chr == '`' || chr == '<' || chr == '>';
    }

    private static Character bracketPair(char open) {
        return switch (open) {
            case '(' -> ')';
            case '{' -> '}';
            case '[' -> ']';
            case '<' -> '>';
            case '"' -> '"';
            case '\'' -> '\'';
            case '`' -> '`';
            default -> null;
        };
    }

    @Override
    public void setValue(String text) {
        if (!accessor.getTextPredicate().test(text)) {
            return;
        }
        accessor.setTextVariable(text.length() > accessor.invokeGetMaxLength() ? text.substring(0, accessor.invokeGetMaxLength()) : text);
        this.moveCursorToEnd(Screen.hasShiftDown());
        this.onTextChanged(text, true);
    }

    /**
     * Replaces all text without moving the caret or notifying {@link #setOnChange(Consumer)}.
     */
    public void setValuePreserveCursor(String text) {
        applyText(text, true, false, false);
    }

    /**
     * Replaces all text without moving the caret.
     */
    public void setValuePreserveCursor(String text, boolean notifyChange) {
        applyText(text, true, notifyChange, false);
    }

    public void setLines(List<String> sourceLines) {
        applyLines(sourceLines, true, true, true);
    }

    public void setLinesPreserveCursor(List<String> sourceLines) {
        applyLines(sourceLines, true, false, false);
    }

    public void setLinesPreserveCursor(List<String> sourceLines, boolean notifyChange) {
        applyLines(sourceLines, true, notifyChange, false);
    }

    /**
     * Appends text and scrolls to the bottom. Useful for log views.
     */
    public void appendText(String text) {
        appendText(text, true);
    }

    public void appendText(String text, boolean scrollToEnd) {
        appendText(text, scrollToEnd, false);
    }

    public void appendText(String text, boolean scrollToEnd, boolean notifyChange) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String combined = accessor.getText() + text;
        applyText(combined, true, notifyChange, false);
        if (scrollToEnd) {
            this.scrollToEnd();
        }
    }

    private void applyText(String text, boolean rehighlight, boolean notifyChange, boolean moveCursorToEnd) {
        if (!accessor.getTextPredicate().test(text)) {
            return;
        }
        if (text.length() > accessor.invokeGetMaxLength()) {
            text = text.substring(0, accessor.invokeGetMaxLength());
        }

        int selectionStart = accessor.getSelectionStart();
        int selectionEnd = accessor.getSelectionEnd();
        accessor.setTextVariable(text);

        if (moveCursorToEnd) {
            this.moveCursorToEnd(false);
        } else {
            int length = text.length();
            accessor.setSelectionEnd(clamp(selectionEnd, 0, length));
            accessor.setSelectionStart(clamp(selectionStart, 0, length));
        }

        if (notifyChange) {
            this.onTextChanged(text, rehighlight);
        } else {
            this.onChanged(text, rehighlight);
        }
    }

    private void applyLines(List<String> sourceLines, boolean rehighlight, boolean notifyChange, boolean moveCursorToEnd) {
        List<String> normalized = truncateLines(normalizeLines(sourceLines), accessor.invokeGetMaxLength());
        applyText(joinLines(normalized), rehighlight, notifyChange, moveCursorToEnd);
    }

    private static List<String> normalizeLines(List<String> sourceLines) {
        if (sourceLines == null || sourceLines.isEmpty()) {
            return new LinkedList<>(List.of(""));
        }
        LinkedList<String> normalized = new LinkedList<>();
        for (String line : sourceLines) {
            normalized.add(line == null ? "" : line);
        }
        return normalized;
    }

    private static String joinLines(List<String> sourceLines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sourceLines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(sourceLines.get(i));
        }
        return builder.toString();
    }

    private static List<String> truncateLines(List<String> sourceLines, int maxLength) {
        LinkedList<String> truncated = new LinkedList<>();
        int remaining = maxLength;
        for (int i = 0; i < sourceLines.size(); i++) {
            if (remaining <= 0) {
                break;
            }
            String line = sourceLines.get(i);
            boolean hasNext = i < sourceLines.size() - 1;
            int allowedForLine = hasNext ? Math.min(line.length(), Math.max(remaining - 1, 0)) : Math.min(line.length(), remaining);
            if (allowedForLine <= 0 && (!hasNext || remaining <= 0)) {
                break;
            }
            truncated.add(line.substring(0, allowedForLine));
            remaining -= allowedForLine;
            if (hasNext && remaining > 0) {
                remaining -= 1;
            }
        }
        if (truncated.isEmpty()) {
            truncated.add("");
        }
        return truncated;
    }

    public void setOnChange(Consumer<String> listener) {
        this.setResponder(listener);
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        this.setEditable(!readOnly);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void scrollToEnd() {
        visibleFirstLineIndex = Math.max(0, lines.size() - visibleLines);
        syncScrollbarPositions();
    }

    public void scrollToTop() {
        visibleFirstLineIndex = 0;
        syncScrollbarPositions();
    }

    public void setRawText(String text) {
        this.setValue(text);
    }

    public void refreshFormatting(){
        setRawText(getValue());
    }

    private void onTextChanged(String newText, boolean rehighlight) {
        onChanged(newText, rehighlight, true);
    }

    private void onChanged(String newText, boolean rehighlight) {
        onChanged(newText, rehighlight, false);
    }

    private void onChanged(String newText, boolean rehighlight, boolean notifyChange) {
        if (notifyChange) {
            Consumer<String> listener = accessor.getChangedListener();
            if (listener != null) {
                listener.accept(newText);
            }
        }
        this.setUnformattedText(newText);
        if (syntaxHighlighter != null) {
            colorsPairs = new LinkedList<>(syntaxHighlighter.getColorsPairs(newText));
        }
        refreshScrollMetrics();
    }

    private void setUnformattedText(String text) {
        colorsPairs = new LinkedList<>();
        colorsPairs.add(Pair.of(Style.EMPTY.withColor(ChatFormatting.GRAY), 0));

        lines = new LinkedList<>();
        linesStartsOffsets = new LinkedList<>();

        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                lines.add(text.substring(start, i));
                linesStartsOffsets.add(start);
                start = i + 1;
            }
        }
        if (lines.isEmpty()) {
            lines.add("");
            linesStartsOffsets.add(0);
        }

        maxLineWidth = 0;
        for (String line : lines) {
            maxLineWidth = Math.max(line.length(), maxLineWidth);
        }
    }

    private boolean getHovered(double mouseX, double mouseY) {
        return mouseX >= (double) this.getX() && mouseX < (double)(this.getX() + this.width) && mouseY >= (double) this.getY() && mouseY < (double)(this.getY() + this.height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if (!this.isVisible()) {
            return false;
        }

        if (scrollX.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (scrollY.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        boolean hovered = getHovered(mouseX, mouseY);
        if (accessor.getFocusUnlocked()) {
            this.setFocused(hovered);
        }
        if(this.isFocused() && hovered && button == 0) {
            int previousIndex = accessor.getSelectionStart();
            this.setCursor(pointToIndex(mouseX, mouseY), Screen.hasShiftDown());
            cursorPosPreference = Pair.of((int)mouseX, (int)mouseY);
            if (!readOnly && timeSinceClick < 0.5f && previousIndex == accessor.getSelectionStart()) {
                selectWord();
            }
            timeSinceClick = 0.0f;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount){
        if (!this.isVisible()) {
            return false;
        }
        if (LShiftPressed || RShiftPressed) {
            horizontalCharOffset = clamp(horizontalCharOffset -(int)verticalAmount*MultilineEditorSettings.SCROLL_STEP_X, 0, maxHorizontalScrollOffset());
            syncScrollbarPositions();
        } else {
            visibleFirstLineIndex = clamp(visibleFirstLineIndex -(int)verticalAmount*MultilineEditorSettings.SCROLL_STEP_Y, 0, Math.max(lines.size()-visibleLines, 0));
            syncScrollbarPositions();
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!this.isVisible()) {
            return;
        }
        scrollX.mouseMoved(mouseX, mouseY);
        scrollY.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY){
        if (!this.isVisible()) {
            return false;
        }
        scrollX.onDrag(mouseX, mouseY, deltaX, deltaY);
        scrollY.onDrag(mouseX, mouseY, deltaX, deltaY);

        if (this.isHovered() && this.isFocused()) {
            accessor.setSelectionStart(pointToIndex(mouseX, mouseY));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button){
        if (!this.isVisible()) {
            return false;
        }
        scrollX.mouseReleased(mouseX, mouseY, button);
        scrollY.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    private void moveCursorVertical(int delta) {
        Pair<Integer, Integer> lineAndOffset = indexToLineAndOffset(accessor.invokeGetCursorPosWithOffset(0));
        int yPreference = getY() + 5 + (lineAndOffset.getFirst() - visibleFirstLineIndex) * lineStep();
        cursorPosPreference = Pair.of(cursorPosPreference.getFirst(), yPreference + delta * lineStep());
        int index = pointToIndex(cursorPosPreference.getFirst(), cursorPosPreference.getSecond());
        setCursor(index, Screen.hasShiftDown());

        updateScrollPositions();
    }

    @Override
    public void moveCursor(int offset, boolean shiftKeyPressed) {
        super.moveCursor(offset, shiftKeyPressed);
        if (lines.isEmpty()) {
            return;
        }
        EditorFontMetrics metrics = metrics();
        Pair<Integer, Integer> lineAndOffset = indexToLineAndOffset(getCursorPosition());
        int lineIndex = Math.min(lineAndOffset.getFirst(), lines.size() - 1);
        String line = lines.get(lineIndex);
        int xPreference = this.getX() + 5 + metrics.textWidth(line.substring(0, lineOffsetEnd(line, lineAndOffset.getSecond())));
        cursorPosPreference = Pair.of(xPreference, this.getY() + 5 + lineStep() * (lineIndex - visibleFirstLineIndex));
        updateScrollPositions();
    }

    private void setCursor(int cursor, boolean shiftKeyPressed) {
        this.moveCursorTo(cursor, shiftKeyPressed);
        this.onChanged(accessor.getText(), false);
    }

    private void updateScrollPositions() {
        EditorFontMetrics metrics = metrics();
        Pair<Integer, Integer> lineAndOffset = indexToLineAndOffset(accessor.invokeGetCursorPosWithOffset(0));
        if (lines.isEmpty()) {
            horizontalCharOffset = 0;
            visibleFirstLineIndex = 0;
            syncScrollbarPositions();
            return;
        }
        String line = lines.get(lineAndOffset.getFirst());
        int cursorOffset = lineOffsetEnd(line, lineAndOffset.getSecond());
        int visibleCols = visibleCharCount();

        if (cursorOffset < horizontalCharOffset) {
            horizontalCharOffset = cursorOffset;
        } else if (cursorOffset - horizontalCharOffset >= visibleCols) {
            horizontalCharOffset = cursorOffset - visibleCols + 1;
        }

        int maxHScroll = maxHorizontalScrollOffset();
        horizontalCharOffset = clamp(horizontalCharOffset, 0, maxHScroll);
        syncScrollbarPositions();

        int lineIndex = lineAndOffset.getFirst();
        int scrollableLines = scrollableLineCount();
        if (lineIndex < visibleFirstLineIndex) {
            visibleFirstLineIndex = clamp(visibleFirstLineIndex - (visibleFirstLineIndex - lineIndex), 0, Math.max(lines.size() - visibleLines, 0));
            syncScrollbarPositions();
        } else if (lineIndex >= visibleFirstLineIndex + visibleLines) {
            visibleFirstLineIndex = clamp(visibleFirstLineIndex + 1 + ((lineIndex - visibleFirstLineIndex) - visibleLines), 0, Math.max(lines.size() - visibleLines, 0));
            syncScrollbarPositions();
        }
    }

    public void setScroll(double value) {
        this.visibleFirstLineIndex = (int) Math.max(Math.round((lines.size() - visibleLines) * value), 0);
    }

    public void setHorizontalOffset(double value) {
        int maxOffset = maxHorizontalScrollOffset();
        this.horizontalCharOffset = (int) Math.floor(maxOffset * value);
    }

    @Override
    public void setEditable(boolean value) {
        super.setEditable(value);
    }

    public boolean wasModified(){
        return textModified;
    }

    public void resetModified(){
        textModified = false;
    }

    private int clamp(int i, int min, int max) {
        return Math.max(Math.min(i, max), min);
    }

    private double clamp(double i, double min, double max) {
        return Math.max(Math.min(i, max), min);
    }
}
