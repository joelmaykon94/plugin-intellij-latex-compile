package com.github.joelmaykon94.latex.preview

import com.github.joelmaykon94.latex.compiler.LatexCompiler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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

    @Volatile
    var isScrollSyncEnabled: Boolean = true

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
                    data == "download" -> handleDownloadPdf()
                    data.startsWith("toggle-scroll-sync:") -> {
                        isScrollSyncEnabled = data.removePrefix("toggle-scroll-sync:").toBooleanStrictOrNull() ?: true
                    }
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

    fun handleDownloadPdf() {
        ApplicationManager.getApplication().invokeLater {
            val ioFile = File(texFile.path)
            val expectedPdf = File(ioFile.parentFile, "${ioFile.nameWithoutExtension}.pdf")
            if (!expectedPdf.exists()) {
                Messages.showWarningDialog(
                    project,
                    "O arquivo PDF ainda não foi gerado. Por favor, compile o documento LaTeX antes de realizar o download.",
                    "Download do PDF"
                )
                return@invokeLater
            }

            val descriptor = FileSaverDescriptor(
                "Salvar PDF Compilado",
                "Selecione o destino para salvar o arquivo PDF",
                "pdf"
            )
            val saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
            val baseDir = texFile.parent
            val target = saveDialog.save(baseDir, "${ioFile.nameWithoutExtension}.pdf")
            if (target != null) {
                try {
                    val destFile = target.file
                    expectedPdf.copyTo(destFile, overwrite = true)
                    Messages.showInfoMessage(
                        project,
                        "Arquivo PDF salvo com sucesso em:\n${destFile.absolutePath}",
                        "Download Concluído"
                    )
                } catch (e: Exception) {
                    log.error("Falha ao salvar o arquivo PDF", e)
                    Messages.showErrorDialog(
                        project,
                        "Falha ao salvar o arquivo PDF: ${e.message}",
                        "Erro no Download"
                    )
                }
            }
        }
    }

    fun scrollToRatio(ratio: Double, line: Int, totalLines: Int) {
        if (!isScrollSyncEnabled || browser == null) return
        val clampedRatio = ratio.coerceIn(0.0, 1.0)
        ApplicationManager.getApplication().invokeLater {
            browser?.cefBrowser?.executeJavaScript(
                "if (window.scrollToRatio) window.scrollToRatio($clampedRatio, $line, $totalLines);",
                browser?.cefBrowser?.url ?: "",
                0
            )
        }
    }

    fun setScrollSync(enabled: Boolean) {
        isScrollSyncEnabled = enabled
        ApplicationManager.getApplication().invokeLater {
            browser?.cefBrowser?.executeJavaScript(
                "if (window.setScrollSyncEnabled) window.setScrollSyncEnabled($enabled);",
                browser?.cefBrowser?.url ?: "",
                0
            )
        }
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
              transition: background 0.15s, border-color 0.15s;
            }
            button:hover {
              background: #444444;
              border-color: #666666;
            }
            button.active {
              background: #1b4b27;
              border-color: #2ea043;
              color: #ffffff;
            }
            #zoom-val {
              min-width: 42px;
              text-align: center;
              font-size: 11px;
              color: #aaa;
              cursor: pointer;
              user-select: none;
            }
            #zoom-val:hover {
              color: #ffffff;
              text-decoration: underline;
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
              <button id="download-btn" title="Baixar / Salvar PDF compilado em disco">💾 Baixar PDF</button>
              <button id="sync-scroll-btn" class="active" title="Sincronizar rolagem do código com o PDF (clique para ativar/desativar)">🔗 Sync: ON</button>
              <button id="zoom-out-btn" title="Reduzir zoom (Ctrl + - ou Ctrl + Scroll Down)">➖</button>
              <span id="zoom-val" title="Zoom atual. Clique para alternar 100% / 150%">150%</span>
              <button id="zoom-in-btn" title="Aumentar zoom (Ctrl + + ou Ctrl + Scroll Up)">➕</button>
              <button id="fit-width-btn" title="Ajustar PDF à largura da janela">↔️ Ajustar</button>
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
            let scrollSyncEnabled = true;

            window.setScrollSyncEnabled = function(enabled) {
              scrollSyncEnabled = !!enabled;
              updateSyncBtn();
            };

            function updateSyncBtn() {
              const btn = document.getElementById('sync-scroll-btn');
              if (scrollSyncEnabled) {
                btn.classList.add('active');
                btn.innerText = '🔗 Sync: ON';
                btn.title = 'Sincronização de rolagem ATIVA (clique para desativar)';
              } else {
                btn.classList.remove('active');
                btn.innerText = '🔗 Sync: OFF';
                btn.title = 'Sincronização de rolagem DESATIVADA (clique para ativar)';
              }
            }

            document.getElementById('sync-scroll-btn').onclick = function() {
              scrollSyncEnabled = !scrollSyncEnabled;
              updateSyncBtn();
              const payload = 'toggle-scroll-sync:' + scrollSyncEnabled;
              ${'$'}injectJs
            };

            window.scrollToRatio = function(ratio, line, totalLines) {
              if (!scrollSyncEnabled) return;
              const scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight;
              const clientHeight = window.innerHeight || document.documentElement.clientHeight;
              const maxScroll = Math.max(0, scrollHeight - clientHeight);
              if (maxScroll <= 0) return;
              const targetY = ratio * maxScroll;
              window.scrollTo(0, targetY);
            };

            async function renderPages(pdf, renderId) {
              const container = document.getElementById('container');
              container.innerHTML = '';
              const outputScale = window.devicePixelRatio || 1;

              for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
                if (renderId !== currentRenderId) return;
                const page = await pdf.getPage(pageNum);
                if (renderId !== currentRenderId) return;

                const viewport = page.getViewport({ scale: currentScale });
                const canvas = document.createElement('canvas');
                const context = canvas.getContext('2d');
                canvas.width = Math.floor(viewport.width * outputScale);
                canvas.height = Math.floor(viewport.height * outputScale);
                canvas.style.width = Math.floor(viewport.width) + 'px';
                canvas.style.height = Math.floor(viewport.height) + 'px';
                canvas.title = 'Página ' + pageNum;

                canvas.ondblclick = function() {
                  const payload = 'page:' + pageNum.toString();
                  ${'$'}injectJs
                };

                container.appendChild(canvas);
                const transform = outputScale !== 1 ? [outputScale, 0, 0, outputScale, 0, 0] : null;
                await page.render({ canvasContext: context, transform: transform, viewport: viewport }).promise;
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

            async function fitToWidth() {
              if (!currentPdf) return;
              try {
                const firstPage = await currentPdf.getPage(1);
                const unscaledViewport = firstPage.getViewport({ scale: 1.0 });
                const availableWidth = window.innerWidth - 36;
                if (availableWidth > 0 && unscaledViewport.width > 0) {
                  currentScale = Math.max(0.25, Math.min(Math.round((availableWidth / unscaledViewport.width) * 100) / 100, 4.0));
                  document.getElementById('zoom-val').innerText = Math.round(currentScale * 100) + '%';
                  renderPages(currentPdf, ++currentRenderId);
                }
              } catch (e) {
                console.error('Fit width error', e);
              }
            }

            document.getElementById('fit-width-btn').onclick = fitToWidth;

            document.getElementById('zoom-val').onclick = function() {
              if (Math.round(currentScale * 100) === 100) {
                currentScale = 1.5;
              } else {
                currentScale = 1.0;
              }
              document.getElementById('zoom-val').innerText = Math.round(currentScale * 100) + '%';
              if (currentPdf) renderPages(currentPdf, ++currentRenderId);
            };

            document.getElementById('zoom-in-btn').onclick = function() {
              currentScale = Math.min(Math.round((currentScale + 0.25) * 100) / 100, 4.0);
              document.getElementById('zoom-val').innerText = Math.round(currentScale * 100) + '%';
              if (currentPdf) renderPages(currentPdf, ++currentRenderId);
            };

            document.getElementById('zoom-out-btn').onclick = function() {
              currentScale = Math.max(Math.round((currentScale - 0.25) * 100) / 100, 0.25);
              document.getElementById('zoom-val').innerText = Math.round(currentScale * 100) + '%';
              if (currentPdf) renderPages(currentPdf, ++currentRenderId);
            };

            document.getElementById('recompile-btn').onclick = function() {
              const payload = 'recompile';
              ${'$'}injectJs
            };

            document.getElementById('download-btn').onclick = function() {
              const payload = 'download';
              ${'$'}injectJs
            };

            window.addEventListener('wheel', function(e) {
              if (e.ctrlKey || e.metaKey) {
                e.preventDefault();
                const step = e.deltaY < 0 ? 0.25 : -0.25;
                const newScale = Math.max(0.25, Math.min(Math.round((currentScale + step) * 100) / 100, 4.0));
                if (newScale !== currentScale) {
                  currentScale = newScale;
                  document.getElementById('zoom-val').innerText = Math.round(currentScale * 100) + '%';
                  if (currentPdf) renderPages(currentPdf, ++currentRenderId);
                }
              }
            }, { passive: false });

            window.addEventListener('keydown', function(e) {
              if (e.ctrlKey || e.metaKey) {
                if (e.key === '+' || e.key === '=') {
                  e.preventDefault();
                  document.getElementById('zoom-in-btn').click();
                } else if (e.key === '-') {
                  e.preventDefault();
                  document.getElementById('zoom-out-btn').click();
                } else if (e.key === '0') {
                  e.preventDefault();
                  currentScale = 1.0;
                  document.getElementById('zoom-val').innerText = '100%';
                  if (currentPdf) renderPages(currentPdf, ++currentRenderId);
                }
              }
            });

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
