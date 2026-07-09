/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.project.ProjectManager

/**
 * Re-runs the highlighting passes of every open project when the switch flips,
 * which is what makes every rendered command appear or disappear. All four
 * renderings now live in passes, so this is all it takes.
 */
class RenderingSettingsRefresher : RenderingSettingsListener {

    override fun smartRenderingChanged(smartRendering: Boolean) {
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { DaemonCodeAnalyzer.getInstance(it).restart() }
    }
}
