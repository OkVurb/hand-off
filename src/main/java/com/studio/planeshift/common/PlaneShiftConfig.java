package com.studio.planeshift.common;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config split per the save-domain rules (Design Bible, "Saving, config, and
 * migration"): client config owns camera comfort and HUD with "no authority over
 * rewards"; server config owns rules.
 */
public final class PlaneShiftConfig {

    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        CLIENT = new Client(client);
        CLIENT_SPEC = client.build();

        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        SERVER = new Server(server);
        SERVER_SPEC = server.build();
    }

    private PlaneShiftConfig() {
    }

    /** Camera comfort, HUD and accessibility (Design Bible, "Accessibility and input"). */
    public static final class Client {
        public final ModConfigSpec.DoubleValue cameraSmoothing;
        public final ModConfigSpec.DoubleValue lookAheadScale;
        public final ModConfigSpec.BooleanValue reducedMotion;
        public final ModConfigSpec.BooleanValue showModeBadge;
        public final ModConfigSpec.BooleanValue showDebugHud;
        public final ModConfigSpec.DoubleValue hudScale;
        public final ModConfigSpec.BooleanValue hurryUpMusic;

        Client(ModConfigSpec.Builder builder) {
            builder.push("camera");
            cameraSmoothing = builder
                    .comment("Camera smoothing strength in 2.5D mode (0 = rigid, 1 = floaty).")
                    .defineInRange("cameraSmoothing", 0.35D, 0.0D, 1.0D);
            lookAheadScale = builder
                    .comment("Scale applied to the authored camera look-ahead (comfort slider).")
                    .defineInRange("lookAheadScale", 1.0D, 0.0D, 2.0D);
            reducedMotion = builder
                    .comment("Reduced motion: shorter camera blends, no shake, low parallax.",
                            "Transaction timing is identical; only presentation changes.")
                    .define("reducedMotion", false);
            builder.pop();

            builder.push("hud");
            showModeBadge = builder
                    .comment("Show the 2.5D/3D mode badge during play and transitions.")
                    .define("showModeBadge", true);
            showDebugHud = builder
                    .comment("Developer overlay: state, mode, rail, drift, form, transaction.")
                    .define("showDebugHud", false);
            hudScale = builder
                    .comment("Extra scale applied to the course HUD on top of the game's GUI scale.",
                            "Raise it on a large display where the coin and clock readouts get lost.")
                    .defineInRange("hudScale", 1.0D, 0.5D, 3.0D);
            builder.pop();

            builder.push("audio");
            hurryUpMusic = builder
                    .comment("Speed the course track up when the clock drops under 100 seconds.",
                            "Turn off if the pitch shift is uncomfortable; the HUD still warns.")
                    .define("hurryUpMusic", true);
            builder.pop();
        }
    }

    /** Server-authoritative gameplay rules. */
    public static final class Server {
        public final ModConfigSpec.IntValue transitionDurationTicks;
        public final ModConfigSpec.BooleanValue allowManualShift;
        public final ModConfigSpec.DoubleValue courseJumpBoost;
        public final ModConfigSpec.DoubleValue courseRunBoost;
        public final ModConfigSpec.DoubleValue conveyorSpeed;

        Server(ModConfigSpec.Builder builder) {
            builder.push("transitions");
            transitionDurationTicks = builder
                    .comment("Perspective blend duration in ticks (bible window: 12-18 = 0.6-0.9 s).")
                    .defineInRange("transitionDurationTicks", 14, 8, 40);
            allowManualShift = builder
                    .comment("Allow non-operator players to shift modes by command (courses",
                            "normally authorize shifts only through gates).")
                    .define("allowManualShift", false);
            builder.pop();

            builder.push("movement");
            courseJumpBoost = builder
                    .comment("Extra jump strength while inside a course, as a fraction of the base.",
                            "JUMP_STRENGTH is an initial velocity, so height goes with its square:",
                            "1.3 gives 2.3x the launch speed and roughly five times the height,",
                            "about 6 blocks against vanilla's 1.25. That is sized off the course",
                            "generator, which puts platforms at 3 to 5 blocks and question blocks",
                            "at 3 - vanilla cannot reach either, which is why courses felt unfair.")
                    .defineInRange("courseJumpBoost", 1.3D, 0.0D, 6.0D);
            courseRunBoost = builder
                    .comment("Extra ground speed while inside a course, as a fraction of the base.",
                            "0.9 means running at 1.9 times vanilla walking speed. Momentum is",
                            "what makes a gap feel jumpable; at vanilla speed the same gap reads",
                            "as unfair.")
                    .defineInRange("courseRunBoost", 0.9D, 0.0D, 4.0D);
            builder.pop();

            builder.push("blocks");
            conveyorSpeed = builder
                    .comment("Terminal drift a conveyor belt imparts, in blocks per tick.",
                            "This is a target the belt eases entities toward, not an impulse it",
                            "adds every tick, so a player who walks against it wins. Raise it to",
                            "make belts a hazard; lower it to make them scenery.")
                    .defineInRange("conveyorSpeed", 0.035D, 0.0D, 0.4D);
            builder.pop();
        }
    }
}
