//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import org.joml.Vector4f
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.TraitHasOverlay
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.item.interfaces.ItemOverlay
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.core.entity.traits.TraitFoodLevel

class TraitHealthFoodOverlay(entity: Entity) : TraitHasOverlay(entity) {

	private fun clamp(f: Float, min: Float, max: Float) = if (f < min) min else if (f > max) max else f

	override fun drawEntityOverlay(renderer: GuiDrawer) {
		val health = entity.traits[TraitHealth::class]?.getHealth() ?: 0f
		val maxHealth = entity.traits[TraitHealth::class]?.maxHealth ?: 0f
		val foodLevel = entity.traits[TraitFoodLevel::class]?.getValue() ?: 0f
		val maxFood = 100.0f

		if ((entity.world as? WorldClient)?.client?.player?.controlledEntity == entity) {

			/*renderer.drawBox(
					renderer.gui.viewportWidth / 2 - 128, 48, 256, 32,
					0f, 32f / 256f, 1f, 0f,
					"./textures/gui/hud/hud_survival.png", null)*/

			var hearths = ((health / maxHealth) * 20).toInt()
			var hpos = (renderer.gui.viewportWidth / 2) - 10 * 10 - 13 - 0

			for (i in 0 until 10) {
				if (hearths >= 2) {
					renderer.drawBox(hpos, 48, 16, 16, "textures/gui/hud/hearth10.png")
					hearths -= 2
				} else if (hearths >= 1) {
					renderer.drawBox(hpos, 48, 16, 16, "textures/gui/hud/hearth_half10.png")
					hearths -= 1
				} else {
					renderer.drawBox(hpos, 48, 16, 16, "textures/gui/hud/hearth_empty10.png")
				}
				hpos += 10
			}

			hpos = (renderer.gui.viewportWidth / 2) + 8 + 0

			var foods = ((foodLevel / maxFood) * 20).toInt()
			for (i in 0 until 10) {
				if (foods >= 2) {
					renderer.drawBox(hpos, 48, 16, 16, "textures/gui/hud/food10.png")
					foods -= 2
				} else if (foods >= 1) {
					renderer.drawBox(hpos, 48, 16, 16, "textures/gui/hud/food_half10.png")
					foods -= 1
				} else {
					renderer.drawBox(hpos, 48, 16, 16, "textures/gui/hud/food_empty10.png")
				}
				hpos += 10
			}

			val inventory = entity.traits[TraitInventory::class]?.inventory
			if (inventory != null) {
				val selectedItem = entity.traits[TraitSelectedItem::class]?.getSelectedSlot()
				var offset = renderer.gui.viewportWidth / 2 - 22 * inventory.width / 2
				for (x in 0 until inventory.width) {

					if (selectedItem == x)
						renderer.drawBox(offset, 24, 22, 22, "textures/gui/inventory/slot.png", Vector4f(2f, 2f, 2f, 0.5f))
					else
						renderer.drawBox(offset, 24, 22, 22, "textures/gui/inventory/slot.png", Vector4f(1f, 1f, 1f, 0.5f))

					offset += 22
				}

				offset = renderer.gui.viewportWidth / 2 - 22 * inventory.width / 2
				for (x in 0 until inventory.width) {
					val pile = inventory.getItemPileAt(x, 0)
					if (pile != null) {
						renderer.drawBox(offset + 3, 24 + 3, 16, 16, pile.item.getTextureName(), Vector4f(1f, 1f, 1f, 1f))
					}
					offset += 22
				}

				offset = renderer.gui.viewportWidth / 2 - 22 * inventory.width / 2
				for (x in 0 until inventory.width) {
					val pile = inventory.getItemPileAt(x, 0)
					if (pile != null && pile.amount > 1) {
						renderer.drawStringWithShadow(renderer.fonts.defaultFont(), offset + 15, 20, "${pile.amount}")
					}
					offset += 22
				}
			}


			/*

			// Health bar
			val maxHealth = entity.traits[TraitHealth::class]?.maxHealth ?: 0f
			var horizontalBitsToDraw = (8 + 118 * clamp(health / maxHealth, 0.0f, 1.0f)).toInt()

			renderer.drawBox(
					renderer.gui.viewportWidth / 2 - 128, 48, horizontalBitsToDraw * 1, 32,
					0f, 64f / 256f, horizontalBitsToDraw / 256f, 32f / 256f,
					"./textures/gui/hud/hud_survival.png",
					Vector4f(1.0f, 1.0f, 1.0f, 0.75f))

			// Food bar
			horizontalBitsToDraw = (0 + 126 * clamp(foodLevel / 100f, 0.0f, 1.0f)).toInt()
			renderer.drawBox(
					renderer.gui.viewportWidth / 2, 48, horizontalBitsToDraw * 1, 32,
					0.5f, 64f / 256f, 0.5f + horizontalBitsToDraw / 256f,
					32f / 256f, "./textures/gui/hud/hud_survival.png",
					Vector4f(1.0f, 1.0f, 1.0f, 0.75f))
*/
			val selectedItemPile = entity.traits[TraitSelectedItem::class]?.selectedItem
			// If we're using an item that can render an overlay
			if (selectedItemPile != null) {
				val item = selectedItemPile.item
				if (item is ItemOverlay)
					(item as ItemOverlay).drawItemOverlay(renderer, selectedItemPile)
			}

			// We don't want to render our own tag do we ?
			return
		}

		// Renders the nametag above the player heads
		val pos = entity.location

		// don't render tags too far out
		/*if (pos.distance(renderer.getCamera().getCameraPosition()) > 32f)
			return

		// Don't render a dead player tag
		if (health <= 0f)
			return

		val posOnScreen = renderer.getCamera().transform3DCoordinate(
				Vector3f(pos.x().toFloat(), pos.y().toFloat() + 2.0f, pos.z().toFloat()))

		val scale = posOnScreen.z()
		val txt = name.name
		val dekal = renderer.getFontRenderer().defaultFont().getWidth(txt) * 16 * scale
		if (scale > 0)
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
					posOnScreen.x() - dekal / 2, posOnScreen.y(), txt, 16 * scale, 16 * scale,
					Vector4f(1f, 1f, 1f, 1f))*/
	}
}