package com.breakinblocks.qualitysushi.utils;

import de.cadentem.quality_food.core.codecs.Quality;
import de.cadentem.quality_food.core.codecs.QualityType;
import de.cadentem.quality_food.util.QualityUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Utils {

    public static float getQualityBonus(ItemStack stack){
        float bonus = 0;
        if(!QualityUtils.hasQuality(stack)){
            return bonus;
        }
        Holder<QualityType> type = QualityUtils.getType(stack);
        if(type.value() != QualityType.NONE){
            bonus += (float) (type.value().craftingBonus());
        }
        return bonus;
    }

    public static float getQualityBonus(List<ItemStack> stacks){
        int validIngredients = 0;
        float bonus = 0;

        for (ItemStack stack : stacks) {
            validIngredients++;
        }
        if (validIngredients == 0) {
            return 0;
        }
        for (ItemStack stack : stacks) {
            Holder<QualityType> type = QualityUtils.getType(stack);
            if (type.value() != QualityType.NONE) {
                bonus += (float) (type.value().craftingBonus() / validIngredients);
            }
        }

        return bonus;
    }

    public static HashMap<Integer, Integer> getQualitiesMap(List<ItemStack> stacks){
        HashMap<Integer, Integer> qualities = new HashMap<>();
        for (ItemStack stack : stacks) {
            Quality quality = QualityUtils.getQuality(stack);
            qualities.compute(quality.level(), (key, value) -> value == null ? 1 : value + 1);
        }
        return qualities;
    }

    public static Quality getQuality(final HashMap<Integer, Integer> qualities, int itemCount, final ItemStack result) {
        List<Integer> levels = qualities.keySet().stream().sorted(Comparator.comparingInt(Integer::intValue).reversed()).toList();

        for (Integer level : levels) {
            itemCount -= qualities.get(level);

            if (itemCount <= 0) {
                return Quality.getRandom(result, level);
            }
        }
        return Quality.NONE;
    }

}
