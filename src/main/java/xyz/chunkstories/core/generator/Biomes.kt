package xyz.chunkstories.core.generator

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.*
import xyz.chunkstories.api.converter.MinecraftBlocksTranslator
import xyz.chunkstories.api.math.random.WeightedSet
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.structures.McSchematicStructure
import xyz.chunkstories.api.voxel.structures.Structure

fun loadBiomesFromJson(content: Content, json: Json.Dict, translator: MinecraftBlocksTranslator): Map<String, HorizonGenerator.Biome> {
    return json.elements.mapValues {(_, properties) ->
        val properties = properties.asDict ?: throw Exception("this has to be a dict")
        object : HorizonGenerator.Biome {
            override val surfaceBlock: Voxel = content.voxels.getVoxel(properties["surfaceBlock"].asString!!)!!
            override val groundBlock: Voxel = content.voxels.getVoxel(properties["groundBlock"].asString!!)!!

            override val underwaterGround: Voxel = properties["underwaterGround"].asString?.let { content.voxels.getVoxel(it) } ?: groundBlock

            override val surfaceDecorationsDensity: Double = properties["surfaceDecorationsDensity"].asDouble ?: 0.0

            override val surfaceDecorations: WeightedSet<Voxel> = WeightedSet(
                    properties["surfaceDecorations"].asArray!!.elements.map {
                        val surfDecoration = it.asArray!!
                        val voxel = content.voxels.getVoxel(surfDecoration[0].asString!!)!!
                        Pair(surfDecoration[1].asDouble!!, voxel) }
            )
            override val treesDensity: Double = properties["treesDensity"].asDouble ?: 0.0
            override val treesVariants: WeightedSet<Structure> = properties["treesVariants"].asArray?.let { loadStructures(content, it, translator) } ?: WeightedSet(emptySet())

            override val additionalStructures: WeightedSet<Structure>
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

        }
    }
}

private fun loadStructures(content: Content, array: Json.Array, translator: MinecraftBlocksTranslator): WeightedSet<Structure> {
    return WeightedSet(array.elements.mapNotNull {
        val dict = it.asDict ?: return@mapNotNull null
        val weight = dict["weight"].asDouble ?: 1.0
        val type = dict["type"].asString!!

        when(type) {
            "mcschematic" -> {
                val url = dict["url"].asString!!
                val structure = McSchematicStructure.fromAsset(content.getAsset(url)!!, translator)!!
                println("$url ok")
                Pair(weight, structure)
            }
            else -> throw Exception("Unhandled type '$type'")
        }
    })
}