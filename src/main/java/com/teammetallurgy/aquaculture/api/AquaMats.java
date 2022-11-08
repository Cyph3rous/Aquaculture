package com.teammetallurgy.aquaculture.api;

import com.teammetallurgy.aquaculture.init.AquaItems;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.IItemTier;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

import javax.annotation.Nonnull;

public class AquaMats {
    public IItemTier NEPTUNIUM = new IItemTier() {
        @Override
        public int getMaxUses() {
            return 1796;
        }
        @Override
        public float getEfficiency() {
            return 8.5F;
        }
        @Override
        public float getAttackDamage() {
            return 3.5F;
        }
        @Override
        public int getHarvestLevel() {
            return 3;
        }
        @Override
        public int getEnchantability() {
            return 14;
        }
        @Override
        @Nonnull
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(AquaItems.NEPTUNIUM_INGOT);
        }
    };
    public IArmorMaterial NEPTINIUM_ARMOR = new IArmorMaterial() {
        private final int MAX_DAMAGE_FACTOR = 35;

        @Override
        public int getDurability(@Nonnull EquipmentSlotType slot) {
            int[] maxDamageArray = new int[]{13, 15, 16, 11};
            return maxDamageArray[slot.getIndex()] * MAX_DAMAGE_FACTOR;
        }
        @Override
        public int getDamageReductionAmount(@Nonnull EquipmentSlotType slot) {
            int[] damageReduction = new int[]{3, 6, 8, 3};
            return damageReduction[slot.getIndex()];
        }
        @Override
        public int getEnchantability() {
            return 14;
        }
        @Override
        @Nonnull
        public SoundEvent getSoundEvent() {
            return SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND;
        }
        @Override
        @Nonnull
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(AquaItems.NEPTUNIUM_INGOT);
        }
        @Override
        @Nonnull
        public String getName() {
            return "neptunium";
        }
        @Override
        public float getToughness() {
            return 2.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };
}
