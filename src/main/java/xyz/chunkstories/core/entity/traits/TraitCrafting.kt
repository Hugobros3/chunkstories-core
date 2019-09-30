//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.gui.inventory.InventoryManagementUI
import xyz.chunkstories.api.gui.inventory.InventorySlot
import xyz.chunkstories.api.gui.inventory.InventoryManagementUIPanel
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.inventory.Inventory

class TraitCrafting(entity: Entity) : Trait(entity) {
    override val traitName = "crafting"

    val baseCraftingStation = object : CraftingStation(2, entity.world.content.recipes) {}
    fun getCraftingStation(): CraftingStation? {
        val blockPointingAt = entity.traits[TraitSight::class]?.getSelectableBlockLookingAt(5.0)
        if (blockPointingAt != null) {
            val craftingAreaSize = blockPointingAt.voxel.definition["craftingAreaSize"]?.asInt
            if (craftingAreaSize != null) {
                return object : CraftingStation(craftingAreaSize, entity.world.content.recipes) {}
            }
        }

        return baseCraftingStation
    }
}

abstract class CraftingStation(val craftingAreaSideSize: Int, val recipes: Content.Recipes) {
    fun bringUpCraftingMenuSlots(ui: InventoryManagementUIPanel, baseHeight: Int) {
        val craftSizeReal = 20 * craftingAreaSideSize
        val offsetx = ui.width / 2 - craftSizeReal / 2

        val craftingSlots = Array(craftingAreaSideSize) { y ->
            Array(craftingAreaSideSize) { x ->
                InventorySlot.FakeSlot()
            }
        }
        val craftingUiSlots = Array(craftingAreaSideSize) { y ->
            Array(craftingAreaSideSize) { x ->
                val slot = craftingSlots[y][x]
                val uiSlot = ui.InventorySlotUI(slot, offsetx + x * 20, baseHeight + 8 + (craftingAreaSideSize - y - 1) * 20)
                uiSlot
            }
        }

        craftingUiSlots.forEach { it.forEach { ui.slots.add(it) } }

        val outputSlot = object : InventorySlot.SummoningSlot() {

            override val visibleContents: Pair<Item, Int>?
                get() {
                    val recipe = recipes.getRecipeForInventorySlots(craftingSlots)
                    if (recipe != null) {
                        var compensation = 1
                        if(InventoryManagementUI.draggingFrom == this) {
                            compensation = InventoryManagementUI.draggingAmount + 1
                        }
                        return Pair(recipe.result.first.newItem(), recipe.result.second * compensation)
                    }
                    return null
                }

            //TODO use packet to talk the server into this
            override fun commitTransfer(destinationInventory: Inventory, destX: Int, destY: Int, amount: Int) {
                val recipe = recipes.getRecipeForInventorySlots(craftingSlots) ?: return
                repeat(amount / recipe.result.second) {
                    recipe.craftUsing(craftingSlots, destinationInventory, destX, destY)
                }
            }
        }

        ui.slots.add(ui.InventorySlotUI(outputSlot, offsetx + craftSizeReal + 20, baseHeight + 8 + craftSizeReal / 2 - 10))
    }
}