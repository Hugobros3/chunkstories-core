//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item.armor;

import io.xol.chunkstories.api.item.ItemDeclaration;

import java.util.Arrays;
import java.util.Collection;

public class ItemArmorLegs extends ItemArmor {
    public static final Collection<String> bodyParts = Arrays
            .asList("boneLegRU", "boneLegRD", "boneLegLU", "boneLegLD");

    public ItemArmorLegs(ItemDeclaration type) {
        super(type);
    }

    @Override
    public Collection<String> bodyPartsAffected() {
        return bodyParts;
    }

}
