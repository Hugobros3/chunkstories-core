//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth;
import io.xol.chunkstories.api.entity.traits.serializable.TraitSerializable;
import io.xol.chunkstories.api.physics.EntityHitbox;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.serialization.StreamSource;
import io.xol.chunkstories.api.world.serialization.StreamTarget;
import io.xol.chunkstories.core.entity.ai.ZombieAI;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class EntityZombie extends EntityHumanoid implements DamageCause {
	ZombieAI zombieAi;

	final StageComponent stageComponent;

	public enum Stage {
		INFECTION(0.045, 5, 1800, 10f, 40f), TAKEOVER(0.060, 10, 1200, 15f, 80f), WHOLESOME(0.075, 15, 800, 20f, 160f),;

		private Stage(double speed, double aggroDistance, int attackCooldown, float attackDamage, float hp) {
			this.speed = speed;
			this.aggroRadius = aggroDistance;
			this.attackCooldown = attackCooldown;
			this.attackDamage = attackDamage;
			this.hp = hp;
		}

		public final double speed;
		public final double aggroRadius;
		public final int attackCooldown;
		public final float attackDamage;
		public final float hp;
	}

	static class StageComponent extends TraitSerializable {

		Stage stage;

		public StageComponent(Entity entity) {
			super(entity);
		}

		public String getSerializedComponentName() {
			return "stage";
		}

		@Override
		protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException {
			dos.writeByte(stage.ordinal());
		}

		@Override
		protected void pull(StreamSource from, DataInputStream dis) throws IOException {
			byte ok = dis.readByte();
			int i = (int) ok;

			stage = Stage.values()[i];
		}

		public void setStage(Stage stage2) {
			this.stage = stage2;
			this.pushComponentEveryone();
		}

	}

	static Set<Class<? extends Entity>> zombieTargets = new HashSet<Class<? extends Entity>>();

	static {
		zombieTargets.add(EntityPlayer.class);
	}

	public EntityZombie(EntityDefinition t, World world) {
		this(t, world, Stage.values()[(int) Math.floor(Math.random() * Stage.values().length)]);
	}

	public EntityZombie(EntityDefinition t, World world, Stage stage) {
		super(t, world);
		zombieAi = new ZombieAI(this, zombieTargets);

		this.stageComponent = new StageComponent(this);
		this.stageComponent.setStage(stage);

		this.entityHealth = new TraitHealth(this) {

			@Override
			public float damage(DamageCause cause, EntityHitbox osef, float damage) {
				if (!this.isDead())
					EntityZombie.this.world.getSoundManager().playSoundEffect("sounds/entities/zombie/hurt.ogg", Mode.NORMAL,
							getLocation(), (float) Math.random() * 0.4f + 0.8f, 1.5f + Math.min(0.5f, damage / 15.0f));

				if (cause instanceof EntityLiving) {
					EntityLiving entity = (EntityLiving) cause;
					zombieAi.setAiTask(zombieAi.new AiTaskAttackEntity(entity, 15f, 20f, zombieAi.currentTask(),
							stage().attackCooldown, stage().attackDamage));
				}

				return super.damage(cause, osef, damage);
			}
		};
		this.entityHealth.setHealth(stage.hp);

		//new TraitRenderable(this, EntityZombieRenderer::new);
	}

	@Override
	public void tick() {
		// AI works on master
		if (world instanceof WorldMaster)
			zombieAi.tick();

		// Ticks the entity
		super.tick();

		// Anti-glitch
		if (Double.isNaN(entityRotation.getHorizontalRotation())) {
			System.out.println("nan !" + this);
			entityRotation.setRotation(0.0, 0.0);
		}
	}

	/*class EntityZombieRenderer extends EntityHumanoidRenderer<EntityZombie> {

		@Override
		public void setupRender(RenderingInterface renderingContext) {
			super.setupRender(renderingContext);
		}

		@Override
		public int renderEntities(RenderingInterface renderer,
				RenderingIterator<EntityZombie> renderableEntitiesIterator) {
			renderer.useShader("entities_animated");
			setupRender(renderer);

			int e = 0;

			for (EntityZombie entity : renderableEntitiesIterator.getElementsInFrustrumOnly()) {
				Location location = entity.getLocation();// entity.getPredictedLocation();

				if (renderer.getCurrentPass().name.startsWith("shadow")
						&& location.distance(renderer.getCamera().getCameraPosition()) > 15f)
					continue;

				CellData cell = entity.getWorld().peekSafely(entity.getLocation());
				renderer.currentShader().setUniform2f("worldLightIn", cell.getBlocklight(), cell.getSunlight());

				TraitAnimated animation = entity.traits.get(TraitAnimated.class);
				((CachedLodSkeletonAnimator) animation.getAnimatedSkeleton()).lodUpdate(renderer);

				Matrix4f matrix = new Matrix4f();
				matrix.translate((float) location.x, (float) location.y, (float) location.z);
				renderer.setObjectMatrix(matrix);

				// Player textures
				Texture2D playerTexture = renderer.textures()
						.getTexture("./models/human/zombie_s" + (entity.stage().ordinal() + 1) + ".png");
				playerTexture.setLinearFiltering(false);

				renderer.bindAlbedoTexture(playerTexture);
				renderer.meshes().getRenderableAnimatableMesh("./models/human/human.dae").render(renderer,
						animation.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);

				e++;
			}

			return e;
		}
	}*/

	public Stage stage() {
		return stageComponent.stage;
	}

	public void attack(EntityLiving target, float maxDistance) {
		this.zombieAi.setAiTask(zombieAi.new AiTaskAttackEntity(target, 15f, maxDistance, zombieAi.currentTask(),
				stage().attackCooldown, stage().attackDamage));
	}

	@Override
	public String getName() {
		return "Zombie";
	}
}
