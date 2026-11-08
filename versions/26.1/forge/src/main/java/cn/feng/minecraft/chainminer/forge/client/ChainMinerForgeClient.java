package cn.feng.minecraft.chainminer.forge.client;

import cn.feng.minecraft.chainminer.forge.ChainMinerForge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ChainMinerForge.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ChainMinerForgeClient {
    private static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.feng_chain_miner.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("feng_chain_miner", "general"))
    );

    private ChainMinerForgeClient() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
        MinecraftForge.EVENT_BUS.register(ChainMinerForgeClient.class);
    }

    @SubscribeEvent
    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_CONFIG.consumeClick()) {
            minecraft.setScreen(new ChainMinerConfigScreen(minecraft.screen));
        }
    }
}
