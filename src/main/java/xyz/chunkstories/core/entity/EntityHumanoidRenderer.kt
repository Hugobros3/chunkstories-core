package xyz.chunkstories.core.entity

import org.joml.Matrix4f
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.entity.traits.TraitRenderable
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.util.kotlin.toVec3f

open class EntityHumanoidRenderer(entity: EntityHumanoid, private val customSkin: MeshMaterial? = null) : TraitRenderable<EntityHumanoid>(entity) {

    override fun buildRepresentation(representationsGobbler: RepresentationsGobbler) {
        val model = entity.world.content.models["./models/human/human.dae"]

        val matrix = Matrix4f()
        matrix.translate(entity.location.toVec3f())
        val position = ModelPosition(matrix)

        val isPlayerEntity = (entity.world.gameContext as? Client)?.ingame?.player?.controlledEntity == this.entity

        var visibility = 0
        for (i in 0 until representationsGobbler.renderingContexts.size) {
            val isPassShadow = representationsGobbler.renderingContexts[i].name.startsWith("shadow")

            val ithBit = isPassShadow or !isPlayerEntity
            visibility = visibility or (ithBit.toInt() shl i)
        }

        val materials = if (customSkin != null)
            model.meshes.mapIndexed { index, _ -> index }.associateWith { customSkin!! }
        else
            emptyMap()

        val modelInstance = ModelInstance(model, position, materials, visibility)

        representationsGobbler.acceptRepresentation(modelInstance, visibility)
    }

}

private inline fun Boolean.toInt() = if (this) 1 else 0

class ZombieRenderer(entity: EntityZombie) : EntityHumanoidRenderer(entity, Unit.let {
    MeshMaterial("zombie", mapOf(
            "albedoTexture" to "./models/human/zombie_s" + (entity.stage().ordinal + 1) + ".png"
    ))
} )

/*protected static class EntityHumanoidRenderer<H extends EntityHumanoid> extends EntityRenderer<H> {
		void setupRender(RenderingInterface renderingContext) {
			// Player textures
			Texture2D playerTexture = renderingContext.textures().getTexture("./models/human/humanoid_test.png");
			playerTexture.setLinearFiltering(false);

			renderingContext.bindAlbedoTexture(playerTexture);

			renderingContext.textures().getTexture("./models/human/humanoid_normal.png").setLinearFiltering(false);

			renderingContext.bindNormalTexture(renderingContext.textures().getTexture("./textures/normalnormal.png"));
			renderingContext
					.bindMaterialTexture(renderingContext.textures().getTexture("./textures/defaultmaterial.png"));
		}

		@Override
		public int renderEntities(RenderingInterface renderer, RenderingIterator<H> renderableEntitiesIterator) {
			renderer.useShader("entities_animated");

			setupRender(renderer);

			int e = 0;

			for (EntityHumanoid entity : renderableEntitiesIterator.getElementsInFrustrumOnly()) {
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

				renderer.meshes().getRenderableAnimatableMesh("./models/human/human.dae").render(renderer,
						animation.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);
			}

			// Render items in hands
			for (EntityHumanoid entity : renderableEntitiesIterator) {

				if (renderer.getCurrentPass().name.startsWith("shadow")
						&& entity.getLocation().distance(renderer.getCamera().getCameraPosition()) > 15f)
					continue;

				TraitAnimated animation = entity.traits.get(TraitAnimated.class);
				ItemPile selectedItemPile = entity.traits.tryWith(TraitSelectedItem.class,
						eci -> eci.getSelectedItem());

				if (selectedItemPile != null) {
					Matrix4f itemMatrix = new Matrix4f();
					itemMatrix.translate((float) entity.getLocation().x(), (float) entity.getLocation().y(),
							(float) entity.getLocation().z());

					itemMatrix.mul(animation.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix(
							"boneItemInHand", System.currentTimeMillis() % 1000000));

					selectedItemPile.getItem().getDefinition().getRenderer().renderItemInWorld(renderer,
							selectedItemPile, entity.world, entity.getLocation(), itemMatrix);
				}

				e++;
			}

			return e;
		}

		@Override
		public void freeRessources() {

		}

	}*/