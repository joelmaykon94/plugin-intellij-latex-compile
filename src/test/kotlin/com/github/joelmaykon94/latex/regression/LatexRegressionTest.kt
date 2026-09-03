package com.github.joelmaykon94.latex.regression

import com.github.joelmaykon94.latex.compiler.LatexCompiler
import com.github.joelmaykon94.latex.editor.LatexScrollSyncCoordinator
import com.github.joelmaykon94.latex.lexer.LatexSimpleLexer
import com.github.joelmaykon94.latex.lexer.LatexTokenTypes
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

class LatexRegressionTest {

    private fun tokenize(text: String): List<Pair<IElementType, String>> {
        val lexer = LatexSimpleLexer()
        lexer.start(text, 0, text.length, 0)
        val tokens = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            val type = lexer.tokenType!!
            val tokenText = text.substring(lexer.tokenStart, lexer.tokenEnd)
            tokens.add(type to tokenText)
            lexer.advance()
        }
        return tokens
    }

    @Test
    fun `regression - escaped percent does not trigger comment and preserves following text`() {
        // Escaped \% should NOT swallow the rest of the line as a comment
        val tokens = tokenize("Discount of 50\\% on all books")
        val commentTokens = tokens.filter { it.first == LatexTokenTypes.COMMENT }
        assertTrue(commentTokens.isEmpty(), "Escaped percent must not be parsed as a comment")

        val specialTokens = tokens.filter { it.first == LatexTokenTypes.SPECIAL_CHAR }
        assertEquals(1, specialTokens.size)
        assertEquals("\\%", specialTokens[0].second)

        val textTokens = tokens.filter { it.first == LatexTokenTypes.TEXT }
        assertTrue(textTokens.any { it.second == "on" })
        assertTrue(textTokens.any { it.second == "books" })
    }

    @Test
    fun `regression - escaped dollar does not trigger inline math`() {
        // Escaped \$ must be parsed as SPECIAL_CHAR, not start of MATH_INLINE
        val tokens = tokenize("Price: \\$100 CAD")
        val mathTokens = tokens.filter { it.first == LatexTokenTypes.MATH_INLINE }
        assertTrue(mathTokens.isEmpty(), "Escaped dollar must not be parsed as math")

        val specialTokens = tokens.filter { it.first == LatexTokenTypes.SPECIAL_CHAR }
        assertEquals(1, specialTokens.size)
        assertEquals("\\$", specialTokens[0].second)
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    fun `regression - unclosed inline math terminates safely without infinite loop`() {
        val unclosed = "Some text with \$unclosed math at end of file"
        val tokens = tokenize(unclosed)
        assertFalse(tokens.isEmpty())
        assertEquals(LatexTokenTypes.MATH_INLINE, tokens.last().first)
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    fun `regression - unclosed display math terminates safely without infinite loop`() {
        val unclosed = "Some text with \$\$unclosed display math"
        val tokens = tokenize(unclosed)
        assertFalse(tokens.isEmpty())
        assertEquals(LatexTokenTypes.MATH_DISPLAY, tokens.last().first)
    }

    @Test
    fun `regression - command with asterisk sections are parsed as single command token`() {
        val tokens = tokenize("\\section*{Introduction} \\subsection*{Details}")
        val commands = tokens.filter { it.first == LatexTokenTypes.COMMAND }
        assertEquals(2, commands.size)
        assertEquals("\\section*", commands[0].second)
        assertEquals("\\subsection*", commands[1].second)
    }

    @Test
    fun `regression - consecutive backslashes do not throw index out of bounds`() {
        val tokens = tokenize("First Line \\\\ Second Line")
        val specialTokens = tokens.filter { it.first == LatexTokenTypes.SPECIAL_CHAR }
        assertEquals(1, specialTokens.size)
        assertEquals("\\\\", specialTokens[0].second)
    }

    @Test
    fun `regression - lexer handles non-zero start offset and bounds cleanly`() {
        val fullText = "PREFIX \\section{Test} SUFFIX"
        val startOffset = 7
        val endOffset = 21 // "\\section{Test}"
        val lexer = LatexSimpleLexer()
        lexer.start(fullText, startOffset, endOffset, 0)

        val tokens = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            val tokenText = fullText.substring(lexer.tokenStart, lexer.tokenEnd)
            tokens.add(lexer.tokenType!! to tokenText)
            lexer.advance()
        }

        assertFalse(tokens.isEmpty())
        assertEquals(LatexTokenTypes.COMMAND, tokens[0].first)
        assertEquals("\\section", tokens[0].second)
    }

    @Test
    fun `regression - scroll sync ratio with degenerate inputs never returns NaN or Infinity`() {
        val r1 = LatexScrollSyncCoordinator.calculateRatioFromScroll(0, 0, 0)
        assertFalse(r1.isNaN())
        assertFalse(r1.isInfinite())
        assertEquals(0.0, r1)

        val r2 = LatexScrollSyncCoordinator.calculateRatioFromLine(0, 0)
        assertFalse(r2.isNaN())
        assertFalse(r2.isInfinite())
        assertEquals(0.0, r2)

        val r3 = LatexScrollSyncCoordinator.calculateRatioFromLine(500, 1)
        assertFalse(r3.isNaN())
        assertFalse(r3.isInfinite())
        assertEquals(0.0, r3)

        val target = LatexScrollSyncCoordinator.calculateTargetScrollY(Double.NaN, 1000, 200)
        assertTrue(target >= 0)
    }

    @Test
    fun `regression - error extraction on large output does not throw exception`() {
        val largeLog = buildString {
            for (i in 1..20_000) {
                appendLine("Overfull \\hbox (12.34pt too wide) in paragraph at lines $i--${i + 5}")
            }
            appendLine("! Undefined control sequence.")
            appendLine("l.20005 \\nonexistentmacro")
            for (i in 20_006..20_050) {
                appendLine("Output continued line $i")
            }
        }

        val errors = LatexCompiler.extractErrors(largeLog)
        assertTrue(errors.contains("! Undefined control sequence."))
    }
}
