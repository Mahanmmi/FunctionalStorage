package com.buuz135.functionalstorage.chemical;

import com.buuz135.functionalstorage.util.Utils;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * Chemical storage handler based on BigFluidHandler patterns.
 * Handles chemical storage, filtering, and tank operations for chemical drawers.
 */
public abstract class BigChemicalHandler implements IChemicalHandler, INBTSerializable<CompoundTag> {

    private CustomChemicalTank[] tanks;
    private ChemicalStack[] filterStack;
    private long capacity;

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
        
        // Stage 1: Block radioactive chemicals
        if (stack.isRadioactive()) {
            return false;
        }
        
        // Use Mekanism's validator for other checks
        return ChemicalAttributeValidator.DEFAULT.process(stack);
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
        return this.tanks[tank].getCapacity();
    }

    @Override
    public boolean isValid(int tank, @NotNull ChemicalStack stack) {
        return this.tanks[tank].isValid(stack);
    }

    @Override
    public @NotNull ChemicalStack insertChemical(int tank, @NotNull ChemicalStack stack, @NotNull Action action) {
        ChemicalStack result = this.tanks[tank].insert(stack, action, AutomationType.MANUAL);
        if (action == Action.EXECUTE) onChange();
        return result;
    }

    @Override
    public @NotNull ChemicalStack extractChemical(int tank, long amount, @NotNull Action action) {
        ChemicalStack result = this.tanks[tank].extract(amount, action, AutomationType.MANUAL);
        if (action == Action.EXECUTE) onChange();
        return result;
    }

    @Override
    public @NotNull ChemicalStack insertChemical(@NotNull ChemicalStack stack, @NotNull Action action) {
        // Try existing tanks first
        for (CustomChemicalTank tank : tanks) {
            if (!tank.getStack().isEmpty() && tank.isValid(stack)) {
                ChemicalStack result = tank.insert(stack, action, AutomationType.MANUAL);
                if (action == Action.EXECUTE) onChange();
                if (!result.isEmpty()) return result;
                return ChemicalStack.EMPTY;
            }
        }
        // Then try empty tanks
        for (CustomChemicalTank tank : tanks) {
            if (tank.getStack().isEmpty() && tank.isValid(stack)) {
                ChemicalStack result = tank.insert(stack, action, AutomationType.MANUAL);
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
                ChemicalStack result = tank.extract(amount, action, AutomationType.MANUAL);
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
                ChemicalStack result = tank.extract(stack.getAmount(), action, AutomationType.MANUAL);
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
    }

    public abstract void onChange();

    public abstract boolean isDrawerLocked();

    public abstract boolean isDrawerVoid();

    public abstract boolean isDrawerCreative();

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

            if (isDrawerVoid() && ((isDrawerLocked() && isValid(stack)) || 
                                 (!this.stack.isEmpty() && ChemicalStack.isSameChemical(this.stack, stack)))) {
                return ChemicalStack.EMPTY;
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
