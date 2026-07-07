package com.tomljump.core

class TomlReferenceClassifier {
    fun classify(rawValue: String): TomlReference? {
        if (rawValue.isEmpty() || rawValue.any(Char::isWhitespace)) {
            return null
        }

        parseCallable(rawValue)?.let { return it }
        parseFilePath(rawValue)?.let { return it }
        parseClassOrModule(rawValue)?.let { return it }

        return null
    }

    private fun parseCallable(rawValue: String): TomlReference? {
        val separatorIndex = rawValue.indexOfFirst { it == ':' || it == '#' }
        if (separatorIndex <= 0 || separatorIndex == rawValue.lastIndex) {
            return null
        }

        val qualifier = rawValue.substring(0, separatorIndex)
        val member = rawValue.substring(separatorIndex + 1)
        if (!isDottedIdentifier(qualifier) || !isIdentifier(member)) {
            return null
        }

        return TomlReference(
            rawValue = rawValue,
            lookupValue = rawValue,
            kind = ReferenceKind.CALLABLE,
            qualifier = qualifier,
            member = member,
        )
    }

    private fun parseFilePath(rawValue: String): TomlReference? {
        if (!rawValue.contains('/')) {
            return null
        }

        val lookupValue = rawValue.removePrefix("./")
        val segments = lookupValue.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) {
            return null
        }
        if (!hasMeaningfulPathSegment(segments)) {
            return null
        }

        return TomlReference(
            rawValue = rawValue,
            lookupValue = lookupValue,
            kind = ReferenceKind.FILE_PATH,
        )
    }

    private fun parseClassOrModule(rawValue: String): TomlReference? {
        if (isCommonBareFilename(rawValue)) {
            return null
        }
        if (!isDottedIdentifier(rawValue)) {
            return null
        }

        return TomlReference(
            rawValue = rawValue,
            lookupValue = rawValue,
            kind = ReferenceKind.CLASS_OR_MODULE,
        )
    }

    private fun isDottedIdentifier(value: String): Boolean {
        val parts = value.split('.')
        return parts.size > 1 && parts.all(::isIdentifier)
    }

    private fun isIdentifier(value: String): Boolean {
        return value.isNotEmpty() &&
            (isAsciiLetter(value.first()) || value.first() == '_') &&
            value.drop(1).all { isAsciiLetter(it) || it.isDigit() || it == '_' }
    }

    private fun hasMeaningfulPathSegment(segments: List<String>): Boolean {
        return segments.drop(1).any { segment ->
            hasDotExtension(segment) || segment.any(::isAsciiLetter)
        }
    }

    private fun hasDotExtension(segment: String): Boolean {
        val dotIndex = segment.lastIndexOf('.')
        return dotIndex > 0 && dotIndex < segment.lastIndex && segment.substring(dotIndex + 1).all(::isAsciiLetter)
    }

    private fun isCommonBareFilename(value: String): Boolean {
        if (value.contains('/')) {
            return false
        }

        val dotIndex = value.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex == value.lastIndex) {
            return false
        }

        val extension = value.substring(dotIndex + 1)
        return extension in setOf("json", "toml", "yaml", "yml", "md")
    }

    private fun isAsciiLetter(char: Char): Boolean {
        return char in 'A'..'Z' || char in 'a'..'z'
    }
}
