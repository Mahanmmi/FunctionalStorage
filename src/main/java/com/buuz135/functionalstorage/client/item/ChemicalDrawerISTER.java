package com.buuz135.functionalstorage.client.item;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.chemical.ChemicalUtils;
import com.buuz135.functionalstorage.client.ChemicalDrawerRenderer;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * Inventory/hand renderer for chemical drawers.
 * Based on FluidDrawerISTER patterns but adapted for chemical content.
 */
public class ChemicalDrawerISTER extends FunctionalStorageISTER {

    public static final ChemicalDrawerISTER SLOT_1 = new ChemicalDrawerISTER(FunctionalStorage.DrawerType.X_1);
    public static final ChemicalDrawerISTER SLOT_2 = new ChemicalDrawerISTER(FunctionalStorage.DrawerType.X_2);
    public static final ChemicalDrawerISTER SLOT_4 = new ChemicalDrawerISTER(FunctionalStorage.DrawerType.X_4);

    private final FunctionalStorage.DrawerType type;

    public ChemicalDrawerISTER(FunctionalStorage.DrawerType type) {
        this.type = type;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        // Empty implementation like FluidDrawerISTER
    }

    @Override
    public void renderByItem(HolderLookup.Provider access, @NotNull ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack matrix, @NotNull MultiBufferSource renderer, int light, int overlayLight) {
        var modelData = getData(stack);
        renderBlockItem(stack, displayContext, matrix, renderer, light, overlayLight, modelData);
        if (stack.has(FSAttachments.TILE)) {
            var options = new ControllableDrawerTile.DrawerOptions();
            options.deserializeNBT(access, stack.get(FSAttachments.TILE).getCompound("drawerOptions"));
            matrix.mulPose(Axis.YP.rotationDegrees(180));
            matrix.translate(-1, 0, -1);
            var tileTag = stack.get(FSAttachments.TILE).getCompound("chemicalHandler");
            
            if (type == FunctionalStorage.DrawerType.X_1) {
                ChemicalStack chemicalStack = deserialize(access, tileTag, 0);
                if (!chemicalStack.isEmpty()) {
                    long displayAmount = chemicalStack.getAmount();
                    AABB bounds = new AABB(1 / 16D, 1.25 / 16D, 1 / 16D, 15 / 16D, 1.25 / 16D + (chemicalStack.getAmount() / (double) chemicalStack.getAmount()) * (12.5 / 16D), 15 / 16D);
                    ChemicalDrawerRenderer.renderChemicalStack(matrix, renderer, light, overlayLight, chemicalStack, displayAmount, chemicalStack.getAmount(), 0.007f, options, bounds, false, false);
                }
            } else if (type == FunctionalStorage.DrawerType.X_2) {
                ChemicalStack chemicalStack = deserialize(access, tileTag, 0);
                if (!chemicalStack.isEmpty()) {
                    long displayAmount = chemicalStack.getAmount();
                    AABB bounds = new AABB(1 / 16D, 1.25 / 16D, 1 / 16D, 15 / 16D, 1.25 / 16D + (chemicalStack.getAmount() / (double) chemicalStack.getAmount()) * (5.5 / 16D), 15 / 16D);
                    ChemicalDrawerRenderer.renderChemicalStack(matrix, renderer, light, overlayLight, chemicalStack, displayAmount, chemicalStack.getAmount(), 0.007f, options, bounds, false, true);
                }
                chemicalStack = deserialize(access, tileTag, 1);
                if (!chemicalStack.isEmpty()) {
                    matrix.pushPose();
                    matrix.translate(0, 0.5, 0);
                    long displayAmount = chemicalStack.getAmount();
                    AABB bounds = new AABB(1 / 16D, 1.25 / 16D, 1 / 16D, 15 / 16D, 1.25 / 16D + (chemicalStack.getAmount() / (double) chemicalStack.getAmount()) * (5.5 / 16D), 15 / 16D);
                    ChemicalDrawerRenderer.renderChemicalStack(matrix, renderer, light, overlayLight, chemicalStack, displayAmount, chemicalStack.getAmount(), 0.007f, options, bounds, false, true);
                    matrix.popPose();
                }
            } else if (type == FunctionalStorage.DrawerType.X_4) {
                ChemicalStack chemicalStack = deserialize(access, tileTag, 0);
                if (!chemicalStack.isEmpty()) {
                    matrix.pushPose();
                    matrix.translate(0.5, 0, 0);
                    long displayAmount = chemicalStack.getAmount();
                    AABB bounds = new AABB(1 / 16D, 1.25 / 16D, 1 / 16D, 8 / 16D, 1.25 / 16D + (chemicalStack.getAmount() / (double) chemicalStack.getAmount()) * (5.5 / 16D), 15 / 16D);
                    ChemicalDrawerRenderer.renderChemicalStack(matrix, renderer, light, overlayLight, chemicalStack, displayAmount, chemicalStack.getAmount(), 0.007f, options, bounds, true, true);
                    matrix.popPose();
                }
                chemicalStack = deserialize(access, tileTag, 1);
                if (!chemicalStack.isEmpty()) {
                    matrix.pushPose();
                    long displayAmount = chemicalStack.getAmount();
                    AABB bounds = new AABB(1 / 16D, 1.25 / 16D, 1 / 16D, 8 / 16D, 1.25 / 16D + (chemicalStack.getAmount() / (double) chemicalStack.getAmount()) * (5.5 / 16D), 15 / 16D);
                    ChemicalDrawerRenderer.renderChemicalStack(matrix, renderer, light, overlayLight, chemicalStack, displayAmount, chemicalStack.getAmount(), 0.007f, options, bounds, true, true);
                    matrix.popPose();
                }
                chemicalStack = deserialize(access, tileTag, 2);
                if (!chemicalStack.isEmpty()) {
                    matrix.pushPose();
                    matrix.translate(0.5, 0.5, 0);
                    long displayAmount = chemicalStack.getAmount();
                    AABB bounds = new AABB(1 / 16D, 1.25 / 16D, 1 / 16D, 8 / 16D, 1.25 / 16D + (chemicalStack.getAmount() / (double) chemicalStack.getAmount()) * (5.5 / 16D), 15 / 16D);
                    ChemicalDrawerRenderer.renderChemicalStack(matrix, renderer, light, overlayLight, chemicalStack, displayAmount, chemicalStack.getAmount(), 0.007f, options, bounds, true, true);
                    matrix.popPose();
                }
                chemicalStack = deserialize(access, tileTag, 3);
                if (!chemicalStack.isEmpty()) {
                    matrix.pushPose();
                    matrix.translate(0, 0.5, 0);
                    long displayAmount = chemicalStack.getAmount();
                    AABB bounds = new AABB(1 / 16D, 1.25 / 16D, 1 / 16D, 8 / 16D, 1.25 / 16D + (chemicalStack.getAmount() / (double) chemicalStack.getAmount()) * (5.5 / 16D), 15 / 16D);
                    ChemicalDrawerRenderer.renderChemicalStack(matrix, renderer, light, overlayLight, chemicalStack, displayAmount, chemicalStack.getAmount(), 0.007f, options, bounds, true, true);
                    matrix.popPose();
                }
            }
        }
    }

    public static ChemicalStack deserialize(HolderLookup.Provider access, CompoundTag tileTag, int i) {
        var chemicalTag = tileTag.getCompound(String.valueOf(i));
        return ChemicalUtils.deserializeChemical(access, chemicalTag.getCompound("Stack"));
    }
}
