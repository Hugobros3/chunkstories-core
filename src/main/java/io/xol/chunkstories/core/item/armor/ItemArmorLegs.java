//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item.armor;

import io.xol.chunkstories.api.item.ItemDefinition;

public class ItemArmorLegs extends ItemArmor
{
	public static final String[] bodyParts = {"boneLegRU","boneLegRD","boneLegLU","boneLegLD"};

	public ItemArmorLegs(ItemDefinition type)
	{
		super(type);
	}

	@Override
	public String[] bodyPartsAffected()
	{
		return bodyParts;
	}

}
