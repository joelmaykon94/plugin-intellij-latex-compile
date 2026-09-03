package com.github.joelmaykon94.latex.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class LatexStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        LatexAutoCompileService.getInstance(project)
    }
}
