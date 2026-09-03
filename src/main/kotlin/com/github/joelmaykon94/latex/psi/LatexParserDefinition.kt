package com.github.joelmaykon94.latex.psi

import com.github.joelmaykon94.latex.lang.LatexLanguage
import com.github.joelmaykon94.latex.lexer.LatexSimpleLexer
import com.github.joelmaykon94.latex.lexer.LatexTokenTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class LatexParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(LatexLanguage.INSTANCE)
        val COMMENTS = TokenSet.create(LatexTokenTypes.COMMENT)
        val WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE)
    }

    override fun createLexer(project: Project?): Lexer = LatexSimpleLexer()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = WHITESPACES

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val rootMarker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        rootMarker.done(root)
        builder.treeBuilt
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = LatexFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
}
