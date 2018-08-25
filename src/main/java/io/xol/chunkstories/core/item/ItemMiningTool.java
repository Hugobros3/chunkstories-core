//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory;
import io.xol.chunkstories.api.graphics.representation.Representation;
import io.xol.chunkstories.api.graphics.representation.RepresentationElement;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDeclaration;
import io.xol.chunkstories.api.item.ItemRepresentationBuilder;
import io.xol.chunkstories.api.item.ItemRepresentationBuildingContext;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.core.entity.traits.MinerTrait;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4d;

public class ItemMiningTool extends Item implements MiningTool {

    public String toolType = "pickaxe";
    public float miningEfficiency = 0.5f;

    public long animationCycleDuration = 500;

    public ItemMiningTool(ItemDeclaration<ItemMiningTool> type) {
        super(type);
    }

    static void buildRepresentation(ItemRepresentationBuildingContext<ItemMiningTool> context) {
        ItemMiningTool item = context.getItem();

        //Take the representation specified in the file and build that
        ItemDeclaration<ItemMiningTool> def = (ItemDeclaration<ItemMiningTool>) item.getDefinition();
        def.getRepresentation().build(context);

        final Representation representation = context.getRepresentation();

        //Additionally set up a onEveryFrame hook that appropriately rotates the item
        final RepresentationElement root = representation.getRoot();
        context.onEveryFrame((frameContext -> {
                    Inventory inv = item.getItemPile().getInventory();

                    final Matrix4d transformation = root.getMatrix();
                    transformation.identity();

                    boolean mining = false;
                    if (inv instanceof TraitInventory) {
                        Entity entity = ((TraitInventory) inv).entity;

                        MinerTrait miningTrait = entity.traits.get(MinerTrait.class);
                        if (miningTrait != null) {
                            if (miningTrait.getProgress() != null)
                                mining = true;
                        }
                    }

                    if (mining) {
                        transformation.rotate((float) Math.PI, 0, 0, 1);
                    }
                    transformation.rotate((float) Math.PI * 1.5f, 0, 1, 0);
                    transformation.translate(0, -0.2f, 0);
                    transformation.scale(0.5f);

                    return Unit.INSTANCE;
                })

        );
    }

    @NotNull
    @Override
    public ItemRepresentationBuilder<ItemMiningTool> getRepresentation() {
        return ItemMiningTool::buildRepresentation;
    }

    @Override
    public float getMiningEfficiency() {
        return this.miningEfficiency;
    }

    @Override
    public String getToolTypeName() {
        return this.toolType;
    }
}
