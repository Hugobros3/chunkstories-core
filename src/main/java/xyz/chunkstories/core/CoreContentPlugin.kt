//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.plugin.Plugin
import xyz.chunkstories.api.plugin.PluginInformation
import xyz.chunkstories.api.world.GameInstance
import xyz.chunkstories.core.cli.FoodCommand
import xyz.chunkstories.core.logic.EntityLogicListener
import xyz.chunkstories.core.logic.ItemsLogicListener

/** 'Glue' for hooking core functions into the base engine  */
class CoreContentPlugin(pluginInformation: PluginInformation, pluginExecutionContext: GameInstance) : Plugin(pluginInformation, pluginExecutionContext) {
	private lateinit var itemsLogic: ItemsLogicListener
	private lateinit var entityLogic: EntityLogicListener

	init {
		pluginExecutionContext.logger.info("Initializing core content plugin")

		gameInstance.pluginManager.registerCommand("food", FoodCommand())

		if (pluginExecutionContext is Client) {
			pluginExecutionContext.logger.info("Registering additional configuration options for the client")
			pluginExecutionContext.configuration.addOptions(CoreOptions.options)
		}
	}

	override fun onEnable() {
		itemsLogic = ItemsLogicListener(this)
		gameInstance.pluginManager.registerEventListener(itemsLogic, this)

		entityLogic = EntityLogicListener(this)
		gameInstance.pluginManager.registerEventListener(entityLogic, this)
	}

	override fun onDisable() { }
}
