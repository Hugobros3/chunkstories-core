//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity.traits;

import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.Trait;
import io.xol.chunkstories.api.entity.traits.TraitCollidable;
import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth;
import io.xol.chunkstories.api.entity.traits.serializable.TraitRotation;
import io.xol.chunkstories.api.entity.traits.serializable.TraitVelocity;
import io.xol.chunkstories.api.world.cell.CellData;

public class TraitBasicMovement extends Trait {

	public Vector3d acceleration = new Vector3d();
	public Vector3d targetVelocity = new Vector3d(0);
	// double jumpForce = 0;

	protected long lastJump;

	/*
	 * boolean justJumped = false; boolean justLanded = false;
	 * 
	 * boolean running = false;
	 */

	public TraitBasicMovement(Entity entity) {
		super(entity);
	}

	public void tick() {
		TraitCollidable collisions = entity.traits.get(TraitCollidable.class);

		TraitVelocity entityVelocity = entity.traits.get(TraitVelocity.class);
		TraitRotation entityRotation = entity.traits.get(TraitRotation.class);

		if (collisions == null || entityVelocity == null || entityRotation == null)
			return;

		// Unloaded chunk ? nothing moves!
		if (entity.world.getChunkWorldCoordinates(entity.getLocation()) == null) {
			entityVelocity.setVelocity(new Vector3d(0.0f));
			acceleration.set(0);
			return;
		}

		collisions.unstuck();

		Vector3dc ogVelocity = entityVelocity.getVelocity();
		Vector3d velocity = new Vector3d(ogVelocity);

		// Applies head movement force and falloff
		Vector2f headRotationVelocity = entityRotation.tickInpulse();
		entityRotation.addRotation(headRotationVelocity.x(), headRotationVelocity.y());

		boolean inWater = isInWater();

		// TODO redo jump bc it's kinda stupid right now
		/*
		 * if (jumpForce > 0.0 && (!justJumped || inWater)) { //Set the velocity
		 * velocity.y = jumpForce; justJumped = true; //metersWalked = 0.0; jumpForce =
		 * 0.0; }
		 */

		// Set acceleration vector to wanted speed - actual speed
		if (entity.traits.tryWithBoolean(TraitHealth.class, eh -> eh.isDead()))
			targetVelocity = new Vector3d(0.0);

		acceleration = new Vector3d(targetVelocity.x() - velocity.x(), 0, targetVelocity.z() - velocity.z());

		// Limit maximal acceleration depending if we're on the groud or not, we
		// accelerate 2x faster on ground
		double maxAcceleration = collisions.isOnGround() ? 0.010 : 0.005;
		if (inWater)
			maxAcceleration = 0.005;
		if (acceleration.length() > maxAcceleration) {
			acceleration.normalize();
			acceleration.mul(maxAcceleration);
		}

		// Gravity
		double terminalVelocity = inWater ? -0.05 : -0.5;
		if (velocity.y() > terminalVelocity)
			velocity.y = (velocity.y() - 0.008);
		if (velocity.y() < terminalVelocity)
			velocity.y = (terminalVelocity);

		// Water limits your overall movement
		// TODO cleanup & generalize
		double targetSpeedInWater = 0.02;
		if (inWater) {
			if (velocity.length() > targetSpeedInWater) {
				double decelerationThen = Math.pow((velocity.length() - targetSpeedInWater), 1.0);

				double maxDeceleration = 0.006;
				if (decelerationThen > maxDeceleration)
					decelerationThen = maxDeceleration;

				acceleration.add(new Vector3d(velocity).normalize().negate().mul(decelerationThen));
			}
		}

		// Acceleration
		velocity.add(acceleration);

		// Eventually moves
		Vector3dc remainingToMove = entity.world.collisionsManager().tryMovingEntityWithCollisions(entity,
				entity.getLocation(), velocity);
		Vector2d remaining2d = new Vector2d(remainingToMove.x(), remainingToMove.z());

		// Auto-step logic
		if (remaining2d.length() > 0.001 && collisions.isOnGround()) {
			// Cap max speed we can get through the bump ?
			if (remaining2d.length() > 0.20d) {
				System.out.println("Too fast, capping");
				remaining2d.normalize();
				remaining2d.mul(0.20);
			}

			// Get whatever we are colliding with
			// Test if setting yourself on top would be ok
			// Do it if possible
			// TODO remake proper
			Vector3d blockedMomentum = new Vector3d(remaining2d.x(), 0, remaining2d.y());
			for (double d = 0.25; d < 0.5; d += 0.05) {
				// I don't want any of this to reflect on the object, because it causes ugly
				// jumps in the animation
				Vector3dc canMoveUp = entity.world.collisionsManager().runEntityAgainstWorldVoxelsAndEntities(entity,
						entity.getLocation(), new Vector3d(0.0, d, 0.0));
				// It can go up that bit
				if (canMoveUp.length() == 0.0f) {
					// Would it help with being stuck ?
					Vector3d tryFromHigher = new Vector3d(entity.getLocation());
					tryFromHigher.add(new Vector3d(0.0, d, 0.0));
					Vector3dc blockedMomentumRemaining = entity.world.collisionsManager()
							.runEntityAgainstWorldVoxelsAndEntities(entity, tryFromHigher, blockedMomentum);
					// If length of remaining momentum < of what we requested it to do, that means
					// it *did* go a bit further away
					if (blockedMomentumRemaining.length() < blockedMomentum.length()) {
						// Where would this land ?
						Vector3d afterJump = new Vector3d(tryFromHigher);
						afterJump.add(blockedMomentum);
						afterJump.sub(blockedMomentumRemaining);

						// land distance = whatever is left of our -0.55 delta when it hits the ground
						Vector3dc landDistance = entity.world.collisionsManager()
								.runEntityAgainstWorldVoxelsAndEntities(entity, afterJump, new Vector3d(0.0, -d, 0.0));
						afterJump.add(new Vector3d(0.0, -d, 0.0));
						afterJump.sub(landDistance);

						entity.entityLocation.set(afterJump);
						// this.setLocation(new Location(world, afterJump));

						remaining2d = new Vector2d(blockedMomentumRemaining.x(), blockedMomentumRemaining.z());
						break;
					}
				}
			}
		}

		// Collisions, snap to axises
		if (Math.abs(remaining2d.x()) >= 0.001d)
			velocity.x = (0d);
		if (Math.abs(remaining2d.y()) >= 0.001d)
			velocity.z = (0d);
		// Stap it
		if (collisions.isOnGround())
			velocity.y = (0d);

		entityVelocity.setVelocity(velocity);
	}

	public void jump(double force) {
		entity.traits.with(TraitVelocity.class, ev -> {
			Vector3d velocity = ev.getVelocity();
			velocity.y += force;

		});

		lastJump = entity.world.getTicksElapsed();
	}

	public boolean isInWater() {
		for (CellData cell : entity.world.getVoxelsWithin(entity.getTranslatedBoundingBox())) {
			if (cell.getVoxel().getDefinition().isLiquid())
				return true;
		}
		return false;
	}
}
