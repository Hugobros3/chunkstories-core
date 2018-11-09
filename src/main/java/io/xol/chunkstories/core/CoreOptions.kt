package io.xol.chunkstories.core

import io.xol.chunkstories.api.util.configuration.OptionsDeclarationCtx

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