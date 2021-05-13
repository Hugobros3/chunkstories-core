//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import org.joml.Matrix4f
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.block.BlockRepresentation
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.block.MiningTool
import xyz.chunkstories.api.content.json.asDouble
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.entity.traits.serializable.TraitCreativeMode
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.chunk.MutableChunkCell
import xyz.chunkstories.core.item.ItemMiningTool

class TraitMining(entity: Entity) : Trait(entity) {
    override val traitName = "mining"

    var progress: BlockMiningOperation? = null
        private set

    private val hands: MiningTool = object : MiningTool {

        override val miningEfficiency: Float
            get() = 1f

        override val toolTypeName: String
            get() = "hands"

    }

    override fun tick() {
        val tool = entity.traits[TraitSelectedItem::class]?.selectedItem?.item as? ItemMiningTool ?: hands

        val world = entity.world

        val controller = entity.controller
        if (controller is Player && entity.traits[TraitCreativeMode::class]?.enabled != true) {
            val inputs = controller.inputsManager

            var lookingAt = entity.traits[TraitSight::class]?.getSelectableBlockLookingAt(5.0) as MutableChunkCell?

            if (lookingAt != null && lookingAt.location.distance(entity.location) > 7f)
                lookingAt = null

            if (lookingAt != null && inputs.getInputByName("mouse.left")!!.isPressed) {
                // val cell = world.peek(lookingAt)

                // Cancel mining if looking away or the block changed by itself
                if (progress != null && (lookingAt.location.distance(progress!!.location) > 0 || !lookingAt.data.blockType.sameKind(progress!!.voxel))) {
                    progress = null
                }

                if (progress == null) {
                    // Try starting mining something
                    progress = BlockMiningOperation(lookingAt, tool)
                } else {
                    progress!!.keepGoing(entity, controller)
                }
            } else {
                progress = null
            }
        }


    }
}

/** Represents a mining operation in progress, keeps all the state related to that */
class BlockMiningOperation(val cell: MutableChunkCell, internal var tool: MiningTool) {
    val voxel: BlockType
    // val material: VoxelMaterial
    val location: Location

    var progress: Double = 0.0
    val started: Long
    var lastSound: Long = 0

    val miningDifficulty: Double
    val toolAppropriate: Boolean
    val miningEfficiency: Double

    internal var timesSoundPlayed = 0

    init {
        this.location = cell.location

        voxel = cell.data.blockType

        // material = voxel.voxelMaterial

        miningDifficulty = voxel.definition["miningDifficulty"].asDouble ?: 1.0 // TODO material.miningDifficulty
        val preferredTool = voxel.definition["mineUsing"].asString ?: "TODO" // TODO material.mineUsing
        toolAppropriate = (preferredTool == "nothing_in_particular" || tool.toolTypeName == preferredTool)
        if (toolAppropriate)
            miningEfficiency = tool.miningEfficiency.toDouble()
        else
            miningEfficiency = 1.0

        this.progress = 0.0
        this.started = System.currentTimeMillis()
    }

    fun keepGoing(entity: Entity, player: Player): BlockMiningOperation? {
        // Progress using efficiency / ticks per second
        progress += miningEfficiency / 60f / miningDifficulty

        if (progress >= 1.0f) {
            if (entity.world is WorldMaster) {
                // TODO
                // val minedSound = voxel.voxelMaterial.minedSounds
                // entity.world.soundManager.playSoundEffect(minedSound.resolveIntRange(), SoundSource.Mode.NORMAL, location, 0.95f + Math.random().toFloat() * 0.10f, 1.0f)
                voxel.breakBlock(cell, player, tool)
            }

            return null
        }

        val now = System.currentTimeMillis()
        if (now - lastSound > 300) {
            lastSound = now

            // TODO
            // val miningSound = voxel.voxelMaterial.miningSounds
            // entity.world.soundManager.playSoundEffect(miningSound.resolveIntRange(), SoundSource.Mode.NORMAL, location, 0.85f + Math.random().toFloat() * 0.10f, 1.0f)
        }

        return this
    }

    val representation: List<Representation>
        get() {
            val step = (1 + (progress * 6.0).toInt()).coerceIn(1, 6)

            val representation = voxel.representation
            if(representation is BlockRepresentation.Custom) {
                val list = mutableListOf<Representation>()
                val cmri = object : BlockRepresentation.Custom.RenderInterface {
                    override fun addModel(model: Model, transformation: Matrix4f?, materialsOverrides: Map<Int, MeshMaterial>) {
                        val matrix = Matrix4f().translate(location.toVec3f())
                        if(transformation != null)
                            matrix.mul(transformation)
                        val pos = ModelPosition(matrix)

                        val instance = ModelInstance(model, pos, model.meshes.mapIndexed { i, material ->
                            Pair(i, MeshMaterial("breakingDecal", mapOf("albedoTexture" to "textures/voxel/cracking$step.png"), "forward"))
                        }.toMap())
                        list += instance
                    }
                }
                representation.drawRoutine(cmri, cell)
                return list
            }

            val model = voxel.content.models["voxels/blockmodels/cube/cube.dae"]
            val pos = ModelPosition(Matrix4f().translate(location.toVec3f()))

            val instance = ModelInstance(model, pos, model.meshes.mapIndexed { i, material ->
                Pair(i, MeshMaterial("breakingDecal", mapOf("albedoTexture" to "textures/voxel/cracking$step.png"), "forward"))
            }.toMap())

            return listOf(instance)
        }

    fun represent(representationsGobbler: RepresentationsGobbler) {
        for (r in representation)
            representationsGobbler.acceptRepresentation(r)
    }
}
