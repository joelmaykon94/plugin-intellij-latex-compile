package com.github.joelmaykon94.latex.editor

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview

class LatexEditorWithPreview(
    textEditor: TextEditor,
    previewEditor: LatexPreviewFileEditor
) : TextEditorWithPreview(
    textEditor,
    previewEditor,
    "LaTeX Split Editor",
    Layout.SHOW_EDITOR_AND_PREVIEW
) {
    init {
        // Trigger initial compilation when editor opens
        previewEditor.previewPanel.triggerInitialCompilation()
    }
}
