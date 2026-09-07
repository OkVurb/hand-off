# Night log

Unattended iterations, newest last. Honest about failures; a quiet iteration is a real entry.

## Iteration 1

Plan item 5, per-world interior tinting. UNDERGROUND was one grey cave shared by every world,
which collapsed six distinct interiors into one and made the most common transition in the game --
surface to cave and back -- read as leaving the world rather than going under it. Caves are now cut
from the rock of the world around them: sandstone over sand in the desert, ice in the snow world,
basalt under the volcano.

GenContext carries a worldTheme alongside its theme, defaulting to the theme itself so an untinted
course is exactly the old behaviour. CourseStructureService resolves it through
WorldRegistry.worldForCourse, which already existed -- I wrote a hand-rolled stream version first
and replaced it once I found the real one.

Also fixed in passing: the LAVA palette's hazard was still vanilla Blocks.LAVA despite the custom
fluid existing. A course pit was filling with Minecraft lava, not ours, so none of the fog or tint
work applied to the place it was most meant for.

282 tests, no failures, verified by counting result XML rather than trusting the build line.

## Iteration 2

Finished wiring item 4. MovingPlatformEntity now marks out the two ends of its travel with a
sparse line at its own height. A platform is only a fair jump if the player can see where it goes
while it is still at the near end; without that the choice is between waiting a full cycle to learn
the range and guessing.

Only the endpoints and the run between them are drawn, at the platform's own height, so the line
reads as this platform's path rather than as a floor near it.

Firebar and moving platform are now both telegraphed. Telegraph.arc is still unused -- it is
waiting on the pendulum platforms, which do not exist yet.

282 tests, no failures, counted from XML.
