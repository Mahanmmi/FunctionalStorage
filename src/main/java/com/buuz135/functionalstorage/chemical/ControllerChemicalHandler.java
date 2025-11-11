package com.buuz135.functionalstorage.chemical;

import com.buuz135.functionalstorage.inventory.ControllerInventoryHandler;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ControllerChemicalHandler implements IChemicalHandler {

    HandlerTankSelector[] selectors;
    private int tanks = 0;

    public ControllerChemicalHandler() {
        invalidateSlots();
    }

    public void invalidateSlots() {
        List<HandlerTankSelector> selectors = new ArrayList<>();
        this.tanks = 0;
        for (IChemicalHandler handler : getDrawers().getChemicalHandlers()) {
            if (handler instanceof ControllerInventoryHandler) continue;
            int handlerTanks = handler.getChemicalTanks();
            for (int i = 0; i < handlerTanks; ++i) {
                selectors.add(new HandlerTankSelector(handler, i));
            }
            this.tanks += handlerTanks;
        }
        this.selectors = selectors.toArray(new HandlerTankSelector[selectors.size()]);
    }

    private HandlerTankSelector selectorForTank(int tank) {
        return tank >= 0 && tank < selectors.length ? selectors[tank] : null;
    }

    @Override
    public int getChemicalTanks() {
        return tanks;
    }

    @Override
    public @NotNull ChemicalStack getChemicalInTank(int tank) {
        HandlerTankSelector selector = selectorForTank(tank);
        return null != selector ? selector.getStackInSlot() : ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, @NotNull ChemicalStack stack) {
        HandlerTankSelector selector = selectorForTank(tank);
        if (selector != null) {
            selector.setStackInSlot(stack);
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        HandlerTankSelector selector = selectorForTank(tank);
        return null != selector ? selector.getCapacity() : 0;
    }

    @Override
    public boolean isValid(int tank, @NotNull ChemicalStack stack) {
        HandlerTankSelector selector = selectorForTank(tank);
        return null != selector && selector.isChemicalValid(stack);
    }

    @Override
    public @NotNull ChemicalStack insertChemical(int tank, @NotNull ChemicalStack stack, @NotNull Action action) {
        HandlerTankSelector selector = selectorForTank(tank);
        return null != selector ? selector.insertChemical(stack, action) : stack;
    }

    @Override
    public @NotNull ChemicalStack extractChemical(int tank, long amount, @NotNull Action action) {
        HandlerTankSelector selector = selectorForTank(tank);
        return null != selector ? selector.extractChemical(amount, action) : ChemicalStack.EMPTY;
    }

    // Mirror ControllerFluidHandler.fill() method exactly
    @Override
    public @NotNull ChemicalStack insertChemical(@NotNull ChemicalStack resource, @NotNull Action action) {
        // Priority 1: Non-empty tanks with matching chemical
        for (HandlerTankSelector selector : this.selectors) {
            if (!selector.getStackInSlot().isEmpty() && ChemicalStack.isSameChemical(selector.getStackInSlot(), resource) && selector.insertChemical(resource, Action.SIMULATE).getAmount() < resource.getAmount()) {
                return selector.insertChemical(resource, action);
            }
        }
        
        // Priority 2: Empty tanks
        for (HandlerTankSelector selector : this.selectors) {
            if (selector.getStackInSlot().isEmpty() && selector.insertChemical(resource, Action.SIMULATE).getAmount() < resource.getAmount()) {
                return selector.insertChemical(resource, action);
            }
        }
        
        return resource;
    }

    // Mirror ControllerFluidHandler.drain() methods exactly
    @Override
    public @NotNull ChemicalStack extractChemical(@NotNull ChemicalStack resource, @NotNull Action action) {
        for (HandlerTankSelector selector : this.selectors) {
            if (!selector.getStackInSlot().isEmpty() && ChemicalStack.isSameChemical(selector.getStackInSlot(), resource)) {
                return selector.extractChemical(resource, action);
            }
        }
        for (HandlerTankSelector selector : this.selectors) {
            if (selector.getStackInSlot().isEmpty()) {
                return selector.extractChemical(resource, action);
            }
        }
        return ChemicalStack.EMPTY;
    }

    @Override
    public @NotNull ChemicalStack extractChemical(long maxDrain, @NotNull Action action) {
        for (HandlerTankSelector selector : this.selectors) {
            if (!selector.getStackInSlot().isEmpty()) {
                return selector.extractChemical(maxDrain, action);
            }
        }
        return ChemicalStack.EMPTY;
    }

    public abstract ConnectedDrawers getDrawers();
}

class HandlerTankSelector {
    IChemicalHandler handler;
    int slot;

    public HandlerTankSelector(IChemicalHandler handler, int slot) {
        this.handler = handler;
        this.slot = slot;
    }

    public ChemicalStack getStackInSlot() {
        return handler.getChemicalInTank(slot);
    }

    public void setStackInSlot(ChemicalStack stack) {
        handler.setChemicalInTank(slot, stack);
    }

    public ChemicalStack insertChemical(@NotNull ChemicalStack stack, Action action) {
        return handler.insertChemical(slot, stack, action);
    }

    public ChemicalStack extractChemical(long amount, Action action) {
        return handler.extractChemical(slot, amount, action);
    }

    public ChemicalStack extractChemical(ChemicalStack chemicalStack, Action action) {
        return handler.extractChemical(slot, chemicalStack.getAmount(), action);
    }

    public long getCapacity() {
        return handler.getChemicalTankCapacity(slot);
    }

    public boolean isChemicalValid(@NotNull ChemicalStack stack) {
        return handler.isValid(slot, stack);
    }
}
