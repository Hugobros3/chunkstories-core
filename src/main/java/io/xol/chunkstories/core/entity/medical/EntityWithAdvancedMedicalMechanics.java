//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity.medical;

import io.xol.chunkstories.api.entity.EntityLiving;

public interface EntityWithAdvancedMedicalMechanics extends EntityLiving
{
	public AdvancedMedicalComponent getAdvancedMedicalComponent();
}
