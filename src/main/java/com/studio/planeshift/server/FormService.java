package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.entity.BoomerangProjectile;
import com.studio.planeshift.common.entity.EmberBoltEntity;
import com.studio.planeshift.common.entity.FireballProjectile;
import com.studio.planeshift.common.entity.HammerProjectile;
import com.studio.planeshift.common.entity.IceballProjectile;
import com.studio.planeshift.common.entity.ProjectileTracker;
import com.studio.planeshift.common.form.FormDefinition;
import com.studio.planeshift.common.form.FormSlot;
import com.studio.planeshift.common.registry.ModEffects;
import com.studio.planeshift.common.registry.ModRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Form lifecycle owner (Design Bible, "Power-up framework" and "Reserve inventory and
 * balance rules").
 *
 * <p>"Only the server grants, spends, replaces, or removes a Form." All acquisition
 * rules from the reserve table are implemented here; clients only ever see the synced
 * {@link FormSlot} result.
 */
public final class FormService {

    /** Ember Core limit: "Two active shots; heat recovery." */
    private static final int MAX_ACTIVE_BOLTS = 2;
    /** Hard rate floor for action requests, independent of per-Form cooldowns. */
    private static final int MIN_ACTION_INTERVAL_TICKS = 4;
    /** Reserve swap lockout ("swaps with a short lockout and animation"). */
    private static final int RESERVE_SWAP_LOCKOUT_TICKS = 10;
    /** Coin refund when a reserve Form is displaced. */
    private static final int RESERVE_REFUND_COINS = 3;
    /** Magnet Lantern pull duration per action. */
    private static final int MAGNET_DURATION_TICKS = 100;

    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();
    private static final Map<UUID, Long> MAGNET_ACTIVE_UNTIL = new HashMap<>();

    private FormService() {
    }

    public static Optional<FormDefinition> lookup(ServerPlayer player, Identifier formId) {
        return player.level().registryAccess().lookup(ModRegistries.FORM)
                .flatMap(registry -> registry.get(ResourceKey.create(ModRegistries.FORM, formId)))
                .map(holder -> holder.value());
    }

    /**
     * Reserve table (verbatim from the bible):
     * <ul>
     *   <li>Pickup while empty — activate the Form; no reserve item.</li>
     *   <li>Pickup same Form — refresh charges/duration up to the Form cap.</li>
     *   <li>Pickup different Form, reserve empty — activate new; move old to reserve if eligible.</li>
     *   <li>Pickup different Form, reserve full — convert older reserve to Glints, then rotate.</li>
     * </ul>
     */
    public static boolean grant(ServerPlayer player, Identifier formId) {
        if (!CourseStateAccess.get(player).inCourse()) {
            return false;
        }
        Optional<FormDefinition> definition = lookup(player, formId);
        if (definition.isEmpty()) {
            // Unknown/missing Form IDs migrate to NONE and refund a neutral pickup.
            PlaneShift.LOGGER.warn("Unknown form id {} for {}", formId, player.getName().getString());
            CourseStateAccess.update(player, s -> s.withCoins(s.coins() + 1));
            return false;
        }
        FormDefinition form = definition.get();

        CourseStateAccess.update(player, state -> {
            FormSlot slot = state.formSlot();
            int refundedCoins = state.coins();

            FormSlot next;
            if (slot.active().isEmpty()) {
                next = slot.withActive(formId, form.maxCharges());
            } else if (slot.active().get().equals(formId)) {
                next = slot.withActive(formId, form.maxCharges());
                if (form.reserveEligible()) {
                    if (slot.reserve().isPresent() && !slot.reserve().get().equals(formId)) {
                        refundedCoins += RESERVE_REFUND_COINS;
                    }
                    next = next.withReserve(Optional.of(formId));
                }
            } else {
                Identifier displaced = slot.active().get();
                boolean displacedEligible = lookup(player, displaced)
                        .map(FormDefinition::reserveEligible).orElse(false);
                Optional<Identifier> nextReserve;
                if (displacedEligible) {
                    // The displaced active form rotates into reserve; cash in the old reserve.
                    if (slot.reserve().isPresent()) {
                        refundedCoins += RESERVE_REFUND_COINS;
                    }
                    nextReserve = Optional.of(displaced);
                } else {
                    // Non-reserve-eligible forms cannot be stored; reserve is unchanged.
                    nextReserve = slot.reserve();
                }
                next = new FormSlot(Optional.of(formId), form.maxCharges(), 0L, nextReserve);
            }
            return state.withFormSlot(next).withCoins(refundedCoins);
        });
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR,
                SoundSource.PLAYERS, 0.7F, 1.4F);
        return true;
    }

    /** Client requested the Form action. Validate everything, then execute. */
    public static void useAction(ServerPlayer player, Vec3 rawAim) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        if (now - LAST_ACTION.getOrDefault(id, -(long) MIN_ACTION_INTERVAL_TICKS) < MIN_ACTION_INTERVAL_TICKS) {
            return;
        }
        FormSlot slot = state.formSlot();
        if (!slot.actionReady(now) || ModeTransitionService.isTransitioning(player)) {
            return;
        }
        Optional<Identifier> active = slot.active();
        if (active.isEmpty()) {
            return;
        }
        Optional<FormDefinition> definition = lookup(player, active.get());
        if (definition.isEmpty()) {
            CourseStateAccess.update(player, s -> s.withFormSlot(s.formSlot().loseActive()));
            return;
        }
        FormDefinition form = definition.get();

        // Validate and normalize the client-supplied aim; never trust magnitudes.
        Vec3 aim = sanitizeAim(rawAim, player);
        // 2.5D rule: actions aim within the plane.
        if (state.in2_5D() && state.rail().isPresent()) {
            aim = state.rail().get().flattenVelocity(aim);
            if (aim.lengthSqr() < 1.0E-4D) {
                aim = state.rail().get().flattenVelocity(player.getLookAngle()).normalize();
            } else {
                aim = aim.normalize();
            }
        }

        boolean executed = switch (form.action()) {
            case EMBER_SHOT -> fireEmberBolt(player, aim, form);
            case FIRE_SHOT -> fireFireball(player, aim, form);
            case ICE_SHOT -> fireIceball(player, aim, form);
            case HAMMER_THROW -> throwHammer(player, aim, form);
            case BOOMERANG_THROW -> throwBoomerang(player, aim, form);
            case CLAW_SWIPE -> clawSwipe(player, form);
            case TAIL_WHACK -> tailWhack(player, form);
            case PROPELLER_SPIN -> propellerSpin(player, form);
            case ACORN_GLIDE -> acornGlide(player, aim, form);
            case CLOUD_STEP -> cloudStep(player, form);
            case GALE_DASH -> galeDash(player, aim, form, state);
            case MAGNET_PULSE -> magnetPulse(player, now);
            case BARRIER, NONE -> false; // Barrier is passive; nothing to trigger.
        };

        if (executed) {
            LAST_ACTION.put(id, now);
            CourseStateAccess.update(player, s -> s.withFormSlot(
                    s.formSlot().withCharges(s.formSlot().charges() - 1, now + form.cooldownTicks())));
        }
    }

    private static boolean fireEmberBolt(ServerPlayer player, Vec3 aim, FormDefinition form) {
        // Bounded spatial query for the two-active-shots rule.
        // Bounded by ProjectileTracker instead of a 128³ AABB scan.
        int active = ProjectileTracker.count(player.getUUID(), EmberBoltEntity.class);
        if (active >= MAX_ACTIVE_BOLTS) {
            return false;
        }
        EmberBoltEntity bolt = new EmberBoltEntity(player.level(), player);
        bolt.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        float speed = 0.9F * form.actionPower();
        bolt.shoot(aim.x, aim.y + 0.12D, aim.z, speed, 0.5F);
        player.level().addFreshEntity(bolt);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 0.6F, 1.3F);
        return true;
    }

    private static boolean fireFireball(ServerPlayer player, Vec3 aim, FormDefinition form) {
        // Bounded by ProjectileTracker instead of a 128³ AABB scan.
        int active = ProjectileTracker.count(player.getUUID(), FireballProjectile.class);
        if (active >= MAX_ACTIVE_BOLTS) {
            return false;
        }
        FireballProjectile ball = new FireballProjectile(player.level(), player);
        ball.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        float speed = 0.9F * form.actionPower();
        ball.shoot(aim.x, aim.y + 0.12D, aim.z, speed, 0.5F);
        player.level().addFreshEntity(ball);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 0.6F, 1.2F);
        return true;
    }

    private static boolean fireIceball(ServerPlayer player, Vec3 aim, FormDefinition form) {
        // Bounded by ProjectileTracker instead of a 128³ AABB scan.
        int active = ProjectileTracker.count(player.getUUID(), IceballProjectile.class);
        if (active >= MAX_ACTIVE_BOLTS) {
            return false;
        }
        IceballProjectile ball = new IceballProjectile(player.level(), player);
        ball.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        float speed = 0.9F * form.actionPower();
        ball.shoot(aim.x, aim.y + 0.12D, aim.z, speed, 0.5F);
        player.level().addFreshEntity(ball);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
                SoundSource.PLAYERS, 0.6F, 1.4F);
        return true;
    }

    private static boolean throwHammer(ServerPlayer player, Vec3 aim, FormDefinition form) {
        // Bounded by ProjectileTracker instead of a 128³ AABB scan.
        int active = ProjectileTracker.count(player.getUUID(), HammerProjectile.class);
        if (active >= MAX_ACTIVE_BOLTS) {
            return false;
        }
        HammerProjectile hammer = new HammerProjectile(player.level(), player);
        hammer.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        float speed = 0.9F * form.actionPower();
        hammer.shoot(aim.x, aim.y + 0.2D, aim.z, speed, 0.4F);
        player.level().addFreshEntity(hammer);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EGG_THROW,
                SoundSource.PLAYERS, 0.6F, 0.8F);
        return true;
    }

    private static boolean throwBoomerang(ServerPlayer player, Vec3 aim, FormDefinition form) {
        // Bounded by ProjectileTracker instead of a 128³ AABB scan.
        int active = ProjectileTracker.count(player.getUUID(), BoomerangProjectile.class);
        if (active >= MAX_ACTIVE_BOLTS) {
            return false;
        }
        BoomerangProjectile boomerang = new BoomerangProjectile(player.level(), player);
        boomerang.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        float speed = 0.9F * form.actionPower();
        boomerang.shoot(aim.x, aim.y + 0.05D, aim.z, speed, 0.2F);
        player.level().addFreshEntity(boomerang);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EGG_THROW,
                SoundSource.PLAYERS, 0.6F, 1.1F);
        return true;
    }

    /**
     * The Cat Suit claw: two quick arcs in front of the player.
     *
     * <p>Shorter reach than the tail whack and a tighter box, but it hits twice and applies no
     * knockback. That difference is the whole point of having both — the tail pushes a crowd away,
     * the claw kills the thing in front of you. Knockback would undo the second hit by shoving the
     * target out of the box, so it is left off deliberately rather than forgotten.
     */
    private static boolean clawSwipe(ServerPlayer player, FormDefinition form) {
        Vec3 look = player.getLookAngle();
        Vec3 center = player.position()
                .add(look.scale(1.1D))
                .add(0.0D, player.getBbHeight() / 2.0D, 0.0D);
        AABB box = new AABB(center, center).inflate(0.85D);

        boolean hit = false;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.canBeHitByProjectile())) {
            // Two strikes, resolved in one action so the cooldown still governs the rate.
            float damage = 3.0F + form.actionPower();
            target.hurtServer(player.level(), player.damageSources().playerAttack(player), damage);
            target.invulnerableTime = 0;
            target.hurtServer(player.level(), player.damageSources().playerAttack(player), damage);
            hit = true;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8F, 1.7F);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                    center.x, center.y, center.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
        }
        return true;
    }

    private static boolean tailWhack(ServerPlayer player, FormDefinition form) {
        Vec3 look = player.getLookAngle();
        Vec3 center = player.position().add(look.scale(1.5D)).add(0.0D, player.getBbHeight() / 2.0D, 0.0D);
        AABB box = new AABB(center, center).inflate(1.0D);
        boolean hit = false;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.canBeHitByProjectile())) {
            target.hurtServer(player.level(), player.damageSources().playerAttack(player),
                    4.0F + form.actionPower());
            target.knockback(0.5F, -look.x, -look.z);
            hit = true;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.7F, 1.2F);
        return true;
    }

    private static boolean propellerSpin(ServerPlayer player, FormDefinition form) {
        player.setDeltaMovement(player.getDeltaMovement().x, 1.2D * form.actionPower(), player.getDeltaMovement().z);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(ModEffects.PROPELLER_AURA, 120, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, true));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT,
                SoundSource.PLAYERS, 0.8F, 1.8F);
        return true;
    }

    private static boolean acornGlide(ServerPlayer player, Vec3 aim, FormDefinition form) {
        if (player.onGround()) {
            return false;
        }
        Vec3 horizontal = new Vec3(aim.x, 0.0D, aim.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return false;
        }
        Vec3 impulse = horizontal.normalize().scale(0.6D * form.actionPower());
        player.setDeltaMovement(impulse.x, Math.max(player.getDeltaMovement().y, -0.2D), impulse.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(ModEffects.ACORN_AURA, 120, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, true));
        player.level().playSound(null, player.blockPosition(), SoundEvents.EGG_THROW,
                SoundSource.PLAYERS, 0.5F, 1.2F);
        return true;
    }

    private static boolean cloudStep(ServerPlayer player, FormDefinition form) {
        if (player.onGround()) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.8D * form.actionPower(), player.getDeltaMovement().z);
        } else {
            player.setDeltaMovement(player.getDeltaMovement().x, Math.max(player.getDeltaMovement().y, -0.1D) + 0.6D * form.actionPower(), player.getDeltaMovement().z);
        }
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(ModEffects.CLOUD_AURA, 100, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT,
                SoundSource.PLAYERS, 0.6F, 0.9F);
        return true;
    }

    private static boolean galeDash(ServerPlayer player, Vec3 aim, FormDefinition form, CourseState state) {
        // Traversal bound: air dash only; landing refreshes the charge (see tick()).
        if (player.onGround()) {
            return false;
        }
        Vec3 horizontal = new Vec3(aim.x, 0.0D, aim.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return false;
        }
        Vec3 impulse = horizontal.normalize().scale(form.actionPower());
        player.setDeltaMovement(impulse.x, Math.max(player.getDeltaMovement().y, 0.25D), impulse.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT,
                SoundSource.PLAYERS, 0.7F, 1.5F);
        return true;
    }

    private static boolean magnetPulse(ServerPlayer player, long now) {
        MAGNET_ACTIVE_UNTIL.put(player.getUUID(), now + MAGNET_DURATION_TICKS);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE,
                SoundSource.PLAYERS, 0.7F, 1.6F);
        return true;
    }

    /** Reserve swap: "Server validates safe state, swaps with a short lockout." */
    public static void swapReserve(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (ModeTransitionService.isTransitioning(player)) {
            return;
        }
        CourseState state = CourseStateAccess.get(player);
        FormSlot slot = state.formSlot();
        if (slot.reserve().isEmpty()) {
            return;
        }
        Identifier incoming = slot.reserve().get();
        Optional<FormDefinition> incomingDef = lookup(player, incoming);
        if (incomingDef.isEmpty()) {
            CourseStateAccess.update(player, s -> s.withFormSlot(s.formSlot().withReserve(Optional.empty())));
            return;
        }
        Optional<Identifier> outgoing = slot.active().filter(active ->
                lookup(player, active).map(FormDefinition::reserveEligible).orElse(false));
        FormSlot swapped = new FormSlot(Optional.of(incoming), incomingDef.get().maxCharges(),
                now + RESERVE_SWAP_LOCKOUT_TICKS, outgoing);
        CourseStateAccess.update(player, s -> s.withFormSlot(swapped));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(),
                SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    /** Damage rule: "a normal hit removes the Form first, then one pip." */
    public static boolean absorbHitWithForm(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (state.formSlot().hasActive()) {
            CourseStateAccess.update(player, s -> s.withFormSlot(s.formSlot().loseActive()));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(),
                    SoundSource.PLAYERS, 0.9F, 0.8F);
            return true;
        }
        return false;
    }

    /** Per-player upkeep, called from the server player tick. */
    public static void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        CourseState state = CourseStateAccess.get(player);
        FormSlot slot = state.formSlot();

        // Gale Mantle: "1 charge; refresh on landing."
        if (player.onGround() && slot.hasActive() && slot.charges() == 0) {
            lookup(player, slot.active().get()).ifPresent(form -> {
                if (form.action() == com.studio.planeshift.common.form.FormActionKind.GALE_DASH) {
                    CourseStateAccess.update(player, s -> s.withFormSlot(
                            s.formSlot().withCharges(form.maxCharges(), s.formSlot().cooldownUntil())));
                }
            });
        }

        // Magnet Lantern aura: bounded pull with range and mass cap (items only).
        Long magnetUntil = MAGNET_ACTIVE_UNTIL.get(player.getUUID());
        if (magnetUntil != null) {
            if (now > magnetUntil) {
                MAGNET_ACTIVE_UNTIL.remove(player.getUUID());
            } else {
                AABB range = player.getBoundingBox().inflate(8.0D);
                for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, range)) {
                    Vec3 pull = player.position().add(0.0D, 0.5D, 0.0D).subtract(item.position());
                    double distance = pull.length();
                    if (distance > 0.5D) {
                        item.setDeltaMovement(item.getDeltaMovement().add(pull.normalize().scale(0.08D)));
                    }
                }
            }
        }
    }

    private static Vec3 sanitizeAim(Vec3 raw, ServerPlayer player) {
        if (!Double.isFinite(raw.x) || !Double.isFinite(raw.y) || !Double.isFinite(raw.z)
                || raw.lengthSqr() < 1.0E-6D) {
            return player.getLookAngle();
        }
        return raw.normalize();
    }

    public static void clear(UUID playerId) {
        LAST_ACTION.remove(playerId);
        MAGNET_ACTIVE_UNTIL.remove(playerId);
    }
}
