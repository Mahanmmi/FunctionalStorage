package com.buuz135.functionalstorage.item;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ChemicalDrawerTile;
import com.buuz135.functionalstorage.chemical.BigChemicalHandler;
import com.buuz135.functionalstorage.util.StorageTags;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.ChatFormatting;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RadioactiveUpgradeItem extends UpgradeItem {

    public RadioactiveUpgradeItem(Properties properties) {
        super(properties, Type.UTILITY); // Uses existing UTILITY type
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        
        // Add radioactive upgrade specific warnings
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("upgrade.radioactive.warning").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("upgrade.radioactive.chemical_only").withStyle(ChatFormatting.RED));

        tooltip.add(Component.translatable("upgrade.radioactive.storage_info").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("upgrade.radioactive.safety").withStyle(ChatFormatting.DARK_RED));
    }

    /**
     * Validates if radioactive upgrade can be installed in the given chemical drawer
     */
    public static boolean canInstallRadioactiveUpgrade(ChemicalDrawerTile tile) {
        if (tile == null) return false;
        
        BigChemicalHandler handler = tile.getChemicalHandler();
        if (handler == null) return false;
        
        // Cannot install if non-radioactive chemicals are present
        for (int i = 0; i < handler.getChemicalTanks(); i++) {
            ChemicalStack stack = handler.getChemicalInTank(i);
            if (!stack.isEmpty()) {
                // Use direct isRadioactive() method
                boolean isRadioactive = stack.isRadioactive();
                if (!isRadioactive) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Validates if radioactive upgrade can be removed from the given chemical drawer
     */
    public static boolean canRemoveRadioactiveUpgrade(ChemicalDrawerTile tile) {
        if (tile == null) return false;
        
        BigChemicalHandler handler = tile.getChemicalHandler();
        if (handler == null) return false;
        
        // Cannot remove if radioactive chemicals are present
        for (int i = 0; i < handler.getChemicalTanks(); i++) {
            ChemicalStack stack = handler.getChemicalInTank(i);
            if (!stack.isEmpty()) {
                // Use direct isRadioactive() method
                boolean isRadioactive = stack.isRadioactive();
                if (isRadioactive) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Gets validation error message for upgrade installation
     */
    public static Component getInstallationError(ChemicalDrawerTile tile) {
        if (!canInstallRadioactiveUpgrade(tile)) {
            return Component.translatable("upgrade.radioactive.install_error").withStyle(ChatFormatting.RED);
        }
        return Component.empty();
    }

    /**
     * Gets validation error message for upgrade removal
     */
    public static Component getRemovalError(ChemicalDrawerTile tile) {
        if (!canRemoveRadioactiveUpgrade(tile)) {
            return Component.translatable("upgrade.radioactive.remove_error").withStyle(ChatFormatting.RED);
        }
        return Component.empty();
    }
    
    /**
     * Generates the radioactive upgrade recipe
     */
    public static void registerRecipe(RecipeOutput output) {
        // Pattern matches Mekanism's radioactive waste barrel: SIS / IDI / SIS
        // But center accepts any chemical drawer instead of air
        TitaniumShapedRecipeBuilder.shapedRecipe(FunctionalStorage.RADIOACTIVE_UPGRADE.get())
                .pattern("SIS").pattern("IDI").pattern("SIS")
                .define('S', ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/steel"))) // Steel ingots
                .define('I', ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/lead")))  // Lead ingots
                .define('D', StorageTags.CHEMICAL_DRAWER) // Any chemical drawer (1x1, 1x2, 2x2)
                .save(output);
    }
}
