//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity;

import xyz.chunkstories.api.entity.DamageCause;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.EntityDefinition;
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth;
import xyz.chunkstories.api.physics.EntityHitbox;
import xyz.chunkstories.api.sound.SoundSource.Mode;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.core.entity.ai.ZombieAI;

import java.util.HashSet;
import java.util.Set;

public class EntityZombie extends EntityHumanoid implements DamageCause {
	ZombieAI zombieAi;

	final TraitZombieInfectionStage stageComponent;

	static Set<Class<? extends Entity>> zombieTargets = new HashSet<Class<? extends Entity>>();

	static {
		zombieTargets.add(EntityPlayer.class);
	}

	public EntityZombie(EntityDefinition t, World world) {
		this(t, world, ZombieInfectionStage.values()[(int) Math.floor(Math.random() * ZombieInfectionStage.values().length)]);
	}

	public EntityZombie(EntityDefinition t, World world, ZombieInfectionStage stage) {
		super(t, world);
		zombieAi = new ZombieAI(this, zombieTargets);

		this.stageComponent = new TraitZombieInfectionStage(this, stage);

		this.entityHealth = new TraitHealth(this) {

			@Override public float damage(DamageCause cause, EntityHitbox osef, float damage) {
				if (!this.isDead())
					EntityZombie.this.getWorld().getSoundManager()
							.playSoundEffect("sounds/entities/zombie/hurt.ogg", Mode.NORMAL, getLocation(), (float) Math.random() * 0.4f + 0.8f,
									1.5f + Math.min(0.5f, damage / 15.0f));

				if (cause instanceof EntityLiving) {
					EntityLiving entity = (EntityLiving) cause;
					zombieAi.setAiTask(zombieAi.new AiTaskAttackEntity(entity, 15f, 20f, zombieAi.currentTask(), stage().attackCooldown, stage().attackDamage));
				}

				return super.damage(cause, osef, damage);
			}
		};
		this.entityHealth.setHealth(stage.hp);

		new ZombieRenderer(this);
		//new TraitRenderable(this, EntityZombieRenderer::new);
	}

	@Override public void tick() {
		// AI works on master
		if (getWorld() instanceof WorldMaster)
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

	public ZombieInfectionStage stage() {
		return stageComponent.getStage();
	}

	public void attack(EntityLiving target, float maxDistance) {
		this.zombieAi
				.setAiTask(zombieAi.new AiTaskAttackEntity(target, 15f, maxDistance, zombieAi.currentTask(), stage().attackCooldown, stage().attackDamage));
	}

	@Override public String getName() {
		return "Zombie";
	}
}
