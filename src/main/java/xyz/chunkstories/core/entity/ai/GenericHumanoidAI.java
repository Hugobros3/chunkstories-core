//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.ai;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.ai.AI;
import xyz.chunkstories.api.entity.traits.TraitCollidable;
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth;
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation;
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity;
import xyz.chunkstories.api.sound.SoundSource.Mode;
import xyz.chunkstories.core.entity.EntityHumanoid;
import xyz.chunkstories.core.entity.EntityLiving;
import xyz.chunkstories.core.entity.traits.TraitBasicMovement;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Random;

//TODO refator properly for components & traits
public class GenericHumanoidAI extends AI<EntityHumanoid> {
	static Random rng = new Random();

	long counter = 0;

	public GenericHumanoidAI(EntityHumanoid entity) {
		super(entity);
		currentTask = new AiTaskLookArround(5f);
	}

	public void tick() {
		if (entity.traits.tryWithBoolean(TraitHealth.class, eh -> eh.isDead())) {
			// Dead entities shouldn't be moving
			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().x = (0d);
			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().z = (0d);
			return;
		}

		counter++;

		if (currentTask != null)
			currentTask.execute();

		// Random bark
		if (rng.nextFloat() > 0.9990) {
			entity.getWorld().getSoundManager().playSoundEffect("sounds/entities/zombie/grunt.ogg", Mode.NORMAL, entity.getLocation(), (float) (0.9 + Math.random() * 0.2), 1.0f);
		}

		// Jump when in water
		if (entity.getWorld().peekSafely(entity.getLocation().add(0, 1.15, 0)).getVoxel().isLiquid()) {
			if (entity.traits.get(TraitVelocity.class).getVelocity().y() < 0.0)
				entity.traits.get(TraitVelocity.class).addVelocity(0.0, 0.10, 0.0);
		}

	}

	class AiTaskLookArround extends AiTask {

		AiTaskLookArround(double lookAtNearbyEntities) {
			this.lookAtNearbyEntities = lookAtNearbyEntities;
		}

		double targetH = 0;
		double targetV = 0;

		double lookAtNearbyEntities;
		int lookAtEntityCoolDown = 60 * 5;

		@Override
		public void execute() {
			// if(entity.traits.get(TraitRotation.class).getHorizontalRotation() ==
			// Float.NaN)
			// entity.traits.get(TraitRotation.class).setRotation(0.0, 0.0);

			if (Math.random() > 0.990) {
				targetH = entity.traits.get(TraitRotation.class).getHorizontalRotation()
						+ (Math.random() * 2.0 - 1.0) * 30f;

				if (Math.random() > 0.5)
					targetV = targetV / 2.0f + (Math.random() * 2.0 - 1.0) * 20f;

				if (targetV > 90f)
					targetV = 90f;
				if (targetV < -90f)
					targetV = -90f;
			}

			double diffH = targetH - entity.traits.get(TraitRotation.class).getHorizontalRotation();
			double diffV = targetV - entity.traits.get(TraitRotation.class).getVerticalRotation();

			entity.traits.get(TraitRotation.class).addRotation(diffH / 15f, diffV / 15f);

			if (lookAtEntityCoolDown > 0)
				lookAtEntityCoolDown--;

			if (lookAtNearbyEntities > 0.0 && lookAtEntityCoolDown == 0) {
				for (Entity entityToLook : entity.getWorld().getEntitiesInBox(entity.getLocation(),
						new Vector3d(lookAtNearbyEntities))) {
					if (!entityToLook.equals(entity)
							&& entityToLook.getLocation()
									.distance(GenericHumanoidAI.this.entity.getLocation()) <= lookAtNearbyEntities
							&& entityToLook instanceof EntityHumanoid
							&& !((EntityHumanoid) entityToLook).traits.get(TraitHealth.class).isDead()) {
						GenericHumanoidAI.this
								.setAiTask(new AiTaskLookAtEntity((EntityHumanoid) entityToLook, 10f, this));
						lookAtEntityCoolDown = (int) (Math.random() * 60 * 5);
						return;
					}
				}

				lookAtEntityCoolDown = (int) (Math.random() * 60);
			}

			if (Math.random() > 0.9990) {
				GenericHumanoidAI.this
						.setAiTask(new AiTaskGoSomewhere(
								new Location(entity.getWorld(), entity.getLocation()
										.add((Math.random() * 2.0 - 1.0) * 10, 0d, (Math.random() * 2.0 - 1.0) * 10)),
								505));
				return;
			}

			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().x = (0d);
			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().z = (0d);
			// entity.traits.get(TraitVelocity.class).setVelocityX(0);
			// entity.traits.get(TraitVelocity.class).setVelocityZ(0);
		}
	}

	class AiTaskLookAtEntity extends AiTask {

		EntityHumanoid entityFollowed;
		float maxDistance;
		AiTask previousTask;

		int timeBeforeDoingSomethingElse;

		public AiTaskLookAtEntity(EntityHumanoid entity, float maxDistance, AiTask previousTask) {
			this.entityFollowed = entity;
			this.maxDistance = maxDistance;
			this.previousTask = previousTask;
			this.timeBeforeDoingSomethingElse = (int) (60 * Math.random() * 30);
		}

		@Override
		public void execute() {
			timeBeforeDoingSomethingElse--;

			if (timeBeforeDoingSomethingElse <= 0 || entityFollowed == null
					|| entityFollowed.traits.get(TraitHealth.class).isDead()) {
				GenericHumanoidAI.this.setAiTask(previousTask);
				return;
			}

			if (entityFollowed.getLocation().distance(entity.getLocation()) > maxDistance) {
				// System.out.println("too
				// far"+entityFollowed.getLocation().distanceTo(entity.getLocation()));
				GenericHumanoidAI.this.setAiTask(previousTask);
				return;
			}

			Vector3d delta = entity.getLocation().sub(entityFollowed.getLocation());

			makeEntityLookAt(entity, delta);

			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().x = (0d);
			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().z = (0d);
			// entity.traits.get(TraitVelocity.class).setVelocityX(0);
			// entity.traits.get(TraitVelocity.class).setVelocityZ(0);
		}

	}

	class AiTaskGoAtEntity extends AiTask {

		EntityLiving entityFollowed;
		float maxDistance;
		AiTask previousTask;

		double entitySpeed = 0.02;

		public AiTaskGoAtEntity(EntityLiving entity, float maxDistance, AiTask previousTask) {
			this.entityFollowed = entity;
			this.maxDistance = maxDistance;
			this.previousTask = previousTask;
		}

		@Override
		public void execute() {
			if (entityFollowed == null || entityFollowed.traits.get(TraitHealth.class).isDead()) {
				GenericHumanoidAI.this.setAiTask(previousTask);
				return;
			}

			if (entityFollowed.getLocation().distance(entity.getLocation()) > maxDistance) {
				// System.out.println("Entity too
				// far"+entityFollowed.getLocation().distanceTo(entity.getLocation()));
				GenericHumanoidAI.this.setAiTask(previousTask);
				return;
			}

			Vector3d delta = entityFollowed.getLocation().sub(entity.getLocation());

			makeEntityLookAt(entity, new Vector3d(delta));

			delta.y = (0d);

			// System.out.println("CUCK +"+delta);

			delta.normalize().mul(entitySpeed);

			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().x = (delta.x());
			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().z = (delta.z());

			// entity.traits.get(TraitVelocity.class).setVelocityX(delta.getX());
			// entity.traits.get(TraitVelocity.class).setVelocityZ(delta.getZ());

			if (((EntityHumanoid) entity).traits.get(TraitCollidable.class).isOnGround()) {
				Vector3dc rem = entity.traits.get(TraitCollidable.class)
						.canMoveWithCollisionRestrain(entity.traits.get(TraitBasicMovement.class).getTargetVelocity());

				if (Math.sqrt(rem.x() * rem.x() + rem.z() * rem.z()) > 0.001) {
					// System.out.println("cuck");
					// If they have their feet in water
					if (entity.getWorld().peekSafely(entity.getLocation().add(0, 0.0, 0)).getVoxel().isLiquid()) {
						entity.traits.get(TraitVelocity.class).addVelocity(0.0, 0.20, 0.0);
						// System.out.println("feet in water yo");
					} else
						entity.traits.get(TraitVelocity.class).addVelocity(0.0, 0.15, 0.0);
				}
			}
		}

	}

	protected class AiTaskGoSomewhere extends AiTask {

		Location location;
		int timeOut = -1;

		protected AiTaskGoSomewhere(Location location) {
			this.location = location;
		}

		protected AiTaskGoSomewhere(Location location, int timeOutInTicks) {
			this.location = location;
			this.timeOut = timeOutInTicks;
		}

		@Override
		public void execute() {
			if (timeOut > 0)
				timeOut--;

			if (timeOut == 0) {
				GenericHumanoidAI.this.setAiTask(new AiTaskLookArround(5f));
				return;
			}

			Vector3d delta = new Vector3d(location).sub(entity.getLocation());

			if (delta.length() < 0.25) {
				GenericHumanoidAI.this.setAiTask(new AiTaskLookArround(5f));
				return;
			}

			makeEntityLookAt(entity, new Vector3d(delta));

			delta.y = (0d);

			double entitySpeed = 0.02;

			// System.out.println("CUCK +"+delta);

			delta.normalize().mul(entitySpeed);

			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().x = (delta.x());
			entity.traits.get(TraitBasicMovement.class).getTargetVelocity().z = (delta.z());

			// entity.traits.get(TraitVelocity.class).setVelocityX(delta.getX());
			// entity.traits.get(TraitVelocity.class).setVelocityZ(delta.getZ());

			if (((EntityHumanoid) entity).traits.get(TraitCollidable.class).isOnGround()) {
				Vector3dc rem = entity.traits.get(TraitCollidable.class)
						.canMoveWithCollisionRestrain(entity.traits.get(TraitBasicMovement.class).getTargetVelocity());
				// rem.setY(0.0D);

				if (Math.sqrt(rem.x() * rem.x() + rem.z() * rem.z()) > 0.001)
					// if(rem.length() > 0.001)
					entity.traits.get(TraitVelocity.class).addVelocity(0.0, 0.15, 0.0);
			}
		}

	}

	private void makeEntityLookAt(EntityHumanoid entity, Vector3d delta) {
		Vector2f deltaHorizontal = new Vector2f((float) (double) delta.x(), (float) (double) delta.z());
		Vector2f deltaVertical = new Vector2f(deltaHorizontal.length(), (float) (double) delta.y());
		deltaHorizontal.normalize();
		deltaVertical.normalize();

		double targetH = Math.acos(deltaHorizontal.y()) * 180.0 / Math.PI;
		double targetV = Math.asin(deltaVertical.y()) * 180.0 / Math.PI;

		if (deltaHorizontal.x() < 0.0)
			targetH *= -1;

		if (targetV > 90f)
			targetV = 90f;
		if (targetV < -90f)
			targetV = -90f;

		while (targetH < 0.0)
			targetH += 360.0;

		double diffH = targetH - entity.traits.get(TraitRotation.class).getHorizontalRotation();

		// Ensures we always take the fastest route
		if (Math.abs(diffH + 360) < Math.abs(diffH))
			diffH = diffH + 360;
		else if (Math.abs(diffH - 360) < Math.abs(diffH))
			diffH = diffH - 360;

		double diffV = targetV - entity.traits.get(TraitRotation.class).getVerticalRotation();

		if (Double.isNaN(diffH))
			diffH = 0;

		if (Double.isNaN(diffV))
			diffV = 0;

		entity.traits.get(TraitRotation.class).addRotation(diffH / 15f, diffV / 15f);
	}
}
