package com.github.joelmaykon94.latex.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class LatexFileType private constructor() : LanguageFileType(LatexLanguage.INSTANCE) {
    companion object {
        @JvmField
        val INSTANCE = LatexFileType()
    }

    override fun getName(): String = "LaTeX"
    override fun getDescription(): String = "LaTeX source file"
    override fun getDefaultExtension(): String = "tex"
    override fun getIcon(): Icon = LatexIcons.FILE
}
