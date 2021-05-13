//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.block.BlockRepresentation
import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.world.WorldCell
import xyz.chunkstories.api.world.cell.Cell

class VoxelFence(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {

	val post: Model
	val beams: Array<Model>

	init {
		post = content.models["voxels/blockmodels/fence/fence_post.dae"]
		beams = BlockSide.values().copyOfRange(0, 4).map { content.models["voxels/blockmodels/fence/fence_${it.name.toLowerCase()}.dae"] }.toTypedArray()

	}

	override fun loadRepresentation(): BlockRepresentation {
		val mappedOverrides = mapOf(0 to MeshMaterial("fence_material", mapOf("albedoTexture" to "voxels/textures/${textures[BlockSide.FRONT.ordinal].name}.png")))
		return BlockRepresentation.Custom { cell ->
			addModel(post, materialsOverrides = mappedOverrides)

			val leftNeighbour = if (cell is WorldCell) cell.getNeighbour(BlockSide.LEFT) else null
			val leftNeighbourBlockType = leftNeighbour?.data?.blockType
			val connectLeft = leftNeighbourBlockType != null && (leftNeighbourBlockType.solid && leftNeighbourBlockType.opaque || leftNeighbourBlockType.sameKind(this@VoxelFence))

			val frontNeighbour = if (cell is WorldCell) cell.getNeighbour(BlockSide.FRONT) else null
			val frontNeighbourBlockType = frontNeighbour?.data?.blockType
			val connectFront = frontNeighbourBlockType != null && (frontNeighbourBlockType.solid && frontNeighbourBlockType.opaque || frontNeighbourBlockType.sameKind(this@VoxelFence))

			val rightNeighbour = if (cell is WorldCell) cell.getNeighbour(BlockSide.RIGHT) else null
			val rightNeighbourBlockType = rightNeighbour?.data?.blockType
			val connectRight = rightNeighbourBlockType != null && (rightNeighbourBlockType.solid && rightNeighbourBlockType.opaque || rightNeighbourBlockType.sameKind(this@VoxelFence))

			val backNeighbour = if (cell is WorldCell) cell.getNeighbour(BlockSide.BACK) else null
			val backNeighbourBlockType = backNeighbour?.data?.blockType
			val connectBack = backNeighbourBlockType != null && (backNeighbourBlockType.solid && backNeighbourBlockType.opaque || backNeighbourBlockType.sameKind(this@VoxelFence))

			if (connectLeft)
				addModel(beams[BlockSide.LEFT.ordinal], materialsOverrides = mappedOverrides)
			if (connectRight)
				addModel(beams[BlockSide.RIGHT.ordinal], materialsOverrides = mappedOverrides)
			if (connectFront)
				addModel(beams[BlockSide.FRONT.ordinal], materialsOverrides = mappedOverrides)
			if (connectBack)
				addModel(beams[BlockSide.BACK.ordinal], materialsOverrides = mappedOverrides)
		}
	}

	override fun getCollisionBoxes(cell: Cell): Array<Box> {
		val leftNeighbour = if (cell is WorldCell) cell.getNeighbour(BlockSide.LEFT) else null
		val leftNeighbourBlockType = leftNeighbour?.data?.blockType
		val connectLeft = leftNeighbourBlockType != null && (leftNeighbourBlockType.solid && leftNeighbourBlockType.opaque || leftNeighbourBlockType.sameKind(this))

		val frontNeighbour = if (cell is WorldCell) cell.getNeighbour(BlockSide.FRONT) else null
		val frontNeighbourBlockType = frontNeighbour?.data?.blockType
		val connectFront = frontNeighbourBlockType != null && (frontNeighbourBlockType.solid && frontNeighbourBlockType.opaque || frontNeighbourBlockType.sameKind(this))

		val rightNeighbour = if (cell is WorldCell) cell.getNeighbour(BlockSide.RIGHT) else null
		val rightNeighbourBlockType = rightNeighbour?.data?.blockType
		val connectRight = rightNeighbourBlockType != null && (rightNeighbourBlockType.solid && rightNeighbourBlockType.opaque || rightNeighbourBlockType.sameKind(this))

		val backNeighbour = if (cell is WorldCell) cell.getNeighbour(BlockSide.BACK) else null
		val backNeighbourBlockType = backNeighbour?.data?.blockType
		val connectBack = backNeighbourBlockType != null && (backNeighbourBlockType.solid && backNeighbourBlockType.opaque || backNeighbourBlockType.sameKind(this))

		val width = 0.4
		val delta1 = 0.3
		val delta2 = 0.7

		val boxes =
				// Cross case
				if (connectLeft && connectFront && connectRight && connectBack)
					arrayOf(Box.fromExtents(width, 1.0, 1.0).translate(delta1, 0.0, 0.0), Box.fromExtents(1.0, 1.0, width).translate(0.0, 0.0, delta1))
				// T cases
				else if (connectLeft && connectFront && connectRight)
					arrayOf(Box.fromExtents(1.0, 1.0, width).translate(0.0, 0.0, delta1), Box.fromExtents(width, 1.0, 0.5).translate(delta1, 0.0, 0.5))
				else if (connectLeft && connectFront && connectBack)
					arrayOf(Box.fromExtents(width, 1.0, 1.0).translate(delta1, 0.0, 0.0), Box.fromExtents(0.5, 1.0, width).translate(0.0, 0.0, delta1))
				else if (connectLeft && connectBack && connectRight)
					arrayOf(Box.fromExtents(1.0, 1.0, width).translate(0.0, 0.0, delta1), Box.fromExtents(width, 1.0, 0.5).translate(delta1, 0.0, 0.0))
				else if (connectBack && connectFront && connectRight)
					arrayOf(Box.fromExtents(width, 1.0, 1.0).translate(delta1, 0.0, 0.0), Box.fromExtents(0.5, 1.0, width).translate(0.5, 0.0, delta1))
				// Line cases
				else if (connectLeft && connectRight)
					arrayOf(Box.fromExtents(1.0, 1.0, width).translate(0.0, 0.0, delta1))
				else if (connectFront && connectBack)
					arrayOf(Box.fromExtents(width, 1.0, 1.0).translate(delta1, 0.0, 0.0))
				// Corner cases
				else if (connectLeft && connectBack)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(0.0, 0.0, delta1), Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, 0.0))
				else if (connectRight && connectBack)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(delta1, 0.0, delta1), Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, 0.0))
				else if (connectLeft && connectFront)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(0.0, 0.0, delta1), Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, delta1))
				else if (connectRight && connectFront)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(delta1, 0.0, delta1), Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, delta1))
				// Lone cases
				else if (connectLeft)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(0.0, 0.0, delta1))
				else if (connectRight)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(delta1, 0.0, delta1))
				else if (connectFront)
					arrayOf(Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, delta1))
				else if (connectBack)
					arrayOf(Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, 0.0))
				else
					arrayOf(Box.fromExtents(width, 1.0, width).translate(delta1, 0.0, delta1))

		return boxes
	}
}
