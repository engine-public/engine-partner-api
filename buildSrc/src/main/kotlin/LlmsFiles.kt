import java.io.File

/**
 * Writes LLM-friendly copies of the docs site into the built site directory:
 * - a plain markdown copy of every page at the same path with a `.md` extension
 *   (`/webhooks.html` -> `/webhooks.md`, `/` -> `/index.md`)
 * - `llms.txt`, an index of every page following https://llmstxt.org
 * - `llms-full.txt`, every page's markdown concatenated in nav order
 *
 * Input is the staged Jekyll content (front matter + markdown + link footer), so the
 * markdown matches what Jekyll renders. Kramdown-only syntax (TOC blocks, attribute
 * lists, lint directives) is stripped and unused reference link definitions are dropped.
 */
object LlmsFiles {
    data class Page(
        val title: String,
        val permalink: String,
        val parent: String?,
        val navOrder: Int,
        val description: String?,
        val body: String,
    ) {
        val markdownPath: String
            get() = permalink.removePrefix("/").let {
                when {
                    it.isEmpty() || it.endsWith("/") -> it + "index.md"
                    it.endsWith(".html") -> it.removeSuffix(".html") + ".md"
                    else -> "$it.md"
                }
            }
    }

    fun write(contentDir: File, siteDir: File, siteUrl: String, headerFile: File, downloads: List<Pair<String, String>>) {
        val base = siteUrl.trimEnd('/')
        val origin = Regex("""^https?://[^/]+""").find(base)?.value ?: ""
        val pages = contentDir.walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .mapNotNull { parse(it.readText()) }
            .map { it.copy(body = absolutizeLinks(it.body, origin)) }
            .toList()

        for (page in pages) {
            val out = File(siteDir, page.markdownPath)
            out.parentFile.mkdirs()
            out.writeText(page.body)
        }

        val ordered = navOrder(pages)
        val header = headerFile.readText().trim()

        val index = StringBuilder(header).append("\n")
        val byParent = pages.groupBy { it.parent }
        fun appendSection(section: Page, depth: Int) {
            val children = byParent[section.title].orEmpty().sortedWith(compareBy({ it.navOrder }, { it.title }))
            index.append("\n").append("#".repeat(depth)).append(" ").append(section.title).append("\n\n")
            index.append(linkLine(section, base))
            for (child in children) {
                if (byParent.containsKey(child.title)) {
                    appendSection(child, depth + 1)
                } else {
                    index.append(linkLine(child, base))
                }
            }
        }
        for (top in pages.filter { it.parent == null }.sortedWith(compareBy({ it.navOrder }, { it.title }))) {
            appendSection(top, 2)
        }
        if (downloads.isNotEmpty()) {
            index.append("\n## API definitions\n\n")
            for ((name, path) in downloads) {
                index.append("- [$name]($base/$path)\n")
            }
        }
        File(siteDir, "llms.txt").writeText(index.toString())

        val full = StringBuilder(header).append("\n")
        for (page in ordered) {
            full.append("\n---\n\n")
            full.append("Source: $base/${page.markdownPath}\n\n")
            full.append(page.body.trim()).append("\n")
        }
        File(siteDir, "llms-full.txt").writeText(full.toString())
    }

    // Site-relative links (`](/path` and `[Ref]: /path`) become absolute so the markdown works out of context.
    private fun absolutizeLinks(markdown: String, origin: String): String =
        if (origin.isEmpty()) markdown
        else markdown.replace(Regex("""(]\(|(?m)^\[[^\]\n]+]:[ \t]*)/(?!/)""")) { it.groupValues[1] + origin + "/" }

    private fun linkLine(page: Page, base: String): String {
        val desc = page.description?.let { ": $it" } ?: ""
        return "- [${page.title}]($base/${page.markdownPath})$desc\n"
    }

    private fun navOrder(pages: List<Page>): List<Page> {
        val byParent = pages.groupBy { it.parent }
        val result = mutableListOf<Page>()
        fun visit(parent: String?) {
            for (p in byParent[parent].orEmpty().sortedWith(compareBy({ it.navOrder }, { it.title }))) {
                result += p
                visit(p.title)
            }
        }
        visit(null)
        // pages whose parent is missing would otherwise be dropped
        result += pages.filter { it !in result }
        return result
    }

    fun parse(text: String): Page? {
        val match = Regex("""\A---\r?\n(.*?)\r?\n---\r?\n""", RegexOption.DOT_MATCHES_ALL).find(text) ?: return null
        val frontMatter = match.groupValues[1].lines().mapNotNull { line ->
            val idx = line.indexOf(':')
            if (idx <= 0 || line.startsWith(" ")) null
            else line.substring(0, idx).trim() to line.substring(idx + 1).trim().removeSurrounding("\"").removeSurrounding("'")
        }.toMap()
        val permalink = frontMatter["permalink"] ?: return null
        val title = frontMatter["title"] ?: return null
        return Page(
            title = title,
            permalink = permalink,
            parent = frontMatter["parent"]?.takeIf { it.isNotEmpty() },
            navOrder = frontMatter["nav_order"]?.toIntOrNull() ?: Int.MAX_VALUE,
            description = frontMatter["description"]?.takeIf { it.isNotEmpty() },
            body = clean(text.substring(match.range.last + 1), title),
        )
    }

    fun clean(markdown: String, title: String): String {
        var body = markdown
            // kramdown TOC block: <details markdown="block"> ... {:toc} ... </details>
            .replace(Regex("""<details markdown="block">.*?\{:toc}.*?</details>\s*""", RegexOption.DOT_MATCHES_ALL), "")
            // markdownlint directives
            .replace(Regex("""<!--\s*markdownlint[^>]*-->[ \t]*\r?\n?"""), "")
            // kramdown options and block attribute lists on their own line
            .replace(Regex("""(?m)^[ \t]*\{:[^}\n]*}[ \t]*\r?\n"""), "")
            .replace(Regex("""(?m)^[ \t]*\{::[^}\n]*/}[ \t]*\r?\n"""), "")
            // inline attribute lists, e.g. [text][Ref]{:target="_blank"}
            .replace(Regex("""\{:[^}\n]*}"""), "")

        // keep only the reference link definitions this page uses
        val definition = Regex("""(?m)^\[([^\]\n]+)]:[ \t]*\S+.*$""")
        val content = definition.replace(body, "")
        val used = mutableSetOf<String>()
        Regex("""]\[([^\]\n]+)]""").findAll(content).forEach { used += it.groupValues[1].lowercase() }
        Regex("""\[([^\]\n]+)]\[]""").findAll(content).forEach { used += it.groupValues[1].lowercase() }
        Regex("""\[([^\]\n]+)](?![\[(:])""").findAll(content).forEach { used += it.groupValues[1].lowercase() }
        body = definition.replace(body) { if (it.groupValues[1].lowercase() in used) it.value else "" }

        body = body.replace(Regex("""\n{3,}"""), "\n\n").trim()
        if (!Regex("""(?m)^# """).containsMatchIn(body)) {
            body = "# $title\n\n$body"
        }
        return body + "\n"
    }
}
