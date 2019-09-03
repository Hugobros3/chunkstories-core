//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core

import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.plugin.ChunkStoriesPlugin
import xyz.chunkstories.api.plugin.PluginInformation
import xyz.chunkstories.core.cli.FoodCommand
import xyz.chunkstories.core.logic.EntityLogicListener
import xyz.chunkstories.core.logic.ItemsLogicListener

/** 'Glue' for hooking core functions into the base engine  */
class CoreContentPlugin(pluginInformation: PluginInformation, pluginExecutionContext: GameContext) : ChunkStoriesPlugin(pluginInformation, pluginExecutionContext) {

	private lateinit var itemsLogic: ItemsLogicListener
	private lateinit var entityLogic: EntityLogicListener

	init {
		this.pluginManager.registerCommand("food", FoodCommand())

		pluginExecutionContext.logger().info("Initializing core content plugin")
		if (pluginExecutionContext is Client) {
			pluginExecutionContext.logger().info("Installing additional options")
			(pluginExecutionContext as Client).configuration.addOptions(CoreOptions.options)
		}
	}

	override fun onEnable() {
		itemsLogic = ItemsLogicListener(this)
		gameContext.pluginManager.registerEventListener(itemsLogic, this)

		entityLogic = EntityLogicListener(this)
		gameContext.pluginManager.registerEventListener(entityLogic, this)

		/*if (this.getPluginExecutionContext() instanceof Client) {
			renderingLogic = new RenderingEventsListener(this, (Client) getPluginExecutionContext());
			pluginExecutionContext.getPluginManager().registerEventListener(renderingLogic, this);
		}*/
	}

	override fun onDisable() {
		// pluginExecutionContext.getPluginManager().unRegisterEventListener(itemsLogic,
		// this);
	}

}
