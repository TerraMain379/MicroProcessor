package ru.terramain.microprocessor.bcbui;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Style;

import java.util.List;

public interface SyntaxHighlighter {
    List<Pair<Style, Integer>> getColorsPairs(String text);
}
