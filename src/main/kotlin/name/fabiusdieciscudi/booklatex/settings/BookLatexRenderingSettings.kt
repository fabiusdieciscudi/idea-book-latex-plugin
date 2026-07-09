/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import name.fabiusdieciscudi.booklatex.ui.LatexEditorColorScheme
import name.fabiusdieciscudi.booklatex.ui.RenderFonts

/**
 * Application-wide switch between smart rendering and the plain source view.
 * Persisted in bookLatex.xml under the IDE config directory.
 */
@Service(Service.Level.APP)
@State(name = "BookLatexRenderingSettings", storages = [Storage("bookLatex.xml")])
class BookLatexRenderingSettings :
    SimplePersistentStateComponent<BookLatexRenderingSettings.State>(State()) {

    class State : BaseState() {
        var smartRendering: Boolean by property(true)
        var latexColorScheme: Boolean by property(true)
        var latexColorSchemeName: String? by string(LatexEditorColorScheme.DEFAULT_SCHEME_NAME)
        var renderFontFamily: String? by string(RenderFonts.DEFAULT_FAMILY)
    }

    var smartRendering: Boolean
        get() = state.smartRendering
        set(value) {
            if (state.smartRendering == value) return
            state.smartRendering = value
            ApplicationManager.getApplication().messageBus
                .syncPublisher(RenderingSettingsListener.TOPIC)
                .smartRenderingChanged(value)
        }

    /**
     * Whether LaTeX editors get their own colour scheme. Not broadcast on the
     * topic: nothing needs re-rendering, only the open editors need repainting,
     * and the settings page does that itself.
     */
    var latexColorScheme: Boolean
        get() = state.latexColorScheme
        set(value) {
            state.latexColorScheme = value
        }

    /** The scheme to use in LaTeX editors, by the name Settings shows. */
    var latexColorSchemeName: String
        get() = state.latexColorSchemeName ?: LatexEditorColorScheme.DEFAULT_SCHEME_NAME
        set(value) {
            state.latexColorSchemeName = value
        }

    /** The family used to paint the sticky notes and the attribute strips. */
    var renderFontFamily: String
        get() = state.renderFontFamily ?: RenderFonts.DEFAULT_FAMILY
        set(value) {
            state.renderFontFamily = value
        }

    companion object {
        fun getInstance(): BookLatexRenderingSettings = service()
    }
}
