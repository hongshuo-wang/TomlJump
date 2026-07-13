package com.tomljump.core

class ConfigKeyPath private constructor(
    val segments: List<String>,
) {
    val leaf: String = segments.last()
    val display: String = segments.joinToString(".")

    override fun equals(other: Any?): Boolean {
        return this === other || other is ConfigKeyPath && segments == other.segments
    }

    override fun hashCode(): Int = segments.hashCode()

    override fun toString(): String = display

    companion object {
        fun of(vararg segments: String): ConfigKeyPath = from(segments.toList())

        fun from(segments: List<String>): ConfigKeyPath {
            require(segments.isNotEmpty()) { "Config key path must have at least one segment" }
            val cleaned = segments.map(String::trim)
            require(cleaned.all(String::isNotEmpty)) { "Config key path segments must be non blank" }
            return ConfigKeyPath(cleaned)
        }
    }
}
