package astminer.examples

import astminer.common.getProjectFilesWithExtension
import astminer.parse.antlr.javascript.JavaScriptParser
import astminer.storage.path.CsvPathStorage
import astminer.storage.path.PathBasedStorageConfig
import astminer.storage.TokenProcessor
import java.io.File

fun allJavaScriptFiles() {
    val folder = "src/test/resources/examples"
    val outputDir = "out_examples/allJavaScriptFilesAntlr"

    val storage = CsvPathStorage(outputDir, PathBasedStorageConfig(5, 5), TokenProcessor.Split)

    val files = getProjectFilesWithExtension(File(folder), "js")
    JavaScriptParser().parseFiles(files) { parseResult ->
        parseResult.labeledWithFilePath()?.let { labeledResult ->
            storage.store(labeledResult)
        }
    }

    storage.close()
}