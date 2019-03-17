package xyz.chunkstories.core.entity

import org.joml.Vector3f
import org.joml.Vector4f
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.TraitHasOverlay
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.item.interfaces.ItemOverlay
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.core.entity.traits.TraitFoodLevel

class PlayerOverlay(entity: Entity) : TraitHasOverlay(entity) {

    private fun clamp(f: Float, min: Float, max: Float) = if(f < min) min else if(f > max) max else f

    override fun drawEntityOverlay(renderer: GuiDrawer) {
        val health = entity.traits[TraitHealth::class]?.getHealth() ?: 0f
        val foodLevel = entity.traits[TraitFoodLevel::class]?.getValue() ?: 0f

        if ((entity.world as? WorldClient)?.client?.player?.controlledEntity == entity) {

            renderer.drawBox(
                    renderer.gui.viewportWidth / 2 - 128, 48, 256, 32,
                    0f, 32f / 256f, 1f, 0f,
                    "./textures/gui/hud/hud_survival.png", null)

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