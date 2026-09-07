package com.studio.planeshift.common.entity;

/**
 * Something that withdraws into a shell and spins.
 *
 * <p>The renderer already drew this, for exactly one class: {@code state.inShell} was extracted
 * with an {@code instanceof KoopaEntity} check. That was right while a Koopa was the only thing
 * that did it. A dense frame pass over the World 1 castle showed the tower bosses doing the same
 * thing -- walk the hall, withdraw, spin-dash along the floor, emerge -- so there are two, and two
 * special cases in an extract method is how a third arrives without anyone choosing it.
 *
 * <p>Small on purpose. It says whether the shell is showing and nothing else: speed, damage and
 * when to withdraw all belong to the entity, because a Koopa's shell and a boss's shell are the
 * same picture and completely different fights.
 */
public interface ShellSpinner {

    /** Whether the shell should be drawn instead of the walking body. */
    boolean spinning();
}
