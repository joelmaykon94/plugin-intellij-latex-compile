package com.github.joelmaykon94.latex.psi

import com.github.joelmaykon94.latex.lang.LatexFileType
import com.github.joelmaykon94.latex.lang.LatexLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class LatexFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, LatexLanguage.INSTANCE) {
    override fun getFileType(): FileType = LatexFileType.INSTANCE
    override fun toString(): String = "LaTeX File"
}
