package com.nshiftui.rainbowparser.ast

data class RainbowNode(
    val name: String,
    val parameters: List<RainbowParameter> = emptyList(),
    val block: RainbowBlock? = null,
    val leading: RainbowLeadingTrivia = RainbowLeadingTrivia(),
) {
    val children: List<RainbowNode>
        field = ArrayList(block?.children ?: emptyList())

    val hasBlock: Boolean
        get() = block != null

    constructor(
        name: String,
        parameters: List<RainbowParameter> = emptyList(),
        children: List<RainbowNode>,
        leading: RainbowLeadingTrivia = RainbowLeadingTrivia(),
    ) : this(
        name = name,
        parameters = parameters,
        block = RainbowBlock(children),
        leading = leading,
    )
}
