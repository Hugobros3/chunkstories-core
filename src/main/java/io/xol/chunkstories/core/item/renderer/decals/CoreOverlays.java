package io.xol.chunkstories.core.item.renderer.decals;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.item.ItemMiningTool;
import io.xol.chunkstories.core.item.ItemMiningTool.MiningProgress;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Simple class that is responsible of drawing the core overlays */
public class CoreOverlays
{
	World world;

	public CoreOverlays(World w)
	{
		world = w;
	}
	
	BreakingBlockDecal dekal = null;
	
	public void drawnCrackedBlocks(RenderingInterface renderingInterface) {
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
