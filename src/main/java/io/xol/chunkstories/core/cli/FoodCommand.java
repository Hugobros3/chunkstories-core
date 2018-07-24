//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.cli;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;
import io.xol.chunkstories.core.entity.components.EntityFoodLevel;

/** Heals */
public class FoodCommand implements CommandHandler {


	
	// Lazy, why does Java standard lib doesn't have a clean way to do this tho
	// http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
	public static boolean isNumeric(String str)
	{
	    for (char c : str.toCharArray())
	    {
	        if (!Character.isDigit(c)) return false;
	    }
	    return true;
	}
	
	@Override
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments) {

		if (!(emitter instanceof Player)) {
			emitter.sendMessage("You need to be a player to use this command.");
			return true;
		}

		Player player = (Player) emitter;
		
		if(!emitter.hasPermission("self.setfood"))
		{
			emitter.sendMessage("You don't have the permission.");
			return true;
		}
		
		if(arguments.length < 1 || !isNumeric(arguments[0]))
		{
			emitter.sendMessage("Syntax: /food <hp>");
			return true;
		}
		
		float food = Float.parseFloat(arguments[0]);
		
		Entity entity = player.getControlledEntity();
		if(!entity.traits.tryWithBoolean(EntityFoodLevel.class, fl -> {
			fl.setValue(food);
			player.sendMessage("Food set to: " + food);
			
			return true;
		}))
			emitter.sendMessage("This action doesn't apply to your current entity.");
		
		return true;
	}

}
