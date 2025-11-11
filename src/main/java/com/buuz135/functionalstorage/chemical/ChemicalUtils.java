package com.buuz135.functionalstorage.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ChemicalDrawerTile;
import com.buuz135.functionalstorage.util.NumberUtils;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;

import java.util.List;

/**
 * Utility class for chemical-related operations in FunctionalStorage.
 * Follows the same patterns as Utils.java for consistency.
 */
public class ChemicalUtils {
    
    /**
     * Deserializes a ChemicalStack from NBT, following the same pattern as Utils.deserializeFluid().
     * Safe to use with Mekanism API dependency.
     * 
     * @param provider Registry provider for codec operations
     * @param tag NBT compound tag containing the serialized chemical
     * @return Deserialized ChemicalStack, or ChemicalStack.EMPTY if tag is empty
     */
    public static ChemicalStack deserializeChemical(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        try {
            return ChemicalStack.OPTIONAL_CODEC.decode(
                RegistryOps.create(NbtOps.INSTANCE, provider), tag
            ).getOrThrow().getFirst();
        } catch (Exception e) {
            // Handle malformed or empty chemical stack data gracefully
            return ChemicalStack.EMPTY;
        }
    }
    
    /**
     * Converts ChemicalStack amount to display string with proper formatting.
     * Follows the same pattern as NumberUtils.getFormatedFluidBigNumber().
     */
    public static String getFormattedAmount(long amount) {
        return NumberUtils.getFormatedFluidBigNumber(amount);
    }
    

    
    /**
     * Checks if a chemical is radioactive using direct API
     */
    public static boolean isChemicalRadioactive(ChemicalStack stack) {
        if (stack.isEmpty() || !FunctionalStorage.MEKANISM_LOADED) {
            return false;
        }
        
        return stack.isRadioactive();
    }
    
    /**
     * Adds radioactive information to chemical tooltips
     */
    public static void addRadioactiveInformation(List<Component> tooltip, ChemicalStack stack, boolean isRadioactiveMode) {
        if (!isRadioactiveMode || stack.isEmpty() || !FunctionalStorage.MEKANISM_LOADED) {
            return;
        }
        
        if (isChemicalRadioactive(stack)) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("tooltip.radioactive.chemical")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            tooltip.add(Component.translatable("tooltip.radioactive.storage_mode")
                .withStyle(ChatFormatting.GOLD));
        }
    }
}
