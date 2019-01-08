//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.Trait;
import xyz.chunkstories.api.entity.traits.TraitCollidable;
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity;
import xyz.chunkstories.api.sound.SoundSource.Mode;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.materials.VoxelMaterial;
import xyz.chunkstories.api.world.WorldClient;
import xyz.chunkstories.core.voxel.VoxelLiquid;
import org.joml.Vector3d;

public class TraitWalkingSounds extends Trait {

	public double horizontalSpeed = 0;
	public double metersWalked = 0d;

	boolean justJumped = false;
	boolean justLanded = false;

	// boolean running = false;

	private boolean lastTickOnGround;

	public TraitWalkingSounds(Entity entity) {
		super(entity);
	}

	public void handleWalkingEtcSounds() {
		// This is strictly a clientside thing
		if (!(entity.getWorld() instanceof WorldClient))
			return;

		TraitCollidable collisions = entity.traits.get(TraitCollidable.class);
		TraitVelocity entityVelocity = entity.traits.get(TraitVelocity.class);
		TraitBasicMovement locomotion = entity.traits.get(TraitBasicMovement.class);

		if (collisions == null || entityVelocity == null || locomotion == null)
			return;

		// When the entities are too far from the player, don't play any sounds
		if (((WorldClient) entity.getWorld()).getClient().getPlayer().getControlledEntity() != null)
			if (((WorldClient) entity.getWorld()).getClient().getPlayer().getControlledEntity().getLocation()
					.distance(entity.getLocation()) > 25f)
				return;

		// Sound stuff
		if (collisions.isOnGround() && !lastTickOnGround) {
			justLanded = true;
			metersWalked = 0.0;
		}

		// Used to trigger landing sound
		lastTickOnGround = collisions.isOnGround();

		// Bobbing
		Vector3d horizontalSpeed = new Vector3d(entityVelocity.getVelocity());
		horizontalSpeed.y = 0d;

		if (collisions.isOnGround())
			metersWalked += Math.abs(horizontalSpeed.length());

		boolean inWater = locomotion.isInWater();

		Voxel voxelStandingOn = entity.world.peekSafely(new Vector3d(entity.getLocation()).add(0.0, -0.01, 0.0))
				.getVoxel();

		if (voxelStandingOn == null
				|| !voxelStandingOn.isSolid() && !(voxelStandingOn instanceof VoxelLiquid))
			voxelStandingOn = entity.world.getContent().voxels().air();

		VoxelMaterial material = voxelStandingOn.getVoxelMaterial();

		if (justJumped && !inWater) {
			// TODO useless
			justJumped = false;
			entity.getWorld().getSoundManager().playSoundEffect(material.resolveProperty("jumpingSounds"), Mode.NORMAL,
					entity.getLocation(),
					(float) (0.9f + Math.sqrt(entityVelocity.getVelocity().x() * entityVelocity.getVelocity().x()
							+ entityVelocity.getVelocity().z() * entityVelocity.getVelocity().z()) * 0.1f),
					1f).setAttenuationEnd(10);
		}
		if (justLanded) {
			justLanded = false;
			entity.getWorld().getSoundManager().playSoundEffect(material.resolveProperty("landingSounds"), Mode.NORMAL,
					entity.getLocation(),
					(float) (0.9f + Math.sqrt(entityVelocity.getVelocity().x() * entityVelocity.getVelocity().x()
							+ entityVelocity.getVelocity().z() * entityVelocity.getVelocity().z()) * 0.1f),
					1f).setAttenuationEnd(10);
		}

		if (metersWalked > 0.2 * Math.PI * 2) {
			metersWalked %= 0.2 * Math.PI * 2;
			if (horizontalSpeed.length() <= 0.06)
				entity.getWorld().getSoundManager().playSoundEffect(material.resolveProperty("walkingSounds"),
						Mode.NORMAL, entity.getLocation(),
						(float) (0.9f + Math.sqrt(entityVelocity.getVelocity().x() * entityVelocity.getVelocity().x()
								+ entityVelocity.getVelocity().z() * entityVelocity.getVelocity().z()) * 0.1f),
						1f).setAttenuationEnd(10);
			else
				entity.getWorld().getSoundManager().playSoundEffect(material.resolveProperty("runningSounds"),
						Mode.NORMAL, entity.getLocation(),
						(float) (0.9f + Math.sqrt(entityVelocity.getVelocity().x() * entityVelocity.getVelocity().x()
								+ entityVelocity.getVelocity().z() * entityVelocity.getVelocity().z()) * 0.1f),
						1f).setAttenuationEnd(10);

		}
	}
}
