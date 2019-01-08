//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item;

import xyz.chunkstories.api.item.Item;
import xyz.chunkstories.api.item.ItemDefinition;

public class ItemMiningTool extends Item implements MiningTool {

    public final String toolType;
    public final float miningEfficiency;

    public final long animationCycleDuration;

    public ItemMiningTool(ItemDefinition type) {
        super(type);

        this.toolType = type.resolveProperty("toolType", "pickaxe");
        this.miningEfficiency = Float.parseFloat(type.resolveProperty("miningEfficiency", "0.5"));

        this.animationCycleDuration = Long.parseLong(type.resolveProperty("animationCycleDuration", "500"));
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
    @Override
    public float getMiningEfficiency() {
        return this.miningEfficiency;
    }

    @Override
    public String getToolTypeName() {
        return this.toolType;
    }
}
