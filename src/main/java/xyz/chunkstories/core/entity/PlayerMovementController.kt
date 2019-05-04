package xyz.chunkstories.core.entity

import xyz.chunkstories.api.client.LocalPlayer
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.core.entity.traits.TraitControlledMovement
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance

internal class PlayerMovementController(val entityPlayer: EntityPlayer) : TraitControlledMovement(entityPlayer) {

    override val forwardSpeed: Double
        get() = if (!running || entityPlayer.traitStance.stance === TraitHumanoidStance.HumanoidStance.CROUCHING) 0.06 else 0.09

    override val backwardsSpeed: Double
        get() = 0.05

    override fun tick(controller: LocalPlayer) {
        if (entityPlayer.traitFlyingMode.get()) {
            // Delegate movement handling to the fly mode component
            entityPlayer.traitFlyingMode.tick(controller)

            // Flying also means we're standing
            entityPlayer.traitStance.set(TraitHumanoidStance.HumanoidStance.STANDING)
        } else {

            val focus = controller.hasFocus()
            if (focus && entityPlayer.traits[TraitCollidable::class.java]!!.isOnGround) {
                if (controller.inputsManager.getInputByName("crouch")!!.isPressed)
                    entityPlayer.traitStance.set(TraitHumanoidStance.HumanoidStance.CROUCHING)
                else
                    entityPlayer.traitStance.set(TraitHumanoidStance.HumanoidStance.STANDING)
            }

            super.tick(controller)

            // if(focus)
            // traits.with(MinerTrait.class, mt -> mt.tickTrait());
        }

        // TODO check if this is needed
        // Instead of creating a packet and dealing with it ourselves, we instead push
        // the relevant components
        entityPlayer.traitLocation.pushComponentEveryoneButController()
        // In that case that means pushing to the server.
    }
}
