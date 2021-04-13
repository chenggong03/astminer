package astminer.examples

import astminer.cli.LabeledResult
import astminer.common.model.MethodInfo
import astminer.parse.java.GumTreeJavaNode
import astminer.parse.java.GumTreeJavaParser
import astminer.parse.java.GumTreeJavaMethodSplitter
import astminer.storage.*
import astminer.storage.path.CsvPathStorage
import astminer.storage.path.PathBasedStorageConfig
import java.io.File


private fun getCsvFriendlyMethodId(methodInfo: MethodInfo<GumTreeJavaNode>): String {
    val className = methodInfo.enclosingElementName() ?: ""
    val methodName = methodInfo.name() ?: "unknown_method"
    val parameterTypes = methodInfo.methodParameters.joinToString("|") { it.name() ?: "_" }
    return "$className.$methodName($parameterTypes)"
}


//Retrieve paths from all Java files, using a GumTree parser.
//GumTreeMethodSplitter is used to extract individual method nodes from the compilation unit tree.
fun allJavaMethods() {
    val inputDir = "src/test/resources/gumTreeMethodSplitter"

    val outputDir = "out_examples/allJavaMethods"
    val storage = CsvPathStorage(outputDir, PathBasedStorageConfig(5, 5), TokenProcessor.Split)

    File(inputDir).forFilesWithSuffix(".java") { file ->
        //parse file
        val fileNode = GumTreeJavaParser().parseInputStream(file.inputStream()) ?: return@forFilesWithSuffix

        //extract method nodes
        val methodNodes = GumTreeJavaMethodSplitter().splitIntoMethods(fileNode)

        methodNodes.forEach { methodInfo ->
            //Retrieve a method identifier
            val entityId = "${file.path}::${getCsvFriendlyMethodId(methodInfo)}"
            val labelingResult = LabeledResult(fileNode, entityId, file.path)
            storage.store(labelingResult)
        }
    }

    storage.close()
}
