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
class CoreContentPlugin(pluginInformation: PluginInformation, gameInstance: GameInstance) : Plugin(pluginInformation, gameInstance) {
	private lateinit var itemsLogic: ItemsLogicListener
	private lateinit var entityLogic: EntityLogicListener

	init {
		gameInstance.logger.info("Initializing core content plugin")

		this.gameInstance.pluginManager.registerCommand("food", FoodCommand())

		val engine = gameInstance.engine
		if (engine is Client) {
			gameInstance.logger.info("Registering additional configuration options for the client")
			engine.configuration.addOptions(CoreOptions.options)
		}

		onEnable()
	}

	fun onEnable() {
		/*itemsLogic = ItemsLogicListener(this)
		gameInstance.pluginManager.registerEventListener(itemsLogic, this)

		entityLogic = EntityLogicListener(this)
		gameInstance.pluginManager.registerEventListener(entityLogic, this)*/
	}

	override fun onDisable() { }
}
