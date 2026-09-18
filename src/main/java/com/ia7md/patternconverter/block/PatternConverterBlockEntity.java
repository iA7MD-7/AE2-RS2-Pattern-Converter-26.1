package com.ia7md.patternconverter.block;

import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionDirection;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.convert.ConversionEngine;
import com.ia7md.patternconverter.menu.PatternConverterMenu;
import com.ia7md.patternconverter.registry.PCBlockEntities;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PatternConverterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_START = 0;
    public static final int INPUT_COUNT = 9;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_COUNT = 9;
    public static final int BLANK_SUPPLY_SLOT = 18;
    public static final int BLANK_RETURN_SLOT = 19;
    public static final int SLOT_COUNT = 20;

    private static final int MAX_NOTES = 6;
    private static final Codec<List<SlotStatus>> STATUS_LIST_CODEC = SlotStatus.CODEC.listOf();
    private static final Codec<List<Component>> NOTES_CODEC = ComponentSerialization.CODEC.listOf();
    private static final Component IDLE = ConversionText.idle();

    public final class ConverterInventory extends ItemStacksResourceHandler {
        ConverterInventory() {
            super(SLOT_COUNT);
        }

        public ItemStack getStack(int index) {
            return stacks.get(index);
        }

        public void setStack(int index, ItemStack stack) {
            if (stack.isEmpty()) {
                set(index, ItemResource.EMPTY, 0);
            } else {
                set(index, ItemResource.of(stack), stack.getCount());
            }
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (resource.isEmpty()) {
                return false;
            }
            ItemStack probe = resource.toStack();
            if (index >= INPUT_START && index < INPUT_START + INPUT_COUNT) {
                return PatternConverterApi.isEncodedPattern(probe);
            }
            if (index == BLANK_SUPPLY_SLOT) {
                return PatternConverterApi.isBlankPattern(probe);
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack stack) {
            setChanged();
            cooldown = Math.min(cooldown, 1);
        }

        @Override
        public void deserialize(ValueInput input) {
            super.deserialize(input);
            if (stacks.size() != SLOT_COUNT) {
                NonNullList<ItemStack> fixed = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
                for (int i = 0; i < Math.min(SLOT_COUNT, stacks.size()); i++) {
                    fixed.set(i, stacks.get(i));
                }
                setStacks(fixed);
            }
        }
    }

    private final ConverterInventory inventory = new ConverterInventory();
    private final ResourceHandler<ItemResource> automation = new AutomationHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PatternConverterMenu.DATA_MODE -> mode.ordinal();
                case PatternConverterMenu.DATA_DIRECTION -> activeDirection == null ? 0 : activeDirection.ordinal() + 1;
                case PatternConverterMenu.DATA_POWERED -> getBlockState().getValue(PatternConverterBlock.POWERED) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == PatternConverterMenu.DATA_MODE) {
                mode = DirectionMode.byOrdinal(value);
            }
        }

        @Override
        public int getCount() {
            return PatternConverterMenu.DATA_COUNT;
        }
    };

    private DirectionMode mode = DirectionMode.AUTO;
    @Nullable
    private ConversionDirection activeDirection;
    private final SlotStatus[] statuses = new SlotStatus[INPUT_COUNT];
    private Component lastEvent = IDLE;
    private List<Component> notes = new ArrayList<>();
    private int cooldown;

    public PatternConverterBlockEntity(BlockPos pos, BlockState state) {
        super(PCBlockEntities.PATTERN_CONVERTER.get(), pos, state);
        Arrays.fill(statuses, SlotStatus.EMPTY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PatternConverterBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }
        be.processCycle(level, state);
        be.cooldown = PCConfig.CYCLE_TICKS.get();
    }

    private void processCycle(Level level, BlockState state) {
        SlotStatus[] before = statuses.clone();
        ConversionDirection directionBefore = activeDirection;
        Component eventBefore = lastEvent;

        ConversionDirection forced = mode.forced();
        int budget = PCConfig.PATTERNS_PER_CYCLE.get();
        int converted = 0;
        ConversionDirection convertedDirection = null;
        List<Component> cycleNotes = new ArrayList<>();
        activeDirection = forced;

        for (int i = 0; i < INPUT_COUNT; i++) {
            ItemStack input = inventory.getStack(INPUT_START + i);
            if (input.isEmpty()) {
                statuses[i] = SlotStatus.EMPTY;
                continue;
            }
            Optional<ConversionDirection> directionOpt = ConversionEngine.directionOf(input);
            if (directionOpt.isEmpty()) {
                statuses[i] = SlotStatus.error(PatternConverterApi.isBlankPattern(input)
                        ? ConversionText.blankInInput() : ConversionText.notAPattern());
                continue;
            }
            ConversionDirection direction = directionOpt.get();
            if (activeDirection == null) {
                activeDirection = direction;
            }
            if (forced != null && forced != direction) {
                statuses[i] = SlotStatus.error(ConversionText.directionLocked(forced));
                continue;
            }
            if (!supplyHolds(direction.target())) {
                statuses[i] = SlotStatus.waiting(ConversionText.waitingForBlank(direction.target()));
                continue;
            }
            if (converted >= budget) {
                statuses[i] = SlotStatus.queued(ConversionText.queued());
                continue;
            }
            Result<ConversionEngine.Conversion> result = ConversionEngine.convert(input, level, forced);
            if (!result.isOk()) {
                statuses[i] = SlotStatus.error(result.failure());
                continue;
            }
            ConversionEngine.Conversion conversion = result.get();
            if (!insertIntoRange(OUTPUT_START, OUTPUT_START + OUTPUT_COUNT, conversion.converted(), true).isEmpty()) {
                statuses[i] = SlotStatus.waiting(ConversionText.outputFull());
                continue;
            }
            if (!returnSlotAccepts(conversion.emptiedOriginal())) {
                statuses[i] = SlotStatus.waiting(ConversionText.returnFull(direction.source()));
                continue;
            }
            ItemStack blank = inventory.getStack(BLANK_SUPPLY_SLOT).copy();
            blank.shrink(1);
            inventory.setStack(BLANK_SUPPLY_SLOT, blank);
            insertIntoRange(OUTPUT_START, OUTPUT_START + OUTPUT_COUNT, conversion.converted(), false);
            ItemStack remainder = insertIntoRange(BLANK_RETURN_SLOT, BLANK_RETURN_SLOT + 1, conversion.emptiedOriginal(), false);
            if (!remainder.isEmpty()) {
                insertIntoRange(OUTPUT_START, OUTPUT_START + OUTPUT_COUNT, remainder, false);
            }
            ItemStack rest = input.copy();
            rest.shrink(1);
            inventory.setStack(INPUT_START + i, rest);
            converted++;
            convertedDirection = direction;
            cycleNotes.addAll(conversion.warnings());
            statuses[i] = rest.isEmpty() ? SlotStatus.EMPTY : SlotStatus.queued(ConversionText.queued());
        }

        if (activeDirection == null) {
            Optional<PatternFormat> supplied = PatternConverterApi.blankFormat(inventory.getStack(BLANK_SUPPLY_SLOT));
            if (supplied.isPresent()) {
                activeDirection = ConversionDirection.towards(supplied.get());
            }
        }
        if (converted > 0) {
            lastEvent = ConversionText.converted(convertedDirection, converted);
            if (!cycleNotes.isEmpty()) {
                List<Component> merged = new ArrayList<>(cycleNotes);
                merged.addAll(notes);
                notes = new ArrayList<>(merged.subList(0, Math.min(MAX_NOTES, merged.size())));
            }
            pulse(level, state);
            setChanged();
        } else if (!hasInputs()) {
            lastEvent = IDLE;
        }

        boolean changed = converted > 0 || directionBefore != activeDirection || eventBefore != lastEvent
                || !Arrays.equals(before, statuses);
        if (changed) {
            sync();
        }
    }

    private boolean supplyHolds(PatternFormat format) {
        ItemStack stack = inventory.getStack(BLANK_SUPPLY_SLOT);
        return !stack.isEmpty() && PatternConverterApi.blankFormat(stack).filter(f -> f == format).isPresent();
    }

    private boolean returnSlotAccepts(ItemStack emptied) {
        return insertIntoRange(BLANK_RETURN_SLOT, BLANK_RETURN_SLOT + 1, emptied, true).isEmpty();
    }

    private boolean hasInputs() {
        for (int i = 0; i < INPUT_COUNT; i++) {
            if (!inventory.getStack(INPUT_START + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private ItemStack insertIntoRange(int from, int to, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copy();
        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            for (int slot = from; slot < to && !remaining.isEmpty(); slot++) {
                ItemStack current = inventory.getStack(slot);
                boolean empty = current.isEmpty();
                if ((pass == 0) == empty) {
                    continue;
                }
                if (!empty && !ItemStack.isSameItemSameComponents(current, remaining)) {
                    continue;
                }
                int limit = remaining.getMaxStackSize();
                int room = limit - current.getCount();
                if (room <= 0) {
                    continue;
                }
                int moved = Math.min(room, remaining.getCount());
                if (!simulate) {
                    inventory.setStack(slot, remaining.copyWithCount(current.getCount() + moved));
                }
                remaining.shrink(moved);
            }
        }
        return remaining;
    }

    private void pulse(Level level, BlockState state) {
        if (!state.getValue(PatternConverterBlock.POWERED)) {
            level.setBlock(worldPosition, state.setValue(PatternConverterBlock.POWERED, true), Block.UPDATE_ALL);
        }
        level.scheduleTick(worldPosition, state.getBlock(), PCConfig.REDSTONE_PULSE_TICKS.get());
    }

    public void cycleMode() {
        mode = mode.next();
        cooldown = 0;
        setChanged();
        sync();
    }

    public DirectionMode getMode() {
        return mode;
    }

    @Nullable
    public ConversionDirection getActiveDirection() {
        return activeDirection;
    }

    public SlotStatus getStatus(int inputSlot) {
        return statuses[inputSlot];
    }

    public Component getLastEvent() {
        return lastEvent;
    }

    public List<Component> getNotes() {
        return notes;
    }

    public ConverterInventory getInventory() {
        return inventory;
    }

    public ResourceHandler<ItemResource> getAutomationHandler() {
        return automation;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public int getOutputComparatorSignal() {
        int filled = 0;
        float fullness = 0;
        for (int i = 0; i < OUTPUT_COUNT; i++) {
            ItemStack stack = inventory.getStack(OUTPUT_START + i);
            if (!stack.isEmpty()) {
                filled++;
                fullness += (float) stack.getCount() / stack.getMaxStackSize();
            }
        }
        return filled == 0 ? 0 : Math.max(1, (int) Math.floor(fullness / OUTPUT_COUNT * 14.0F) + 1);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                }
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.pattern_converter.pattern_converter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PatternConverterMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("Inventory"));
        output.putString("Mode", mode.getSerializedName());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Inventory").ifPresent(inventory::deserialize);
        input.getString("Mode").ifPresent(name -> mode = DirectionMode.byName(name));
        input.getInt("Direction").ifPresent(d -> activeDirection = d == 0 ? null
                : ConversionDirection.values()[Math.floorMod(d - 1, ConversionDirection.values().length)]);
        input.read("Statuses", STATUS_LIST_CODEC).ifPresent(list -> {
            for (int i = 0; i < INPUT_COUNT; i++) {
                statuses[i] = i < list.size() ? list.get(i) : SlotStatus.EMPTY;
            }
        });
        input.read("Event", ComponentSerialization.CODEC).ifPresent(c -> lastEvent = c);
        input.read("Notes", NOTES_CODEC).ifPresent(list -> notes = new ArrayList<>(list));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        output.putString("Mode", mode.getSerializedName());
        output.putInt("Direction", activeDirection == null ? 0 : activeDirection.ordinal() + 1);
        output.store("Statuses", STATUS_LIST_CODEC, Arrays.asList(statuses));
        output.store("Event", ComponentSerialization.CODEC, lastEvent);
        output.store("Notes", NOTES_CODEC, notes);
        return output.buildResult();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private final class AutomationHandler extends DelegatingResourceHandler<ItemResource> {
        AutomationHandler() {
            super(inventory);
        }

        private static boolean insertable(int index) {
            return (index >= INPUT_START && index < INPUT_START + INPUT_COUNT) || index == BLANK_SUPPLY_SLOT;
        }

        private static boolean extractable(int index) {
            return (index >= OUTPUT_START && index < OUTPUT_START + OUTPUT_COUNT) || index == BLANK_RETURN_SLOT;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return insertable(index) && super.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return insertable(index) ? super.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            int inserted = 0;
            for (int index = 0; index < size() && inserted < amount; index++) {
                inserted += insert(index, resource, amount - inserted, transaction);
            }
            return inserted;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return extractable(index) ? super.extract(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            int extracted = 0;
            for (int index = 0; index < size() && extracted < amount; index++) {
                extracted += extract(index, resource, amount - extracted, transaction);
            }
            return extracted;
        }
    }
}
