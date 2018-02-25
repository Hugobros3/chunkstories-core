//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4fc;

import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.world.WorldRenderer.RenderingPass;
import io.xol.chunkstories.api.world.WorldClient;
	
public class CachedLodSkeletonAnimator implements SkeletonAnimator
{
	final Entity entity;
	final SkeletonAnimator dataSource;
	final double lodStart;
	final double lodEnd;
	
	Map<String, CachedData> cachedBones = new HashMap<String, CachedData>();

	public CachedLodSkeletonAnimator(Entity entity, SkeletonAnimator dataSource, double lodStart, double lodEnd)
	{
		this.entity = entity;
		this.dataSource = dataSource;
		this.lodStart = lodStart;
		this.lodEnd = lodEnd;
	}

	public void lodUpdate(RenderingInterface renderingContext)
	{
		double distance = entity.getLocation().distance(renderingContext.getCamera().getCameraPosition());
		double targetFps = renderingContext.renderingConfig().getAnimationCacheFrameRate();

		int lodDivisor = 1;
		if (distance > lodStart)
		{
			lodDivisor *= 4;
			if (distance > lodEnd)
				lodDivisor *= 4;
		}
		if (renderingContext.getWorldRenderer().getCurrentRenderingPass() == RenderingPass.SHADOW)
			lodDivisor *= 2;

		targetFps /= lodDivisor;

		double maxMsDiff = 1000.0 / targetFps;
		long time = System.currentTimeMillis();

		for (CachedData cachedData : cachedBones.values())
		{
			if (time - cachedData.lastUpdate > maxMsDiff)
				cachedData.needsUpdate = true;
		}
	}

	class CachedData
	{
		Matrix4fc matrix = null;
		long lastUpdate = -1;

		boolean needsUpdate = false;

		CachedData(Matrix4fc matrix, long lastUpdate)
		{
			super();
			this.matrix = matrix;
			this.lastUpdate = lastUpdate;
		}
	}

	@Override
	public Matrix4fc getBoneHierarchyTransformationMatrix(String nameOfEndBone, double animationTime)
	{
		return dataSource.getBoneHierarchyTransformationMatrix(nameOfEndBone, animationTime);
	}

	@Override
	public Matrix4fc getBoneHierarchyTransformationMatrixWithOffset(String nameOfEndBone, double animationTime)
	{
		//Don't mess with the player animation, it should NEVER be cached
		if (entity.getWorld() instanceof WorldClient && ((WorldClient)entity.getWorld()).getClient() != null && ((WorldClient)entity.getWorld()).getClient().getPlayer().getControlledEntity() == entity)
			return dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);

		CachedData cachedData = cachedBones.get(nameOfEndBone);
		//If the matrix exists and doesn't need an update
		if (cachedData != null && !cachedData.needsUpdate)
		{
			cachedData.needsUpdate = false;
			return cachedData.matrix;
		}

		//Obtains the matrix and caches it
		Matrix4fc matrix = dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);
		cachedBones.put(nameOfEndBone, new CachedData(matrix, System.currentTimeMillis()));

		return matrix;
	}

	public boolean shouldHideBone(RenderingInterface renderingContext, String boneName)
	{
		return dataSource.shouldHideBone(renderingContext, boneName);
	}

}
