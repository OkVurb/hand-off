package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.entity.EnemyRigProfile;
import com.studio.planeshift.common.entity.BooEntity;
import com.studio.planeshift.common.entity.BoomerangProjectile;
import com.studio.planeshift.common.entity.BowserEntity;
import com.studio.planeshift.common.entity.BowserFire;
import com.studio.planeshift.common.entity.BulletBillEntity;
import com.studio.planeshift.common.entity.BuzzyBeetleEntity;
import com.studio.planeshift.common.entity.EmberBoltEntity;
import com.studio.planeshift.common.entity.FireballProjectile;
import com.studio.planeshift.common.entity.FirebarEntity;
import com.studio.planeshift.common.entity.GoombaEntity;
import com.studio.planeshift.common.entity.IceballProjectile;
import com.studio.planeshift.common.entity.HammerBroEntity;
import com.studio.planeshift.common.entity.HammerProjectile;
import com.studio.planeshift.common.entity.BobOmbEntity;
import com.studio.planeshift.common.entity.BoomerangBroEntity;
import com.studio.planeshift.common.entity.FireBroEntity;
import com.studio.planeshift.common.entity.DryBonesEntity;
import com.studio.planeshift.common.entity.KoopaEntity;
import com.studio.planeshift.common.entity.ParatroopaEntity;
import com.studio.planeshift.common.entity.PodobooEntity;
import com.studio.planeshift.common.entity.LakituEntity;
import com.studio.planeshift.common.entity.MovingPlatformEntity;
import com.studio.planeshift.common.entity.PiranhaPlantEntity;
import com.studio.planeshift.common.entity.SpinyEntity;
import com.studio.planeshift.common.entity.ThwompEntity;
import com.studio.planeshift.common.entity.ToadEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mario-style enemy roster (Design Bible, "Ground enemy archetypes") plus the Ember
 * Core projectile. Enemies never spawn naturally - courses author spawn groups.
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, PlaneShift.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GoombaEntity>> GOOMBA =
            ENTITY_TYPES.register("goomba", () -> EntityType.Builder
                    .of(GoombaEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.GOOMBA.scaled(0.58F), EnemyRigProfile.GOOMBA.scaled(0.58F))
                    .clientTrackingRange(8)
                    .build(key("goomba")));

    public static final DeferredHolder<EntityType<?>, EntityType<KoopaEntity>> KOOPA =
            ENTITY_TYPES.register("koopa", () -> EntityType.Builder
                    .of(KoopaEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.KOOPA.scaled(0.6F), EnemyRigProfile.KOOPA.scaled(1.0F))
                    .clientTrackingRange(8)
                    .build(key("koopa")));

    public static final DeferredHolder<EntityType<?>, EntityType<ParatroopaEntity>> PARATROOPA =
            ENTITY_TYPES.register("paratroopa", () -> EntityType.Builder
                    .of(ParatroopaEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.PARATROOPA.scaled(0.6F),
                            EnemyRigProfile.PARATROOPA.scaled(1.0F))
                    .clientTrackingRange(8)
                    .build(key("paratroopa")));

    public static final DeferredHolder<EntityType<?>, EntityType<DryBonesEntity>> DRY_BONES =
            ENTITY_TYPES.register("dry_bones", () -> EntityType.Builder
                    .of(DryBonesEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.DRY_BONES.scaled(0.6F),
                            EnemyRigProfile.DRY_BONES.scaled(1.0F))
                    .clientTrackingRange(8)
                    .build(key("dry_bones")));

    public static final DeferredHolder<EntityType<?>, EntityType<PodobooEntity>> PODOBOO =
            ENTITY_TYPES.register("podoboo", () -> EntityType.Builder
                    .of(PodobooEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.PODOBOO.scaled(0.7F),
                            EnemyRigProfile.PODOBOO.scaled(0.7F))
                    .clientTrackingRange(8)
                    .build(key("podoboo")));

    public static final DeferredHolder<EntityType<?>, EntityType<BobOmbEntity>> BOB_OMB =
            ENTITY_TYPES.register("bob_omb", () -> EntityType.Builder
                    .of(BobOmbEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.BOB_OMB.scaled(0.6F),
                            EnemyRigProfile.BOB_OMB.scaled(0.7F))
                    .clientTrackingRange(8)
                    .build(key("bob_omb")));

    public static final DeferredHolder<EntityType<?>, EntityType<FireBroEntity>> FIRE_BRO =
            ENTITY_TYPES.register("fire_bro", () -> EntityType.Builder
                    .of(FireBroEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.HAMMER_BRO.scaled(0.6F),
                            EnemyRigProfile.HAMMER_BRO.scaled(1.0F))
                    .clientTrackingRange(8)
                    .build(key("fire_bro")));

    public static final DeferredHolder<EntityType<?>, EntityType<BoomerangBroEntity>> BOOMERANG_BRO =
            ENTITY_TYPES.register("boomerang_bro", () -> EntityType.Builder
                    .of(BoomerangBroEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.HAMMER_BRO.scaled(0.6F),
                            EnemyRigProfile.HAMMER_BRO.scaled(1.0F))
                    .clientTrackingRange(8)
                    .build(key("boomerang_bro")));

    public static final DeferredHolder<EntityType<?>, EntityType<ThwompEntity>> THWOMP =
            ENTITY_TYPES.register("thwomp", () -> EntityType.Builder
                    .of(ThwompEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.THWOMP.scaled(0.95F), EnemyRigProfile.THWOMP.scaled(0.95F))
                    .clientTrackingRange(8)
                    .build(key("thwomp")));

    public static final DeferredHolder<EntityType<?>, EntityType<BulletBillEntity>> BULLET_BILL =
            ENTITY_TYPES.register("bullet_bill", () -> EntityType.Builder
                    .of(BulletBillEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.BULLET_BILL.scaled(0.45F), EnemyRigProfile.BULLET_BILL.scaled(0.45F))
                    .clientTrackingRange(8)
                    .build(key("bullet_bill")));

    public static final DeferredHolder<EntityType<?>, EntityType<BooEntity>> BOO =
            ENTITY_TYPES.register("boo", () -> EntityType.Builder
                    .of(BooEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.BOO.scaled(0.6F), EnemyRigProfile.BOO.scaled(0.6F))
                    .clientTrackingRange(8)
                    .build(key("boo")));

    public static final DeferredHolder<EntityType<?>, EntityType<LakituEntity>> LAKITU =
            ENTITY_TYPES.register("lakitu", () -> EntityType.Builder
                    .of(LakituEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.LAKITU.scaled(0.7F), EnemyRigProfile.LAKITU.scaled(0.95F))
                    .clientTrackingRange(8)
                    .build(key("lakitu")));

    public static final DeferredHolder<EntityType<?>, EntityType<HammerBroEntity>> HAMMER_BRO =
            ENTITY_TYPES.register("hammer_bro", () -> EntityType.Builder
                    .of(HammerBroEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.HAMMER_BRO.scaled(0.6F), EnemyRigProfile.HAMMER_BRO.scaled(1.0F))
                    .clientTrackingRange(8)
                    .build(key("hammer_bro")));

    public static final DeferredHolder<EntityType<?>, EntityType<SpinyEntity>> SPINY =
            ENTITY_TYPES.register("spiny", () -> EntityType.Builder
                    .of(SpinyEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.SPINY.scaled(0.52F), EnemyRigProfile.SPINY.scaled(0.5F))
                    .clientTrackingRange(8)
                    .build(key("spiny")));

    public static final DeferredHolder<EntityType<?>, EntityType<BuzzyBeetleEntity>> BUZZY_BEETLE =
            ENTITY_TYPES.register("buzzy_beetle", () -> EntityType.Builder
                    .of(BuzzyBeetleEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.BUZZY_BEETLE.scaled(0.6F), EnemyRigProfile.BUZZY_BEETLE.scaled(0.45F))
                    .clientTrackingRange(8)
                    .build(key("buzzy_beetle")));

    public static final DeferredHolder<EntityType<?>, EntityType<PiranhaPlantEntity>> PIRANHA_PLANT =
            ENTITY_TYPES.register("piranha_plant", () -> EntityType.Builder
                    .of(PiranhaPlantEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.PIRANHA_PLANT.scaled(0.55F), EnemyRigProfile.PIRANHA_PLANT.scaled(1.15F))
                    .clientTrackingRange(8)
                    .build(key("piranha_plant")));

    public static final DeferredHolder<EntityType<?>, EntityType<EmberBoltEntity>> EMBER_BOLT =
            ENTITY_TYPES.register("ember_bolt", () -> EntityType.Builder
                    .<EmberBoltEntity>of(EmberBoltEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build(key("ember_bolt")));

    public static final DeferredHolder<EntityType<?>, EntityType<HammerProjectile>> HAMMER =
            ENTITY_TYPES.register("hammer", () -> EntityType.Builder
                    .<HammerProjectile>of(HammerProjectile::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build(key("hammer")));

    public static final DeferredHolder<EntityType<?>, EntityType<FireballProjectile>> FIREBALL =
            ENTITY_TYPES.register("fireball", () -> EntityType.Builder
                    .<FireballProjectile>of(FireballProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build(key("fireball")));

    public static final DeferredHolder<EntityType<?>, EntityType<IceballProjectile>> ICEBALL =
            ENTITY_TYPES.register("iceball", () -> EntityType.Builder
                    .<IceballProjectile>of(IceballProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build(key("iceball")));

    public static final DeferredHolder<EntityType<?>, EntityType<BoomerangProjectile>> BOOMERANG =
            ENTITY_TYPES.register("boomerang", () -> EntityType.Builder
                    .<BoomerangProjectile>of(BoomerangProjectile::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build(key("boomerang")));

    public static final DeferredHolder<EntityType<?>, EntityType<ToadEntity>> TOAD =
            ENTITY_TYPES.register("toad", () -> EntityType.Builder
                    .<ToadEntity>of(ToadEntity::new, MobCategory.CREATURE)
                    .sized(EnemyRigProfile.TOAD.scaled(0.5F), EnemyRigProfile.TOAD.scaled(0.8F))
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(key("toad")));

    public static final DeferredHolder<EntityType<?>, EntityType<BowserEntity>> BOWSER =
            ENTITY_TYPES.register("bowser", () -> EntityType.Builder
                    .<BowserEntity>of(BowserEntity::new, MobCategory.MONSTER)
                    .sized(EnemyRigProfile.BOWSER.scaled(1.3F), EnemyRigProfile.BOWSER.scaled(1.7F))
                    .clientTrackingRange(12)
                    .updateInterval(3)
                    .build(key("bowser")));

    public static final DeferredHolder<EntityType<?>, EntityType<BowserFire>> BOWSER_FIRE =
            ENTITY_TYPES.register("bowser_fire", () -> EntityType.Builder
                    .<BowserFire>of(BowserFire::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build(key("bowser_fire")));

    /** Rotating castle hazard. MISC because it is a moving obstacle, not an AI mob. */
    public static final DeferredHolder<EntityType<?>, EntityType<FirebarEntity>> FIREBAR =
            ENTITY_TYPES.register("firebar", () -> EntityType.Builder
                    .<FirebarEntity>of(FirebarEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(key("firebar")));

    public static final DeferredHolder<EntityType<?>, EntityType<MovingPlatformEntity>> MOVING_PLATFORM =
            ENTITY_TYPES.register("moving_platform", () -> EntityType.Builder
                    .<MovingPlatformEntity>of(MovingPlatformEntity::new, MobCategory.MISC)
                    .sized(2.0F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(key("moving_platform")));

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, PlaneShift.id(name));
    }

    public static void onCreateAttributes(EntityAttributeCreationEvent event) {
        event.put(GOOMBA.get(), GoombaEntity.createAttributes().build());
        event.put(PARATROOPA.get(), ParatroopaEntity.createAttributes().build());
        event.put(DRY_BONES.get(), DryBonesEntity.createAttributes().build());
        event.put(PODOBOO.get(), PodobooEntity.createAttributes().build());
        event.put(BOB_OMB.get(), BobOmbEntity.createAttributes().build());
        event.put(BOOMERANG_BRO.get(), BoomerangBroEntity.createAttributes().build());
        event.put(FIRE_BRO.get(), FireBroEntity.createAttributes().build());
        event.put(KOOPA.get(), KoopaEntity.createAttributes().build());
        event.put(TOAD.get(), ToadEntity.createMobAttributes().build());
        event.put(THWOMP.get(), ThwompEntity.createAttributes().build());
        event.put(BULLET_BILL.get(), BulletBillEntity.createAttributes().build());
        event.put(BOO.get(), BooEntity.createAttributes().build());
        event.put(BOWSER.get(), BowserEntity.createAttributes().build());
        event.put(MOVING_PLATFORM.get(), MovingPlatformEntity.createAttributes().build());
        event.put(LAKITU.get(), LakituEntity.createAttributes().build());
        event.put(HAMMER_BRO.get(), HammerBroEntity.createAttributes().build());
        event.put(SPINY.get(), SpinyEntity.createAttributes().build());
        event.put(BUZZY_BEETLE.get(), BuzzyBeetleEntity.createAttributes().build());
        event.put(PIRANHA_PLANT.get(), PiranhaPlantEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
