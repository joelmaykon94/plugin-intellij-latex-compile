plugins {
    id("java")
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.github.joelmaykon94"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3.1")
        instrumentationTools()
        pluginVerifier()
        jetbrainsRuntime()
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.joelmaykon94.intellij-latex-plugin"
        name = "LaTeX Compile & Preview"
        vendor {
            name = "Joel Maykon"
            email = "joelmaykon94@gmail.com"
            url = "https://github.com/joelmaykon94/plugin-intellij-latex-compile"
        }
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "263.*"
        }
    }
}
