//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.entity.components.EntityComponentVelocity;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.physics.CollisionBox;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;

public class EntityGroundItem extends EntityBase implements EntityRenderable
{
	//protected float tilt = 0f;
	//protected float direction = 0f;
	protected float rotation = 0f;
	
	public final EntityComponentVelocity velocityComponent = new EntityComponentVelocity(this);
	
	private long spawnTime;
	private final EntityGroundItemPileComponent itemPileWithin;
	
	public EntityGroundItem(EntityDefinition t, Location location)
	{
		super(t, location);
		itemPileWithin = new EntityGroundItemPileComponent(this);
	}
	
	public EntityGroundItem(EntityDefinition t, Location location, ItemPile itemPile)
	{
		super(t, location);
		itemPileWithin = new EntityGroundItemPileComponent(this, itemPile);
		spawnTime = System.currentTimeMillis();
	}

	public ItemPile getItemPile()
	{
		return itemPileWithin.itemPile;
	}
	
	public void setItemPile(ItemPile itemPile)
	{
		itemPileWithin.setItemPile(itemPile);
		spawnTime = System.currentTimeMillis();
	}
	
	public boolean canBePickedUpYet()
	{
		return System.currentTimeMillis() - spawnTime > 2000L;
	}
	
	@Override
	public void tick()
	{
		//this.moveWithCollisionRestrain(0, -0.05, 0);
		Vector3d velocity = velocityComponent.getVelocity();

		if (world instanceof WorldMaster) {
			Voxel voxelIn = world.peekSafely(positionComponent.getLocation()).getVoxel();
			boolean inWater = voxelIn.getDefinition().isLiquid();

			double terminalVelocity = inWater ? -0.25 : -0.5;
			if (velocity.y() > terminalVelocity && !this.isOnGround())
				velocity.y = (velocity.y() - 0.016);
			if (velocity.y() < terminalVelocity)
				velocity.y = (terminalVelocity);

			Vector3dc remainingToMove = moveWithCollisionRestrain(velocity.x(), velocity.y(), velocity.z());
			if (remainingToMove.y() < -0.02 && this.isOnGround()) {
				if (remainingToMove.y() < -0.01) {
					//Bounce
					double originalDownardsVelocity = velocity.y();
					double bounceFactor = 0.15;
					velocity.mul(bounceFactor);
					velocity.y = (-originalDownardsVelocity * bounceFactor);
					
					//world.getSoundManager().playSoundEffect("./sounds/dogez/weapon/grenades/grenade_bounce.ogg", Mode.NORMAL, getLocation(), 1, 1, 10, 35);
				} else
					velocity.mul(0d);
			}

			if (Math.abs(velocity.x()) < 0.02)
				velocity.x = (0.0);
			if (Math.abs(velocity.z()) < 0.02)
				velocity.z = (0.0);
			
			if (Math.abs(velocity.y()) < 0.01)
				velocity.y = (0.0);

			velocityComponent.setVelocity(velocity);
		}

		if (world instanceof WorldClient) {
			
			if(this.isOnGround())
			{
				rotation += 1.0f;
				rotation %= 360;
			}
		}
		
		super.tick();
	}
	
	static EntityRenderer<EntityGroundItem> entityRenderer = new EntityGroundItemRenderer();
	
	static class EntityGroundItemRenderer implements EntityRenderer<EntityGroundItem> {
		
		@Override
		public int renderEntities(RenderingInterface renderer, RenderingIterator<EntityGroundItem> renderableEntitiesIterator)
		{
			int i = 0;
			
			while(renderableEntitiesIterator.hasNext())
			{
				EntityGroundItem e = renderableEntitiesIterator.next();
				
				ItemPile within = e.itemPileWithin.getItemPile();
				if(within != null)
				{
					CellData cell = e.getWorld().peekSafely(e.getLocation());
					renderer.currentShader().setUniform2f("worldLightIn", cell.getBlocklight(), cell.getSunlight());
					
					Matrix4f matrix = new Matrix4f();
					
					Vector3d loc = e.getLocation().add(0.0, 0.25, 0.0);
					matrix.translate((float)loc.x, (float)(loc.y + Math.sin(Math.PI/180*e.rotation * 2) * 0.125 + 0.25), (float)loc.z);
					//matrix.rotate((float)Math.PI/2, new Vector3f(1,0 ,0));
					matrix.rotate((float)Math.PI/180*e.rotation, new Vector3f(0, 1, 0));
					within.getItem().getDefinition().getRenderer().renderItemInWorld(renderer, within, e.getWorld(), e.getLocation(), matrix);
					//renderingInterface.flush();
				}
				else
				{
					System.out.println("EntityGroundItem: Not within any inventory ???");
				}
				
				i++;
			}
			
			return i;
		}

		@Override
		public void freeRessources()
		{
			//Not much either
		}
		
	}
	
	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return entityRenderer;
	}

	@Override
	public CollisionBox getBoundingBox() {
		return new CollisionBox(0.5, 0.75, 0.5).translate(-0.25, 0.0, -0.25);
	}
	
	
}
