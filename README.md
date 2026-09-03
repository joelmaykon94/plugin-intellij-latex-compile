# LaTeX Compile & Preview — IntelliJ Plugin

> **LaTeX brings first-class LaTeX and BibTeX authoring to IntelliJ Platform IDEs.**

![Plugin Icon](src/main/resources/META-INF/pluginIcon.svg)

---

## 🌟 Overview

### ✨ Write with IDE intelligence
- **Syntax and semantic highlighting**, completion, documentation, formatting, folding, inspections, spellchecking, and Grazie prose support.
- **Project-aware diagnostics** for undefined commands, include cycles, structural mistakes, unused declarations, duplicates, and obsolete constructs.
- **Project-aware navigation**, rename and find usages for labels, citations, commands, environments, packages, and included files.
- **Structure views**, breadcrumbs, live and postfix templates, source intentions, a floating formatting toolbar, Cmd+N generation, visual table insertion, an equation editor, and Beamer support.

### 📚 Manage bibliographies
- **Search rich citation completion** by key, author, title, or year; navigate, rename, find usages, or safely delete entries.
- **Complete and refactor `@string` macros**, format and fold BibTeX entries, validate styles, find unused entries, and open attached PDFs.
- **Keep chapterbib resources chapter-scoped**; index exported `.bib` files or securely synchronize cached Zotero, Better BibTeX, Mendeley, and HTTPS bibliography sources.

### ⚡ Build and preview locally
- **Compile with `latexmk`**, Tectonic, or pdfLaTeX/XeLaTeX/LuaLaTeX and navigate compiler diagnostics.
- **Infer or explicitly select the main document** across multi-file projects.
- **Side-by-side PDF preview** with continuous rebuilding, single-page or continuous-document display, zoom and navigation controls, and forward/reverse SyncTeX.

### 🔄 Convert documents
- **Convert LaTeX to Typst or Typst to LaTeX** with locally installed Pandoc or Morph. Conversion is available from file context menus, Refactor, and Tools, with executable auto-detection and review-before-overwrite safeguards.

---

## 🚀 What's New (Latest Release)

### Added
- Missing font families used by Babel, fontspec, and unicode-math are now highlighted directly in the editor. Verified IBM Plex fonts can be installed for the current user from the quick-fix menu, including the complete Serif, Sans, Mono, and Math families needed by a document.

### Fixed
- Builds whose main file is nested below the JetBrains project root now resolve relative packages, inputs, bibliographies, and output paths from the main file's directory unless a project root is explicitly configured.
- Continuous PDF previews now render approaching pages before they enter the viewport, preventing blank pages while scrolling through longer documents.
- Environments such as `cases` inside display math now match their `\begin` and `\end` declarations correctly instead of reporting an orphan end.
- Mathematical spacing controls, floor notation, half-open intervals, and sized square delimiters now parse without misleading internal-token errors.
- Missing closing brackets in command options now produce a concise, LaTeX-specific error instead of a list of internal parser token names.
- Standard LaTeX commands documented by latexref, including spacing commands and page-layout parameters, no longer receive incorrect undefined-command warnings.
- Commands supplied by loaded packages are now discovered from TeX Live, MiKTeX, and local Tectonic bundles, including declarations in auxiliary definition files and primitive aliases.
- Loading `unicode-math` now satisfies commands from its internal XeTeX and LuaTeX adapters, while core symbols such as `\to` remain package-independent.
- The floating formatting toolbar now appears only for prose selections, not when selecting LaTeX commands or mathematical markup.
- PDF previews can now follow the source caret to the corresponding page, with controls in both the preview toolbar and Preview settings.
- Continuous preview now virtualizes long PDFs in a single lightweight canvas, avoiding thousands of page controls and keeping scrolling fast.
- Large papers now receive inspections and structure navigation substantially faster by reusing document-root, package, declaration, and outline analysis.

---

## ℹ️ Additional Info for Developers

- **Repository**: [https://github.com/joelmaykon94/plugin-intellij-latex-compile](https://github.com/joelmaykon94/plugin-intellij-latex-compile)
- **Author / Maintainer**: Joel Maykon (`joelmaykon94@gmail.com`)
- **Compatibility**: IntelliJ IDEA 2024.2+ up to 2026.x (`sinceBuild = 242`, `untilBuild = 263.*`)
- **Runtime**: Java 21 (JVM 21) & Gradle 8.10.2
- **Privacy & Security**: Language editing works without a TeX distribution. External tools are optional, run locally, and are never bundled. Shell escape is disabled by default. The plugin contains no telemetry and never transmits document contents.

---

## 🛠️ Build & Install

```bash
# Set Java 21 & Gradle via mise
mise use java@21 gradle@8.10.2

# Build the plugin ZIP
./gradlew buildPlugin
```

Install via IntelliJ:
**Settings → Plugins → ⚙️ → Install Plugin from Disk...** and choose `build/distributions/intellij-latex-plugin-1.0.0.zip`.
