package com.studio.planeshift.client.render;

import com.studio.planeshift.common.entity.EnemyRigProfile;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BespokeEnemyModelTest {

    /**
     * Visible cuboid groups per silhouette.
     *
     * <p>Every number here went up in the pass that rebuilt these rigs, because the complaint was
     * that the enemies were squashed and hard to recognise — the fix for that is more parts, not
     * different colours. They are recorded rather than derived so that a rig losing geometry is a
     * test failure and not a shrug.
     *
     * <p>Updating a number here is fine <em>when you meant to</em>, and the commit should say which
     * part changed and why. It is not fine as a way of making a red test go green: a previous pass
     * through this file halved every one of these to accommodate a broken rewrite, which is how a
     * one-part Thwomp shipped. See R13.
     */
    private static final Map<EnemyRigProfile, Long> SOLID_PARTS = Map.ofEntries(
            Map.entry(EnemyRigProfile.GOOMBA, 11L),
            Map.entry(EnemyRigProfile.KOOPA, 14L),
            // A Koopa minus the tail, plus two wings. Deliberately the same body: a Paratroopa is
            // meant to read as "that enemy, but flying", and restyling it would break the two-hit
            // lesson it exists to teach.
            Map.entry(EnemyRigProfile.PARATROOPA, 13L),
            // Koopa proportions with the mass taken off: spine instead of a body, four ribs, thin
            // limbs, a broken shell plate. The player should recognise the species before they
            // recognise that stomping it will not stick.
            Map.entry(EnemyRigProfile.DRY_BONES, 14L),
            // A teardrop: body, head, tapering crown, tip, and four trailing wisps that the
            // animation lags behind the mass. Small, because it is a blob and not a creature.
            Map.entry(EnemyRigProfile.PODOBOO, 8L),
            // Almost all body, which is what makes it read as a bomb rather than a small angry
            // creature: the feet, the wind-up key and the fuse are appendages on a mass.
            Map.entry(EnemyRigProfile.BOB_OMB, 8L),
            Map.entry(EnemyRigProfile.THWOMP, 11L),
            Map.entry(EnemyRigProfile.BULLET_BILL, 11L),
            Map.entry(EnemyRigProfile.BOO, 11L),
            // Same count as BOO, and that equality is the assertion: the big one is the small one
            // at a larger rig scale, so the day these differ somebody has built a second ghost.
            Map.entry(EnemyRigProfile.BIG_BOO, 11L),
            Map.entry(EnemyRigProfile.LAKITU, 12L),
            Map.entry(EnemyRigProfile.HAMMER_BRO, 13L),
            Map.entry(EnemyRigProfile.SPINY, 15L),
            Map.entry(EnemyRigProfile.BUZZY_BEETLE, 14L),
            // Body, head, tail fin, two pectorals, a dorsal and lips. Far fewer parts than
            // anything else here, and deliberately so: a Cheep Cheep is only ever seen side-on
            // because it never turns to face the player, so parts that would only be visible from
            // the front or back would cost geometry nobody ever sees.
            Map.entry(EnemyRigProfile.CHEEP_CHEEP, 7L),
            // The same seven. BIG_CHEEP shares the small fish's mesh deliberately -- its size
            // comes from the registered hitbox and the rig scale, so a second set of boxes would
            // put the difference in two places and let them drift apart.
            Map.entry(EnemyRigProfile.BIG_CHEEP, 7L),
            Map.entry(EnemyRigProfile.PIRANHA_PLANT, 15L),
            // Body, shell, head, snout, jaw, two arms, two legs, a three-piece crest and a
            // two-piece wand. No separate eye boxes, unlike the Koopa it is built from: the crest
            // needs the trim region for its colour, and a box picks a material, so the eyes are
            // painted onto the head's front face instead.
            Map.entry(EnemyRigProfile.KOOPALING, 14L),
            Map.entry(EnemyRigProfile.BOWSER, 19L),
            // Same count as BOWSER, and that is the assertion worth having: the revived form is
            // the same mesh at a larger rig scale, so the day these two numbers differ is the day
            // somebody built a second Bowser by accident.
            Map.entry(EnemyRigProfile.SUPER_BOWSER, 19L));

    @ParameterizedTest(name = "{0} bakes as a complete bespoke mesh")
    @EnumSource(value = EnemyRigProfile.class, names = "TOAD", mode = EnumSource.Mode.EXCLUDE)
    void everyEnemyLayerBakesAndAnimates(EnemyRigProfile profile) {
        ModelPart root = assertDoesNotThrow(() -> BespokeEnemyModel.createLayer(profile).bakeRoot());
        BespokeEnemyModel model = assertDoesNotThrow(() -> new BespokeEnemyModel(root, profile));

        CourseEnemyRenderState state = new CourseEnemyRenderState();
        state.ageInTicks = 17.25F;
        state.walkAnimationPos = 3.5F;
        state.walkAnimationSpeed = 0.8F;
        state.yRot = 22.0F;
        state.xRot = -8.0F;
        assertDoesNotThrow(() -> model.setupAnim(state));

        long solid = root.getAllParts().stream().filter(part -> !part.isEmpty()).count();
        assertEquals(SOLID_PARTS.get(profile), solid,
                () -> profile + " lost or unexpectedly gained a visible cuboid group");
    }
}
