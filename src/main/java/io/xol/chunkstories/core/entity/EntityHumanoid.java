//
// This file is a part of the Chunk Stories API codebase
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
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemVoxel;
import io.xol.chunkstories.api.item.interfaces.ItemCustomHoldingAnimation;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.materials.VoxelMaterial;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.core.entity.components.EntityComponentStance;

public abstract class EntityHumanoid extends EntityLivingImplementation
{
	double jumpForce = 0;
	protected Vector3d targetVelocity = new Vector3d(0);

	boolean justJumped = false;
	boolean justLanded = false;

	boolean running = false;

	//public double maxSpeedRunning = 0.25;
	//public double maxSpeed = 0.15;

	public double horizontalSpeed = 0;
	public double metersWalked = 0d;

	public double eyePosition = 1.65;

	CachedLodSkeletonAnimator cachedSkeleton;
	
	public final EntityComponentStance stance;

	public EntityHumanoid(EntityDefinition t, Location location)
	{
		super(t, location);

		stance = new EntityComponentStance(this);
		
		cachedSkeleton = new CachedLodSkeletonAnimator(this, new EntityHumanoidAnimatedSkeleton(), 25f, 75f);
		animatedSkeleton = cachedSkeleton;
	}

	protected class EntityHumanoidAnimatedSkeleton extends CompoundAnimationHelper
	{
		@Override
		public SkeletalAnimation getAnimationPlayingForBone(String boneName, double animationTime)
		{
			if (EntityHumanoid.this.isDead())
				return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/ded.bvh");

			if (Arrays.asList(new String[] { "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand" }).contains(boneName))
			{
				if (EntityHumanoid.this instanceof EntityWithSelectedItem)
				{
					ItemPile selectedItemPile = ((EntityWithSelectedItem) EntityHumanoid.this).getSelectedItem();

					if (selectedItemPile != null)
					{
						//TODO refactor BVH subsystem to enable SkeletonAnimator to also take care of additional transforms
						Item item = selectedItemPile.getItem();
						
						if (item instanceof ItemCustomHoldingAnimation)
							return world.getGameContext().getContent().getAnimationsLibrary().getAnimation(((ItemCustomHoldingAnimation)item).getCustomAnimationName());
						else
							return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/holding-item.bvh");
					}
				}
			}

			Vector3d vel = getVelocityComponent().getVelocity();

			double horizSpd = Math.sqrt(vel.x() * vel.x() + vel.z() * vel.z());

			if(stance.get() == EntityHumanoidStance.STANDING)
			{
				if (horizSpd > 0.065)
				{
					//System.out.println("running");
					return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/running.bvh");
				}
				if (horizSpd > 0.0)
					return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/walking.bvh");
			
				return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/standstill.bvh");
			}
			else if(stance.get() == EntityHumanoidStance.CROUCHING)
			{
				if (horizSpd > 0.0)
					return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/crouched-walking.bvh");
				
				return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/crouched.bvh");
			}
			else
			{
				return world.getGameContext().getContent().getAnimationsLibrary().getAnimation("./animations/human/ded.bvh");
			}
			
		}

		public Matrix4f getBoneTransformationMatrix(String boneName, double animationTime)
		{
			Vector3d vel = getVelocityComponent().getVelocity();

			double horizSpd = Math.sqrt(vel.x() * vel.x() + vel.z() * vel.z());

			animationTime *= 0.75;

			// animationTime += metersWalked * 50;
			//	return BVHLibrary.getAnimation("res/animations/human/running.bvh");

			if (boneName.endsWith("boneHead"))
			{
				Matrix4f modify = new Matrix4f(getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime));
				modify.rotate((float) (-EntityHumanoid.this.getEntityRotationComponent().getVerticalRotation() / 180 * Math.PI), new Vector3f(0, 0, 1));
				return modify;
			}

			if (horizSpd > 0.030)
				animationTime *= 1.5;

			if (horizSpd > 0.060)
				animationTime *= 1.5;
			else if (Arrays.asList(new String[] { "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand", "boneTorso" }).contains(boneName))
			{
				//Vector3d vel = getVelocityComponent().getVelocity();
				//double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

				//System.out.println((horizSpd / 0.065) * 0.3);
			}

			Matrix4f characterRotationMatrix = new Matrix4f();
			//Only the torso is modified, the effect is replicated accross the other bones later
			if (boneName.endsWith("boneTorso"))
				characterRotationMatrix.rotate((90 - getEntityRotationComponent().getHorizontalRotation()) / 180f * 3.14159f, new Vector3f(0, 1, 0));

			ItemPile selectedItem = null;

			if (EntityHumanoid.this instanceof EntityWithSelectedItem)
				selectedItem = ((EntityWithSelectedItem) EntityHumanoid.this).getSelectedItem();

			if (Arrays.asList("boneArmLU", "boneArmRU").contains(boneName))
			{
				float k = (stance.get() == EntityHumanoidStance.CROUCHING) ? 0.65f : 0.75f;

				if (selectedItem != null)
				{
					characterRotationMatrix.translate(new Vector3f(0f, k, 0));
					characterRotationMatrix.rotate((getEntityRotationComponent().getVerticalRotation() + ((stance.get() == EntityHumanoidStance.CROUCHING) ? -50f : 0f)) / 180f * -3.14159f, new Vector3f(0, 0, 1));
					characterRotationMatrix.translate(new Vector3f(0f, -k, 0));
					
					
					
					if(stance.get() == EntityHumanoidStance.CROUCHING && EntityHumanoid.this.equals(((WorldClient)getWorld()).getClient().getPlayer().getControlledEntity()))
						characterRotationMatrix.translate(new Vector3f(-0.25f, -0.2f, 0f));
					
				}
			}
			
			if(boneName.equals("boneItemInHand") && selectedItem.getItem() instanceof ItemCustomHoldingAnimation) {
				animationTime = ((ItemCustomHoldingAnimation) selectedItem.getItem()).transformAnimationTime(animationTime);
			}

			return characterRotationMatrix.mul(getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime));
		}

		public boolean shouldHideBone(RenderingInterface renderingContext, String boneName)
		{
			if (EntityHumanoid.this.equals(((WorldClient)getWorld()).getClient().getPlayer().getControlledEntity()))
			{
				if (renderingContext.getCurrentPass().name.startsWith("shadow"))
					return false;

				ItemPile selectedItem = null;

				if (EntityHumanoid.this instanceof EntityWithSelectedItem)
					selectedItem = ((EntityWithSelectedItem) EntityHumanoid.this).getSelectedItem();

				if (Arrays.asList("boneArmRU", "boneArmRD").contains(boneName) && selectedItem != null)
					if (selectedItem.getItem() instanceof ItemVoxel)
						return true;

				if (Arrays.asList("boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD").contains(boneName) && selectedItem != null)
					return false;

				return true;
			}
			return false;
		}
	}

	protected class EntityHumanoidRenderer<H extends EntityHumanoid> extends EntityRenderer<H>
	{
		void setupRender(RenderingInterface renderingContext)
		{
			//Player textures
			Texture2D playerTexture = renderingContext.textures().getTexture("./models/humanoid_test.png");
			playerTexture.setLinearFiltering(false);

			renderingContext.bindAlbedoTexture(playerTexture);

			renderingContext.textures().getTexture("./models/humanoid_normal.png").setLinearFiltering(false);

			renderingContext.bindAlbedoTexture(renderingContext.textures().getTexture("./models/humanoid_test.png"));
			renderingContext.bindNormalTexture(renderingContext.textures().getTexture("./textures/normalnormal.png"));
			renderingContext.bindMaterialTexture(renderingContext.textures().getTexture("./textures/defaultmaterial.png"));
		}

		@Override
		public int renderEntities(RenderingInterface renderer, RenderingIterator<H> renderableEntitiesIterator)
		{
			renderer.useShader("entities_animated");
			
			setupRender(renderer);
			
			int e = 0;

			for (EntityHumanoid entity : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				Location location = entity.getPredictedLocation();

				if (renderer.getCurrentPass().name.startsWith("shadow") && location.distance(renderer.getCamera().getCameraPosition()) > 15f)
					continue;

				CellData cell = entity.getWorld().peekSafely(entity.getLocation());
				renderer.currentShader().setUniform2f("worldLightIn", cell.getBlocklight(), cell.getSunlight());
				
				entity.cachedSkeleton.lodUpdate(renderer);

				Matrix4f matrix = new Matrix4f();
				matrix.translate((float)location.x, (float)location.y, (float)location.z);
				renderer.setObjectMatrix(matrix);

				renderer.meshes().getRenderableMultiPartAnimatableMeshByName("./models/human.obj").render(renderer, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);
				
				renderer.bindAlbedoTexture(renderer.textures().getTexture("./textures/armor/isis.png"));
				renderer.textures().getTexture("./textures/armor/isis.png").setLinearFiltering(false);
				renderer.meshes().getRenderableMultiPartAnimatableMeshByName("./models/human_overlay.obj").render(renderer, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);
			}
			
			//Render items in hands
			for (EntityHumanoid entity : renderableEntitiesIterator)
			{

				if (renderer.getCurrentPass().name.startsWith("shadow") && entity.getLocation().distance(renderer.getCamera().getCameraPosition()) > 15f)
					continue;

				ItemPile selectedItemPile = null;

				if (entity instanceof EntityWithSelectedItem)
					selectedItemPile = ((EntityWithSelectedItem) entity).getSelectedItem();

				if (selectedItemPile != null)
				{
					Matrix4f itemMatrix = new Matrix4f();
					itemMatrix.translate((float)entity.getPredictedLocation().x(), (float)entity.getPredictedLocation().y(), (float)entity.getPredictedLocation().z());

					itemMatrix.mul(entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000));
					//Matrix4f.mul(itemMatrix, entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000), itemMatrix);

					selectedItemPile.getItem().getDefinition().getRenderer().renderItemInWorld(renderer, selectedItemPile, world, entity.getLocation(), itemMatrix);
				}

				e++;
			}
			
			return e;
		}

		@Override
		public void freeRessources()
		{

		}

	}

	public enum EntityHumanoidStance {
		STANDING,
		CROUCHING,
	}
	
	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return new EntityHumanoidRenderer<EntityHumanoid>();
	}

	public Vector3d getTargetVelocity()
	{
		return targetVelocity;
	}

	@Override
	public void tick()
	{
		eyePosition = stance.get() == EntityHumanoidStance.CROUCHING ? 1.15 : 1.65;
		
		//Only  if we are allowed to
		boolean tick = false;
		if (this instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) this).getControllerComponent().getController();
			if (controller == null)
				tick = (getWorld() instanceof WorldMaster);
			else if (getWorld() instanceof WorldClient && ((WorldClient)getWorld()).getClient().getPlayer().equals(controller))
				tick = true;

		}
		else
			tick = (getWorld() instanceof WorldMaster);

		if (tick)
		{
			//The actual moment the jump takes effect
			boolean inWater = isInWater(); //voxelIn != null && voxelIn.getType().isLiquid();
			
			if (jumpForce > 0.0 && (!justJumped || inWater))
			{
				//Set the velocity
				getVelocityComponent().setVelocityY(jumpForce);
				justJumped = true;
				metersWalked = 0.0;
				jumpForce = 0.0;
			}

			//Set acceleration vector to wanted speed - actual speed
			if(isDead())
				targetVelocity = new Vector3d(0.0);
			acceleration = new Vector3d(targetVelocity.x() - getVelocityComponent().getVelocity().x(), 0, targetVelocity.z() - getVelocityComponent().getVelocity().z());

			//Limit maximal acceleration depending if we're on the groud or not, we accelerate 2x faster on ground
			double maxAcceleration = isOnGround() ? 0.010 : 0.005;
			if (inWater)
				maxAcceleration = 0.005;
			if (acceleration.length() > maxAcceleration)
			{
				acceleration.normalize();
				acceleration.mul(maxAcceleration);
			}
		}

		//Plays the walking sounds
		handleWalkingEtcSounds();

		//Tick : will move the entity, solve velocity/acceleration and so on
		super.tick();
	}

	boolean lastTickOnGround = false;

	public void tickClientPrediction()
	{
		handleWalkingEtcSounds();
	}

	protected void handleWalkingEtcSounds()
	{
		//This is strictly a clientside hack
		if (!(getWorld() instanceof WorldClient))
			return;

		//When the entities are too far from the player, don't play any sounds
		if (((WorldClient)getWorld()).getClient().getPlayer().getControlledEntity() != null)
			if (((WorldClient)getWorld()).getClient().getPlayer().getControlledEntity().getLocation().distance(this.getLocation()) > 25f)
				return;

		// Sound stuff
		if (isOnGround() && !lastTickOnGround)
		{
			justLanded = true;
			metersWalked = 0.0;
		}

		//Used to trigger landing sound
		lastTickOnGround = this.isOnGround();

		//Bobbing
		Vector3d horizontalSpeed = new Vector3d(this.getVelocityComponent().getVelocity());
		horizontalSpeed.y = 0d;

		if (isOnGround())
			metersWalked += Math.abs(horizontalSpeed.length());

		boolean inWater = isInWater();

		Voxel voxelStandingOn = world.peekSafely(new Vector3d(this.getLocation()).add(0.0, -0.01, 0.0)).getVoxel();

		if (voxelStandingOn == null || !voxelStandingOn.getDefinition().isSolid() && !voxelStandingOn.getDefinition().isLiquid())
			return;

		VoxelMaterial material = voxelStandingOn.getMaterial();

		if (justJumped && !inWater)
		{
			justJumped = false;
			getWorld().getSoundManager()
					.playSoundEffect(material.resolveProperty("jumpingSounds"), Mode.NORMAL, getLocation(),
							(float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().x() * getVelocityComponent().getVelocity().x() + getVelocityComponent().getVelocity().z() * getVelocityComponent().getVelocity().z()) * 0.1f), 1f)
					.setAttenuationEnd(10);
		}
		if (justLanded)
		{
			justLanded = false;
			getWorld().getSoundManager()
					.playSoundEffect(material.resolveProperty("landingSounds"), Mode.NORMAL, getLocation(),
							(float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().x() * getVelocityComponent().getVelocity().x() + getVelocityComponent().getVelocity().z() * getVelocityComponent().getVelocity().z()) * 0.1f), 1f)
					.setAttenuationEnd(10);
		}

		if (metersWalked > 0.2 * Math.PI * 2)
		{
			metersWalked %= 0.2 * Math.PI * 2;
			if (horizontalSpeed.length() <= 0.06)
				getWorld().getSoundManager()
						.playSoundEffect(material.resolveProperty("walkingSounds"), Mode.NORMAL, getLocation(),
								(float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().x() * getVelocityComponent().getVelocity().x() + getVelocityComponent().getVelocity().z() * getVelocityComponent().getVelocity().z()) * 0.1f),
								1f)
						.setAttenuationEnd(10);
			else
				getWorld().getSoundManager()
						.playSoundEffect(material.resolveProperty("runningSounds"), Mode.NORMAL, getLocation(),
								(float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().x() * getVelocityComponent().getVelocity().x() + getVelocityComponent().getVelocity().z() * getVelocityComponent().getVelocity().z()) * 0.1f),
								1f)
						.setAttenuationEnd(10);

		}
	}

	@Override
	public CollisionBox getBoundingBox()
	{
		if (isDead())
			return new CollisionBox(1.6, 1.0, 1.6).translate(-0.8, 0.0, -0.8);
		//Have it centered
		return new CollisionBox(1.0, stance.get() == EntityHumanoidStance.CROUCHING ? 1.5 : 2.0, 1.0).translate(-0.5, 0.0, -0.5);
	}

	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[] { new CollisionBox(0.6, stance.get() == EntityHumanoidStance.CROUCHING ? 1.45 : 1.9, 0.6).translate(-0.3, 0.0, -0.3) };
	}

	HitBoxImpl[] hitboxes = { new HitBoxImpl(this, new CollisionBox(-0.15, 0.0, -0.25, 0.30, 0.675, 0.5), "boneTorso"), new HitBoxImpl(this, new CollisionBox(-0.25, 0.0, -0.25, 0.5, 0.5, 0.5), "boneHead"),
			new HitBoxImpl(this, new CollisionBox(-0.1, -0.375, -0.1, 0.2, 0.375, 0.2), "boneArmRU"), new HitBoxImpl(this, new CollisionBox(-0.1, -0.375, -0.1, 0.2, 0.375, 0.2), "boneArmLU"),
			new HitBoxImpl(this, new CollisionBox(-0.1, -0.3, -0.1, 0.2, 0.3, 0.2), "boneArmRD"), new HitBoxImpl(this, new CollisionBox(-0.1, -0.3, -0.1, 0.2, 0.3, 0.2), "boneArmLD"),
			new HitBoxImpl(this, new CollisionBox(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegRU"), new HitBoxImpl(this, new CollisionBox(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegLU"),
			new HitBoxImpl(this, new CollisionBox(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegRD"), new HitBoxImpl(this, new CollisionBox(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegLD"),
			new HitBoxImpl(this, new CollisionBox(-0.15, -0.075, -0.125, 0.35, 0.075, 0.25), "boneFootL"), new HitBoxImpl(this, new CollisionBox(-0.15, -0.075, -0.125, 0.35, 0.075, 0.25), "boneFootR"), };

	@Override
	public HitBoxImpl[] getHitBoxes()
	{
		return hitboxes;
	}

	@Override
	public float damage(DamageCause cause, HitBox osef, float damage)
	{
		if (osef != null)
		{
			if (osef.getName().equals("boneHead"))
				damage *= 2.8f;
			else if(osef.getName().contains("Arm"))
				damage *= 0.75;
			else if(osef.getName().contains("Leg"))
				damage *= 0.5;
			else if(osef.getName().contains("Foot"))
				damage *= 0.25;
		}
		
		damage *= 0.5;

		world.getSoundManager().playSoundEffect("sounds/entities/flesh.ogg", Mode.NORMAL, this.getLocation(), (float)Math.random() * 0.4f + 0.4f, 1);
		
		return super.damage(cause, null, damage);
	}

	public Location getPredictedLocation()
	{
		return getLocation();
	}
}
