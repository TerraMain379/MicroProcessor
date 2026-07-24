package ru.terramain.microprocessor.bcbui;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Heuristic JavaScript syntax coloring (VS Code–style, no semantic analysis).
 */
public final class JsSyntaxHighlighter implements SyntaxHighlighter {
    public static final JsSyntaxHighlighter INSTANCE = new JsSyntaxHighlighter();

    private static final Style DEFAULT = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style KEYWORD = Style.EMPTY.withColor(ChatFormatting.GOLD);
    private static final Style DECLARATION = Style.EMPTY.withColor(ChatFormatting.WHITE);
    private static final Style VARIABLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style FUNCTION = Style.EMPTY.withColor(ChatFormatting.YELLOW);
    private static final Style PROPERTY = Style.EMPTY.withColor(ChatFormatting.AQUA);
    private static final Style CONSTANT = Style.EMPTY.withColor(ChatFormatting.RED);
    private static final Style TYPE = Style.EMPTY.withColor(ChatFormatting.AQUA);
    private static final Style BUILTIN = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
    private static final Style LITERAL = Style.EMPTY.withColor(ChatFormatting.GOLD);
    private static final Style STRING = Style.EMPTY.withColor(ChatFormatting.GREEN);
    private static final Style COMMENT = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    private static final Style NUMBER = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);

    private static final Set<String> KEYWORDS = new LinkedHashSet<>(Set.of(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete",
            "do", "else", "export", "extends", "finally", "for", "function", "if", "import", "in",
            "instanceof", "let", "new", "return", "super", "switch", "throw", "try", "typeof", "var",
            "void", "while", "with", "yield", "async", "await", "of", "static", "enum", "implements",
            "interface", "package", "private", "protected", "public"
    ));

    private static final Set<String> LITERALS = Set.of("true", "false", "null", "undefined", "NaN", "Infinity");

    private static final Set<String> BINDING_PREFIXES = Set.of(
            "const", "let", "var", "function", "class", "catch", "import", "default"
    );

    private static final Set<String> BUILTINS = new LinkedHashSet<>(Set.of(
            "console", "Math", "JSON", "Object", "Array", "String", "Number", "Boolean", "BigInt",
            "Symbol", "Date", "RegExp", "Map", "Set", "WeakMap", "WeakSet", "Promise", "Proxy",
            "Reflect", "Intl", "Error", "TypeError", "ReferenceError", "SyntaxError", "RangeError",
            "parseInt", "parseFloat", "isNaN", "isFinite", "decodeURI", "encodeURI", "decodeURIComponent",
            "encodeURIComponent", "setTimeout", "setInterval", "clearTimeout", "clearInterval",
            "globalThis", "eval", "ArrayBuffer", "DataView", "Float32Array", "Float64Array",
            "Int8Array", "Int16Array", "Int32Array", "Uint8Array", "Uint16Array", "Uint32Array"
    ));

    private JsSyntaxHighlighter() {
    }

    @Override public List<Pair<Style, Integer>> getColorsPairs(String text) {
        List<Pair<Style, Integer>> spans = new ArrayList<>();
        pushSpan(spans, DEFAULT, 0);

        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);

            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                pushSpan(spans, COMMENT, i);
                i += 2;
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
                pushSpan(spans, DEFAULT, i);
                continue;
            }

            if (c == '"' || c == '\'' || c == '`') {
                char quote = c;
                pushSpan(spans, STRING, i);
                i++;
                while (i < text.length()) {
                    if (text.charAt(i) == '\\' && i + 1 < text.length()) {
                        i += 2;
                        continue;
                    }
                    if (text.charAt(i) == quote) {
                        i++;
                        break;
                    }
                    i++;
                }
                pushSpan(spans, DEFAULT, i);
                continue;
            }

            if (Character.isDigit(c) || (c == '.' && i + 1 < text.length() && Character.isDigit(text.charAt(i + 1)))) {
                pushSpan(spans, NUMBER, i);
                i++;
                while (i < text.length()) {
                    char d = text.charAt(i);
                    if (Character.isDigit(d) || d == '.' || d == 'e' || d == 'E'
                            || ((d == '+' || d == '-') && i > 0 && (text.charAt(i - 1) == 'e' || text.charAt(i - 1) == 'E'))) {
                        i++;
                    } else {
                        break;
                    }
                }
                pushSpan(spans, DEFAULT, i);
                continue;
            }

            if (isIdentStart(c)) {
                int start = i;
                i++;
                while (i < text.length() && isIdentPart(text.charAt(i))) {
                    i++;
                }
                String word = text.substring(start, i);
                pushSpan(spans, classifyIdentifier(text, start, i, word), start);
                pushSpan(spans, DEFAULT, i);
                continue;
            }

            i++;
        }

        return spans;
    }

    private static Style classifyIdentifier(String text, int start, int end, String word) {
        if (KEYWORDS.contains(word)) {
            return KEYWORD;
        }
        if (LITERALS.contains(word)) {
            return LITERAL;
        }

        boolean afterDot = previousNonWhitespace(text, start - 1) == '.';
        boolean beforeCall = nextNonWhitespace(text, end) == '(';

        if (afterDot) {
            return beforeCall ? FUNCTION : PROPERTY;
        }
        if (beforeCall) {
            return FUNCTION;
        }

        if (isConstantName(word)) {
            return CONSTANT;
        }
        if (BUILTINS.contains(word)) {
            return BUILTIN;
        }
        if (isTypeName(word)) {
            return TYPE;
        }
        if (isBindingPosition(text, start)) {
            return DECLARATION;
        }
        return VARIABLE;
    }

    /** SCREAMING_SNAKE_CASE — likely a constant. */
    private static boolean isConstantName(String word) {
        if (word.length() < 2) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c != '_' && !Character.isUpperCase(c) && !Character.isDigit(c)) {
                return false;
            }
        }
        return Character.isUpperCase(word.charAt(0));
    }

    /** PascalCase — likely a class / constructor name. */
    private static boolean isTypeName(String word) {
        if (word.length() < 2 || !Character.isUpperCase(word.charAt(0))) {
            return false;
        }
        return !isConstantName(word);
    }

    private static boolean isBindingPosition(String text, int identStart) {
        String prev = previousWord(text, identStart);
        if (prev != null && BINDING_PREFIXES.contains(prev)) {
            return true;
        }
        char before = previousNonWhitespace(text, identStart - 1);
        return before == '{' || before == ',';
    }

    private static String previousWord(String text, int fromIndex) {
        int p = fromIndex - 1;
        while (p >= 0 && isWhitespace(text.charAt(p))) {
            p--;
        }
        if (p < 0 || !isIdentPart(text.charAt(p))) {
            return null;
        }
        int end = p;
        while (p >= 0 && isIdentPart(text.charAt(p))) {
            p--;
        }
        return text.substring(p + 1, end + 1);
    }

    private static char previousNonWhitespace(String text, int index) {
        int p = index;
        while (p >= 0 && isWhitespace(text.charAt(p))) {
            p--;
        }
        return p >= 0 ? text.charAt(p) : '\0';
    }

    private static char nextNonWhitespace(String text, int index) {
        int p = index;
        while (p < text.length() && isWhitespace(text.charAt(p))) {
            p++;
        }
        return p < text.length() ? text.charAt(p) : '\0';
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static void pushSpan(List<Pair<Style, Integer>> spans, Style style, int index) {
        if (!spans.isEmpty()) {
            Pair<Style, Integer> last = spans.get(spans.size() - 1);
            if (last.getSecond().equals(index)) {
                spans.set(spans.size() - 1, Pair.of(style, index));
                return;
            }
        }
        spans.add(Pair.of(style, index));
    }
}
