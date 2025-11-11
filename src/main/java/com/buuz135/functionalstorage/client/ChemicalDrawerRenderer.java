package com.buuz135.functionalstorage.client;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.client.FunctionalStorageClientConfig;
import com.buuz135.functionalstorage.block.tile.ChemicalDrawerTile;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.chemical.BigChemicalHandler;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.buuz135.functionalstorage.util.NumberUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Renderer for chemical drawers, displaying chemical content in 3D world.
 * Based on FluidDrawerRenderer patterns but adapted for Mekanism chemicals.
 */
public class ChemicalDrawerRenderer implements BlockEntityRenderer<ChemicalDrawerTile> {

    public static void renderChemicalStack(PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLight, int combinedOverlay, ChemicalStack stack, long amount, long maxAmount, float scale, ControllableDrawerTile.DrawerOptions options, AABB bounds, boolean halfText, boolean isSmallBar) {
        if (options.isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER)) {
            matrixStack.pushPose();


            ResourceLocation texture = stack.getChemical().getIcon();
            TextureAtlasSprite still = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
            VertexConsumer builder = bufferIn.getBuffer(RenderType.translucent());

            int chemicalColor = stack.getChemicalTint();
            float[] color = decomposeColorF(chemicalColor);
            float red = color[1];
            float green = color[2];
            float blue = color[3];
            // Handle gaseous chemicals with different alpha (following plan requirement)
            float alpha = amount == 0 ? 0.3f : (stack.getChemical().isGaseous() ? Math.min(1.0f, color[0] + 0.2f) : color[0]);

            float x1 = (float) bounds.minX;
            float x2 = (float) bounds.maxX;
            float y1 = (float) bounds.minY;
            float y2 = (float) bounds.maxY;
            float z1 = (float) bounds.minZ;
            float z2 = (float) bounds.maxZ;
            float bx1 = (float) bounds.minX * 1.0f;
            float bx2 = (float) bounds.maxX * 1.0f;
            float by1 = (float) bounds.minY * 1.0f;
            float by2 = (float) bounds.maxY * 1.0f;
            float bz1 = (float) bounds.minZ * 1.0f;
            float bz2 = (float) bounds.maxZ * 1.0f;

            if (amount > 0 && maxAmount > 0) {
                float filledVolume = (float) amount / (float) maxAmount;
                y2 = y1 + filledVolume * (y2 - y1);
                by2 = by1 + filledVolume * (by2 - by1);
            }

            Matrix4f posMat = matrixStack.last().pose();

            // TOP face - exact FluidDrawerRenderer pattern
            {
                float u1 = still.getU(bx1);
                float u2 = still.getU(bx2);
                float v1 = still.getV(bz1);
                float v2 = still.getV(bz2);
                builder.addVertex(posMat, x1, y2, z2).setColor(red, green, blue, alpha).setUv(u1, v2).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(0f, 1f, 0f);
                builder.addVertex(posMat, x2, y2, z2).setColor(red, green, blue, alpha).setUv(u2, v2).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(0f, 1f, 0f);
                builder.addVertex(posMat, x2, y2, z1).setColor(red, green, blue, alpha).setUv(u2, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(0f, 1f, 0f);
                builder.addVertex(posMat, x1, y2, z1).setColor(red, green, blue, alpha).setUv(u1, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(0f, 1f, 0f);
            }

            // FRONT face
            {
                float u1 = still.getU(bx1);
                float u2 = still.getU(bx2);
                float v1 = still.getV(by1);
                float v2 = still.getV(by2);
                builder.addVertex(posMat, x2, y1, z2).setColor(red, green, blue, alpha).setUv(u2, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(0f, 0f, 1f);
                builder.addVertex(posMat, x2, y2, z2).setColor(red, green, blue, alpha).setUv(u2, v2).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(0f, 0f, 1f);
                builder.addVertex(posMat, x1, y2, z2).setColor(red, green, blue, alpha).setUv(u1, v2).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(0f, 0f, 1f);
                builder.addVertex(posMat, x1, y1, z2).setColor(red, green, blue, alpha).setUv(u1, v1).setOverlay(combinedOverlay).setLight(combinedLight).setNormal(0f, 0f, 1f);
            }
            matrixStack.popPose();
        }

        if (options.isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS)) {
            matrixStack.pushPose();
            matrixStack.translate(0.5, 0.84, 0.97);
            if (halfText) matrixStack.translate(-0.25, 0, 0);
            DrawerRenderer.renderText(matrixStack, bufferIn, combinedOverlay, Component.literal(ChatFormatting.WHITE + "" + NumberUtils.getFormatedFluidBigNumber(amount)), Direction.NORTH, scale);
            matrixStack.popPose();
        }
        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.453, 0.97);
        if (halfText) {
            matrixStack.scale(0.5f, 0.65f, 0.5f);
            matrixStack.translate(-0.5, -0.18, 0);
        }
        DrawerRenderer.renderIndicator(matrixStack, bufferIn, combinedLight, combinedOverlay, Math.min(1, amount / (float) maxAmount), options);
        matrixStack.popPose();
    }

    public static float[] decomposeColorF(int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        if (alpha == 0) alpha = 1.0F; // Default to opaque if no alpha specified
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        return new float[]{alpha, red, green, blue};
    }



    @Override
    public void render(ChemicalDrawerTile tile, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if (!FunctionalStorage.MEKANISM_LOADED) return;
        
        // Use same distance culling pattern as FluidDrawerRenderer
        if (net.minecraft.client.Minecraft.getInstance().player != null && !tile.getBlockPos().closerThan(net.minecraft.client.Minecraft.getInstance().player.getOnPos(), FunctionalStorageClientConfig.DRAWER_RENDER_RANGE)) {
            return;
        }
        
        matrixStack.pushPose();
        Direction facing = tile.getFacingDirection();
        matrixStack.mulPose(Axis.YP.rotationDegrees(-180));
        if (facing == Direction.NORTH) {
            matrixStack.translate(-1, 0, -1);
        }
        if (facing == Direction.EAST) {
            matrixStack.translate(0, 0, -1);
            matrixStack.mulPose(Axis.YP.rotationDegrees(-90));
        }
        if (facing == Direction.SOUTH) {
            matrixStack.mulPose(Axis.YP.rotationDegrees(-180));
        }
        if (facing == Direction.WEST) {
            matrixStack.translate(-1, 0, 0);
            matrixStack.mulPose(Axis.YP.rotationDegrees(90));
        }
        combinedLightIn = net.minecraft.client.renderer.LevelRenderer.getLightColor(tile.getLevel(), tile.getBlockPos().relative(facing));

        if (tile.getDrawerType() == FunctionalStorage.DrawerType.X_1)
            render1Slot(matrixStack, bufferIn, combinedLightIn, combinedOverlayIn, tile);
        if (tile.getDrawerType() == FunctionalStorage.DrawerType.X_2)
            render2Slot(matrixStack, bufferIn, combinedLightIn, combinedOverlayIn, tile);
        if (tile.getDrawerType() == FunctionalStorage.DrawerType.X_4)
            render4Slot(matrixStack, bufferIn, combinedLightIn, combinedOverlayIn, tile);

        DrawerRenderer.renderUpgrades(matrixStack, bufferIn, combinedLightIn, combinedOverlayIn, tile);
        matrixStack.popPose();
    }

    private void render1Slot(PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLight, int combinedOverlay, ChemicalDrawerTile tile) {
        BigChemicalHandler handler = tile.getChemicalHandler();
        ChemicalStack stack = handler.getChemicalInTank(0);
        ChemicalStack filterStack = handler.getFilterStack()[0];
        
        ChemicalStack displayStack = !stack.isEmpty() ? stack : filterStack;
        if (!displayStack.isEmpty()) {
            long amount = !stack.isEmpty() ? stack.getAmount() : 0;
            long maxAmount = handler.getChemicalTankCapacity(0);
            
            AABB bounds = new AABB(0.0625, 0.0625, 0.001, 0.9375, 0.9375, 0.125);
            renderChemicalStack(matrixStack, bufferIn, combinedLight, combinedOverlay, displayStack, 
                               amount, maxAmount, 1.0f, tile.getDrawerOptions(), bounds, false, false);
        }
    }

    private void render2Slot(PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLight, int combinedOverlay, ChemicalDrawerTile tile) {
        BigChemicalHandler handler = tile.getChemicalHandler();
        
        // Top slot
        ChemicalStack stack0 = handler.getChemicalInTank(0);
        ChemicalStack filterStack0 = handler.getFilterStack()[0];
        ChemicalStack displayStack0 = !stack0.isEmpty() ? stack0 : filterStack0;
        if (!displayStack0.isEmpty()) {
            long amount = !stack0.isEmpty() ? stack0.getAmount() : 0;
            long maxAmount = handler.getChemicalTankCapacity(0);
            AABB bounds = new AABB(0.0625, 0.5625, 0.001, 0.9375, 0.9375, 0.125);
            renderChemicalStack(matrixStack, bufferIn, combinedLight, combinedOverlay, displayStack0, 
                               amount, maxAmount, 0.75f, tile.getDrawerOptions(), bounds, true, true);
        }
        
        // Bottom slot
        ChemicalStack stack1 = handler.getChemicalInTank(1);
        ChemicalStack filterStack1 = handler.getFilterStack()[1];
        ChemicalStack displayStack1 = !stack1.isEmpty() ? stack1 : filterStack1;
        if (!displayStack1.isEmpty()) {
            long amount = !stack1.isEmpty() ? stack1.getAmount() : 0;
            long maxAmount = handler.getChemicalTankCapacity(1);
            AABB bounds = new AABB(0.0625, 0.0625, 0.001, 0.9375, 0.4375, 0.125);
            renderChemicalStack(matrixStack, bufferIn, combinedLight, combinedOverlay, displayStack1, 
                               amount, maxAmount, 0.75f, tile.getDrawerOptions(), bounds, true, true);
        }
    }

    private void render4Slot(PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLight, int combinedOverlay, ChemicalDrawerTile tile) {
        BigChemicalHandler handler = tile.getChemicalHandler();
        
        // Top-left
        ChemicalStack stack0 = handler.getChemicalInTank(0);
        ChemicalStack filterStack0 = handler.getFilterStack()[0];
        ChemicalStack displayStack0 = !stack0.isEmpty() ? stack0 : filterStack0;
        if (!displayStack0.isEmpty()) {
            long amount = !stack0.isEmpty() ? stack0.getAmount() : 0;
            long maxAmount = handler.getChemicalTankCapacity(0);
            AABB bounds = new AABB(0.5625, 0.5625, 0.001, 0.9375, 0.9375, 0.125);
            renderChemicalStack(matrixStack, bufferIn, combinedLight, combinedOverlay, displayStack0, 
                               amount, maxAmount, 0.5f, tile.getDrawerOptions(), bounds, true, true);
        }
        
        // Top-right
        ChemicalStack stack1 = handler.getChemicalInTank(1);
        ChemicalStack filterStack1 = handler.getFilterStack()[1];
        ChemicalStack displayStack1 = !stack1.isEmpty() ? stack1 : filterStack1;
        if (!displayStack1.isEmpty()) {
            long amount = !stack1.isEmpty() ? stack1.getAmount() : 0;
            long maxAmount = handler.getChemicalTankCapacity(1);
            AABB bounds = new AABB(0.0625, 0.5625, 0.001, 0.4375, 0.9375, 0.125);
            renderChemicalStack(matrixStack, bufferIn, combinedLight, combinedOverlay, displayStack1, 
                               amount, maxAmount, 0.5f, tile.getDrawerOptions(), bounds, true, true);
        }
        
        // Bottom-left
        ChemicalStack stack2 = handler.getChemicalInTank(2);
        ChemicalStack filterStack2 = handler.getFilterStack()[2];
        ChemicalStack displayStack2 = !stack2.isEmpty() ? stack2 : filterStack2;
        if (!displayStack2.isEmpty()) {
            long amount = !stack2.isEmpty() ? stack2.getAmount() : 0;
            long maxAmount = handler.getChemicalTankCapacity(2);
            AABB bounds = new AABB(0.5625, 0.0625, 0.001, 0.9375, 0.4375, 0.125);
            renderChemicalStack(matrixStack, bufferIn, combinedLight, combinedOverlay, displayStack2, 
                               amount, maxAmount, 0.5f, tile.getDrawerOptions(), bounds, true, true);
        }
        
        // Bottom-right
        ChemicalStack stack3 = handler.getChemicalInTank(3);
        ChemicalStack filterStack3 = handler.getFilterStack()[3];
        ChemicalStack displayStack3 = !stack3.isEmpty() ? stack3 : filterStack3;
        if (!displayStack3.isEmpty()) {
            long amount = !stack3.isEmpty() ? stack3.getAmount() : 0;
            long maxAmount = handler.getChemicalTankCapacity(3);
            AABB bounds = new AABB(0.0625, 0.0625, 0.001, 0.4375, 0.4375, 0.125);
            renderChemicalStack(matrixStack, bufferIn, combinedLight, combinedOverlay, displayStack3, 
                               amount, maxAmount, 0.5f, tile.getDrawerOptions(), bounds, true, true);
        }
    }
}
