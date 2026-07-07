package com.tomljump.core

data class TomlReference(
    val rawValue: String,
    val lookupValue: String,
    val kind: ReferenceKind,
    val qualifier: String? = null,
    val member: String? = null,
)
