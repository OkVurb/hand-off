package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * C2S: "use my Form action, aimed here."
 *
 * <p>Networking table (Design Bible, "Multiplayer and networking"): validation is
 * ownership, charges, state, target bounds and cooldown — all in {@code FormService}.
 * The aim is treated as untrusted and re-normalized server-side.
 */
public record FormActionPayload(Vec3 aim) implements CustomPacketPayload {

    public static final Type<FormActionPayload> TYPE = new Type<>(PlaneShift.id("form_action"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, FormActionPayload> STREAM_CODEC =
            Vec3.STREAM_CODEC.map(FormActionPayload::new, FormActionPayload::aim);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
