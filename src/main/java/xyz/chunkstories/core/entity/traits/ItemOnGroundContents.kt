//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable
import xyz.chunkstories.api.exceptions.NullItemException
import xyz.chunkstories.api.exceptions.UndefinedItemTypeException
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.serialization.StreamSource
import xyz.chunkstories.api.world.serialization.StreamTarget

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class ItemOnGroundContents : TraitSerializable {
    var itemPile: ItemPile? = null
        set(value) {
            field = value
            if (entity.getWorld() is WorldMaster)
                this.pushComponentEveryone()
        }

    constructor(entity: Entity) : super(entity) {}

    constructor(entity: Entity, actualItemPile: ItemPile) : super(entity) {
        this.itemPile = actualItemPile
    }

    @Throws(IOException::class)
    override fun push(destinator: StreamTarget, dos: DataOutputStream) {
        if (itemPile == null)
            dos.writeInt(0)
        else
            itemPile!!.saveIntoStream(entity.getWorld().contentTranslator, dos)
    }

    @Throws(IOException::class)
    override fun pull(from: StreamSource, dis: DataInputStream) {
        try {
            itemPile = ItemPile.obtainItemPileFromStream(entity.getWorld().contentTranslator, dis)
        } catch (e: UndefinedItemTypeException) {
            // Etc
        } catch (e: NullItemException) {
        }

    }

}