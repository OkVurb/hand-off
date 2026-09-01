package com.studio.planeshift.server;

import java.util.UUID;
import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseCrouch;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.item.CoinItem;
import com.studio.planeshift.common.network.OpenCourseMapPayload;
import com.studio.planeshift.common.network.OpenTitleScreenPayload;
import com.studio.planeshift.common.registry.ModEffects;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.block.FlagPoleBlock;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import com.studio.planeshift.common.item.BoomerangItem;
import com.studio.planeshift.common.item.AcornItem;
import com.studio.planeshift.common.item.CloudFlowerItem;
import com.studio.planeshift.common.item.ExtraPipItem;
import com.studio.planeshift.server.CourseCoopService;
import com.studio.planeshift.common.item.FireFlowerItem;
import com.studio.planeshift.common.item.FiveUpItem;
import com.studio.planeshift.common.item.PoisonMushroomItem;
import com.studio.planeshift.common.item.PropellerMushroomItem;
import com.studio.planeshift.common.item.HammerItem;
import com.studio.planeshift.common.item.IceFlowerItem;
import com.studio.planeshift.common.item.LeafItem;
import com.studio.planeshift.common.item.MegaMushroomItem;
import com.studio.planeshift.common.item.MiniMushroomItem;
import com.studio.planeshift.common.item.StarCoinItem;
import com.studio.planeshift.common.item.StarPowerItem;
import com.studio.planeshift.common.item.SuperMushroomItem;
import com.studio.planeshift.common.item.TanookiSuitItem;
import com.studio.planeshift.common.item.ThreeUpItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Game-bus wiring for the server services. Everything here runs on the logical server;
 * nothing touches client classes (enforced by the {@code checkClientClassLeak} task).
 */
@EventBusSubscriber(modid = PlaneShift.MOD_ID)
public final class ServerEvents {

    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        ModeTransitionService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MovementRuleService.tick(player);
            HungerService.tick(player);
            PlayerSizeService.apply(player, CourseStateAccess.get(player));
            AirMoveService.tick(player);
            LeafFlightService.tick(player);
            CourseTimerService.tick(player);
            CourseProgressService.tick(player);
            ToadDialogueService.tick(player);
            // Task 59: Ambient ash and spark particles for lava theme
            if (CourseStateAccess.get(player).inCourse()
                    && CourseThemeService.get(player) == com.studio.planeshift.common.course.CourseTheme.LAVA
                    && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && player.getRandom().nextInt(5) == 0) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                        player.getX(), player.getY() + 3.0D, player.getZ(),
                        2, 4.0D, 2.0D, 4.0D, 0.0D);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        1, 3.0D, 1.0D, 3.0D, 0.01D);
            }
            if (player.onGround()) {
                // Landing closes any airborne stomp chain, so the combo ladder only rewards
                // bounces strung together in the air.
                CourseScoringService.endChain(player);
            }
        }
    }

    /**
     * Course crouch hitbox on the server, which owns collision.
     *
     * <p>{ ClientEvents} mirrors this so the client predicts the same height; a disagreement
     * makes the player stutter against gaps the server thinks they fit through.
     */
    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.connection == null) {
            return;
        }
        var crouched = CourseCrouch.crouchedDimensions(event.getPose(), event.getNewSize(),
                CourseStateAccess.get(player).inCourse());
        if (crouched != null) {
            event.setNewSize(crouched);
        }
    }

    /**
     * Drives the power-up drift from each item entity's own tick.
     *
     * <p>Hooked per entity rather than by scanning the level: a whole-level sweep every tick to
     * find a handful of mushrooms is the same mistake `checkNoRawCuboidScan` exists to prevent.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ItemEntity item) {
            PowerupDriftService.tick(item);
        }
    }

    /** The pip damage model intercepts course damage before vanilla health is touched. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && DamageService.interceptDamage(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    /** No fall damage inside courses; landing is a mechanic, not a threat. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !CourseStateAccess.get(player).isHub()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Loaded state was sanitized by the codec (never mid-transition). Re-apply
            // role stats and resolve a mid-course disconnect to the checkpoint.
            RoleService.reapply(player);
            CourseMovementService.refresh(player);
            CourseState state = CourseStateAccess.get(player);
            if (state.pips() < CourseState.MAX_PIPS || state.lives() <= 0) {
                CourseStateAccess.update(player, s -> s
                        .withPips(CourseState.MAX_PIPS, 0L)
                        .withLives(Math.max(s.lives(), CourseState.STARTING_LIVES)));
            }
            if (state.inCourse()) {
                CheckpointService.returnToCheckpoint(player);
            }
            player.syncData(com.studio.planeshift.common.registry.ModAttachments.COURSE_STATE.get());

            // First-time / hub players get the title screen immediately.
            if (!CourseStateAccess.get(player).inCourse()) {
                PacketDistributor.sendToPlayer(player, OpenTitleScreenPayload.INSTANCE);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerCaches(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerCaches(player);
            RoleService.reapply(player);
            CourseMovementService.refresh(player);
            CourseState state = CourseStateAccess.get(player);
            CourseStateAccess.update(player, s -> s
                    .withPips(CourseState.MAX_PIPS, 0L)
                    .withLives(state.lives() <= 0 ? CourseState.STARTING_LIVES : state.lives()));
            if (CourseStateAccess.get(player).inCourse()) {
                CheckpointService.returnToCheckpoint(player);
            }
            player.syncData(com.studio.planeshift.common.registry.ModAttachments.COURSE_STATE.get());
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerCaches(player);
            RoleService.reapply(player);
            CourseMovementService.refresh(player);
        }
    }

    private static void clearPlayerCaches(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ModeTransitionService.clear(playerId);
        FormService.clear(playerId);
        AirMoveService.forget(player);
        CourseThemeService.clear(playerId);
        CourseScoringService.clear(playerId);
        FlagPoleBlock.clear(playerId);
        PayloadRateLimiter.forget(playerId);
        PlayerSizeService.forget(playerId);
        CourseProgressService.removeBar(player);
    }

    /** Power-up items pop out of blocks and float; mobs in courses become Mario enemies. */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity item) {
            ItemStack stack = item.getItem();
            if (!stack.isEmpty() && isFloatingPowerup(stack.getItem())) {
                item.setNoGravity(true);
                item.setPickUpDelay(0);
                if (item.getDeltaMovement().lengthSqr() > 1.0E-4D) {
                    item.setDeltaMovement(0.0D, 0.05D, 0.0D);
                }
            }
            return;
        }

        if (entity instanceof net.minecraft.world.entity.Mob mob
                && entity.level().dimension().identifier().equals(PlaneShift.id("course"))
                && !MobReplacementService.disableMobReplacement()) {
            java.util.Optional<net.minecraft.world.entity.Mob> replacement = MobReplacementService.replace(mob);
            if (replacement.isPresent()) {
                event.setCanceled(true);
            }
        }
    }

    /** Right-click a floating power-up to collect it. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity target = event.getTarget();
        if (!(target instanceof ItemEntity item)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Item pickup = item.getItem().getItem();
        if (!isFloatingPowerup(pickup)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && CourseStateAccess.get(player).inCourse()) {
            applyPowerup(player, item, pickup);
            event.setCanceled(true);
        }
    }

    /** Mario-style pickups convert on collection instead of going to inventory. */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!CourseStateAccess.get(serverPlayer).inCourse()) {
            return;
        }
        Item item = event.getItemEntity().getItem().getItem();
        if (!isFloatingPowerup(item)) {
            return;
        }
        event.setCanPickup(TriState.FALSE);
        applyPowerup(serverPlayer, event.getItemEntity(), item);
    }

    private static boolean isFloatingPowerup(Item item) {
        return item == ModItems.COIN.get()
                || item == ModItems.STAR_POWER.get()
                || item == ModItems.EXTRA_PIP.get()
                || item == ModItems.SUPER_MUSHROOM.get()
                || item == ModItems.MEGA_MUSHROOM.get()
                || item == ModItems.MINI_MUSHROOM.get()
                || item == ModItems.THREE_UP.get()
                || item == ModItems.FIVE_UP.get()
                || item == ModItems.FIRE_FLOWER.get()
                || item == ModItems.ICE_FLOWER.get()
                || item == ModItems.LEAF.get()
                || item == ModItems.PROPELLER_MUSHROOM.get()
                || item == ModItems.ACORN.get()
                || item == ModItems.CLOUD_FLOWER.get()
                || item == ModItems.HAMMER.get()
                || item == ModItems.BOOMERANG.get()
                || item == ModItems.TANOOKI.get()
                || item == ModItems.STAR_COIN.get();
    }

    private static void applyPowerup(ServerPlayer player, ItemEntity entity, Item item) {
        Level level = player.level();
        if (item instanceof CoinItem) {
            entity.discard();
            boolean[] gainedLife = new boolean[1];
            CourseStateAccess.update(player, s -> {
                int coins = s.coins() + 1;
                int lives = s.lives();
                while (coins >= 100) {
                    lives++;
                    coins -= 100;
                    gainedLife[0] = true;
                }
                return s.withCoins(coins).withLives(lives);
            });
            CourseScoringService.awardCoin(player);
            if (gainedLife[0]) {
                level.playSound(null, player.blockPosition(), ModSounds.ONE_UP.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.studio.planeshift.common.network.ScorePopupPayload(
                                player.getX(), player.getY() + 1.6D, player.getZ(), 0));
            } else {
                level.playSound(null, player.blockPosition(),
                        ModSounds.COIN_PICKUP.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            }
        } else if (item instanceof PoisonMushroomItem) {
            entity.discard();
            // Routed through DamageService so the Form buffer, invulnerability window and
            // life/game-over handling all behave exactly as they do for any other hit.
            DamageService.interceptDamage(player, player.damageSources().magic());
            level.playSound(null, player.blockPosition(),
                    ModSounds.DAMAGE.get(), SoundSource.PLAYERS, 0.9F, 0.7F);
        } else if (item instanceof StarCoinItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withStarCoins(s.starCoins() + 1));
            // The run counter above resets with the course; this is the permanent record, capped
            // per course so replaying cannot inflate the collection total.
            ProgressionService.recordStarCoin(player);
            CourseScoringService.awardStarCoin(player);
            level.playSound(null, player.blockPosition(),
                    ModSounds.COIN_PICKUP.get(), SoundSource.PLAYERS, 0.9F, 1.5F);
        } else if (item instanceof StarPowerItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            player.addEffect(new MobEffectInstance(ModEffects.STAR_POWER, 300, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 300, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 300, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (item instanceof ExtraPipItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s
                    .withPips(CourseState.MAX_PIPS, 0L)
                    .withLives(s.lives() + 1));
            CourseCoopService.shareLives(player, 1);
            level.playSound(null, player.blockPosition(),
                    ModSounds.ONE_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (item instanceof ThreeUpItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s
                    .withPips(CourseState.MAX_PIPS, 0L)
                    .withLives(s.lives() + 3));
            CourseCoopService.shareLives(player, 3);
            level.playSound(null, player.blockPosition(),
                    ModSounds.ONE_UP.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        } else if (item instanceof FiveUpItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s
                    .withPips(CourseState.MAX_PIPS, 0L)
                    .withLives(s.lives() + 5));
            CourseCoopService.shareLives(player, 5);
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (item instanceof SuperMushroomItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (item instanceof MegaMushroomItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            player.addEffect(new MobEffectInstance(ModEffects.MEGA_AURA, 300, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 300, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 300, 1, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.9F, 0.9F);
        } else if (item instanceof MiniMushroomItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            player.addEffect(new MobEffectInstance(ModEffects.MINI_AURA, 300, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 300, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 300, 2, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.9F, 1.3F);
        } else if (item instanceof FireFlowerItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            FormService.grant(player, PlaneShift.id("fire_flower"));
            player.addEffect(new MobEffectInstance(ModEffects.FIRE_AURA, 400, 0, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (item instanceof IceFlowerItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            FormService.grant(player, PlaneShift.id("ice_flower"));
            player.addEffect(new MobEffectInstance(ModEffects.ICE_AURA, 400, 0, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (item instanceof LeafItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            player.addEffect(new MobEffectInstance(ModEffects.LEAF_AURA, 400, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 400, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 1, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (item instanceof PropellerMushroomItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            FormService.grant(player, PlaneShift.id("propeller"));
            player.addEffect(new MobEffectInstance(ModEffects.PROPELLER_AURA, 300, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 300, 0, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
        } else if (item instanceof AcornItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            FormService.grant(player, PlaneShift.id("acorn"));
            player.addEffect(new MobEffectInstance(ModEffects.ACORN_AURA, 400, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 400, 0, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.1F);
        } else if (item instanceof CloudFlowerItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            FormService.grant(player, PlaneShift.id("cloud"));
            player.addEffect(new MobEffectInstance(ModEffects.CLOUD_AURA, 400, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 400, 0, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 0.9F);
        } else if (item instanceof HammerItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            FormService.grant(player, PlaneShift.id("hammer"));
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 0, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (item instanceof BoomerangItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            FormService.grant(player, PlaneShift.id("boomerang"));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (item instanceof TanookiSuitItem) {
            entity.discard();
            CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
            FormService.grant(player, PlaneShift.id("tanooki"));
            player.addEffect(new MobEffectInstance(ModEffects.LEAF_AURA, 400, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 400, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 1, false, false, true));
            level.playSound(null, player.blockPosition(),
                    ModSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }

        if (entity.isRemoved() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Per-power-up bursts rather than one generic glow: see PickupParticles for why the
            // shape of the burst is the only confirmation the player reliably gets.
            PickupParticles.spawn(player, serverLevel, item);
        }
    }
}
