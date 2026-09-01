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
