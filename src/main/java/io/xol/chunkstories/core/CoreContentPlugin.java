package io.xol.chunkstories.core;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.plugin.PluginInformation;
import io.xol.chunkstories.core.logic.ItemsLogic;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** 'Glue' for hooking core functions into the base engine */
public class CoreContentPlugin extends ChunkStoriesPlugin {

	//TODO add block decals rendering here
	private ItemsLogic itemsLogic = new ItemsLogic(this);
	
	public CoreContentPlugin(PluginInformation pluginInformation, GameContext pluginExecutionContext) {
		super(pluginInformation, pluginExecutionContext);
		
	}

	@Override
	public void onEnable() {
		pluginExecutionContext.getPluginManager().registerEventListener(itemsLogic, this);
	}

	@Override
	public void onDisable() {
		
		//pluginExecutionContext.getPluginManager().unRegisterEventListener(itemsLogic, this);
	}

}
