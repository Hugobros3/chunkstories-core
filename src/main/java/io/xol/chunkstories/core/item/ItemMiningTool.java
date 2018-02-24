//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityWorldModifier;
import io.xol.chunkstories.api.events.player.voxel.PlayerVoxelModificationEvent;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDefinition;
import io.xol.chunkstories.api.item.ItemVoxel;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.item.ItemRenderer;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.materials.Material;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.World.WorldCell;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.core.entity.EntityGroundItem;

public class ItemMiningTool extends Item {

	public final String toolType;
	public final float miningEfficiency;

	public final long animationCycleDuration;

	private MiningProgress progress;
	public static MiningProgress myProgress;

	public ItemMiningTool(ItemDefinition type) {
		super(type);

		this.toolType = type.resolveProperty("toolType", "pickaxe");
		this.miningEfficiency = Float.parseFloat(type.resolveProperty("miningEfficiency", "0.5"));

		this.animationCycleDuration = Long.parseLong(type.resolveProperty("animationCycleDuration", "500"));
	}

	@Override
	public void tickInHand(Entity owner, ItemPile itemPile) {

		World world = owner.getWorld();
		if (owner instanceof EntityControllable && owner instanceof EntityWorldModifier) {
			EntityControllable entityControllable = (EntityControllable) owner;
			Controller controller = entityControllable.getController();

			if (controller != null && controller instanceof Player) {
				InputsManager inputs = controller.getInputsManager();

				Location lookingAt = entityControllable.getBlockLookingAt(true);

				if (lookingAt != null && lookingAt.distance(owner.getLocation()) > 7f)
					lookingAt = null;

				if (inputs.getInputByName("mouse.left").isPressed() && lookingAt != null) {

					WorldCell cell = world.peekSafely(lookingAt);

					// Cancel mining if looking away or the block changed by itself
					if (lookingAt == null || (progress != null && (lookingAt.distance(progress.loc) > 0 || !cell.getVoxel().sameKind(progress.voxel)))) {
						progress = null;
					}

					if (progress == null) {
						// Try starting mining something
						if (lookingAt != null)
							progress = new MiningProgress(world.peekSafely(lookingAt));
					} else {
						// Progress using efficiency / ticks per second
						progress.progress += ItemMiningTool.this.miningEfficiency / 60f / progress.materialHardnessForThisTool;

						if (progress.progress >= 1.0f) {
							if (owner.getWorld() instanceof WorldMaster) {
								FutureCell future = new FutureCell(cell);
								future.setVoxel(this.getDefinition().store().parent().voxels().air());

								// Check no one minds
								PlayerVoxelModificationEvent event = new PlayerVoxelModificationEvent(cell, future, (WorldModificationCause) entityControllable,
										(Player) controller);
								owner.getWorld().getGameContext().getPluginManager().fireEvent(event);

								// Break the block
								if (!event.isCancelled()) {
									Vector3d rnd = new Vector3d();
									for (int i = 0; i < 40; i++) {
										rnd.set(progress.loc);
										rnd.add(Math.random() * 0.98, Math.random() * 0.98, Math.random() * 0.98);
										world.getParticlesManager().spawnParticleAtPosition("voxel_frag", rnd);
									}
									world.getSoundManager().playSoundEffect("sounds/gameplay/voxel_remove.ogg", Mode.NORMAL, progress.loc, 1.0f, 1.0f);

									Location itemSpawnLocation = new Location(world, progress.loc);
									itemSpawnLocation.add(0.5, 0.0, 0.5);
									ItemPile droppedItemPile = null;

									for (ItemPile pile : progress.context.getVoxel().getItems()) {
										// Look for a basic match
										if (droppedItemPile == null)
											droppedItemPile = pile.duplicate();

										// Look for a hard match
										if (pile.getItem() instanceof ItemVoxel) {
											ItemVoxel pi = (ItemVoxel) pile.getItem();
											if (pi.getVoxelMeta() == progress.context.getMetaData())
												droppedItemPile = pile.duplicate();
										}
									}

									EntityGroundItem thrownItem = (EntityGroundItem) getDefinition().store().parent().entities()
											.getEntityTypeByName("groundItem").create(itemSpawnLocation);
									thrownItem.positionComponent.setPosition(itemSpawnLocation);
									thrownItem.velocityComponent.setVelocity(new Vector3d(Math.random() * 0.125 - 0.0625, 0.1, Math.random() * 0.125 - 0.0625));
									thrownItem.setItemPile(droppedItemPile);
									world.addEntity(thrownItem);

									try {
										world.poke(future, (WorldModificationCause) entityControllable);
									} catch (WorldException e) {
										// Didn't work
										// TODO make some ingame effect so as to clue in the player why it failed
									}
								}
							}

							progress = null;
						}
					}
				} else {
					progress = null;
				}

				Player player = (Player) controller;
				if (player.getContext() instanceof ClientInterface) {
					Player me = ((ClientInterface) player.getContext()).getPlayer();
					if (me.equals(player)) {
						myProgress = progress;
					}
				}
			}
		}

	}

	@Override
	public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer) {
		return new SwingToolRenderer(fallbackRenderer);
	}

	class SwingToolRenderer extends ItemRenderer {

		public SwingToolRenderer(ItemRenderer fallbackRenderer) {
			super(fallbackRenderer);
		}

		@Override
		public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world, Location location, Matrix4f transformation) {

			// Controlled by some player
			if (pile.getInventory() != null && pile.getInventory().getHolder() != null && pile.getInventory().getHolder() instanceof EntityControllable
					&& ((EntityControllable) pile.getInventory().getHolder()).getController() != null) {

				Matrix4f rotated = new Matrix4f(transformation);

				Vector3f center = new Vector3f(0.0f, -0.0f, -100f);

				rotated.translate(0.05f, -0.1f, 0f);
				MiningProgress progress = ((ItemMiningTool) pile.getItem()).progress;

				if (progress != null) {
					long elapsed = System.currentTimeMillis() - progress.started;
					float elapsedd = (float) elapsed;
					elapsedd /= (float) animationCycleDuration;

					if (elapsedd >= progress.timesSoundPlayed && elapsed > 50) {
						world.getSoundManager().playSoundEffect("sounds/gameplay/voxel_remove.ogg", Mode.NORMAL, progress.context.getLocation(), 1.5f, 1.0f);
						progress.timesSoundPlayed++;
					}

					float swingCycle = (float) Math.sin(Math.PI * 2 * elapsedd + Math.PI);

					rotated.translate(center);
					rotated.rotate((float) (swingCycle), 0f, 0f, 1f);

					center.negate();
					rotated.translate(center);
				}

				// rotated.rotate((System.currentTimeMillis() % 100000) / 10000f, 0f, 0f, 1f);
				rotated.scale(2.0f);

				super.renderItemInWorld(renderingInterface, pile, world, location, rotated);

			} else
				super.renderItemInWorld(renderingInterface, pile, world, location, transformation);
		}
	}

	public class MiningProgress {

		public final CellData context;
		public final Voxel voxel;
		public final Material material;
		public final Location loc;
		// public final int startId;
		public float progress;
		public final long started;

		public final float materialHardnessForThisTool;
		int timesSoundPlayed = 0;

		public MiningProgress(CellData context) {
			this.context = context;
			this.loc = context.getLocation();

			voxel = context.getVoxel();
			material = voxel.getMaterial();
			String hardnessString = null;

			// First order, check the voxel itself if it states a certain hardness for this tool type
			hardnessString = voxel.getDefinition().resolveProperty("hardnessFor" + ItemMiningTool.this.toolType, null);

			// Then check if the voxel states a general hardness multiplier
			if (hardnessString == null)
				hardnessString = voxel.getDefinition().resolveProperty("hardness", null);

			// if the voxel is devoid of information, we do the same on the material
			if (hardnessString == null)
				hardnessString = material.resolveProperty("materialHardnessFor" + ItemMiningTool.this.toolType, null);

			// Eventually we default to 1.0
			if (hardnessString == null)
				hardnessString = material.resolveProperty("materialHardness", "1.0");

			this.materialHardnessForThisTool = Float.parseFloat(hardnessString);

			this.progress = 0.0f;
			this.started = System.currentTimeMillis();
		}
	}
}
