//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.entity.components.EntityComponentController;
import io.xol.chunkstories.api.entity.components.EntityComponentCreativeMode;
import io.xol.chunkstories.api.entity.components.EntityComponentFlying;
import io.xol.chunkstories.api.entity.components.EntityComponentInventory;
import io.xol.chunkstories.api.entity.components.EntityComponentName;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityFeedable;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.entity.interfaces.EntityOverlay;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.entity.interfaces.EntityWorldModifier;
import io.xol.chunkstories.api.events.player.voxel.PlayerVoxelModificationEvent;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.ItemVoxel;
import io.xol.chunkstories.api.item.interfaces.ItemOverlay;
import io.xol.chunkstories.api.item.interfaces.ItemZoom;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.math.Math2;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.world.WorldRenderer.RenderingPass;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.util.ColorsTools;
import io.xol.chunkstories.api.world.World.WorldCell;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.core.entity.EntityArmorInventory.EntityWithArmor;
import io.xol.chunkstories.core.entity.components.EntityComponentFoodLevel;
import io.xol.chunkstories.core.entity.components.EntityComponentSelectedItem;
import io.xol.chunkstories.core.item.armor.ItemArmor;
import io.xol.chunkstories.core.item.inventory.InventoryLocalCreativeMenu;
import io.xol.chunkstories.core.voxel.VoxelClimbable;

/**
 * Core/Vanilla player, has all the functionality you'd want from it: creative/survival mode, flying and walking controller...
 */
public class EntityPlayer extends EntityHumanoid implements 
EntityControllable, EntityFeedable, EntityOverlay, EntityNameable, 
EntityWithInventory, EntityWithSelectedItem, EntityCreative, EntityFlying, EntityWithArmor,
EntityWorldModifier
{
	//Add the controller component to whatever else the superclass may have
	final EntityComponentController controllerComponent;

	final EntityComponentInventory inventoryComponent;
	final EntityComponentSelectedItem selectedItemComponent;

	final EntityComponentName name;
	final EntityComponentCreativeMode creativeMode;
	final EntityComponentFlying flying;
	
	final EntityArmorInventory armor;

	//FOOD
	EntityComponentFoodLevel foodLevel;
	
	protected boolean onLadder = false;
	
	protected boolean noclip = true;
	
	//Nasty bullshit
	double lastPX = -1f;
	double lastPY = -1f;

	Location lastCameraLocation;

	int variant;
	
	public EntityPlayer(EntityDefinition t, Location location)
	{
		super(t, location);
		
		controllerComponent = new EntityComponentController(this, this.getComponents().getLastComponent());
		
		inventoryComponent = new EntityComponentInventory(this, 10, 4);
		selectedItemComponent = new EntityComponentSelectedItem(this, inventoryComponent);
		
		name = new EntityComponentName(this, this.getComponents().getLastComponent());
		creativeMode = new EntityComponentCreativeMode(this, this.getComponents().getLastComponent());
		flying = new EntityComponentFlying(this, this.getComponents().getLastComponent());
		
		foodLevel = new EntityComponentFoodLevel(this, 100);
		
		armor = new EntityArmorInventory(this, 4, 1);
	}

	public EntityPlayer(EntityDefinition t, Location location, String name)
	{
		this(t, location);
		this.name.setName(name);

		variant = ColorsTools.getUniqueColorCode(name) % 6;
	}

	protected void moveCamera(LocalPlayer controller)
	{
		if (isDead())
			return;
		double cPX = controller.getInputsManager().getMouse().getCursorX();
		double cPY = controller.getInputsManager().getMouse().getCursorY();
		
		double dx = 0, dy = 0;
		if (lastPX != -1f)
		{
			dx = cPX - controller.getWindow().getWidth() / 2.0;
			dy = cPY - controller.getWindow().getHeight() / 2.0;
		}
		lastPX = cPX;
		lastPY = cPY;
		
		double rotH = this.getEntityRotationComponent().getHorizontalRotation();
		double rotV = this.getEntityRotationComponent().getVerticalRotation();

		double modifier = 1.0f;
		if (this.getSelectedItemComponent().getSelectedItem() != null && this.getSelectedItemComponent().getSelectedItem().getItem() instanceof ItemZoom)
		{
			ItemZoom item = (ItemZoom) this.getSelectedItemComponent().getSelectedItem().getItem();
			modifier = 1.0 / item.getZoomFactor();
		}
		
		rotH += dx * modifier / 3f * controller.getClient().renderingConfig().getMouseSensitivity();
		rotV -= dy * modifier / 3f * controller.getClient().renderingConfig().getMouseSensitivity();
		this.getEntityRotationComponent().setRotation(rotH, rotV);
		
		controller.getInputsManager().getMouse().setMouseCursorLocation(controller.getWindow().getWidth() / 2.0, controller.getWindow().getHeight() / 2.0);
	}

	// Server-side updating
	@Override
	public void tick()
	{
		//This is a controllable entity, take care of controlling
		if (world instanceof WorldClient && ((WorldClient)getWorld()).getClient().getPlayer().getControlledEntity() == this)
			tickClientController(((WorldClient)getWorld()).getClient().getPlayer());

		//Tick item in hand if one such exists
		ItemPile pileSelected = getSelectedItemComponent().getSelectedItem();
		if (pileSelected != null)
			pileSelected.getItem().tickInHand(this, pileSelected);

		//Auto-pickups items on the ground
		if (world instanceof WorldMaster && (world.getTicksElapsed() % 60L) == 0L)
		{
			//TODO Use more precise, regional functions to not iterate over the entire world like a retard
			for (Entity e : world.getEntitiesInBox(getLocation(), new Vector3d(3.0)))
			{
				if (e instanceof EntityGroundItem && e.getLocation().distance(this.getLocation()) < 3.0f)
				{
					EntityGroundItem eg = (EntityGroundItem) e;
					if (!eg.canBePickedUpYet())
						continue;

					world.getSoundManager().playSoundEffect("sounds/item/pickup.ogg", Mode.NORMAL, getLocation(), 1.0f, 1.0f);
					
					ItemPile pile = eg.getItemPile();
					if (pile != null)
					{
						ItemPile left = this.inventoryComponent.getInventory().addItemPile(pile);
						if (left == null)
							world.removeEntity(eg);
						else
							eg.setItemPile(left);
					}
				}
			}
		}

		if (world instanceof WorldMaster)
		{
			//Food/health subsystem handled here decrease over time
			
			//Take damage when starving
			if ((world.getTicksElapsed() % 100L) == 0L)
			{
				if (this.getFoodLevel() == 0)
					this.damage(EntityComponentFoodLevel.HUNGER_DAMAGE_CAUSE, 1);
				else
				{
					//27 minutes to start starving at 0.1 starveFactor
					//Takes 100hp / ( 0.6rtps * 0.1 hp/hit )

					//Starve slowly if inactive
					float starve = 0.03f;

					//Walking drains you
					if (this.getVelocityComponent().getVelocity().length() > 0.3)
					{
						starve = 0.06f;
						//Running is even worse
						if (this.getVelocityComponent().getVelocity().length() > 0.7)
							starve = 0.15f;
					}

					float newfoodLevel = this.getFoodLevel() - starve;
					this.setFoodLevel(newfoodLevel);
					//System.out.println("new food level:"+newfoodLevel);
				}
			}

			//It restores hp
			if (getFoodLevel() > 20 && !this.isDead())
			{
				if (this.getHealth() < this.getMaxHealth())
				{
					this.setHealth(this.getHealth() + 0.01f);

					float newfoodLevel = this.getFoodLevel() - 0.01f;
					this.setFoodLevel(newfoodLevel);
				}
			}
			
			//Being on a ladder resets your jump height
			if(onLadder)
				lastStandingHeight = this.positionComponent.getLocation().y();
			if(this.getFlyingComponent().get())
				lastStandingHeight = Double.NaN;
			
			//else
			//	System.out.println("prout"+(world.getTicksElapsed() % 10L));

			//System.out.println(this.getVelocityComponent().getVelocity().length());
		}

		super.tick();

	}

	// client-side method for updating the player movement
	@Override
	public void tickClientController(LocalPlayer controller)
	{
		// Null-out acceleration, until modified by controls
		synchronized (this)
		{
			if (this.getFlyingComponent().get())
				tickFlyMove(controller);
			else
				tickNormalMove(controller);
		}

		//TODO check if this is needed
		//Instead of creating a packet and dealing with it ourselves, we instead push the relevant components
		this.positionComponent.pushComponentEveryoneButController();
		//In that case that means pushing to the server.
	}

	public void tickNormalMove(LocalPlayer controller)
	{
		if (isDead())
			return;

		boolean focus = controller.hasFocus();
		
		if (focus && isOnGround())
		{
			if(controller.getInputsManager().getInputByName("crouch").isPressed())
				this.stance.set(EntityHumanoidStance.CROUCHING);
			else
				this.stance.set(EntityHumanoidStance.STANDING);
		}
		
		boolean inWater = isInWater();
		
		onLadder = false;
		
		all:
		for(CellData vctx : world.getVoxelsWithin(this.getTranslatedBoundingBox())) {
			if(vctx.getVoxel() instanceof VoxelClimbable)
			{
				for(CollisionBox box : vctx.getTranslatedCollisionBoxes()) {
					if(box.collidesWith(this.getTranslatedBoundingBox())) {
						onLadder = true;
						break all;
					}
				}
			}
		}
		

		if (focus && !inWater && controller.getInputsManager().getInputByName("jump").isPressed() && isOnGround())
		{
			// System.out.println("jumpin");
			jumpForce = 0.15;
		}
		else if (focus && inWater && controller.getInputsManager().getInputByName("jump").isPressed())
			jumpForce = 0.05;
		else
			jumpForce = 0.0;

		// Movement
		// Run ?
		if (focus && controller.getInputsManager().getInputByName("forward").isPressed())
		{
			if (controller.getInputsManager().getInputByName("run").isPressed())
				running = true;
		}
		else
			running = false;

		double modif = 0;
		if (focus)
		{
			if (controller.getInputsManager().getInputByName("forward").isPressed() || controller.getInputsManager().getInputByName("left").isPressed() || controller.getInputsManager().getInputByName("right").isPressed())
				horizontalSpeed = ((running && this.stance.get() == EntityHumanoidStance.STANDING) ? 0.09 : 0.06);
			else if (controller.getInputsManager().getInputByName("back").isPressed())
				horizontalSpeed = -0.05;
			else
				horizontalSpeed = 0.0;
			
			if(this.stance.get() == EntityHumanoidStance.CROUCHING)
				horizontalSpeed *= 0.85f;
		}
		else
			horizontalSpeed = 0.0;
		// Water slows you down

		//Strafing
		if(controller.getInputsManager().getInputByName("forward").isPressed())
		{
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 45;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 45;
		}
		else if(controller.getInputsManager().getInputByName("back").isPressed())
		{
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 180 - 45;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 180 - 45;
		}
		else
		{
			if (controller.getInputsManager().getInputByName("left").isPressed())
				modif += 90;
			if (controller.getInputsManager().getInputByName("right").isPressed())
				modif -= 90;
		}
		
		if (onLadder)
		{
			//moveWithCollisionRestrain(0, (float)(Math.sin(((rotV) / 180f * Math.PI)) * hSpeed), 0, false);
			this.getVelocityComponent().setVelocityY((float) (Math.sin((-(this.getEntityRotationComponent().getVerticalRotation()) / 180f * Math.PI)) * horizontalSpeed));
		}

		targetVelocity.x = (Math.sin((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed);
		targetVelocity.z = (Math.cos((180 - this.getEntityRotationComponent().getHorizontalRotation() + modif) / 180f * Math.PI) * horizontalSpeed);
	}

	public static float flySpeed = 0.125f;

	public void tickFlyMove(LocalPlayer controller)
	{
		if (!controller.hasFocus())
			return;
		
		//Flying means we're standing
		this.stance.set(EntityHumanoidStance.STANDING);
		
		getVelocityComponent().setVelocity(0, 0, 0);
		
		float camspeed = flySpeed;
		if (controller.getInputsManager().getInputByName("flyReallyFast").isPressed())
			camspeed *= 8 * 5f;
		else if (controller.getInputsManager().getInputByName("flyFast").isPressed())
			camspeed *= 8f;

		if (controller.getInputsManager().getInputByName("back").isPressed())
		{
			float a = (float) ((-this.getEntityRotationComponent().getHorizontalRotation()) / 180f * Math.PI);
			float b = (float) ((this.getEntityRotationComponent().getVerticalRotation()) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
		}
		if (controller.getInputsManager().getInputByName("forward").isPressed())
		{
			float a = (float) ((180 - this.getEntityRotationComponent().getHorizontalRotation()) / 180f * Math.PI);
			float b = (float) ((-this.getEntityRotationComponent().getVerticalRotation()) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
			else
				moveWithCollisionRestrain(Math.sin(a) * camspeed * Math.cos(b), Math.sin(b) * camspeed, Math.cos(a) * camspeed * Math.cos(b));
		}
		if (controller.getInputsManager().getInputByName("right").isPressed())
		{
			float a = (float) ((-this.getEntityRotationComponent().getHorizontalRotation() - 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
		}
		if (controller.getInputsManager().getInputByName("left").isPressed())
		{
			float a = (float) ((-this.getEntityRotationComponent().getHorizontalRotation() + 90) / 180f * Math.PI);
			if (noclip)
				moveWithoutCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
			else
				moveWithCollisionRestrain(-Math.sin(a) * camspeed, 0, -Math.cos(a) * camspeed);
		}
	}

	@Override
	public void setupCamera(RenderingInterface rd)
	{
		synchronized (this)
		{
			lastCameraLocation = getLocation();

			rd.getCamera().setCameraPosition(new Vector3d(positionComponent.getLocation().add(0.0, eyePosition, 0.0)));
			//camera.pos = lastCameraLocation.clone().negate();
			//camera.pos.add(0d, -eyePosition, 0d);

			rd.getCamera().setRotationX(this.getEntityRotationComponent().getVerticalRotation());
			rd.getCamera().setRotationY(this.getEntityRotationComponent().getHorizontalRotation());

			float modifier = 1.0f;
			if (this.getSelectedItemComponent().getSelectedItem() != null && this.getSelectedItemComponent().getSelectedItem().getItem() instanceof ItemZoom)
			{
				ItemZoom item = (ItemZoom) this.getSelectedItemComponent().getSelectedItem().getItem();
				modifier = 1.0f / item.getZoomFactor();
			}

			rd.getCamera().setFOV(modifier * (float) (rd.renderingConfig().getFov()//RenderingConfig.fov
					+ ((getVelocityComponent().getVelocity().x() * getVelocityComponent().getVelocity().x() + getVelocityComponent().getVelocity().z() * getVelocityComponent().getVelocity().z()) > 0.07 * 0.07
							? ((getVelocityComponent().getVelocity().x() * getVelocityComponent().getVelocity().x() + getVelocityComponent().getVelocity().z() * getVelocityComponent().getVelocity().z()) - 0.07 * 0.07) * 500 : 0)));
			
		}
	}

	@Override
	public Location getBlockLookingAt(boolean inside)
	{
		Vector3d initialPosition = new Vector3d(getLocation());
		initialPosition.add(new Vector3d(0, eyePosition, 0));

		Vector3d direction = new Vector3d(getDirectionLookingAt());

		if (inside)
			return world.collisionsManager().raytraceSelectable(new Location(world, initialPosition), direction, 256.0);
		else
			return world.collisionsManager().raytraceSolidOuter(new Location(world, initialPosition), direction, 256.0);
	}

	@Override
	public void drawEntityOverlay(RenderingInterface renderer)
	{
		//super.drawEntityOverlay(renderer);
		if (this.equals(((WorldClient)getWorld()).getClient().getPlayer().getControlledEntity()))
		{
			float scale = 2.0f;
			
			renderer.textures().getTexture("./textures/gui/hud/hud_survival.png").setLinearFiltering(false);
			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(renderer.getWindow().getWidth() / 2 - 256 * 0.5f * scale, 64 + 64 + 16 - 32 * 0.5f * scale, 256 * scale, 32 * scale, 0, 32f / 256f, 1, 0,
					renderer.textures().getTexture("./textures/gui/hud/hud_survival.png"), false, true, null);

			//Health bar
			int horizontalBitsToDraw = (int) (8 + 118 * Math2.clamp(getHealth() / getMaxHealth(), 0.0, 1.0));
			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(renderer.getWindow().getWidth() / 2 - 128 * scale, 64 + 64 + 16 - 32 * 0.5f * scale, horizontalBitsToDraw * scale, 32 * scale, 0, 64f / 256f, horizontalBitsToDraw / 256f,
					32f / 256f, renderer.textures().getTexture("./textures/gui/hud/hud_survival.png"), false, true, new Vector4f(1.0f, 1.0f, 1.0f, 0.75f));

			//Food bar
			horizontalBitsToDraw = (int) (0 + 126 * Math2.clamp(getFoodLevel() / 100f, 0.0, 1.0));
			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(
					renderer.getWindow().getWidth() / 2 + 0 * 128 * scale + 0, 64 + 64 + 16 - 32 * 0.5f * scale, horizontalBitsToDraw * scale, 32 * scale, 0.5f , 64f / 256f, 0.5f + horizontalBitsToDraw / 256f,
					32f / 256f, renderer.textures().getTexture("./textures/gui/hud/hud_survival.png"), false, true, new Vector4f(1.0f, 1.0f, 1.0f, 0.75f));
		
			//If we're using an item that can render an overlay
			if (this.getSelectedItemComponent().getSelectedItem() != null)
			{
				ItemPile pile = this.getSelectedItemComponent().getSelectedItem();
				if (pile.getItem() instanceof ItemOverlay)
					((ItemOverlay) pile.getItem()).drawItemOverlay(renderer, pile);
			}
			
			//We don't want to render our own tag do we ?
			return;
		}

		//Renders the nametag above the player heads
		Vector3d pos = getLocation();

		//don't render tags too far out
		if (pos.distance(renderer.getCamera().getCameraPosition()) > 32f)
			return;

		//Don't render a dead player tag
		if (this.getHealth() <= 0)
			return;

		Vector3fc posOnScreen = renderer.getCamera().transform3DCoordinate(new Vector3f((float)(double) pos.x(), (float)(double) pos.y() + 2.0f, (float)(double) pos.z()));

		float scale = posOnScreen.z();
		String txt = name.getName();// + rotH;
		float dekal = renderer.getFontRenderer().defaultFont().getWidth(txt) * 16 * scale;
		//System.out.println("dekal"+dekal);
		if (scale > 0)
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), posOnScreen.x() - dekal / 2, posOnScreen.y(), txt, 16 * scale, 16 * scale, new Vector4f(1, 1, 1, 1));
	}

	class EntityPlayerRenderer<H extends EntityPlayer> extends EntityHumanoidRenderer<H>
	{
		@Override
		public void setupRender(RenderingInterface renderingContext)
		{
			super.setupRender(renderingContext);
		}

		@Override
		public int renderEntities(RenderingInterface renderingContext, RenderingIterator<H> renderableEntitiesIterator)
		{
			setupRender(renderingContext);
			
			int e = 0;

			Vector3f loc3f = new Vector3f();
			Vector3f pre3f = new Vector3f();
			
			for (EntityPlayer entity : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				Location location = entity.getPredictedLocation();
				loc3f.set((float)location.x(), (float)location.y(), (float)location.z());
				pre3f.set((float)entity.getPredictedLocation().x(), (float)entity.getPredictedLocation().y(), (float)entity.getPredictedLocation().z());

				if (!(renderingContext.getWorldRenderer().getCurrentRenderingPass() == RenderingPass.SHADOW && location.distance(renderingContext.getCamera().getCameraPosition()) > 15f))
				{
					entity.cachedSkeleton.lodUpdate(renderingContext);

					Matrix4f matrix = new Matrix4f();
					matrix.translate(loc3f);
					renderingContext.setObjectMatrix(matrix);

					//Obtains the data for the block the player is standing inside of, so he can be lit appropriately
					CellData cell = entity.getWorld().peekSafely(entity.getLocation());
					renderingContext.currentShader().setUniform2f("worldLightIn", cell.getBlocklight(), cell.getSunlight() );
					
					variant = ColorsTools.getUniqueColorCode(entity.getName()) % 6;

					//Player textures
					Texture2D playerTexture = renderingContext.textures().getTexture("./models/variant" + variant + ".png");
					playerTexture.setLinearFiltering(false);

					renderingContext.bindAlbedoTexture(playerTexture);
					renderingContext.bindNormalTexture(renderingContext.textures().getTexture("./textures/normalnormal.png"));
					renderingContext.bindMaterialTexture(renderingContext.textures().getTexture("./textures/defaultmaterial.png"));

					renderingContext.meshes().getRenderableMultiPartAnimatableMeshByName("./models/human.obj").render(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);
					//animationsData.add(new AnimatableData(location.castToSinglePrecision(), entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, bl, sl));
			
					for(ItemPile aip : entity.armor.getInventory().iterator())
					{
						ItemArmor ia = (ItemArmor)aip.getItem();
						
						renderingContext.bindAlbedoTexture(renderingContext.textures().getTexture(ia.getOverlayTextureName()));
						renderingContext.textures().getTexture(ia.getOverlayTextureName()).setLinearFiltering(false);
						renderingContext.meshes().getRenderableMultiPartAnimatableMeshByName("./models/human_overlay.obj").render(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, ia.bodyPartsAffected());
					}
					
					ItemPile selectedItemPile = null;

					if (entity instanceof EntityWithSelectedItem)
						selectedItemPile = ((EntityWithSelectedItem) entity).getSelectedItem();

					renderingContext.currentShader().setUniform3f("objectPosition", new Vector3f(0));

					if (selectedItemPile != null)
					{
						Matrix4f itemMatrix = new Matrix4f();
						itemMatrix.translate(pre3f);

						itemMatrix.mul(entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000));
						//Matrix4f.mul(itemMatrix, entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000), itemMatrix);

						selectedItemPile.getItem().getDefinition().getRenderer().renderItemInWorld(renderingContext, selectedItemPile, world, entity.getLocation(), itemMatrix);
					}
				}
				e++;
			}

			return e;
		}
	}

	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return new EntityPlayerRenderer<EntityPlayer>();
	}

	@Override
	public String getName()
	{
		return name.getName();
	}

	@Override
	public void setName(String n)
	{
		name.setName(n);
	}

	@Override
	public void onEachFrame(LocalPlayer controller)
	{
		if (controller.hasFocus())
		{
			moveCamera(controller);
		}
	}

	@Override
	public boolean onControllerInput(Input input, Controller controller)
	{
		//We are moving inventory bringup here !
		if(input.getName().equals("inventory") && world instanceof WorldClient)
		{
			
			if(creativeMode.get()) {
				((WorldClient)getWorld()).getClient().openInventories(this.inventoryComponent.getInventory(), new InventoryLocalCreativeMenu(world));
			}
			else {
				((WorldClient)getWorld()).getClient().openInventories(this.inventoryComponent.getInventory(), this.armor.getInventory());
			}
			
			return true;
		}
		
		Location blockLocation = this.getBlockLookingAt(true);
		
		double maxLen = 1024;
		
		if(blockLocation != null ) {
			Vector3d diff = new Vector3d(blockLocation).sub(this.getLocation());
			//Vector3d dir = diff.clone().normalize();
			maxLen = diff.length();
		}
		
		Vector3d initialPosition = new Vector3d(getLocation());
		initialPosition.add(new Vector3d(0, eyePosition, 0));
		
		Vector3dc direction = getDirectionLookingAt();
		
		Iterator<Entity> i = world.collisionsManager().rayTraceEntities(initialPosition, direction, maxLen);
		while(i.hasNext()) {
			Entity e = i.next();
			if(e.handleInteraction(this, input))
				return true;
		}
		
		ItemPile itemSelected = getSelectedItemComponent().getSelectedItem();
		if (itemSelected != null)
		{
			//See if the item handles the interaction
			if (itemSelected.getItem().onControllerInput(this, itemSelected, input, controller))
				return true;
		}
		if (getWorld() instanceof WorldMaster)
		{
			//Creative mode features building and picking.
			if (this.getCreativeModeComponent().get())
			{
				if (input.getName().equals("mouse.left"))
				{
					if (blockLocation != null)
					{
						// Player events mod
						if(controller instanceof Player) {
							Player player = (Player)controller;
							WorldCell cell = world.peekSafely(blockLocation);
							FutureCell future = new FutureCell(cell);
							future.setVoxel(this.getDefinition().store().parent().voxels().air());
							
							PlayerVoxelModificationEvent event = new PlayerVoxelModificationEvent(cell, future, EntityCreative.CREATIVE_MODE, player);
							
							//Anyone has objections ?
							world.getGameContext().getPluginManager().fireEvent(event);
							
							if(event.isCancelled())
								return true;
						
							try {
								world.poke(future, this);
								//world.poke((int)blockLocation.x, (int)blockLocation.y, (int)blockLocation.z, null, 0, 0, 0, this);
							} catch (WorldException e) {
								//Discard but maybe play some effect ?
							}
							
							return true;
						}
					}
				}
				else if (input.getName().equals("mouse.middle"))
				{
					if (blockLocation != null)
					{
						CellData ctx = this.getWorld().peekSafely(blockLocation);

						if (!ctx.getVoxel().isAir())
						{
							//Spawn new itemPile in his inventory
							ItemVoxel item = (ItemVoxel) world.getGameContext().getContent().items().getItemTypeByName("item_voxel").newItem();
							item.voxel = ctx.getVoxel();
							item.voxelMeta = ctx.getMetaData();

							ItemPile itemVoxel = new ItemPile(item);
							this.getInventory().setItemPileAt(getSelectedItemComponent().getSelectedSlot(), 0, itemVoxel);
							return true;
						}
					}
				}
			}
		}
		//Here goes generic entity response to interaction

		//				n/a

		//Then we check if the world minds being interacted with
		return world.handleInteraction(this, blockLocation, input);
	}
	
	public boolean handleInteraction(Entity entity, Input input) {
		if(isDead() && input.getName().equals("mouse.right") && entity instanceof EntityControllable) {
			EntityControllable ctrla = (EntityControllable)entity;
			
			Controller ctrlr = ctrla.getController();
			if(ctrlr != null && ctrlr instanceof Player) {
				Player p = (Player)ctrlr;
				
				p.openInventory(this.getInventory());
				//p.sendMessage("HELLO THIS MY CADAVERER, PLZ FUCK OFF");
				return true;
			}
		}
		return false;
	}

	@Override
	public EntityComponentController getControllerComponent()
	{
		return this.controllerComponent;
	}

	@Override
	public Inventory getInventory()
	{
		return inventoryComponent.getInventory();
	}

	public EntityComponentSelectedItem getSelectedItemComponent()
	{
		return this.selectedItemComponent;
	}

	@Override
	public EntityComponentName getNameComponent()
	{
		return name;
	}

	@Override
	public EntityComponentFlying getFlyingComponent()
	{
		return flying;
	}

	@Override
	public EntityComponentCreativeMode getCreativeModeComponent()
	{
		return creativeMode;
	}

	@Override
	public boolean shouldSaveIntoRegion()
	{
		//Player entities are handled their own way.
		return false;
	}

	@Override
	public Location getPredictedLocation()
	{
		return lastCameraLocation != null ? lastCameraLocation : getLocation();
	}

	public float getFoodLevel()
	{
		return foodLevel.getValue();
	}

	public void setFoodLevel(float level)
	{
		foodLevel.setValue(level);
	}
	
	@Override
	public float damage(DamageCause cause, HitBox osef, float damage)
	{
		if(!isDead())
		{
			int i = 1 + (int) Math.random() * 3;
			world.getSoundManager().playSoundEffect("sounds/entities/human/hurt"+i+".ogg", Mode.NORMAL, this.getLocation(), (float)Math.random() * 0.4f + 0.8f, 5.0f);
		}
		
		return super.damage(cause, osef, damage);
	}

	@Override
	public EntityArmorInventory getArmor()
	{
		return armor;
	}

	@Override
	public ItemPile getSelectedItem() {
		return this.selectedItemComponent.getSelectedItem();
	}

	@Override
	public int getSelectedItemIndex() {
		return this.selectedItemComponent.getSelectedSlot();
	}

	@Override
	public void setSelectedItemIndex(int i) {
		this.selectedItemComponent.setSelectedSlot(i);
	}
}
