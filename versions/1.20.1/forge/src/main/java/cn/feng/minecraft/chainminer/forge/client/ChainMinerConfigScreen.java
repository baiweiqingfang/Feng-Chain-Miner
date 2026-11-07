package cn.feng.minecraft.chainminer.forge.client;

import cn.feng.minecraft.chainminer.forge.ChainMinerConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

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
    private final Set<ResourceLocation> selectedBlocks = new HashSet<>();
    private final List<Button> categoryButtons = new ArrayList<>();
    private List<BlockOption> filteredBlocks = List.of();

    private EditBox maxBlocksField;
    private EditBox searchField;
    private Button previousPageButton;
    private Button nextPageButton;
    private BlockCategory category = BlockCategory.ALL;
    private Component status = Component.empty();
    private int statusColor = 0xFFFFFF;
    private int page;
    private int columns;
    private int rows;
    private int gridLeft;
    private int gridTop;
    private int controlsY;
    private int infoY;
    private int saveY;

    public ChainMinerConfigScreen(Screen parent) {
        super(Component.translatable("screen.feng_chain_miner.title"));
        this.parent = parent;

        for (Block block : BuiltInRegistries.BLOCK) {
            ItemStack stack = new ItemStack(block);
            if (block.asItem() != Items.AIR && !stack.isEmpty()) {
                allBlocks.add(new BlockOption(BuiltInRegistries.BLOCK.getKey(block), block, stack));
            }
        }
        allBlocks.sort(Comparator.comparing(option -> option.id().toString()));
        selectRules(ChainMinerConfig.getRules());
        filteredBlocks = List.copyOf(allBlocks);
    }

    @Override
    protected void init() {
        String previousSearch = searchField == null ? "" : searchField.getValue();
        categoryButtons.clear();

        int contentWidth = Math.min(560, width - 24);
        int left = (width - contentWidth) / 2;
        int maxFieldWidth = Math.min(132, Math.max(112, contentWidth / 4));

        maxBlocksField = new EditBox(
                font,
                left,
                32,
                maxFieldWidth,
                20,
                Component.translatable("screen.feng_chain_miner.max_blocks")
        );
        maxBlocksField.setValue(Integer.toString(ChainMinerConfig.getMaxBlocks()));
        maxBlocksField.setMaxLength(4);
        addRenderableWidget(maxBlocksField);

        searchField = new EditBox(
                font,
                left + maxFieldWidth + 8,
                32,
                contentWidth - maxFieldWidth - 8,
                20,
                Component.translatable("screen.feng_chain_miner.search")
        );
        searchField.setValue(previousSearch);
        searchField.setResponder(this::filterBlocks);
        addRenderableWidget(searchField);

        int categoryY = 57;
        int categoryGap = 2;
        int categoryWidth = Math.max(24,
                (contentWidth - categoryGap * (BlockCategory.values().length - 1))
                        / BlockCategory.values().length);
        for (int index = 0; index < BlockCategory.values().length; index++) {
            BlockCategory buttonCategory = BlockCategory.values()[index];
            Button categoryButton = addRenderableWidget(Button.builder(
                    categoryText(buttonCategory),
                    button -> selectCategory(buttonCategory)
            ).bounds(left + index * (categoryWidth + categoryGap), categoryY, categoryWidth, 18).build());
            categoryButtons.add(categoryButton);
        }

        saveY = height - 23;
        controlsY = saveY - 22;
        infoY = controlsY - 13;
        gridTop = 80;
        columns = Math.max(1, contentWidth / CELL_SIZE);
        rows = Math.max(2, (infoY - gridTop - 3) / CELL_SIZE);
        gridLeft = (width - columns * CELL_SIZE) / 2;

        int actionGap = 4;
        int actionWidth = (contentWidth - actionGap * 3) / 4;

        previousPageButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.feng_chain_miner.previous"),
                button -> changePage(-1)
        ).bounds(left, controlsY, actionWidth, 18).build());

        nextPageButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.feng_chain_miner.next"),
                button -> changePage(1)
        ).bounds(left + actionWidth + actionGap, controlsY, actionWidth, 18).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.feng_chain_miner.clear"),
                button -> clearSelection()
        ).bounds(left + (actionWidth + actionGap) * 2, controlsY, actionWidth, 18).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.feng_chain_miner.reset"),
                button -> resetDefaults()
        ).bounds(left + (actionWidth + actionGap) * 3, controlsY, actionWidth, 18).build());

        int saveWidth = Math.min(140, (contentWidth - 4) / 2);

        addRenderableWidget(Button.builder(
                Component.translatable("screen.feng_chain_miner.save"),
                button -> saveAndClose()
        ).bounds(width / 2 - saveWidth - 2, saveY, saveWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(width / 2 + 2, saveY, saveWidth, 20).build());

        setInitialFocus(searchField);
        applyFilters();
        refreshCategoryButtons();
        refreshPageButtons();
    }

    private void filterBlocks(String query) {
        applyFilters();
        page = 0;
        status = Component.empty();
        refreshPageButtons();
    }

    private void selectCategory(BlockCategory newCategory) {
        category = newCategory;
        page = 0;
        status = Component.empty();
        applyFilters();
        refreshCategoryButtons();
        refreshPageButtons();
    }

    private void applyFilters() {
        String normalized = searchField == null
                ? ""
                : searchField.getValue().trim().toLowerCase(Locale.ROOT);
        filteredBlocks = allBlocks.stream()
                .filter(this::matchesCategory)
                .filter(option -> normalized.isEmpty()
                        || option.id().toString().contains(normalized)
                        || option.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
        page = Math.max(0, Math.min(getPageCount() - 1, page));
    }

    private boolean matchesCategory(BlockOption option) {
        return switch (category) {
            case ALL -> true;
            case SELECTED -> selectedBlocks.contains(option.id());
            case ORES -> isOre(option);
            case LOGS -> isInAnyTag(option, BlockTags.LOGS);
            case STONE -> isStone(option);
            case NATURAL -> isNatural(option);
            case BUILDING -> isBuilding(option);
            case OTHER -> !isOre(option)
                    && !isInAnyTag(option, BlockTags.LOGS)
                    && !isStone(option)
                    && !isNatural(option)
                    && !isBuilding(option);
        };
    }

    private boolean isOre(BlockOption option) {
        return isInAnyTag(
                option,
                BlockTags.COAL_ORES,
                BlockTags.COPPER_ORES,
                BlockTags.IRON_ORES,
                BlockTags.LAPIS_ORES,
                BlockTags.REDSTONE_ORES,
                BlockTags.EMERALD_ORES,
                BlockTags.GOLD_ORES,
                BlockTags.DIAMOND_ORES
        );
    }

    private boolean isStone(BlockOption option) {
        String path = option.id().getPath();
        return isInAnyTag(
                option,
                BlockTags.BASE_STONE_OVERWORLD,
                BlockTags.BASE_STONE_NETHER,
                BlockTags.STONE_BRICKS
        ) || path.contains("tuff")
                || path.contains("calcite")
                || path.contains("dripstone")
                || path.contains("end_stone")
                || path.contains("obsidian");
    }

    private boolean isNatural(BlockOption option) {
        return isInAnyTag(
                option,
                BlockTags.DIRT,
                BlockTags.SAND,
                BlockTags.LEAVES,
                BlockTags.SAPLINGS,
                BlockTags.FLOWERS,
                BlockTags.CROPS,
                BlockTags.ICE,
                BlockTags.SNOW,
                BlockTags.CORALS,
                BlockTags.WART_BLOCKS
        );
    }

    private boolean isBuilding(BlockOption option) {
        String path = option.id().getPath();
        return isInAnyTag(
                option,
                BlockTags.PLANKS,
                BlockTags.WOOL,
                BlockTags.TERRACOTTA,
                BlockTags.STAIRS,
                BlockTags.SLABS,
                BlockTags.WALLS,
                BlockTags.DOORS,
                BlockTags.TRAPDOORS,
                BlockTags.FENCES,
                BlockTags.FENCE_GATES
        ) || path.contains("glass")
                || path.contains("brick")
                || path.contains("concrete");
    }

    @SafeVarargs
    private static boolean isInAnyTag(BlockOption option, TagKey<Block>... tags) {
        for (TagKey<Block> tag : tags) {
            if (option.block().defaultBlockState().is(tag)) {
                return true;
            }
        }
        return false;
    }

    private Component categoryText(BlockCategory buttonCategory) {
        String key = "screen.feng_chain_miner.category." + buttonCategory.translationSuffix;
        return buttonCategory == category
                ? Component.literal("✓ ").append(Component.translatable(key))
                : Component.translatable(key);
    }

    private void refreshCategoryButtons() {
        BlockCategory[] categories = BlockCategory.values();
        for (int index = 0; index < categoryButtons.size(); index++) {
            categoryButtons.get(index).setMessage(categoryText(categories[index]));
        }
    }

    private void changePage(int direction) {
        page = Math.max(0, Math.min(getPageCount() - 1, page + direction));
        status = Component.empty();
        refreshPageButtons();
    }

    private void clearSelection() {
        selectedBlocks.clear();
        applyFilters();
        refreshPageButtons();
        status = Component.translatable("screen.feng_chain_miner.selection_cleared");
        statusColor = 0xFFFF55;
    }

    private void resetDefaults() {
        selectRules(ChainMinerConfig.getDefaultRules());
        applyFilters();
        refreshPageButtons();
        status = Component.translatable("screen.feng_chain_miner.defaults_restored");
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
        ResourceLocation id = ResourceLocation.tryParse(isTag ? rawRule.substring(1) : rawRule);
        if (id == null) {
            return false;
        }
        if (!isTag) {
            return option.id().equals(id);
        }
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
        return option.block().defaultBlockState().is(tag);
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
            int maxBlocks = Integer.parseInt(maxBlocksField.getValue().trim());
            if (maxBlocks < 1 || maxBlocks > 4096) {
                throw new IllegalArgumentException("range");
            }

            List<String> rules = selectedBlocks.stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();
            ChainMinerConfig.save(maxBlocks, rules);
            onClose();
        } catch (IllegalArgumentException exception) {
            showError("screen.feng_chain_miner.error_number");
        } catch (IOException exception) {
            showError("screen.feng_chain_miner.error_save");
        }
    }

    private void showError(String translationKey) {
        status = Component.translatable(translationKey);
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

        ResourceLocation id = filteredBlocks.get(index).id();
        if (!selectedBlocks.add(id)) {
            selectedBlocks.remove(id);
        }
        if (category == BlockCategory.SELECTED) {
            applyFilters();
            refreshPageButtons();
        }
        status = Component.empty();
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);

        int contentWidth = Math.min(560, width - 24);
        int left = (width - contentWidth) / 2;
        int maxFieldWidth = Math.min(132, Math.max(112, contentWidth / 4));
        graphics.fill(left - 5, 18, left + contentWidth + 5, saveY + 22, 0x55000000);
        graphics.drawString(
                font,
                Component.translatable("screen.feng_chain_miner.max_blocks"),
                left,
                21,
                0xA0A0A0
        );
        graphics.drawString(
                font,
                Component.translatable("screen.feng_chain_miner.search"),
                left + maxFieldWidth + 8,
                21,
                0xA0A0A0
        );

        BlockOption hovered = renderBlockGrid(graphics, mouseX, mouseY);
        boolean hasStatus = !status.getString().isEmpty();
        Component info = hasStatus
                ? status
                : Component.translatable(
                        "screen.feng_chain_miner.page_status",
                        page + 1,
                        getPageCount(),
                        selectedBlocks.size()
                );
        graphics.drawCenteredString(
                font,
                info,
                width / 2,
                infoY + 1,
                hasStatus ? statusColor : 0xFFFFFF
        );

        super.render(graphics, mouseX, mouseY, delta);
        if (hovered != null) {
            graphics.renderTooltip(font, hovered.stack(), mouseX, mouseY);
        }
    }

    private BlockOption renderBlockGrid(GuiGraphics graphics, int mouseX, int mouseY) {
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
            graphics.fill(x, y, x + CELL_SIZE - 2, y + CELL_SIZE - 2, background);
            graphics.fill(x, y, x + CELL_SIZE - 2, y + 1, border);
            graphics.fill(x, y + CELL_SIZE - 3, x + CELL_SIZE - 2, y + CELL_SIZE - 2, border);
            graphics.fill(x, y, x + 1, y + CELL_SIZE - 2, border);
            graphics.fill(x + CELL_SIZE - 3, y, x + CELL_SIZE - 2, y + CELL_SIZE - 2, border);
            graphics.renderItem(option.stack(), x + 2, y + 2);

            if (isHovered) {
                hovered = option;
            }
        }
        return hovered;
    }

    private record BlockOption(ResourceLocation id, Block block, ItemStack stack) {
    }

    private enum BlockCategory {
        ALL("all"),
        SELECTED("selected"),
        ORES("ores"),
        LOGS("logs"),
        STONE("stone"),
        NATURAL("natural"),
        BUILDING("building"),
        OTHER("other");

        private final String translationSuffix;

        BlockCategory(String translationSuffix) {
            this.translationSuffix = translationSuffix;
        }
    }
}
