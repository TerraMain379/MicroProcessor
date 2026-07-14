package ru.terramain.microprocessor.bcbui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static ru.terramain.microprocessor.bcbui.MultilineEditorResources.SCROLLBAR_HORIZONTAL;
import static ru.terramain.microprocessor.bcbui.MultilineEditorResources.SCROLLBAR_VERTICAL;

public class ScrollbarWidget extends AbstractWidget {
    protected boolean dragging = false;
    protected final boolean horizontal;
    protected double prevMouseX;
    protected double prevMouseY;
    protected double pos;
    protected double scale = 1.0d;
    protected int length;
    protected int barLength;
    protected int frameRepeatLength;
    protected int barRepeatLength;
    protected final int textureLength = 256;
    protected java.util.function.Consumer<Double> changedListener;

    public ScrollbarWidget(int x, int y, int width, int height, Component message, boolean horizontal) {
        super(x, y, width, height, message);
        this.horizontal = horizontal;
        this.length = horizontal ? width : height;
        this.barLength = length;
        this.frameRepeatLength = length - textureLength;
        this.barRepeatLength = barLength - textureLength;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) {
            return;
        }
        isHovered = checkHovered(mouseX, mouseY);
        renderFrame(graphics);
        renderSlider(graphics, mouseX, mouseY, delta);
    }

    protected void renderFrame(GuiGraphics graphics) {
        renderLongBox(graphics, false, false, 0, horizontal ? width : height, frameRepeatLength);
    }

    protected void renderSlider(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderLongBox(graphics, true, isHovered, (int) (pos * (length - barLength)), barLength, barRepeatLength);
    }

    protected void renderLongBox(GuiGraphics graphics, boolean enabled, boolean hovered, int position, int boxLength, int repeatLength) {
        int capSize = textureLength / 2;
        if (horizontal) {
            ResourceLocation textures = SCROLLBAR_HORIZONTAL.get(enabled, hovered);
            int capWidth = Math.min(boxLength / 2, capSize);
            graphics.blitSprite(textures, textureLength, 10, 0, 0, getX() + position, getY(), capWidth, height);
            graphics.blitSprite(
                    textures, textureLength, 10,
                    Math.max(capSize, textureLength - boxLength / 2), 0,
                    getX() + position + boxLength - capWidth, getY(),
                    capWidth, height
            );
            if (repeatLength > 0) {
                int drawX = getX() + position + capSize;
                for (int drawn = 0; drawn < repeatLength; drawn += capSize) {
                    int segmentWidth = Math.min(repeatLength - drawn, capSize);
                    graphics.blitSprite(textures, textureLength, 10, textureLength / 4, 0, drawX, getY(), segmentWidth, height);
                    drawX += segmentWidth;
                }
            }
        } else {
            ResourceLocation textures = SCROLLBAR_VERTICAL.get(enabled, hovered);
            int capHeight = Math.min(boxLength / 2, capSize);
            graphics.blitSprite(textures, 10, textureLength, 0, 0, getX(), getY() + position, width, capHeight);
            graphics.blitSprite(
                    textures, 10, textureLength,
                    0, Math.max(capSize, textureLength - boxLength / 2),
                    getX(), getY() + position + boxLength - capHeight,
                    width, capHeight
            );
            if (repeatLength > 0) {
                int drawY = getY() + position + capSize;
                for (int drawn = 0; drawn < repeatLength; drawn += capSize) {
                    int segmentHeight = Math.min(repeatLength - drawn, capSize);
                    graphics.blitSprite(textures, 10, textureLength, 0, textureLength / 4, getX(), drawY, width, segmentHeight);
                    drawY += segmentHeight;
                }
            }
        }
    }

    public void setChangedListener(java.util.function.Consumer<Double> changedListener) {
        this.changedListener = changedListener;
    }

    protected boolean checkHovered(double mouseX, double mouseY) {
        if (horizontal) {
            return mouseX >= getX() + pos * (length - barLength) && mouseY >= getY() && mouseX < getX() + pos * (length - barLength) + barLength && mouseY < getY() + height;
        }
        return mouseX >= getX() && mouseY >= getY() + pos * (length - barLength) && mouseX < getX() + width && mouseY < getY() + pos * (length - barLength) + barLength;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isValidClickButton(button) && checkHovered(mouseX, mouseY)) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onClick(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isValidClickButton(button)) {
            onRelease(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!visible) {
            return;
        }
        dragging = true;
        prevMouseX = mouseX;
        prevMouseY = mouseY;
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        dragging = false;
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double distX, double distY) {
        if (!dragging) {
            return;
        }
        int track = length - barLength;
        if (track <= 0) {
            return;
        }
        double posBefore = pos;
        if (horizontal) {
            pos = Math.min(Math.max(pos + distX / track, 0), 1);
        } else {
            pos = Math.min(Math.max(pos + distY / track, 0), 1);
        }
        if (changedListener != null && Math.abs(posBefore - pos) > 0.0d) {
            changedListener.accept(pos);
        }
    }

    public void setScale(double newScale) {
        scale = Math.max(newScale, 1);
        barLength = (int) (length / Math.min(scale, 8));
        barLength = Math.min(Math.max(barLength, 20), length);
        barRepeatLength = Math.max(barLength - textureLength, 0);
    }

    public void refreshTrackMetrics() {
        length = horizontal ? width : height;
        frameRepeatLength = Math.max(length - textureLength, 0);
        setScale(scale);
        updatePos(pos);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (horizontal) {
            refreshTrackMetrics();
        }
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        if (!horizontal) {
            refreshTrackMetrics();
        }
    }

    public void updatePos(double newPos) {
        pos = Math.max(Math.min(newPos, 1), 0);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
    }
}
