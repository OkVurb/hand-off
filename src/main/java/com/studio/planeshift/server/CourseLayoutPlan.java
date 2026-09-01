package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseTheme;

/** Pure, deterministic geometry plan used by {@link CourseStructureService}. */
record CourseLayoutPlan(int length, int[][] gaps) {

    static CourseLayoutPlan forTheme(CourseTheme theme, int length) {
        int shift = switch (theme) {
            case GRASS -> 0;
            case DESERT -> 3;
            case SNOW -> 6;
            case LAVA -> 9;
            case UNDERGROUND -> 12;
            case GHOST_HOUSE -> 15;
            default -> 0;
        };
        int middle = length / 2;
        return new CourseLayoutPlan(length, new int[][]{
                {22 + shift, 27 + shift}, // 5 block gap
                {38 + shift, 44 + shift}, // 6 block gap
                {middle - 8, middle - 3}, // 5 block gap before checkpoint
                {middle + 10, middle + 16}, // 6 block gap after checkpoint
                {length - 50 - shift / 3, length - 44 - shift / 3}, // 6 block gap
                {length - 35, length - 30} // 5 block gap near end
        });
    }

    int midpoint() {
        return length / 2;
    }

    boolean hasGroundAt(int offset) {
        if (offset <= 14 || offset >= length - 14) {
            return true;
        }
        for (int[] gap : gaps) {
            if (offset >= gap[0] && offset <= gap[1]) {
                return false;
            }
        }
        return true;
    }
}