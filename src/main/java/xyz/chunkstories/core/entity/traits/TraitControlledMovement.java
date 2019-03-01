//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits;

import xyz.chunkstories.api.client.LocalPlayer;
import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.TraitCollidable;
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable;
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth;
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation;
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity;
import xyz.chunkstories.api.physics.Box;
import xyz.chunkstories.api.world.cell.CellData;
import xyz.chunkstories.core.voxel.VoxelClimbable;

public abstract class TraitControlledMovement extends TraitBasicMovement {

	protected boolean running;

	public TraitControlledMovement(Entity entity) {
		super(entity);
	}

	public void tick(LocalPlayer controller) {
		TraitCollidable collisions = getEntity().traits.get(TraitCollidable.class);

		TraitHealth entityHealth = getEntity().traits.get(TraitHealth.class);
		TraitVelocity entityVelocity = getEntity().traits.get(TraitVelocity.class);
		TraitRotation entityRotation = getEntity().traits.get(TraitRotation.class);

		if (collisions == null || entityVelocity == null || entityRotation == null || entityHealth == null
				|| entityHealth.isDead())
			return;

		boolean focus = controller.hasFocus();

		boolean inWater = isInWater();
		boolean onLadder = false;

		all: for (CellData vctx : getEntity().world.getVoxelsWithin(getEntity().getTranslatedBoundingBox())) {
			if (vctx.getVoxel() instanceof VoxelClimbable) {
				for (Box box : vctx.getTranslatedCollisionBoxes()) {
					// TODO use actual collision model of the entity here
					if (box.collidesWith(getEntity().getTranslatedBoundingBox())) {
						onLadder = true;
						break all;
					}
				}
			}
		}

		if (focus) {
			if (entityVelocity.getVelocity().y <= 0.02) {
				if (!inWater && controller.getInputsManager().getInputByName("jump").isPressed()
						&& collisions.isOnGround()) {
					jump(0.15);
				} else if (inWater && controller.getInputsManager().getInputByName("jump").isPressed())
					jump(0.05);
			}
		}

		// Movement
		// Run ?
		if (focus && controller.getInputsManager().getInputByName("forward").isPressed()) {
			if (controller.getInputsManager().getInputByName("run").isPressed())
				running = true;
		} else
			running = false;

		double horizontalSpeed = 0.0;

		double modif = 0;
		if (focus) {
			if (controller.getInputsManager().getInputByName("forward").isPressed()
					|| controller.getInputsManager().getInputByName("left").isPressed()
					|| controller.getInputsManager().getInputByName("right").isPressed())

				horizontalSpeed = getForwardSpeed();
			else if (controller.getInputsManager().getInputByName("back").isPressed())
				horizontalSpeed = -getBackwardsSpeed();
		}

		// Water slows you down
		// Strafing
		if (controller.getInputsManager().getInputByName("forward").isPressed()) {
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 45;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 45;
		} else if (controller.getInputsManager().getInputByName("back").isPressed()) {
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 180 - 45;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 180 - 45;
		} else {
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 90;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 90;
		}

		if (onLadder) {
			entityVelocity.setVelocityY(
					(float) (Math.sin((-(entityRotation.getVerticalRotation()) / 180f * Math.PI)) * horizontalSpeed));
		}

		getTargetVelocity().x = (Math.sin((entityRotation.getHorizontalRotation() + modif) / 180f * Math.PI)
				* horizontalSpeed);
		getTargetVelocity().z = (Math.cos((entityRotation.getHorizontalRotation() + modif) / 180f * Math.PI)
				* horizontalSpeed);

		super.tick();

	}

	public abstract double getBackwardsSpeed();

	public abstract double getForwardSpeed();

	@Override
	public void tick() {
		Controller controller = getEntity().traits.tryWith(TraitControllable.class, TraitControllable::getController);

		// Consider player inputs...
		if (controller != null && controller instanceof LocalPlayer) {
			tick((LocalPlayer) controller);
		} else { // no player ? just let it sit
			super.tick();
		}
	}
}
