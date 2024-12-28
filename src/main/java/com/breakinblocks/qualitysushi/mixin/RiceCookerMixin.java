package com.breakinblocks.qualitysushi.mixin;

import com.breakinblocks.qualitysushi.utils.Utils;
import com.buuz135.sushigocrafting.item.AmountItem;
import com.buuz135.sushigocrafting.proxy.SushiContent;
import com.buuz135.sushigocrafting.tile.machinery.RiceCookerTile;
import com.hrznstudio.titanium.Titanium;
import com.hrznstudio.titanium.component.fluid.FluidTankComponent;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.component.progress.ProgressBarComponent;
import com.hrznstudio.titanium.nbthandler.NBTManager;
import com.hrznstudio.titanium.network.messages.TileFieldNetworkMessage;
import com.mojang.logging.LogUtils;
import de.cadentem.quality_food.core.Bonus;
import de.cadentem.quality_food.core.codecs.Quality;
import de.cadentem.quality_food.util.QualityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(RiceCookerTile.class)
public class RiceCookerMixin {
    RiceCookerTile original = (RiceCookerTile) (Object) this;
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow @Final private InventoryComponent input;
    @Shadow @Final private ProgressBarComponent bar;
    @Shadow private InventoryComponent output;
    @Shadow @Final private FluidTankComponent water;
    @Shadow @Final private double burnTime;

    @Inject(method="<init>", at=@At("RETURN"))
    private void qualityRice(BlockPos pos, BlockState state, CallbackInfo ci) {
        original.getBar().setOnFinishWork(()->{
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < this.input.getSlots(); i++) {
                if(!this.input.getStackInSlot(i).isEmpty()){
                    stacks.add(this.input.getStackInSlot(i));
                }
            }

            ItemStack result = SushiContent.Items.COOKED_RICE.get().withAmount(getSlotsFilled() * 50);
            if (!this.output.getStackInSlot(0).isEmpty()) {
                ItemStack original = this.output.getStackInSlot(0);
                int amount = ((AmountItem) original.getItem()).getCurrentAmount(original);
                for(int i = 50; i < amount; i+=50){
                    stacks.add(original);
                }
                result = SushiContent.Items.COOKED_RICE.get().withAmount(Math.min(((AmountItem) original.getItem()).getMaxCombineAmount(), ((AmountItem) original.getItem()).getCurrentAmount(original) + ((AmountItem) result.getItem()).getCurrentAmount(result)));
            }

            Quality quality = Utils.getQuality(Utils.getQualitiesMap(stacks), stacks.size()-1, result);
            QualityUtils.applyQuality(result, quality);

            this.output.setStackInSlot(0, result);

            for (int i = 0; i < this.input.getSlots(); i++) {
                this.input.setStackInSlot(i, ItemStack.EMPTY);
            }
            this.water.drainForced(1000, IFluidHandler.FluidAction.EXECUTE);
            --this.burnTime;
            original.syncObject(this.bar);
            original.setChanged();
        });

    }
    private int getSlotsFilled() {
        int amount = 0;
        for (int i = 0; i < this.input.getSlots(); i++) {
            if (!this.input.getStackInSlot(i).isEmpty()) ++amount;
        }
        return amount;
    }


}
