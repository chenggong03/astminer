package astminer.parse.gumtree.python

import astminer.common.model.EnclosingElement
import astminer.common.model.EnclosingElementType
import astminer.common.model.FunctionInfo
import astminer.common.model.FunctionInfoParameter
import astminer.parse.antlr.getTokensFromSubtree
import astminer.parse.findEnclosingElementBy
import astminer.parse.gumtree.GumTreeNode
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Gumtree-Java-function-info")

class GumTreePythonFunctionInfo(
    override val root: GumTreeNode,
    override val filePath: String
) : FunctionInfo<GumTreeNode> {
    override val nameNode: GumTreeNode? = root.getChildOfType(NAME)

    override val returnType = if (root.children.find { it.originalToken == RETURN_TYPE_OPERATOR } == null) {
        null
    } else root.getChildrenOfType(NAME).lastOrNull()?.preOrder()?.mapNotNull { it.originalToken }?.joinToString("")

    override val enclosingElement: EnclosingElement<GumTreeNode>? = extractWithLogger(logger) {
        val enclosing = root.findEnclosingElementBy { it.typeLabel in possibleEnclosingElements }
            ?: return@extractWithLogger null
        val type = when (enclosing.typeLabel) {
            FUNCTION_DECLARATION -> EnclosingElementType.Function
            CLASS_DECLARATION -> EnclosingElementType.Class
            else -> error("No enclosing type can be associated")
        }
        EnclosingElement(
            name = enclosing.getChildOfType(NAME)?.originalToken,
            type = type,
            root = enclosing
        )
    }
    override val parameters: List<FunctionInfoParameter>? = extractWithLogger(logger) {
        val parameters = root.getChildOfType(PARAMETERS)
        checkNotNull(parameters) { "Node has no parameter node" }
        parameters.children.filter { it.typeLabel == PARAMETER }.map { param ->
            // Simple case: param has name and possibly default
            if (param.getChildOfType(TYPE_DEFINITION) == null) {
                val name = param.getChildOfType(NAME)?.originalToken
                checkNotNull(name) { "Parameter has no name" }
                FunctionInfoParameter(name, null)
            } else {
                // Complicated case: parameter has some type
                val variableDef = param.getChildOfType(TYPE_DEFINITION)
                    ?: error("Tree structure was changed while function info collection")
                val name = variableDef.getChildOfType(NAME)?.originalToken
                    ?: error("Parameter has no name")
                val type = if (variableDef.children.size > 1) variableDef.children[1].getTokensFromSubtree() else null
                FunctionInfoParameter(name, type)
            }
        }
    }

    companion object {
        const val NAME = "name"
        const val TYPE = "type"
        const val TYPE_DEFINITION = "tfpdef"
        const val PARAMETERS = "parameters"
        const val PARAMETER = "param"
        const val RETURN_TYPE_OPERATOR = "->"
        const val FUNCTION_DECLARATION = "funcdef"
        const val CLASS_DECLARATION = "classdef"
        val possibleEnclosingElements = listOf(FUNCTION_DECLARATION, CLASS_DECLARATION)
    }
}
