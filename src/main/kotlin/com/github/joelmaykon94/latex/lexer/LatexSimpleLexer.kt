package com.github.joelmaykon94.latex.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * A hand-written lexer for LaTeX that handles:
 * - Commands (\command)
 * - Comments (% ...)
 * - Braces { }
 * - Brackets [ ]
 * - Inline math $...$
 * - Display math $$...$$
 * - Special escaped characters (\%, \$, \&, etc.)
 * - Plain text
 */
class LatexSimpleLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var startOffset: Int = 0
    private var endOffset: Int = 0
    private var currentPosition: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var currentTokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.currentPosition = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = currentTokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        if (currentPosition >= endOffset) {
            currentTokenType = null
            return
        }

        tokenStart = currentPosition
        val ch = buffer[currentPosition]

        when {
            // Whitespace
            ch.isWhitespace() -> {
                while (currentPosition < endOffset && buffer[currentPosition].isWhitespace()) {
                    currentPosition++
                }
                currentTokenType = TokenType.WHITE_SPACE
            }

            // Comment: % to end of line
            ch == '%' -> {
                // Check if escaped
                if (currentPosition > startOffset && buffer[currentPosition - 1] == '\\') {
                    currentPosition++
                    currentTokenType = LatexTokenTypes.SPECIAL_CHAR
                } else {
                    while (currentPosition < endOffset && buffer[currentPosition] != '\n') {
                        currentPosition++
                    }
                    currentTokenType = LatexTokenTypes.COMMENT
                }
            }

            // Backslash: command or special character
            ch == '\\' -> {
                currentPosition++
                if (currentPosition < endOffset) {
                    val next = buffer[currentPosition]
                    if (next.isLetter()) {
                        // LaTeX command: \word
                        while (currentPosition < endOffset && buffer[currentPosition].isLetter()) {
                            currentPosition++
                        }
                        // Consume optional trailing * (e.g., \section*)
                        if (currentPosition < endOffset && buffer[currentPosition] == '*') {
                            currentPosition++
                        }
                        currentTokenType = LatexTokenTypes.COMMAND
                    } else {
                        // Escaped special character: \%, \$, \\, etc.
                        currentPosition++
                        currentTokenType = LatexTokenTypes.SPECIAL_CHAR
                    }
                } else {
                    currentTokenType = LatexTokenTypes.SPECIAL_CHAR
                }
            }

            // Math: $ or $$
            ch == '$' -> {
                if (currentPosition + 1 < endOffset && buffer[currentPosition + 1] == '$') {
                    // Display math $$...$$
                    currentPosition += 2
                    while (currentPosition < endOffset - 1) {
                        if (buffer[currentPosition] == '$' && buffer[currentPosition + 1] == '$') {
                            currentPosition += 2
                            break
                        }
                        currentPosition++
                    }
                    if (currentPosition >= endOffset - 1 && (currentPosition < endOffset && buffer[currentPosition] != '$')) {
                        currentPosition = endOffset
                    }
                    currentTokenType = LatexTokenTypes.MATH_DISPLAY
                } else {
                    // Inline math $...$
                    currentPosition++
                    while (currentPosition < endOffset && buffer[currentPosition] != '$') {
                        if (buffer[currentPosition] == '\\') {
                            currentPosition++ // skip escaped char inside math
                        }
                        currentPosition++
                    }
                    if (currentPosition < endOffset) {
                        currentPosition++ // consume closing $
                    }
                    currentTokenType = LatexTokenTypes.MATH_INLINE
                }
            }

            // Braces
            ch == '{' -> {
                currentPosition++
                currentTokenType = LatexTokenTypes.OPEN_BRACE
            }
            ch == '}' -> {
                currentPosition++
                currentTokenType = LatexTokenTypes.CLOSE_BRACE
            }

            // Brackets
            ch == '[' -> {
                currentPosition++
                currentTokenType = LatexTokenTypes.OPEN_BRACKET
            }
            ch == ']' -> {
                currentPosition++
                currentTokenType = LatexTokenTypes.CLOSE_BRACKET
            }

            // Plain text
            else -> {
                while (currentPosition < endOffset) {
                    val c = buffer[currentPosition]
                    if (c == '\\' || c == '%' || c == '$' || c == '{' || c == '}' || c == '[' || c == ']' || c.isWhitespace()) {
                        break
                    }
                    currentPosition++
                }
                currentTokenType = LatexTokenTypes.TEXT
            }
        }

        tokenEnd = currentPosition
    }
}
