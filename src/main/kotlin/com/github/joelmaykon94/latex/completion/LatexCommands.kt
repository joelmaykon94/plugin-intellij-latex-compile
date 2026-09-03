package com.github.joelmaykon94.latex.completion

data class LatexCommandDescriptor(
    val name: String,
    val arguments: String,
    val description: String,
    val category: String,
    val snippet: String? = null
)

data class LatexEnvironmentDescriptor(
    val name: String,
    val description: String,
    val template: String? = null
)

object LatexCommands {

    val COMMON_COMMANDS: List<LatexCommandDescriptor> = listOf(
        // Document setup
        LatexCommandDescriptor("documentclass", "{class}", "Defines document type (article, report, book)", "Preamble", "\\documentclass{$1}"),
        LatexCommandDescriptor("usepackage", "{package}", "Loads external LaTeX package", "Preamble", "\\usepackage{$1}"),
        LatexCommandDescriptor("title", "{title}", "Document title", "Preamble", "\\title{$1}"),
        LatexCommandDescriptor("author", "{author}", "Document author", "Preamble", "\\author{$1}"),
        LatexCommandDescriptor("date", "{date}", "Document date", "Preamble", "\\date{$1}"),
        LatexCommandDescriptor("maketitle", "", "Generates title block", "Preamble", "\\maketitle"),
        LatexCommandDescriptor("tableofcontents", "", "Generates Table of Contents", "Structure", "\\tableofcontents"),

        // Structure
        LatexCommandDescriptor("section", "{title}", "Major section heading", "Structure", "\\section{$1}"),
        LatexCommandDescriptor("section*", "{title}", "Unnumbered section heading", "Structure", "\\section*{$1}"),
        LatexCommandDescriptor("subsection", "{title}", "Subsection heading", "Structure", "\\subsection{$1}"),
        LatexCommandDescriptor("subsection*", "{title}", "Unnumbered subsection heading", "Structure", "\\subsection*{$1}"),
        LatexCommandDescriptor("subsubsection", "{title}", "Sub-subsection heading", "Structure", "\\subsubsection{$1}"),
        LatexCommandDescriptor("paragraph", "{title}", "Paragraph heading", "Structure", "\\paragraph{$1}"),
        LatexCommandDescriptor("chapter", "{title}", "Chapter heading (report/book)", "Structure", "\\chapter{$1}"),
        LatexCommandDescriptor("part", "{title}", "Part heading", "Structure", "\\part{$1}"),
        LatexCommandDescriptor("appendix", "", "Marks start of appendices", "Structure", "\\appendix"),

        // Text formatting
        LatexCommandDescriptor("textbf", "{text}", "Bold text font weight", "Font", "\\textbf{$1}"),
        LatexCommandDescriptor("textit", "{text}", "Italic text font shape", "Font", "\\textit{$1}"),
        LatexCommandDescriptor("texttt", "{text}", "Monospaced / typewriter font", "Font", "\\texttt{$1}"),
        LatexCommandDescriptor("emph", "{text}", "Emphasized text", "Font", "\\emph{$1}"),
        LatexCommandDescriptor("underline", "{text}", "Underlined text", "Font", "\\underline{$1}"),
        LatexCommandDescriptor("textsc", "{text}", "Small capitals font", "Font", "\\textsc{$1}"),

        // Cross-referencing & citations
        LatexCommandDescriptor("label", "{key}", "Assigns label for cross-referencing", "References", "\\label{$1}"),
        LatexCommandDescriptor("ref", "{key}", "References a labelled section, figure or table", "References", "\\ref{$1}"),
        LatexCommandDescriptor("pageref", "{key}", "References the page of a label", "References", "\\pageref{$1}"),
        LatexCommandDescriptor("cite", "{key}", "Cites a bibliographic entry", "Citations", "\\cite{$1}"),
        LatexCommandDescriptor("bibliography", "{file}", "Specifies BibTeX bibliography file", "Citations", "\\bibliography{$1}"),
        LatexCommandDescriptor("bibliographystyle", "{style}", "Specifies bibliography formatting style", "Citations", "\\bibliographystyle{$1}"),

        // Environments & layout
        LatexCommandDescriptor("begin", "{environment}", "Starts a LaTeX environment", "Environment", "\\begin{$1}\n\t$2\n\\end{$1}"),
        LatexCommandDescriptor("end", "{environment}", "Ends a LaTeX environment", "Environment", "\\end{$1}"),
        LatexCommandDescriptor("item", "", "List item bullet/number", "List", "\\item "),
        LatexCommandDescriptor("caption", "{text}", "Figure or table caption", "Float", "\\caption{$1}"),
        LatexCommandDescriptor("centering", "", "Centers content within environment", "Layout", "\\centering"),
        LatexCommandDescriptor("newpage", "", "Forces page break", "Layout", "\\newpage"),
        LatexCommandDescriptor("clearpage", "", "Flushes floats and starts new page", "Layout", "\\clearpage"),
        LatexCommandDescriptor("vspace", "{length}", "Vertical space", "Layout", "\\vspace{$1}"),
        LatexCommandDescriptor("hspace", "{length}", "Horizontal space", "Layout", "\\hspace{$1}"),
        LatexCommandDescriptor("footnote", "{text}", "Creates a footnote", "Text", "\\footnote{$1}"),
        LatexCommandDescriptor("input", "{file}", "Inserts code from another .tex file", "Inclusion", "\\input{$1}"),
        LatexCommandDescriptor("include", "{file}", "Includes file with automatic page break", "Inclusion", "\\include{$1}"),

        // Mathematics
        LatexCommandDescriptor("frac", "{num}{den}", "Fraction numerator / denominator", "Math", "\\frac{$1}{$2}"),
        LatexCommandDescriptor("sqrt", "{val}", "Square root radical", "Math", "\\sqrt{$1}"),
        LatexCommandDescriptor("sum", "", "Summation operator with limits", "Math", "\\sum_{$1}^{$2}"),
        LatexCommandDescriptor("int", "", "Integral operator with limits", "Math", "\\int_{$1}^{$2}"),
        LatexCommandDescriptor("prod", "", "Product operator with limits", "Math", "\\prod_{$1}^{$2}"),
        LatexCommandDescriptor("lim", "", "Limit operator", "Math", "\\lim_{$1}"),
        LatexCommandDescriptor("infty", "", "Infinity symbol (∞)", "Math", "\\infty"),
        LatexCommandDescriptor("partial", "", "Partial derivative symbol (∂)", "Math", "\\partial"),
        LatexCommandDescriptor("nabla", "", "Del / nabla operator (∇)", "Math", "\\nabla"),
        LatexCommandDescriptor("left", "(", "Sized opening delimiter", "Math", "\\left("),
        LatexCommandDescriptor("right", ")", "Sized closing delimiter", "Math", "\\right)"),
        LatexCommandDescriptor("mathbf", "{text}", "Bold math font", "Math", "\\mathbf{$1}"),
        LatexCommandDescriptor("mathcal", "{text}", "Calligraphic math font", "Math", "\\mathcal{$1}"),
        LatexCommandDescriptor("mathbb", "{text}", "Blackboard bold font (e.g. ℝ, ℂ, ℕ)", "Math", "\\mathbb{$1}")
    )

    val COMMON_ENVIRONMENTS: List<LatexEnvironmentDescriptor> = listOf(
        LatexEnvironmentDescriptor("document", "Root body of the LaTeX document", "\\begin{document}\n\t$1\n\\end{document}"),
        LatexEnvironmentDescriptor("figure", "Floating figure with graphic, caption and label", "\\begin{figure}[htbp]\n\t\\centering\n\t\\includegraphics[width=0.8\\textwidth]{$1}\n\t\\caption{$2}\n\t\\label{fig:$3}\n\\end{figure}"),
        LatexEnvironmentDescriptor("table", "Floating table container with caption and label", "\\begin{table}[htbp]\n\t\\centering\n\t\\caption{$1}\n\t\\label{tab:$2}\n\t\\begin{tabular}{ccc}\n\t\t$3\n\t\\end{tabular}\n\\end{table}"),
        LatexEnvironmentDescriptor("tabular", "Tabular data grid with column specification", "\\begin{tabular}{$1}\n\t$2\n\\end{tabular}"),
        LatexEnvironmentDescriptor("equation", "Numbered single-line displayed equation", "\\begin{equation}\n\t$1\n\\end{equation}"),
        LatexEnvironmentDescriptor("equation*", "Unnumbered displayed equation", "\\begin{equation*}\n\t$1\n\\end{equation*}"),
        LatexEnvironmentDescriptor("align", "Aligned multi-line equations with numbering", "\\begin{align}\n\t$1 &= $2\n\\end{align}"),
        LatexEnvironmentDescriptor("align*", "Unnumbered aligned multi-line equations", "\\begin{align*}\n\t$1 &= $2\n\\end{align*}"),
        LatexEnvironmentDescriptor("gather", "Multi-line centered equations", "\\begin{gather}\n\t$1\n\\end{gather}"),
        LatexEnvironmentDescriptor("itemize", "Bulleted unordered list", "\\begin{itemize}\n\t\\item $1\n\\end{itemize}"),
        LatexEnvironmentDescriptor("enumerate", "Numbered ordered list", "\\begin{enumerate}\n\t\\item $1\n\\end{enumerate}"),
        LatexEnvironmentDescriptor("description", "Labelled item description list", "\\begin{description}\n\t\\item[$1] $2\n\\end{description}"),
        LatexEnvironmentDescriptor("center", "Horizontally centered text environment", "\\begin{center}\n\t$1\n\\end{center}"),
        LatexEnvironmentDescriptor("abstract", "Paper or report abstract block", "\\begin{abstract}\n\t$1\n\\end{abstract}"),
        LatexEnvironmentDescriptor("verbatim", "Preformatted monospaced text preserving spaces", "\\begin{verbatim}\n$1\n\\end{verbatim}"),
        LatexEnvironmentDescriptor("lstlisting", "Source code listing environment", "\\begin{lstlisting}[language=$1]\n$2\n\\end{lstlisting}"),
        LatexEnvironmentDescriptor("cases", "Piecewise function conditions block", "\\begin{cases}\n\t$1 & \\text{if } $2\n\\end{cases}"),
        LatexEnvironmentDescriptor("proof", "Mathematical proof environment", "\\begin{proof}\n\t$1\n\\end{proof}"),
        LatexEnvironmentDescriptor("theorem", "Mathematical theorem block", "\\begin{theorem}\n\t$1\n\\end{theorem}"),
        LatexEnvironmentDescriptor("lemma", "Mathematical lemma block", "\\begin{lemma}\n\t$1\n\\end{lemma}")
    )

    val COMMON_PACKAGES: List<Pair<String, String>> = listOf(
        "amsmath" to "AMS mathematical facilities for LaTeX",
        "amssymb" to "Extended AMS mathematical symbols",
        "graphicx" to "Enhanced support for graphics inclusion",
        "hyperref" to "Hypertext links in LaTeX documents",
        "babel" to "Multilingual support for LaTeX",
        "geometry" to "Flexible and easy interface to page dimensions",
        "booktabs" to "Publication quality tables in LaTeX",
        "tikz" to "Tool for creating graphic elements programmatically",
        "xcolor" to "Driver-independent color extensions for LaTeX",
        "csquotes" to "Context sensitive quotation facilities",
        "biblatex" to "Sophisticated Bibliographies in LaTeX",
        "microtype" to "Subliminal typographic enhancements",
        "enumitem" to "Control layout of itemize, enumerate, description",
        "caption" to "Customising captions in floating environments",
        "subcaption" to "Support for sub-captions (subfigures and subtables)"
    )

    val COMMON_CLASSES: List<Pair<String, String>> = listOf(
        "article" to "Scientific articles, short reports, documentation",
        "report" to "Longer reports with chapters, theses",
        "book" to "Real books with frontmatter, mainmatter, backmatter",
        "beamer" to "High quality presentation slides",
        "letter" to "Standard correspondence letters"
    )
}
