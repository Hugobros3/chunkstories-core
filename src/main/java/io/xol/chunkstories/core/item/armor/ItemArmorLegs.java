package io.xol.chunkstories.core.item.armor;

import io.xol.chunkstories.api.item.ItemDefinition;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

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
