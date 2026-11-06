package cn.feng.minecraft.chainminer.fabric.client;

import cn.feng.minecraft.chainminer.fabric.ChainMinerConfig;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ChainMinerConfigScreen extends Screen {
    private static final int CELL_SIZE = 22;

    private final Screen parent;
    private final List<BlockOption> allBlocks = new ArrayList<>();
    private final Set<Identifier> selectedBlocks = new HashSet<>();
    private List<BlockOption> filteredBlocks = List.of();

    private TextFieldWidget maxBlocksField;
    private TextFieldWidget searchField;
    private ButtonWidget previousPageButton;
    private ButtonWidget nextPageButton;
    private Text status = Text.empty();
    private int statusColor = 0xFFFFFF;
    private int page;
    private int columns;
    private int rows;
    private int gridLeft;
    private int gridTop;
    private int controlsY;
    private int statusY;
    private int saveY;

    public ChainMinerConfigScreen(Screen parent) {
        super(Text.translatable("screen.feng_chain_miner.title"));
        this.parent = parent;

        for (Block block : Registries.BLOCK) {
            ItemStack stack = new ItemStack(block);
            if (block.asItem() != Items.AIR && !stack.isEmpty()) {
                allBlocks.add(new BlockOption(Registries.BLOCK.getId(block), block, stack));
            }
        }
        allBlocks.sort(Comparator.comparing(option -> option.id().toString()));
        selectRules(ChainMinerConfig.getRules());
        filteredBlocks = List.copyOf(allBlocks);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(520, width - 32);
        int left = (width - contentWidth) / 2;

        maxBlocksField = new TextFieldWidget(
                textRenderer,
                left,
                34,
                86,
                20,
                Text.translatable("screen.feng_chain_miner.max_blocks")
        );
        maxBlocksField.setText(Integer.toString(ChainMinerConfig.getMaxBlocks()));
        maxBlocksField.setMaxLength(4);
        addDrawableChild(maxBlocksField);

        searchField = new TextFieldWidget(
                textRenderer,
                left + 94,
                34,
                contentWidth - 94,
                20,
                Text.translatable("screen.feng_chain_miner.search")
        );
        searchField.setChangedListener(this::filterBlocks);
        addDrawableChild(searchField);

        saveY = height - 24;
        statusY = saveY - 14;
        controlsY = statusY - 22;
        gridTop = 64;
        columns = Math.max(1, contentWidth / CELL_SIZE);
        rows = Math.max(2, (controlsY - gridTop - 4) / CELL_SIZE);
        gridLeft = (width - columns * CELL_SIZE) / 2;

        previousPageButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.previous"),
                button -> changePage(-1)
        ).dimensions(left, controlsY, 70, 18).build());

        nextPageButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.next"),
                button -> changePage(1)
        ).dimensions(left + 74, controlsY, 70, 18).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.clear"),
                button -> clearSelection()
        ).dimensions(left + contentWidth - 154, controlsY, 70, 18).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.reset"),
                button -> resetDefaults()
        ).dimensions(left + contentWidth - 80, controlsY, 80, 18).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.feng_chain_miner.save"),
                button -> saveAndClose()
        ).dimensions(width / 2 - 104, saveY, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                button -> close()
        ).dimensions(width / 2 + 4, saveY, 100, 20).build());

        setInitialFocus(searchField);
        refreshPageButtons();
    }

    private void filterBlocks(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            filteredBlocks = List.copyOf(allBlocks);
        } else {
            filteredBlocks = allBlocks.stream()
                    .filter(option -> option.id().toString().contains(normalized)
                            || option.stack().getName().getString().toLowerCase(Locale.ROOT).contains(normalized))
                    .toList();
        }
        page = 0;
        status = Text.empty();
        refreshPageButtons();
    }

    private void changePage(int direction) {
        page = Math.max(0, Math.min(getPageCount() - 1, page + direction));
        status = Text.empty();
        refreshPageButtons();
    }

    private void clearSelection() {
        selectedBlocks.clear();
        status = Text.translatable("screen.feng_chain_miner.selection_cleared");
        statusColor = 0xFFFF55;
    }

    private void resetDefaults() {
        selectRules(ChainMinerConfig.getDefaultRules());
        status = Text.translatable("screen.feng_chain_miner.defaults_restored");
        statusColor = 0x55FF55;
    }

    private void selectRules(List<String> rules) {
        selectedBlocks.clear();
        for (BlockOption option : allBlocks) {
            for (String rawRule : rules) {
                if (matchesRule(option, rawRule)) {
                    selectedBlocks.add(option.id());
                    break;
                }
            }
        }
    }

    private boolean matchesRule(BlockOption option, String rawRule) {
        if (rawRule == null || rawRule.isBlank()) {
            return false;
        }
        boolean isTag = rawRule.startsWith("#");
        Identifier id = Identifier.tryParse(isTag ? rawRule.substring(1) : rawRule);
        if (id == null) {
            return false;
        }
        if (!isTag) {
            return option.id().equals(id);
        }
        TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, id);
        return option.block().getDefaultState().isIn(tag);
    }

    private int getPageSize() {
        return Math.max(1, columns * rows);
    }

    private int getPageCount() {
        return Math.max(1, (filteredBlocks.size() + getPageSize() - 1) / getPageSize());
    }

    private void refreshPageButtons() {
        if (previousPageButton == null || nextPageButton == null) {
            return;
        }
        previousPageButton.active = page > 0;
        nextPageButton.active = page + 1 < getPageCount();
    }

    private void saveAndClose() {
        try {
            int maxBlocks = Integer.parseInt(maxBlocksField.getText().trim());
            if (maxBlocks < 1 || maxBlocks > 4096) {
                throw new IllegalArgumentException("range");
            }

            List<String> rules = selectedBlocks.stream()
                    .map(Identifier::toString)
                    .sorted()
                    .toList();
            ChainMinerConfig.save(maxBlocks, rules);
            close();
        } catch (IllegalArgumentException exception) {
            showError("screen.feng_chain_miner.error_number");
        } catch (IOException exception) {
            showError("screen.feng_chain_miner.error_save");
        }
    }

    private void showError(String translationKey) {
        status = Text.translatable(translationKey);
        statusColor = 0xFF5555;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || mouseX < gridLeft || mouseY < gridTop) {
            return false;
        }

        int column = (int) ((mouseX - gridLeft) / CELL_SIZE);
        int row = (int) ((mouseY - gridTop) / CELL_SIZE);
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            return false;
        }

        int index = page * getPageSize() + row * columns + column;
        if (index >= filteredBlocks.size()) {
            return false;
        }

        Identifier id = filteredBlocks.get(index).id();
        if (!selectedBlocks.add(id)) {
            selectedBlocks.remove(id);
        }
        status = Text.empty();
        return true;
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
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);

        int contentWidth = Math.min(520, width - 32);
        int left = (width - contentWidth) / 2;
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.feng_chain_miner.max_blocks"),
                left,
                22,
                0xA0A0A0
        );
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.feng_chain_miner.search"),
                left + 94,
                22,
                0xA0A0A0
        );

        BlockOption hovered = renderBlockGrid(context, mouseX, mouseY);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable(
                        "screen.feng_chain_miner.page_status",
                        page + 1,
                        getPageCount(),
                        selectedBlocks.size()
                ),
                width / 2,
                controlsY + 5,
                0xFFFFFF
        );
        context.drawCenteredTextWithShadow(textRenderer, status, width / 2, statusY + 2, statusColor);

        super.render(context, mouseX, mouseY, delta);
        if (hovered != null) {
            context.drawTooltip(textRenderer, hovered.stack().getName(), mouseX, mouseY);
        }
    }

    private BlockOption renderBlockGrid(DrawContext context, int mouseX, int mouseY) {
        BlockOption hovered = null;
        int start = page * getPageSize();
        int end = Math.min(filteredBlocks.size(), start + getPageSize());

        for (int index = start; index < end; index++) {
            int local = index - start;
            int column = local % columns;
            int row = local / columns;
            int x = gridLeft + column * CELL_SIZE;
            int y = gridTop + row * CELL_SIZE;
            BlockOption option = filteredBlocks.get(index);
            boolean selected = selectedBlocks.contains(option.id());
            boolean isHovered = mouseX >= x && mouseX < x + CELL_SIZE
                    && mouseY >= y && mouseY < y + CELL_SIZE;

            int background = selected ? 0xCC2E7D32 : (isHovered ? 0xCC555555 : 0xAA111111);
            int border = selected ? 0xFF55FF55 : 0xFF888888;
            context.fill(x, y, x + CELL_SIZE - 2, y + CELL_SIZE - 2, background);
            context.fill(x, y, x + CELL_SIZE - 2, y + 1, border);
            context.fill(x, y + CELL_SIZE - 3, x + CELL_SIZE - 2, y + CELL_SIZE - 2, border);
            context.fill(x, y, x + 1, y + CELL_SIZE - 2, border);
            context.fill(x + CELL_SIZE - 3, y, x + CELL_SIZE - 2, y + CELL_SIZE - 2, border);
            context.drawItem(option.stack(), x + 2, y + 2);

            if (isHovered) {
                hovered = option;
            }
        }
        return hovered;
    }

    private record BlockOption(Identifier id, Block block, ItemStack stack) {
    }
}

