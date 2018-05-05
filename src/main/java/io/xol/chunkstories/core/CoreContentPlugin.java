//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.plugin.PluginInformation;
import io.xol.chunkstories.core.cli.FoodCommand;
import io.xol.chunkstories.core.logic.EntityLogicListener;
import io.xol.chunkstories.core.logic.ItemsLogicListener;
import io.xol.chunkstories.core.rendering.RenderingEventsListener;

/** 'Glue' for hooking core functions into the base engine */
public class CoreContentPlugin extends ChunkStoriesPlugin {

	private ItemsLogicListener itemsLogic;
	private EntityLogicListener entityLogic;
	
	private RenderingEventsListener renderingLogic;
	
	public CoreContentPlugin(PluginInformation pluginInformation, GameContext pluginExecutionContext) {
		super(pluginInformation, pluginExecutionContext);
		
		this.getPluginManager().registerCommandHandler("food", new FoodCommand());
	}

	@Override
	public void onEnable() {
		itemsLogic = new ItemsLogicListener(this);
		pluginExecutionContext.getPluginManager().registerEventListener(itemsLogic, this);
		
		entityLogic = new EntityLogicListener(this);
		pluginExecutionContext.getPluginManager().registerEventListener(entityLogic, this);
		
		if(this.getPluginExecutionContext() instanceof ClientInterface) {
			renderingLogic = new RenderingEventsListener(this, (ClientInterface) getPluginExecutionContext());
			pluginExecutionContext.getPluginManager().registerEventListener(renderingLogic, this);
		}
	}

	@Override
	public void onDisable() {
		//pluginExecutionContext.getPluginManager().unRegisterEventListener(itemsLogic, this);
	}

}
