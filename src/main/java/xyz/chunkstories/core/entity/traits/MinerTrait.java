//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.Trait;
import xyz.chunkstories.api.entity.traits.TraitVoxelSelection;
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable;
import xyz.chunkstories.api.events.voxel.WorldModificationCause;
import xyz.chunkstories.api.input.InputsManager;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.World.WorldCell;
import xyz.chunkstories.core.item.MiningProgress;
import xyz.chunkstories.core.item.MiningTool;

public class MinerTrait extends Trait {
	public MinerTrait(Entity entity) {
		super(entity);

		if (!(entity instanceof WorldModificationCause))
			throw new RuntimeException("Sorry but only entities implementing WorldModificationCause may be miners.");
	}

	private MiningProgress progress;

	private final MiningTool hands = new MiningTool() {

		@Override
		public float getMiningEfficiency() {
			return 1;
		}

		@Override
		public String getToolTypeName() {
			return "hands";
		}

	};

	public void tickTrait() {
		MiningTool tool = hands;

		World world = entity.getWorld();

		entity.traits.with(TraitControllable.class, ec -> {
			Controller controller = ec.getController();

			if (controller instanceof Player) {
				InputsManager inputs = controller.getInputsManager();

				Location lookingAt = entity.traits.tryWith(TraitVoxelSelection.class,
						tvs -> tvs.getBlockLookingAt(true, false));
				// entity.getBlockLookingAt(true);

				if (lookingAt != null && lookingAt.distance(entity.getLocation()) > 7f)
					lookingAt = null;

				if (lookingAt != null && inputs.getInputByName("mouse.left").isPressed()) {

					WorldCell cell = world.peekSafely(lookingAt);

					// Cancel mining if looking away or the block changed by itself
					if (progress != null && (lookingAt.distance(progress.loc) > 0 || !cell.getVoxel().sameKind(progress.voxel))) {
						progress = null;
					}

					if (progress == null) {
						// Try starting mining something
						progress = new MiningProgress(world.peekSafely(lookingAt), tool);
					} else {
						progress.keepGoing(entity, controller);
					}
				} else {
					progress = null;
				}
			}
		});

	}

	public MiningProgress getProgress() {
		return progress;
	}
}
