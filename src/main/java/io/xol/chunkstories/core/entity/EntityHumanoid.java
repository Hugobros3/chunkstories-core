//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import java.util.Arrays;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.animation.CompoundAnimationHelper;
import io.xol.chunkstories.api.animation.SkeletalAnimation;
import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.entity.traits.TraitAnimated;
import io.xol.chunkstories.api.entity.traits.TraitCollidable;
import io.xol.chunkstories.api.entity.traits.TraitHitboxes;
import io.xol.chunkstories.api.entity.traits.TraitRenderable;
import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth;
import io.xol.chunkstories.api.entity.traits.serializable.TraitSelectedItem;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemVoxel;
import io.xol.chunkstories.api.item.interfaces.ItemCustomHoldingAnimation;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.physics.EntityHitbox;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.core.entity.components.EntityStance;
import io.xol.chunkstories.core.entity.components.EntityStance.EntityHumanoidStance;
import io.xol.chunkstories.core.entity.traits.MinerTrait;
import io.xol.chunkstories.core.entity.traits.TraitBasicMovement;
import io.xol.chunkstories.core.entity.traits.TraitWalkingSounds;
import io.xol.chunkstories.core.item.ItemMiningTool;
import io.xol.chunkstories.core.item.MiningProgress;

public abstract class EntityHumanoid extends EntityLiving {

	protected TraitHitboxes hitboxes;
	protected TraitAnimated animationTrait;
	protected EntityStance stance;

	public EntityHumanoid(EntityDefinition t, Location location) {
		super(t, location);

		EntityHitbox[] hitboxes = { new EntityHitbox(this, new CollisionBox(-0.15, 0.0, -0.25, 0.30, 0.675, 0.5), "boneTorso"),
				new EntityHitbox(this, new CollisionBox(-0.25, 0.0, -0.25, 0.5, 0.5, 0.5), "boneHead"),
				new EntityHitbox(this, new CollisionBox(-0.1, -0.375, -0.1, 0.2, 0.375, 0.2), "boneArmRU"),
				new EntityHitbox(this, new CollisionBox(-0.1, -0.375, -0.1, 0.2, 0.375, 0.2), "boneArmLU"),
				new EntityHitbox(this, new CollisionBox(-0.1, -0.3, -0.1, 0.2, 0.3, 0.2), "boneArmRD"),
				new EntityHitbox(this, new CollisionBox(-0.1, -0.3, -0.1, 0.2, 0.3, 0.2), "boneArmLD"),
				new EntityHitbox(this, new CollisionBox(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegRU"),
				new EntityHitbox(this, new CollisionBox(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegLU"),
				new EntityHitbox(this, new CollisionBox(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegRD"),
				new EntityHitbox(this, new CollisionBox(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegLD"),
				new EntityHitbox(this, new CollisionBox(-0.15, -0.075, -0.125, 0.35, 0.075, 0.25), "boneFootL"),
				new EntityHitbox(this, new CollisionBox(-0.15, -0.075, -0.125, 0.35, 0.075, 0.25), "boneFootR"), };

		this.hitboxes = new TraitHitboxes(this) {
			@Override
			public EntityHitbox[] getHitBoxes() {
				return hitboxes;
			}
		};

		this.animationTrait = new TraitAnimated(this) {
			CachedLodSkeletonAnimator cachedSkeleton = new CachedLodSkeletonAnimator(EntityHumanoid.this, new EntityHumanoidAnimatedSkeleton(), 25f, 75f);

			@Override
			public SkeletonAnimator getAnimatedSkeleton() {
				return cachedSkeleton;
			}
		};

		this.stance = new EntityStance(this);
		
		new TraitBasicMovement(this);
		
		//Override the entityliving's health component with a modified version
		this.entityHealth = new EntityHumanoidHealth(this);
		
		new TraitRenderable(this, EntityHumanoidRenderer<EntityHumanoid>::new );
		
		new TraitCollidable(this) {

			@Override
			public CollisionBox[] getCollisionBoxes() {
				
				double height = stance.get() == EntityHumanoidStance.CROUCHING ? 1.45 : 1.9;
				if (EntityHumanoid.this.entityHealth.isDead())
					height = 0.2;
				return new CollisionBox[] { new CollisionBox(0.6, height, 0.6).translate(-0.3, 0.0, -0.3) };
			}
			
		};
		
		new TraitWalkingSounds(this);
	}

	@Override
	public void tick() {
		// Tick : will move the entity, solve velocity/acceleration and so on
		super.tick();
		
		this.traits.with(TraitWalkingSounds.class, s -> s.handleWalkingEtcSounds());
	}

	@Override
	public CollisionBox getBoundingBox() {
		if (entityHealth.isDead())
			return new CollisionBox(1.6, 1.0, 1.6).translate(-0.8, 0.0, -0.8);
		
		return new CollisionBox(1.0, stance.get() == EntityHumanoidStance.CROUCHING ? 1.5 : 2.0, 1.0).translate(-0.5, 0.0, -0.5);
	}
	
	/** Extends the original entity health component to add in support for damage multipliers */
	protected class EntityHumanoidHealth extends TraitHealth {
		
		public EntityHumanoidHealth(Entity entity) {
			super(entity);
		}

		@Override
		public float damage(DamageCause cause, EntityHitbox osef, float damage) {
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

			world.getSoundManager().playSoundEffect("sounds/entities/flesh.ogg", Mode.NORMAL, EntityHumanoid.this.getLocation(), (float) Math.random() * 0.4f + 0.4f, 1);

			return super.damage(cause, null, damage);
		}
	}

	protected class EntityHumanoidAnimatedSkeleton extends CompoundAnimationHelper {
		@Override
		public SkeletalAnimation getAnimationPlayingForBone(String boneName, double animationTime) {
			if (EntityHumanoid.this.entityHealth.isDead())
				return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/ded.bvh");

			if (Arrays.asList(new String[] { "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand" }).contains(boneName)) {
				SkeletalAnimation r = EntityHumanoid.this.traits.tryWith(TraitSelectedItem.class, ecs -> {
					ItemPile selectedItemPile = ecs.getSelectedItem();

					if (selectedItemPile != null) {
						// TODO refactor BVH subsystem to enable SkeletonAnimator to also take care of
						// additional transforms
						Item item = selectedItemPile.getItem();
						
						if(item instanceof ItemMiningTool) {
							MinerTrait trait = traits.get(MinerTrait.class);
							if(trait != null) {
								if(trait.getProgress() != null)
									return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/mining.bvh");
							}
						}

						if (item instanceof ItemCustomHoldingAnimation)
							return world.getGameContext().getContent().getAnimationsLibrary()
									.getAnimation(((ItemCustomHoldingAnimation) item).getCustomAnimationName());
						else
							return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/holding-item.bvh");
					}
					
					return null;
				});

				if (r != null)
					return r;
			}

			Vector3d vel = entityVelocity.getVelocity();

			// Extract just the horizontal speed from that
			double horizSpd = Math.sqrt(vel.x() * vel.x() + vel.z() * vel.z());

			if (stance.get() == EntityHumanoidStance.STANDING) {
				if (horizSpd > 0.065) {
					// System.out.println("running");
					return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/running.bvh");
				}
				if (horizSpd > 0.0)
					return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/walking.bvh");

				return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/standstill.bvh");
			} else if (stance.get() == EntityHumanoidStance.CROUCHING) {
				if (horizSpd > 0.0)
					return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/crouched-walking.bvh");

				return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/crouched.bvh");
			} else {
				return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/ded.bvh");
			}

		}

		public Matrix4f getBoneTransformationMatrix(String boneName, double animationTime) {
			Matrix4f characterRotationMatrix = new Matrix4f();
			// Only the torso is modified, the effect is replicated accross the other bones
			// later
			if (boneName.endsWith("boneTorso"))
				characterRotationMatrix.rotate((90 - entityRotation.getHorizontalRotation()) / 180f * 3.14159f, new Vector3f(0, 1, 0));
			
			Vector3d vel = entityVelocity.getVelocity();

			double horizSpd = Math.sqrt(vel.x() * vel.x() + vel.z() * vel.z());

			animationTime *= 0.75;

			if (boneName.endsWith("boneHead")) {
				Matrix4f modify = new Matrix4f(getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime));
				modify.rotate((float) (-EntityHumanoid.this.entityRotation.getVerticalRotation() / 180 * Math.PI), new Vector3f(0, 0, 1));
				return modify;
			}

			if (horizSpd > 0.030)
				animationTime *= 1.5;

			if (horizSpd > 0.060)
				animationTime *= 1.5;
			else if (Arrays.asList(new String[] { "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand", "boneTorso" }).contains(boneName)) {
				
				MinerTrait trait = traits.get(MinerTrait.class);
				if(trait != null && Arrays.asList(new String[] { "boneArmLU", "boneArmLD", "boneItemInHand"}).contains(boneName)) {
					MiningProgress miningProgress = trait.getProgress();
					if(miningProgress != null) {
						SkeletalAnimation lol = world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/mining.bvh");
						
						return characterRotationMatrix.mul(lol.getBone(boneName).getTransformationMatrix((System.currentTimeMillis() - miningProgress.started) * 1.5f));
					}
				}
				
				// Vector3d vel = getVelocityComponent().getVelocity();
				// double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() *
				// vel.getZ());

				// System.out.println((horizSpd / 0.065) * 0.3);
			}

			ItemPile selectedItem = EntityHumanoid.this.traits.tryWith(TraitSelectedItem.class, eci -> eci.getSelectedItem());

			if (Arrays.asList("boneArmLU", "boneArmRU").contains(boneName)) {
				float k = (stance.get() == EntityHumanoidStance.CROUCHING) ? 0.65f : 0.75f;

				if (selectedItem != null) {
					characterRotationMatrix.translate(new Vector3f(0f, k, 0));
					characterRotationMatrix.rotate(
							(entityRotation.getVerticalRotation() + ((stance.get() == EntityHumanoidStance.CROUCHING) ? -50f : 0f)) / 180f * -3.14159f,
							new Vector3f(0, 0, 1));
					characterRotationMatrix.translate(new Vector3f(0f, -k, 0));

					if (stance.get() == EntityHumanoidStance.CROUCHING
							&& EntityHumanoid.this.equals(((WorldClient) getWorld()).getClient().getPlayer().getControlledEntity()))
						characterRotationMatrix.translate(new Vector3f(-0.25f, -0.2f, 0f));

				}
			}

			if (boneName.equals("boneItemInHand") && selectedItem.getItem() instanceof ItemCustomHoldingAnimation) {
				animationTime = ((ItemCustomHoldingAnimation) selectedItem.getItem()).transformAnimationTime(animationTime);
			}

			return characterRotationMatrix.mul(getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime));
		}

		public boolean shouldHideBone(RenderingInterface renderingContext, String boneName) {
			if (EntityHumanoid.this.equals(((WorldClient) getWorld()).getClient().getPlayer().getControlledEntity())) {
				if (renderingContext.getCurrentPass().name.startsWith("shadow"))
					return false;

				ItemPile selectedItem = EntityHumanoid.this.traits.tryWith(TraitSelectedItem.class, eci -> eci.getSelectedItem());

				if (Arrays.asList("boneArmRU", "boneArmRD").contains(boneName) && selectedItem != null)
					if (selectedItem.getItem() instanceof ItemVoxel || selectedItem.getItem() instanceof ItemMiningTool)
						return true;

				if (Arrays.asList("boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD").contains(boneName) && selectedItem != null)
					return false;

				return true;
			}
			return false;
		}
	}

	protected static class EntityHumanoidRenderer<H extends EntityHumanoid> extends EntityRenderer<H> {
		void setupRender(RenderingInterface renderingContext) {
			// Player textures
			Texture2D playerTexture = renderingContext.textures().getTexture("./models/human/humanoid_test.png");
			playerTexture.setLinearFiltering(false);

			renderingContext.bindAlbedoTexture(playerTexture);

			renderingContext.textures().getTexture("./models/human/humanoid_normal.png").setLinearFiltering(false);

			renderingContext.bindNormalTexture(renderingContext.textures().getTexture("./textures/normalnormal.png"));
			renderingContext.bindMaterialTexture(renderingContext.textures().getTexture("./textures/defaultmaterial.png"));
		}

		@Override
		public int renderEntities(RenderingInterface renderer, RenderingIterator<H> renderableEntitiesIterator) {
			renderer.useShader("entities_animated");

			setupRender(renderer);

			int e = 0;

			for (EntityHumanoid entity : renderableEntitiesIterator.getElementsInFrustrumOnly()) {
				Location location = entity.getLocation();//entity.getPredictedLocation();

				if (renderer.getCurrentPass().name.startsWith("shadow") && location.distance(renderer.getCamera().getCameraPosition()) > 15f)
					continue;

				CellData cell = entity.getWorld().peekSafely(entity.getLocation());
				renderer.currentShader().setUniform2f("worldLightIn", cell.getBlocklight(), cell.getSunlight());

				TraitAnimated animation = entity.traits.get(TraitAnimated.class);
				((CachedLodSkeletonAnimator) animation.getAnimatedSkeleton()).lodUpdate(renderer);

				Matrix4f matrix = new Matrix4f();
				matrix.translate((float) location.x, (float) location.y, (float) location.z);
				renderer.setObjectMatrix(matrix);

				renderer.meshes().getRenderableAnimatableMesh("./models/human/human.dae").render(renderer, animation.getAnimatedSkeleton(),
						System.currentTimeMillis() % 1000000);
			}

			// Render items in hands
			for (EntityHumanoid entity : renderableEntitiesIterator) {

				if (renderer.getCurrentPass().name.startsWith("shadow") && entity.getLocation().distance(renderer.getCamera().getCameraPosition()) > 15f)
					continue;

				TraitAnimated animation = entity.traits.get(TraitAnimated.class);
				ItemPile selectedItemPile = entity.traits.tryWith(TraitSelectedItem.class, eci -> eci.getSelectedItem());

				if (selectedItemPile != null) {
					Matrix4f itemMatrix = new Matrix4f();
					itemMatrix.translate((float) entity.getLocation().x(), (float) entity.getLocation().y(),
							(float) entity.getLocation().z());

					itemMatrix.mul(animation.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000));
					// Matrix4f.mul(itemMatrix,
					// entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand",
					// System.currentTimeMillis() % 1000000), itemMatrix);

					selectedItemPile.getItem().getDefinition().getRenderer().renderItemInWorld(renderer, selectedItemPile, entity.world, entity.getLocation(),
							itemMatrix);
				}

				e++;
			}

			return e;
		}

		@Override
		public void freeRessources() {

		}

	}
}
