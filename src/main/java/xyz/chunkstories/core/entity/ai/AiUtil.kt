//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.ai

import org.joml.Vector2f
import org.joml.Vector3d
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation

/** Helper to orient an entity in a certain direction */
fun Entity.lookAt(delta: Vector3d) {
	val deltaHorizontal = Vector2f(delta.x().toFloat(), delta.z().toFloat())
	val deltaVertical = Vector2f(deltaHorizontal.length(), delta.y().toFloat())
	if(deltaHorizontal.length() == 0.0f)
		return

	deltaHorizontal.normalize()
	deltaVertical.normalize()

	var targetH = Math.acos(deltaHorizontal.y().toDouble()) * 180.0 / Math.PI
	var targetV = Math.asin(deltaVertical.y().toDouble()) * 180.0 / Math.PI

	if (deltaHorizontal.x() < 0.0)
		targetH *= -1.0

	if (targetV > 90f)
		targetV = 90.0
	if (targetV < -90f)
		targetV = -90.0

	while (targetH < 0.0)
		targetH += 360.0

	var diffH = targetH - this.traits[TraitRotation::class.java]!!.horizontalRotation

	// Ensures we always take the fastest route
	if (Math.abs(diffH + 360) < Math.abs(diffH))
		diffH = diffH + 360
	else if (Math.abs(diffH - 360) < Math.abs(diffH))
		diffH = diffH - 360

	var diffV = targetV - this.traits[TraitRotation::class.java]!!.verticalRotation

	if (java.lang.Double.isNaN(diffH))
		diffH = 0.0

	if (java.lang.Double.isNaN(diffV))
		diffV = 0.0

	this.traits[TraitRotation::class.java]!!.addRotation(diffH / 15f, diffV / 15f)
}