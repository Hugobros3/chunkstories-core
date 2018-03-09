//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.rendering;

import java.util.Map;

import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.rendering.RenderingPipelineInitEvent;
import io.xol.chunkstories.api.events.rendering.WorldRenderingDecalsEvent;
import io.xol.chunkstories.api.rendering.RenderPass;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.RenderingPipeline;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.core.CoreContentPlugin;
import io.xol.chunkstories.core.item.ItemMiningTool;
import io.xol.chunkstories.core.item.ItemMiningTool.MiningProgress;
import io.xol.chunkstories.core.item.renderer.decals.BreakingBlockDecal;
import io.xol.chunkstories.core.rendering.passes.ApplySunlightPass;
import io.xol.chunkstories.core.rendering.passes.FarTerrainPass;
import io.xol.chunkstories.core.rendering.passes.GBuffersOpaquePass;
import io.xol.chunkstories.core.rendering.passes.PostProcessPass;
import io.xol.chunkstories.core.rendering.passes.ShadowPass;
import io.xol.chunkstories.core.rendering.passes.SkyPass;
import io.xol.chunkstories.core.rendering.sky.DefaultSkyRenderer;

public class RenderingEventsListener implements Listener {
	
	@SuppressWarnings("unused")
	private final CoreContentPlugin core;
	
	public RenderingEventsListener(CoreContentPlugin core) {
		this.core = core;
	}

	class FakeRenderPass extends RenderPass {

		public FakeRenderPass(RenderingPipeline pipeline, String name, String[] requires, String[] exports) {
			super(pipeline, name, requires, exports);
		}

		@Override
		public void resolvedInputs(Map<String, Texture> inputs) {
			
		}

		@Override
		public void render(RenderingInterface renderer) {
			
		}

		@Override
		public void onScreenResize(int w, int h) {
			
		}
		
	}
	
	@EventHandler
	public void onRenderingPipelineInitialization(RenderingPipelineInitEvent event) {
		RenderingPipeline pipeline = event.getPipeline();
		WorldRenderer worldRenderer = pipeline.getWorldRenderer();

		SkyRenderer sky = new DefaultSkyRenderer(event.getPipeline().getRenderingInterface().getWorldRenderer());
		worldRenderer.setSkyRenderer(sky);
				
		SkyPass skyPass = new SkyPass(pipeline, "sky", worldRenderer.getSkyRenderer());
		pipeline.registerRenderPass(skyPass);

		// gbuffer uses the zBuffer and outputs albedo/normal/materials
		GBuffersOpaquePass gBuffers = new GBuffersOpaquePass(pipeline, "gBuffers", 
				new String[]{"sky.zBuffer!"}, 
				new String[]{"albedo", "normals", "voxelLight", "specularity", "material", "zBuffer" } );
		
		pipeline.registerRenderPass(gBuffers);

		// a shadowmap pass requires no previous buffer and just outputs a shadowmap	
		ShadowPass sunShadowPass = new ShadowPass(pipeline, "shadowsSun",  new String[]{}, new String[]{"shadowMap"}, sky);
		// note we could generalize the shadowmappass to not only the sun but also the moon, point and spotlights
		pipeline.registerRenderPass(sunShadowPass);

		// aka shadows_apply in the current code, it takes the gbuffers and applies the shadowmapping to them, then outputs to the shaded pixels buffers already filled with the far terrain pixels
		ApplySunlightPass applySunlight = new ApplySunlightPass(pipeline, "applySunlight", 
				new String[]{"gBuffers.albedo", "gBuffers.normals", "gBuffers.voxelLight", "gBuffers.specularity", "gBuffers.material", 
						"gBuffers.zBuffer", "shadowsSun.shadowMap", "sky.shadedBuffer!"}, 
				new String[]{"shadedBuffer"},
				sunShadowPass);
		pipeline.registerRenderPass(applySunlight);	

		// far terrain needs the shaded buffer from sky and outputs it, as well with a zbuffer
		FarTerrainPass farTerrain = new FarTerrainPass(pipeline, "farTerrain", 
				new String[]{"applySunlight.shadedBuffer!", "gBuffers.specularity!", "gBuffers.zBuffer!"}, 
				new String[]{"shadedBuffer", "zBuffer"} );
		pipeline.registerRenderPass(farTerrain);
		
		 // the pass declared as 'final' is considered the last one and it's outputs are shown to the screen
		pipeline.registerRenderPass(new PostProcessPass(pipeline, "final", 
				new String[] {"farTerrain.shadedBuffer", "farTerrain.zBuffer"},
				sunShadowPass) );
	}

	
	@EventHandler
	public void renderCoreDecals(WorldRenderingDecalsEvent event) {
		drawCrackedBlocks(event.getWorldRenderer().getRenderingInterface());
	}
	
	BreakingBlockDecal dekal = null;
	
	private void drawCrackedBlocks(RenderingInterface renderingInterface) {
		MiningProgress progress = ItemMiningTool.myProgress;
		if(progress == null || (dekal != null && !dekal.miningProgress.equals(progress))) {
			if(dekal != null) {
				dekal.destroy();
				dekal = null;
			}
		}
		
		if(progress != null) {
			if(dekal == null) {
				dekal = new BreakingBlockDecal(progress, renderingInterface);
			}
			dekal.render(renderingInterface);
		}
	}

}
