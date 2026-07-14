package ru.terramain.microprocessor.bcbui;

import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;

public final class MultilineEditorResources {
    private static final String NS = "bettercommandblockui";

    public static final WidgetSprites SCROLLBAR_HORIZONTAL = new WidgetSprites(
            rl("scrollbar_horizontal_enabled"),
            rl("scrollbar_horizontal_disabled"),
            rl("scrollbar_horizontal_focused")
    );
    public static final WidgetSprites SCROLLBAR_VERTICAL = new WidgetSprites(
            rl("scrollbar_vertical_enabled"),
            rl("scrollbar_vertical_disabled"),
            rl("scrollbar_vertical_focused")
    );

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }

    private MultilineEditorResources() {
    }
}
