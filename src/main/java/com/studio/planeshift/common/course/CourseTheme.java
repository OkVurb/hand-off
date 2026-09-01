package com.studio.planeshift.common.course;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Course world themes that affect ambience, music and sky color.
 */
public enum CourseTheme implements StringRepresentable {
    GRASS("grass", 0xFF55AA22, 0xFF70C0FF),
    DESERT("desert", 0xFFEECC66, 0xFF88CCFF),
    SNOW("snow", 0xFFFFFFFF, 0xFF88AAFF),
    LAVA("lava", 0xFF331111, 0xFFAA3300),
    UNDERGROUND("underground", 0xFF332222, 0xFF000000),
    GHOST_HOUSE("ghost_house", 0xFF221133, 0xFF000000);

    public static final Codec<CourseTheme> CODEC = StringRepresentable.fromEnum(CourseTheme::values);

    private final String name;
    private final int groundColor;
    private final int skyColor;

    CourseTheme(String name, int groundColor, int skyColor) {
        this.name = name;
        this.groundColor = groundColor;
        this.skyColor = skyColor;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int groundColor() {
        return groundColor;
    }

    public int skyColor() {
        return skyColor;
    }
}
