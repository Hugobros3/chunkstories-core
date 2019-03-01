//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity;

import xyz.chunkstories.api.animation.Animator;
import xyz.chunkstories.api.entity.DamageCause;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.EntityDefinition;
import xyz.chunkstories.api.entity.traits.TraitAnimated;
import xyz.chunkstories.api.entity.traits.TraitCollidable;
import xyz.chunkstories.api.entity.traits.TraitHitboxes;
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth;
import xyz.chunkstories.api.physics.Box;
import xyz.chunkstories.api.physics.EntityHitbox;
import xyz.chunkstories.api.sound.SoundSource.Mode;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.core.entity.components.EntityStance;
import xyz.chunkstories.core.entity.components.EntityStance.EntityHumanoidStance;
import xyz.chunkstories.core.entity.traits.TraitBasicMovement;
import xyz.chunkstories.core.entity.traits.TraitWalkingSounds;

public abstract class EntityHumanoid extends EntityLiving {

	protected TraitHitboxes hitboxes;
	protected TraitAnimated animationTrait;
	protected EntityStance stance;

	public EntityHumanoid(EntityDefinition t, World world) {
		super(t, world);

		EntityHitbox[] hitboxes = { new EntityHitbox(this, new Box(-0.15, 0.0, -0.25, 0.30, 0.675, 0.5), "boneTorso"),
				new EntityHitbox(this, new Box(-0.25, 0.0, -0.25, 0.5, 0.5, 0.5), "boneHead"),
				new EntityHitbox(this, new Box(-0.1, -0.375, -0.1, 0.2, 0.375, 0.2), "boneArmRU"),
				new EntityHitbox(this, new Box(-0.1, -0.375, -0.1, 0.2, 0.375, 0.2), "boneArmLU"),
				new EntityHitbox(this, new Box(-0.1, -0.3, -0.1, 0.2, 0.3, 0.2), "boneArmRD"),
				new EntityHitbox(this, new Box(-0.1, -0.3, -0.1, 0.2, 0.3, 0.2), "boneArmLD"),
				new EntityHitbox(this, new Box(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegRU"),
				new EntityHitbox(this, new Box(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegLU"),
				new EntityHitbox(this, new Box(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegRD"),
				new EntityHitbox(this, new Box(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegLD"),
				new EntityHitbox(this, new Box(-0.15, -0.075, -0.125, 0.35, 0.075, 0.25), "boneFootL"),
				new EntityHitbox(this, new Box(-0.15, -0.075, -0.125, 0.35, 0.075, 0.25), "boneFootR"), };

		this.stance = new EntityStance(this);

		this.animationTrait = new TraitAnimated(this) {
			Animator humanoidSkeletonAnimator = new HumanoidSkeletonAnimator(EntityHumanoid.this);

			@Override public Animator getAnimatedSkeleton() {
				return humanoidSkeletonAnimator;
			}
		};

		this.hitboxes = new TraitHitboxes(this) {
			@Override public EntityHitbox[] getHitBoxes() {
				return hitboxes;
			}
		};

		new TraitBasicMovement(this);

		// Override the entityliving's health component with a modified version
		this.entityHealth = new EntityHumanoidHealth(this);

		//new TraitRenderable(this, EntityHumanoidRenderer<EntityHumanoid>::new);

		new EntityHumanoidRenderer(this);

		new TraitCollidable(this) {

			@Override public Box[] getCollisionBoxes() {

				double height = stance.getStance() == EntityHumanoidStance.CROUCHING ? 1.45 : 1.9;
				if (EntityHumanoid.this.entityHealth.isDead())
					height = 0.2;
				return new Box[] { new Box(0.6, height, 0.6).translate(-0.3, 0.0, -0.3) };
			}

		};

		new TraitWalkingSounds(this);
	}

	@Override public void tick() {
		// Tick : will move the entity, solve velocity/acceleration and so on
		super.tick();

		this.traits.with(TraitWalkingSounds.class, s -> s.handleWalkingEtcSounds());
	}

	@Override public Box getBoundingBox() {
		if (entityHealth.isDead())
			return new Box(1.6, 1.0, 1.6).translate(-0.8, 0.0, -0.8);

		return new Box(1.0, stance.getStance() == EntityHumanoidStance.CROUCHING ? 1.5 : 2.0, 1.0).translate(-0.5, 0.0, -0.5);
	}

	/**
	 * Extends the original entity health component to add in support for damage
	 * multipliers
	 */
	protected class EntityHumanoidHealth extends TraitHealth {

		public EntityHumanoidHealth(Entity entity) {
			super(entity);
		}

		@Override public float damage(DamageCause cause, EntityHitbox osef, float damage) {
			if (osef != null) {
				if (osef.getName().equals("boneHead"))
					damage *= 2.8f;
				else if (osef.getName().contains("Arm"))
					damage *= 0.75;
				else if (osef.getName().contains("Leg"))
					damage *= 0.5;
				else if (osef.getName().contains("Foot"))
					damage *= 0.25;
			}

			damage *= 0.5;

			world.getSoundManager()
					.playSoundEffect("sounds/entities/flesh.ogg", Mode.NORMAL, EntityHumanoid.this.getLocation(), (float) Math.random() * 0.4f + 0.4f, 1);

			return super.damage(cause, null, damage);
		}
	}
}
