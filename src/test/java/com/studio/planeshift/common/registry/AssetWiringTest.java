package com.studio.planeshift.common.registry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every registered thing must have the asset files the game will ask for at load.
 *
 * <p>This exists because 326 unit tests were green while both custom fluids had no blockstate at
 * all -- the water and lava the whole water theme is built on were rendering as the missing-model
 * placeholder, and a spawn egg had no item definition. Asset wiring is resolved when the game
 * loads, not when Java compiles, so none of it is visible from inside the JVM under test.
 *
 * <p>Launching the game is still the real oracle and the brief now requires it. This is the cheap
 * standing guard underneath: it catches the same class of mistake in two seconds instead of two
 * minutes, and it fails in CI where nobody is watching a client boot.
 *
 * <p>Deliberately checks existence and reference integrity only. Whether a texture looks right is
 * not something a test can answer, and pretending otherwise would be worse than saying so.
 */
class AssetWiringTest {

    private static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("src"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("could not find the repo root");
        }
        return dir;
    }

    private static Path assets() {
        return root().resolve("src/main/resources/assets/planeshift");
    }

    private static String source(String relative) throws IOException {
        return Files.readString(root().resolve(relative), StandardCharsets.UTF_8);
    }

    private static Set<String> matches(String body, String regex) {
        Set<String> found = new LinkedHashSet<>();
        Matcher m = Pattern.compile(regex).matcher(body);
        while (m.find()) {
            for (int g = 1; g <= m.groupCount(); g++) {
                if (m.group(g) != null) {
                    found.add(m.group(g));
                }
            }
        }
        return found;
    }

    @Test
    @DisplayName("every registered block has a blockstate file")
    void blocksHaveBlockstates() throws IOException {
        Set<String> blocks = matches(
                source("src/main/java/com/studio/planeshift/common/registry/ModBlocks.java"),
                "registerBlock\\(\"([a-z_0-9]+)\"|registerSimpleBlock\\(\"([a-z_0-9]+)\"");
        List<String> missing = new ArrayList<>();
        for (String block : blocks) {
            if (!Files.exists(assets().resolve("blockstates/" + block + ".json"))) {
                missing.add(block);
            }
        }
        assertTrue(missing.isEmpty(), "blocks with no blockstate, which render as the "
                + "missing-model placeholder: " + missing);
    }

    @Test
    @DisplayName("every registered item has an item definition")
    void itemsHaveDefinitions() throws IOException {
        Set<String> items = matches(
                source("src/main/java/com/studio/planeshift/common/registry/ModItems.java"),
                "register\\w*\\(\\s*\"([a-z_0-9]+)\"");
        List<String> missing = new ArrayList<>();
        for (String item : items) {
            if (!Files.exists(assets().resolve("items/" + item + ".json"))) {
                missing.add(item);
            }
        }
        assertTrue(missing.isEmpty(),
                "items with no definition under assets/planeshift/items: " + missing
                        + " -- note this is the directory the loader reads, not models/item");
    }

    @Test
    @DisplayName("no model points at a texture that does not exist")
    void modelTexturesExist() throws IOException {
        List<String> broken = new ArrayList<>();
        Pattern ref = Pattern.compile("\"planeshift:([a-z_0-9/]+)\"");
        try (Stream<Path> models = Files.walk(assets().resolve("models"))) {
            for (Path model : models.filter(p -> p.toString().endsWith(".json")).toList()) {
                String body = Files.readString(model, StandardCharsets.UTF_8);
                if (!body.contains("\"textures\"")) {
                    continue;
                }
                Matcher m = ref.matcher(body);
                while (m.find()) {
                    String path = m.group(1);
                    // Model parents are also namespaced; only texture paths live under block/ or
                    // item/ as PNGs, so a parent reference simply will not resolve to one.
                    Path png = assets().resolve("textures/" + path + ".png");
                    Path parent = assets().resolve("models/" + path + ".json");
                    if (!Files.exists(png) && !Files.exists(parent)) {
                        broken.add(model.getFileName() + " -> " + path);
                    }
                }
            }
        }
        assertTrue(broken.isEmpty(), "models referencing something that does not exist: " + broken);
    }

    @Test
    @DisplayName("every entity texture named by a renderer exists")
    void entityTexturesExist() throws IOException {
        Set<String> paths = matches(
                source("src/main/java/com/studio/planeshift/client/ClientModEvents.java"),
                "PlaneShift\\.id\\(\"(textures/entity/[a-z_0-9]+\\.png)\"\\)");
        List<String> missing = new ArrayList<>();
        for (String path : paths) {
            if (!Files.exists(assets().resolve(path))) {
                missing.add(path);
            }
        }
        assertTrue(missing.isEmpty(), "renderers naming a texture that is not on disk: " + missing);
    }

    @Test
    @DisplayName("every registered entity has a renderer")
    void entitiesHaveRenderers() throws IOException {
        Set<String> entities = matches(
                source("src/main/java/com/studio/planeshift/common/registry/ModEntities.java"),
                "EntityType<\\w+>> (\\w+) =");
        String client = source(
                "src/main/java/com/studio/planeshift/client/ClientModEvents.java");
        List<String> missing = new ArrayList<>();
        for (String entity : entities) {
            if (!client.contains("ModEntities." + entity + ".get()")) {
                missing.add(entity);
            }
        }
        assertTrue(missing.isEmpty(),
                "entities with no registered renderer, which crash the client on first sight: "
                        + missing);
    }
}
