package com.github.joelmaykon94.latex.lang

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LatexFileTypeTest {

    @Test
    fun `test file type properties`() {
        val fileType = LatexFileType.INSTANCE
        assertEquals("LaTeX", fileType.name)
        assertEquals("tex", fileType.defaultExtension)
        assertEquals("LaTeX source file", fileType.description)
        assertNotNull(fileType.icon)
    }
}
