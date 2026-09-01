package com.studio.planeshift.common.network;

import com.studio.planeshift.server.CourseService;
import com.studio.planeshift.server.TesterService;
import com.studio.planeshift.server.FormService;
import com.studio.planeshift.server.ToadShopService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration (Design Bible, "Multiplayer and networking").
 *
 * <p>"Every packet has direction, size limit, validation rule, rate limit, and version
 * field." Direction is enforced by {@code playToServer}; the registrar version stamps
 * the channel; sizes are fixed-small by codec construction; validation and rate limits
 * live in the handling services. Mode changes intentionally have no C2S payload —
 * they only enter through gate contact server-side.
 */
public final class ModNetworking {

    /** Channel version. Bump on any wire-format change and document in CHANGELOG. */
    public static final String PROTOCOL_VERSION = "1";

    private ModNetworking() {
    }

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(FormActionPayload.TYPE, FormActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        FormService.useAction(player, payload.aim());
                    }
                }));

        registrar.playToServer(ReserveSwapPayload.TYPE, ReserveSwapPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        FormService.swapReserve(player);
                    }
                }));

        registrar.playToServer(CourseSelectPayload.TYPE, CourseSelectPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        CourseService.loadCourse(player, payload.courseId());
                    }
                }));

        // Client-only screen payloads are registered here without handlers; the client
        // assigns handlers in RegisterClientPayloadHandlersEvent. This keeps the payloads
        // in the server-side registry so S2C negotiation succeeds on dedicated servers.
        registrar.playToClient(OpenTitleScreenPayload.TYPE, OpenTitleScreenPayload.STREAM_CODEC);
        registrar.playToClient(OpenCourseMapPayload.TYPE, OpenCourseMapPayload.STREAM_CODEC);
        registrar.playToClient(OpenToadShopPayload.TYPE, OpenToadShopPayload.STREAM_CODEC);
        registrar.playToClient(ScorePopupPayload.TYPE, ScorePopupPayload.STREAM_CODEC);
        registrar.playToClient(CourseResultsPayload.TYPE, CourseResultsPayload.STREAM_CODEC);
        registrar.playToClient(GameOverPayload.TYPE, GameOverPayload.STREAM_CODEC);
        registrar.playToClient(OpenTesterPayload.TYPE, OpenTesterPayload.STREAM_CODEC);

        registrar.playToServer(LeaveCoursePayload.TYPE, LeaveCoursePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        CourseService.leaveCourse(player);
                    }
                }));

        registrar.playToServer(TesterActionPayload.TYPE, TesterActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        TesterService.handle(player, payload.action(), payload.arg());
                    }
                }));

        registrar.playToServer(ToadShopPurchasePayload.TYPE, ToadShopPurchasePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        ToadShopService.purchase(player, payload.slot());
                    }
                }));
    }
}
