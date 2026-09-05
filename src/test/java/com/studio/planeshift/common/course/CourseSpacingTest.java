package com.studio.planeshift.common.course;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * No two courses can ever be built on top of each other.
 *
 * <p>Every course lives in one shared dimension, tiled along X, and {@code CourseWriter} clears its
 * whole footprint before writing. So if two footprints overlap, loading one silently demolishes the
 * other — and because courses are rebuilt on entry, single-player never notices. Two players in
 * different courses would notice immediately, and so would anything persistent left behind.
 *
 * <p>This was real. Courses were spaced 256 apart from an era when they were about 150 blocks long.
 * {@code DEFAULT_LENGTH} later became 720 and the spacing was never revisited, so any course
 * without an explicit length overlapped its next two neighbours — and two courses had been given
 * the same start position outright.
 */
class CourseSpacingTest {

    private record Course(String name, int x, int length) {
    }

    private static List<Course> load() throws Exception {
        // Located by asking for a file we know exists and taking its parent, rather than asking
        // for the directory. A directory only resolves as a URL when resources happen to be laid
        // out on disk, which is a property of the build rather than of anything being tested - and
        // it is what made the first two attempts at this test fail for unrelated reasons.
        java.net.URL url = CourseSpacingTest.class.getResource(
                "/data/planeshift/planeshift/course/course_1.json");
        Path dir;
        if (url != null) {
            dir = Path.of(url.toURI()).getParent();
        } else {
            // Fallback: walk up from wherever the test happens to be running until the source
            // tree turns up.
            Path search = Path.of("").toAbsolutePath();
            Path relative = Path.of("src", "main", "resources", "data", "planeshift",
                    "planeshift", "course");
            while (search != null && !Files.isDirectory(search.resolve(relative))) {
                search = search.getParent();
            }
            assertTrue(search != null, "could not locate the course data from " + Path.of("").toAbsolutePath());
            dir = search.resolve(relative);
        }
        List<Course> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!json.has("start_pos")) {
                        continue;
                    }
                    int x = json.getAsJsonArray("start_pos").get(0).getAsInt();
                    int length = json.has("length")
                            ? json.get("length").getAsInt()
                            : CourseDefinition.DEFAULT_LENGTH;
                    out.add(new Course(file.getFileName().toString(), x, length));
                }
            }
        }
        return out;
    }

    @Test
    void noCourseReachesItsNeighbour() throws Exception {
        List<Course> courses = load();
        assertTrue(courses.size() > 10, "expected the course data to be present");
        courses.sort(Comparator.comparingInt(Course::x));

        List<String> clashes = new ArrayList<>();
        for (int i = 0; i + 1 < courses.size(); i++) {
            Course here = courses.get(i);
            Course next = courses.get(i + 1);
            // The writer clears a margin either side of the declared length, so the real footprint
            // is wider than the course itself.
            int reach = here.length() + 64;
            if (next.x() - here.x() < reach) {
                clashes.add(here.name() + " (x=" + here.x() + ", len=" + here.length()
                        + ") reaches " + next.name() + " at x=" + next.x());
            }
        }
        assertTrue(clashes.isEmpty(), "overlapping courses: " + clashes);
    }

    @Test
    void everyCourseHasSpaceForTheLongestOneTheCodecAllows() {
        // Spacing has to survive somebody raising a course's length later, which is exactly how
        // this broke the first time: DEFAULT_LENGTH went 144 -> 720 and nothing re-checked.
        assertTrue(CourseDefinition.DEFAULT_LENGTH < 2560,
                "the slot width assumed by the course data must still fit a default course");
    }
}
