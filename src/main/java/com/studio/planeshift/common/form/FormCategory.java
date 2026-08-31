package com.studio.planeshift.common.form;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Form catalog categories (Design Bible, "Power-up framework"). */
public enum FormCategory implements StringRepresentable {
    OFFENSE("offense"),
    TRAVERSAL("traversal"),
    DEFENSE("defense"),
    UTILITY("utility");

    public static final Codec<FormCategory> CODEC = StringRepresentable.fromEnum(FormCategory::values);

    private final String name;

    FormCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
