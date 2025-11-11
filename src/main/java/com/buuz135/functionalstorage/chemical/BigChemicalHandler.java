package com.buuz135.functionalstorage.chemical;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.util.Utils;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;


import java.util.function.Predicate;

/**
 * Chemical storage handler based on BigFluidHandler patterns.
 * Handles chemical storage, filtering, and tank operations for chemical drawers.
 */
public abstract class BigChemicalHandler implements IChemicalHandler, INBTSerializable<CompoundTag> {

    private CustomChemicalTank[] tanks;
    private ChemicalStack[] filterStack;
    private long capacity;
    
    private boolean radioactiveMode = false;
    
    // Reference to tile for radioactive upgrade checking
    protected com.buuz135.functionalstorage.block.tile.ControllableDrawerTile<?> tile;
    


    public BigChemicalHandler(int size, long capacity) {
        this.tanks = new CustomChemicalTank[size];
        this.filterStack = new ChemicalStack[size];
        for (int i = 0; i < this.tanks.length; i++) {
            this.filterStack[i] = ChemicalStack.EMPTY;
            int finalI = i;
            this.tanks[i] = new CustomChemicalTank(capacity, chemicalStack -> {
                if (isDrawerLocked()) {
                    return ChemicalStack.isSameChemical(chemicalStack, this.filterStack[finalI]);
                }
                return isValidChemical(chemicalStack);
            });
        }
        this.capacity = capacity;
    }

    protected boolean isValidChemical(ChemicalStack stack) {
        if (stack.isEmpty()) return false;
        return isChemicalCompatible(stack);
    }

    public CustomChemicalTank[] getTankList() {
        return this.tanks;
    }

    @Override
    public int getChemicalTanks() {
        return this.tanks.length;
    }

    @Override
    public @NotNull ChemicalStack getChemicalInTank(int tank) {
        return this.tanks[tank].getStack();
    }

    @Override
    public void setChemicalInTank(int tank, @NotNull ChemicalStack stack) {
        this.tanks[tank].setStack(stack);
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return this.capacity;
    }

    @Override
    public boolean isValid(int tank, @NotNull ChemicalStack stack) {
        return this.tanks[tank].isValid(stack);
    }

    @Override
    public @NotNull ChemicalStack insertChemical(int tank, @NotNull ChemicalStack stack, @NotNull Action action) {
        ChemicalStack result = this.tanks[tank].insert(stack, action, AutomationType.EXTERNAL);
        if (action == Action.EXECUTE) onChange();
        return result;
    }

    @Override
    public @NotNull ChemicalStack extractChemical(int tank, long amount, @NotNull Action action) {
        ChemicalStack result = this.tanks[tank].extract(amount, action, AutomationType.EXTERNAL);
        if (action == Action.EXECUTE) onChange();
        return result;
    }

    @Override
    public @NotNull ChemicalStack insertChemical(@NotNull ChemicalStack stack, @NotNull Action action) {
        if (stack.isEmpty()) return ChemicalStack.EMPTY;
        
        // Update radioactive mode before validation
        updateRadioactiveMode();
        
        // Validate chemical compatibility with current drawer mode
        if (!isChemicalCompatible(stack)) {
            // Complete rejection for incompatible chemicals
            return stack;
        }
        
        
        // Try existing tanks first - use same pattern as BigFluidHandler.fill()
        for (CustomChemicalTank tank : tanks) {
            if (!tank.getStack().isEmpty() && tank.insert(stack, Action.SIMULATE, AutomationType.EXTERNAL).getAmount() < stack.getAmount()) {
                ChemicalStack result = tank.insert(stack, action, AutomationType.EXTERNAL);
                if (action == Action.EXECUTE) onChange();
                return result;
            }
        }
        
        // Then try empty tanks
        for (CustomChemicalTank tank : tanks) {
            if (tank.getStack().isEmpty() && tank.insert(stack, Action.SIMULATE, AutomationType.EXTERNAL).getAmount() < stack.getAmount()) {
                ChemicalStack result = tank.insert(stack, action, AutomationType.EXTERNAL);
                if (action == Action.EXECUTE) onChange();
                return result;
            }
        }
        
        return stack;
    }

    @Override
    public @NotNull ChemicalStack extractChemical(long amount, @NotNull Action action) {
        for (CustomChemicalTank tank : tanks) {
            if (!tank.getStack().isEmpty()) {
                ChemicalStack result = tank.extract(amount, action, AutomationType.EXTERNAL);
                if (action == Action.EXECUTE) onChange();
                if (!result.isEmpty()) return result;
            }
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public @NotNull ChemicalStack extractChemical(@NotNull ChemicalStack stack, @NotNull Action action) {
        for (CustomChemicalTank tank : tanks) {
            if (!tank.getStack().isEmpty() && ChemicalStack.isSameChemical(tank.getStack(), stack)) {
                ChemicalStack result = tank.extract(stack.getAmount(), action, AutomationType.EXTERNAL);
                if (action == Action.EXECUTE) onChange();
                if (!result.isEmpty()) return result;
            }
        }
        return ChemicalStack.EMPTY;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
        for (CustomChemicalTank tank : this.tanks) {
            tank.setCapacity(capacity);
            if (!tank.getStack().isEmpty()) {
                long currentAmount = tank.getStack().getAmount();
                if (currentAmount > capacity) {
                    tank.getStack().setAmount(capacity);
                }
            }
        }
    }

    @Override
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        for (int i = 0; i < this.tanks.length; i++) {
            compoundTag.put(i + "", this.tanks[i].serializeNBT(provider));
            compoundTag.put("Locked" + i, ChemicalStack.OPTIONAL_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, provider), this.filterStack[i]).getOrThrow());
        }
        compoundTag.putLong("Capacity", this.capacity);
        
        // Save radioactive mode state
        compoundTag.putBoolean("RadioactiveMode", radioactiveMode);
        
        return compoundTag;
    }

    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag nbt) {
        this.capacity = nbt.getLong("Capacity");
        for (int i = 0; i < this.tanks.length; i++) {
            this.tanks[i].deserializeNBT(provider, nbt.getCompound(i + ""));
            this.tanks[i].setCapacity(this.capacity);
            this.filterStack[i] = ChemicalUtils.deserializeChemical(provider, nbt.getCompound("Locked" + i));
        }
        
        // Load radioactive mode state
        if (nbt.contains("RadioactiveMode")) {
            this.radioactiveMode = nbt.getBoolean("RadioactiveMode");
        }
        
        // Validate and synchronize state after loading
        validateAndSynchronizeAfterLoad();
    }
    
    /**
     * Validates and synchronizes the drawer state after loading from NBT.
     * This ensures that the radioactive mode, capacity, and content are all consistent.
     */
    private void validateAndSynchronizeAfterLoad() {
        // Skip if tile is not available yet (early loading phase)
        if (tile == null) {
            return;
        }
        
        // Update radioactive mode based on current upgrade state
        updateRadioactiveMode();
        
        // Validate existing content against current compatibility rules
        validateExistingContent();
    }
    
    /**
     * Validates existing chemical content against current compatibility rules.
     * Removes incompatible chemicals that shouldn't be in the drawer.
     */
    private void validateExistingContent() {
        for (int i = 0; i < tanks.length; i++) {
            ChemicalStack existingStack = tanks[i].getStack();
            if (!existingStack.isEmpty() && !isChemicalCompatible(existingStack)) {
                // Chemical is no longer compatible - remove it
                // This can happen if upgrades were removed while the chunk was unloaded
                tanks[i].setStack(ChemicalStack.EMPTY);
                filterStack[i] = ChemicalStack.EMPTY;
            }
        }
    }
    

    
    public long getCapacity() {
        return this.capacity;
    }

    public abstract void onChange();

    public abstract boolean isDrawerLocked();

    public abstract boolean isDrawerVoid();

    public abstract boolean isDrawerCreative();

    /**
     * Checks if the drawer has radioactive upgrade installed
     */
    private boolean hasRadioactiveUpgrade() {
        if (!FunctionalStorage.MEKANISM_LOADED) return false;
        if (tile == null) return false;
        
        // Use the cached method from ControllableDrawerTile
        return tile.isRadioactive();
    }
    
    /**
     * Updates radioactive mode based on upgrade presence
     */
    public void updateRadioactiveMode() {
        // Skip update if tile or level is not available yet
        if (tile == null || tile.getLevel() == null) {
            return;
        }
        
        boolean newMode = hasRadioactiveUpgrade();
        if (this.radioactiveMode != newMode) {
            this.radioactiveMode = newMode;
            onChange();
        }
    }
    
    /**
     * Validates if a chemical is compatible with current drawer mode
     */
    private boolean isChemicalCompatible(ChemicalStack stack) {
        if (stack.isEmpty()) return true;
        
        // Use direct isRadioactive() method instead of validator
        boolean isRadioactive = stack.isRadioactive();
        boolean hasUpgrade = hasRadioactiveUpgrade();
        
        // Radioactive chemicals require radioactive upgrade
        if (isRadioactive && !hasUpgrade) {
            return false;
        }
        
        // Non-radioactive chemicals incompatible with radioactive mode
        if (!isRadioactive && hasUpgrade) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the current radioactive mode status
     */
    public boolean isRadioactiveMode() {
        updateRadioactiveMode();
        return radioactiveMode;
    }
    
    /**
     * Sets the tile reference for radioactive upgrade checking
     */
    public void setTile(com.buuz135.functionalstorage.block.tile.ControllableDrawerTile<?> tile) {
        this.tile = tile;
        
        // Validate and synchronize state if we have content loaded from NBT
        // This handles cases where tile wasn't available during deserialization
        if (tile != null) {
            validateAndSynchronizeAfterLoad();
        }
    }
    


    public void lockHandler() {
        for (int i = 0; i < this.tanks.length; i++) {
            this.filterStack[i] = this.tanks[i].getStack().copy();
            if (!this.filterStack[i].isEmpty()) this.filterStack[i].setAmount(1);
        }
    }

    public ChemicalStack[] getFilterStack() {
        return filterStack;
    }

    /**
     * Custom chemical tank implementation based on CustomFluidTank patterns.
     * Handles creative/void/locked modes specific to FunctionalStorage drawers.
     */
    public class CustomChemicalTank implements IChemicalTank, INBTSerializable<CompoundTag> {

        private ChemicalStack stack = ChemicalStack.EMPTY;
        private long capacity;
        private Predicate<ChemicalStack> validator;

        public CustomChemicalTank(long capacity) {
            this(capacity, stack -> true);
        }

        public CustomChemicalTank(long capacity, Predicate<ChemicalStack> validator) {
            this.capacity = capacity;
            this.validator = validator;
        }

        @Override
        public @NotNull ChemicalStack getStack() {
            ChemicalStack result = this.stack.copy();
            if (!result.isEmpty() && isDrawerCreative()) {
                result.setAmount(Long.MAX_VALUE);
            }
            return result;
        }

        @Override
        public void setStack(@NotNull ChemicalStack stack) {
            this.stack = stack.copy();
        }

        @Override
        public void setStackUnchecked(@NotNull ChemicalStack stack) {
            this.stack = stack.copy();
        }

        @Override
        public void onContentsChanged() {
            // Empty implementation for IContentsListener
        }

        @Override
        public long getCapacity() {
            return isDrawerCreative() ? Long.MAX_VALUE : this.capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        @Override
        public @NotNull ChemicalStack insert(@NotNull ChemicalStack stack, @NotNull Action action, @NotNull AutomationType automationType) {
            if (stack.isEmpty() || !isValid(stack)) {
                return stack;
            }

            if (this.stack.isEmpty()) {
                long insertAmount = Math.min(stack.getAmount(), this.capacity);
                if (action == Action.EXECUTE) {
                    this.stack = stack.copy();
                    this.stack.setAmount(insertAmount);
                }
                ChemicalStack remainder = stack.copy();
                remainder.setAmount(stack.getAmount() - insertAmount);
                return remainder.isEmpty() ? ChemicalStack.EMPTY : remainder;
            }

            if (!ChemicalStack.isSameChemical(this.stack, stack)) {
                return stack;
            }

            long space = this.capacity - this.stack.getAmount();
            long insertAmount = Math.min(stack.getAmount(), space);
            
            if (action == Action.EXECUTE) {
                this.stack.setAmount(this.stack.getAmount() + insertAmount);
            }
            
            ChemicalStack remainder = stack.copy();
            remainder.setAmount(stack.getAmount() - insertAmount);
            
            // Void upgrade logic - void excess chemicals that couldn't fit
            if (isDrawerVoid() && !remainder.isEmpty() && 
                ((isDrawerLocked() && isValid(stack)) || (!this.stack.isEmpty() && ChemicalStack.isSameChemical(this.stack, stack)))) {
                return ChemicalStack.EMPTY; // Void the excess
            }
            
            return remainder.isEmpty() ? ChemicalStack.EMPTY : remainder;
        }

        @Override
        public @NotNull ChemicalStack extract(long amount, @NotNull Action action, @NotNull AutomationType automationType) {
            if (amount <= 0 || this.stack.isEmpty()) {
                return ChemicalStack.EMPTY;
            }

            if (isDrawerCreative()) {
                ChemicalStack result = this.stack.copy();
                result.setAmount(amount);
                return result;
            }

            long extractAmount = Math.min(amount, this.stack.getAmount());
            ChemicalStack result = this.stack.copy();
            result.setAmount(extractAmount);

            if (action == Action.EXECUTE) {
                this.stack.setAmount(this.stack.getAmount() - extractAmount);
                if (this.stack.getAmount() <= 0) {
                    this.stack = ChemicalStack.EMPTY;
                }
            }

            return result;
        }

        @Override
        public boolean isValid(@NotNull ChemicalStack stack) {
            return this.validator.test(stack);
        }

        @Override
        public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.put("Stack", ChemicalStack.OPTIONAL_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, provider), this.stack).getOrThrow());
            tag.putLong("Capacity", this.capacity);
            return tag;
        }

        @Override
        public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag nbt) {
            this.stack = ChemicalUtils.deserializeChemical(provider, nbt.getCompound("Stack"));
            this.capacity = nbt.getLong("Capacity");
        }
    }
}
