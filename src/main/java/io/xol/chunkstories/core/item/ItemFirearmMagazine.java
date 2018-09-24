//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDefinition;

public class ItemFirearmMagazine extends Item {
    private Set<String> supportedWeaponsSet = new HashSet<>();

    public ItemFirearmMagazine(ItemDefinition type) {
        super(type);

        supportedWeaponsSet.addAll(Arrays.asList(type.resolveProperty("forWeapon", "").split(",")));
    }

    public boolean isSuitableFor(ItemFirearm item) {
        return supportedWeaponsSet.contains(item.getInternalName());
    }

}
