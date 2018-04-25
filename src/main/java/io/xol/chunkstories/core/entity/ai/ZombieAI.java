//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity.ai;

import java.util.Collection;

import org.joml.Vector3d;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.ai.AI;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.core.entity.EntityHumanoid;
import io.xol.chunkstories.core.entity.EntityLiving;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.entity.EntityZombie;
import io.xol.chunkstories.core.entity.components.EntityStance.EntityHumanoidStance;

public class ZombieAI extends GenericHumanoidAI
{
	Collection<Class<? extends Entity>> targetsTypes;

	EntityZombie entity;
	
	//double aggroRadius;
	int attackEntityCooldown = 60 * 5;

	public ZombieAI(EntityZombie entity, Collection<Class<? extends Entity>> targetsTypes)
	{
		super(entity);
		this.entity = entity;
		//this.currentTask = new AiTaskLookArroundAndSearchTarget(aggroRadius);
		this.targetsTypes = targetsTypes;
	}

	public void tick()
	{
		super.tick();

		if(entity.isDead())
			return;
		
		if (attackEntityCooldown > 0)
			attackEntityCooldown--;

		//Find entities to attack
		if (!(this.currentTask instanceof AiTaskAttackEntity) && entity.stage().aggroRadius > 0.0 && attackEntityCooldown == 0)
		{
			//Only look for them once in 2s
			attackEntityCooldown = (int) (Math.random() * 60 * 2);

			for (Entity entityToLook : entity.getWorld().getEntitiesInBox(entity.getLocation(), new Vector3d(entity.stage().aggroRadius * 2f)))
			{
				float visibilityModifier = 1f;
				if(entityToLook instanceof EntityPlayer) {

					EntityPlayer player = (EntityPlayer)entityToLook;
					
					//Crouched players are 70% less visible
					if(player.stance.get().equals(EntityHumanoidStance.CROUCHING))
						visibilityModifier -= 0.7f;
				
					//If the entity is sprinting
					if (player.getVelocityComponent().getVelocity().length() > 0.7)
						visibilityModifier += 1.0f;
				
				}
				
				if (!entityToLook.equals(entity) && entityToLook.getLocation().distance(entity.getLocation()) * visibilityModifier <= entity.stage().aggroRadius && entityToLook instanceof EntityHumanoid && !((EntityHumanoid) entityToLook).isDead())
				{
					//Check target is in set
					if (targetsTypes.contains(entityToLook.getClass()))
					{
						//Play a borking sound
						entity.getWorld().getSoundManager().playSoundEffect("sounds/entities/zombie/grunt.ogg", Mode.NORMAL, entity.getLocation(), (float) (1.5 + Math.random() * 0.2), 1.5f);//.setPitch();
						entity.getWorld().getSoundManager().playSoundEffect("sounds/entities/zombie/grunt.ogg", Mode.NORMAL, entity.getLocation(), (float) (1.5 + Math.random() * 0.2), 1.5f);//.setPitch();
						
						//Set new task
						setAiTask(new AiTaskAttackEntity((EntityHumanoid) entityToLook, 10f, 15f, currentTask, entity.stage().attackCooldown, entity.stage().attackDamage));
						return;
					}
				}
			}

		}
	}

	public class AiTaskAttackEntity extends AiTaskGoAtEntity
	{
		final long attackCooldownMS;
		final float damage;
		
		final float giveupDistance;

		long lastAttackMS = 0;

		public AiTaskAttackEntity(EntityLiving entity, float giveupDistance, float initGiveupDistance, AI<EntityHumanoid>.AiTask previousTask, long attackCooldownMS, float damage)
		{
			super(entity, initGiveupDistance, previousTask);

			this.giveupDistance = giveupDistance;
			this.attackCooldownMS = attackCooldownMS;
			this.damage = damage;

			this.entitySpeed = ZombieAI.this.entity.stage().speed;
		}

		@Override
		public void execute()
		{
			super.execute();

			float distance = (float)(double) this.entityFollowed.getLocation().distance(entity.getLocation());
			
			//Within the final give up distance ? Set the give up distance to be at that from then on
			if(giveupDistance - distance > 1)
			{
				this.maxDistance = giveupDistance;
			}
			
			if (distance < 1.5)
			{
				if (System.currentTimeMillis() - lastAttackMS > attackCooldownMS)
				{
					//System.out.println("Attacking");
					entityFollowed.damage(entity, damage);
					lastAttackMS = System.currentTimeMillis();
				}
			}
		}
	}
}
