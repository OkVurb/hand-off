package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModBlocks;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CaveStructurePaletteTest {
    @ParameterizedTest
    @EnumSource(value = CourseTheme.class, names = {"DESERT", "SNOW", "LAVA", "GHOST_HOUSE"})
    void caveStructureBelongsToItsWorld(CourseTheme world) {
        var palette = GenContext.Palette.forTheme(CourseTheme.UNDERGROUND, world);
        var material = switch (world) {
            case DESERT -> ModBlocks.COURSE_SANDSTONE.get();
            case SNOW -> ModBlocks.COURSE_ICE_BLOCK.get();
            case LAVA -> ModBlocks.COURSE_BASALT.get();
            case GHOST_HOUSE -> ModBlocks.COURSE_GHOST_BEAM.get();
            default -> throw new AssertionError(world);
        };
        // These are repeated structural surfaces, not the rare reward/hazard accents.
        assertTrue(palette.accent().is(material), "cave structural accents lose their world family");
        assertTrue(palette.platform().is(material), "cave ledges lose their world family");
    }
}
