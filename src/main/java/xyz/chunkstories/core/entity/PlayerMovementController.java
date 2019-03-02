package xyz.chunkstories.core.entity;

import xyz.chunkstories.api.client.LocalPlayer;
import xyz.chunkstories.api.entity.traits.TraitCollidable;
import xyz.chunkstories.core.entity.traits.TraitControlledMovement;
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance;

class PlayerMovementController extends TraitControlledMovement {

	final EntityPlayer entityPlayer;

	public PlayerMovementController(EntityPlayer entity) {
		super(entity);
		entityPlayer = entity;
	}

	@Override
	public void tick(LocalPlayer controller) {
		if (entityPlayer.flyMode.get()) {
			// Delegate movement handling to the fly mode component
			entityPlayer.flyMode.tick(controller);

			// Flying also means we're standing
			entityPlayer.stance.set(TraitHumanoidStance.HumanoidStance.STANDING);
		} else {

			boolean focus = controller.hasFocus();
			if (focus && entityPlayer.traits.get(TraitCollidable.class).isOnGround()) {
				if (controller.getInputsManager().getInputByName("crouch").isPressed())
					entityPlayer.stance.set(TraitHumanoidStance.HumanoidStance.CROUCHING);
				else
					entityPlayer.stance.set(TraitHumanoidStance.HumanoidStance.STANDING);
			}

			super.tick(controller);

			// if(focus)
			// traits.with(MinerTrait.class, mt -> mt.tickTrait());
		}

		// TODO check if this is needed
		// Instead of creating a packet and dealing with it ourselves, we instead push
		// the relevant components
		entityPlayer.traitLocation.pushComponentEveryoneButController();
		// In that case that means pushing to the server.
	}

	@Override
	public double getForwardSpeed() {
		return ((!running || entityPlayer.stance.getStance() == TraitHumanoidStance.HumanoidStance.CROUCHING) ? 0.06 : 0.09);
	}

	@Override
	public double getBackwardsSpeed() {
		return 0.05;
	}
}
