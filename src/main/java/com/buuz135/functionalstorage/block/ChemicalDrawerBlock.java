package com.buuz135.functionalstorage.block;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ChemicalDrawerTile;
import com.buuz135.functionalstorage.chemical.ChemicalUtils;
import com.buuz135.functionalstorage.client.item.ChemicalDrawerISTER;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.util.NumberUtils;
import com.hrznstudio.titanium.block.RotatableBlock;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import com.hrznstudio.titanium.tab.TitaniumTab;
import com.hrznstudio.titanium.util.TileUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Chemical drawer block that handles chemical storage for Mekanism chemicals.
 * Based on FluidDrawerBlock patterns but adapted for chemical-specific functionality.
 */
public class ChemicalDrawerBlock extends Drawer<ChemicalDrawerTile> {

    private final FunctionalStorage.DrawerType type;

    public ChemicalDrawerBlock(FunctionalStorage.DrawerType type, Properties properties) {
        super("chemical_" + type.getSlots(), properties, ChemicalDrawerTile.class);
        this.type = type;
        setItemGroup(FunctionalStorage.TAB);
        registerDefaultState(defaultBlockState().setValue(RotatableBlock.FACING_HORIZONTAL, Direction.NORTH).setValue(DrawerBlock.LOCKED, false));
    }

    private static List<VoxelShape> getShapes(BlockState state, BlockGetter source, BlockPos pos, FunctionalStorage.DrawerType type) {
        List<VoxelShape> boxes = new ArrayList<>();
        DrawerBlock.CACHED_SHAPES.get(type).get(state.getValue(RotatableBlock.FACING_HORIZONTAL)).forEach(boxes::add);
        VoxelShape total = Shapes.block();
        boxes.add(total);
        return boxes;
    }

    @Override
    public BlockEntityType.BlockEntitySupplier<ChemicalDrawerTile> getTileEntityFactory() {
        return (blockPos, state) -> {
            BlockEntityType<ChemicalDrawerTile> entityType = (BlockEntityType<ChemicalDrawerTile>) FunctionalStorage.CHEMICAL_DRAWER_1.type().get();
            if (type == FunctionalStorage.DrawerType.X_2) {
                entityType = (BlockEntityType<ChemicalDrawerTile>) FunctionalStorage.CHEMICAL_DRAWER_2.type().get();
            }
            if (type == FunctionalStorage.DrawerType.X_4) {
                entityType = (BlockEntityType<ChemicalDrawerTile>) FunctionalStorage.CHEMICAL_DRAWER_4.type().get();
            }
            return new ChemicalDrawerTile(this, entityType, blockPos, state, type);
        };
    }

    @Override
    public List<VoxelShape> getBoundingBoxes(BlockState state, BlockGetter source, BlockPos pos) {
        return getShapes(state, source, pos, this.type);
    }

    @Override
    public Collection<VoxelShape> getHitShapes(BlockState state) {
        return DrawerBlock.CACHED_SHAPES.get(type).get(state.getValue(RotatableBlock.FACING_HORIZONTAL));
    }

    // Capability registration handled in FunctionalStorage constructor via RegisterCapabilitiesEvent

    @Override
    public void registerRecipe(RecipeOutput consumer) {
        // Get Basic Chemical Tank item safely using registry lookup
        var basicChemicalTank = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mekanism", "basic_chemical_tank"));
        
        if (type.getSlots() == 1) {
            TitaniumShapedRecipeBuilder.shapedRecipe(this)
                    .pattern("PPP").pattern("PCP").pattern("PPP")
                    .define('P', ItemTags.PLANKS)
                    .define('C', basicChemicalTank)
                    .save(consumer);
        }
        if (type.getSlots() == 2) {
            TitaniumShapedRecipeBuilder.shapedRecipe(this, 2)
                    .pattern("PCP").pattern("PPP").pattern("PCP")
                    .define('P', ItemTags.PLANKS)
                    .define('C', basicChemicalTank)
                    .save(consumer);
        }
        if (type.getSlots() == 4) {
            TitaniumShapedRecipeBuilder.shapedRecipe(this, 4)
                    .pattern("CPC").pattern("PPP").pattern("CPC")
                    .define('P', ItemTags.PLANKS)
                    .define('C', basicChemicalTank)
                    .save(consumer);
        }
    }

    public static class ChemicalDrawerItem extends BlockItem {

        private final ChemicalDrawerBlock drawerBlock;

        public ChemicalDrawerItem(ChemicalDrawerBlock block, Item.Properties props, TitaniumTab tab) {
            super(block, props);
            this.drawerBlock = block;
        }

        @Override
        public void appendHoverText(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Component> components, TooltipFlag tooltipFlag) {
            if (itemStack.has(FSAttachments.TILE)) {
                var provider = tooltipContext.registries();
                var tile = itemStack.get(FSAttachments.TILE);
                var tileTag = tile.getCompound("chemicalHandler");
                components.add(Component.translatable("drawer.block.contents").withStyle(ChatFormatting.GRAY));
                for (int i = 0; i < drawerBlock.type.getSlots(); i++) {
                    var tankCompound = tileTag.getCompound(String.valueOf(i));
                    if (!tankCompound.isEmpty()) {
                        var stack = ChemicalUtils.deserializeChemical(provider, tankCompound.getCompound("Stack"));
                        if (!stack.isEmpty()) {
                            int chemicalColor = stack.getChemicalTint();
                            // Show infinite amount for creative drawers
                            long displayAmount = (tile.contains("isCreative") && tile.getBoolean("isCreative")) ? Long.MAX_VALUE : stack.getAmount();
                            components.add(Component.literal(" - " + ChatFormatting.YELLOW + NumberUtils.getFormatedFluidBigNumber(displayAmount) + ChatFormatting.WHITE + " of ").append(stack.getChemical().getTextComponent().copy().withStyle(style -> style.withColor(chemicalColor))));
                        }
                    }
                }
                components.add(Component.translatable("drawer.block.upgrades").withStyle(ChatFormatting.GRAY));
                var anyupgrade = false;
                if (tile.contains("isCreative") && tile.getBoolean("isCreative")) {
                    components.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("drawer.block.upgrades.is_creative").withStyle(ChatFormatting.LIGHT_PURPLE)));
                    anyupgrade = true;
                }
                if (tile.contains("isVoid") && tile.getBoolean("isVoid")) {
                    components.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("drawer.block.upgrades.is_void").withStyle(ChatFormatting.BLUE)));
                    anyupgrade = true;
                }
                if (FunctionalStorage.MEKANISM_LOADED && tile.contains("isRadioactive") && tile.getBoolean("isRadioactive")) {
                    components.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("drawer.block.upgrades.is_radioactive").withStyle(ChatFormatting.YELLOW)));
                    anyupgrade = true;
                }
                if (!anyupgrade) {
                    components.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("drawer.block.upgrades.none").withStyle(ChatFormatting.GRAY)));
                }
            }
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return switch (drawerBlock.type) {
                        case X_2 -> ChemicalDrawerISTER.SLOT_2;
                        case X_4 -> ChemicalDrawerISTER.SLOT_4;
                        default -> ChemicalDrawerISTER.SLOT_1;
                    };
                }
            });
        }
    }

    @Override
    public int getSignal(BlockState p_60483_, BlockGetter blockGetter, BlockPos blockPos, Direction p_60486_) {
        ChemicalDrawerTile tile = TileUtil.getTileEntity(blockGetter, blockPos, ChemicalDrawerTile.class).orElse(null);
        if (tile != null) {
            for (int i = 0; i < tile.getUtilityUpgrades().getSlots(); i++) {
                ItemStack stack = tile.getUtilityUpgrades().getStackInSlot(i);
                if (stack.getItem().equals(FunctionalStorage.REDSTONE_UPGRADE.get())) {
                    // Calculate total capacity and stored amount across all tanks
                    long totalStored = 0;
                    long totalCapacity = 0;
                    
                    for (int tankIndex = 0; tankIndex < tile.getChemicalHandler().getChemicalTanks(); tankIndex++) {
                        totalStored += tile.getChemicalHandler().getChemicalInTank(tankIndex).getAmount();
                        totalCapacity += tile.getChemicalHandler().getChemicalTankCapacity(tankIndex);
                    }
                    
                    if (totalCapacity > 0) {
                        return (int) (totalStored * 15 / totalCapacity);
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, net.minecraft.world.level.Level level, BlockPos blockPos) {
        ChemicalDrawerTile tile = TileUtil.getTileEntity(level, blockPos, ChemicalDrawerTile.class).orElse(null);
        if (tile != null) {
            for (int i = 0; i < tile.getUtilityUpgrades().getSlots(); i++) {
                var stack = tile.getUtilityUpgrades().getStackInSlot(i);
                if (stack.getItem().equals(FunctionalStorage.REDSTONE_UPGRADE.get())) {
                    // Calculate total capacity and stored amount across all tanks
                    long totalStored = 0;
                    long totalCapacity = 0;
                    
                    for (int tankIndex = 0; tankIndex < tile.getChemicalHandler().getChemicalTanks(); tankIndex++) {
                        totalStored += tile.getChemicalHandler().getChemicalInTank(tankIndex).getAmount();
                        totalCapacity += tile.getChemicalHandler().getChemicalTankCapacity(tankIndex);
                    }
                    
                    if (totalCapacity > 0) {
                        return (int) (totalStored * 15 / totalCapacity);
                    }
                }
            }
        }
        return 0;
    }
}
