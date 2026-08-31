package com.studio.planeshift.server;

import java.util.Set;
import net.neoforged.fml.ModList;

/**
 * Compatibility patch layer. Checks for known conflicting mods and disables
 * PlaneShift features that would fight with them (e.g. mob replacement).
 */
public final class ModCompatibility {

    private static final Set<String> HOSTILE_MOB_MODS = Set.of(
            "lycanitesmobs",
            "mutantmonsters",
            "mowziesmobs",
            "alexsmobs"
    );

    private ModCompatibility() {
    }

    public static boolean disableMobReplacement() {
        ModList modList = ModList.get();
        if (modList == null) {
            return false;
        }
        for (String id : HOSTILE_MOB_MODS) {
            if (modList.isLoaded(id)) {
                return true;
            }
        }
        return false;
    }
}
