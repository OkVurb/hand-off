package com.studio.planeshift.common.course;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a world map: where each node sits, what kind it is, and which nodes it connects to.
 *
 * <p>A straight row of ten buttons is a menu. A world map is a <em>place</em> — a winding path
 * with a Toad House off to one side, a cannon that skips ahead, and a castle at the end you can
 * see coming from the first node. The difference is not decoration: it is that the player can look
 * at it and understand the shape of what is ahead, which is the entire job of a level select.
 *
 * <p>Layout is computed rather than authored so a world with a different course count still
 * produces a sensible map, and it is deterministic so the map does not move between sessions —
 * a map that reshuffles is a map nobody can build a mental image of.
 *
 * <p>Lives in {@code common} because the client draws it and the server validates against it. A
 * client that could invent its own nodes could invent a Toad House next to the final castle.
 */
public final class WorldMapLayout {

    /** What a node is, which decides both how it draws and what happens when it is entered. */
    public enum NodeType {
        /** An ordinary course. */
        COURSE,
        /** The world's final course. Always last, always drawn as a castle. */
        CASTLE,
        /** A free power-up, once per save. */
        TOAD_HOUSE,
        /** Skips ahead to a later world once unlocked. */
        CANNON,
        /** Where the player token starts. Not enterable. */
        START
    }

    /**
     * One point on the map.
     *
     * @param id       course id for COURSE and CASTLE; a synthetic id for the rest
     * @param type     what this node is
     * @param x        horizontal position, 0 to 1 across the map area
     * @param y        vertical position, 0 to 1 down the map area
     * @param index    position in the world's course list, or -1 for non-course nodes
     */
    public record Node(String id, NodeType type, float x, float y, int index) {
        public boolean isPlayable() {
            return type == NodeType.COURSE || type == NodeType.CASTLE;
        }
    }

    /** A drawn path between two nodes. */
    public record Link(int from, int to) {
    }

    private final List<Node> nodes;
    private final List<Link> links;

    private WorldMapLayout(List<Node> nodes, List<Link> links) {
        this.nodes = List.copyOf(nodes);
        this.links = List.copyOf(links);
    }

    public List<Node> nodes() {
        return nodes;
    }

    public List<Link> links() {
        return links;
    }

    public Node node(int index) {
        return nodes.get(index);
    }

    /** Index of the node for a course id, or -1. */
    public int indexOfCourse(String courseId) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id().equals(courseId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Builds the map for a world.
     *
     * <p>The path serpentines: courses run left to right along a row, drop down, then run right to
     * left along the next. That is how every Mario world map is laid out and it is not arbitrary —
     * it keeps the whole world on one screen without the path ever crossing itself, so the route is
     * unambiguous at a glance.
     *
     * <p>The Toad House hangs <em>off</em> the path rather than sitting on it, because it is
     * optional and the layout should say so before the player reads any text.
     */
    public static WorldMapLayout forWorld(WorldDefinition world) {
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();

        List<String> courses = world.courseIds();
        int count = courses.size();
        int perRow = 5;
        int rows = (count + perRow - 1) / perRow;

        // Start marker, just before the first course.
        nodes.add(new Node("start", NodeType.START, 0.06f, rowY(0, rows), -1));

        int previous = 0;
        for (int i = 0; i < count; i++) {
            int row = i / perRow;
            int column = i % perRow;
            // Reverse every other row so the path snakes instead of jumping back across the map.
            boolean reversed = row % 2 == 1;
            int drawColumn = reversed ? (perRow - 1 - column) : column;

            float x = 0.14f + drawColumn * (0.72f / (perRow - 1));
            float y = rowY(row, rows);

            boolean last = i == count - 1;
            NodeType type = last ? NodeType.CASTLE : NodeType.COURSE;
            nodes.add(new Node(courses.get(i), type, x, y, i));
            int current = nodes.size() - 1;
            links.add(new Link(previous, current));
            previous = current;

            // A Toad House after the third course, offset above the path.
            if (i == 2) {
                nodes.add(new Node("toad_house_" + world.worldId(), NodeType.TOAD_HOUSE,
                        x + 0.06f, y - 0.17f, -1));
                links.add(new Link(current, nodes.size() - 1));
            }
            // A cannon halfway, offset below.
            if (i == 5) {
                nodes.add(new Node("cannon_" + world.worldId(), NodeType.CANNON,
                        x - 0.05f, y + 0.16f, -1));
                links.add(new Link(current, nodes.size() - 1));
            }
        }

        return new WorldMapLayout(nodes, links);
    }

    /** Rows are spread down the map area with margins top and bottom. */
    private static float rowY(int row, int rows) {
        if (rows <= 1) {
            return 0.5f;
        }
        return 0.26f + row * (0.48f / (rows - 1));
    }
}
