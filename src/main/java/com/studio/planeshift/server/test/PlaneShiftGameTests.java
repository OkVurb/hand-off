package com.studio.planeshift.server.test;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.block.HitFromBelowBlock;
import com.studio.planeshift.common.block.QuestionBlock;
import com.studio.planeshift.common.block.PSwitchBlock;
import com.studio.planeshift.common.block.OnOffBlock;
import com.studio.planeshift.common.block.OnOffSwitchBlock;
import com.studio.planeshift.common.entity.CourseEnemyEntity;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
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
import net.neoforged.neoforge.registries.RegisterEvent;
import java.lang.reflect.Method;
import net.minecraft.world.level.Level;

public class PlaneShiftGameTests {

    public static final Identifier QUESTION_BLOCK_TEST = PlaneShift.id("question_block_test");
    public static final Identifier P_SWITCH_TEST = PlaneShift.id("p_switch_test");
    public static final Identifier ON_OFF_SWITCH_TEST = PlaneShift.id("on_off_switch_test");
    public static final Identifier AIR_DROP_TEST = PlaneShift.id("air_drop_test");

    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> {
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, QUESTION_BLOCK_TEST), PlaneShiftGameTests::testQuestionBlock);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, P_SWITCH_TEST), PlaneShiftGameTests::testPSwitch);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, ON_OFF_SWITCH_TEST), PlaneShiftGameTests::testOnOffSwitch);
            helper.register(ResourceKey.create(Registries.TEST_FUNCTION, AIR_DROP_TEST), PlaneShiftGameTests::testAirDrop);
        });
    }

    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> env = event.registerEnvironment(PlaneShift.id("default"), new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true);

        event.registerTest(QUESTION_BLOCK_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, QUESTION_BLOCK_TEST), data));
        event.registerTest(P_SWITCH_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, P_SWITCH_TEST), data));
        event.registerTest(ON_OFF_SWITCH_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, ON_OFF_SWITCH_TEST), data));
        event.registerTest(AIR_DROP_TEST, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, AIR_DROP_TEST), data));
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
        BlockPos pos = new BlockPos(1, 2, 1);
        CourseEnemyEntity enemy = helper.spawn(ModEntities.GOOMBA.get(), pos);

        enemy.markAirDropped();
        helper.assertTrue(enemy.fallingFromDrop(), "Enemy should be falling from drop after markAirDropped");

        enemy.setOnGround(true);
        enemy.tick();

        helper.succeedWhen(() -> {
            helper.assertFalse(enemy.fallingFromDrop(), "Enemy should clear airDropped flag when on ground");
        });
    }
}