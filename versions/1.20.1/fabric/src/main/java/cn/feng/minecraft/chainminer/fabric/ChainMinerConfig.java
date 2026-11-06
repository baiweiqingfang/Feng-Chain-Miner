package cn.feng.minecraft.chainminer.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ChainMinerConfig {
    private static final int DEFAULT_MAX_BLOCKS = 128;
    private static final int MAX_ALLOWED_BLOCKS = 4096;
    private static final List<String> DEFAULT_RULES = List.of(
            "#minecraft:coal_ores",
            "#minecraft:copper_ores",
            "#minecraft:iron_ores",
            "#minecraft:lapis_ores",
            "#minecraft:redstone_ores",
            "#minecraft:emerald_ores",
            "#minecraft:gold_ores",
            "#minecraft:diamond_ores",
            "#minecraft:logs"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("feng-chain-miner.json");

    private static volatile Snapshot current = createSnapshot(DEFAULT_MAX_BLOCKS, DEFAULT_RULES);

    private ChainMinerConfig() {
    }

    public static synchronized void initialize() {
        if (!Files.exists(CONFIG_PATH)) {
            try {
                save(DEFAULT_MAX_BLOCKS, DEFAULT_RULES);
            } catch (IOException ignored) {
                current = createSnapshot(DEFAULT_MAX_BLOCKS, DEFAULT_RULES);
            }
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            StoredConfig stored = GSON.fromJson(reader, StoredConfig.class);
            if (stored == null) {
                throw new IOException("Empty configuration");
            }
            current = createSnapshot(stored.maxBlocks, stored.blocks);
        } catch (Exception ignored) {
            current = createSnapshot(DEFAULT_MAX_BLOCKS, DEFAULT_RULES);
        }
    }

    public static synchronized void save(int maxBlocks, List<String> rules) throws IOException {
        Snapshot next = createSnapshot(maxBlocks, rules);
        Files.createDirectories(CONFIG_PATH.getParent());

        StoredConfig stored = new StoredConfig();
        stored.maxBlocks = next.maxBlocks();
        stored.blocks = next.ruleTexts();
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(stored, writer);
        }
        current = next;
    }

    public static int getMaxBlocks() {
        return current.maxBlocks();
    }

    public static List<String> getRules() {
        return current.ruleTexts();
    }

    public static boolean allows(BlockState state) {
        for (Rule rule : current.rules()) {
            if (rule.matches(state)) {
                return true;
            }
        }
        return false;
    }

    public static TagKey<Block> firstMatchingTag(BlockState state) {
        for (Rule rule : current.rules()) {
            if (rule.tag() != null && state.isIn(rule.tag())) {
                return rule.tag();
            }
        }
        return null;
    }

    private static Snapshot createSnapshot(int maxBlocks, List<String> rawRules) {
        int safeMax = Math.max(1, Math.min(MAX_ALLOWED_BLOCKS, maxBlocks));
        Set<String> normalized = new LinkedHashSet<>();

        if (rawRules != null) {
            for (String raw : rawRules) {
                String rule = normalizeRule(raw);
                if (rule != null) {
                    normalized.add(rule);
                }
            }
        }
        if (rawRules == null) {
            normalized.addAll(DEFAULT_RULES);
        }

        List<Rule> parsed = new ArrayList<>();
        for (String text : normalized) {
            boolean isTag = text.startsWith("#");
            Identifier id = Identifier.tryParse(isTag ? text.substring(1) : text);
            if (id == null) {
                continue;
            }
            if (isTag) {
                parsed.add(new Rule(null, TagKey.of(RegistryKeys.BLOCK, id)));
            } else {
                parsed.add(new Rule(id, null));
            }
        }

        List<String> texts = List.copyOf(normalized);
        return new Snapshot(safeMax, texts, List.copyOf(parsed));
    }

    public static String normalizeRule(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        boolean isTag = value.startsWith("#");
        Identifier id = Identifier.tryParse(isTag ? value.substring(1) : value);
        return id == null ? null : (isTag ? "#" : "") + id;
    }

    public static List<String> getDefaultRules() {
        return DEFAULT_RULES;
    }

    private record Snapshot(int maxBlocks, List<String> ruleTexts, List<Rule> rules) {
    }

    private record Rule(Identifier blockId, TagKey<Block> tag) {
        private boolean matches(BlockState state) {
            if (tag != null) {
                return state.isIn(tag);
            }
            return blockId.equals(Registries.BLOCK.getId(state.getBlock()));
        }
    }

    private static final class StoredConfig {
        private int maxBlocks = DEFAULT_MAX_BLOCKS;
        private List<String> blocks = new ArrayList<>(DEFAULT_RULES);
    }
}

