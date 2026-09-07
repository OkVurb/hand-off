package com.studio.planeshift.common.course;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every theme has the art its own code goes looking for.
 *
 * <p>{@code CourseSkyboxRenderer} builds its texture path out of the theme's serialized name. That
 * is a nice piece of design and it has exactly one failure mode: adding a theme silently adds a
 * demand for a file nobody drew. Two of the eight had been sitting like that -- water and sky
 * courses were asking for skyboxes that did not exist, and nothing anywhere said so, because a
 * missing texture is not an error in Minecraft, it is a purple chequerboard.
 *
 * <p>Reads the files rather than the registry, because the registry is not what is missing.
 */
class ThemeAssetTest {

    private static final Path ROOT = findRoot();

    private static Path findRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("tools"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("could not find repo root");
        }
        return dir;
    }

    @Test
    void everyThemeHasASkybox() {
        Path environment = ROOT.resolve(
                "src/main/resources/assets/planeshift/textures/environment");
        List<String> missing = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            String name = "course_skybox_" + theme.getSerializedName() + ".png";
            if (!Files.isRegularFile(environment.resolve(name))) {
                missing.add(name);
            }
        }
        assertEquals(List.of(), missing,
                "CourseSkyboxRenderer will ask for these and get a purple chequerboard");
    }
}
