package com.studio.planeshift.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.studio.planeshift.common.entity.EnemyRigProfile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every rig moves.
 *
 * <p>Thirteen profiles were drawn and never animated — every fish, both squid-and-urchin
 * newcomers, the Chomp, the Fuzzy, the winged Goomba, the tower bosses and the revived Bowser. They
 * rendered, they had textures, they passed every other check, and they stood perfectly still while
 * the eight older enemies walked around them. Nothing failed, because a static model is a valid
 * model.
 *
 * <p>Reads the source rather than running the renderer, which needs a client. The switch in
 * {@code setupAnim} is the animation, so a profile absent from it is a profile that does not move.
 */
class EveryRigAnimatesTest {

    private static final Path MODEL = findRoot().resolve(
            "src/main/java/com/studio/planeshift/client/render/BespokeEnemyModel.java");

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
    @DisplayName("no rig is left standing still")
    void everyProfileHasAnAnimationCase() throws IOException {
        String source = Files.readString(MODEL, StandardCharsets.UTF_8);
        String animation = source.substring(source.indexOf("public void setupAnim"));

        List<String> cased = new ArrayList<>();
        Matcher matcher = Pattern.compile("case ([A-Z_, ]+?)\s*->").matcher(animation);
        while (matcher.find()) {
            for (String name : matcher.group(1).split(",")) {
                cased.add(name.trim());
            }
        }

        List<String> still = new ArrayList<>();
        for (EnemyRigProfile profile : EnemyRigProfile.values()) {
            if (!cased.contains(profile.name())) {
                still.add(profile.name());
            }
        }
        assertEquals(List.of(), still,
                "these rigs render and never move; a static model is a valid model, which is why "
                        + "nothing else catches this");
    }
}
