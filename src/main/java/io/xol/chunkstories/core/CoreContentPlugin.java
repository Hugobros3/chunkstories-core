//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.plugin.PluginInformation;
import io.xol.chunkstories.core.logic.ItemsLogicListener;
import io.xol.chunkstories.core.logic.RenderingEventsListener;

/** 'Glue' for hooking core functions into the base engine */
public class CoreContentPlugin extends ChunkStoriesPlugin {

	private ItemsLogicListener itemsLogic = new ItemsLogicListener(this);
	private RenderingEventsListener renderingLogic = new RenderingEventsListener(this);
	
	public CoreContentPlugin(PluginInformation pluginInformation, GameContext pluginExecutionContext) {
		super(pluginInformation, pluginExecutionContext);
		
	}

	@Override
	public void onEnable() {
		pluginExecutionContext.getPluginManager().registerEventListener(itemsLogic, this);
		pluginExecutionContext.getPluginManager().registerEventListener(renderingLogic, this);
	}

	@Override
	public void onDisable() {
		
		//pluginExecutionContext.getPluginManager().unRegisterEventListener(itemsLogic, this);
	}

}
