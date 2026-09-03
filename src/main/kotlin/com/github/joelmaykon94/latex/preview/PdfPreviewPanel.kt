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
                when {
                    data == "recompile" -> triggerInitialCompilation()
                    data.startsWith("page:") -> handleInverseSearch(data.removePrefix("page:"))
                    else -> handleInverseSearch(data)
                }
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
                        """
                        if (window.renderPdfFromBase64) {
                            window.renderPdfFromBase64('$base64');
                        } else {
                            window.pendingBase64Data = '$base64';
                        }
                        """.trimIndent(),
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
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
              overflow-y: auto;
              padding: 0;
            }
            #toolbar {
              position: sticky;
              top: 0;
              background: #252526;
              border-bottom: 1px solid #3c3c3c;
              z-index: 100;
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 6px 14px;
              gap: 10px;
              font-size: 12px;
            }
            #status {
              color: #cccccc;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
            #toolbar-actions {
              display: flex;
              align-items: center;
              gap: 6px;
              flex-shrink: 0;
            }
            button {
              background: #333333;
              color: #e0e0e0;
              border: 1px solid #4a4a4a;
              border-radius: 4px;
              padding: 3px 8px;
              cursor: pointer;
              font-size: 11px;
              display: inline-flex;
              align-items: center;
              gap: 4px;
              transition: background 0.15s;
            }
            button:hover {
              background: #444444;
              border-color: #666666;
            }
            #zoom-val {
              min-width: 42px;
              text-align: center;
              font-size: 11px;
              color: #aaa;
            }
            #container {
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: 16px;
              padding: 16px 10px 40px 10px;
            }
            canvas {
              box-shadow: 0 4px 16px rgba(0, 0, 0, 0.7);
              border-radius: 3px;
              max-width: 100%;
              height: auto;
              background-color: #ffffff;
            }
            #error-panel {
              display: none;
              background: #3c1f1f;
              border: 1px solid #ff4444;
              border-radius: 6px;
              padding: 16px;
              margin: 16px;
              font-size: 12px;
              font-family: 'JetBrains Mono', 'Consolas', monospace;
              white-space: pre-wrap;
              word-wrap: break-word;
              color: #ff6b6b;
              max-height: 300px;
              overflow-y: auto;
            }
          </style>
        </head>
        <body>
          <div id="toolbar">
            <div id="status">⏳ Aguardando compilação...</div>
            <div id="toolbar-actions">
              <button id="recompile-btn" title="Recompilar documento">⚡ Recompilar</button>
              <button id="zoom-out-btn" title="Reduzir zoom">➖</button>
              <span id="zoom-val">150%</span>
              <button id="zoom-in-btn" title="Aumentar zoom">➕</button>
            </div>
          </div>
          <div id="error-panel"></div>
          <div id="container"></div>

          <script type="module">
            import * as pdfjsLib from 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.4.168/pdf.min.mjs';
            pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.4.168/pdf.worker.min.mjs';

            let currentPdf = null;
            let currentScale = 1.5;
            let currentRenderId = 0;

            async function renderPages(pdf, renderId) {
              const container = document.getElementById('container');
              container.innerHTML = '';

              for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
                if (renderId !== currentRenderId) return;
                const page = await pdf.getPage(pageNum);
                if (renderId !== currentRenderId) return;

                const viewport = page.getViewport({ scale: currentScale });
                const canvas = document.createElement('canvas');
                const context = canvas.getContext('2d');
                canvas.height = viewport.height;
                canvas.width = viewport.width;
                canvas.title = 'Página ' + pageNum;

                canvas.ondblclick = function() {
                  const payload = 'page:' + pageNum.toString();
                  ${'$'}injectJs
                };

                container.appendChild(canvas);
                await page.render({ canvasContext: context, viewport: viewport }).promise;
              }

              if (renderId === currentRenderId) {
                document.getElementById('status').innerText =
                  '✅ PDF renderizado — ' + pdf.numPages + ' página(s)';
              }
            }

            window.renderPdfFromBase64 = async function(base64Data) {
              const renderId = ++currentRenderId;
              document.getElementById('status').innerText = '🔄 Renderizando PDF...';
              document.getElementById('error-panel').style.display = 'none';

              try {
                const raw = atob(base64Data);
                const uint8Array = new Uint8Array(raw.length);
                for (let i = 0; i < raw.length; i++) {
                  uint8Array[i] = raw.charCodeAt(i);
                }

                const pdf = await pdfjsLib.getDocument({ data: uint8Array }).promise;
                if (renderId !== currentRenderId) return;

                currentPdf = pdf;
                await renderPages(pdf, renderId);
              } catch (err) {
                if (renderId === currentRenderId) {
                  document.getElementById('status').innerText = '❌ Erro: ' + err.message;
                }
              }
            };

            window.showCompilationError = function(msg) {
              const panel = document.getElementById('error-panel');
              panel.innerText = msg;
              panel.style.display = 'block';
              document.getElementById('status').innerText = '❌ Erro de compilação';
            };

            document.getElementById('zoom-in-btn').onclick = function() {
              currentScale = Math.min(currentScale + 0.25, 3.0);
              document.getElementById('zoom-val').innerText = Math.round(currentScale * 100) + '%';
              if (currentPdf) renderPages(currentPdf, ++currentRenderId);
            };

            document.getElementById('zoom-out-btn').onclick = function() {
              currentScale = Math.max(currentScale - 0.25, 0.5);
              document.getElementById('zoom-val').innerText = Math.round(currentScale * 100) + '%';
              if (currentPdf) renderPages(currentPdf, ++currentRenderId);
            };

            document.getElementById('recompile-btn').onclick = function() {
              const payload = 'recompile';
              ${'$'}injectJs
            };

            if (window.pendingBase64Data) {
              window.renderPdfFromBase64(window.pendingBase64Data);
              window.pendingBase64Data = null;
            }
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
