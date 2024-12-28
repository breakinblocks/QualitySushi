package com.breakinblocks.qualitysushi.mixin;


import com.buuz135.sushigocrafting.item.AmountItem;
import com.buuz135.sushigocrafting.item.SushiDataComponent;
import com.buuz135.sushigocrafting.proxy.SushiContent;
import com.buuz135.sushigocrafting.recipe.FermentingBarrelRecipe;
import com.buuz135.sushigocrafting.tile.machinery.FermentationBarrelTile;
import com.hrznstudio.titanium.component.fluid.FluidTankComponent;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.util.RecipeUtil;
import de.cadentem.quality_food.core.codecs.Quality;
import de.cadentem.quality_food.util.QualityUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FermentationBarrelTile.class)
public class FermentingBarrelMixin {
    FermentationBarrelTile original = (FermentationBarrelTile) (Object) this;
    @Final @Shadow private InventoryComponent input;
    @Final @Shadow private InventoryComponent output;
    @Final @Shadow private FluidTankComponent fluid;


    @Inject(method="onFinish", at=@At("HEAD"))
    public void qualityFermenting(CallbackInfo ci) {
        RecipeUtil.getRecipes(original.getLevel(), (RecipeType<FermentingBarrelRecipe>) SushiContent.RecipeTypes.FERMENTING_BARREL.get()).stream()
                .filter(fermentingBarrelRecipe -> fermentingBarrelRecipe.input.test(this.input.getStackInSlot(0))
                        && (fermentingBarrelRecipe.fluid.isEmpty() || (fermentingBarrelRecipe.fluid.isFluidEqual(this.fluid.getFluid()) && this.fluid.getFluid().getAmount() >= fermentingBarrelRecipe.fluid.getAmount()))
                        && (original.canStack(fermentingBarrelRecipe))
                )
                .findFirst()
                .ifPresent(fermentingBarrelRecipe -> {
                    Quality quality = QualityUtils.getQuality(this.input.getStackInSlot(0));
                    this.input.getStackInSlot(0).shrink(1);
                    this.fluid.drainForced(fermentingBarrelRecipe.getFluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
                    if (fermentingBarrelRecipe.output.getItem() instanceof AmountItem) {
                        ItemStack outputStack = this.output.getStackInSlot(0);
                        ItemStack recipeOutput = ((AmountItem) fermentingBarrelRecipe.output.getItem()).random(null, original.getLevel());
                        QualityUtils.applyQuality(recipeOutput, quality);
                        if (outputStack.isEmpty()) {
                            ItemHandlerHelper.insertItem(this.output, recipeOutput, false);
                        } else {
                            combineStacks(outputStack, recipeOutput);
                        }
                    } else {
                        QualityUtils.applyQuality(fermentingBarrelRecipe.output.copy(), quality);
                        ItemHandlerHelper.insertItem(this.output, fermentingBarrelRecipe.output.copy(), false);
                    }

                });

    }
    private static ItemStack combineStacks(ItemStack first, ItemStack second) {
        if (!first.is(second.getItem())) {
            return null;
        } else {
            Item var4 = first.getItem();
            if (var4 instanceof AmountItem) {
                AmountItem firstAmount = (AmountItem)var4;
                var4 = second.getItem();
                if (var4 instanceof AmountItem) {
                    Quality firstQuality = QualityUtils.getQuality(first);
                    Quality secondQuality = QualityUtils.getQuality(second);
                    AmountItem secondAmount = (AmountItem)var4;
                    first.set(SushiDataComponent.AMOUNT, Math.min(firstAmount.getMaxCombineAmount(), firstAmount.getCurrentAmount(first) + secondAmount.getCurrentAmount(second)));
                    QualityUtils.applyQuality(first, secondQuality, true);
                    return first;
                }
            }

            return null;
        }
    }
}
