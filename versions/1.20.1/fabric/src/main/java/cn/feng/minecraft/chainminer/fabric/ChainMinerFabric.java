package cn.feng.minecraft.chainminer.fabric;

import cn.feng.minecraft.chainminer.core.ChainSearch;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ChainMinerFabric implements ModInitializer {
    private static final int LOG_FAMILY = 100;
    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();

    @Override
    public void onInitialize() {
        ChainMinerConfig.initialize();

        PlayerBlockBreakEvents.AFTER.register((world, player, origin, originalState, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || ACTIVE_PLAYERS.contains(player.getUuid())) {
                return;
            }

            if (!ChainMinerConfig.allows(originalState)) {
                return;
            }

            int family = getFamily(originalState);
            Block originalBlock = originalState.getBlock();
            TagKey<Block> customTag = family == 0
                    ? ChainMinerConfig.firstMatchingTag(originalState)
                    : null;
            List<BlockPos> connected = ChainSearch.findConnected(
                    origin.toImmutable(),
                    ChainMinerFabric::neighbors,
                    pos -> isLoaded(serverWorld, pos)
                            && matches(serverWorld.getBlockState(pos), originalBlock, family, customTag),
                    ChainMinerConfig.getMaxBlocks()
            );

            ACTIVE_PLAYERS.add(player.getUuid());
            try {
                for (BlockPos target : connected) {
                    if (!serverPlayer.isAlive()) {
                        break;
                    }
                    if (matches(serverWorld.getBlockState(target), originalBlock, family, customTag)) {
                        serverPlayer.interactionManager.tryBreakBlock(target);
                    }
                }
            } finally {
                ACTIVE_PLAYERS.remove(player.getUuid());
            }
        });
    }

    private static Iterable<BlockPos> neighbors(BlockPos center) {
        List<BlockPos> result = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        result.add(center.add(x, y, z).toImmutable());
                    }
                }
            }
        }
        return result;
    }

    private static boolean isLoaded(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
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
            return state.isIn(customTag);
        }
        return state.getBlock() == originalBlock;
    }

    private static int getFamily(BlockState state) {
        if (state.isIn(BlockTags.COAL_ORES)) return 1;
        if (state.isIn(BlockTags.COPPER_ORES)) return 2;
        if (state.isIn(BlockTags.IRON_ORES)) return 3;
        if (state.isIn(BlockTags.LAPIS_ORES)) return 4;
        if (state.isIn(BlockTags.REDSTONE_ORES)) return 5;
        if (state.isIn(BlockTags.EMERALD_ORES)) return 6;
        if (state.isIn(BlockTags.GOLD_ORES)) return 7;
        if (state.isIn(BlockTags.DIAMOND_ORES)) return 8;
        if (state.isIn(BlockTags.LOGS)) return LOG_FAMILY;
        return 0;
    }
}

