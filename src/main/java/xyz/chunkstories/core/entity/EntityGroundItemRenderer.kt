package xyz.chunkstories.core.entity

import org.joml.Matrix4f
import xyz.chunkstories.api.entity.traits.TraitRenderable
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.core.entity.traits.ItemOnGroundContents

class EntityGroundItemRenderer(entity: EntityGroundItem) : TraitRenderable<EntityGroundItem>(entity) {

    override fun buildRepresentation(representationsGobbler: RepresentationsGobbler) {
        val item = entity.traits[ItemOnGroundContents::class]?.itemPile ?: return //TODO show some error
        val matrix = Matrix4f()
        matrix.translate(entity.location.toVec3f())
        item.item.buildRepresentation(item, matrix, representationsGobbler)
    }
}