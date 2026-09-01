# CurseForge playtest instance

Path: `C:\Users\cr0od\curseforge\minecraft\Instances\PlaneShift Playtest`

Target: **Minecraft 1.21.11 / NeoForge 21.11.45** — correct for PlaneShift, which declares
`minecraft [1.21.11,1.22)` and `neoforge [21.11.0,)`.

Build the mod with `.\gradlew build` and copy `build/libs/planeshift-0.1.0.jar` into the
instance's `mods/` folder to update it.

## The version problem

The instance is 1.21.11 but a large share of the installed mods are **1.21.1** builds. That is a
different Minecraft version, not a patch difference — 1.21.1 to 1.21.11 changed the screen input
API (`mouseClicked`/`keyPressed` now take `MouseButtonEvent`/`KeyEvent` records), the render
pipeline, and item construction (`Item` cannot be constructed outside registration).

Check any jar's real declared range rather than trusting its filename:

```bash
unzip -p <mod>.jar META-INF/neoforge.mods.toml | grep -A3 'modId *= *"minecraft"'
```

### Verified by launching

The instance now launches. Getting there took removing every 1.21.1 build; with them present the
process died silently between mixin configuration and Minecraft's own `main`, producing no
exception, no crash report and no log line — the worst possible failure to read. With them gone,
the game reaches `Setting user`, initialises OpenAL, loads a world and runs.

A clean-room launch (PlaneShift alone, same launcher, same arguments) worked throughout, which is
what proved the launcher was fine and the mod set was not.

**Remove 1.21.1 builds. Do not try to make them work.** They are not "probably fine" — they are
the entire reason this instance would not start.

### Entity Model Features crashes on the first-person hand

A correct 1.21.11 build that still crashes:

```
NullPointerException: EMFState.state() returned null
  at entity_model_features $emf$endRender (ModelPartFeatureRenderer)
  at ItemInHandRenderer.renderHandsWithItems
```

It survives 80 seconds and a thousand ticks and then dies the moment a hand is drawn, because a
held item has no entity render state for EMF to read. Disabled. Nothing in PlaneShift needs it —
the enemy models are bespoke geometry, not resource-pack overrides.

### Crashers — load fine, then die during mixin setup

Confirmed by launching, not predicted. Both are 1.21.1 builds calling FML static methods whose
signatures changed in 21.11, which throws `IncompatibleClassChangeError` from a mixin plugin
constructor — long before any PlaneShift code runs.

| Mod | Failure |
|---|---|
| FerriteCore 7.0.3 | `FMLLoader.getLoadingModList()` and `isProduction()` no longer match |
| ModernFix 5.27.24 | same, via its own mixin plugin |

Both renamed to `.jar.disabled`. This is the failure mode worth internalising: they passed every
version check FML performs, appeared in the mod list, and still killed the game. A declared
version range says nothing about whether the code inside matches.

### Dependency blockers — caught at load

`musicnotification` required `cloth_config 21.11.153+` and found `15.0.140`, the 1.21.1 build.
Resolved by installing the real 1.21.11 Cloth Config.

### Hard blockers — NeoForge refuses these outright

Renamed to `.jar.disabled` in the instance. Rename back to re-enable.

| Mod | Declared minecraft range |
|---|---|
| ImmediatelyFast | `[1.21,1.21.1]` |
| Sound Physics Remastered | `[1.21,1.21.1]` |
| Clumps | `[1.21.1]` |
| Controlling | `[1.21.1]` |
| EntityCulling | `[1.21.1]` |

### Loads but unverified — 1.21.1 builds with open-ended ranges

These declare something like `[1.21,)`, so FML accepts them on 1.21.11 even though they were
compiled against 1.21.1. A declared range is a promise, not a guarantee; the ones that mixin into
rendering, mod loading or screens are the likely crash sources.

Jade, MouseTweaks, NoChatReports, AppleSkin, **Cloth Config**, **Embeddium**, **FancyMenu**,
FerriteCore, Konkrete, **ModernFix**.

Bolded ones are the deep-mixin risks. Replace with genuine 1.21.11 builds before trusting a
playtest result — a crash from one of these looks exactly like a crash in PlaneShift.

### Correct 1.21.11 builds already installed

BetterAdvancements, GlitchCore, ParticleEffects, SereneSeasons, AttributeFix, Configured,
Entity Model Features, Entity Texture Features, Melody, Music Notification, Prickle,
Smart Particles, PlaneShift itself.

## Movement mods conflict with the 2.5D rail

`enhanced-movement` and `omni` are both correct 1.21.11 builds, so they load cleanly — but they
are a design conflict, not a technical one. PlaneShift's 2.5D mode rewrites `moveVector` in
`PlaneConstrainedInput`, adds velocity in `PlaneMovementAssists`, and `MovementRuleService` snaps
the player back whenever depth drift exceeds the corridor tolerance.

A movement mod that adds its own velocity on the depth axis will therefore be fought by the
server every tick, which reads as stuttering or rubber-banding rather than as a mod conflict.
If 2.5D movement feels wrong, disable these two before investigating PlaneShift.

## FancyMenu

Not yet configured. `config/fancymenu/` does not exist, because FancyMenu generates its
customization structure on **first launch** — and the installed build is the 1.21.1 one listed
above. Authoring a layout before it has run once means guessing at a format that may not
initialise.

Order of operations:

1. Install the 1.21.11 builds of FancyMenu, Konkrete and Cloth Config.
2. Launch once so `config/fancymenu/customization/` is generated.
3. Author the main-menu layout against the real generated structure.

## Launching without the CurseForge launcher

Mod loading happens before authentication, so a mod-loading crash reproduces on an offline launch
with a dummy token — no account credentials needed. Reconstruct the command from the `-cp` and
JVM args recorded in `logs/instance_audit.txt`, then:

```
net.neoforged.fml.startup.Client --gameDir <instance> --assetsDir <install>\assets
  --assetIndex 29 --version neoforge-21.11.45 --accessToken 0 --userType legacy
```

Asset index `29` is 1.21.11 (from `Install/versions/1.21.11/1.21.11.json`).

This is only good for reproducing load-time failures. Anything past the main menu needs a real
session.
