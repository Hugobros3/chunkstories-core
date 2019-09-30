package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.inventory.InventoryManagementUIPanel
import xyz.chunkstories.api.gui.inventory.InventorySlot
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.loot.makeLootTableFromJson
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.voxel.components.VoxelInventoryComponent
import xyz.chunkstories.api.voxel.textures.VoxelTexture
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellComponents
import xyz.chunkstories.api.world.cell.EditableCell
import xyz.chunkstories.api.world.cell.FutureCell
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.FreshChunkCell
import kotlin.math.abs

class BlockFurnace(definition: VoxelDefinition) : Voxel(definition) {
    override fun getVoxelTexture(cell: Cell, side: VoxelSide): VoxelTexture {
        val actualSide = VoxelSide.values()[cell.metaData]//getSideMcStairsChestFurnace(cell.metaData)

        if (side == VoxelSide.TOP)
            return voxelTextures[VoxelSide.TOP.ordinal]
        if (side == VoxelSide.BOTTOM)
            return voxelTextures[VoxelSide.BOTTOM.ordinal]

        return if (side == actualSide) voxelTextures[VoxelSide.FRONT.ordinal] else voxelTextures[VoxelSide.LEFT.ordinal]
    }

    override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
        val definition = ItemDefinition(itemStore, name, Json.Dict(mapOf(
                "voxel" to Json.Value.Text(name),
                "class" to Json.Value.Text(ItemFurnace::class.java.canonicalName!!)
        )))

        return listOf(definition)
    }

    override fun whenPlaced(cell: FreshChunkCell) {
        cell.registerComponent("fuel", FurnaceFuelInventory(cell.components))
        cell.registerComponent("input", FurnaceInputInventory(cell.components))
        cell.registerComponent("output", FurnaceOutputInventory(cell.components))
    }

    override fun handleInteraction(entity: Entity, cell: ChunkCell, input: Input): Boolean {
        if (input.name == "mouse.right" && cell.world is WorldMaster) {

            val controller = entity.traits[TraitControllable::class]?.controller
            if (controller is Player) {
                val player = controller as Player?
                val playerEntity = player!!.controlledEntity

                if (playerEntity != null) {
                    if (playerEntity.location.distance(cell.location) <= 5) {
                        player.openInventory(((cell as ChunkCell).components.getVoxelComponent("fuel") as FurnaceFuelInventory).inventory)
                    }
                }
            }
        }
        return false
    }

    override fun tick(cell: EditableCell) {
        //TODO better mechanics
        val fuelPile = ((cell as ChunkCell).components.getVoxelComponent("fuel") as FurnaceFuelInventory).inventory.getItemPileAt(0, 0) ?: return
        val inputPile = ((cell as ChunkCell).components.getVoxelComponent("input") as FurnaceInputInventory).inventory.getItemPileAt(0, 0) ?: return
        val outputPile = ((cell as ChunkCell).components.getVoxelComponent("output") as FurnaceOutputInventory).inventory.getItemPileAt(0, 0)

        if(fuelPile.amount > 0 && inputPile.amount > 0) {
            val potentialOutput = makeLootTableFromJson(inputPile.item.definition["smeltingDrops"]!!, cell.world.content).spawn().getOrNull(0) ?: return

            val currentAmount = outputPile?.amount ?: 0
            if(outputPile == null || (outputPile.item.definition == potentialOutput.first.definition && outputPile.amount + potentialOutput.second <= potentialOutput.first.definition.maxStackSize)) {
                ((cell as ChunkCell).components.getVoxelComponent("output") as FurnaceOutputInventory).inventory.setItemAt(0, 0, potentialOutput.first, currentAmount + potentialOutput.second, force = true)
                inputPile.amount -= 1
                fuelPile.amount -= 1
                println("smelted ${inputPile.item} into $potentialOutput using ${fuelPile.item}")
            }
        }
        super.tick(cell)
    }
}

class FurnaceFuelInventory(cell: CellComponents) : VoxelInventoryComponent(cell, 1, 1) {
    override fun isItemAccepted(item: Item): Boolean {
        val value = item.definition["fuelPower"].asInt
        return value != null
    }

    override fun createMainInventoryPanel(inventory: Inventory, layer: Layer) = createFurnaceInventoryPanel(this.holder.cell, layer)
}

class FurnaceInputInventory(cell: CellComponents) : VoxelInventoryComponent(cell, 1, 1) {
    override fun isItemAccepted(item: Item): Boolean {
        val value = item.definition["smeltingDrops"]
        return value != null
    }

    override fun createMainInventoryPanel(inventory: Inventory, layer: Layer) = createFurnaceInventoryPanel(this.holder.cell, layer)
}

class FurnaceOutputInventory(cell: CellComponents) : VoxelInventoryComponent(cell, 1, 1) {
    override fun isItemAccepted(item: Item): Boolean {
        return false
    }

    override fun createMainInventoryPanel(inventory: Inventory, layer: Layer) = createFurnaceInventoryPanel(this.holder.cell, layer)
}

fun createFurnaceInventoryPanel(cell: Cell, parentLayer: Layer): FurnaceInventoryUIPanel {
    val width = 20 *3 + 16
    val height = 20 * 3 + 16

    val fuelInventory = ((cell as ChunkCell).components.getVoxelComponent("fuel") as FurnaceFuelInventory).inventory
    val inputInventory = ((cell as ChunkCell).components.getVoxelComponent("input") as FurnaceInputInventory).inventory
    val outputInventory = ((cell as ChunkCell).components.getVoxelComponent("output") as FurnaceOutputInventory).inventory

    val panel = FurnaceInventoryUIPanel(parentLayer, width, height)

    val slot = InventorySlot.RealSlot(fuelInventory, 0, 0)
    val uiSlot = panel.InventorySlotUI(slot, 0 * 20 + 8,  0 * 20 + 8)
    panel.slots.add(uiSlot)

    val slot2 = InventorySlot.RealSlot(inputInventory, 0, 0)
    val uiSlot2 = panel.InventorySlotUI(slot2, 0 * 20 + 8,  2 * 20 + 8)
    panel.slots.add(uiSlot2)

    val slot3 = InventorySlot.RealSlot(outputInventory, 0, 0)
    val uiSlot3 = panel.InventorySlotUI(slot3, 2 * 20 + 8,  1 * 20 + 8)
    panel.slots.add(uiSlot3)

    return panel
}

class FurnaceInventoryUIPanel(layer: Layer, width: Int, height: Int) : InventoryManagementUIPanel(layer, width, height) {

}

class ItemFurnace(definition: ItemDefinition) : ItemVoxel(definition) {
    override fun prepareNewBlockData(cell: FutureCell, adjacentCell: Cell, adjacentCellSide: VoxelSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): Boolean {
        super.prepareNewBlockData(cell, adjacentCell, adjacentCellSide, placingEntity, hit)

        val loc = placingEntity.location
        val dx = (cell.x + 0.5) - loc.x()
        val dz = (cell.z + 0.5) - loc.z()

        val facing = if (abs(dx) > abs(dz)) {
            if (dx > 0)
                VoxelSide.LEFT
            else
                VoxelSide.RIGHT
        } else {
            if (dz > 0)
                VoxelSide.BACK
            else
                VoxelSide.FRONT
        }

        cell.metaData = facing.ordinal

        return true
    }
}
