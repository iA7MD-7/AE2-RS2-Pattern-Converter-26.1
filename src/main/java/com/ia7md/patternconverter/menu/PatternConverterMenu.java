package com.ia7md.patternconverter.menu;

import com.ia7md.patternconverter.api.ConversionDirection;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.block.DirectionMode;
import com.ia7md.patternconverter.block.PatternConverterBlockEntity;
import com.ia7md.patternconverter.registry.PCBlocks;
import com.ia7md.patternconverter.registry.PCMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jetbrains.annotations.Nullable;

public class PatternConverterMenu extends AbstractContainerMenu {
    public static final int DATA_MODE = 0;
    public static final int DATA_DIRECTION = 1;
    public static final int DATA_POWERED = 2;
    public static final int DATA_COUNT = 3;

    public static final int BUTTON_CYCLE_MODE = 0;

    public static final int INPUT_X = 8;
    public static final int OUTPUT_X = 116;
    public static final int GRID_Y = 18;
    public static final int BLANK_SUPPLY_X = 68;
    public static final int BLANK_SUPPLY_Y = 54;
    public static final int BLANK_RETURN_X = 93;
    public static final int BLANK_RETURN_Y = 18;
    public static final int PLAYER_INV_Y = 86;
    public static final int HOTBAR_Y = 144;

    private static final int MACHINE_SLOTS = PatternConverterBlockEntity.SLOT_COUNT;

    @Nullable
    private final PatternConverterBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public PatternConverterMenu(int containerId, Inventory playerInventory, PatternConverterBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, blockEntity.getMenuData(),
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    public static PatternConverterMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        PatternConverterBlockEntity be = playerInventory.player.level().getBlockEntity(pos)
                instanceof PatternConverterBlockEntity converter ? converter : null;
        return new PatternConverterMenu(containerId, playerInventory, be, new SimpleContainerData(DATA_COUNT),
                ContainerLevelAccess.NULL);
    }

    private PatternConverterMenu(int containerId, Inventory playerInventory, @Nullable PatternConverterBlockEntity blockEntity,
                                 ContainerData data, ContainerLevelAccess access) {
        super(PCMenus.PATTERN_CONVERTER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.data = data;

        PatternConverterBlockEntity.ConverterInventory handler = blockEntity != null ? blockEntity.getInventory()
                : new PatternConverterBlockEntity(BlockPos.ZERO, PCBlocks.PATTERN_CONVERTER.get().defaultBlockState()).getInventory();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                addSlot(new ResourceHandlerSlot(handler, handler::set, PatternConverterBlockEntity.INPUT_START + index,
                        INPUT_X + col * 18, GRID_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return PatternConverterApi.isEncodedPattern(stack);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                addSlot(new ResourceHandlerSlot(handler, handler::set, PatternConverterBlockEntity.OUTPUT_START + index,
                        OUTPUT_X + col * 18, GRID_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addSlot(new ResourceHandlerSlot(handler, handler::set, PatternConverterBlockEntity.BLANK_SUPPLY_SLOT, BLANK_SUPPLY_X, BLANK_SUPPLY_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PatternConverterApi.isBlankPattern(stack);
            }
        });
        addSlot(new ResourceHandlerSlot(handler, handler::set, PatternConverterBlockEntity.BLANK_RETURN_SLOT, BLANK_RETURN_X, BLANK_RETURN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    @Nullable
    public PatternConverterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public DirectionMode getMode() {
        return DirectionMode.byOrdinal(data.get(DATA_MODE));
    }

    @Nullable
    public ConversionDirection getActiveDirection() {
        int d = data.get(DATA_DIRECTION);
        return d == 0 ? null : ConversionDirection.values()[Math.floorMod(d - 1, ConversionDirection.values().length)];
    }

    public boolean isPulsing() {
        return data.get(DATA_POWERED) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CYCLE_MODE && blockEntity != null) {
            blockEntity.cycleMode();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = MACHINE_SLOTS;
        int playerEnd = playerStart + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (PatternConverterApi.isEncodedPattern(stack)) {
                moved = moveItemStackTo(stack, PatternConverterBlockEntity.INPUT_START,
                        PatternConverterBlockEntity.INPUT_START + PatternConverterBlockEntity.INPUT_COUNT, false);
            } else if (PatternConverterApi.isBlankPattern(stack)) {
                moved = moveItemStackTo(stack, PatternConverterBlockEntity.BLANK_SUPPLY_SLOT,
                        PatternConverterBlockEntity.BLANK_SUPPLY_SLOT + 1, false);
            }
            if (!moved) {
                if (index < playerStart + 27) {
                    if (!moveItemStackTo(stack, playerStart + 27, playerEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, playerStart, playerStart + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, PCBlocks.PATTERN_CONVERTER.get());
    }
}
