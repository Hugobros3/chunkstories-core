//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.logic;

import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.rendering.WorldRenderingDecalsEvent;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.core.CoreContentPlugin;
import io.xol.chunkstories.core.item.ItemMiningTool;
import io.xol.chunkstories.core.item.ItemMiningTool.MiningProgress;
import io.xol.chunkstories.core.item.renderer.decals.BreakingBlockDecal;

public class RenderingEventsListener implements Listener {
	@SuppressWarnings("unused")
	private final CoreContentPlugin core;
	
	public RenderingEventsListener(CoreContentPlugin core) {
		this.core = core;
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
