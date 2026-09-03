package com.github.joelmaykon94.latex.compiler

import com.github.joelmaykon94.latex.editor.LatexEditorWithPreview
import com.github.joelmaykon94.latex.editor.LatexPreviewFileEditor
import com.github.joelmaykon94.latex.lang.LatexFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.PROJECT)
class LatexAutoCompileService(
    private val project: Project,
    private val cs: CoroutineScope
) : Disposable {

    private val compileRequests = MutableSharedFlow<VirtualFile>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        setupDocumentListener()
        setupDebouncedPipeline()
    }

    private fun setupDocumentListener() {
        val multicaster = EditorFactory.getInstance().eventMulticaster
        multicaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val doc = event.document
                val file = FileDocumentManager.getInstance().getFile(doc) ?: return
                if (isLatexFile(file) && file.isInLocalFileSystem) {
                    compileRequests.tryEmit(file)
                }
            }
        }, this)
    }

    private fun isLatexFile(file: VirtualFile): Boolean {
        return file.fileType == LatexFileType.INSTANCE ||
               file.extension?.lowercase() in listOf("tex", "sty", "cls")
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedPipeline() {
        cs.launch {
            compileRequests
                .debounce(800.milliseconds)
                .collect { file ->
                    triggerCompilation(file)
                }
        }
    }

    private fun triggerCompilation(file: VirtualFile) {
        val ioFile = File(file.path)
        val outputDir = ioFile.parentFile ?: return

        // Save document to disk
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        // Execute compiler in background
        LatexCompiler.compile(
            project = project,
            texFile = ioFile,
            outputDir = outputDir,
            onSuccess = { pdfFile -> updatePreviewPanels(file, pdfFile) },
            onError = { errorLog -> showErrorInPanels(file, errorLog) }
        )
    }

    private fun updatePreviewPanels(texFile: VirtualFile, pdfFile: File) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editors = fileEditorManager.getAllEditors(texFile)
        for (editor in editors) {
            when (editor) {
                is LatexPreviewFileEditor -> editor.previewPanel.updatePdf(pdfFile)
                is LatexEditorWithPreview -> {
                    val preview = editor.previewEditor
                    if (preview is LatexPreviewFileEditor) {
                        preview.previewPanel.updatePdf(pdfFile)
                    }
                }
            }
        }
    }

    private fun showErrorInPanels(texFile: VirtualFile, errorLog: String) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editors = fileEditorManager.getAllEditors(texFile)
        for (editor in editors) {
            when (editor) {
                is LatexPreviewFileEditor -> editor.previewPanel.showError(errorLog)
                is LatexEditorWithPreview -> {
                    val preview = editor.previewEditor
                    if (preview is LatexPreviewFileEditor) {
                        preview.previewPanel.showError(errorLog)
                    }
                }
            }
        }
    }

    override fun dispose() {}

    companion object {
        fun getInstance(project: Project): LatexAutoCompileService = project.service()
    }
}
