package com.buuz135.functionalstorage.chemical;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Chemical capabilities mirroring Mekanism's capability structure.
 * Uses the same resource location as Mekanism so the game correctly links them.
 * Based on Applied-Mekanistics pattern.
 */
public class ChemicalCapabilities {

    private ChemicalCapabilities() {
    }

    /**
     * Chemical handler capability that mirrors Mekanism's Capabilities.CHEMICAL
     * Uses Mekanism's resource location "mekanism:chemical_handler"
     */
    public static final CapSet<IChemicalHandler> CHEMICAL = new CapSet<>(
        ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "chemical_handler"),
        IChemicalHandler.class
    );

    /**
     * Capability set containing both block and item capabilities
     */
    public record CapSet<T>(BlockCapability<T, @Nullable Direction> block, ItemCapability<T, Void> item) {
        public CapSet(ResourceLocation name, Class<T> handlerClass) {
            this(
                BlockCapability.createSided(name, handlerClass),
                ItemCapability.createVoid(name, handlerClass)
            );
        }
    }
}
