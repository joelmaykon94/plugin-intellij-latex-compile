package com.github.joelmaykon94.latex.completion

import com.github.joelmaykon94.latex.commenter.LatexCommenter
import com.github.joelmaykon94.latex.psi.LatexParserDefinition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LatexCompletionTest {

    @Test
    fun testCommonCommandsIntegrity() {
        val commands = LatexCommands.COMMON_COMMANDS
        assertTrue(commands.isNotEmpty(), "Command list should not be empty")

        val commandNames = commands.map { it.name }.toSet()
        assertTrue(commandNames.contains("section"), "Should contain section command")
        assertTrue(commandNames.contains("subsection"), "Should contain subsection command")
        assertTrue(commandNames.contains("begin"), "Should contain begin command")
        assertTrue(commandNames.contains("end"), "Should contain end command")
        assertTrue(commandNames.contains("cite"), "Should contain cite command")
        assertTrue(commandNames.contains("ref"), "Should contain ref command")
        assertTrue(commandNames.contains("textbf"), "Should contain textbf command")
        assertTrue(commandNames.contains("documentclass"), "Should contain documentclass command")
        assertTrue(commandNames.contains("usepackage"), "Should contain usepackage command")

        for (cmd in commands) {
            assertFalse(cmd.name.startsWith("\\"), "Command name should not start with backslash: ${cmd.name}")
            assertTrue(cmd.category.isNotBlank(), "Category should be specified for ${cmd.name}")
            assertTrue(cmd.description.isNotBlank(), "Description should be specified for ${cmd.name}")
        }
    }

    @Test
    fun testCommonEnvironmentsIntegrity() {
        val envs = LatexCommands.COMMON_ENVIRONMENTS
        assertTrue(envs.isNotEmpty(), "Environment list should not be empty")

        val envNames = envs.map { it.name }.toSet()
        assertTrue(envNames.contains("document"), "Should contain document environment")
        assertTrue(envNames.contains("figure"), "Should contain figure environment")
        assertTrue(envNames.contains("table"), "Should contain table environment")
        assertTrue(envNames.contains("equation"), "Should contain equation environment")
        assertTrue(envNames.contains("align"), "Should contain align environment")
        assertTrue(envNames.contains("itemize"), "Should contain itemize environment")
        assertTrue(envNames.contains("enumerate"), "Should contain enumerate environment")
        assertTrue(envNames.contains("tabular"), "Should contain tabular environment")

        for (env in envs) {
            assertTrue(env.name.isNotBlank(), "Environment name must not be blank")
            assertTrue(env.description.isNotBlank(), "Environment description must not be blank")
        }
    }

    @Test
    fun testPackagesAndClasses() {
        val pkgs = LatexCommands.COMMON_PACKAGES.toMap()
        assertTrue(pkgs.containsKey("amsmath"))
        assertTrue(pkgs.containsKey("graphicx"))
        assertTrue(pkgs.containsKey("hyperref"))

        val classes = LatexCommands.COMMON_CLASSES.toMap()
        assertTrue(classes.containsKey("article"))
        assertTrue(classes.containsKey("report"))
        assertTrue(classes.containsKey("book"))
    }

    @Test
    fun testParserDefinitionAndCommenter() {
        val parserDef = LatexParserDefinition()
        assertNotNull(parserDef.createLexer(null))
        assertNotNull(parserDef.createParser(null))
        assertEquals(LatexParserDefinition.FILE, parserDef.fileNodeType)
        assertTrue(parserDef.commentTokens.contains(com.github.joelmaykon94.latex.lexer.LatexTokenTypes.COMMENT))

        val commenter = LatexCommenter()
        assertEquals("%", commenter.lineCommentPrefix)
        assertNull(commenter.blockCommentPrefix)
    }
}
