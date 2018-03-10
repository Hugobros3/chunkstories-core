//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.rendering;

import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.rendering.RenderPassesInitEvent;
import io.xol.chunkstories.api.events.rendering.WorldRenderingDecalsEvent;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.core.CoreContentPlugin;
import io.xol.chunkstories.core.item.ItemMiningTool;
import io.xol.chunkstories.core.item.ItemMiningTool.MiningProgress;
import io.xol.chunkstories.core.item.renderer.decals.BreakingBlockDecal;
import io.xol.chunkstories.core.rendering.passes.ApplySunlightPass;
import io.xol.chunkstories.core.rendering.passes.BloomPass;
import io.xol.chunkstories.core.rendering.passes.DecalsPass;
import io.xol.chunkstories.core.rendering.passes.DefferedLightsPass;
import io.xol.chunkstories.core.rendering.passes.FarTerrainPass;
import io.xol.chunkstories.core.rendering.passes.ForwardPass;
import io.xol.chunkstories.core.rendering.passes.GBuffersOpaquePass;
import io.xol.chunkstories.core.rendering.passes.PostProcessPass;
import io.xol.chunkstories.core.rendering.passes.ReflectionsPass;
import io.xol.chunkstories.core.rendering.passes.ShadowPass;
import io.xol.chunkstories.core.rendering.passes.SkyPass;
import io.xol.chunkstories.core.rendering.passes.WaterPass;
import io.xol.chunkstories.core.rendering.passes.gi.GiPass;
import io.xol.chunkstories.core.rendering.sky.DefaultSkyRenderer;

public class RenderingEventsListener implements Listener {
	
	@SuppressWarnings("unused")
	private final CoreContentPlugin core;
	
	public RenderingEventsListener(CoreContentPlugin core) {
		this.core = core;
	}
	
	@EventHandler
	public void onRenderPassesInitialization(RenderPassesInitEvent event) {
		RenderPasses pipeline = event.getPasses();
		WorldRenderer worldRenderer = pipeline.getWorldRenderer();

		SkyRenderer sky = new DefaultSkyRenderer(event.getPasses().getRenderingInterface().getWorldRenderer());
		worldRenderer.setSkyRenderer(sky);
				
		SkyPass skyPass = new SkyPass(pipeline, "sky", worldRenderer.getSkyRenderer());
		pipeline.registerRenderPass(skyPass);

		// gbuffer uses the zBuffer and outputs albedo/normal/materials
		GBuffersOpaquePass gBuffers = new GBuffersOpaquePass(pipeline, "gBuffers", 
				new String[]{"sky.zBuffer!"}, 
				new String[]{"albedoBuffer", "normalBuffer", "voxelLightBuffer", "specularityBuffer", "materialsBuffer", "zBuffer" } );
		pipeline.registerRenderPass(gBuffers);
		
		DecalsPass decals = new DecalsPass(pipeline, "decals", new String[] {"gBuffers.albedoBuffer!", "gBuffers.zBuffer"}, new String[] {});
		pipeline.registerRenderPass(decals);
		
		WaterPass waterPass = new WaterPass(pipeline, "water", 
				new String[]{"decals.albedoBuffer", "gBuffers.normalBuffer", "gBuffers.voxelLightBuffer", "gBuffers.specularityBuffer", "gBuffers.materialsBuffer", 
				"gBuffers.zBuffer"}, 
				new String[]{"albedoBuffer", "normalBuffer", "voxelLightBuffer", "specularityBuffer", "materialsBuffer", "zBuffer" }
			, sky);
		pipeline.registerRenderPass(waterPass);

		// a shadowmap pass requires no previous buffer and just outputs a shadowmap	
		ShadowPass sunShadowPass = new ShadowPass(pipeline, "shadowsSun",  new String[]{}, new String[]{"shadowMap"}, sky);
		// note we could generalize the shadowmappass to not only the sun but also the moon, point and spotlights
		pipeline.registerRenderPass(sunShadowPass);

		GiPass giPass = new GiPass(pipeline, "gi", new String[] {"water.albedoBuffer", "water.normalBuffer", "water.zBuffer"}, new String[] {"giBuffer"});
		pipeline.registerRenderPass(giPass);
		
		// aka shadows_apply in the current code, it takes the gbuffers and applies the shadowmapping to them, then outputs to the shaded pixels buffers already filled with the far terrain pixels
		ApplySunlightPass applySunlight = new ApplySunlightPass(pipeline, "applySunlight", 
				new String[]{"water.albedoBuffer", "water.normalBuffer", "water.voxelLightBuffer", "water.specularityBuffer", "water.materialsBuffer", 
						"water.zBuffer", "shadowsSun.shadowMap", "sky.shadedBuffer!"}, 
				new String[]{"shadedBuffer"},
				sunShadowPass);
		pipeline.registerRenderPass(applySunlight);	

		DefferedLightsPass lightsPass = new DefferedLightsPass(pipeline, "lights", 
				new String[]{"applySunlight.shadedBuffer!", "water.albedoBuffer", "water.normalBuffer", "water.zBuffer"}, 
				new String[]{});
		pipeline.registerRenderPass(lightsPass);	
		
		// far terrain needs the shaded buffer from sky and outputs it, as well with a zbuffer
		FarTerrainPass farTerrain = new FarTerrainPass(pipeline, "farTerrain", 
				new String[]{"lights.shadedBuffer!", "gBuffers.specularityBuffer!", "gBuffers.zBuffer!"}, 
				new String[]{"shadedBuffer", "zBuffer", "specularityBuffer"} );
		pipeline.registerRenderPass(farTerrain);
		
		BloomPass bloomPass = new BloomPass(pipeline, "bloom", new String[] {"farTerrain.shadedBuffer"}, new String[] {"bloomBuffer"});
		pipeline.registerRenderPass(bloomPass);
		
		ForwardPass forward = new ForwardPass(pipeline, "forward", 
				new String[]{"farTerrain.shadedBuffer!"}, 
				new String[]{});
		pipeline.registerRenderPass(forward);
		
		ReflectionsPass reflections = new ReflectionsPass(pipeline, "reflections", 
				new String[]{"farTerrain.shadedBuffer", "gBuffers.normalBuffer", "gBuffers.voxelLightBuffer", "farTerrain.specularityBuffer",
							"farTerrain.zBuffer"}, new String[] {"reflectionsBuffer"}, sky);
		pipeline.registerRenderPass(reflections);
		
		 // the pass declared as 'final' is considered the last one and it's outputs are shown to the screen
		pipeline.registerRenderPass(new PostProcessPass(pipeline, "final", 
				new String[] {"gi.giBuffer", "forward.shadedBuffer", "farTerrain.zBuffer", "bloom.bloomBuffer", "reflections.reflectionsBuffer", "farTerrain.specularityBuffer"},
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
