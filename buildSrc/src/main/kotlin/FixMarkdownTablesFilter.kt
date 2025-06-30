import java.io.Reader
import java.io.FilterReader
import java.io.StringReader

/**
 * Fixes markdown tables:
 * * replace line breaks within a table cell by wrapping the lines in <p> tags and removing the line breaks.
 * * Ensures tables have whitespace above and beneath
 */
class FixMarkdownTablesFilter(i: Reader) : FilterReader(i) {
    private val lineBreakInCellPattern = Regex("""\| ([^|]*?)\n+([^|]*?) \|""", RegexOption.MULTILINE)
    private val lineBreakInCellReplacement = "| {::nomarkdown}<p>{:/}$1{::nomarkdown}</p><p>{:/}$2{::nomarkdown}</p>{:/} |"

    private val missingSurroundingLineBreaksPattern = Regex("""^([^|]+\n)?((?:\|.*\|\n)+)([^\n]+$)?""", RegexOption.MULTILINE)
    private val missingSurroundingLineBreaksReplacement = "$1\n$2\n$3"

    private val transformed: Reader

    init {
        // read all the old text
        val old = i.readText()
        // create a placeholder for the new text, initialized to the old text
        var new: String = old
        do {
            // store the previously transformed value for comparison so we know if we're done
            val prev = new
            // transform the results -- only one line break will be fixed per cell per loop
            new = prev.replace(lineBreakInCellPattern, lineBreakInCellReplacement)
            // if nothing changed on this iteration, we're done.
        } while (prev != new)
        transformed = StringReader(new.replace(missingSurroundingLineBreaksPattern, missingSurroundingLineBreaksReplacement))
    }

    override fun read(): Int = transformed.read()
    override fun read(cbuf: CharArray?, off: Int, len: Int): Int = transformed.read(cbuf, off, len)
    override fun skip(n: Long): Long = transformed.skip(n)
    override fun ready(): Boolean = transformed.ready()
    override fun markSupported(): Boolean = transformed.markSupported()
    override fun mark(readAheadLimit: Int) = transformed.mark(readAheadLimit)
    override fun reset() = transformed.reset()
    override fun close() = transformed.close()
}
