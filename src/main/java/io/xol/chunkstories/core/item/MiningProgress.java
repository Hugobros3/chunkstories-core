//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.player.voxel.PlayerVoxelModificationEvent;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.materials.VoxelMaterial;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.core.entity.EntityGroundItem;
import org.joml.Vector3d;

public class MiningProgress {

	public final CellData context;
	public final Voxel voxel;
	public final VoxelMaterial material;
	public final Location loc;

	public float progress;
	public final long started;

	public final float materialHardnessForThisTool;
	int timesSoundPlayed = 0;

	MiningTool tool;

	public MiningProgress(CellData context, MiningTool tool) {
		this.context = context;
		this.loc = context.getLocation();

		this.tool = tool;
		// toolType = tool != null ? tool.toolType : "hand";

		voxel = context.getVoxel();
		material = voxel.getVoxelMaterial();
		String hardnessString = null;

		// First order, check the voxel itself if it states a certain hardness for this
		// tool type
		hardnessString = voxel.getDefinition().resolveProperty("hardnessFor" + tool.getToolTypeName());

		// Then check if the voxel states a general hardness multiplier
		if (hardnessString == null)
			hardnessString = voxel.getDefinition().resolveProperty("hardness");

		// if the voxel is devoid of information, we do the same on the material
		if (hardnessString == null)
			hardnessString = material.resolveProperty("materialHardnessFor" + tool.getToolTypeName());

		// Eventually we default to 1.0
		if (hardnessString == null)
			hardnessString = material.resolveProperty("materialHardness", "1.0");

		this.materialHardnessForThisTool = Float.parseFloat(hardnessString);

		this.progress = 0.0f;
		this.started = System.currentTimeMillis();
	}

	public MiningProgress keepGoing(Entity owner, Controller controller) {
		// Progress using efficiency / ticks per second
		progress += tool.getMiningEfficiency() / 60f / materialHardnessForThisTool;

		if (progress >= 1.0f) {
			if (owner.getWorld() instanceof WorldMaster) {

				FutureCell future = new FutureCell(context);
				future.setVoxel(owner.getWorld().getContent().voxels().air());

				// Check no one minds
				PlayerVoxelModificationEvent event = new PlayerVoxelModificationEvent(context, future,
						(WorldModificationCause) owner, (Player) controller);
				owner.getWorld().getGameContext().getPluginManager().fireEvent(event);

				// Break the block
				if (!event.isCancelled()) {
					Vector3d rnd = new Vector3d();
					for (int i = 0; i < 40; i++) {
						rnd.set(loc);
						rnd.add(Math.random() * 0.98, Math.random() * 0.98, Math.random() * 0.98);
						context.getWorld().getParticlesManager().spawnParticleAtPosition("voxel_frag", rnd);
					}
					context.getWorld().getSoundManager().playSoundEffect("sounds/gameplay/voxel_remove.ogg",
							Mode.NORMAL, loc, 1.0f, 1.0f);

					Location itemSpawnLocation = new Location(context.getWorld(), loc);
					itemSpawnLocation.add(0.5, 0.0, 0.5);

					// ItemPile droppedItemPile = null;
					for (ItemPile droppedItemPile : context.getVoxel().getLoot(context,
							(WorldModificationCause) owner)) {

						EntityGroundItem thrownItem = (EntityGroundItem) context.getWorld().getContent().entities()
								.getEntityDefinition("groundItem").newEntity(itemSpawnLocation.world);
						thrownItem.traitLocation.set(itemSpawnLocation);
						thrownItem.entityVelocity.setVelocity(
								new Vector3d(Math.random() * 0.125 - 0.0625, 0.1, Math.random() * 0.125 - 0.0625));
						thrownItem.setItemPile(droppedItemPile);
						context.getWorld().addEntity(thrownItem);
					}

					try {
						context.getWorld().poke(future, (WorldModificationCause) owner);
					} catch (WorldException e) {
						// Didn't work
						// TODO make some ingame effect so as to clue in the player why it failed
					}
				}
			}

			return null;
		}

		return this;
	}

}
