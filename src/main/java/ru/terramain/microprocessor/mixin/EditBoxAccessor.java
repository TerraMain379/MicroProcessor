package ru.terramain.microprocessor.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(EditBox.class)
public interface EditBoxAccessor {

    @Invoker("renderHighlight")
    void invokeDrawSelectionHighlight(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2);

    @Invoker("getMaxLength")
    int invokeGetMaxLength();

    @Invoker("deleteChars")
    void invokeErase(int characterOffset);

    @Invoker("getCursorPos")
    int invokeGetCursorPosWithOffset(int characterOffset);

    @Accessor("bordered")
    boolean getDrawsBackground();

    @Accessor("isEditable")
    boolean getEditable();

    @Accessor("canLoseFocus")
    boolean getFocusUnlocked();

    @Accessor("textColor")
    int getEditableColor();

    @Accessor("textColorUneditable")
    int getUneditableColor();

    @Accessor("highlightPos")
    int getSelectionStart();

    @Accessor("highlightPos")
    void setSelectionStart(int index);

    @Accessor("cursorPos")
    int getSelectionEnd();

    @Accessor("cursorPos")
    void setSelectionEnd(int index);

    @Accessor("displayPos")
    int getFirstCharacterIndex();

    @Accessor("focusedTime")
    long getLastSwitchFocusTime();

    @Accessor("focusedTime")
    void setLastSwitchFocusTime(long value);

    @Accessor("value")
    String getText();

    @Accessor("value")
    void setTextVariable(String text);

    @Accessor("suggestion")
    String getSuggestion();

    @Accessor("filter")
    Predicate<String> getTextPredicate();

    @Accessor("font")
    Font getTextRenderer();

    @Accessor("responder")
    Consumer<String> getChangedListener();

    @Accessor("formatter")
    BiFunction<String, Integer, FormattedCharSequence> getRenderTextProvider();
}
