package com.breakinblocks.qualitysushi.mixin;

import com.buuz135.sushigocrafting.item.AmountItem;
import com.buuz135.sushigocrafting.item.SushiDataComponent;
import com.buuz135.sushigocrafting.recipe.CombineAmountItemRecipe;
import com.buuz135.sushigocrafting.tile.machinery.CoolerBoxTile;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.mojang.logging.LogUtils;
import de.cadentem.quality_food.util.QualityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(CoolerBoxTile.class)
public class CoolingBoxMixin {
    CoolerBoxTile original = (CoolerBoxTile) (Object) this;
    @Shadow @Final private InventoryComponent input;
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/buuz135/sushigocrafting/tile/machinery/CoolerBoxTile;)V",
            at = @At(value = "INVOKE", target = "Lcom/hrznstudio/titanium/component/inventory/InventoryComponent;getSlots()I"),
            cancellable = true
    )
    private void checkForNBT(Level level, BlockPos pos, BlockState state, CoolerBoxTile blockEntity, CallbackInfo ci) {
        for (int current = 0; current < input.getSlots(); current++)
            for (int other = 0; other < input.getSlots(); other++) {
                if (current == other || !CombineAmountItemRecipe.stackMatches(input.getStackInSlot(current), input.getStackInSlot(other))) continue;

                if(QualityUtils.getQuality(input.getStackInSlot(current)).level() == QualityUtils.getQuality(input.getStackInSlot(other)).level()) {
                    ItemStack result = this.getResult(Arrays.asList(input.getStackInSlot(current), input.getStackInSlot(other)));
                    if (!result.isEmpty()) {
                        input.setStackInSlot(current, result);
                        input.setStackInSlot(other, ItemStack.EMPTY);
                        return;
                    }
                }
                ci.cancel();
            }
        ci.cancel();
    }


    private ItemStack getResult(List<ItemStack> list) {
        if (list.size() == 2) {
            ItemStack first = (ItemStack)list.get(0);
            ItemStack second = (ItemStack)list.get(1);
            if (first.getItem() == second.getItem() && first.getCount() == 1 && second.getCount() == 1 && first.getItem() instanceof AmountItem) {
                ItemStack output = new ItemStack(first.getItem());
                QualityUtils.applyQuality(output, QualityUtils.getQuality(first));
                output.set(SushiDataComponent.AMOUNT, Math.min(((AmountItem)first.getItem()).getMaxCombineAmount(), ((AmountItem)first.getItem()).getCurrentAmount(first) + ((AmountItem)second.getItem()).getCurrentAmount(second)));
                return output;
            }
        }
        return ItemStack.EMPTY;
    }

}
