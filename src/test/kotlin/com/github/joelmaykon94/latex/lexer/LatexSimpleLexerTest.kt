package com.github.joelmaykon94.latex.lexer

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LatexSimpleLexerTest {

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
    fun `test empty buffer returns no tokens`() {
        val tokens = tokenize("")
        assertEquals(0, tokens.size)
    }

    @Test
    fun `test tokenize commands`() {
        val tokens = tokenize("\\documentclass \\begin \\section* \\textbf")
        val commandTokens = tokens.filter { it.first == LatexTokenTypes.COMMAND }
        assertEquals(4, commandTokens.size)
        assertEquals("\\documentclass", commandTokens[0].second)
        assertEquals("\\begin", commandTokens[1].second)
        assertEquals("\\section*", commandTokens[2].second)
        assertEquals("\\textbf", commandTokens[3].second)
    }

    @Test
    fun `test tokenize comments`() {
        val tokens = tokenize("Hello % This is a comment\nWorld")
        val commentToken = tokens.find { it.first == LatexTokenTypes.COMMENT }
        assertEquals("% This is a comment", commentToken?.second)
    }

    @Test
    fun `test tokenize escaped special characters`() {
        val tokens = tokenize("\\% \\$ \\\\ \\&")
        val specialTokens = tokens.filter { it.first == LatexTokenTypes.SPECIAL_CHAR }
        assertEquals(4, specialTokens.size)
        assertEquals("\\%", specialTokens[0].second)
        assertEquals("\\$", specialTokens[1].second)
        assertEquals("\\\\", specialTokens[2].second)
        assertEquals("\\&", specialTokens[3].second)
    }

    @Test
    fun `test tokenize inline math`() {
        val tokens = tokenize("Formula \$E = mc^2\$ here")
        val mathTokens = tokens.filter { it.first == LatexTokenTypes.MATH_INLINE }
        assertEquals(1, mathTokens.size)
        assertEquals("\$E = mc^2\$", mathTokens[0].second)
    }

    @Test
    fun `test tokenize display math`() {
        val tokens = tokenize("Display \$\$\\sum_{i=1}^n x_i\$\$ math")
        val mathTokens = tokens.filter { it.first == LatexTokenTypes.MATH_DISPLAY }
        assertEquals(1, mathTokens.size)
        assertEquals("\$\$\\sum_{i=1}^n x_i\$\$", mathTokens[0].second)
    }

    @Test
    fun `test tokenize braces and brackets`() {
        val tokens = tokenize("{ [ ] }")
        assertEquals(LatexTokenTypes.OPEN_BRACE, tokens[0].first)
        assertEquals("{", tokens[0].second)

        val nonWhitespace = tokens.filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(listOf(
            LatexTokenTypes.OPEN_BRACE,
            LatexTokenTypes.OPEN_BRACKET,
            LatexTokenTypes.CLOSE_BRACKET,
            LatexTokenTypes.CLOSE_BRACE
        ), nonWhitespace.map { it.first })
    }

    @Test
    fun `test tokenize plain text and whitespace`() {
        val tokens = tokenize("Simple LaTeX text")
        val textTokens = tokens.filter { it.first == LatexTokenTypes.TEXT }
        assertEquals(3, textTokens.size)
        assertEquals("Simple", textTokens[0].second)
        assertEquals("LaTeX", textTokens[1].second)
        assertEquals("text", textTokens[2].second)
    }
}
