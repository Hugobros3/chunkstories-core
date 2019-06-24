package xyz.chunkstories.core.gui

import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.gui.*
import xyz.chunkstories.api.gui.elements.Scroller
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.net.packets.PacketInventoryMoveItemPile
import xyz.chunkstories.api.world.WorldClientNetworkedRemote

class CreativeBlockSelector(gui: Gui, parentLayer: Layer?) : Layer(gui, parentLayer) {
    val allBlockItems: List<ItemVoxel>

    val scroller = Scroller<ItemsLine>(this, 0, 0, emptyList())

    val seperation = 4
    val itemSize = 24

    init {
        gui.client.inputsManager.mouse.isGrabbed = false

        elements.add(scroller)
        allBlockItems = gui.client.content.voxels().all().flatMap { it.variants }.map { it.newItem<ItemVoxel>() }.filter { !it.voxel.isAir() }
    }

    var hoverItem: ItemVoxel? = null

    override fun render(drawer: GuiDrawer) {
        scroller.elements.clear()

        val itemsPerLine = 8
        scroller.width = itemsPerLine * itemSize + (itemsPerLine - 1) * seperation + 8
        //scroller.width = width / 2
        scroller.height = height - 64
        scroller.positionY = 32
        scroller.positionX = (width - scroller.width) / 2 + 4
        scroller.elementsSpacing = seperation

        scroller.scrollIncrements = 8//itemSize + seperation

        /*var s = scroller.width
        var itemsPerLine = 0
        while(s > 40 + seperation) {
            s -= 40
            if(s >= 0)
                itemsPerLine++
            s -= seperation
        }*/

        val titleFont = gui.fonts.getFont("LiberationSans-Regular", 18f)
        val titleText = "Select a block..."
        val titleTextLength = titleFont.getWidth(titleText)
        drawer.drawStringWithShadow(titleFont,width / 2 - titleTextLength / 2, height - 32, titleText)

        val currentLine = mutableListOf<ItemVoxel>()
        val emptyMe = allBlockItems.toMutableList()
        while(emptyMe.isNotEmpty()) {
            currentLine.add(emptyMe.removeAt(0))
            if(currentLine.size == itemsPerLine) {
                scroller.elements.add(ItemsLine(this, scroller.width, itemSize, currentLine.map {
                    SelectBlockButton(it, this)
                }))
                currentLine.clear()
            }
        }
        if(currentLine.size > 0) {
            scroller.elements.add(ItemsLine(this, scroller.width, itemSize, currentLine.map {
                SelectBlockButton(it, this)
            }))
            currentLine.clear()
        }

        hoverItem = null
        scroller.render(drawer)

        val selectedItemFont = gui.fonts.getFont("LiberationSans-Regular", 16f)
        val selectedItemText = hoverItem?.name ?: ""
        val selectedItemTextLength = selectedItemFont.getWidth(selectedItemText)
        drawer.drawStringWithShadow(selectedItemFont,width / 2 - selectedItemTextLength / 2, 8, selectedItemText)
    }

    override fun handleInput(input: Input): Boolean {
        if(input.name == "exit" || input.name == "inventory") {
            gui.popTopLayer()
            return true
        }

        if(input is Mouse.MouseScroll) {
            scroller.handleScroll(input)
        }

        super.handleInput(input)
        return true // don't allow ingame stuff to happen
    }

    inner class ItemsLine(layer: Layer, width: Int, height: Int, val items: List<SelectBlockButton>) : GuiElement(layer, width, height), ClickableGuiElement {
        override fun render(drawer: GuiDrawer) {
            var posX = positionX
            for(item in items) {
                item.positionX = posX
                item.positionY = positionY
                posX += item.width
                posX += seperation
                item.render(drawer)
            }
        }

        override fun handleClick(mouseButton: Mouse.MouseButton): Boolean {
            for(item in items) {
                if(item.isMouseOver) {
                    return item.handleClick(mouseButton)
                }
            }
            return false
        }
    }

    inner class SelectBlockButton(val item: ItemVoxel, layer: Layer) : GuiElement(layer, itemSize, itemSize), ClickableGuiElement {
        override fun render(drawer: GuiDrawer) {
            var buttonTexture = "textures/gui/scalableButton.png"
            if (isMouseOver)
                buttonTexture = "textures/gui/scalableButtonOver.png"

            drawer.drawBoxWithCorners(positionX, positionY, width, height, 8, buttonTexture)

            drawer.drawBox(positionX + 4, positionY + 4, 16, 16, item.getTextureName())

            if(isMouseOver)
                hoverItem = item
        }

        override fun handleClick(mouseButton: Mouse.MouseButton): Boolean {
            this.layer.gui.client.soundManager.playSoundEffect("sounds/gui/gui_click2.ogg")

            gui.client.ingame?.apply {
                val controlledEntity = this.player.controlledEntity
                controlledEntity?.apply {
                    val selectedItemSlot = traits[TraitSelectedItem::class]?.getSelectedSlot() ?: return@apply
                    val targetInventory = traits[TraitInventory::class]?.inventory ?: return@apply

                    val world = world
                    if(world is WorldClientNetworkedRemote) {
                        val fakeInventory = Inventory(1,1,null,null)
                        val pile = ItemPile(fakeInventory, 0,0, item, 1)
                        val packetMove = PacketInventoryMoveItemPile(world, pile, fakeInventory, targetInventory, 0, 0, selectedItemSlot, 0, 1)
                        world.remoteServer.pushPacket(packetMove)
                    } else {
                        targetInventory.setItemAt(selectedItemSlot, 0, item, 1, true)
                    }
                    gui.popTopLayer()
                }
            }
            return true
        }

    }
}