//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core

import xyz.chunkstories.api.util.configuration.OptionsDeclarationCtx

object CoreOptions {
	// We use those fields to capture the fully-qualified option name so we can't make spelling errors
	lateinit var mouseSensitivity: String private set

	val options: OptionsDeclarationCtx.() -> Unit = {
		section("client" ) {
			section("input") {

				mouseSensitivity = optionRangeDouble("mouseSensitivity") {
					minimumValue = 0.5
					maximumValue = 2.0
					default = 1.0
					granularity = 0.05
				}
			}
		}
	}
}