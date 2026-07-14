package ru.terramain.microprocessor.mixin;

import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(FontManager.class)
public interface FontManagerAccessor {
    @Accessor("fontSets")
    Map<ResourceLocation, FontSet> microprocessor$getFontSets();

    @Invoker("getFontSetCached")
    FontSet microprocessor$getFontSetCached(ResourceLocation id);
}
