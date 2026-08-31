package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseDefinition;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Places a datapack-defined structure template at course start if one is configured.
 * This is a skeleton loader; actual .nbt course templates can be added later without
 * changing Java.
 */
public final class CourseStructureService {

    private CourseStructureService() {
    }

    public static void place(ServerLevel level, CourseDefinition course) {
        Optional<net.minecraft.resources.Identifier> structureId = course.structure();
        if (structureId.isEmpty()) {
            return;
        }

        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> template = manager.get(structureId.get());
        if (template.isEmpty()) {
            PlaneShift.LOGGER.warn("Course structure not found: {}", structureId.get());
            return;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(false);
        BlockPos anchor = course.startPos();
        if (!template.get().placeInWorld(level, anchor, anchor, settings, level.getRandom(), 2)) {
            PlaneShift.LOGGER.warn("Failed to place course structure {} at {}", structureId.get(), anchor);
            return;
        }
        PlaneShift.LOGGER.info("Placed course structure {} at {}", structureId.get(), anchor);
    }
}
