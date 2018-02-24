//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.entity.components.EntityComponentRotation;
import io.xol.chunkstories.api.entity.components.EntityComponentVelocity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.entity.interfaces.EntityWithVelocity;
import io.xol.chunkstories.api.events.entity.EntityDamageEvent;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.core.entity.components.EntityComponentHealth;

public abstract class EntityLivingImplementation extends EntityBase implements EntityLiving, EntityWithVelocity
{
	//Head/body rotation
	final protected EntityComponentRotation entityRotationComponent;
	final protected EntityComponentVelocity velocityComponent;

	//Movement stuff
	public Vector3d acceleration = new Vector3d();

	//Damage/health stuff
	private EntityComponentHealth entityHealthComponent;
	private long damageCooldown = 0;
	private DamageCause lastDamageCause;
	long deathDespawnTimer = 6000;

	protected SkeletonAnimator animatedSkeleton;

	protected double lastStandingHeight = Double.NaN;
	protected boolean wasStandingLastTick = true;

	public EntityLivingImplementation(EntityDefinition t, Location location)
	{
		super(t, location);

		velocityComponent = new EntityComponentVelocity(this);
		
		entityRotationComponent = new EntityComponentRotation(this, this.getComponents().getLastComponent());
		entityHealthComponent = new EntityComponentHealth(this, getStartHealth());
	}
	
	public EntityComponentVelocity getVelocityComponent() {
		return velocityComponent;
	}
	
	@Override
	/** Teleports reset the last standing height, meanining you can't take fall damage from a teleport */
	public void setLocation(Location loc)
	{
		super.setLocation(loc);
		lastStandingHeight = Double.NaN;
	}

	@Override
	public SkeletonAnimator getAnimatedSkeleton()
	{
		return animatedSkeleton;
	}

	@Override
	public float getMaxHealth()
	{
		return 100;
	}

	@Override
	public float getStartHealth()
	{
		return getMaxHealth();
	}

	@Override
	public void setHealth(float health)
	{
		entityHealthComponent.setHealth(health);
	}

	public float getHealth()
	{
		return entityHealthComponent.getHealth();
	}

	@Override
	public float damage(DamageCause cause, float damage)
	{
		return damage(cause, null, damage);
	}

	@Override
	public float damage(DamageCause cause, HitBox osef, float damage)
	{
		if (damageCooldown > System.currentTimeMillis())
			return 0f;

		EntityDamageEvent event = new EntityDamageEvent(this, cause, damage);
		this.getWorld().getGameLogic().getPluginsManager().fireEvent(event);

		if (!event.isCancelled())
		{
			entityHealthComponent.damage(event.getDamageDealt());
			lastDamageCause = cause;

			damageCooldown = System.currentTimeMillis() + cause.getCooldownInMs();

			float damageDealt = event.getDamageDealt();

			//Applies knockback
			if (cause instanceof Entity)
			{
				Entity attacker = (Entity) cause;
				Vector3d attackKnockback = this.getLocation().sub(attacker.getLocation().add(0d, 0d, 0d));
				attackKnockback.y = (0d);
				attackKnockback.normalize();
				
				float knockback = (float) Math.max(1f, Math.pow(damageDealt, 0.5f));
				
				attackKnockback.mul(knockback / 50d);
				attackKnockback.y = (knockback / 50d);
				/*
				attackKnockback.scale(damageDealt / 500d);
				attackKnockback.scale(1.0 / (1.0 + 5 * this.getVelocityComponent().getVelocity().length()));*/

				//.scale(1/60d).scale(damageDealt / 10f);
				this.getVelocityComponent().addVelocity(attackKnockback);
			}

			return damageDealt;
		}

		return 0f;
	}

	@Override
	public void tick()
	{
		if (getWorld() == null)
			return;
		
		//Despawn counter is strictly a client matter
		if (getWorld() instanceof WorldMaster)
		{
			if (isDead())
			{
				deathDespawnTimer--;
				if (deathDespawnTimer < 0)
				{
					world.removeEntity(this);
					return;
				}
			}
			
			//Fall damage
			if(isOnGround())
			{
				if(!wasStandingLastTick && !Double.isNaN(lastStandingHeight))
				{
					double fallDistance = lastStandingHeight - this.positionComponent.getLocation().y();
					if(fallDistance > 0)
					{
						//System.out.println("Fell "+fallDistance+" meters");
						if(fallDistance > 5)
						{
							float fallDamage = (float) (fallDistance * fallDistance / 2);
							System.out.println(this + "Took "+fallDamage+" hp of fall damage");
							this.damage(DAMAGE_CAUSE_FALL, fallDamage);
						}
					}
				}
				lastStandingHeight = this.positionComponent.getLocation().y();
			}
			this.wasStandingLastTick = isOnGround();
		}

		boolean shouldDoTick = false;
		if (this instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) this).getControllerComponent().getController();
			if (controller == null)
				shouldDoTick = (getWorld() instanceof WorldMaster);
			else if (getWorld() instanceof WorldClient && ((WorldClient)getWorld()).getClient().getPlayer().equals(controller))
				shouldDoTick = true;

		}
		else
			shouldDoTick = (getWorld() instanceof WorldMaster);

		if (shouldDoTick)
		{
			Vector3dc ogVelocity = getVelocityComponent().getVelocity();
			Vector3d velocity = new Vector3d(ogVelocity);
			
			Vector2f headRotationVelocity = this.getEntityRotationComponent().tickInpulse();
			getEntityRotationComponent().addRotation(headRotationVelocity.x(), headRotationVelocity.y());

			//voxelIn = VoxelsStore.get().getVoxelById(VoxelFormat.id(world.getVoxelData(positionComponent.getLocation())));
			boolean inWater = isInWater(); //voxelIn.getType().isLiquid();

			// Gravity
			if (!(this instanceof EntityFlying && ((EntityFlying) this).getFlyingComponent().get()))
			{
				double terminalVelocity = inWater ? -0.05 : -0.5;
				if (velocity.y() > terminalVelocity)
					velocity.y = (velocity.y() - 0.008);
				if (velocity.y() < terminalVelocity)
					velocity.y = (terminalVelocity);

				//Water limits your overall movement
				double targetSpeedInWater = 0.02;
				if (inWater)
				{
					if (velocity.length() > targetSpeedInWater)
					{
						double decelerationThen = Math.pow((velocity.length() - targetSpeedInWater), 1.0);

						//System.out.println(decelerationThen);
						double maxDeceleration = 0.006;
						if (decelerationThen > maxDeceleration)
							decelerationThen = maxDeceleration;

						//System.out.println(decelerationThen);

						acceleration.add(new Vector3d(velocity).normalize().negate().mul(decelerationThen));
						//acceleration.add(0.0, decelerationThen * (velocity.y() > 0.0 ? 1.0 : -1.0), 0.0);
					}
				}
			}

			// Acceleration
			velocity.x = (velocity.x() + acceleration.x());
			velocity.y = (velocity.y() + acceleration.y());
			velocity.z = (velocity.z() + acceleration.z());

			//TODO ugly
			if (!world.isChunkLoaded((int) (double) positionComponent.getLocation().x() / 32, (int) (double) positionComponent.getLocation().y() / 32, (int) (double) positionComponent.getLocation().z() / 32))
			{
				velocity.set(0d, 0d, 0d);
			}

			//Eventually moves
			Vector3dc remainingToMove = moveWithCollisionRestrain(velocity.x(), velocity.y(), velocity.z());
			Vector2d remaining2d = new Vector2d(remainingToMove.x(), remainingToMove.z());

			//Auto-step logic
			if (remaining2d.length() > 0.001 && isOnGround())
			{
				//Cap max speed we can get through the bump ?
				if (remaining2d.length() > 0.20d)
				{
					System.out.println("Too fast, capping");
					remaining2d.normalize();
					remaining2d.mul(0.20);
				}

				//Get whatever we are colliding with

				//Test if setting yourself on top would be ok

				//Do it if possible

				//TODO remake proper
				Vector3d blockedMomentum = new Vector3d(remaining2d.x(), 0, remaining2d.y());
				for (double d = 0.25; d < 0.5; d += 0.05)
				{
					//I don't want any of this to reflect on the object, because it causes ugly jumps in the animation
					Vector3dc canMoveUp = this.canMoveWithCollisionRestrain(new Vector3d(0.0, d, 0.0));
					//It can go up that bit
					if (canMoveUp.length() == 0.0f)
					{
						//Would it help with being stuck ?
						Vector3d tryFromHigher = new Vector3d(this.getLocation());
						tryFromHigher.add(new Vector3d(0.0, d, 0.0));
						Vector3dc blockedMomentumRemaining = this.canMoveWithCollisionRestrain(tryFromHigher, blockedMomentum);
						//If length of remaining momentum < of what we requested it to do, that means it *did* go a bit further away
						if (blockedMomentumRemaining.length() < blockedMomentum.length())
						{
							//Where would this land ?
							Vector3d afterJump = new Vector3d(tryFromHigher);
							afterJump.add(blockedMomentum);
							afterJump.sub(blockedMomentumRemaining);

							//land distance = whatever is left of our -0.55 delta when it hits the ground
							Vector3dc landDistance = this.canMoveWithCollisionRestrain(afterJump, new Vector3d(0.0, -d, 0.0));
							afterJump.add(new Vector3d(0.0, -d, 0.0));
							afterJump.sub(landDistance);

							this.setLocation(new Location(world, afterJump));

							remaining2d = new Vector2d(blockedMomentumRemaining.x(), blockedMomentumRemaining.z());
							break;
						}
					}
				}
			}

			//Collisions, snap to axises
			if (Math.abs(remaining2d.x()) >= 0.001d)
				velocity.x = (0d);
			if (Math.abs(remaining2d.y()) >= 0.001d)
				velocity.z = (0d);
			// Stap it
			if (isOnGround() && velocity.y() < 0)
				velocity.y = (0d);
			else if (isOnGround())
				velocity.y = (0d);

			getVelocityComponent().setVelocity(velocity);
		}
	}

	public boolean isInWater() {
		for(CellData cell : world.getVoxelsWithin(this.getTranslatedBoundingBox())) {
			if(cell.getVoxel().getDefinition().isLiquid())
				return true;
		}
		return false;
	}

	@Override
	public boolean isDead()
	{
		return getHealth() <= 0;
	}

	public EntityComponentRotation getEntityRotationComponent()
	{
		return entityRotationComponent;
	}

	public Vector3dc getDirectionLookingAt()
	{
		return getEntityRotationComponent().getDirectionLookingAt();
	}

	@Override
	public DamageCause getLastDamageCause()
	{
		return lastDamageCause;
	}

	@Override
	public String getName()
	{
		return this.getClass().getSimpleName();
	}
}
