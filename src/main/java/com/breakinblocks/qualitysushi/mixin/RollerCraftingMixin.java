package com.breakinblocks.qualitysushi.mixin;

import com.buuz135.sushigocrafting.api.IFoodIngredient;
import com.buuz135.sushigocrafting.api.impl.FoodAPI;
import com.buuz135.sushigocrafting.api.impl.FoodHelper;
import com.buuz135.sushigocrafting.item.FoodItem;
import com.buuz135.sushigocrafting.item.SushiDataComponent;
import com.buuz135.sushigocrafting.proxy.SushiContent;
import com.buuz135.sushigocrafting.tile.machinery.RollerTile;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.mojang.logging.LogUtils;
import de.cadentem.quality_food.core.Bonus;
import de.cadentem.quality_food.core.codecs.Quality;
import de.cadentem.quality_food.core.codecs.QualityType;
import de.cadentem.quality_food.registry.QFComponents;
import de.cadentem.quality_food.util.QualityUtils;
import de.cadentem.quality_food.util.Utils;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.breakinblocks.qualitysushi.utils.Utils.getQualitiesMap;
import static com.breakinblocks.qualitysushi.utils.Utils.getQuality;


@Mixin(RollerTile.class)
public class RollerCraftingMixin {
    RollerTile original = (RollerTile) (Object) this;
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow @Final private InventoryComponent<RollerTile> slots;
    @Shadow @Final private InventoryComponent<RollerTile> spices;
    @Shadow private String selected;
    @Shadow private int craftProgress;
    @Shadow @Final private RollerTile.WeightTracker weightTracker;



    @Inject(
            method = "onClick(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void modifyResult(Player player, CallbackInfo ci){


        FoodAPI.get().getTypeFromName(selected).ifPresent(iFoodType -> {
            float bonus = 0;
            boolean allFull = true;
            for (int i1 = 0; i1 < slots.getSlots(); i1++) {
                if (i1 < iFoodType.getFoodIngredients().size()) {
                    IFoodIngredient ingredient = FoodAPI.get().getIngredientFromItem(slots.getStackInSlot(i1).getItem());
                    if (!ingredient.isEmpty() && !ingredient.getIngredientConsumer().canConsume(ingredient, slots.getStackInSlot(i1), weightTracker.getWeights().get(i1))) {
                        allFull = false;
                        break;
                    }
                }
            }
            if (allFull) {
                ++craftProgress;
                if (craftProgress >= 4) {
                    Random random = new Random(((ServerLevel) original.getLevel()).getSeed() + selected.hashCode());
                    craftProgress = 0;
                    List<IFoodIngredient> foodIngredients = new ArrayList<>();
                    List<Integer> weightValues = new ArrayList<>();
                    AtomicReference<ItemStack> discovery = new AtomicReference<>(ItemStack.EMPTY);
                    for (int slot = 0; slot < slots.getSlots(); slot++) {
                        if (slot < iFoodType.getFoodIngredients().size()) {
                            IFoodIngredient ingredient = FoodAPI.get().getIngredientFromItem(slots.getStackInSlot(slot).getItem());
                            foodIngredients.add(ingredient);
                        }
                    }
                    FoodItem item = FoodHelper.getFoodFromIngredients(selected, foodIngredients);
                    if (item != null) {
                        ItemStack stack = new ItemStack(item);
                        for (int slot = 0; slot < slots.getSlots(); slot++) {
                            if (slot < iFoodType.getFoodIngredients().size()) {
                                IFoodIngredient ingredient = foodIngredients.get(slot);
                                ingredient.getIngredientConsumer().consume(ingredient, slots.getStackInSlot(slot), weightTracker.getWeights().get(slot));
                                int value = random.nextInt(5) - weightTracker.getWeights().get(slot);
                                weightValues.add(value);
                                if (value == 0 && !ingredient.isEmpty()) {
                                    int finalSlot = slot;
                                    var iSushiWeightDiscovery = player.getData(SushiContent.AttachmentTypes.SUSHI_WEIGHT_DISCOVERY);
                                    if (!iSushiWeightDiscovery.hasDiscovery(selected + "-" + finalSlot)) {
                                        iSushiWeightDiscovery.setDiscovery(selected + "-" + finalSlot, weightTracker.getWeights().get(finalSlot));
                                        discovery.set(stack.copy());
                                    }
                                }
                            }
                        }
                        stack.set(SushiDataComponent.FOOD_WEIGHTS, weightValues);
                        CompoundTag spicesNBT = new CompoundTag();
                        for (int i = 0; i < spices.getSlots(); i++) {
                            if (!spices.getStackInSlot(i).isEmpty()) {
                                IFoodIngredient soy = FoodAPI.get().getIngredientFromItem(spices.getStackInSlot(i).getItem());
                                if (soy.getIngredientConsumer().canConsume(soy, spices.getStackInSlot(i), 0)) {
                                    soy.getIngredientConsumer().consume(soy, spices.getStackInSlot(i), 0);
                                    spicesNBT.putBoolean(soy.getName(), true);
                                }
                            }
                        }
                        stack.set(SushiDataComponent.FOOD_SPICES, spicesNBT);

                        List<ItemStack> stacks = new ArrayList<>();
                        for(int i1 = 0; i1 < slots.getSlots(); i1++){
                            ItemStack stack1 = slots.getStackInSlot(i1);
                            if(!stack1.isEmpty()){
                                stacks.add(stack1);
                            }
                        }

                        for(int i2 = 0; i2 < spices.getSlots(); i2++){
                            ItemStack stack2 = spices.getStackInSlot(i2);
                            if(!stack2.isEmpty()){
                                stacks.add(stack2);
                            }
                        }

                        Quality quality = getQuality(getQualitiesMap(stacks), stacks.size()-1, stack);
                        QualityUtils.applyQuality(stack,quality);
                        Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
                    }
                    if (player instanceof ServerPlayer && !discovery.get().isEmpty()) {
                        player.getData(SushiContent.AttachmentTypes.SUSHI_WEIGHT_DISCOVERY).requestUpdate((ServerPlayer) player, discovery.get(), null);
                    }
                }

            }
        });

        ci.cancel();
    }


}
