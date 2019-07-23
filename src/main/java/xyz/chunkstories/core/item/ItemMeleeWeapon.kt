//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.client.LocalPlayer
import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.TraitHitboxes
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.EntityHitbox
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.cell.EditableCell
import xyz.chunkstories.core.entity.traits.TraitEyeLevel
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f

class ItemMeleeWeapon(type: ItemDefinition) : ItemWeapon(type) {
    internal val swingDuration: Long
    internal val hitTime: Long
    internal val range: Double

    internal val damage: Float

    internal val itemRenderScale: Float

    internal var currentSwingStart = 0L
    internal var hasHitYet = false
    internal var cooldownEnd = 0L

    init {

        swingDuration = Integer.parseInt(type.resolveProperty("swingDuration", "100")).toLong()
        hitTime = Integer.parseInt(type.resolveProperty("hitTime", "100")).toLong()

        range = java.lang.Double.parseDouble(type.resolveProperty("range", "3"))
        damage = java.lang.Float.parseFloat(type.resolveProperty("damage", "100"))

        itemRenderScale = java.lang.Float.parseFloat(type.resolveProperty("itemRenderScale", "2"))
    }

    /*public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer) {
        ItemRenderer itemRenderer;

        String modelName = getDefinition().resolveProperty("modelObj", "none");
        if (!modelName.equals("none"))
            itemRenderer = new ItemModelRenderer(this, fallbackRenderer, modelName,
                    getDefinition().resolveProperty("modelDiffuse", "none"));
        else
            itemRenderer = new FlatIconItemRenderer(this, fallbackRenderer, getDefinition());

        itemRenderer = new MeleeWeaponRenderer(fallbackRenderer);

        return itemRenderer;
    }*/

    override fun tickInHand(owner: Entity, itemPile: ItemPile) {

        if (currentSwingStart != 0L && !hasHitYet && System.currentTimeMillis() - currentSwingStart > hitTime) {
            val controller = owner.traits[TraitControllable::class]?.controller

            // For now only client-side players can trigger shooting actions
            if (controller != null && controller is LocalPlayer) {
                if (!controller.hasFocus())
                    return

                val LocalPlayer = controller as LocalPlayer?

                // Uses fake input to notify server/master of intention to attack.
                LocalPlayer!!.inputsManager
                        .onInputPressed(LocalPlayer.inputsManager.getInputByName("shootGun")!!)

                hasHitYet = true
            }

        }

    }

    override fun onControllerInput(entity: Entity, pile: ItemPile, input: Input, controller: Controller): Boolean {
        if (input.name.startsWith("mouse.left")) {
            // Checks current swing is done
            if (System.currentTimeMillis() - currentSwingStart > swingDuration) {
                currentSwingStart = System.currentTimeMillis()
                hasHitYet = false
            }

            return true
        } else if (input.name == "shootGun" && entity.world is WorldMaster) {
            // Actually hits
            val direction = entity.traits[TraitRotation::class]?.directionLookingAt ?: return false

            val eyeLocation = Vector3d(entity.location)
            eyeLocation.y += entity.traits[TraitEyeLevel::class]?.eyeLevel ?: 0.0

            // Find wall collision
            var shotBlock = entity.world.collisionsManager.raytraceSolid(eyeLocation, direction!!, range)
            val nearestLocation = Vector3d()

            // Loops to try and break blocks
            while (entity.world is WorldMaster && shotBlock != null) {
                val peek = entity.world.peekSafely(shotBlock)

                if (!peek.voxel!!.isAir() && peek.voxel!!.voxelMaterial.resolveProperty("bulletBreakable") != null
                        && peek.voxel!!.voxelMaterial.resolveProperty("bulletBreakable") == "true") {
                    // TODO: Spawn an event to check if it's okay

                    // Destroy it
                    peek.voxel = definition.store.parent.voxels.air
                    //peek.setVoxel(definition.store().parent().voxels().air())

                    spawnDebris(entity, direction, shotBlock)
                    entity.world.soundManager.playSoundEffect("sounds/environment/glass.ogg", Mode.NORMAL,
                            shotBlock, Math.random().toFloat() * 0.2f + 0.9f, 1.0f)

                    // Re-raytrace the ray
                    shotBlock = entity.world.collisionsManager.raytraceSolid(eyeLocation, direction, range)
                } else
                    break
            }

            if (shotBlock != null) {
                val shotBlockOuter = entity.world.collisionsManager.raytraceSolidOuter(eyeLocation,
                        direction, range)
                if (shotBlockOuter != null) {
                    val normal = shotBlockOuter.sub(shotBlock)

                    val NbyI2x = 2.0 * direction.dot(normal)
                    val NxNbyI2x = Vector3d(normal)
                    NxNbyI2x.mul(NbyI2x)

                    val reflected = Vector3d(direction)
                    reflected.sub(NxNbyI2x)

                    val peek = entity.world.peekSafely(shotBlock)

                    // This seems fine
                    for (box in peek.translatedCollisionBoxes!!) {
                        val thisLocation = box.lineIntersection(eyeLocation, direction)
                        if (thisLocation != null) {
                            if (nearestLocation == null || nearestLocation.distance(eyeLocation) > thisLocation.distance(eyeLocation))
                                nearestLocation.set(thisLocation)
                        }
                    }

                    // Position adjustements so shot blocks always shoot proper particles
                    if (shotBlock.x() - nearestLocation.x() <= -1.0)
                        nearestLocation.add(-0.01, 0.0, 0.0)
                    if (shotBlock.y() - nearestLocation.y() <= -1.0)
                        nearestLocation.add(0.0, -0.01, 0.0)
                    if (shotBlock.z() - nearestLocation.z() <= -1.0)
                        nearestLocation.add(0.0, 0.0, -0.01)

                    for (i in 0..24) {
                        val untouchedReflection = Vector3d(reflected)

                        val random = Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0,
                                Math.random() * 2.0 - 1.0)
                        random.mul(0.5)
                        untouchedReflection.add(random)
                        untouchedReflection.normalize()

                        untouchedReflection.mul(0.25)

                        val ppos = Vector3d(nearestLocation)
                        //TODO entity.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", ppos, untouchedReflection);
                        entity.world.soundManager.playSoundEffect(entity.world.peekSafely(shotBlock)
                                .voxel!!.voxelMaterial.resolveProperty("landingSounds"), Mode.NORMAL, ppos, 1f,
                                0.25f)
                    }

                    entity.world.decalsManager.add(nearestLocation, normal.negate(), Vector3d(0.5),
                            "bullethole")
                }
            }

            // Hitreg takes place on server bois
            if (entity.world is WorldMaster) {
                // Iterate over each found entities
                val shotEntities = entity.world.collisionsManager.rayTraceEntities(eyeLocation,
                        direction, range)
                while (shotEntities.hasNext()) {
                    val shotEntity = shotEntities.next()
                    // Don't shoot itself & only living things get shot
                    if (shotEntity != entity) {
                        val hitboxes = shotEntity.traits[TraitHitboxes::class.java]
                        val health = shotEntity.traits[TraitHealth::class.java]

                        if (health != null && hitboxes != null) {
                            // Get hit location
                            for (hitBox in hitboxes.hitBoxes) {
                                val hitPoint = hitBox.lineIntersection(eyeLocation, direction) ?: continue

// Deal damage
                                health.damage(pileAsDamageCause(pile), hitBox, damage)

                                // Spawn blood particles
                                val bloodDir = Vector3d()
                                direction.normalize(bloodDir).mul(0.25)
                                for (i in 0..249) {
                                    val random = Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0,
                                            Math.random() * 2.0 - 1.0)
                                    random.mul(0.25)
                                    random.add(bloodDir)

                                    //TODO shotEntity.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("blood", hitPoint, random);
                                }

                                // Spawn blood on walls
                                if (nearestLocation != null)
                                    shotEntity.world.decalsManager.add(nearestLocation, bloodDir,
                                            Vector3d(3.0), "blood")
                            }
                        }
                    }
                }
            }

        }
        return false
    }

    companion object {

        internal fun spawnDebris(entity: Entity, direction: Vector3dc, shotBlock: Location) {
            for (i in 0..24) {
                val smashedVoxelParticleDirection = Vector3d(direction)
                smashedVoxelParticleDirection.mul(2.0)
                smashedVoxelParticleDirection.add(Math.random() - 0.5, Math.random() - 0.5,
                        Math.random() - 0.5)
                smashedVoxelParticleDirection.normalize()

                //TODO entity.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", shotBlock, smashedVoxelParticleDirection);
            }
        }
    }

    /*class MeleeWeaponRenderer extends ItemRenderer {
        MeleeWeaponRenderer(ItemRenderer fallbackRenderer) {
            super(fallbackRenderer);
        }

        @Override
        public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world,
                                      Location location, Matrix4f handTransformation) {
            Matrix4f matrixed = new Matrix4f(handTransformation);

            float rot = 0;

            ItemMeleeWeapon instance = (ItemMeleeWeapon) pile.getItem();

            if (System.currentTimeMillis() - instance.currentSwingStart < instance.swingDuration) {
                if (instance.hitTime == instance.swingDuration) {
                    // Whole thing over the same duration
                    rot = (float) (0 - Math.PI / 4f * (float) (System.currentTimeMillis() - instance.currentSwingStart)
                            / instance.hitTime);
                } else {
                    // We didn't hit yet
                    if (System.currentTimeMillis() - instance.currentSwingStart < instance.hitTime)
                        rot = (float) (0 - Math.PI / 4f
                                * (float) (System.currentTimeMillis() - instance.currentSwingStart) / instance.hitTime);
                        // We did
                    else
                        rot = (float) (0 - Math.PI / 4f + Math.PI / 4f
                                * (float) (System.currentTimeMillis() - instance.currentSwingStart - instance.hitTime)
                                / (instance.swingDuration - instance.hitTime));

                }
            }

            float dekal = -0.45f;
            matrixed.translate(new Vector3f(0, dekal, 0));
            matrixed.rotate(rot, new Vector3f(0, 0, 1));
            matrixed.translate(new Vector3f(0, 0.25f - dekal, 0));

            matrixed.scale(new Vector3f(instance.itemRenderScale));

            super.renderItemInWorld(renderingInterface, pile, world, location, matrixed);
        }
    }*/
}
