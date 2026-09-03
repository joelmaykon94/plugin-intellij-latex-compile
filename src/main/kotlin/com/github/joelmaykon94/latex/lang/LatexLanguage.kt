package com.github.joelmaykon94.latex.lang

import com.intellij.lang.Language

class LatexLanguage private constructor() : Language("LaTeX") {
    companion object {
        @JvmField
        val INSTANCE = LatexLanguage()
    }
}
