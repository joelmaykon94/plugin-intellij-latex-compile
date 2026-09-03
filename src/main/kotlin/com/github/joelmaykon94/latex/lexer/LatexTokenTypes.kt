package com.github.joelmaykon94.latex.lexer

import com.github.joelmaykon94.latex.lang.LatexLanguage
import com.intellij.psi.tree.IElementType

class LatexTokenType(debugName: String) : IElementType(debugName, LatexLanguage.INSTANCE) {
    override fun toString(): String = "LatexTokenType." + super.toString()
}

object LatexTokenTypes {
    @JvmField val COMMAND = LatexTokenType("COMMAND")
    @JvmField val COMMENT = LatexTokenType("COMMENT")
    @JvmField val OPEN_BRACE = LatexTokenType("OPEN_BRACE")
    @JvmField val CLOSE_BRACE = LatexTokenType("CLOSE_BRACE")
    @JvmField val OPEN_BRACKET = LatexTokenType("OPEN_BRACKET")
    @JvmField val CLOSE_BRACKET = LatexTokenType("CLOSE_BRACKET")
    @JvmField val MATH_INLINE = LatexTokenType("MATH_INLINE")
    @JvmField val MATH_DISPLAY = LatexTokenType("MATH_DISPLAY")
    @JvmField val TEXT = LatexTokenType("TEXT")
    @JvmField val SPECIAL_CHAR = LatexTokenType("SPECIAL_CHAR")
    @JvmField val ENVIRONMENT_NAME = LatexTokenType("ENVIRONMENT_NAME")
}
