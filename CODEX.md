# ChatGPT / Codex Continuation Instructions — PlaneShift

## Communication style (MANDATORY — do not skip)
Act as an ultra-concise assistant. Provide only direct, high-value answers with no filler, pleasantries, or repeating of my prompt back to me. Keep responses short and punchy. If you need more context to complete the task, ask a single clarifying question rather than guessing. This saves token usage and credits.

Use this file as the project-specific system prompt when opening `OkVurb/hand-off` in ChatGPT Codex.

---

You are continuing the PlaneShift project.

Repository: https://github.com/OkVurb/hand-off
Local root (if already cloned): C:\Dev\PlaneShift

High-level context:
- PlaneShift is a NeoForge 1.21.11 Minecraft mod (MDG 2.0.144, Java 21, NeoForge 21.11.45).
- Architecture: common/ (registries, blocks, entities, items, course/mode/rail state, Form framework, networking), server/ (authoritative services), client/ (keybinds, HUD, camera, input, music, renderers, screens).
- Client code must stay in com.studio.planeshift.client.

Start here:
1. Clone or pull the repo (git clone https://github.com/OkVurb/hand-off.git).
2. **Read PROGRESS.md FIRST** — it tells you exactly where the last session stopped and what to do next.
3. Verify Java 21 JDK and the Gradle wrapper.
4. Run .\gradlew build. It should pass with only this-escape warnings.
5. Read HANDOFF.md sections 3.-5 through 3.0 first — they describe everything changed in the last session and, importantly, what the build checks can and cannot catch.
6. Read AGENTS.md for conventions and the invariants each build check enforces. Its **Token
   budget** section is mandatory: The user pays per token and every tool result is re-sent with the whole conversation. Filter command output, batch edits before verifying, escalate compileJava -> test -> build rather than running a full build after every change, and read line ranges instead of whole files. Check whether a task is already done before implementing it.

**CRITICAL — before ending your session, you MUST update PROGRESS.md** with:
- What you did this session
- What is in progress (if anything)
- What the next session should do
- Build status (green/red)

This is the survival mechanism so the next session knows where you stopped if tokens run out.

STATE: all previously-tracked P0/P1/P2 issues are closed. The build is green, CI runs it on every push, and the mod launches cleanly on both client and dedicated server. This is a working vertical slice with placeholder art and audio, not a finished game.

The build enforces five invariants. Read AGENTS.md before working around any of them:
- checkClientClassLeak    — common/server must never reference net.minecraft.client
- checkNoRawCuboidScan    — area block searches go through BlockAreaScan, not BlockPos.betweenClosed
- checkSoundAssets        — every ModSounds entry has a real OGG and a subtitle key
- checkTextureAssets      — every registered block/entity/effect/particle has a texture, and no two textures are byte-identical
- checkBlockModels        — model/blockstate JSON parses and resolves; multi-variant blocks must not render identically in every state; items need an assets/planeshift/items/<id>.json definition; pack.mcmeta must use min_format/max_format with a major above 81

IMPORTANT — what the build cannot catch. Data-pack *schema* errors parse as valid JSON and are only rejected by the codec at load time. A wrong dimension_type schema crashed world creation while the build stayed green for weeks. After touching anything under src/main/resources/data/, run:

    .\gradlew runServer

It loads every data-pack registry headlessly in about a minute and is the cheapest real check. Look for "Done (Xs)! For help, type help" and zero ERROR lines.

Asset generators (standalone, not compiled into the mod — see tools/README.md):
- tools/SoundGen.java   — regenerates all 22 OGGs. Needs ffmpeg with libvorbis. SFX must be MONO; Minecraft only positions mono sources.
- tools/TextureGen.java — regenerates all 96 placeholder PNGs.

Remaining work, roughly by value:

1. Play-testing. Never done; it needs a human at the keyboard. Launch with .\gradlew runClient, then: /planeshift role planeshift:balanced, /planeshift course course_1, and walk the flow — A/D rail movement, coyote time, glider float, shift gates, checkpoint, death below kill_y, Toad shop, /planeshift leave. HANDOFF.md section 3.-1 explains what to watch for in the movement refactor specifically.

2. GameTests. build.gradle already configures a gameTestServer run and sets neoforge.enabledGameTestNamespaces, but ZERO tests exist. 1.21.11 uses the data-driven test_instance registry; NeoForge bridges to Java via RegisterGameTestsEvent (mod bus) with registerTest/registerEnvironment. Block behaviour is the obvious first target: QuestionBlock popping a pickup on a head bump, PSwitch converting and reverting, OnOffSwitch toggling. This is the highest-value automated-testing work available.

3. Test coverage. JUnit 5 is wired up via MDG's neoForge.unitTest with 31 cases, but they only cover the 2.5D input projection geometry (src/test/java/.../PlaneConstrainedInputTest.java).

4. Real art and audio. Everything ships as generated placeholders — flat colours with 2-4 letter labels, and synthesised chiptune. Both are original and license-clean (see ASSET_LICENSES.md) and can be replaced in place at the same paths and sizes.

5. Known smaller issues:
   - src/generated/resources/ is EMPTY, but build.gradle's comment claims datagen output is checked in so a clean clone builds without running the game. Either run runClientData and commit the output, or correct the comment.
   - The built jar ships src/generated/resources/.cache/ because that whole directory is a resource root. Harmless but dead weight.
   - ModEffects gives ice_aura and mini_aura the same colour (0x88CCFF), so their HUD icons match. Registry-level, not a generator bug.
   - this-escape warnings in block constructors. Cosmetic.

Rules:
- Do not commit build/, .gradle/, run/, .mcsources/, or secrets.
- Prefer existing patterns; the codebase favours small focused Gradle verification tasks in the style of checkClientClassLeak over broad frameworks.
- Before changing client/server event wiring, verify which event bus (mod vs game) an event belongs to.
- When unsure of a Minecraft API, read the decompiled source in .mcsources/ rather than guessing. The 1.21.11 schemas for dimension types, pack metadata and item models all changed recently and guessing costs more than looking.
- Verify a new build check by deliberately breaking the thing it guards and confirming it fails. Every check in this repo was validated that way.

Goal for this session: pick one item above, implement it, run .\gradlew build AND .\gradlew runServer, update PROGRESS.md with what you did and what's next, then update HANDOFF.md.
