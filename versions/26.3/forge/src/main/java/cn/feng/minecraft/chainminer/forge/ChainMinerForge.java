package cn.feng.minecraft.chainminer.forge;

import cn.feng.minecraft.chainminer.core.ChainSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod(ChainMinerForge.MOD_ID)
public final class ChainMinerForge {
    public static final String MOD_ID = "feng_chain_miner";
    private static final int LOG_FAMILY = 100;
    private static final TagKey<Block> COAL_ORES = vanillaTag("coal_ores");
    private static final TagKey<Block> LAPIS_ORES = vanillaTag("lapis_ores");
    private static final TagKey<Block> REDSTONE_ORES = vanillaTag("redstone_ores");
    private static final TagKey<Block> EMERALD_ORES = vanillaTag("emerald_ores");
    private static final TagKey<Block> DIAMOND_ORES = vanillaTag("diamond_ores");
    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();

    public ChainMinerForge() {
        ChainMinerConfig.initialize();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player)
                || ACTIVE_PLAYERS.contains(player.getUUID())) {
            return;
        }

        BlockState originalState = event.getState();
        if (!ChainMinerConfig.allows(originalState)) {
            return;
        }

        int family = getFamily(originalState);
        Block originalBlock = originalState.getBlock();
        TagKey<Block> customTag = family == 0
                ? ChainMinerConfig.firstMatchingTag(originalState)
                : null;
        List<BlockPos> connected = ChainSearch.findConnected(
                event.getPos().immutable(),
                ChainMinerForge::neighbors,
                pos -> isLoaded(level, pos)
                        && matches(level.getBlockState(pos), originalBlock, family, customTag),
                ChainMinerConfig.getMaxBlocks()
        );

        ACTIVE_PLAYERS.add(player.getUUID());
        try {
            for (BlockPos target : connected) {
                if (!player.isAlive()) {
                    break;
                }
                if (target.equals(event.getPos())) {
                    continue;
                }
                if (matches(level.getBlockState(target), originalBlock, family, customTag)) {
                    player.gameMode.destroyBlock(target);
                }
            }
        } finally {
            ACTIVE_PLAYERS.remove(player.getUUID());
        }
    }

    private static Iterable<BlockPos> neighbors(BlockPos center) {
        List<BlockPos> result = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        result.add(center.offset(x, y, z).immutable());
                    }
                }
            }
        }
        return result;
    }

    private static boolean isLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static boolean matches(
            BlockState state,
            Block originalBlock,
            int family,
            TagKey<Block> customTag
    ) {
        if (family == LOG_FAMILY) {
            return state.getBlock() == originalBlock && ChainMinerConfig.allows(state);
        }
        if (family != 0) {
            return getFamily(state) == family && ChainMinerConfig.allows(state);
        }
        if (customTag != null) {
            return state.is(customTag);
        }
        return state.getBlock() == originalBlock;
    }

    private static int getFamily(BlockState state) {
        if (state.is(COAL_ORES)) return 1;
        if (state.is(BlockTags.COPPER_ORES)) return 2;
        if (state.is(BlockTags.IRON_ORES)) return 3;
        if (state.is(LAPIS_ORES)) return 4;
        if (state.is(REDSTONE_ORES)) return 5;
        if (state.is(EMERALD_ORES)) return 6;
        if (state.is(BlockTags.GOLD_ORES)) return 7;
        if (state.is(DIAMOND_ORES)) return 8;
        if (state.is(BlockTags.LOGS)) return LOG_FAMILY;
        return 0;
    }

    private static TagKey<Block> vanillaTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(path));
    }
}
