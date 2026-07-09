/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.settings

import com.intellij.util.messages.Topic

/**
 * Broadcast whenever the smart-rendering switch flips.
 *
 * Every future rendering feature of this plugin should listen here rather than
 * poll the settings, so that a single toggle refreshes all of them at once.
 */
interface RenderingSettingsListener {

    fun smartRenderingChanged(smartRendering: Boolean)

    companion object {
        @JvmField
        val TOPIC: Topic<RenderingSettingsListener> =
            Topic.create("BookLaTeX rendering settings", RenderingSettingsListener::class.java)
    }
}
