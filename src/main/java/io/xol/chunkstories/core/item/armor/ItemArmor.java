//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item.armor;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDeclaration;

import java.util.Collection;

public abstract class ItemArmor extends Item {
    private double damageMultiplier = 0.5;
    private String overlay = "notexture";

    public ItemArmor(ItemDeclaration type) {
        super(type);
    }

    /**
     * Returns either null (and affects the entire holder) or a list of body parts
     * it cares about
     */
    public abstract Collection<String> bodyPartsAffected();

    /**
     * Returns the multiplier, optional bodyPartName (might be null, handled that
     * case) for entities that support it
     */
    public float damageMultiplier(String bodyPartName) {
        return (float) damageMultiplier;
    }

    public String getOverlayTextureName() {
        return overlay;
    }

}
