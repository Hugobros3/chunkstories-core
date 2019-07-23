//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector4f
import xyz.chunkstories.api.client.LocalPlayer
import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.TraitHitboxes
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.entity.traits.serializable.TraitCreativeMode
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.interfaces.ItemCustomHoldingAnimation
import xyz.chunkstories.api.item.interfaces.ItemOverlay
import xyz.chunkstories.api.item.interfaces.ItemZoom
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.entity.traits.TraitEyeLevel

class ItemFirearm(type: ItemDefinition) : ItemWeapon(type), ItemOverlay, ItemZoom, ItemCustomHoldingAnimation {
    val automatic: Boolean
    val rpm: Double
    val soundName: String
    val damage: Double
    val accuracy: Double
    val range: Double
    val soundRange: Double
    val shots: Int
    val shake: Double
    val reloadCooldown: Long

    val isScopedWeapon: Boolean
    val scopeZoom: Float
    val scopeSlow: Float
    val scopeTexture: String

    val holdingAnimationName: String
    val shootingAnimationName: String
    val shootingAnimationDuration: Long

    private var wasTriggerPressedLastTick = false
    private var lastShot = 0L

    private var cooldownEnd = 0L
    private var animationStart = 0L
    private var animationCooldownEnd = 0L

    var isScoped = false
        private set

    private var currentMagazine: ItemPile? = null

    init {

        automatic = type.resolveProperty("fireMode", "semiauto") == "fullauto"
        rpm = java.lang.Double.parseDouble(type.resolveProperty("roundsPerMinute", "60.0"))
        soundName = type.resolveProperty("fireSound", "sounds/weapons/ak47/shoot_old.ogg")

        damage = java.lang.Double.parseDouble(type.resolveProperty("damage", "1.0"))
        accuracy = java.lang.Double.parseDouble(type.resolveProperty("accuracy", "0.0"))
        range = java.lang.Double.parseDouble(type.resolveProperty("range", "1000.0"))
        soundRange = java.lang.Double.parseDouble(type.resolveProperty("soundRange", "1000.0"))

        reloadCooldown = java.lang.Long.parseLong(type.resolveProperty("reloadCooldown", "150"))

        shots = Integer.parseInt(type.resolveProperty("shots", "1"))
        shake = java.lang.Double.parseDouble(type.resolveProperty("shake", (accuracy / 4.0).toString() + ""))

        isScopedWeapon = type.resolveProperty("scoped", "false") == "true"
        scopeZoom = java.lang.Float.parseFloat(type.resolveProperty("scopeZoom", "2.0"))
        scopeSlow = java.lang.Float.parseFloat(type.resolveProperty("scopeSlow", "2.0"))

        scopeTexture = type.resolveProperty("scopeTexture", "./textures/gui/scope.png")

        holdingAnimationName = type.resolveProperty("holdingAnimationName", "./animations/human/holding-rifle.bvh")
        shootingAnimationName = type.resolveProperty("shootingAnimationName",
                "./animations/human/human_shoot_pistol.bvh")

        shootingAnimationDuration = java.lang.Long.parseLong(type.resolveProperty("shootingAnimationDuration", "200"))
    }

    /*
	public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer) {
		ItemRenderer itemRenderer;

		String modelName = getDefinition().resolveProperty("modelObj", "none");
		if (!modelName.equals("none"))
			itemRenderer = new ItemModelRenderer(this, fallbackRenderer, modelName,
					getDefinition().resolveProperty("modelDiffuse", "none"));
		else
			itemRenderer = new FlatIconItemRenderer(this, fallbackRenderer, getDefinition());

		if (scopedWeapon)
			itemRenderer = new ScopedWeaponItemRenderer(itemRenderer);

		return itemRenderer;
	}*/

    /** Displays a scope sometimes  */
    /*class ScopedWeaponItemRenderer extends ItemRenderer {
		ItemRenderer actualRenderer;

		public ScopedWeaponItemRenderer(ItemRenderer itemRenderer) {
			super(null);
			this.actualRenderer = itemRenderer;
		}

		@Override
		public void renderItemInInventory(RenderingInterface renderingInterface, ItemPile pile, float screenPositionX,
										  float screenPositionY, int scaling) {
			actualRenderer.renderItemInInventory(renderingInterface, pile, screenPositionX, screenPositionY, scaling);
		}

		@Override
		public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world,
									  Location location, Matrix4f handTransformation) {
			if (pile.getInventory() != null) {
				if (pile.getInventory().getHolder() != null) {
					Entity clientEntity = renderingInterface.getClient().getPlayer().getControlledEntity();
					ItemFirearm item = (ItemFirearm) pile.getItem();

					if (item.isScoped() && clientEntity.equals(pile.getInventory().getHolder()))
						return;
				}
			}
			actualRenderer.renderItemInWorld(renderingInterface, pile, world, location, handTransformation);
		}
	}*/

    /**
     * Should be called when the owner has this item selected
     *
     * @param owner
     */
    override fun tickInHand(owner: Entity, itemPile: ItemPile) {
        val controller = owner.traits[TraitControllable::class]?.controller

        // For now only client-side players can trigger shooting actions
        if (controller is LocalPlayer) {
            if (!controller.hasFocus())
                return

            val player = controller as LocalPlayer?

            if (player!!.inputsManager.getInputByName("mouse.left")!!.isPressed) {
                // Check for bullet presence (or creative mode)
                val bulletPresence = checkBullet(itemPile) || owner.traits[TraitCreativeMode::class]?.get() == true
                if (!bulletPresence && !wasTriggerPressedLastTick) {
                    // Play sounds
                    owner.world.soundManager.playSoundEffect("sounds/dogez/weapon/default/dry.ogg",
                            Mode.NORMAL, owner.location, 1.0f, 1.0f, 1f, soundRange.toFloat())
                } else if ((automatic || !wasTriggerPressedLastTick) && (System.currentTimeMillis() - lastShot) / 1000.0 > 1.0 / (rpm / 60.0)) {
                    // Fire virtual input
                    player.inputsManager
                            .onInputPressed(controller.inputsManager.getInputByName("shootGun")!!)

                    lastShot = System.currentTimeMillis()
                }
            }

            isScoped = this.isScopedWeapon && controller.inputsManager.getInputByName("mouse.right")!!.isPressed
            wasTriggerPressedLastTick = controller.inputsManager.getInputByName("mouse.left")!!.isPressed
        }
    }

    override fun onControllerInput(entity: Entity, pile: ItemPile, input: Input, controller: Controller): Boolean {
        // Don't do anything with the left mouse click
        if (input.name.startsWith("mouse.")) {
            return true
        }

        val world = entity.world
        if (input.name == "shootGun") {
            // Serverside checks
            // if (user.getWorld() instanceof WorldMaster)
            run {
                // Is the reload cooldown done
                if (cooldownEnd > System.currentTimeMillis())
                    return false

                val inCreativeMode = entity.traits[TraitCreativeMode::class]?.get() == true

                // Do we have any bullets to shoot
                val bulletPresence = checkBullet(pile) || inCreativeMode
                if (!bulletPresence) {
                    // Dry.ogg
                    return true
                } else if (!inCreativeMode) {
                    consumeBullet(pile)
                }
            }

            // Jerk client view a bit
            if (entity.world is WorldClient) {
                entity.traits[TraitRotation::class]?.applyInpulse(shake * (Math.random() - 0.5) * 3.0, shake * -(Math.random() - 0.25) * 5.0)
            }

            // Play sounds
            world.soundManager.playSoundEffect(this.soundName, Mode.NORMAL, entity.location, 1.0f, 1.0f, 1.0f, soundRange.toFloat())

            playAnimation()

            // Raytrace shot
            val eyeLocation = Vector3d(entity.location)
            eyeLocation.y += entity.traits[TraitEyeLevel::class]?.eyeLevel ?: 0.0

            val shooterDirection = entity.traits[TraitRotation::class]?.directionLookingAt ?: return false

            // For each shot
            for (ss in 0 until shots) {
                val direction = Vector3d(shooterDirection!!)
                direction.normalize()

                // Find wall collision
                var shotBlock = entity.world.collisionsManager.raytraceSolid(eyeLocation, direction, range)
                var nearestLocation: Vector3dc? = null

                // Loops to try and break blocks
                var brokeLastBlock = false
                while (entity.world is WorldMaster && shotBlock != null) {
                    val peek = entity.world.peekSafely(shotBlock)
                    // int data = peek.getData();
                    val voxel = peek.voxel

                    brokeLastBlock = false
                    if (!voxel!!.isAir() && voxel.voxelMaterial.resolveProperty("bulletBreakable") != null
                            && voxel.voxelMaterial.resolveProperty("bulletBreakable") == "true") {
                        // TODO Spawn an event to check if it's okay

                        // Destroy it
                        peek.voxel = voxel.store.air
                        //peek.setVoxel(voxel.store().air())

                        brokeLastBlock = true
                        ItemMeleeWeapon.spawnDebris(entity, direction, shotBlock)
                        entity.world.soundManager.playSoundEffect("sounds/environment/glass.ogg", Mode.NORMAL,
                                shotBlock, Math.random().toFloat() * 0.2f + 0.9f, 1.0f)

                        // Re-raytrace the ray
                        shotBlock = entity.world.collisionsManager.raytraceSolid(eyeLocation, direction, range)
                    } else
                        break
                }

                // Spawn decal and particles on block the bullet embedded itself in
                if (shotBlock != null && !brokeLastBlock) {
                    val shotBlockOuter = entity.world.collisionsManager.raytraceSolidOuter(eyeLocation,
                            direction, range)

                    if (shotBlockOuter != null) {
                        val normal = shotBlockOuter.sub(shotBlock)

                        val NbyI2x = 2.0 * direction.dot(normal)
                        val NxNbyI2x = Vector3d(normal)
                        NxNbyI2x.mul(NbyI2x)

                        val reflected = Vector3d(direction)
                        reflected.sub(NxNbyI2x)
                        // Vector3d.sub(direction, NxNbyI2x, reflected);

                        // shotBlock.setX(shotBlock.getX() + 1);

                        val peek = entity.world.peekSafely(shotBlock)

                        // int data = user.getWorld().getVoxelData(shotBlock);
                        // Voxel voxel = VoxelsStore.get().getVoxelById(data);

                        // This seems fine

                        for (box in peek.translatedCollisionBoxes!!) {
                            val thisLocation = box.lineIntersection(eyeLocation, direction)
                            if (thisLocation != null) {
                                if (nearestLocation == null || nearestLocation.distance(eyeLocation) > thisLocation.distance(eyeLocation))
                                    nearestLocation = thisLocation
                            }
                        }

                        val particleSpawnPosition = Vector3d(nearestLocation!!)

                        // Position adjustements so shot blocks always shoot proper particles
                        if (shotBlock.x() - particleSpawnPosition.x() <= -1.0)
                            particleSpawnPosition.add(-0.01, 0.0, 0.0)
                        if (shotBlock.y() - particleSpawnPosition.y() <= -1.0)
                            particleSpawnPosition.add(0.0, -0.01, 0.0)
                        if (shotBlock.z() - particleSpawnPosition.z() <= -1.0)
                            particleSpawnPosition.add(0.0, 0.0, -0.01)

                        for (i in 0..24) {
                            val untouchedReflection = Vector3d(reflected)

                            val random = Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0,
                                    Math.random() * 2.0 - 1.0)
                            random.mul(0.5)
                            untouchedReflection.add(random)
                            untouchedReflection.normalize()

                            untouchedReflection.mul(0.25)

                            //TODO world.getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", particleSpawnPosition, untouchedReflection);
                        }

                        world.soundManager.playSoundEffect(
                                peek.voxel!!.voxelMaterial.resolveProperty("landingSounds"), Mode.NORMAL,
                                particleSpawnPosition, 1f, 0.05f)
                        world.decalsManager.add(nearestLocation, normal.negate(), Vector3d(0.5), "bullethole")
                    }
                }

                // Hitreg takes place on server bois
                if (entity.world is WorldMaster) {
                    // Iterate over each found entities
                    val shotEntities = entity.world.collisionsManager.rayTraceEntities(eyeLocation,
                            direction, 256.0)
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

// System.out.println("shot" + hitBox.getName());

                                    // Deal damage
                                    health.damage(pileAsDamageCause(pile), hitBox, damage.toFloat())

                                    // Spawn blood particles
                                    val bloodDir = direction.normalize().mul(0.75)
                                    for (i in 0..119) {
                                        val random = Vector3d(Math.random() * 2.0 - 1.0,
                                                Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0)
                                        random.mul(0.25)
                                        random.add(bloodDir)

                                        //TODO entity.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("blood", hitPoint, random);
                                    }

                                    // Spawn blood on walls
                                    if (nearestLocation != null)
                                        entity.world.decalsManager.add(nearestLocation, bloodDir,
                                                Vector3d(Math.min(3, shots) * damage / 20f), "blood")
                                }
                            }
                        }
                    }
                }

            }

            //TODO world.getParticlesManager().spawnParticleAtPosition("muzzle", eyeLocation);

            val event = FirearmShotEvent(this, entity, controller)
            entity.world.gameContext.pluginManager.fireEvent(event)

            return entity.world is WorldMaster

        }
        return false
    }

    private fun checkBullet(weaponInstance: ItemPile): Boolean {
        if (currentMagazine == null)
            if (!findMagazine(weaponInstance))
                return false

        if (currentMagazine!!.amount <= 0) {
            currentMagazine = null
            return false
        }

        return true

    }

    private fun consumeBullet(weaponInstance: ItemPile) {
        assert(currentMagazine != null)

        currentMagazine!!.amount = currentMagazine!!.amount - 1

        if (currentMagazine!!.amount <= 0) {
            currentMagazine!!.inventory.setItemAt(currentMagazine!!.x, currentMagazine!!.y, null, 0, false)
            currentMagazine = null

            // Set reload cooldown
            if (findMagazine(weaponInstance))
                cooldownEnd = System.currentTimeMillis() + this.reloadCooldown
        }
    }

    private fun findMagazine(weaponInstance: ItemPile): Boolean {
        val inventory = weaponInstance.inventory
        for (pile in weaponInstance.inventory.contents) {
            if (pile != null && pile.item is ItemFirearmMagazine) {
                val magazineItem = pile.item as ItemFirearmMagazine
                if (magazineItem.isSuitableFor(this) && pile.amount > 0) {
                    currentMagazine = pile
                    break
                }
            }
        }
        return currentMagazine != null
    }

    override fun drawItemOverlay(drawer: GuiDrawer, pile: ItemPile) {
        val holder = pile.inventory.owner
        if (holder is Entity) {
            val controller = holder.traits[TraitControllable::class]?.controller
            if (controller is LocalPlayer) {
                if (isScoped)
                    drawScope(drawer)

                if (cooldownEnd > System.currentTimeMillis()) {
                    val reloadText = "Reloading weapon, please wait"

                    val font = drawer.fonts.defaultFont()
                    val cooldownLength = font.getWidth(reloadText)
                    drawer.drawString(-cooldownLength + drawer.gui.viewportWidth / 2, drawer.gui.viewportHeight / 2, reloadText)
                }
            }
        }
    }

    private fun drawScope(drawer: GuiDrawer) {
        // Temp, rendering interface should provide us
        val min = Math.min(drawer.gui.viewportWidth, drawer.gui.viewportHeight)
        val max = Math.max(drawer.gui.viewportWidth, drawer.gui.viewportHeight)

        val bandwidth = (max - min) / 2
        var x = 0

        x += bandwidth
        drawer.drawBox(x, 0, x, drawer.gui.viewportHeight, Vector4f(0.0f, 0.0f, 0.0f, 1.0f))
        x += min
        drawer.drawBox(x, 0, x, drawer.gui.viewportHeight, scopeTexture)
        x += bandwidth
        drawer.drawBox(x, 0, x, drawer.gui.viewportHeight, Vector4f(0.0f, 0.0f, 0.0f, 1.0f))
    }

    override val zoomFactor: Float
        get() = if (isScoped) scopeZoom else 1f

    fun playAnimation() {
        this.animationStart = System.currentTimeMillis()
        this.animationCooldownEnd = this.animationStart + this.shootingAnimationDuration
    }

    override val customAnimationName: String
        get() = if (this.animationCooldownEnd > System.currentTimeMillis()) this.shootingAnimationName else holdingAnimationName

    override fun transformAnimationTime(originalTime: Double): Double {
        return if (this.animationCooldownEnd > System.currentTimeMillis()) {
            (System.currentTimeMillis() - this.animationStart).toDouble()
        } else originalTime
    }
}
