package xyz.chunkstories.core.entity;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.client.LocalPlayer;
import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.TraitInteractible;
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable;
import xyz.chunkstories.api.entity.traits.serializable.TraitCreativeMode;
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem;
import xyz.chunkstories.api.events.player.voxel.PlayerVoxelModificationEvent;
import xyz.chunkstories.api.exceptions.world.WorldException;
import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.item.ItemVoxel;
import xyz.chunkstories.api.item.interfaces.ItemZoom;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.sound.SoundSource;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.WorldClient;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.api.world.cell.CellData;
import xyz.chunkstories.api.world.cell.FutureCell;
import xyz.chunkstories.core.CoreOptions;
import xyz.chunkstories.core.item.MiningProgress;
import xyz.chunkstories.core.item.inventory.CreativeInventoryHelperKt;

import java.util.Iterator;

class EntityPlayerController extends TraitControllable {

	private final EntityPlayer entityPlayer;

	public EntityPlayerController(EntityPlayer entity) {
		super(entity);
		entityPlayer = entity;
	}

	@Override public boolean onEachFrame() {
		Controller controller = getController();
		if (controller instanceof LocalPlayer && ((LocalPlayer) controller).hasFocus()) {
			moveCamera((LocalPlayer) controller);
			return true;
		}
		return false;
	}

	double lastPX = -1f;
	double lastPY = -1f;

	public void moveCamera(LocalPlayer controller) {
		if (entityPlayer.entityHealth.isDead())
			return;
		if (!controller.getInputsManager().getMouse().isGrabbed())
			return;

		double cPX = controller.getInputsManager().getMouse().getCursorX();
		double cPY = controller.getInputsManager().getMouse().getCursorY();

		double dx = 0, dy = 0;
		if (lastPX != -1f) {
			dx = cPX - controller.getWindow().getWidth() / 2.0;
			dy = cPY - controller.getWindow().getHeight() / 2.0;
		}
		lastPX = cPX;
		lastPY = cPY;

		double rotH = entityPlayer.entityRotation.getHorizontalRotation();
		double rotV = entityPlayer.entityRotation.getVerticalRotation();

		double modifier = 1.0f;
		ItemPile selectedItem = entityPlayer.traits.tryWith(TraitSelectedItem.class, eci -> eci.getSelectedItem());

		if (selectedItem != null && selectedItem.getItem() instanceof ItemZoom) {
			ItemZoom item = (ItemZoom) selectedItem.getItem();
			modifier = 1.0 / item.getZoomFactor();
		}

		rotH -= dx * modifier / 3f * controller.getClient().getConfiguration().getDoubleValue(CoreOptions.INSTANCE.getMouseSensitivity());
		rotV += dy * modifier / 3f * controller.getClient().getConfiguration().getDoubleValue(CoreOptions.INSTANCE.getMouseSensitivity());
		entityPlayer.entityRotation.setRotation(rotH, rotV);

		controller.getInputsManager().getMouse().setMouseCursorLocation(controller.getWindow().getWidth() / 2.0, controller.getWindow().getHeight() / 2.0);
	}

	/*@Override
	public void setupCamera(RenderingInterface renderer) {
		lastCameraLocation = getLocation();

		renderer.getCamera().setCameraPosition(new Vector3d(getLocation().add(0.0, stance.get().eyeLevel, 0.0)));

		renderer.getCamera().setRotationX(entityRotation.getVerticalRotation());
		renderer.getCamera().setRotationY(entityRotation.getHorizontalRotation());

		float modifier = 1.0f;
		if (selectedItemComponent.getSelectedItem() != null
				&& selectedItemComponent.getSelectedItem().getItem() instanceof ItemZoom) {
			ItemZoom item = (ItemZoom) selectedItemComponent.getSelectedItem().getItem();
			modifier = 1.0f / item.getZoomFactor();
		}

		float videoFov = (float) renderer.getClient().getConfiguration().getDoubleOption("client.video.fov");
		float speedEffect = (float) (entityVelocity.getVelocity().x() * entityVelocity.getVelocity().x()
				+ entityVelocity.getVelocity().z() * entityVelocity.getVelocity().z());

		speedEffect -= 0.07f * 0.07f;
		speedEffect = Math.max(0.0f, speedEffect);
		speedEffect *= 500.0f;

		renderer.getCamera().setFOV(modifier * (float) (videoFov + speedEffect));
	}*/

	@Override public boolean onControllerInput(Input input) {
		Controller controller = getController();

		// We are moving inventory bringup here !
		if (input.getName().equals("inventory") && entityPlayer.getWorld() instanceof WorldClient) {

			if (entityPlayer.creativeMode.get()) {
				((WorldClient) entityPlayer.getWorld()).getClient().getGui().openInventories(entityPlayer.inventory.getInventory(),
						CreativeInventoryHelperKt.createCreativeInventory(getEntity().getWorld().getContent().voxels()));
			} else {
				((WorldClient) entityPlayer.getWorld()).getClient().getGui()
						.openInventories(entityPlayer.inventory.getInventory(), entityPlayer.armor.getInventory());
			}

			return true;
		}

		Location blockLocation = entityPlayer.raytracer.getBlockLookingAt(true, false);

		double maxLen = 1024;

		if (blockLocation != null) {
			Vector3d diff = new Vector3d(blockLocation).sub(entityPlayer.getLocation());
			// Vector3d dir = diff.clone().normalize();
			maxLen = diff.length();
		}

		Vector3d initialPosition = new Vector3d(entityPlayer.getLocation());
		initialPosition.add(new Vector3d(0, entityPlayer.stance.getStance().getEyeLevel(), 0));

		Vector3dc direction = entityPlayer.entityRotation.getDirectionLookingAt();

		Iterator<Entity> i = entityPlayer.getWorld().getCollisionsManager().rayTraceEntities(initialPosition, direction, maxLen);
		while (i.hasNext()) {
			Entity e = i.next();
			if (e != entityPlayer && e.traits.with(TraitInteractible.class, ti -> ti.handleInteraction(entityPlayer, input)))
				return true;
		}

		ItemPile itemSelected = entityPlayer.selectedItemComponent.getSelectedItem();
		if (itemSelected != null) {
			// See if the item handles the interaction
			if (itemSelected.getItem().onControllerInput(entityPlayer, itemSelected, input, controller))
				return true;
		}
		if (entityPlayer.getWorld() instanceof WorldMaster) {
			// Creative mode features building and picking.
			if (entityPlayer.creativeMode.get()) {
				if (input.getName().equals("mouse.left")) {
					if (blockLocation != null) {
						// Player events mod
						if (controller instanceof Player) {
							Player player = (Player) controller;
							World.WorldCell cell = entityPlayer.getWorld().peekSafely(blockLocation);
							FutureCell future = new FutureCell(cell);
							future.setVoxel(entityPlayer.getDefinition().store().parent().voxels().air());
							future.setBlocklight(0);
							future.setSunlight(0);
							future.setMetaData(0);

							PlayerVoxelModificationEvent event = new PlayerVoxelModificationEvent(cell, future, TraitCreativeMode.Companion.getCREATIVE_MODE(),
									player);

							// Anyone has objections ?
							entityPlayer.getWorld().getGameContext().getPluginManager().fireEvent(event);

							if (event.isCancelled())
								return true;

							MiningProgress.Companion.spawnBlockDestructionParticles(blockLocation, entityPlayer.getWorld());
							entityPlayer.getWorld().getSoundManager()
									.playSoundEffect("sounds/gameplay/voxel_remove.ogg", SoundSource.Mode.NORMAL, blockLocation, 1.0f, 1.0f);

							try {
								entityPlayer.getWorld().poke(future, entityPlayer);
								// world.poke((int)blockLocation.x, (int)blockLocation.y, (int)blockLocation.z,
								// null, 0, 0, 0, this);
							} catch (WorldException e) {
								// Discard but maybe play some effect ?
							}

							return true;
						}
					}
				} else if (input.getName().equals("mouse.middle")) {
					if (blockLocation != null) {
						CellData ctx = entityPlayer.getWorld().peekSafely(blockLocation);

						if (!ctx.getVoxel().isAir()) {
							// Spawn new itemPile in his inventory
							ItemVoxel item = entityPlayer.getWorld().getGameContext().getContent().items().getItemDefinition("item_voxel").newItem();
							item.setVoxel(ctx.getVoxel());
							item.setVoxelMeta(ctx.getMetaData());

							//ItemPile itemVoxel = new ItemPile(item);
							//entityPlayer.inventory.setItemPileAt(entityPlayer.selectedItemComponent.getSelectedSlot(), 0, itemVoxel);
							entityPlayer.inventory.getInventory().setItemAt(entityPlayer.selectedItemComponent.getSelectedSlot(), 0, item);
							return true;
						}
					}
				}
			}
		}
		// Here goes generic entity response to interaction

		// n/a

		// Then we check if the world minds being interacted with
		return entityPlayer.getWorld().handleInteraction(entityPlayer, blockLocation, input);
	}
}
