# 🗺️ Roadmap de Funcionalidades & Governança de Release

Este documento estabelece o plano arquitetural para implementar gradualmente todos os recursos de produtividade do **LaTeX Compile & Preview**, mantendo a estabilidade, a leveza e a alta performance do núcleo atual.

---

## 🏛️ Princípios Arquiteturais e Regra de Ouro

1. **Isolamento do Núcleo Estável:**
   O pipeline reativo de compilação ([`LatexAutoCompileService`](src/main/kotlin/com/github/joelmaykon94/latex/compiler/LatexAutoCompileService.kt)), o renderizador sequencial JCEF ([`PdfPreviewPanel`](src/main/kotlin/com/github/joelmaykon94/latex/preview/PdfPreviewPanel.kt)) e o editor dividido ([`LatexEditorWithPreview`](src/main/kotlin/com/github/joelmaykon94/latex/editor/LatexEditorWithPreview.kt)) **não devem ser modificados** para adicionar inteligência de código.
2. **Extension Points Desacoplados:**
   Toda nova funcionalidade do editor (autocomplete, folding, outline, inspeções) deve ser implementada como uma extensão separada do IntelliJ Platform e registrada de forma independente em [`plugin.xml`](src/main/resources/META-INF/plugin.xml).
3. **Sem Gramáticas Rígidas (Evitar Grammar-Kit/BNF pesado):**
   LaTeX é uma linguagem de macros Turing-completa. Gramáticas BNF estritas quebram com facilidade e causam falsos positivos de erro sintático. Utilizaremos scanners e lexers rápidos baseados no [`LatexSimpleLexer`](src/main/kotlin/com/github/joelmaykon94/latex/lexer/LatexSimpleLexer.kt) e análise por expressões regulares/tokens.
4. **Governança de Documentação:**
   Nenhum recurso será prometido no `plugin.xml` ou no `README.md` antes de estar 100% implementado, testado e validado.

---

## 📅 Fases de Implementação

### 🔹 Fase 1: Inteligência Básica de Edição (Quick Wins)
*Objetivo: Tornar a digitação de código LaTeX fluida e intuitiva sem qualquer impacto na performance.*

- [ ] **1.1 Autocompletion de Comandos (`LatexCompletionContributor`)**
  - Autocomplete de comandos essenciais ao digitar `\` (`\section`, `\subsection`, `\textbf`, `\textit`, `\emph`, `\cite`, `\ref`, `\label`, `\begin`, `\end`, `\input`, `\include`).
  - Inserção automática de fechamento de chaves `{}` e posicionamento do cursor.
- [ ] **1.2 Autocompletion de Ambientes (`LatexEnvironmentCompletion`)**
  - Sugestões contextuais após `\begin{` ou `\end{` com ambientes padrão: `document`, `figure`, `table`, `equation`, `align`, `itemize`, `enumerate`, `description`, `center`, `abstract`, `verbatim`.
- [ ] **1.3 Dobramento de Código (`LatexFoldingBuilder`)**
  - Recolhimento de blocos `\begin{env} ... \end{env}`.
  - Recolhimento do preâmbulo (`\documentclass` até `\begin{document}`).
  - Recolhimento de comentários multi-linha iniciados por `%`.
- [ ] **1.4 Painel de Estrutura do Documento (`LatexStructureViewFactory`)**
  - Mapeamento hierárquico na aba nativa *Structure* da IDE para:
    - `\part{...}`
    - `\chapter{...}`
    - `\section{...}`
    - `\subsection{...}`
    - `\subsubsection{...}`
    - `\appendix`

---

### 🔹 Fase 2: Gestão de Referências e Bibliografia
*Objetivo: Eliminar erros de digitação em citações bibliográficas e referências cruzadas.*

- [ ] **2.1 Autocomplete de Referências Cruzadas (`\ref`, `\pageref`)**
  - Rastreamento em background de `\label{key}` em arquivos `.tex` do projeto.
  - Sugestão inteligente de chaves ao digitar `\ref{` ou `\pageref{`.
  - Navegação via clique (`Ctrl + Click` / GotoDeclaration) do `\ref` até o `\label` de origem.
- [ ] **2.2 Indexação de Arquivos `.bib` e Autocomplete de `\cite`**
  - Suporte à leitura de arquivos `.bib` via scanner leve.
  - Autocomplete de chaves ao digitar `\cite{`, exibindo autor, título e ano como anotação lateral no popup de completion.
- [ ] **2.3 Ação Rápida de Inserção de Tabela / Figura**
  - Live Templates práticos:
    - `fig` + Tab → estrutura completa de `\begin{figure} ... \includegraphics ... \caption ... \label ... \end{figure}`.
    - `tab` + Tab → estrutura básica de tabela com `tabular` e `booktabs`.
    - `eq` + Tab → ambiente de equação numerada.

---

### 🔹 Fase 3: Configurações e Multi-Engine
*Objetivo: Dar flexibilidade total para usuários avançados sem comprometer a configuração padrão pronta para uso.*

- [ ] **3.1 Painel de Configurações na IDE (`LatexSettingsConfigurable`)**
  - Localização: *Settings / Preferences → Languages & Frameworks → LaTeX*.
  - Persistência com `PersistentStateComponent`.
- [ ] **3.2 Seleção de Motor de Compilação**
  - Opções:
    - `latexmk` (Padrão atual, automático com BibTeX e SyncTeX).
    - `pdflatex` direto.
    - `xelatex` (para suporte nativo a fontes OpenType e Unicode).
    - `lualatex` (para projetos modernos com LuaTeX).
    - `tectonic` (compilador all-in-one moderno).
- [ ] **3.3 Parâmetros e Tuning do Pipeline**
  - Ajuste de debounce de compilação (slider de 200ms a 2000ms, padrão 600ms).
  - Checkbox para habilitar/desabilitar `-shell-escape` (desabilitado por padrão por segurança).
  - Botão de limpeza de arquivos auxiliares (`.aux`, `.bbl`, `.blg`, `.log`, `.out`, `.synctex.gz`).

---

### 🔹 Fase 4: Ferramentas Complementares e Conversão
*Objetivo: Recursos adicionais acionados sob demanda pelo usuário.*

- [ ] **4.1 Ação de Conversão LaTeX ↔ Typst (`LatexConvertAction`)**
  - Adicionado ao menu contextual (clique com botão direito no arquivo `.tex` no Project View).
  - Executa o binário `pandoc` instalado na máquina do usuário.
  - Diálogo de confirmação para escolher o destino sem risco de sobrescrita acidental.
- [ ] **4.2 Diagnóstico e Inspeções de Sintaxe Leves**
  - Destaque de ambientes não fechados (`\begin{xyz}` sem correspondente `\end{xyz}`).
  - Alerta de chaves não fechadas `{` ou `}`.

---

## 📋 Protocolo Obrigatório para Cada Nova Release

Sempre que uma nova funcionalidade ou fase for concluída, o seguinte checklist deve ser rigorosamente seguido:

```
[ ] 1. Testes Automatizados:
       - Criar testes unitários em src/test/kotlin/... para validar a nova funcionalidade.
       - Executar ./gradlew test para garantir regressão zero.

[ ] 2. Atualização de Metadados e Documentação:
       - Incrementar versão em build.gradle.kts (ex: 2026.1.1.4).
       - Atualizar src/main/resources/META-INF/plugin.xml:
         * Ajustar a versão (<version>).
         * Atualizar o <description> (Overview e Additional Info).
         * Escrever o resumo objetivo no <change-notes> (What's New).
       - Atualizar README.md com a descrição do recurso, capturas ou exemplos.
       - Atualizar updatePlugins.xml com a nova URL, versão e changelog conciso.

[ ] 3. Validação do Build:
       - Executar ./gradlew check buildPlugin.
       - Validar a integridade do pacote ZIP gerado em build/distributions/.

[ ] 4. Commit, Tag e Publicação:
       - git add .
       - git commit -m "feat: <descrição do recurso>"
       - git tag -a v<versao> -m "Release v<versao>"
       - git push origin main && git push origin v<versao>
```
