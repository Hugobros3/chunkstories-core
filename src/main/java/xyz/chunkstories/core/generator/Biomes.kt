package xyz.chunkstories.core.generator

import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.block.structures.Prefab
import xyz.chunkstories.api.block.structures.fromAsset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.*
import xyz.chunkstories.api.converter.MinecraftBlocksTranslator
import xyz.chunkstories.api.math.random.WeightedSet

fun loadBiomesFromJson(content: Content, json: Json.Dict, translator: MinecraftBlocksTranslator): Map<String, HorizonGenerator.Biome> {
    return json.elements.mapValues { (_, p) ->
        val properties = p.asDict ?: throw Exception("this has to be a dict")
        object : HorizonGenerator.Biome {
            override val surfaceBlock: BlockType = content.blockTypes[properties["surfaceBlock"].asString!!]!!
            override val groundBlock: BlockType = content.blockTypes[properties["groundBlock"].asString!!]!!

            override val underwaterGround: BlockType = properties["underwaterGround"].asString?.let { content.blockTypes.get(it) } ?: groundBlock

            override val surfaceDecorationsDensity: Double = properties["surfaceDecorationsDensity"].asDouble ?: 0.0

            override val surfaceDecorations: WeightedSet<BlockType> = WeightedSet(
                    properties["surfaceDecorations"].asArray!!.elements.map {
                        val surfDecoration = it.asArray!!
                        val voxel = content.blockTypes[surfDecoration[0].asString!!]!!
                        Pair(surfDecoration[1].asDouble!!, voxel) }
            )
            override val treesDensity: Double = properties["treesDensity"].asDouble ?: 0.0
            override val treesVariants: WeightedSet<Prefab> = properties["treesVariants"].asArray?.let { loadStructures(content, it, translator) } ?: WeightedSet(emptySet())

            override val additionalStructures: WeightedSet<Prefab>
                get() = TODO("not implemented")

        }
    }
}

private fun loadStructures(content: Content, array: Json.Array, translator: MinecraftBlocksTranslator): WeightedSet<Prefab> {
    return WeightedSet(array.elements.mapNotNull {
        val dict = it.asDict ?: return@mapNotNull null
        val weight = dict["weight"].asDouble ?: 1.0
        val type = dict["type"].asString!!

        when(type) {
            "mcschematic" -> {
                val url = dict["url"].asString!!
                val structure = fromAsset(content.getAsset(url)!!, translator)!!
                println("$url ok")
                Pair(weight, structure)
            }
            else -> throw Exception("Unhandled type '$type'")
        }
    })
}