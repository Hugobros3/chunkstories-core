//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import org.joml.Matrix4f
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.util.kotlin.toVec3f
import javax.swing.Spring.scale
import xyz.chunkstories.core.entity.traits.MinerTrait
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.graphics.representation.ModelInstance


class ItemMiningTool(type: ItemDefinition) : Item(type), MiningTool {

    override val toolTypeName: String = type.resolveProperty("toolType", "pickaxe")
    override val miningEfficiency: Float = java.lang.Float.parseFloat(type.resolveProperty("miningEfficiency", "0.5"))

    val animationCycleDuration: Long = java.lang.Long.parseLong(type.resolveProperty("animationCycleDuration", "500"))

    override fun buildRepresentation(pile: ItemPile, worldPosition: Matrix4f, representationsGobbler: RepresentationsGobbler) {
        val owner = pile.inventory.owner
        val miningAction = (owner as? Entity)?.traits?.get(MinerTrait::class)?.progress

        val modelName = definition.resolveProperty("model")

        if(modelName != null) {
            val model = definition.store.parent().models[modelName]

            val handTransformation = Matrix4f(worldPosition)

            if (miningAction != null) {
                handTransformation.rotate(Math.PI.toFloat(), 0f, 0f, 1f)
            }
            handTransformation.rotate(Math.PI.toFloat() * 1.5f, 0f, 1f, 0f)
            handTransformation.translate(0f, -0.2f, 0f)
            handTransformation.scale(0.5f)

            val position = ModelPosition(handTransformation)

            val modelInstance = ModelInstance(model, position)
            representationsGobbler.acceptRepresentation(modelInstance)

            return
        }

        super.buildRepresentation(pile, worldPosition, representationsGobbler)
    }

    /**
     * Some weapons have fancy renderers
     */
    /*public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer) {
        ItemRenderer itemRenderer;

        String modelName = getDefinition().resolveProperty("model", "none");
        if (!modelName.equals("none"))
            itemRenderer = new ItemModelRenderer(this, fallbackRenderer, modelName,
                    getDefinition().resolveProperty("modelDiffuse", "none")) {

                @Override
                public void renderItemInWorld(RenderingInterface renderingContext, ItemPile pile, World world,
                                              Location location, Matrix4f handTransformation) {

                    boolean mining = false;
                    if (pile.getInventory() instanceof TraitInventory) {
                        Entity entity = ((TraitInventory) pile.getInventory()).entity;
                        // System.out.println(entity);
                        MinerTrait miningTrait = entity.traits.get(MinerTrait.class);
                        if (miningTrait != null) {
                            if (miningTrait.getProgress() != null)
                                mining = true;
                        }
                    }

                    if (mining) {
                        handTransformation.rotate((float) Math.PI, 0, 0, 1);
                    }
                    handTransformation.rotate((float) Math.PI * 1.5f, 0, 1, 0);
                    handTransformation.translate(0, -0.2f, 0);
                    handTransformation.scale(0.5f);

                    super.renderItemInWorld(renderingContext, pile, world, location, handTransformation);
                }
            };
        else
            itemRenderer = new FlatIconItemRenderer(this, fallbackRenderer, getDefinition());

        return itemRenderer;
    }*/
}
