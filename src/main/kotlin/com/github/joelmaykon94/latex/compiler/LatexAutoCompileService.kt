package com.github.joelmaykon94.latex.compiler

import com.github.joelmaykon94.latex.editor.LatexEditorWithPreview
import com.github.joelmaykon94.latex.editor.LatexPreviewFileEditor
import com.github.joelmaykon94.latex.lang.LatexFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
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

    private val fileLocks = ConcurrentHashMap<String, Mutex>()

    init {
        setupDocumentListener()
        setupSaveListener()
        setupDebouncedPipeline()
    }

    private fun setupDocumentListener() {
        val multicaster = EditorFactory.getInstance().eventMulticaster
        multicaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (project.isDisposed) return
                val doc = event.document
                val file = FileDocumentManager.getInstance().getFile(doc) ?: return
                if (isLatexFile(file) && file.isInLocalFileSystem) {
                    compileRequests.tryEmit(file)
                }
            }
        }, this)
    }

    private fun setupSaveListener() {
        project.messageBus.connect(this).subscribe(
            FileDocumentManagerListener.TOPIC,
            object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: Document) {
                    if (project.isDisposed) return
                    val file = FileDocumentManager.getInstance().getFile(document) ?: return
                    if (isLatexFile(file) && file.isInLocalFileSystem) {
                        compileRequests.tryEmit(file)
                    }
                }
            }
        )
    }

    private fun isLatexFile(file: VirtualFile): Boolean {
        return file.fileType == LatexFileType.INSTANCE ||
               file.extension?.lowercase() in listOf("tex", "sty", "cls")
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedPipeline() {
        cs.launch {
            compileRequests
                .debounce(600.milliseconds)
                .collect { file ->
                    if (!project.isDisposed) {
                        triggerCompilation(file)
                    }
                }
        }
    }

    private fun triggerCompilation(file: VirtualFile) {
        val ioFile = File(file.path)
        val outputDir = ioFile.parentFile ?: return

        // Save document to disk on EDT
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) {
            val doc = FileDocumentManager.getInstance().getDocument(file)
            if (doc != null) {
                FileDocumentManager.getInstance().saveDocument(doc)
            } else {
                FileDocumentManager.getInstance().saveAllDocuments()
            }
        } else {
            app.invokeAndWait({
                val doc = FileDocumentManager.getInstance().getDocument(file)
                if (doc != null) {
                    FileDocumentManager.getInstance().saveDocument(doc)
                } else {
                    FileDocumentManager.getInstance().saveAllDocuments()
                }
            }, ModalityState.defaultModalityState())
        }

        // Execute compiler with per-file mutex to avoid concurrent latexmk clashes
        val mutex = fileLocks.computeIfAbsent(file.path) { Mutex() }
        cs.launch {
            mutex.withLock {
                LatexCompiler.compile(
                    project = project,
                    texFile = ioFile,
                    outputDir = outputDir,
                    onSuccess = { pdfFile -> updatePreviewPanels(file, pdfFile) },
                    onError = { errorLog -> showErrorInPanels(file, errorLog) }
                )
            }
        }
    }

    private fun updatePreviewPanels(texFile: VirtualFile, pdfFile: File) {
        if (project.isDisposed) return
        val fileEditorManager = FileEditorManager.getInstance(project)
        for (editor in fileEditorManager.allEditors) {
            when (editor) {
                is LatexPreviewFileEditor -> {
                    if (editor.file == texFile || editor.file.path == texFile.path) {
                        editor.previewPanel.updatePdf(pdfFile)
                    }
                }
                is LatexEditorWithPreview -> {
                    val preview = editor.previewEditor
                    if (preview is LatexPreviewFileEditor && (editor.file == texFile || editor.file?.path == texFile.path)) {
                        preview.previewPanel.updatePdf(pdfFile)
                    }
                }
            }
        }
    }

    private fun showErrorInPanels(texFile: VirtualFile, errorLog: String) {
        if (project.isDisposed) return
        val fileEditorManager = FileEditorManager.getInstance(project)
        for (editor in fileEditorManager.allEditors) {
            when (editor) {
                is LatexPreviewFileEditor -> {
                    if (editor.file == texFile || editor.file.path == texFile.path) {
                        editor.previewPanel.showError(errorLog)
                    }
                }
                is LatexEditorWithPreview -> {
                    val preview = editor.previewEditor
                    if (preview is LatexPreviewFileEditor && (editor.file == texFile || editor.file?.path == texFile.path)) {
                        preview.previewPanel.showError(errorLog)
                    }
                }
            }
        }
    }

    override fun dispose() {
        fileLocks.clear()
    }

    companion object {
        fun getInstance(project: Project): LatexAutoCompileService = project.service()
    }
}
