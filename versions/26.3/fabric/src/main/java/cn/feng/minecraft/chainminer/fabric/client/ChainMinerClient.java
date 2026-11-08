package cn.feng.minecraft.chainminer.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ChainMinerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.feng_chain_miner.open_config",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_V,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("feng_chain_miner", "general"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfig.consumeClick()) {
                client.setScreenAndShow(new ChainMinerConfigScreen(null));
            }
        });
    }
}
