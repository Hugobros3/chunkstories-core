//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item.armor;

import io.xol.chunkstories.api.item.ItemDefinition;

public class ItemArmorHead extends ItemArmor
{
	public static final String[] bodyParts = {"boneHead"};

	public ItemArmorHead(ItemDefinition type)
	{
		super(type);
	}

	@Override
	public String[] bodyPartsAffected()
	{
		return bodyParts;
	}

}
