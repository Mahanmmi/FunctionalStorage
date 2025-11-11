package com.buuz135.functionalstorage.client.gui;

import com.buuz135.functionalstorage.chemical.BigChemicalHandler;
import com.buuz135.functionalstorage.util.NumberUtils;
import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * GUI addon for displaying chemical information in chemical drawers.
 * Based on FluidDrawerInfoGuiAddon but adapted for Mekanism chemicals.
 */
public class ChemicalDrawerInfoGuiAddon extends BasicScreenAddon {

    private final ResourceLocation gui;
    private final int slotAmount;
    private final Function<Integer, Pair<Integer, Integer>> slotPosition;
    private final Supplier<BigChemicalHandler> chemicalHandlerSupplier;
    private final Function<Integer, Long> slotMaxAmount;

    public ChemicalDrawerInfoGuiAddon(int posX, int posY, ResourceLocation gui, int slotAmount, 
                                     Function<Integer, Pair<Integer, Integer>> slotPosition, 
                                     Supplier<BigChemicalHandler> chemicalHandlerSupplier, 
                                     Function<Integer, Long> slotMaxAmount) {
        super(posX, posY);
        this.gui = gui;
        this.slotAmount = slotAmount;
        this.slotPosition = slotPosition;
        this.chemicalHandlerSupplier = chemicalHandlerSupplier;
        this.slotMaxAmount = slotMaxAmount;
    }

    public static Rect2i getSizeForSlots(int currentSlot, int slotAmount) {
        if (slotAmount == 1) {
            return new Rect2i(9, 9, 30, 30);
        }
        if (slotAmount == 2) {
            if (currentSlot == 0) return new Rect2i(0, 30, 48, 13);
            if (currentSlot == 1) return new Rect2i(0, 6, 48, 13);
        }
        if (slotAmount == 4) {
            if (currentSlot == 0) return new Rect2i(30, 30, 16, 16);
            if (currentSlot == 1) return new Rect2i(2, 30, 16, 16);
            if (currentSlot == 2) return new Rect2i(30, 2, 16, 16);
            if (currentSlot == 3) return new Rect2i(2, 2, 16, 16);
        }
        return new Rect2i(0, 0, 0, 0);
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Screen screen, IAssetProvider provider, int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, gui);
        guiGraphics.blit(gui, guiX + getPosX(), guiY + getPosY(), 0, 0, 48, 48);
        var handler = chemicalHandlerSupplier.get();
        if (handler != null) {
            for (int i = 0; i < slotAmount; i++) {
                var chemicalStack = handler.getChemicalInTank(i);
                if (!chemicalStack.isEmpty()) {
                    var pos = slotPosition.apply(i);
                    var area = getSizeForSlots(i, slotAmount);
                    
                    // Render chemical using Mekanism's renderer
                    try {
                        // Use Mekanism's chemical rendering if available
                        int color = chemicalStack.getChemical().getTint();
                        
                        // Render colored rectangle representing the chemical
                        float red = ((color >> 16) & 0xFF) / 255.0F;
                        float green = ((color >> 8) & 0xFF) / 255.0F;
                        float blue = (color & 0xFF) / 255.0F;
                        
                        RenderSystem.setShaderColor(red, green, blue, 1.0F);
                        guiGraphics.fill(
                            guiX + getPosX() + pos.getLeft() + area.getX(),
                            guiY + getPosY() + pos.getRight() + area.getY(),
                            guiX + getPosX() + pos.getLeft() + area.getX() + area.getWidth(),
                            guiY + getPosY() + pos.getRight() + area.getY() + area.getHeight(),
                            0xFF000000 | color
                        );
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    } catch (Exception e) {
                        // Fallback to simple colored rectangle if Mekanism rendering fails
                        guiGraphics.fill(
                            guiX + getPosX() + pos.getLeft() + area.getX(),
                            guiY + getPosY() + pos.getRight() + area.getY(),
                            guiX + getPosX() + pos.getLeft() + area.getX() + area.getWidth(),
                            guiY + getPosY() + pos.getRight() + area.getY() + area.getHeight(),
                            0xFF666666 // Gray fallback
                        );
                    }
                }
            }
        }
    }

    @Override
    public void drawForegroundLayer(GuiGraphics guiGraphics, Screen screen, IAssetProvider provider, int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        var handler = chemicalHandlerSupplier.get();
        if (handler != null) {
            for (int i = 0; i < slotAmount; i++) {
                var pos = slotPosition.apply(i);
                var area = getSizeForSlots(i, slotAmount);
                var chemicalStack = handler.getChemicalInTank(i);
                
                // Check if mouse is over this slot
                if (mouseX >= (guiX + getPosX() + pos.getLeft()) && 
                    mouseX < (guiX + getPosX() + pos.getLeft() + area.getWidth()) &&
                    mouseY >= (guiY + getPosY() + pos.getRight()) && 
                    mouseY < (guiY + getPosY() + pos.getRight() + area.getHeight())) {
                    
                    var tooltip = new ArrayList<Component>();
                    
                    if (!chemicalStack.isEmpty()) {
                        // Chemical name
                        tooltip.add(Component.literal(chemicalStack.getChemical().toString())
                                .withStyle(ChatFormatting.WHITE));
                        
                        // Amount with formatting for long values (use fluid formatting for chemicals)
                        var amount = NumberUtils.getFormatedFluidBigNumber(chemicalStack.getAmount());
                        var capacity = NumberUtils.getFormatedFluidBigNumber(slotMaxAmount.apply(i));
                        tooltip.add(Component.literal(amount + " / " + capacity)
                                .withStyle(ChatFormatting.GRAY));
                        
                        // Chemical properties
                        if (chemicalStack.isRadioactive()) {
                            tooltip.add(Component.literal("Radioactive")
                                    .withStyle(ChatFormatting.RED));
                        }
                    } else {
                        tooltip.add(Component.literal("Empty")
                                .withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.literal("0 / " + NumberUtils.getFormatedFluidBigNumber(slotMaxAmount.apply(i)))
                                .withStyle(ChatFormatting.GRAY));
                    }
                    
                    // Convert Component list to FormattedCharSequence list for tooltip rendering
                    var lines = tooltip.stream()
                            .map(component -> component.getVisualOrderText())
                            .toList();
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, lines, mouseX - guiX, mouseY - guiY);
                }
            }
        }
    }

    @Override
    public int getXSize() {
        return 0;
    }

    @Override
    public int getYSize() {
        return 0;
    }
}
