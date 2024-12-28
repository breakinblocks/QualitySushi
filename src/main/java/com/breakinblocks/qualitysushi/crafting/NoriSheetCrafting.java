package com.breakinblocks.qualitysushi.crafting;

import com.buuz135.sushigocrafting.proxy.SushiContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

public class NoriSheetCrafting {
    TagKey<Block> baseTag = BlockTags.create(ResourceLocation.tryBuild("qualitysushi", "pressing_base"));

    public NoriSheetCrafting() {
        EVENT_BUS.register(this);
    }


    @SubscribeEvent
    public void pistonCrafting(PistonEvent.Pre event) {
        BlockPos targetPos = event.getPos().relative(event.getDirection(), 2);
        BlockState targetBlockState = event.getLevel().getBlockState(targetPos);

        if(targetBlockState.is(this.baseTag)){
            NonNullList<ItemStack> list = NonNullList.create();
            var level = event.getLevel();
            var aabb = new AABB(event.getPos().relative(event.getDirection(), 1));
            var entities = level.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
            for (ItemEntity entity : entities) {
                if (entity.getItem().is(Items.DRIED_KELP_BLOCK)) {
                    list.add(new ItemStack(SushiContent.Items.NORI_SHEET.get(), (5 + event.getLevel().getRandom().nextInt(4)) * entity.getItem().getCount()));
                    entity.remove(Entity.RemovalReason.KILLED);
                }
            }
            if (!list.isEmpty()) {
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.playSeededSound(null, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.75f, 1f, serverLevel.random.nextLong());
                }
                Containers.dropContents((Level) event.getLevel(), event.getFaceOffsetPos().offset(0, 0, 0), list);
            }
        }
    }
}
