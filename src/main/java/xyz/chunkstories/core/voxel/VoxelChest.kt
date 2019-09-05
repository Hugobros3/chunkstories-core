//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.exceptions.world.WorldException
import xyz.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.voxel.components.VoxelComponent
import xyz.chunkstories.api.voxel.components.VoxelInventoryComponent
import xyz.chunkstories.api.voxel.textures.VoxelTexture
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.FutureCell
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.FreshChunkCell

import xyz.chunkstories.api.util.compatibility.getSideMcStairsChestFurnace

class VoxelChest(type: VoxelDefinition) : Voxel(type) {
    internal var frontTexture: VoxelTexture
    internal var sideTexture: VoxelTexture
    internal var topTexture: VoxelTexture

    init {

        frontTexture = store.textures.get(name + "_front")
        sideTexture = store.textures.get(name + "_side")
        topTexture = store.textures.get(name + "_top")
    }

    override fun handleInteraction(entity: Entity, voxelContext: ChunkCell, input: Input): Boolean {
        if (input.name == "mouse.right" && voxelContext.world is WorldMaster) {

            val controller = entity.traits[TraitControllable::class]?.controller
            if (controller is Player) {
                val player = controller as Player?
                val playerEntity = player!!.controlledEntity

                if (playerEntity != null) {
                    if (playerEntity.location.distance(voxelContext.location) <= 5) {
                        player.openInventory(getInventory(voxelContext))
                    }
                }
            }
        }
        return false
    }

    private fun getInventory(context: ChunkCell): Inventory {
        val comp = context.components.getVoxelComponent("chestInventory")
        val component = comp as VoxelInventoryComponent?
        return component!!.inventory
    }

    override fun whenPlaced(cell: FreshChunkCell) {
        // Create a new component and insert it into the chunk
        val component = VoxelInventoryComponent(cell.components, 10, 6)
        cell.registerComponent("chestInventory", component)
    }

    override fun getVoxelTexture(info: Cell, side: VoxelSide): VoxelTexture {
        val actualSide = getSideMcStairsChestFurnace(info.metaData)

        if (side == VoxelSide.TOP)
            return topTexture

        return if (side == actualSide) frontTexture else sideTexture

    }

    @Throws(IllegalBlockModificationException::class)
    // Chunk stories chests use Minecraft format to ease porting of maps
    fun onPlace(cell: FutureCell, cause: WorldModificationCause) {
        // Can't access the components of a non-yet placed FutureCell
        // getInventory(context);

        var stairsSide = 0
        // See:
        // http://minecraft.gamepedia.com/Data_values#Ladders.2C_Furnaces.2C_Chests.2C_Trapped_Chests
        if (cause is Entity) {
            val loc = (cause as Entity).location
            val dx = loc.x() - (cell.x + 0.5)
            val dz = loc.z() - (cell.z + 0.5)
            if (Math.abs(dx) > Math.abs(dz)) {
                if (dx > 0)
                    stairsSide = 4
                else
                    stairsSide = 5
            } else {
                if (dz > 0)
                    stairsSide = 2
                else
                    stairsSide = 3
            }
            cell.metaData = stairsSide
        }
    }

    @Throws(WorldException::class)
    override fun onRemove(cell: ChunkCell, cause: WorldModificationCause?) {

        // Delete the components as to not pollute the chunk's components space
        // context.components().erase();
    }
}
