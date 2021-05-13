package xyz.chunkstories.core.generator

import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.*

class OreGeneration(declaration: Json.Dict, content: Content) {
    val voxel: BlockType = declaration["name"].asString!!.let { content.blockTypes.get(it)!! }
    val amount: IntRange = (declaration["amount_min"].asInt ?: 1)..(declaration["amount_max"].asInt ?: 8)
    val frequency: Double = declaration["frequency"].asDouble ?: 0.05
    val heightRange: IntRange = (declaration["min_height"].asInt ?: 0)..(declaration["max_height"].asInt ?: 256)
}

fun loadOresSpawningSection(json: Json.Array, content: Content): List<OreGeneration> {
    return json.elements.mapNotNull { it.asDict?.let { it1 -> OreGeneration(it1, content) } }
}