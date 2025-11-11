package com.buuz135.functionalstorage.chemical;

import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;

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
        return ChemicalStack.OPTIONAL_CODEC.decode(
            RegistryOps.create(NbtOps.INSTANCE, provider), tag
        ).getOrThrow().getFirst();
    }
}
