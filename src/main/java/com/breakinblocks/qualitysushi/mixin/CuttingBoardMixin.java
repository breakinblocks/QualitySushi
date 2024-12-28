package com.breakinblocks.qualitysushi.mixin;


import com.breakinblocks.qualitysushi.Qualitysushi;
import com.breakinblocks.qualitysushi.utils.Utils;
import com.buuz135.sushigocrafting.api.impl.FoodAPI;
import com.buuz135.sushigocrafting.item.AmountItem;
import com.buuz135.sushigocrafting.proxy.SushiContent;
import com.buuz135.sushigocrafting.recipe.CuttingBoardRecipe;
import com.buuz135.sushigocrafting.tile.machinery.CuttingBoardTile;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.util.RecipeUtil;
import com.mojang.logging.LogUtils;
import de.cadentem.quality_food.core.Bonus;
import de.cadentem.quality_food.core.codecs.Quality;
import de.cadentem.quality_food.util.QualityUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;
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

import static com.breakinblocks.qualitysushi.utils.Utils.getQualitiesMap;
import static com.breakinblocks.qualitysushi.utils.Utils.getQuality;

@Mixin(CuttingBoardTile.class)
public class CuttingBoardMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow private int click;
    @Shadow private InventoryComponent input;
    @Inject(method="onActivated", at= @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/items/ItemHandlerHelper;giveItemToPlayer(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V"),
    cancellable = true)
    public void onActivated(Player player, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ, CallbackInfoReturnable<ItemInteractionResult> cir) {
        for (CuttingBoardRecipe recipe : RecipeUtil.getRecipes(player.level(), ((RecipeType<CuttingBoardRecipe>) SushiContent.RecipeTypes.CUTTING_BOARD.get()))) {
            if (recipe.getInput().test(this.input.getStackInSlot(0))) {
                Item item = FoodAPI.get().getIngredientFromName(recipe.getIngredient()).getItem();
                ItemStack stack;
                Quality quality = Quality.NONE;
                if (item instanceof AmountItem) {
                    if(QualityUtils.hasQuality(input.getStackInSlot(0))) {
                        List<ItemStack> stacks = new ArrayList<>();
                        stacks.add(input.getStackInSlot(0));
                        quality = getQuality(getQualitiesMap(stacks), stacks.size(), input.getStackInSlot(0));
                    }
                    stack = ((AmountItem) item).random(player, player.level());
                    QualityUtils.applyQuality(stack, quality);
                } else {
                    stack = new ItemStack(item);
                    QualityUtils.applyQuality(stack, (Player) player);
                }
                ItemHandlerHelper.giveItemToPlayer(player, stack);
                this.input.getStackInSlot(0).shrink(1);
                cir.cancel();
            }
        }
        click = 0;
    }
}
