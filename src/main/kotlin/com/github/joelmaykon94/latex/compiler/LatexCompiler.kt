package com.github.joelmaykon94.latex.compiler

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.charset.StandardCharsets

object LatexCompiler {
    private val log = logger<LatexCompiler>()

    fun compile(
        project: Project,
        texFile: File,
        outputDir: File,
        onSuccess: (pdfFile: File) -> Unit,
        onError: (errorLog: String) -> Unit
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Compilando LaTeX...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Executando latexmk em ${texFile.name}..."

                val baseName = texFile.nameWithoutExtension
                val expectedPdf = File(outputDir, "$baseName.pdf")

                val cmd = GeneralCommandLine()
                    .withExePath(findCompilerExecutable())
                    .withParameters(
                        "-pdf",
                        "-interaction=nonstopmode",
                        "-synctex=1",
                        "-output-directory=${outputDir.absolutePath}",
                        texFile.absolutePath
                    )
                    .withWorkDirectory(outputDir)
                    .withCharset(StandardCharsets.UTF_8)

                try {
                    val processHandler = CapturingProcessHandler(cmd)
                    val output: ProcessOutput = processHandler.runProcess(60_000) // 60s timeout

                    if (output.isTimeout) {
                        ApplicationManager.getApplication().invokeLater {
                            onError("Compilação excedeu o tempo limite de 60 segundos.")
                        }
                        return
                    }

                    if (output.exitCode == 0 && expectedPdf.exists()) {
                        com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(expectedPdf)
                        ApplicationManager.getApplication().invokeLater {
                            onSuccess(expectedPdf)
                        }
                    } else {
                        val fullLog = output.stdout + "\n" + output.stderr
                        ApplicationManager.getApplication().invokeLater {
                            onError(extractErrors(fullLog))
                        }
                    }
                } catch (e: ExecutionException) {
                    log.warn("Failed to execute latexmk", e)
                    ApplicationManager.getApplication().invokeLater {
                        onError("Falha ao executar latexmk: ${e.message}")
                    }
                }
            }
        })
    }

    private fun findCompilerExecutable(): String {
        val possiblePaths = listOf(
            "latexmk",
            "/usr/bin/latexmk",
            "/usr/local/bin/latexmk",
            "/Library/TeX/texbin/latexmk",
            "C:\\texlive\\2024\\bin\\windows\\latexmk.exe",
            "C:\\Program Files\\MiKTeX\\miktex\\bin\\x64\\latexmk.exe"
        )
        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists() && file.canExecute()) {
                return path
            }
        }
        return "latexmk" // fallback to PATH
    }

    private fun extractErrors(logContent: String): String {
        val lines = logContent.lines()
        val errorLines = lines.filter {
            it.startsWith("!") ||
            it.contains("Error:", ignoreCase = true) ||
            it.contains("Fatal error", ignoreCase = true)
        }
        return if (errorLines.isNotEmpty()) {
            errorLines.joinToString("\n")
        } else {
            logContent.takeLast(2000)
        }
    }
}
