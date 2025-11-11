package com.buuz135.functionalstorage.block;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ChemicalDrawerTile;
import com.buuz135.functionalstorage.chemical.ChemicalCapabilities;
import com.buuz135.functionalstorage.chemical.ChemicalUtils;
import com.buuz135.functionalstorage.client.item.ChemicalDrawerISTER;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.util.NumberUtils;
import com.hrznstudio.titanium.block.RotatableBlock;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import com.hrznstudio.titanium.tab.TitaniumTab;
import com.hrznstudio.titanium.util.TileUtil;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.BlockCapability;
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
                var tileTag = itemStack.get(FSAttachments.TILE).getCompound("chemicalHandler");
                for (String key : tileTag.getAllKeys()) {
                    if (key.equals("Capacity")) continue;
                    var tankCompound = tileTag.getCompound(key);
                    if (!tankCompound.isEmpty()) {
                        var stack = ChemicalUtils.deserializeChemical(provider, tankCompound);
                        if (!stack.isEmpty()) {
                            var component = Component.literal("")
                                    .append(Component.literal(stack.getChemical().toString()).withStyle(ChatFormatting.WHITE))
                                    .append(Component.literal(": " + NumberUtils.getFormatedFluidBigNumber(stack.getAmount())).withStyle(ChatFormatting.GRAY));
                            components.add(component);
                        }
                    }
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
    public int getAnalogOutputSignal(BlockState blockState, net.minecraft.world.level.Level level, BlockPos blockPos) {
        ChemicalDrawerTile tile = TileUtil.getTileEntity(level, blockPos, ChemicalDrawerTile.class).orElse(null);
        if (tile != null) {
            int redstoneSlot = 0;
            for (int i = 0; i < tile.getUtilityUpgrades().getSlots(); i++) {
                var stack = tile.getUtilityUpgrades().getStackInSlot(i);
                if (stack.getItem().equals(FunctionalStorage.REDSTONE_UPGRADE.get()) && stack.has(FSAttachments.SLOT)) {
                    redstoneSlot = stack.get(FSAttachments.SLOT);
                }
            }
            if (redstoneSlot < tile.getChemicalHandler().getChemicalTanks()) {
                long stored = tile.getChemicalHandler().getChemicalInTank(redstoneSlot).getAmount();
                long capacity = tile.getChemicalHandler().getChemicalTankCapacity(redstoneSlot);
                if (capacity > 0) {
                    return (int) (stored * 15 / capacity);
                }
            }
        }
        return 0;
    }
}
