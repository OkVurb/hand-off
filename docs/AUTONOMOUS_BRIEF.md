# Standing brief for unattended work

The owner is asleep. This file is the instruction set for a loop that runs without anyone to ask,
so it has to answer the questions that would otherwise be asked. Read it at the start of every
iteration; it is the only thing guaranteed to survive a context reset.

## The job

Work down `docs/WORK_PLAN.md` section 8, in order, skipping anything already done. The plan was
built from a complete read of 108 contact sheets and is the authority on what to build and why.

Done so far: item 1 (background-plane boss), item 2 (per-face blocks), item 3 (the five shipped-code
corrections), item 4 (trajectory telegraph, firebar wired). Next is item 5, per-world interior
tinting, then item 6 (water), item 7 (sub-environments), item 8 (progression links).

## The genre rule, added after a correction

The owner reviewed a night of this work and said, in effect: make it feel like the game it is
imitating. Two things were wrong in the same way, so the rule is written here rather than learned
again.

**Telegraphs are physical objects, never overlays.** A grinder's route is visible because the track
is really there. A spiked ball's arc is visible because the chain is really there. A fire bar's
warning is its own fireballs. Nothing is painted on top of the world. Particle rings and glowing
dotted lines say the right thing in a language this genre does not speak.

**Hazards are solid, chunky and readable.** Do not represent an object with a cloud of particles and
no model. That shortcut is correct for a fire bar, whose flames genuinely are the thing, and wrong
for anything that is meant to be an object.

**Names come from the vocabulary already in the mod.** It has Goombas, Koopas, Thwomps and Boos. A
new hazard called "Saw" or "Chain Ball" is a stranger in that list.

## Rules

**Commit early and often.** Every finished piece gets its own commit and push. Git is the safety
net that makes unattended work reversible, and a large uncommitted working tree is the one state
that is genuinely hard to undo.

**Run the game, not just the tests.** `./gradlew runGameTestServer` loads real Minecraft, runs the
in-world GameTests and exits -- about two minutes, headless, no window. It catches an entire class
of bug the unit suite structurally cannot: asset wiring is resolved at load, so a block with no
blockstate, a model pointing at a texture that does not exist, or an item with no model all pass 326
unit tests and then render as the missing-model placeholder.

This is not hypothetical. Thirty commits went in over one night with unit tests only, and the first
launch immediately found that both custom fluids had no blockstate at all -- the water and lava the
entire water theme was built on were rendering as placeholders. Grep the log for `Missing model`,
`No model loaded`, `Unable to load`, and `Failed to`.

**Never commit without a real test run.** `./gradlew test --rerun-tasks`, then count the XML in
`build/test-results/test/`. An up-to-date build prints BUILD SUCCESSFUL having run nothing, and
that exact trap has already produced one broken commit in this project.

**Stay inside the repo.** `C:\Dev\PlaneShift` only. No system settings, no installs, no other
directories.

**Do not spend money.** No paid APIs, no image generation, no purchases. If a task needs the
inference.sh credits, skip it and leave a note.

**Do not publish anything.** No PRs, no issues, no posts, no external services. Pushing to the
project's own remote is expected; anything that puts work in front of other people is not.

**Do not delete or rewrite history.** No force pushes, no rebases, no reverting the owner's own
commits.

**When blocked, move on and write it down.** Something needing a decision that only the owner can
make goes in `docs/BACKLOG.md` with the reasoning, and the loop moves to the next item. Do not
guess at product decisions and do not stall waiting.

**Prefer reversible work.** Given a choice between a large speculative refactor and a small
verifiable improvement, take the small one. Nobody is watching, which is a reason for more caution
rather than less.

## Reporting

Keep `docs/NIGHT_LOG.md` current: one short entry per iteration saying what was attempted, what
landed, what failed and why. It is the first thing the owner reads in the morning, so it should be
honest about failures rather than a list of wins. A quiet iteration that found nothing worth doing
is a legitimate entry.

## On stopping

If the plan runs out, stop and say so in the log rather than inventing scope. Finished is a
perfectly good place to be.
