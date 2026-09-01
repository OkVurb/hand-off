package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseProgress;
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

    /**
     * The save file: cleared courses, star coin counts, best scores.
     *
     * <p>Deliberately not {@code copyOnDeath}-dependent for correctness — it is copied on death
     * like the state attachment, but unlike the state it is never reset by loading a course.
     * Dying in a course must not cost a player the twelve courses they already beat.
     */
    public static final Supplier<AttachmentType<CourseProgress>> COURSE_PROGRESS =
            ATTACHMENTS.register("course_progress", () -> AttachmentType.builder(() -> CourseProgress.DEFAULT)
                    .serialize(CourseProgress.CODEC)
                    .copyOnDeath()
                    .sync(CourseProgress.STREAM_CODEC)
                    .build());

    private ModAttachments() {
    }
}
