//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item.renderer;

import org.joml.Matrix4f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientContent.TexturesLibrary;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.item.ItemRenderer;
import io.xol.chunkstories.api.rendering.mesh.ClientMeshLibrary;
import io.xol.chunkstories.api.world.World;

public class ObjViewModelRenderer extends ItemRenderer
{
	final String objName;
	final String albedoTextureName;
	final String normalTextureName;
	final String materialTextureName;
	
	private final TexturesLibrary textures;
	private final ClientMeshLibrary models;

	public ObjViewModelRenderer(Item item, ItemRenderer fallbackRenderer, String objName, String albedoTextureName)
	{
		this(item, fallbackRenderer, objName, albedoTextureName, "./textures/normalnormal.png");
	}
	
	public ObjViewModelRenderer(Item item, ItemRenderer fallbackRenderer, String objName, String albedoTextureName, String normalTextureName)
	{
		this(item, fallbackRenderer, objName, albedoTextureName, normalTextureName, "./textures/defaultmaterial.png");
	}
	
	public ObjViewModelRenderer(Item item, ItemRenderer fallbackRenderer, String objName, String albedoTextureName, String normalTextureName, String materialTextureName)
	{
		super(fallbackRenderer);
		
		this.objName = objName;
		this.albedoTextureName = albedoTextureName;
		this.normalTextureName = normalTextureName;
		this.materialTextureName = materialTextureName;
		
		this.textures = ((ClientContent)item.getDefinition().store().parent()).textures();
		this.models = ((ClientContent)item.getDefinition().store().parent()).meshes();
	}

	@Override
	public void renderItemInInventory(RenderingInterface context, ItemPile pile, float screenPositionX, float screenPositionY, int scaling) {
		super.renderItemInInventory(context, pile, screenPositionX, screenPositionY, scaling);
	}

	@Override
	public void renderItemInWorld(RenderingInterface renderingContext, ItemPile pile, World world, Location location, Matrix4f handTransformation) {
		/*Matrix4f c = new Matrix4f();
		c.m11(0);
		c.m22(0);
		c.m12(1);
		c.m21(-1);
		handTransformation.mul(c);*/
		renderingContext.setObjectMatrix(handTransformation);

		renderingContext.bindAlbedoTexture(textures.getTexture(albedoTextureName));
		renderingContext.bindNormalTexture(textures.getTexture(normalTextureName));
		renderingContext.bindMaterialTexture(textures.getTexture(materialTextureName));
		models.getRenderableMesh(objName).render(renderingContext);
	}

}
