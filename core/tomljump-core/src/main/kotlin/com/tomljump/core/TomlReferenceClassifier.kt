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

        return TomlReference(
            rawValue = rawValue,
            lookupValue = lookupValue,
            kind = ReferenceKind.FILE_PATH,
        )
    }

    private fun parseClassOrModule(rawValue: String): TomlReference? {
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
            (value.first().isLetter() || value.first() == '_') &&
            value.drop(1).all { it.isLetterOrDigit() || it == '_' }
    }
}
