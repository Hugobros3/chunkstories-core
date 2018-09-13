//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity.traits;

import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.TraitCollidable;
import io.xol.chunkstories.api.entity.traits.serializable.TraitController;
import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth;
import io.xol.chunkstories.api.entity.traits.serializable.TraitRotation;
import io.xol.chunkstories.api.entity.traits.serializable.TraitVelocity;
import io.xol.chunkstories.api.physics.Box;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.core.voxel.VoxelClimbable;

public abstract class TraitControlledMovement extends TraitBasicMovement {

	protected boolean running;

	public TraitControlledMovement(Entity entity) {
		super(entity);
	}

	public void tick(LocalPlayer controller) {
		TraitCollidable collisions = entity.traits.get(TraitCollidable.class);

		TraitHealth entityHealth = entity.traits.get(TraitHealth.class);
		TraitVelocity entityVelocity = entity.traits.get(TraitVelocity.class);
		TraitRotation entityRotation = entity.traits.get(TraitRotation.class);

		if (collisions == null || entityVelocity == null || entityRotation == null || entityHealth == null
				|| entityHealth.isDead())
			return;

		boolean focus = controller.hasFocus();

		boolean inWater = isInWater();
		boolean onLadder = false;

		all: for (CellData vctx : entity.world.getVoxelsWithin(entity.getTranslatedBoundingBox())) {
			if (vctx.getVoxel() instanceof VoxelClimbable) {
				for (Box box : vctx.getTranslatedCollisionBoxes()) {
					// TODO use actual collision model of the entity here
					if (box.collidesWith(entity.getTranslatedBoundingBox())) {
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

		targetVelocity.x = (Math.sin((180 - entityRotation.getHorizontalRotation() + modif) / 180f * Math.PI)
				* horizontalSpeed);
		targetVelocity.z = (Math.cos((180 - entityRotation.getHorizontalRotation() + modif) / 180f * Math.PI)
				* horizontalSpeed);

		super.tick();

	}

	public abstract double getBackwardsSpeed();

	public abstract double getForwardSpeed();

	@Override
	public void tick() {
		Controller controller = entity.traits.tryWith(TraitController.class, ec -> ec.getController());

		// Consider player inputs...
		if (controller != null && controller instanceof LocalPlayer) {
			tick((LocalPlayer) controller);
		} else { // no player ? just let it sit
			super.tick();
		}
	}
}
