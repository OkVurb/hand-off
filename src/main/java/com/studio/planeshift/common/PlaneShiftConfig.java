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

    /** Camera comfort and accessibility (Design Bible, "Accessibility and input"). */
    public static final class Client {
        public final ModConfigSpec.DoubleValue cameraSmoothing;
        public final ModConfigSpec.DoubleValue lookAheadScale;
        public final ModConfigSpec.BooleanValue reducedMotion;
        public final ModConfigSpec.BooleanValue showModeBadge;
        public final ModConfigSpec.BooleanValue showDebugHud;

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
            builder.pop();
        }
    }

    /** Server-authoritative gameplay rules. */
    public static final class Server {
        public final ModConfigSpec.IntValue transitionDurationTicks;
        public final ModConfigSpec.BooleanValue allowManualShift;

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
        }
    }
}
