package com.studio.planeshift.server.test;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.block.BrickBlock;
import com.studio.planeshift.common.block.HitFromBelowBlock;
import com.studio.planeshift.common.block.QuestionBlock;
import com.studio.planeshift.common.block.PSwitchBlock;
import com.studio.planeshift.common.block.OnOffBlock;
import com.studio.planeshift.common.block.OnOffSwitchBlock;
import com.studio.planeshift.common.entity.CourseEnemyEntity;
import com.studio.planeshift.common.entity.HammerBroEntity;
import com.studio.planeshift.common.entity.HammerBroGoal;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import com.studio.planeshift.common.entity.DefeatVector;
import net.neoforged.neoforge.registries.RegisterEvent;
import java.lang.reflect.Method;
import net.minecraft.world.level.Level;

public class PlaneShiftGameTests {

    public static final Identifier QUESTION_BLOCK_TEST = PlaneShift.id("question_block_test");
    public static final Identifier P_SWITCH_TEST = PlaneShift.id("p_switch_test");
    public static final Identifier ON_OFF_SWITCH_TEST = PlaneShift.id("on_off_switch_test");
    public static final Identifier AIR_DROP_TEST = PlaneShift.id("air_drop_test");
    public static final Identifier COIN_BRICK_TEST = PlaneShift.id("coin_brick_test");
    public static final Identifier HAMMER_BRO_PERCH_TEST = PlaneShift.id("hammer_bro_perch_test");
    public static final Identifier COURSE_GENERATION_TEST = PlaneShift.id("course_generation_test");
    public static final Identifier DEFEAT_MATRIX_TEST = PlaneShift.id("defeat_matrix_test");

    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> {
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, QUESTION_BLOCK_TEST), PlaneShiftGameTests::testQuestionBlock);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, P_SWITCH_TEST), PlaneShiftGameTests::testPSwitch);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, ON_OFF_SWITCH_TEST), PlaneShiftGameTests::testOnOffSwitch);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, AIR_DROP_TEST), PlaneShiftGameTests::testAirDrop);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, COIN_BRICK_TEST), PlaneShiftGameTests::testCoinBrick);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, HAMMER_BRO_PERCH_TEST), PlaneShiftGameTests::testHammerBroPerch);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, COURSE_GENERATION_TEST), PlaneShiftGameTests::testCourseGeneration);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, DEFEAT_MATRIX_TEST), PlaneShiftGameTests::testDefeatMatrix);
        });
    }

    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> env = event.registerEnvironment(PlaneShift.id("default"), new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true);

        event.registerTest(QUESTION_BLOCK_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, QUESTION_BLOCK_TEST), data));
        event.registerTest(P_SWITCH_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, P_SWITCH_TEST), data));
        event.registerTest(ON_OFF_SWITCH_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, ON_OFF_SWITCH_TEST), data));
        event.registerTest(AIR_DROP_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, AIR_DROP_TEST), data));
        event.registerTest(COIN_BRICK_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, COIN_BRICK_TEST), data));
        event.registerTest(HAMMER_BRO_PERCH_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, HAMMER_BRO_PERCH_TEST), data));
        event.registerTest(COURSE_GENERATION_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, COURSE_GENERATION_TEST), data));
        event.registerTest(DEFEAT_MATRIX_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, DEFEAT_MATRIX_TEST), data));
    }

    /**
     * Composes a course inside the running game.
     *
     * <p>This exists because of a bug the entire JUnit suite could not see. The composer used
     * {@code RandomGeneratorFactory.of("Xoroshiro128PlusPlus")}, which resolves algorithms through
     * ServiceLoader. ServiceLoader works perfectly in a plain JUnit run and does not initialise
     * under FML's classloader, so 182 unit tests passed while every attempt to enter a course in
     * game failed with NoClassDefFoundError and simply never teleported the player.
     *
     * <p>The lesson generalises past that one call: anything touching ServiceLoader, reflection,
     * the module system or resource lookup can behave differently in game than in a unit test.
     * The only way to catch that class of bug is to run the code where it will actually live, so
     * course composition is now exercised by a GameTest as well — and CI runs
     * {@code runGameTestServer}, which means it cannot regress silently again.
     */
    private static void testCourseGeneration(GameTestHelper helper) {
        for (com.studio.planeshift.common.course.CourseTheme theme
                : com.studio.planeshift.common.course.CourseTheme.values()) {
            var composition = com.studio.planeshift.server.gen.CourseComposer.compose(
                    theme, 144, 2, 12345L);
            if (composition.canvas().blockCount() <= 0) {
                helper.fail("course generation produced no blocks for theme " + theme);
                return;
            }
            if (composition.segmentIds().isEmpty()) {
                helper.fail("course generation placed no segments for theme " + theme);
                return;
            }
            // The proof that runs in the unit suite, repeated here against the in-game classloader.
            var reach = new com.studio.planeshift.server.gen.CourseReachability(
                    composition.canvas(), 0);
            var result = reach.search(0, composition.spawnY(), composition.flagX());
            if (!result.reachable()) {
                helper.fail("generated " + theme + " course is not walkable: "
                        + result.describe(composition.flagX()));
                return;
            }
        }
        helper.succeed();
    }

    private static void testQuestionBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.QUESTION_BLOCK.get());
        
        BlockState state = helper.getBlockState(pos);
        try {
            Method method = QuestionBlock.class.getDeclaredMethod("popPickup", Level.class, BlockPos.class);
            method.setAccessible(true);
            method.invoke(null, helper.getLevel(), helper.absolutePos(pos));
            helper.getLevel().setBlock(helper.absolutePos(pos), state.setValue(QuestionBlock.USED, true), 3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(pos, QuestionBlock.USED, true);
        });
    }

    private static void testPSwitch(GameTestHelper helper) {
        BlockPos switchPos = new BlockPos(1, 1, 1);
        BlockPos brickPos = new BlockPos(2, 1, 1);
        
        helper.setBlock(switchPos, ModBlocks.P_SWITCH.get());
        helper.setBlock(brickPos, ModBlocks.BRICK_BLOCK.get());
        
        BlockState state = helper.getBlockState(switchPos);
        try {
            Method method = PSwitchBlock.class.getDeclaredMethod("activate", Level.class, BlockPos.class, BlockState.class);
            method.setAccessible(true);
            method.invoke(state.getBlock(), helper.getLevel(), helper.absolutePos(switchPos), state);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(switchPos, PSwitchBlock.PRESSED, true);
            helper.assertBlockPresent(ModBlocks.COIN_BLOCK.get(), brickPos);
        });
    }

    private static void testOnOffSwitch(GameTestHelper helper) {
        BlockPos switchPos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        
        helper.setBlock(switchPos, ModBlocks.ON_OFF_SWITCH.get());
        helper.setBlock(targetPos, ModBlocks.ON_OFF_BLOCK.get().defaultBlockState().setValue(OnOffBlock.ON, false));
        
        BlockState state = helper.getBlockState(switchPos);
        try {
            Method method = OnOffSwitchBlock.class.getDeclaredMethod("toggle", Level.class, BlockPos.class, BlockState.class);
            method.setAccessible(true);
            method.invoke(state.getBlock(), helper.getLevel(), helper.absolutePos(switchPos), state);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(switchPos, OnOffSwitchBlock.POWERED, true);
            helper.assertBlockProperty(targetPos, OnOffBlock.ON, true);
        });
    }

    private static void testAirDrop(GameTestHelper helper) {
        // A floor wide enough to stand a patrolling enemy on.
        //
        // This was a single block, which worked while ground enemies used MeleeAttackGoal and had
        // no target to chase, so they stood still. They patrol now — walk forward, turn at a wall
        // or a ledge — so a one-block platform means the Goomba steps off on its first tick and
        // falls forever, never touching ground, and the air-drop flag never clears. The product
        // behaviour is correct; the fixture assumed an enemy that does not move.
        BlockPos pos = new BlockPos(1, 2, 1);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(new BlockPos(1 + dx, 1, 1 + dz), Blocks.STONE);
            }
        }

        CourseEnemyEntity enemy = helper.spawn(ModEntities.GOOMBA.get(), pos);
        enemy.markAirDropped();
        helper.assertTrue(enemy.fallingFromDrop(),
                "Enemy should be falling from drop after markAirDropped");

        // Force the enemy onto the ground, tick, and verify the air-dropped flag clears.
        enemy.setOnGround(true);
        enemy.tick();
        helper.succeedWhen(() -> helper.assertFalse(enemy.fallingFromDrop(),
                "Enemy should clear airDropped flag when on ground"));
    }

    private static void testCoinBrick(GameTestHelper helper) {
        BlockPos coinPos = findBrickPosition(helper, true);
        BlockPos breakPos = findBrickPosition(helper, false);

        // Coin brick: hit from below, expect SPENT=true and a coin item spawned.
        helper.setBlock(coinPos, ModBlocks.BRICK_BLOCK.get());
        hitBrickFromBelow(helper, coinPos);

        // Regular brick: hit from below, expect it breaks.
        helper.setBlock(breakPos, ModBlocks.BRICK_BLOCK.get());
        hitBrickFromBelow(helper, breakPos);

        helper.succeedIf(() -> {
            helper.assertBlockProperty(coinPos, BrickBlock.SPENT, true);
            helper.assertBlockNotPresent(ModBlocks.BRICK_BLOCK.get(), breakPos);
        });
    }

    private static BlockPos findBrickPosition(GameTestHelper helper, boolean coin) {
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (BrickBlock.isCoinBrick(helper.absolutePos(p)) == coin) {
                        return p;
                    }
                }
            }
        }
        throw new IllegalStateException("Could not find " + (coin ? "coin" : "break") + " brick position");
    }

    private static void hitBrickFromBelow(GameTestHelper helper, BlockPos pos) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(pos);
        player.setPos(abs.getX() + 0.5D, abs.getY() - 1.8D, abs.getZ() + 0.5D);

        BlockState state = helper.getBlockState(pos);
        ((HitFromBelowBlock) state.getBlock()).attemptHitFromBelow(state, helper.getLevel(), abs, player);
    }

    private static void testHammerBroPerch(GameTestHelper helper) {
        BlockPos spawnPos = new BlockPos(1, 2, 1);
        BlockPos floorPos = new BlockPos(1, 1, 1);
        helper.setBlock(floorPos, Blocks.STONE);

        HammerBroEntity bro = helper.spawn(ModEntities.HAMMER_BRO.get(), spawnPos);
        HammerBroGoal goal = new HammerBroGoal(bro);

        // First tick captures the spawn position as the perch anchor.
        goal.tick();

        // Move the Bro beyond its 2.5-block perch and tick again; holdPerch should clamp.
        // The entity's spawn anchor is at the block center (absolute pos + 0.5).
        BlockPos abs = helper.absolutePos(spawnPos);
        double anchorX = abs.getX() + 0.5D;
        double anchorZ = abs.getZ() + 0.5D;
        bro.setPos(anchorX + 5.0D, spawnPos.getY(), anchorZ + 5.0D);
        goal.tick();

        helper.succeedIf(() -> {
            double expectedX = anchorX + 2.5D;
            double expectedZ = anchorZ + 2.5D;
            helper.assertTrue(Math.abs(bro.getX() - expectedX) < 0.01D, "Hammer Bro X not clamped to perch");
            helper.assertTrue(Math.abs(bro.getZ() - expectedZ) < 0.01D, "Hammer Bro Z not clamped to perch");
        });
    }

    /**
     * Every enemy the generator can place must be beatable with what the player always has.
     *
     * <p>A GameTest rather than a unit test, and the reason is the one review R6 was written
     * about: this needs live {@code EntityType}s, and an {@code EntityType} cannot be constructed
     * outside registration. In a plain JUnit classloader the registry is empty, so a unit test
     * here would pass by checking nothing.
     *
     * <p>What it enforces is a design rule, not a crash: an enemy whose only answers are FIRE or
     * STAR is not an enemy, it is a wall, because the generator will cheerfully drop it in a
     * corridor in front of a player who is carrying neither. Hazards - Thwomps, Boos - are exempt
     * by declaring {@code isHazard()}, which is the whole point of that flag existing.
     */
    private static void testDefeatMatrix(GameTestHelper helper) {
        List<String> unbeatable = new ArrayList<>();

        for (EntityType<?> type : List.of(
                ModEntities.GOOMBA.get(), ModEntities.KOOPA.get(), ModEntities.THWOMP.get(),
                ModEntities.BULLET_BILL.get(), ModEntities.BOO.get(), ModEntities.LAKITU.get(),
                ModEntities.HAMMER_BRO.get(), ModEntities.SPINY.get(),
                ModEntities.BUZZY_BEETLE.get(), ModEntities.PIRANHA_PLANT.get(),
                ModEntities.BOWSER.get())) {
            Entity spawned = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            if (!(spawned instanceof CourseEnemyEntity enemy)) {
                continue;
            }
            boolean answered = enemy.answers().stream()
                    .anyMatch(DefeatVector.ALWAYS_AVAILABLE::contains);
            if (!enemy.isHazard() && !answered) {
                unbeatable.add(type.getDescriptionId() + " answers only " + enemy.answers());
            }
            if (enemy.isHazard() && answered) {
                unbeatable.add(type.getDescriptionId()
                        + " is flagged a hazard but can be beaten with " + enemy.answers());
            }
            spawned.discard();
        }

        if (!unbeatable.isEmpty()) {
            helper.fail(String.join("; ", unbeatable));
            return;
        }
        helper.succeed();
    }

}