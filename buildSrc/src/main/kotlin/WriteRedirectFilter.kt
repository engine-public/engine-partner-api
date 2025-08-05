import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.file.FileCopyDetails
import java.io.File
import java.io.FileReader
import java.io.FilterReader
import java.io.Reader

class WriteRedirectFilter(i: Reader) : FilterReader(i) {
    // injected by gradle after construction
    lateinit var permalink: String
    lateinit var redirectUrl: String
    lateinit var templateFile: File

    // must be setup before running
    lateinit var templateReader: Reader
    lateinit var transformed: Reader

    var setupDone = false

    fun setup() {
        templateReader = FileReader(templateFile)
        transformed = ReplaceTokens(templateReader).apply {
            addConfiguredToken(ReplaceTokens.Token().apply {
                key = "PERMALINK"
                value = permalink
            })
            addConfiguredToken(ReplaceTokens.Token().apply {
                key = "REDIRECT_URL"
                value = redirectUrl
            })
        }
        setupDone = true
    }

    override fun read(): Int {
        if (!setupDone) {
            setup()
        }
        return transformed.read()
    }
    override fun read(cbuf: CharArray?, off: Int, len: Int): Int {
        if (!setupDone) {
            setup()
        }
        return transformed.read(cbuf, off, len)
    }
    override fun skip(n: Long): Long {
        if (!setupDone) {
            setup()
        }
        return transformed.skip(n)
    }
    override fun ready(): Boolean {
        if (!setupDone) {
            setup()
        }
        return transformed.ready()
    }
    override fun markSupported(): Boolean {
        if (!setupDone) {
            setup()
        }
        return transformed.markSupported()
    }
    override fun mark(readAheadLimit: Int) {
        if (!setupDone) {
            setup()
        }
        transformed.mark(readAheadLimit)
    }
    override fun reset() {
        if (setupDone) {
            transformed.reset()
        }
    }
    override fun close() {
        if (setupDone) {
            transformed.close()
            templateReader.close()
        }
    }
}
