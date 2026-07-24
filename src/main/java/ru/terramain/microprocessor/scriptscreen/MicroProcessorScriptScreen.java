package ru.terramain.microprocessor.scriptscreen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.terramain.microprocessor.bcbui.JsSyntaxHighlighter;
import ru.terramain.microprocessor.bcbui.MultiLineTextFieldWidget;
import ru.terramain.microprocessor.network.payload.scriptscreen.CloseScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.OpenScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.UpdateScriptScreenPayload;

import java.util.ArrayList;

public class MicroProcessorScriptScreen extends Screen {
    public static MicroProcessorScriptScreen instance = null;
    public static boolean openedLogs = false;

    private static final int MARGIN = 30;
    private static final int BOTTOM_BAR_Y_OFFSET = 35;
    private static final int BUTTON_WIDTH = 130;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COMPLETE_BUTTON_RIGHT_OFFSET = 160;
    private static final int LOGS_PANEL_GAP = 15;
    private static final int CLEAR_LOGS_BUTTON_SIZE = 16;
    private static final int CLEAR_LOGS_BUTTON_MARGIN = 4;

    public final BlockPos blockPos;
    private final UpdateScriptScreenPayload initialUpdate;

    public boolean preInit = true;
    public String code;
    public ArrayList<String> logs;
    public boolean isRunning;

    private Layout layout;
    private MultiLineTextFieldWidget codeEditor;
    private MultiLineTextFieldWidget logsField;
    private Button logsButton;
    private Button clearLogsButton;
    private Button runButton;
    private Button completeButton;

    public MicroProcessorScriptScreen(OpenScriptScreenPayload payload) {
        super(Component.literal("MicroProcessor Script Menu"));
        this.blockPos = payload.updateData().pos();
        this.initialUpdate = payload.updateData();
    }

    @Override
    protected void init() {
        if (this.codeEditor != null) {
            this.code = this.codeEditor.getValue();
        }
        this.layout = Layout.compute(this.width, this.height, this.openedLogs);
        this.buildWidgets();
        if (this.preInit) {
            this.handleUpdatePacket(this.initialUpdate);
            this.preInit = false;
        }
    }

    private void buildWidgets() {
        this.createCodeEditor();
        this.createLogsWidgets();
        this.createActionButtons();
        this.addClearLogsButtonOnTop();
        this.syncRunButtonState();
    }

    private void createCodeEditor() {
        this.codeEditor = new MultiLineTextFieldWidget(
                layout.editorX, layout.editorY, layout.editorWidth, layout.editorHeight,
                Component.empty());
        this.codeEditor.setMaxLength(20_000);
        this.codeEditor.setSyntaxHighlighter(JsSyntaxHighlighter.INSTANCE);
        this.codeEditor.setValue(this.code != null ? this.code : "");
        this.codeEditor.setOnChange(ignored -> this.sendChangeCode());
        this.addRenderableWidget(this.codeEditor);
    }

    private void createLogsWidgets() {
        this.logsField = new MultiLineTextFieldWidget(
                layout.logsX, layout.logsY, layout.logsWidth, layout.logsHeight,
                Component.empty());
        this.logsField.setMaxLength(10_000);
        this.logsField.setSyntaxHighlighter(JsSyntaxHighlighter.INSTANCE);
        this.logsField.setLines(this.logs);
        this.logsField.setReadOnly(true);

        this.clearLogsButton = Button.builder(Component.literal("×"), button -> this.handleClearLogsButton())
                .bounds(0, 0, CLEAR_LOGS_BUTTON_SIZE, CLEAR_LOGS_BUTTON_SIZE)
                .build();

        if (this.openedLogs) {
            this.addRenderableWidget(this.logsField);
        }
    }

    private void addClearLogsButtonOnTop() {
        if (!this.openedLogs || this.clearLogsButton == null) {
            return;
        }
        this.positionClearLogsButton();
        this.removeWidget(this.clearLogsButton);
        this.addRenderableWidget(this.clearLogsButton);
    }

    private void createActionButtons() {
        this.completeButton = Button.builder(Component.literal("Готово"), button -> this.complete())
                .bounds(layout.completeButtonX, layout.buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.logsButton = Button.builder(Component.literal("процесс"), button -> this.handleLogsButton())
                .bounds(layout.logsButtonX, layout.buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.runButton = Button.builder(Component.literal("Запуск"), button -> this.handleRunButton())
                .bounds(layout.runButtonX, layout.buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        this.addRenderableWidget(this.completeButton);
        this.addRenderableWidget(this.logsButton);
        this.addRenderableWidget(this.runButton);
    }

    private void syncRunButtonState() {
        this.updateRunningButton(this.isRunning);
    }

    private void positionClearLogsButton() {
        this.clearLogsButton.setX(layout.logsX + layout.logsWidth - CLEAR_LOGS_BUTTON_SIZE - CLEAR_LOGS_BUTTON_MARGIN);
        this.clearLogsButton.setY(layout.logsY + CLEAR_LOGS_BUTTON_MARGIN);
    }

    private void repositionCodeEditor() {
        this.codeEditor.setX(layout.editorX);
        this.codeEditor.setY(layout.editorY);
        this.codeEditor.setWidth(layout.editorWidth);
        this.codeEditor.setHeight(layout.editorHeight);
    }

    public void handleLogsButton() {
        if (this.openedLogs) {
            this.openedLogs = false;
            this.removeWidget(this.logsField);
            this.removeWidget(this.clearLogsButton);
        } else {
            this.openedLogs = true;
            this.addRenderableWidget(this.logsField);
        }

        this.layout = Layout.compute(this.width, this.height, this.openedLogs);
        if (this.openedLogs) {
            this.logsField.setX(layout.logsX);
            this.logsField.setY(layout.logsY);
            this.logsField.setWidth(layout.logsWidth);
            this.logsField.setHeight(layout.logsHeight);
            this.addClearLogsButtonOnTop();
        }
        this.repositionCodeEditor();
    }

    public void handleClearLogsButton() {
        if (this.logs == null) {
            this.logs = new ArrayList<>();
        }
        this.logs.clear();
        this.logsField.setLinesPreserveCursor(this.logs);
        PacketDistributor.sendToServer(new UpdateScriptScreenPayload(
                this.blockPos,
                false, "",
                false, true, new ArrayList<>(),
                false, false
        ));
    }

    public void handleRunButton() {
        this.updateRunningButton(!this.isRunning);
        this.sendChangeIsRunning();
    }

    public void sendChangeCode() {
        this.code = this.codeEditor.getValue();
        PacketDistributor.sendToServer(new UpdateScriptScreenPayload(
                this.blockPos,
                true, this.code,
                false, false, new ArrayList<>(),
                false, this.isRunning
        ));
    }

    public void sendChangeIsRunning() {
        PacketDistributor.sendToServer(new UpdateScriptScreenPayload(
                this.blockPos,
                false, "",
                false, false, new ArrayList<>(),
                true, this.isRunning
        ));
    }

    public void complete() {
        PacketDistributor.sendToServer(new CloseScriptScreenPayload(this.blockPos));
        this.onClose();
    }

    public void handleUpdatePacket(UpdateScriptScreenPayload payload) {
        if (payload.codeChanged()) {
            this.code = payload.code();
            this.codeEditor.setValuePreserveCursor(this.code);
        }

        if (payload.logsCleared()) {
            this.logs = new ArrayList<>();
        }
        if (payload.logsPushed()) {
            this.logs.addAll(payload.logs());
        }
        this.logsField.setLinesPreserveCursor(this.logs);

        if (payload.isRunningChanged()) this.updateRunningButton(payload.isRunning());
    }

    public void updateRunningButton(boolean newIsRunning) {
        this.isRunning = newIsRunning;
        if (this.runButton != null) {
            this.runButton.setMessage(Component.literal(newIsRunning ? "Остановка" : "Запуск"));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.openedLogs
                && this.clearLogsButton != null
                && this.clearLogsButton.isMouseOver(mouseX, mouseY)
                && this.clearLogsButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        this.complete();
        return true;
    }

    private static final class Layout {
        final int editorX;
        final int editorY;
        final int editorWidth;
        final int editorHeight;
        final int logsX;
        final int logsY;
        final int logsWidth;
        final int logsHeight;
        final int runButtonX;
        final int logsButtonX;
        final int completeButtonX;
        final int buttonY;

        private Layout(
                int editorX, int editorY, int editorWidth, int editorHeight,
                int logsX, int logsY, int logsWidth, int logsHeight,
                int runButtonX, int logsButtonX, int completeButtonX, int buttonY
        ) {
            this.editorX = editorX;
            this.editorY = editorY;
            this.editorWidth = editorWidth;
            this.editorHeight = editorHeight;
            this.logsX = logsX;
            this.logsY = logsY;
            this.logsWidth = logsWidth;
            this.logsHeight = logsHeight;
            this.runButtonX = runButtonX;
            this.logsButtonX = logsButtonX;
            this.completeButtonX = completeButtonX;
            this.buttonY = buttonY;
        }

        static Layout compute(int screenWidth, int screenHeight, boolean logsOpen) {
            int fullWidth = screenWidth - MARGIN * 2;
            int fullHeight = screenHeight - 90;
            int halfWidth = fullWidth / 2 - LOGS_PANEL_GAP;

            int logsX = MARGIN;
            int logsY = MARGIN;
            int logsWidth = halfWidth;
            int logsHeight = fullHeight;

            int editorX;
            int editorY = MARGIN;
            int editorWidth;
            int editorHeight = fullHeight;

            if (logsOpen) {
                editorX = MARGIN + fullWidth / 2 + LOGS_PANEL_GAP;
                editorWidth = halfWidth;
            } else {
                editorX = MARGIN;
                editorWidth = fullWidth;
            }

            int buttonY = screenHeight - BOTTOM_BAR_Y_OFFSET;
            int runButtonX = MARGIN;
            int completeButtonX = screenWidth - COMPLETE_BUTTON_RIGHT_OFFSET;
            int logsButtonX = (completeButtonX + runButtonX) / 2;

            return new Layout(
                    editorX, editorY, editorWidth, editorHeight,
                    logsX, logsY, logsWidth, logsHeight,
                    runButtonX, logsButtonX, completeButtonX, buttonY
            );
        }
    }
}
