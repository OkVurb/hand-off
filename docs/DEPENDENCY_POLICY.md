# Dependency Policy

PlaneShift ships with no third-party runtime dependencies.

- All gameplay code is built against official NeoForge and Minecraft APIs.
- Player Animation Library (PAL) is referenced in build comments but not depended on; a built-in fallback is used until a 1.21.11 artifact is available.
- Sodium, Lithium, and Iris are treated as compatibility test targets only, and are installed manually into run profiles rather than declared as dependencies.
