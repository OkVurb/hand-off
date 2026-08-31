package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Data attachments.
 *
 * <p>{@link #COURSE_STATE} is the per-player authoritative snapshot: serialized with the
 * player (surviving restarts and death via {@code copyOnDeath}) and synced to clients
 * with the full stream codec. Sync is holder-visibility based, so party members can read
 * each other's role/Form/mode for the co-op panel without extra packets.
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PlaneShift.MOD_ID);

    public static final Supplier<AttachmentType<CourseState>> COURSE_STATE =
            ATTACHMENTS.register("course_state", () -> AttachmentType.builder(() -> CourseState.DEFAULT)
                    .serialize(CourseState.CODEC)
                    .copyOnDeath()
                    .sync(CourseState.STREAM_CODEC)
                    .build());

    private ModAttachments() {
    }
}
