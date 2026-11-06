package cn.feng.minecraft.chainminer.fabric.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {
    private static final int MAX_SURVIVAL_COST = 39;

    @Shadow
    @Final
    private Property levelCost;

    @Redirect(
            method = "updateResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;getRepairCost()I"
            )
    )
    private int fengChainMiner$ignorePriorWorkPenalty(ItemStack stack) {
        return 0;
    }

    @Redirect(
            method = "updateResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/screen/AnvilScreenHandler;getNextCost(I)I"
            )
    )
    private static int fengChainMiner$doNotIncreasePriorWork(int currentCost) {
        return 0;
    }

    @ModifyConstant(
            method = "updateResult",
            constant = @Constant(intValue = 40, ordinal = 2)
    )
    private int fengChainMiner$removeTooExpensiveLimit(int originalLimit) {
        return Integer.MAX_VALUE;
    }

    @Inject(
            method = "updateResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/screen/AnvilScreenHandler;sendContentUpdates()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void fengChainMiner$capDisplayedCost(CallbackInfo callbackInfo) {
        if (levelCost.get() > MAX_SURVIVAL_COST) {
            levelCost.set(MAX_SURVIVAL_COST);
        }
    }
}

