package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockTexture
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.block.components.BlockInventory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.inventory.InventoryManagementUIPanel
import xyz.chunkstories.api.gui.inventory.InventorySlot
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.loot.makeLootTableFromJson
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.MutableChunkCell
import kotlin.math.abs

class BlockFurnace(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
    override fun getTexture(cell: Cell, side: BlockSide): BlockTexture {
        val actualSide = BlockSide.values()[cell.data.extraData]//getSideMcStairsChestFurnace(cell.metaData)

        if (side == BlockSide.TOP)
            return textures[BlockSide.TOP.ordinal]
        if (side == BlockSide.BOTTOM)
            return textures[BlockSide.BOTTOM.ordinal]

        return if (side == actualSide) textures[BlockSide.FRONT.ordinal] else textures[BlockSide.LEFT.ordinal]
    }

    override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
        val definition = ItemDefinition(itemStore, name, Json.Dict(mapOf(
                "block" to Json.Value.Text(name),
                "class" to Json.Value.Text(ItemFurnace::class.java.canonicalName!!)
        )))

        return listOf(definition)
    }

    override fun whenPlaced(cell: MutableChunkCell) {
        cell.registerAdditionalData("fuel", FurnaceFuelInventory(cell))
        cell.registerAdditionalData("input", FurnaceInputInventory(cell))
        cell.registerAdditionalData("output", FurnaceOutputInventory(cell))
    }

    override fun onInteraction(entity: Entity, cell: MutableChunkCell, input: Input): Boolean {
        if (input.name == "mouse.right" && cell.world is WorldMaster) {

            val controller = entity.controller // entity.traits[TraitControllable::class]
            if (controller is Player) {
                val gameInstance = cell.world.gameInstance
                if (entity.location.distance(cell.location) <= 5 && gameInstance is Host) {
                    with(gameInstance) {
                        TODO("openInventory")
                        // player.openInventory(((cell as ChunkCell).additionalData["fuel"] as FurnaceFuelInventory).inventory)
                    }
                }
            }
        }
        return false
    }

    override fun tick(cell: MutableChunkCell) {
        //TODO better mechanics
        val fuelPile = ((cell as ChunkCell).additionalData["fuel"] as FurnaceFuelInventory).inventory.getItemPileAt(0, 0) ?: return
        val inputPile = ((cell as ChunkCell).additionalData["input"] as FurnaceInputInventory).inventory.getItemPileAt(0, 0) ?: return
        val outputPile = ((cell as ChunkCell).additionalData["output"] as FurnaceOutputInventory).inventory.getItemPileAt(0, 0)

        if(fuelPile.amount > 0 && inputPile.amount > 0) {
            val potentialOutput = makeLootTableFromJson(inputPile.item.definition.properties["smeltingDrops"]!!, cell.world.gameInstance.content).spawn().getOrNull(0) ?: return

            val currentAmount = outputPile?.amount ?: 0
            if(outputPile == null || (outputPile.item.definition == potentialOutput.first.definition && outputPile.amount + potentialOutput.second <= potentialOutput.first.definition.maxStackSize)) {
                ((cell as ChunkCell).additionalData["output"] as FurnaceOutputInventory).inventory.setItemAt(0, 0, potentialOutput.first, currentAmount + potentialOutput.second, force = true)
                inputPile.amount -= 1
                fuelPile.amount -= 1
                println("smelted ${inputPile.item} into $potentialOutput using ${fuelPile.item}")
            }
        }
        super.tick(cell)
    }

    // TODO drop items upon destruction
}

class FurnaceFuelInventory(cell: ChunkCell) : BlockInventory(cell, 1, 1) {
    override fun isItemAccepted(item: Item): Boolean {
        val value = item.definition.properties["fuelPower"].asInt
        return value != null
    }

    override fun createMainInventoryPanel(inventory: Inventory, layer: Layer) = createFurnaceInventoryPanel(cell, layer)
}

class FurnaceInputInventory(cell: ChunkCell) : BlockInventory(cell, 1, 1) {
    override fun isItemAccepted(item: Item): Boolean {
        val value = item.definition.properties["smeltingDrops"]
        return value != null
    }

    override fun createMainInventoryPanel(inventory: Inventory, layer: Layer) = createFurnaceInventoryPanel(cell, layer)
}

class FurnaceOutputInventory(cell: ChunkCell) : BlockInventory(cell, 1, 1) {
    override fun isItemAccepted(item: Item): Boolean {
        return false
    }

    override fun createMainInventoryPanel(inventory: Inventory, layer: Layer) = createFurnaceInventoryPanel(cell, layer)
}

fun createFurnaceInventoryPanel(cell: Cell, parentLayer: Layer): FurnaceInventoryUIPanel {
    val width = 20 *3 + 16
    val height = 20 * 3 + 16

    val fuelInventory = ((cell as ChunkCell).additionalData.get("fuel") as FurnaceFuelInventory).inventory
    val inputInventory = ((cell as ChunkCell).additionalData.get("input") as FurnaceInputInventory).inventory
    val outputInventory = ((cell as ChunkCell).additionalData.get("output") as FurnaceOutputInventory).inventory

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

class ItemFurnace(definition: ItemDefinition) : ItemBlock(definition) {
    override fun prepareNewBlockData(adjacentCell: Cell, adjacentCellSide: BlockSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): CellData {
        var data = super.prepareNewBlockData(adjacentCell, adjacentCellSide, placingEntity, hit)!!

        val loc = placingEntity.location
        val dx = hit.hitPosition.x() - loc.x()
        val dz = hit.hitPosition.z() - loc.z()

        val facing = if (abs(dx) > abs(dz)) {
            if (dx > 0)
                BlockSide.LEFT
            else
                BlockSide.RIGHT
        } else {
            if (dz > 0)
                BlockSide.BACK
            else
                BlockSide.FRONT
        }

        data = data.copy(extraData = facing.ordinal)

        return data
    }
}
