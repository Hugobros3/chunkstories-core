package io.xol.chunkstories.core.voxel.renderers;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.voxel.VoxelDynamicRenderer;
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkCell;
import io.xol.chunkstories.core.voxel.components.VoxelComponentSignText;

public class SignRenderer implements VoxelDynamicRenderer {

	VoxelRenderer ogVoxelRenderer;
	
	public SignRenderer(VoxelRenderer voxelRenderer) {
		this.ogVoxelRenderer = voxelRenderer;
	}

	private void setupRender(RenderingInterface renderingContext)
	{
		renderingContext.setObjectMatrix(null);

		Texture2D diffuse = renderingContext.textures().getTexture("./models/sign.png");
		diffuse.setLinearFiltering(false);
		renderingContext.bindAlbedoTexture(diffuse);
		renderingContext.bindNormalTexture(renderingContext.textures().getTexture("./textures/normalnormal.png"));
		renderingContext.bindMaterialTexture(renderingContext.textures().getTexture("./textures/defaultmaterial.png"));
	}
	
	@Override
	public void renderVoxels(RenderingInterface renderer, IterableIterator<ChunkCell> renderableEntitiesIterator)
	{
		renderer.useShader("entities");
		setupRender(renderer);
		
		renderer.setObjectMatrix(null);

		for (ChunkCell context : renderableEntitiesIterator)//.getElementsInFrustrumOnly())
		{
			if (renderer.getCamera().getCameraPosition().distance(context.getLocation()) > 32)
				continue;
			
			Texture2D diffuse = renderer.textures().getTexture("./models/sign.png");
			diffuse.setLinearFiltering(false);
			renderer.bindAlbedoTexture(diffuse);
			renderer.bindNormalTexture(renderer.textures().getTexture("./textures/normalnormal.png"));
			
			renderer.currentShader().setUniform2f("worldLightIn", context.getBlocklight(), context.getSunlight() );
			
			boolean isPost = context.getVoxel().getName().endsWith("_post");
			int facing = context.getMetaData();
			
			Matrix4f mutrix = new Matrix4f();
			mutrix.translate(new Vector3f(0.5f, 0.0f, 0.5f));
			
			Location loc = context.getLocation();
			mutrix.translate((float)loc.x, (float)loc.y, (float)loc.z);
			mutrix.rotate((float) Math.PI * 2.0f * (-facing) / 16f, new Vector3f(0, 1, 0));
			if (!isPost)
				mutrix.translate(new Vector3f(0.0f, 0.0f, -0.5f));
			renderer.setObjectMatrix(mutrix);

			//System.out.println("bonsoir");
			
			if (!isPost)
				renderer.meshes().getRenderableMeshByName("./models/sign_post.obj").render(renderer);
			else
				renderer.meshes().getRenderableMeshByName("./models/sign.obj").render(renderer);

			VoxelComponentSignText signTextComponent = (VoxelComponentSignText) context.components().get("signData");
			
			if(signTextComponent == null)
				continue;
			
			// bake sign mesh
			if (signTextComponent.cachedText == null || !signTextComponent.cachedText.equals(signTextComponent.getSignText()))
			{
				//entitySign.renderData = new TextMeshObject(entitySign.signText.getSignText());
				signTextComponent.cachedText = signTextComponent.getSignText();
				signTextComponent.renderData = renderer.getFontRenderer().newTextMeshObject(renderer.getFontRenderer().defaultFont(), signTextComponent.cachedText);
			}
			
			//signTextComponent.setSignText("fuck");
			//System.out.println("cachedText:"+signTextComponent.getSignText());
			
			// Display it
			mutrix.translate(new Vector3f(0.0f, 1.15f, 0.055f));
			renderer.setObjectMatrix(mutrix);
			signTextComponent.renderData.render(renderer);
		}
	}

}
