package com.github.joelmaykon94.latex.completion

import com.github.joelmaykon94.latex.lang.LatexFileType
import com.github.joelmaykon94.latex.lang.LatexIcons
import com.github.joelmaykon94.latex.lang.LatexLanguage
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

class LatexCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val file = parameters.originalFile
        if (file.fileType !is LatexFileType && file.language !is LatexLanguage && file.virtualFile?.extension?.lowercase() != "tex") {
            return
        }

        val editor = parameters.editor
        val document = editor.document
        val offset = parameters.offset
        val chars = document.charsSequence

        // Scan backwards from offset to determine context
        val lineStart = document.getLineStartOffset(document.getLineNumber(offset))
        val textBeforeCaret = chars.subSequence(lineStart, offset).toString()

        when {
            // Context: inside \begin{... or \end{...
            textBeforeCaret.contains(Regex("""\\(?:begin|end)\{[a-zA-Z0-9_*]*$""")) -> {
                val match = Regex("""\\(begin|end)\{([a-zA-Z0-9_*]*)$""").find(textBeforeCaret)
                val isBegin = match?.groupValues?.get(1) == "begin"
                val envPrefix = match?.groupValues?.get(2) ?: ""
                val prefixMatcher = result.withPrefixMatcher(envPrefix)

                for (env in LatexCommands.COMMON_ENVIRONMENTS) {
                    val builder = LookupElementBuilder.create(env.name)
                        .withPresentableText(env.name)
                        .withTailText(" ${env.description}", true)
                        .withTypeText("Environment")
                        .withIcon(LatexIcons.FILE)
                        .withBoldness(true)
                        .withInsertHandler(createEnvironmentInsertHandler(isBegin, env))
                    prefixMatcher.addElement(builder)
                }
            }

            // Context: inside \usepackage{...
            textBeforeCaret.contains(Regex("""\\usepackage(?:\[[^\]]*\])?\{[a-zA-Z0-9_-]*$""")) -> {
                val match = Regex("""\{([a-zA-Z0-9_-]*)$""").find(textBeforeCaret)
                val pkgPrefix = match?.groupValues?.get(1) ?: ""
                val prefixMatcher = result.withPrefixMatcher(pkgPrefix)

                for ((pkg, desc) in LatexCommands.COMMON_PACKAGES) {
                    val builder = LookupElementBuilder.create(pkg)
                        .withPresentableText(pkg)
                        .withTailText(" $desc", true)
                        .withTypeText("Package")
                        .withIcon(LatexIcons.FILE)
                        .withInsertHandler(createClosingBraceInsertHandler())
                    prefixMatcher.addElement(builder)
                }
            }

            // Context: inside \documentclass{...
            textBeforeCaret.contains(Regex("""\\documentclass(?:\[[^\]]*\])?\{[a-zA-Z0-9_-]*$""")) -> {
                val match = Regex("""\{([a-zA-Z0-9_-]*)$""").find(textBeforeCaret)
                val classPrefix = match?.groupValues?.get(1) ?: ""
                val prefixMatcher = result.withPrefixMatcher(classPrefix)

                for ((cls, desc) in LatexCommands.COMMON_CLASSES) {
                    val builder = LookupElementBuilder.create(cls)
                        .withPresentableText(cls)
                        .withTailText(" $desc", true)
                        .withTypeText("Class")
                        .withIcon(LatexIcons.FILE)
                        .withInsertHandler(createClosingBraceInsertHandler())
                    prefixMatcher.addElement(builder)
                }
            }

            // Context: command completion (starts with \)
            textBeforeCaret.contains(Regex("""\\[a-zA-Z*]*$""")) -> {
                val match = Regex("""\\([a-zA-Z*]*)$""").find(textBeforeCaret)
                val cmdPrefix = match?.groupValues?.get(1) ?: ""
                val prefixMatcher = result.withPrefixMatcher(cmdPrefix)

                for (cmd in LatexCommands.COMMON_COMMANDS) {
                    val hasArgs = cmd.arguments.isNotEmpty()
                    val lookupString = cmd.name
                    val presentableText = if (hasArgs) "\\${cmd.name}${cmd.arguments}" else "\\${cmd.name}"

                    val builder = LookupElementBuilder.create(lookupString)
                        .withPresentableText(presentableText)
                        .withTailText(" — ${cmd.description}", true)
                        .withTypeText(cmd.category)
                        .withIcon(LatexIcons.FILE)
                        .withInsertHandler(createCommandInsertHandler(cmd))
                    prefixMatcher.addElement(builder)
                }
            }
        }
    }

    private fun createCommandInsertHandler(cmd: LatexCommandDescriptor): InsertHandler<LookupElement> {
        return InsertHandler { context: InsertionContext, _: LookupElement ->
            val editor = context.editor
            val document = context.document
            val tailOffset = context.tailOffset
            val chars = document.charsSequence

            if (cmd.name == "begin") {
                // Insert \begin{env}...\end{env} shell
                if (tailOffset < chars.length && chars[tailOffset] == '{') {
                    editor.caretModel.moveToOffset(tailOffset + 1)
                } else {
                    document.insertString(tailOffset, "{}")
                    editor.caretModel.moveToOffset(tailOffset + 1)
                }
            } else if (cmd.arguments.startsWith("{")) {
                // If command accepts arguments and doesn't already have '{', insert '{}'
                if (tailOffset >= chars.length || chars[tailOffset] != '{') {
                    document.insertString(tailOffset, "{}")
                    editor.caretModel.moveToOffset(tailOffset + 1)
                } else {
                    editor.caretModel.moveToOffset(tailOffset + 1)
                }
            } else if (cmd.name == "item") {
                if (tailOffset >= chars.length || chars[tailOffset] != ' ') {
                    document.insertString(tailOffset, " ")
                    editor.caretModel.moveToOffset(tailOffset + 1)
                }
            }
        }
    }

    private fun createEnvironmentInsertHandler(isBegin: Boolean, env: LatexEnvironmentDescriptor): InsertHandler<LookupElement> {
        return InsertHandler { context: InsertionContext, _: LookupElement ->
            val editor = context.editor
            val document = context.document
            val tailOffset = context.tailOffset
            val chars = document.charsSequence

            val hasClosingBrace = tailOffset < chars.length && chars[tailOffset] == '}'

            if (!hasClosingBrace) {
                document.insertString(tailOffset, "}")
            }

            val afterBraceOffset = tailOffset + (if (!hasClosingBrace) 1 else 0)

            if (isBegin) {
                // Check if \end{env.name} is already present right after
                val restOfDoc = chars.subSequence(afterBraceOffset, chars.length.coerceAtMost(afterBraceOffset + 100)).toString()
                if (!restOfDoc.trimStart().startsWith("\\end{${env.name}}")) {
                    val insertion = when (env.name) {
                        "itemize", "enumerate" -> "\n\t\\item \n\\end{${env.name}}"
                        "figure" -> "\n\t\\centering\n\t\\includegraphics[width=0.8\\textwidth]{}\n\t\\caption{}\n\t\\label{fig:}\n\\end{figure}"
                        "table" -> "\n\t\\centering\n\t\\caption{}\n\t\\label{tab:}\n\t\\begin{tabular}{ccc}\n\t\t\n\t\\end{tabular}\n\\end{table}"
                        "tabular" -> "\n\t\n\\end{tabular}"
                        "equation", "align" -> "\n\t\n\\end{${env.name}}"
                        else -> "\n\t\n\\end{${env.name}}"
                    }
                    document.insertString(afterBraceOffset, insertion)
                    // Position caret inside the environment
                    val caretTarget = when (env.name) {
                        "itemize", "enumerate" -> afterBraceOffset + "\n\t\\item ".length
                        "figure" -> afterBraceOffset + "\n\t\\centering\n\t\\includegraphics[width=0.8\\textwidth]{".length
                        "table" -> afterBraceOffset + "\n\t\\centering\n\t\\caption{".length
                        else -> afterBraceOffset + "\n\t".length
                    }
                    editor.caretModel.moveToOffset(caretTarget.coerceAtMost(document.textLength))
                    return@InsertHandler
                }
            }

            editor.caretModel.moveToOffset(afterBraceOffset)
        }
    }

    private fun createClosingBraceInsertHandler(): InsertHandler<LookupElement> {
        return InsertHandler { context: InsertionContext, _: LookupElement ->
            val editor = context.editor
            val document = context.document
            val tailOffset = context.tailOffset
            val chars = document.charsSequence

            if (tailOffset >= chars.length || chars[tailOffset] != '}') {
                document.insertString(tailOffset, "}")
                editor.caretModel.moveToOffset(tailOffset + 1)
            } else {
                editor.caretModel.moveToOffset(tailOffset + 1)
            }
        }
    }
}
