package com.buuz135.functionalstorage.block.tile;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;
import com.buuz135.functionalstorage.chemical.BigChemicalHandler;
import com.buuz135.functionalstorage.chemical.ChemicalCapabilities;
import com.buuz135.functionalstorage.client.gui.ChemicalDrawerInfoGuiAddon;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.UpgradeItem;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Chemical drawer tile entity that handles chemical storage and automation.
 * Based on FluidDrawerTile patterns but adapted for Mekanism chemicals.
 */
public class ChemicalDrawerTile extends ControllableDrawerTile<ChemicalDrawerTile> {

    @Save
    public BigChemicalHandler chemicalHandler;
    private final FunctionalStorage.DrawerType type;

    public ChemicalDrawerTile(BasicTileBlock<ChemicalDrawerTile> base, BlockEntityType<ChemicalDrawerTile> blockEntityType, 
                             BlockPos pos, BlockState state, FunctionalStorage.DrawerType type) {
        super(base, blockEntityType, pos, state);
        this.type = type;
        this.chemicalHandler = new BigChemicalHandler(type.getSlots(), getTankCapacity(getStorageMultiplier())) {
            @Override
            public @NotNull ChemicalStack getChemicalInTank(int tank) {
                ChemicalStack stack = super.getChemicalInTank(tank);
                if (!stack.isEmpty() && isDrawerCreative()) stack.setAmount(Long.MAX_VALUE);
                return stack;
            }
            
            @Override
            public long getChemicalTankCapacity(int tank) {
                return isDrawerCreative() ? Long.MAX_VALUE : chemicalHandler.getCapacity();
            }
            
            @Override
            public void onChange() {
                syncObject(chemicalHandler);
            }

            @Override
            public boolean isDrawerLocked() {
                return isLocked();
            }

            @Override
            public boolean isDrawerVoid() {
                return isVoid();
            }

            @Override
            public boolean isDrawerCreative() {
                return isCreative();
            }
        };
        
        // Set tile reference for radioactive upgrade checking
        this.chemicalHandler.setTile(this);
    }

    private long getTankCapacity(int storageMultiplier) {
        long maxCap = ((type.getSlotAmount() / 64)) * 1000L * storageMultiplier;
        return maxCap; // No casting to int - keep as long for chemicals
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initClient() {
        super.initClient();
        var slotName = "";
        if (type.getSlots() == 2) {
            slotName = "_2";
        }
        if (type.getSlots() == 4) {
            slotName = "_4";
        }
        String finalSlotName = slotName;
        addGuiAddonFactory(() -> new ChemicalDrawerInfoGuiAddon(64, 16,
                com.buuz135.functionalstorage.util.Utils.resourceLocation(FunctionalStorage.MOD_ID, "textures/block/chemical_front" + finalSlotName + ".png"),
                type.getSlots(),
                type.getSlotPosition(),
                this::getChemicalHandler,
                integer -> getChemicalHandler().getChemicalTankCapacity(integer)
        ));
    }

    @Override
    public double getStorageDiv() {
        return FunctionalStorageConfig.FLUID_DIVISOR;
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState stateOwn, ChemicalDrawerTile blockEntity) {
        super.serverTick(level, pos, stateOwn, blockEntity);
        if (level.getGameTime() % FunctionalStorageConfig.UPGRADE_TICK == 0) {
            for (int i = 0; i < this.getUtilityUpgrades().getSlots(); i++) {
                var stack = this.getUtilityUpgrades().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    var item = stack.getItem();
                    if (item.equals(FunctionalStorage.PUSHING_UPGRADE.get())) {
                        var direction = UpgradeItem.getDirection(stack);
                        var otherChemicalHandler = level.getCapability(ChemicalCapabilities.CHEMICAL.block(), pos.relative(direction), direction.getOpposite());
                        if (otherChemicalHandler != null) {
                            for (int tankId = 0; tankId < this.getChemicalHandler().getChemicalTanks(); tankId++) {
                                var chemicalTank = this.chemicalHandler.getTankList()[tankId];
                                if (chemicalTank.getStack().isEmpty()) continue;
                                
                                var extracted = chemicalTank.extract(FunctionalStorageConfig.UPGRADE_PUSH_FLUID, Action.SIMULATE, AutomationType.EXTERNAL);
                                if (extracted.isEmpty()) continue;
                                
                                var remainingAfterInsert = otherChemicalHandler.insertChemical(extracted, Action.EXECUTE);
                                var actualInserted = extracted.getAmount() - remainingAfterInsert.getAmount();
                                
                                if (actualInserted > 0) {
                                    chemicalTank.extract(actualInserted, Action.EXECUTE, AutomationType.EXTERNAL);
                                    this.chemicalHandler.onChange();
                                    break;
                                }
                            }
                        }
                    }
                    if (item.equals(FunctionalStorage.PULLING_UPGRADE.get())) {
                        var direction = UpgradeItem.getDirection(stack);
                        var otherChemicalHandler = level.getCapability(ChemicalCapabilities.CHEMICAL.block(), pos.relative(direction), direction.getOpposite());
                        if (otherChemicalHandler != null) {
                            var extracted = otherChemicalHandler.extractChemical(FunctionalStorageConfig.UPGRADE_PULL_FLUID, Action.SIMULATE);
                            if (!extracted.isEmpty()) {
                                var remainingAfterInsert = this.chemicalHandler.insertChemical(extracted, Action.EXECUTE);
                                var actualInserted = extracted.getAmount() - remainingAfterInsert.getAmount();
                                
                                if (actualInserted > 0) {
                                    otherChemicalHandler.extractChemical(actualInserted, Action.EXECUTE);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public IChemicalHandler getChemicalHandler(@Nullable Direction direction) {
        return chemicalHandler;
    }

    public BigChemicalHandler getChemicalHandler() {
        return chemicalHandler;
    }

    public FunctionalStorage.DrawerType getDrawerType() {
        return type;
    }

    /**
     * Checks if the chemical drawer has a radioactive upgrade installed
     */
    public boolean hasRadioactiveUpgrade() {
        return isRadioactive(); // Delegate to parent class method
    }

    /**
     * Gets the radioactive upgrade stack if present
     */
    public ItemStack getRadioactiveUpgrade() {
        if (!hasRadioactiveUpgrade()) return ItemStack.EMPTY;
        
        if (getUtilitySlotAmount() > 0) {
            for (int i = 0; i < getUtilityUpgrades().getSlots(); i++) {
                ItemStack stack = getUtilityUpgrades().getStackInSlot(i);
                if (!stack.isEmpty() && FunctionalStorage.MEKANISM_LOADED && 
                    stack.getItem() == FunctionalStorage.RADIOACTIVE_UPGRADE.get()) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Updates the chemical handler when radioactive mode changes
     */
    public void updateChemicalHandlerRadioactiveMode() {
        if (chemicalHandler != null) {
            chemicalHandler.updateRadioactiveMode();
        }
    }
    
    @Override
    public void setNeedsUpgradeCache(boolean needsUpgradeCache) {
        super.setNeedsUpgradeCache(needsUpgradeCache);
        
        // Update chemical handler radioactive mode when upgrade cache changes
        if (needsUpgradeCache) {
            // Update immediately since upgrade cache will be updated on next access
            updateChemicalHandlerRadioactiveMode();
        }
    }
    
    /**
     * Validates if a chemical can be inserted (used by automation)
     */
    public boolean canInsertChemical(ChemicalStack stack) {
        if (chemicalHandler == null) return false;
        
        // Test insertion without actually inserting
        ChemicalStack remainder = chemicalHandler.insertChemical(stack, Action.SIMULATE);
        
        // Can insert if some amount would be accepted
        return remainder.getAmount() < stack.getAmount();
    }
    
    /**
     * Gets validation error message for chemical insertion
     */
    public Component getChemicalInsertionError(ChemicalStack stack) {
        if (stack.isEmpty()) return Component.empty();
        
        // Use direct isRadioactive() method
        boolean isRadioactive = stack.isRadioactive();
        boolean hasUpgrade = hasRadioactiveUpgrade();
        
        if (isRadioactive && !hasUpgrade) {
            return Component.translatable("drawer.chemical.radioactive_requires_upgrade")
                .withStyle(ChatFormatting.RED);
        }
        
        if (!isRadioactive && hasUpgrade) {
            return Component.translatable("drawer.chemical.radioactive_mode_only")
                .withStyle(ChatFormatting.YELLOW);
        }
        
        return Component.empty();
    }

    @Override
    public void setLocked(boolean locked) {
        super.setLocked(locked);
        this.chemicalHandler.lockHandler();
        syncObject(this.chemicalHandler);
    }

    @Override
    public int getBaseSize(int lost) {
        return type.getSlotAmount();
    }

    @Override
    public int getStorageSlotAmount() {
        return 4;
    }

    @NotNull
    @Override
    public ChemicalDrawerTile getSelf() {
        return this;
    }

    @Override
    public InventoryComponent<ControllableDrawerTile<ChemicalDrawerTile>> getStorageUpgradesConstructor() {
        return new InventoryComponent<ControllableDrawerTile<ChemicalDrawerTile>>("storage_upgrades", 10, 70, getStorageSlotAmount()) {
            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (isStorageUpgradeLocked()) return ItemStack.EMPTY;
                ItemStack stack = this.getStackInSlot(slot);
                if (stack.getItem() instanceof StorageUpgradeItem) {
                    int mult = 1;
                    for (int i = 0; i < getStorageUpgrades().getSlots(); i++) {
                        if (getStorageUpgrades().getStackInSlot(i).getItem() instanceof StorageUpgradeItem) {
                            if (i == slot) continue;
                            var calculated = ((StorageUpgradeItem) getStorageUpgrades().getStackInSlot(i).getItem()).getStorageMultiplier() / getStorageDiv();
                            if (mult == 1)
                                mult = (int) calculated;
                            else
                                mult *= calculated;
                        }
                    }
                    for (int i = 0; i < getChemicalHandler().getChemicalTanks(); i++) {
                        if (getChemicalHandler().getChemicalInTank(i).isEmpty()) continue;
                        if (getChemicalHandler().getChemicalInTank(i).getAmount() > getTankCapacity(mult)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
                return super.extractItem(slot, amount, simulate);
            }
        }
                .setInputFilter((stack, integer) -> {
                    if (isStorageUpgradeLocked()) return false;
                    if (stack.getItem().equals(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.IRON).get())) {
                        return false;
                    }
                    return stack.getItem() instanceof UpgradeItem && ((UpgradeItem) stack.getItem()).getType() == UpgradeItem.Type.STORAGE;
                })
                .setOnSlotChanged((stack, integer) -> {
                    setNeedsUpgradeCache(true);
                    this.chemicalHandler.setCapacity(getTankCapacity(getStorageMultiplier()));
                    syncObject(this.chemicalHandler);
                })
                .setSlotLimit(1);
    }

    @Override
    public boolean isEverythingEmpty() {
        for (int i = 0; i < getChemicalHandler().getChemicalTanks(); i++) {
            if (!getChemicalHandler().getChemicalInTank(i).isEmpty()) return false;
        }
        for (int i = 0; i < this.getStorageUpgrades().getSlots(); i++) {
            if (!this.getStorageUpgrades().getStackInSlot(i).isEmpty()) return false;
        }
        for (int i = 0; i < this.getUtilityUpgrades().getSlots(); i++) {
            if (!this.getUtilityUpgrades().getStackInSlot(i).isEmpty()) return false;
        }
        if (isLocked()) {
            for (ChemicalStack filterStack : getChemicalHandler().getFilterStack()) {
                if (!filterStack.isEmpty()) return false;
            }
        }
        return true;
    }

    public boolean isInventoryEmpty() {
        for (int i = 0; i < getChemicalHandler().getChemicalTanks(); i++) {
            if (!getChemicalHandler().getChemicalInTank(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public InteractionResult onSlotActivated(Player playerIn, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ, int slot) {
        ItemStack stack = playerIn.getItemInHand(hand);
        if (stack.getItem().equals(FunctionalStorage.CONFIGURATION_TOOL.get()) || stack.getItem().equals(FunctionalStorage.LINKING_TOOL.get()))
            return InteractionResult.PASS;
        if (slot != -1 && !playerIn.getItemInHand(hand).isEmpty()) {
            var interactionResult = Optional.ofNullable(stack.getCapability(ChemicalCapabilities.CHEMICAL.item())).map(iChemicalHandlerItem -> Optional.ofNullable(playerIn.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY)).map(iItemHandler -> {
                // Try to transfer chemical from container to drawer
                var extracted = iChemicalHandlerItem.extractChemical(Long.MAX_VALUE, Action.SIMULATE);
                if (!extracted.isEmpty()) {
                    var remaining = this.chemicalHandler.getTankList()[slot].insert(extracted, Action.SIMULATE, AutomationType.MANUAL);
                    long transferAmount = extracted.getAmount() - remaining.getAmount();
                    if (transferAmount > 0) {
                        // Execute the transfer
                        var actualExtracted = iChemicalHandlerItem.extractChemical(transferAmount, Action.EXECUTE);
                        this.chemicalHandler.getTankList()[slot].insert(actualExtracted, Action.EXECUTE, AutomationType.MANUAL);
                        this.chemicalHandler.onChange();
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.PASS;
            }).orElse(InteractionResult.PASS)).orElse(InteractionResult.PASS);
            if (interactionResult == InteractionResult.SUCCESS) {
                return interactionResult;
            }
        }
        return super.onSlotActivated(playerIn, hand, facing, hitX, hitY, hitZ, slot);
    }

    @Override
    public void onClicked(Player playerIn, int slot) {
        ItemStack stack = playerIn.getItemInHand(InteractionHand.MAIN_HAND);
        if (slot != -1 && !stack.isEmpty()) {
            Optional.ofNullable(stack.getCapability(ChemicalCapabilities.CHEMICAL.item())).ifPresent(iChemicalHandlerItem -> {
                Optional.ofNullable(playerIn.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY)).ifPresent(iItemHandler -> {
                    // Try to transfer chemical from drawer to container
                    var drawerTank = this.chemicalHandler.getTankList()[slot];
                    if (!drawerTank.getStack().isEmpty()) {
                        var extracted = drawerTank.extract(Long.MAX_VALUE, Action.SIMULATE, AutomationType.MANUAL);
                        if (!extracted.isEmpty()) {
                            var remaining = iChemicalHandlerItem.insertChemical(extracted, Action.SIMULATE);
                            long transferAmount = extracted.getAmount() - remaining.getAmount();
                            if (transferAmount > 0) {
                                // Execute the transfer
                                var actualExtracted = drawerTank.extract(transferAmount, Action.EXECUTE, AutomationType.MANUAL);
                                iChemicalHandlerItem.insertChemical(actualExtracted, Action.EXECUTE);
                                this.chemicalHandler.onChange();
                            }
                        }
                    }
                });
            });
        }
    }

    protected void onStorageUpgradeChanged() {
        this.chemicalHandler.setCapacity(getTankCapacity(getStorageMultiplier()));
    }

    protected void onUtilityUpgradeChanged() {
        // Chemical-specific utility upgrade handling if needed
    }
}
