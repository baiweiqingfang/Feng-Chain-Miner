package cn.feng.minecraft.chainminer.fabric.client;

import cn.feng.minecraft.chainminer.fabric.ChainMinerConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class ChainMinerConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget maxBlocksField;
    private TextFieldWidget rulesField;
    private Text status = Text.empty();
    private int statusColor = 0xFFFFFF;

    public ChainMinerConfigScreen(Screen parent) {
        super(Text.translatable("screen.feng_chain_miner.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(520, width - 40);
        int left = (width - fieldWidth) / 2;

        maxBlocksField = new TextFieldWidget(
                textRenderer,
                left,
                72,
                110,
                20,
                Text.translatable("screen.feng_chain_miner.max_blocks")
        );
        maxBlocksField.setText(Integer.toString(ChainMinerConfig.getMaxBlocks()));
        maxBlocksField.setMaxLength(4);
        addDrawableChild(maxBlocksField);

        rulesField = new TextFieldWidget(
                textRenderer,
                left,
                126,
                fieldWidth,
                20,
                Text.translatable("screen.feng_chain_miner.blocks")
        );
        rulesField.setText(String.join(",", ChainMinerConfig.getRules()));
        rulesField.setMaxLength(8192);
        addDrawableChild(rulesField);

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
    }

    private void saveAndClose() {
        try {
            int maxBlocks = Integer.parseInt(maxBlocksField.getText().trim());
            if (maxBlocks < 1 || maxBlocks > 4096) {
                throw new IllegalArgumentException("range");
            }

            List<String> rules = Arrays.stream(rulesField.getText().split("[,，;；\\s]+"))
                    .filter(value -> !value.isBlank())
                    .toList();
            ChainMinerConfig.save(maxBlocks, rules);
            close();
        } catch (IllegalArgumentException exception) {
            status = Text.translatable("screen.feng_chain_miner.error_number");
            statusColor = 0xFF5555;
        } catch (IOException exception) {
            status = Text.translatable("screen.feng_chain_miner.error_save");
            statusColor = 0xFF5555;
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
                58,
                0xA0A0A0
        );
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.feng_chain_miner.blocks"),
                left,
                112,
                0xA0A0A0
        );
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.feng_chain_miner.help"),
                left,
                154,
                0x808080
        );
        context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 54, statusColor);

        super.render(context, mouseX, mouseY, delta);
    }
}

