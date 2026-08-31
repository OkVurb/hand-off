# Animation Adapter / PAL Fallback

PlaneShift uses a built-in pose driver for the placeholder enemy rig. The Player Animation Library (PAL) is not a compile or runtime dependency because no 1.21.11 artifact is currently available.

When a compatible PAL artifact is released, the mod can be updated to animate the placeholder rig without changing the `CourseEnemyRenderer` or `PlaceholderRigModel` contracts.
