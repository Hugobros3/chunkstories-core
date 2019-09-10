//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.cli

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.plugin.commands.CommandHandler
import xyz.chunkstories.core.entity.traits.TraitFoodLevel

/** Heals  */
class FoodCommand : CommandHandler {

	override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {

		if (emitter !is Player) {
			emitter.sendMessage("You need to be a player to use this command.")
			return true
		}

		if (!emitter.hasPermission("self.setfood")) {
			emitter.sendMessage("You don't have the permission.")
			return true
		}

		if (arguments.size < 1 || !isNumeric(arguments[0])) {
			emitter.sendMessage("Syntax: /food <hp>")
			return true
		}

		val food = java.lang.Float.parseFloat(arguments[0])

		val entity = emitter.controlledEntity ?: throw Exception("You aren't currently controlling an entity !")
		val foodTrait = entity.traits[TraitFoodLevel::class]

		if (foodTrait != null) {
			foodTrait.foodLevel = food
			emitter.sendMessage("Food set to: $food")
		} else {
			emitter.sendMessage("This action doesn't apply to your current entity.")
		}

		return true
	}

	companion object {

		// Lazy, why does Java standard lib doesn't have a clean way to do this tho
		// http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
		fun isNumeric(str: String): Boolean {
			for (c in str.toCharArray()) {
				if (!Character.isDigit(c))
					return false
			}
			return true
		}
	}

}
