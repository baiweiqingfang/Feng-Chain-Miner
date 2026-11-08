package cn.feng.minecraft.chainminer.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ChainMinerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBinding openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.feng_chain_miner.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyBinding.Category.create(Identifier.of("feng_chain_miner", "general"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfig.wasPressed()) {
                client.setScreen(new ChainMinerConfigScreen(client.currentScreen));
            }
        });
    }
}
