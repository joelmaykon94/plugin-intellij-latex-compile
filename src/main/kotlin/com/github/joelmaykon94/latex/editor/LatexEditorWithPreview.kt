package com.github.joelmaykon94.latex.editor

import com.github.joelmaykon94.latex.compiler.LatexAutoCompileService
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.Project

class LatexEditorWithPreview(
    private val project: Project,
    editor: TextEditor,
    previewEditor: LatexPreviewFileEditor
) : TextEditorWithPreview(
    editor,
    previewEditor,
    "LaTeX Split Editor",
    Layout.SHOW_EDITOR_AND_PREVIEW
) {

    private val visibleAreaListener = VisibleAreaListener { event: VisibleAreaEvent ->
        val newRect = event.newRectangle
        val oldRect = event.oldRectangle
        if (newRect.y == oldRect.y && newRect.height == oldRect.height) return@VisibleAreaListener

        val previewPanel = previewEditor.previewPanel
        if (!previewPanel.isScrollSyncEnabled) return@VisibleAreaListener

        val ed = this.editor
        val lineHeight = ed.lineHeight
        val totalLines = ed.document.lineCount.coerceAtLeast(1)
        val firstVisibleLine = if (lineHeight > 0) (newRect.y / lineHeight) + 1 else 1
        val ratio = LatexScrollSyncCoordinator.calculateRatioFromScroll(
            scrollY = newRect.y,
            contentHeight = ed.contentComponent.height,
            viewportHeight = newRect.height
        )
        previewPanel.scrollToRatio(ratio, firstVisibleLine, totalLines)
    }

    init {
        // Ensure auto-compile service is active for this project
        LatexAutoCompileService.getInstance(project)
        // Trigger initial compilation when editor opens
        previewEditor.previewPanel.triggerInitialCompilation()

        // Attach visible area listener for LaTeX code scroll synchronization
        this.editor.scrollingModel.addVisibleAreaListener(visibleAreaListener)
    }

    override fun dispose() {
        try {
            this.editor.scrollingModel.removeVisibleAreaListener(visibleAreaListener)
        } catch (_: Exception) {
            // Ignored during disposal
        }
        super.dispose()
    }
}
