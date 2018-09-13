//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item.armor;

import java.util.Arrays;
import java.util.Collection;

import io.xol.chunkstories.api.item.ItemDefinition;

public class ItemArmorBoots extends ItemArmor {
    public static final Collection<String> bodyParts = Arrays.asList(new String[] { "boneFootR", "boneFootL" });

    public ItemArmorBoots(ItemDefinition type) {
        super(type);
    }

    @Override
    public Collection<String> bodyPartsAffected() {
        return bodyParts;
    }

}
