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

	@Override
	public void renderVoxels(RenderingInterface renderer, IterableIterator<ChunkCell> renderableEntitiesIterator)
	{
		renderer.useShader("entities");
		
		renderer.setObjectMatrix(null);

		for (ChunkCell context : renderableEntitiesIterator)//.getElementsInFrustrumOnly())
		{
			if (renderer.getCamera().getCameraPosition().distance(context.getLocation()) > 32)
				continue;
			
			Texture2D diffuse = renderer.textures().getTexture("./voxels/blockmodels/sign.png");
			diffuse.setLinearFiltering(false);
			renderer.bindAlbedoTexture(diffuse);
			renderer.bindNormalTexture(renderer.textures().getTexture("./textures/normalnormal.png"));
			
			renderer.currentShader().setUniform2f("worldLightIn", context.getBlocklight(), context.getSunlight() );
			
			boolean isPost = context.getVoxel().getName().endsWith("_post");
			int facing = context.getMetaData();
			
			Matrix4f mutrix = new Matrix4f();
			
			Location loc = context.getLocation();
			mutrix.translate((float)loc.x, (float)loc.y, (float)loc.z);
			
			mutrix.translate(new Vector3f(0.5f, 0.0f, 0.5f));
			mutrix.rotate((float) Math.PI * -0.5f, new Vector3f(0, 1, 0));
			mutrix.rotate((float) Math.PI * 2.0f * (-facing) / 16f, new Vector3f(0, 1, 0));
			if (!isPost)
				mutrix.translate(new Vector3f(-0.5f, 0.0f, 0.0f));
			
			renderer.setObjectMatrix(mutrix);
			
			if (isPost)
				renderer.meshes().getRenderableMesh("./voxels/blockmodels/sign_post.dae").render(renderer);
			else
				renderer.meshes().getRenderableMesh("./voxels/blockmodels/sign.dae").render(renderer);

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
