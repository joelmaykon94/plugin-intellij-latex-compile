package com.github.joelmaykon94.latex.highlighting

import com.github.joelmaykon94.latex.lexer.LatexSimpleLexer
import com.github.joelmaykon94.latex.lexer.LatexTokenTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class LatexSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        val COMMAND: TextAttributesKey = createTextAttributesKey("LATEX_COMMAND", DefaultLanguageHighlighterColors.KEYWORD)
        val COMMENT: TextAttributesKey = createTextAttributesKey("LATEX_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BRACE: TextAttributesKey = createTextAttributesKey("LATEX_BRACE", DefaultLanguageHighlighterColors.BRACES)
        val BRACKET: TextAttributesKey = createTextAttributesKey("LATEX_BRACKET", DefaultLanguageHighlighterColors.BRACKETS)
        val MATH_INLINE: TextAttributesKey = createTextAttributesKey("LATEX_MATH_INLINE", DefaultLanguageHighlighterColors.STRING)
        val MATH_DISPLAY: TextAttributesKey = createTextAttributesKey("LATEX_MATH_DISPLAY", DefaultLanguageHighlighterColors.STRING)
        val SPECIAL_CHAR: TextAttributesKey = createTextAttributesKey("LATEX_SPECIAL_CHAR", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
    }

    override fun getHighlightingLexer(): Lexer = LatexSimpleLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            LatexTokenTypes.COMMAND -> arrayOf(COMMAND)
            LatexTokenTypes.COMMENT -> arrayOf(COMMENT)
            LatexTokenTypes.OPEN_BRACE, LatexTokenTypes.CLOSE_BRACE -> arrayOf(BRACE)
            LatexTokenTypes.OPEN_BRACKET, LatexTokenTypes.CLOSE_BRACKET -> arrayOf(BRACKET)
            LatexTokenTypes.MATH_INLINE -> arrayOf(MATH_INLINE)
            LatexTokenTypes.MATH_DISPLAY -> arrayOf(MATH_DISPLAY)
            LatexTokenTypes.SPECIAL_CHAR -> arrayOf(SPECIAL_CHAR)
            else -> emptyArray()
        }
    }
}
