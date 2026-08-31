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
        };
        int middle = length / 2;
        return new CourseLayoutPlan(length, new int[][]{
                {26 + shift, 28 + shift},
                {middle - 6, middle - 3},
                {length - 48 - shift / 3, length - 45 - shift / 3}
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
