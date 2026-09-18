package com.ia7md.patternconverter.gametest;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.block.DirectionMode;
import com.ia7md.patternconverter.block.PatternConverterBlock;
import com.ia7md.patternconverter.block.PatternConverterBlockEntity;
import com.ia7md.patternconverter.block.SlotStatus;
import com.ia7md.patternconverter.convert.RecipeLookup;
import com.ia7md.patternconverter.convert.ae2.Ae2Patterns;
import com.ia7md.patternconverter.convert.rs2.Rs2Patterns;
import com.ia7md.patternconverter.registry.PCBlocks;
import com.refinedmods.refinedstorage.api.autocrafting.Ingredient;
import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.PatternLayout;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.autocrafting.CraftingPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.PatternState;
import com.refinedmods.refinedstorage.common.autocrafting.ProcessingPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.SmithingTablePatternState;
import com.refinedmods.refinedstorage.common.autocrafting.StonecutterPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternType;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class ConversionGameTests {
    private ConversionGameTests() {
    }
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final int SETTLE_TICKS = 30;

    static void ae2CraftingToRs2(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        List<ItemStack> grid = planksGrid();
        ItemStack ae = encodeAe2Crafting(helper.getLevel(), grid);
        feed(be, ae, Rs2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ItemStack out = expectOutput(helper, be, Rs2Patterns::isRs2PatternItem, "an RS2 pattern");
            Pattern pattern = RefinedStorageApi.INSTANCE.getPattern(out, helper.getLevel())
                    .orElseThrow(() -> new AssertionError("RS2 could not decode the converted pattern"));
            PatternLayout layout = pattern.layout();
            assertEq(helper, "RS2 outputs", 1, layout.outputs().size());
            ResourceAmount output = layout.outputs().get(0);
            assertEq(helper, "RS2 output item", new ItemResource(Items.OAK_PLANKS), output.resource());
            assertEq(helper, "RS2 output amount", 4L, output.amount());
            expectReturned(helper, be, Ae2Patterns::isBlank, "the emptied AE2 blank");
            expectInputEmpty(helper, be);
            helper.succeed();
        });
    }

    static void rs2CraftingToAe2(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        ItemStack rs = encodeRs2Crafting(planksGrid(), true);
        feed(be, rs, Ae2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ItemStack out = expectOutput(helper, be, PatternDetailsHelper::isEncodedPattern, "an AE2 pattern");
            IPatternDetails details = PatternDetailsHelper.decodePattern(out, helper.getLevel());
            if (details == null) {
                helper.fail("AE2 could not decode the converted pattern");
                return;
            }
            GenericStack primary = details.getPrimaryOutput();
            assertEq(helper, "AE2 output key", AEItemKey.of(Items.OAK_PLANKS), primary.what());
            assertEq(helper, "AE2 output amount", 4L, primary.amount());
            if (!(details instanceof appeng.crafting.pattern.AECraftingPattern crafting) || !crafting.canSubstitute) {
                helper.fail("fuzzy mode was not carried over as AE2 substitution");
                return;
            }
            expectReturned(helper, be, Rs2Patterns::isBlank, "the emptied RS2 blank");
            helper.succeed();
        });
    }

    static void ae2ProcessingWithFluidToRs2(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        ItemStack ae = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.COBBLESTONE), 2),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), 1000)),
                List.of(new GenericStack(AEItemKey.of(Items.STONE), 1)));
        feed(be, ae, Rs2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ItemStack out = expectOutput(helper, be, Rs2Patterns::isRs2PatternItem, "an RS2 pattern");
            PatternLayout layout = RefinedStorageApi.INSTANCE.getPattern(out, helper.getLevel()).orElseThrow().layout();
            assertEq(helper, "RS2 ingredients", 2, layout.ingredients().size());
            assertEq(helper, "cobblestone amount", 2L, layout.ingredients().get(0).amount());
            assertEq(helper, "cobblestone key", new ItemResource(Items.COBBLESTONE), layout.ingredients().get(0).inputs().get(0));
            assertEq(helper, "water amount", 1000L, layout.ingredients().get(1).amount());
            assertEq(helper, "water key", new FluidResource(Fluids.WATER), layout.ingredients().get(1).inputs().get(0));
            assertEq(helper, "stone output", new ItemResource(Items.STONE), layout.outputs().get(0).resource());
            helper.succeed();
        });
    }

    static void rs2LargeProcessingToAe2Condenses(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        ItemStack rs = encodeRs2Processing(
                List.of(new ResourceAmount(new ItemResource(Items.COBBLESTONE), 64),
                        new ResourceAmount(new ItemResource(Items.COBBLESTONE), 36)),
                List.of(new ResourceAmount(new ItemResource(Items.STONE), 100)));
        feed(be, rs, Ae2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ItemStack out = expectOutput(helper, be, PatternDetailsHelper::isEncodedPattern, "an AE2 pattern");
            IPatternDetails details = PatternDetailsHelper.decodePattern(out, helper.getLevel());
            assertEq(helper, "AE2 inputs", 1, details.getInputs().length);
            IPatternDetails.IInput input = details.getInputs()[0];
            long total = input.getPossibleInputs()[0].amount() * input.getMultiplier();
            assertEq(helper, "condensed cobblestone", 100L, total);
            assertEq(helper, "stone output amount", 100L, details.getPrimaryOutput().amount());
            helper.succeed();
        });
    }

    static void ae2LargeProcessingToRs2Splits(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        ItemStack ae = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.COBBLESTONE), 150)),
                List.of(new GenericStack(AEItemKey.of(Items.STONE), 150)));
        feed(be, ae, Rs2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ItemStack out = expectOutput(helper, be, Rs2Patterns::isRs2PatternItem, "an RS2 pattern");
            PatternLayout layout = RefinedStorageApi.INSTANCE.getPattern(out, helper.getLevel()).orElseThrow().layout();
            long inputs = layout.ingredients().stream().mapToLong(Ingredient::amount).sum();
            long outputs = layout.outputs().stream().mapToLong(ResourceAmount::amount).sum();
            assertEq(helper, "input slots (150 = 64+64+22)", 3, layout.ingredients().size());
            assertEq(helper, "input total", 150L, inputs);
            assertEq(helper, "output total", 150L, outputs);
            for (Ingredient ingredient : layout.ingredients()) {
                if (ingredient.amount() > 64) {
                    helper.fail("an RS2 slot exceeds the stack size: " + ingredient.amount());
                    return;
                }
            }
            helper.succeed();
        });
    }

    static void rs2TooManyOutputsForAe2IsRefused(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        List<ResourceAmount> outputs = new ArrayList<>();
        for (var item : List.of(Items.STONE, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.COBBLESTONE,
                Items.DEEPSLATE, Items.TUFF, Items.CALCITE, Items.DRIPSTONE_BLOCK, Items.SAND, Items.RED_SAND,
                Items.GRAVEL, Items.CLAY, Items.DIRT, Items.COARSE_DIRT, Items.PODZOL, Items.MYCELIUM,
                Items.GRASS_BLOCK, Items.MUD, Items.NETHERRACK, Items.BASALT, Items.BLACKSTONE, Items.END_STONE,
                Items.OBSIDIAN, Items.ICE, Items.PACKED_ICE, Items.BLUE_ICE, Items.SNOW_BLOCK)) {
            outputs.add(new ResourceAmount(new ItemResource(item), 1));
        }
        ItemStack rs = encodeRs2Processing(List.of(new ResourceAmount(new ItemResource(Items.COBBLESTONE), 1)), outputs);
        feed(be, rs, Ae2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEq(helper, "slot status", SlotStatus.Kind.ERROR, be.getStatus(0).kind());
            if (!be.getInventory().getStack(0).isEmpty() && countOutputs(be) == 0) {
                helper.succeed();
            } else {
                helper.fail("a pattern with 28 outputs should not have converted");
            }
        });
    }

    static void stonecuttingRoundTrip(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        ItemStack rs = Rs2Patterns.blank();
        rs.set(DataComponents.INSTANCE.getPatternState(), new PatternState(UUID.randomUUID(), PatternType.STONECUTTER));
        rs.set(DataComponents.INSTANCE.getStonecutterPatternState(),
                new StonecutterPatternState(new ItemResource(Items.STONE), new ItemResource(Items.STONE_BRICKS)));
        feed(be, rs, Ae2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ItemStack ae = expectOutput(helper, be, PatternDetailsHelper::isEncodedPattern, "an AE2 stonecutting pattern");
            IPatternDetails details = PatternDetailsHelper.decodePattern(ae, helper.getLevel());
            if (!(details instanceof appeng.crafting.pattern.AEStonecuttingPattern)) {
                helper.fail("expected an AE2 stonecutting pattern, got " + details);
                return;
            }
            assertEq(helper, "stone bricks", AEItemKey.of(Items.STONE_BRICKS), details.getPrimaryOutput().what());
            clearOutputs(be);
            feed(be, ae, Rs2Patterns.blank());
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                ItemStack back = expectOutput(helper, be, Rs2Patterns::isRs2PatternItem, "an RS2 pattern");
                PatternState state = back.get(DataComponents.INSTANCE.getPatternState());
                assertEq(helper, "RS2 type", PatternType.STONECUTTER, state.type());
                PatternLayout layout = RefinedStorageApi.INSTANCE.getPattern(back, helper.getLevel()).orElseThrow().layout();
                assertEq(helper, "output", new ItemResource(Items.STONE_BRICKS), layout.outputs().get(0).resource());
                helper.succeed();
            });
        });
    }

    static void smithingRoundTrip(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        ItemStack rs = Rs2Patterns.blank();
        rs.set(DataComponents.INSTANCE.getPatternState(), new PatternState(UUID.randomUUID(), PatternType.SMITHING_TABLE));
        rs.set(DataComponents.INSTANCE.getSmithingTablePatternState(), new SmithingTablePatternState(
                new ItemResource(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemResource(Items.DIAMOND_SWORD),
                new ItemResource(Items.NETHERITE_INGOT)));
        feed(be, rs, Ae2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ItemStack ae = expectOutput(helper, be, PatternDetailsHelper::isEncodedPattern, "an AE2 smithing pattern");
            IPatternDetails details = PatternDetailsHelper.decodePattern(ae, helper.getLevel());
            if (!(details instanceof appeng.crafting.pattern.AESmithingTablePattern)) {
                helper.fail("expected an AE2 smithing pattern, got " + details);
                return;
            }
            assertEq(helper, "netherite sword", Items.NETHERITE_SWORD,
                    ((AEItemKey) details.getPrimaryOutput().what()).getItem());
            clearOutputs(be);
            feed(be, ae, Rs2Patterns.blank());
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                ItemStack back = expectOutput(helper, be, Rs2Patterns::isRs2PatternItem, "an RS2 pattern");
                PatternState state = back.get(DataComponents.INSTANCE.getPatternState());
                assertEq(helper, "RS2 type", PatternType.SMITHING_TABLE, state.type());
                helper.succeed();
            });
        });
    }

    static void lockedDirectionRefusesOtherKind(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        be.cycleMode();
        assertEq(helper, "mode", DirectionMode.FORCE_RS_TO_AE, be.getMode());
        ItemStack ae = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.STONE), 1)));
        feed(be, ae, Rs2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEq(helper, "status", SlotStatus.Kind.ERROR, be.getStatus(0).kind());
            assertEq(helper, "outputs", 0, countOutputs(be));
            helper.succeed();
        });
    }

    static void waitsForBlankOfTargetFormat(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        ItemStack ae = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.STONE), 1)));
        feed(be, ae, Ae2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEq(helper, "status", SlotStatus.Kind.WAITING, be.getStatus(0).kind());
            assertEq(helper, "outputs", 0, countOutputs(be));
            be.getInventory().setStack(PatternConverterBlockEntity.BLANK_SUPPLY_SLOT, Rs2Patterns.blank());
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                expectOutput(helper, be, Rs2Patterns::isRs2PatternItem, "an RS2 pattern");
                helper.succeed();
            });
        });
    }

    static void batchConvertsWholeGridAndPulses(GameTestHelper helper) {
        PatternConverterBlockEntity be = place(helper);
        for (int i = 0; i < 4; i++) {
            ItemStack ae = PatternDetailsHelper.encodeProcessingPattern(
                    List.of(new GenericStack(AEItemKey.of(Items.COBBLESTONE), i + 1)),
                    List.of(new GenericStack(AEItemKey.of(Items.STONE), 1)));
            be.getInventory().setStack(PatternConverterBlockEntity.INPUT_START + i, ae);
        }
        ItemStack blanks = Rs2Patterns.blank();
        blanks.setCount(4);
        be.getInventory().setStack(PatternConverterBlockEntity.BLANK_SUPPLY_SLOT, blanks);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            int rsPatterns = 0;
            for (int i = 0; i < PatternConverterBlockEntity.OUTPUT_COUNT; i++) {
                ItemStack s = be.getInventory().getStack(PatternConverterBlockEntity.OUTPUT_START + i);
                if (Rs2Patterns.isRs2PatternItem(s) && !Rs2Patterns.isBlank(s)) {
                    rsPatterns += s.getCount();
                }
            }
            ItemStack returned = be.getInventory().getStack(PatternConverterBlockEntity.BLANK_RETURN_SLOT);
            assertEq(helper, "converted patterns", 4, rsPatterns);
            assertEq(helper, "returned AE2 blanks stacked in the return slot", true,
                    Ae2Patterns.isBlank(returned) && returned.getCount() == 4);
            assertEq(helper, "supply slot consumed", true,
                    be.getInventory().getStack(PatternConverterBlockEntity.BLANK_SUPPLY_SLOT).isEmpty());
            assertEq(helper, "pulse ended", false, helper.getBlockState(POS).getValue(PatternConverterBlock.POWERED));
            helper.succeed();
        });
        helper.runAfterDelay(5, () -> assertEq(helper, "pulse raised",
                true, helper.getBlockState(POS).getValue(PatternConverterBlock.POWERED)));
    }

    static void refinedTypesFeBridgesToAppliedFlux(GameTestHelper helper) {
        if (!ModList.get().isLoaded("refinedtypes") || !ModList.get().isLoaded("appflux")) {
            helper.succeed();
            return;
        }
        PatternConverterBlockEntity be = place(helper);
        ItemStack rs = encodeRs2Processing(
                List.of(new ResourceAmount(AddonKeys.refinedTypesFe(), 5000),
                        new ResourceAmount(new ItemResource(Items.COBBLESTONE), 1)),
                List.of(new ResourceAmount(new ItemResource(Items.STONE), 1)));
        feed(be, rs, Ae2Patterns.blank());
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ItemStack out = expectOutput(helper, be, PatternDetailsHelper::isEncodedPattern, "an AE2 pattern with FE");
            IPatternDetails details = PatternDetailsHelper.decodePattern(out, helper.getLevel());
            boolean foundFe = false;
            for (IPatternDetails.IInput input : details.getInputs()) {
                GenericStack template = input.getPossibleInputs()[0];
                if (AddonKeys.isAppliedFluxFe(template.what())) {
                    foundFe = true;
                    assertEq(helper, "FE amount", 5000L, template.amount() * input.getMultiplier());
                }
            }
            assertEq(helper, "FE input present", true, foundFe);
            clearOutputs(be);
            feed(be, out, Rs2Patterns.blank());
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                ItemStack back = expectOutput(helper, be, Rs2Patterns::isRs2PatternItem, "an RS2 pattern with FE");
                PatternLayout layout = RefinedStorageApi.INSTANCE.getPattern(back, helper.getLevel()).orElseThrow().layout();
                long fe = layout.ingredients().stream()
                        .filter(i -> AddonKeys.isRefinedTypesFe(i.inputs().get(0)))
                        .mapToLong(Ingredient::amount).sum();
                assertEq(helper, "FE round trip", 5000L, fe);
                helper.succeed();
            });
        });
    }

    private static PatternConverterBlockEntity place(GameTestHelper helper) {
        helper.setBlock(POS, PCBlocks.PATTERN_CONVERTER.get());
        return helper.getBlockEntity(POS, PatternConverterBlockEntity.class);
    }

    private static void feed(PatternConverterBlockEntity be, ItemStack pattern, ItemStack blank) {
        be.getInventory().setStack(PatternConverterBlockEntity.INPUT_START, pattern);
        be.getInventory().setStack(PatternConverterBlockEntity.BLANK_SUPPLY_SLOT, blank);
    }

    private static void expectReturned(GameTestHelper helper, PatternConverterBlockEntity be,
                                       Predicate<ItemStack> test, String what) {
        for (int slot : new int[] {PatternConverterBlockEntity.BLANK_RETURN_SLOT, PatternConverterBlockEntity.BLANK_SUPPLY_SLOT}) {
            ItemStack s = be.getInventory().getStack(slot);
            if (!s.isEmpty() && test.test(s)) {
                return;
            }
        }
        helper.fail("expected " + what + " in a blank slot");
    }

    private static void clearOutputs(PatternConverterBlockEntity be) {
        for (int i = 0; i < PatternConverterBlockEntity.OUTPUT_COUNT; i++) {
            be.getInventory().setStack(PatternConverterBlockEntity.OUTPUT_START + i, ItemStack.EMPTY);
        }
        be.getInventory().setStack(PatternConverterBlockEntity.BLANK_RETURN_SLOT, ItemStack.EMPTY);
        be.getInventory().setStack(PatternConverterBlockEntity.BLANK_SUPPLY_SLOT, ItemStack.EMPTY);
    }

    private static int countOutputs(PatternConverterBlockEntity be) {
        int n = 0;
        for (int i = 0; i < PatternConverterBlockEntity.OUTPUT_COUNT; i++) {
            if (!be.getInventory().getStack(PatternConverterBlockEntity.OUTPUT_START + i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private static ItemStack expectOutput(GameTestHelper helper, PatternConverterBlockEntity be,
                                          Predicate<ItemStack> test, String what) {
        for (int i = 0; i < PatternConverterBlockEntity.OUTPUT_COUNT; i++) {
            ItemStack s = be.getInventory().getStack(PatternConverterBlockEntity.OUTPUT_START + i);
            if (!s.isEmpty() && test.test(s)) {
                return s;
            }
        }
        helper.fail("expected " + what + " in the output grid; slot 0 status: " + be.getStatus(0));
        return ItemStack.EMPTY;
    }

    private static void expectInputEmpty(GameTestHelper helper, PatternConverterBlockEntity be) {
        if (!be.getInventory().getStack(PatternConverterBlockEntity.INPUT_START).isEmpty()) {
            helper.fail("input slot should be empty after conversion");
        }
    }

    private static <T> void assertEq(GameTestHelper helper, String what, T expected, T actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            helper.fail(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static List<ItemStack> planksGrid() {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(ItemStack.EMPTY);
        }
        grid.set(4, new ItemStack(Items.OAK_LOG));
        return grid;
    }

    private static ItemStack encodeAe2Crafting(Level level, List<ItemStack> grid) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = RecipeLookup.crafting(level, grid, null);
        ItemStack result = RecipeLookup.craftingResult(level, recipe.orElseThrow(), grid);
        return PatternDetailsHelper.encodeCraftingPattern(recipe.get(), grid.toArray(new ItemStack[0]), result, false, false);
    }

    private static ItemStack encodeRs2Crafting(List<ItemStack> grid, boolean fuzzy) {
        ItemStack stack = Rs2Patterns.blank();
        stack.set(DataComponents.INSTANCE.getPatternState(), new PatternState(UUID.randomUUID(), PatternType.CRAFTING));
        stack.set(DataComponents.INSTANCE.getCraftingPatternState(),
                new CraftingPatternState(fuzzy, CraftingInput.ofPositioned(3, 3, grid)));
        return stack;
    }

    private static ItemStack encodeRs2Processing(List<ResourceAmount> inputs, List<ResourceAmount> outputs) {
        List<Optional<ProcessingPatternState.ProcessingIngredient>> ingredients = new ArrayList<>();
        List<Optional<ResourceAmount>> results = new ArrayList<>();
        for (ResourceAmount in : inputs) {
            ingredients.add(Optional.of(new ProcessingPatternState.ProcessingIngredient(in, List.of())));
        }
        for (ResourceAmount out : outputs) {
            results.add(Optional.of(out));
        }
        while (ingredients.size() < 81) {
            ingredients.add(Optional.empty());
        }
        while (results.size() < 81) {
            results.add(Optional.empty());
        }
        ItemStack stack = Rs2Patterns.blank();
        stack.set(DataComponents.INSTANCE.getPatternState(), new PatternState(UUID.randomUUID(), PatternType.PROCESSING));
        stack.set(DataComponents.INSTANCE.getProcessingPatternState(), new ProcessingPatternState(ingredients, results));
        return stack;
    }
}
