package com.github.joelmaykon94.latex.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LatexCompilerTest {

    @Test
    fun `test extractErrors with standard exclamation mark errors`() {
        val log = """
            This is pdfTeX, Version 3.141592653-2.6-1.40.26
            entering extended mode
            (./document.tex
            ! Undefined control sequence.
            l.15 \invalidcommand
            )
            Output written on document.pdf (1 page, 1234 bytes).
        """.trimIndent()

        val extracted = LatexCompiler.extractErrors(log)
        assertTrue(extracted.contains("! Undefined control sequence."))
    }

    @Test
    fun `test extractErrors with fatal error message`() {
        val log = """
            LaTeX Warning: File `missing.png' not found on input line 42.
            ! LaTeX Error: File `missing.sty' not found.
            Type X to quit or <RETURN> to proceed.
            Fatal error occurred, no output PDF file produced!
        """.trimIndent()

        val extracted = LatexCompiler.extractErrors(log)
        assertTrue(extracted.contains("! LaTeX Error: File `missing.sty' not found."))
        assertTrue(extracted.contains("Fatal error occurred, no output PDF file produced!"))
    }

    @Test
    fun `test extractErrors fallback to tail when no markers present`() {
        val log = "Some generic output line 1\nSome generic output line 2\nCompilation halted."
        val extracted = LatexCompiler.extractErrors(log)
        assertEquals(log, extracted)
    }

    @Test
    fun `test extractErrors with empty log`() {
        val extracted = LatexCompiler.extractErrors("")
        assertEquals("", extracted)
    }
}
