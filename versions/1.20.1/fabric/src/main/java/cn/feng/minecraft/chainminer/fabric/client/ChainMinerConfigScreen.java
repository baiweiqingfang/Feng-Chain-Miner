package cn.feng.minecraft.chainminer.fabric.client;

import cn.feng.minecraft.chainminer.fabric.ChainMinerConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ChainMinerConfigScreen extends Screen {
    private final Screen parent;
    private final List<String> rules = new ArrayList<>();
    private TextFieldWidget maxBlocksField;
    private TextFieldWidget newRuleField;
    private ButtonWidget[] ruleButtons;
    private ButtonWidget previousPageButton;
    private ButtonWidget nextPageButton;
    private ButtonWidget deleteButton;
    private Text status = Text.empty();
    private int statusColor = 0xFFFFFF;
    private int selectedIndex = -1;
    private int pageStart;
    private int visibleRows;

    public ChainMinerConfigScreen(Screen parent) {
        super(Text.translatable("screen.feng_chain_miner.title"));
        this.parent = parent;
        rules.addAll(ChainMinerConfig.getRules());
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(520, width - 40);
        int left = (width - fieldWidth) / 2;

        maxBlocksField = new TextFieldWidget(
                textRenderer,
                left,
                58,
                110,
                20,
                Text.translatable("screen.feng_chain_miner.max_blocks")
        );
        maxBlocksField.setText(Integer.toString(ChainMinerConfig.getMaxBlocks()));
        maxBlocksField.setMaxLength(4);
        addDrawableChild(maxBlocksField);

        newRuleField = new TextFieldWidget(
                textRenderer,
                left,
                104,
                fieldWidth - 86,
                20,
                Text.translatable("screen.feng_chain_miner.new_rule")
        );
        newRuleField.setMaxLength(256);
        addDrawableChild(newRuleField);

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.add"),
                button -> addRule()
        ).dimensions(left + fieldWidth - 80, 104, 80, 20).build());

        visibleRows = Math.max(4, Math.min(12, (height - 236) / 22));
        ruleButtons = new ButtonWidget[visibleRows];
        int listTop = 148;
        for (int row = 0; row < visibleRows; row++) {
            final int rowIndex = row;
            ruleButtons[row] = addDrawableChild(ButtonWidget.builder(
                    Text.empty(),
                    button -> selectRow(rowIndex)
            ).dimensions(left, listTop + row * 22, fieldWidth, 20).build());
        }

        int controlsY = listTop + visibleRows * 22 + 2;
        previousPageButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.previous"),
                button -> changePage(-1)
        ).dimensions(left, controlsY, 80, 20).build());

        nextPageButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.next"),
                button -> changePage(1)
        ).dimensions(left + 84, controlsY, 80, 20).build());

        deleteButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.delete"),
                button -> deleteSelected()
        ).dimensions(left + fieldWidth - 164, controlsY, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.reset"),
                button -> resetDefaults()
        ).dimensions(left + fieldWidth - 80, controlsY, 80, 20).build());

        int buttonY = height - 34;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.save"),
                button -> saveAndClose()
        ).dimensions(width / 2 - 104, buttonY, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                button -> close()
        ).dimensions(width / 2 + 4, buttonY, 100, 20).build());

        setInitialFocus(maxBlocksField);
        refreshRuleButtons();
    }

    private void addRule() {
        String normalized = ChainMinerConfig.normalizeRule(newRuleField.getText());
        if (normalized == null) {
            showError("screen.feng_chain_miner.error_rule");
            return;
        }
        if (rules.contains(normalized)) {
            showError("screen.feng_chain_miner.error_duplicate");
            return;
        }

        rules.add(normalized);
        selectedIndex = rules.size() - 1;
        pageStart = (selectedIndex / visibleRows) * visibleRows;
        newRuleField.setText("");
        status = Text.empty();
        refreshRuleButtons();
    }

    private void selectRow(int row) {
        int index = pageStart + row;
        if (index < rules.size()) {
            selectedIndex = index;
            status = Text.empty();
            refreshRuleButtons();
        }
    }

    private void deleteSelected() {
        if (selectedIndex < 0 || selectedIndex >= rules.size()) {
            showError("screen.feng_chain_miner.error_select");
            return;
        }

        rules.remove(selectedIndex);
        if (rules.isEmpty()) {
            selectedIndex = -1;
            pageStart = 0;
        } else {
            selectedIndex = Math.min(selectedIndex, rules.size() - 1);
            pageStart = Math.min(pageStart, ((rules.size() - 1) / visibleRows) * visibleRows);
        }
        status = Text.empty();
        refreshRuleButtons();
    }

    private void resetDefaults() {
        rules.clear();
        rules.addAll(ChainMinerConfig.getDefaultRules());
        selectedIndex = rules.isEmpty() ? -1 : 0;
        pageStart = 0;
        status = Text.translatable("screen.feng_chain_miner.defaults_restored");
        statusColor = 0x55FF55;
        refreshRuleButtons();
    }

    private void changePage(int direction) {
        pageStart += direction * visibleRows;
        int lastPage = rules.isEmpty() ? 0 : ((rules.size() - 1) / visibleRows) * visibleRows;
        pageStart = Math.max(0, Math.min(lastPage, pageStart));
        refreshRuleButtons();
    }

    private void refreshRuleButtons() {
        if (ruleButtons == null) {
            return;
        }
        for (int row = 0; row < ruleButtons.length; row++) {
            int index = pageStart + row;
            ButtonWidget button = ruleButtons[row];
            button.visible = index < rules.size();
            button.active = button.visible;
            if (button.visible) {
                String prefix = index == selectedIndex ? "▶ " : "  ";
                button.setMessage(Text.literal(prefix + rules.get(index)));
            }
        }

        previousPageButton.active = pageStart > 0;
        nextPageButton.active = pageStart + visibleRows < rules.size();
        deleteButton.active = selectedIndex >= 0 && selectedIndex < rules.size();
    }

    private void showError(String translationKey) {
        status = Text.translatable(translationKey);
        statusColor = 0xFF5555;
    }

    private void saveAndClose() {
        try {
            int maxBlocks = Integer.parseInt(maxBlocksField.getText().trim());
            if (maxBlocks < 1 || maxBlocks > 4096) {
                throw new IllegalArgumentException("range");
            }

            ChainMinerConfig.save(maxBlocks, rules);
            close();
        } catch (IllegalArgumentException exception) {
            showError("screen.feng_chain_miner.error_number");
        } catch (IOException exception) {
            showError("screen.feng_chain_miner.error_save");
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 24, 0xFFFFFF);

        int fieldWidth = Math.min(520, width - 40);
        int left = (width - fieldWidth) / 2;
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.feng_chain_miner.max_blocks"),
                left,
                44,
                0xA0A0A0
        );
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.feng_chain_miner.new_rule"),
                left,
                90,
                0xA0A0A0
        );
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.feng_chain_miner.rule_list", rules.size()),
                left,
                134,
                0xA0A0A0
        );
        context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 54, statusColor);

        super.render(context, mouseX, mouseY, delta);
    }
}

