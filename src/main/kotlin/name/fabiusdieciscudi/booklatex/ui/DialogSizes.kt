/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Toolkit

/**
 * Sizes a dialog against the window it opens over.
 *
 * Fixed pixels look right on one screen only, and are not scaled by the IDE's
 * own size factor. A fraction of the main window travels better, with a floor
 * under it: a third of a small window is not a usable dialog.
 *
 * Only the first opening uses this. A dialog that declares a dimension service
 * key has its geometry restored afterwards, and the platform ignores the
 * preferred size it was given.
 */
object DialogSizes {

    const val THIRD: Double = 1.0 / 3.0

    private val MIN_WIDTH: Int get() = JBUI.scale(420)
    private val MIN_HEIGHT: Int get() = JBUI.scale(220)

    fun fractionOfMainWindow(project: Project?, widthFraction: Double, heightFraction: Double): Dimension {
        val window = mainWindowSize(project)
        return Dimension(
            (window.width * widthFraction).toInt().coerceAtLeast(MIN_WIDTH),
            (window.height * heightFraction).toInt().coerceAtLeast(MIN_HEIGHT),
        )
    }

    fun widthFractionOfMainWindow(project: Project?, fraction: Double): Int =
        (mainWindowSize(project).width * fraction).toInt().coerceAtLeast(MIN_WIDTH)

    /** The screen, when there is no frame to measure -- tests, headless runs. */
    private fun mainWindowSize(project: Project?): Dimension {
        val frame = project?.takeUnless { it.isDisposed }?.let { WindowManager.getInstance().getFrame(it) }
        val size = frame?.size
        if (size != null && size.width > 0 && size.height > 0) return size
        return Toolkit.getDefaultToolkit().screenSize
    }
}
