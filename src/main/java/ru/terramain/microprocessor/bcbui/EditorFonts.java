package ru.terramain.microprocessor.bcbui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import ru.terramain.microprocessor.mixin.FontManagerAccessor;
import ru.terramain.microprocessor.mixin.MinecraftAccessor;

import java.util.Objects;

/**
 * Resolves bundled editor fonts from {@code assets/microprocessor/font/<id>.json}.
 */
public final class EditorFonts {
    public static final ResourceLocation JETBRAINS_MONO = ResourceLocation.fromNamespaceAndPath("microprocessor", "jetbrains_mono");

    private static String cachedKey;
    private static Font cachedFont;

    private EditorFonts() {
    }

    public static void invalidate() {
        cachedKey = null;
        cachedFont = null;
    }

    public static ResourceLocation parseFontId(String fontSetting) {
        if (fontSetting == null || fontSetting.isBlank()) {
            return null;
        }
        String normalized = fontSetting.trim();
        if ("default".equalsIgnoreCase(normalized) || "minecraft:default".equalsIgnoreCase(normalized)) {
            return null;
        }
        if (normalized.contains(":")) {
            return ResourceLocation.parse(normalized);
        }
        return ResourceLocation.fromNamespaceAndPath("microprocessor", normalized);
    }

    public static boolean isActive(String fontSetting) {
        ResourceLocation fontId = parseFontId(fontSetting);
        if (fontId == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && accessor(minecraft).microprocessor$getFontSets().containsKey(fontId);
    }

    public static Font getFont(String fontSetting) {
        String key = fontSetting == null ? "" : fontSetting.trim();
        if (cachedFont != null && Objects.equals(cachedKey, key)) {
            return cachedFont;
        }

        Font resolved = resolveFont(key);
        if (isActive(key)) {
            cachedKey = key;
            cachedFont = resolved;
        }
        return resolved;
    }

    private static Font resolveFont(String fontSetting) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            throw new IllegalStateException("Minecraft instance is not available");
        }

        ResourceLocation fontId = parseFontId(fontSetting);
        FontManagerAccessor fontManager = accessor(minecraft);
        if (fontId == null || !fontManager.microprocessor$getFontSets().containsKey(fontId)) {
            return minecraft.font;
        }

        FontSet fontSet = fontManager.microprocessor$getFontSetCached(fontId);
        return new Font(id -> {
            if (Style.DEFAULT_FONT.equals(id) || fontId.equals(id)) {
                return fontSet;
            }
            return fontManager.microprocessor$getFontSetCached(id);
        }, false);
    }

    private static FontManagerAccessor accessor(Minecraft minecraft) {
        FontManager fontManager = ((MinecraftAccessor) minecraft).microprocessor$getFontManager();
        return (FontManagerAccessor) fontManager;
    }
}
