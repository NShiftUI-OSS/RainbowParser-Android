package com.nshiftui.rainbowparser.ast

data class RainbowTrigger(
    val name: String,
    val events: List<RainbowNode>,
    val leading: RainbowLeadingTrivia = RainbowLeadingTrivia(),
)

data class RainbowSlot(
    val name: String,
    val plugins: List<RainbowNode>,
    val leading: RainbowLeadingTrivia = RainbowLeadingTrivia(),
)

data class RainbowNode(
    val name: String,
    val parameters: List<RainbowParameter> = emptyList(),
    val block: RainbowBlock? = null,
    val leading: RainbowLeadingTrivia = RainbowLeadingTrivia(),
    val id: String? = null,
    val style: List<RainbowParameter> = emptyList(),
    val events: List<RainbowTrigger> = emptyList(),
    val slots: List<RainbowSlot> = emptyList(),
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
