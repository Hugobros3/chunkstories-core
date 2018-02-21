package io.xol.chunkstories.core.item.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDefinition;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.item.ItemRenderer;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class FlatIconItemRenderer extends DefaultItemRenderer
{
	
	public FlatIconItemRenderer(Item item, ItemRenderer fallbackRenderer, ItemDefinition itemType)
	{
		super(item);
	}
	
	@Override
	public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world, Location location, Matrix4f handTransformation)
	{
		handTransformation.translate(new Vector3f(-0.05f, -0.05f, 0.05f));
		handTransformation.rotate((float) -(Math.PI / 4f), new Vector3f(0.0f, 0.0f, 1.0f));
		super.renderItemInWorld(renderingInterface, pile, world, location, handTransformation);
	}
}
