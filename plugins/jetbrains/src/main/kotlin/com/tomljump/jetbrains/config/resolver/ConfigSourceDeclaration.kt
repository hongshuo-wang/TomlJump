package com.tomljump.jetbrains.config.resolver

enum class ConfigSourceDeclarationKind {
    CONTAINER,
    FIELD,
}

data class ConfigSourceDeclaration(
    val offset: Int,
    val label: String,
    val kind: ConfigSourceDeclarationKind,
    val aliases: Set<String> = emptySet(),
    val ownerLabel: String? = null,
) {
    fun toTarget(): ConfigSourceTarget = ConfigSourceTarget(offset, label)

    fun containsOffset(sourceOffset: Int): Boolean {
        return sourceOffset in offset until (offset + label.length)
    }
}
