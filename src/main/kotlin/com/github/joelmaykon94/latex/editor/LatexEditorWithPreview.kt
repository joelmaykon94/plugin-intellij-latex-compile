package com.github.joelmaykon94.latex.editor

import com.github.joelmaykon94.latex.compiler.LatexAutoCompileService
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.Project

class LatexEditorWithPreview(
    private val project: Project,
    textEditor: TextEditor,
    previewEditor: LatexPreviewFileEditor
) : TextEditorWithPreview(
    textEditor,
    previewEditor,
    "LaTeX Split Editor",
    Layout.SHOW_EDITOR_AND_PREVIEW
) {
    init {
        // Ensure auto-compile service is active for this project
        LatexAutoCompileService.getInstance(project)
        // Trigger initial compilation when editor opens
        previewEditor.previewPanel.triggerInitialCompilation()
    }
}
