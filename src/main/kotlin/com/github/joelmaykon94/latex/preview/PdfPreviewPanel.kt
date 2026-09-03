package com.github.joelmaykon94.latex.preview

import com.github.joelmaykon94.latex.compiler.LatexCompiler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import java.awt.BorderLayout
import java.io.File
import java.util.Base64
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class PdfPreviewPanel(
    private val project: Project,
    private val texFile: VirtualFile,
    parentDisposable: Disposable
) : Disposable {

    private val log = logger<PdfPreviewPanel>()
    val component = JPanel(BorderLayout())
    private var browser: JBCefBrowser? = null
    private var jsQuery: JBCefJSQuery? = null

    init {
        if (!JBCefApp.isSupported()) {
            component.add(
                JLabel("JCEF is not supported. PDF preview requires JetBrains Runtime with JCEF.", SwingConstants.CENTER),
                BorderLayout.CENTER
            )
        } else {
            val jbBrowser = JBCefBrowser.createBuilder()
                .setOffScreenRendering(false)
                .build()
            this.browser = jbBrowser
            Disposer.register(this, jbBrowser)

            val query = JBCefJSQuery.create(jbBrowser as JBCefBrowserBase)
            this.jsQuery = query
            Disposer.register(this, query)

            query.addHandler { data ->
                handleInverseSearch(data)
                JBCefJSQuery.Response("OK")
            }

            jbBrowser.loadHTML(getViewerHtml(query))
            component.add(jbBrowser.component, BorderLayout.CENTER)
        }

        Disposer.register(parentDisposable, this)
    }

    fun triggerInitialCompilation() {
        val ioFile = File(texFile.path)
        val outputDir = ioFile.parentFile ?: return

        LatexCompiler.compile(
            project = project,
            texFile = ioFile,
            outputDir = outputDir,
            onSuccess = { pdfFile -> updatePdf(pdfFile) },
            onError = { errorLog ->
                log.warn("Initial compilation failed: $errorLog")
                showError(errorLog)
            }
        )
    }

    fun updatePdf(pdfFile: File) {
        if (browser == null || !pdfFile.exists()) return

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val bytes = pdfFile.readBytes()
                val base64 = Base64.getEncoder().encodeToString(bytes)

                ApplicationManager.getApplication().invokeLater {
                    browser?.cefBrowser?.executeJavaScript(
                        "window.renderPdfFromBase64('$base64');",
                        browser?.cefBrowser?.url ?: "",
                        0
                    )
                }
            } catch (e: Exception) {
                log.error("Failed to load PDF bytes", e)
            }
        }
    }

    fun showError(errorMessage: String) {
        ApplicationManager.getApplication().invokeLater {
            val escaped = errorMessage
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")
            browser?.cefBrowser?.executeJavaScript(
                "window.showCompilationError('$escaped');",
                browser?.cefBrowser?.url ?: "",
                0
            )
        }
    }

    private fun handleInverseSearch(data: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val line = data.toIntOrNull() ?: 1
                val descriptor = OpenFileDescriptor(project, texFile, (line - 1).coerceAtLeast(0), 0)
                descriptor.navigate(true)
            } catch (e: Exception) {
                log.warn("Inverse search jump failed", e)
            }
        }
    }

    private fun getViewerHtml(query: JBCefJSQuery): String {
        val injectJs = query.inject("payload")
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.4.168/pdf.min.mjs" type="module"></script>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              background-color: #1e1e1e;
              color: #d4d4d4;
              font-family: 'JetBrains Mono', 'Consolas', monospace;
              overflow-y: auto;
              padding: 16px;
            }
            #status {
              font-size: 13px;
              color: #888;
              text-align: center;
              padding: 8px;
              position: sticky;
              top: 0;
              background: #1e1e1e;
              z-index: 100;
            }
            #container {
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: 12px;
              padding-bottom: 20px;
            }
            canvas {
              box-shadow: 0 2px 12px rgba(0, 0, 0, 0.6);
              border-radius: 2px;
              max-width: 100%;
              height: auto;
            }
            #error-panel {
              display: none;
              background: #3c1f1f;
              border: 1px solid #ff4444;
              border-radius: 6px;
              padding: 16px;
              margin: 16px;
              font-size: 12px;
              white-space: pre-wrap;
              word-wrap: break-word;
              color: #ff6b6b;
              max-height: 300px;
              overflow-y: auto;
            }
          </style>
        </head>
        <body>
          <div id="status">⏳ Aguardando compilação...</div>
          <div id="error-panel"></div>
          <div id="container"></div>

          <script type="module">
            import * as pdfjsLib from 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.4.168/pdf.min.mjs';
            pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.4.168/pdf.worker.min.mjs';

            let currentPdf = null;
            let currentScale = 1.5;

            window.renderPdfFromBase64 = function(base64Data) {
              document.getElementById('status').innerText = '🔄 Renderizando PDF...';
              document.getElementById('error-panel').style.display = 'none';

              const raw = atob(base64Data);
              const uint8Array = new Uint8Array(raw.length);
              for (let i = 0; i < raw.length; i++) {
                uint8Array[i] = raw.charCodeAt(i);
              }

              pdfjsLib.getDocument({ data: uint8Array }).promise.then(function(pdf) {
                currentPdf = pdf;
                const container = document.getElementById('container');
                container.innerHTML = '';

                const renderPromises = [];
                for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
                  renderPromises.push(
                    pdf.getPage(pageNum).then(function(page) {
                      const viewport = page.getViewport({ scale: currentScale });
                      const canvas = document.createElement('canvas');
                      const context = canvas.getContext('2d');
                      canvas.height = viewport.height;
                      canvas.width = viewport.width;
                      canvas.title = 'Página ' + pageNum;

                      canvas.ondblclick = function() {
                        const payload = pageNum.toString();
                        ${'$'}injectJs
                      };

                      container.appendChild(canvas);
                      return page.render({ canvasContext: context, viewport: viewport }).promise;
                    })
                  );
                }

                Promise.all(renderPromises).then(function() {
                  document.getElementById('status').innerText =
                    '✅ PDF renderizado — ' + pdf.numPages + ' página(s)';
                });
              }).catch(function(err) {
                document.getElementById('status').innerText = '❌ Erro: ' + err.message;
              });
            };

            window.showCompilationError = function(msg) {
              const panel = document.getElementById('error-panel');
              panel.innerText = msg;
              panel.style.display = 'block';
              document.getElementById('status').innerText = '❌ Erro de compilação';
            };
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    override fun dispose() {
        browser = null
        jsQuery = null
    }
}
