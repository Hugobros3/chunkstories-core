//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item;

import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDefinition;

public class ItemFirearmMagazine extends Item
{
	Set<String> supportedWeaponsSet = new HashSet<String>();
	
	public ItemFirearmMagazine(ItemDefinition type)
	{
		super(type);
		
		for(String s : type.resolveProperty("forWeapon", "").split(","))
			supportedWeaponsSet.add(s);
	}
	
	public boolean isSuitableFor(ItemFirearm item)
	{
		return supportedWeaponsSet.contains(item.getInternalName());
	}

}
