//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDeclaration;

import java.util.HashSet;
import java.util.Set;

public class ItemFirearmMagazine extends Item {
    Set<String> supportedWeaponsSet = new HashSet<String>();

    public ItemFirearmMagazine(ItemDeclaration type) {
        super(type);

        for (String s : type.getExt().getOrDefault("forWeapon", "").split(","))
            supportedWeaponsSet.add(s);
    }

    public boolean isSuitableFor(ItemFirearm item) {
        return supportedWeaponsSet.contains(item.getInternalName());
    }

}
