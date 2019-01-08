//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item.armor;

import java.util.Arrays;
import java.util.Collection;

import xyz.chunkstories.api.item.ItemDefinition;

public class ItemArmorChest extends ItemArmor {
    public static final Collection<String> bodyParts = Arrays
            .asList(new String[] { "boneArmRU", "boneArmLU", "boneArmRD", "boneArmLD", "boneTorso" });

    public ItemArmorChest(ItemDefinition type) {
        super(type);
    }

    @Override
    public Collection<String> bodyPartsAffected() {
        return bodyParts;
    }

}
