# TOML Config Symbol Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build JetBrains navigation from TOML table names and keys to matching Go, Python, Java, TypeScript, and JavaScript configuration declarations.

**Architecture:** Add editor-neutral config key models in `core`, then add a second JetBrains reference path for TOML table/key PSI elements. Language resolvers remain optional by depending only on IntelliJ project/Psi APIs and source-file adapters, so missing Go/Python/JS language plugins cannot break plugin startup or default verification.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, IntelliJ Platform PSI/reference APIs, TOML PSI, Kotlin test, JUnit 4 platform fixture tests.

---

## File Structure

- Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/ConfigKeyPath.kt`: immutable key path model.
- Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/ConfigNameNormalizer.kt`: normalized matching for snake, kebab, camel, Pascal, and acronym forms.
- Create `core/tomljump-core/src/test/kotlin/com/tomljump/core/ConfigKeyPathTest.kt`: key path behavior tests.
- Create `core/tomljump-core/src/test/kotlin/com/tomljump/core/ConfigNameNormalizerTest.kt`: name normalization behavior tests.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/ExtractedConfigKeyPath.kt`: JetBrains extracted key path model plus text range.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigKeyPathExtractor.kt`: TOML table/key extractor.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigReference.kt`: PSI reference for config navigation.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributor.kt`: registers TOML table/key references.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolver.kt`: language resolver interface.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/ConfigSourceTarget.kt`: source target model.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/SourcePatternResolver.kt`: shared project file scanning and PSI target conversion.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/TomlConfigTargetResolver.kt`: coordinates resolver execution, logging, and deduplication.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/GoConfigResolver.kt`: Go struct tag and field resolver.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/PythonConfigResolver.kt`: Python class/dataclass/Pydantic-style field resolver.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/JavaConfigResolver.kt`: Java field and `@JsonProperty` resolver.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/TypeScriptConfigResolver.kt`: TypeScript property resolver.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/JavaScriptConfigResolver.kt`: JavaScript property resolver.
- Modify `plugins/jetbrains/src/main/resources/META-INF/plugin.xml`: register the new reference contributor.
- Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigKeyPathExtractorTest.kt`: TOML PSI extraction tests.
- Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributorTest.kt`: reference registration and Go resolution fixture tests.
- Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolverTest.kt`: resolver tests for Go, Python, Java, TypeScript, and JavaScript source fixtures.
- Modify `fixtures/go/handlers/user.go`: append the Go TOML tag config fixture shown in Task 9.
- Modify `fixtures/python/app/user.py`: append the Python config field fixture shown in Task 9.
- Modify `fixtures/java/src/main/java/com/example/PaymentClient.java`: append the Java config field fixture shown in Task 9.
- Modify `fixtures/typescript/src/jobs/sync.ts`: append the TypeScript config property fixture shown in Task 9.
- Modify `fixtures/javascript/src/jobs/sync.js`: append the JavaScript config property fixture shown in Task 9.

## Task 1: Core Key Path Model

**Files:**
- Create: `core/tomljump-core/src/main/kotlin/com/tomljump/core/ConfigKeyPath.kt`
- Create: `core/tomljump-core/src/test/kotlin/com/tomljump/core/ConfigKeyPathTest.kt`

- [ ] **Step 1: Write the failing key path tests**

Create `core/tomljump-core/src/test/kotlin/com/tomljump/core/ConfigKeyPathTest.kt`:

```kotlin
package com.tomljump.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigKeyPathTest {
    @Test
    fun `creates table path from non blank segments`() {
        val path = ConfigKeyPath.of("openai")

        assertEquals(listOf("openai"), path.segments)
        assertEquals("openai", path.leaf)
        assertEquals("openai", path.display)
    }

    @Test
    fun `creates nested key path`() {
        val path = ConfigKeyPath.of("servers", "production", "host")

        assertEquals(listOf("servers", "production", "host"), path.segments)
        assertEquals("host", path.leaf)
        assertEquals("servers.production.host", path.display)
    }

    @Test
    fun `rejects empty path`() {
        assertFailsWith<IllegalArgumentException> {
            ConfigKeyPath.of()
        }
    }

    @Test
    fun `rejects blank segment`() {
        assertFailsWith<IllegalArgumentException> {
            ConfigKeyPath.of("openai", " ")
        }
    }
}
```

- [ ] **Step 2: Run the core test and verify it fails**

Run:

```bash
./gradlew :core:tomljump-core:test --tests com.tomljump.core.ConfigKeyPathTest
```

Expected: compilation fails because `ConfigKeyPath` does not exist.

- [ ] **Step 3: Implement the key path model**

Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/ConfigKeyPath.kt`:

```kotlin
package com.tomljump.core

data class ConfigKeyPath private constructor(
    val segments: List<String>,
) {
    val leaf: String = segments.last()
    val display: String = segments.joinToString(".")

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
```

- [ ] **Step 4: Run the focused test and verify it passes**

Run:

```bash
./gradlew :core:tomljump-core:test --tests com.tomljump.core.ConfigKeyPathTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the core key path model**

Run:

```bash
git add core/tomljump-core/src/main/kotlin/com/tomljump/core/ConfigKeyPath.kt core/tomljump-core/src/test/kotlin/com/tomljump/core/ConfigKeyPathTest.kt
git commit -m "feat: add config key path model"
```

## Task 2: Core Name Normalization

**Files:**
- Create: `core/tomljump-core/src/main/kotlin/com/tomljump/core/ConfigNameNormalizer.kt`
- Create: `core/tomljump-core/src/test/kotlin/com/tomljump/core/ConfigNameNormalizerTest.kt`

- [ ] **Step 1: Write the failing normalizer tests**

Create `core/tomljump-core/src/test/kotlin/com/tomljump/core/ConfigNameNormalizerTest.kt`:

```kotlin
package com.tomljump.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigNameNormalizerTest {
    @Test
    fun `normalizes separators and case`() {
        assertEquals("apikey", ConfigNameNormalizer.normalize("api_key"))
        assertEquals("apikey", ConfigNameNormalizer.normalize("api-key"))
        assertEquals("apikey", ConfigNameNormalizer.normalize("apiKey"))
        assertEquals("apikey", ConfigNameNormalizer.normalize("APIKey"))
    }

    @Test
    fun `normalizes url acronym forms`() {
        assertEquals("baseurl", ConfigNameNormalizer.normalize("base_url"))
        assertEquals("baseurl", ConfigNameNormalizer.normalize("baseUrl"))
        assertEquals("baseurl", ConfigNameNormalizer.normalize("BaseURL"))
    }

    @Test
    fun `matches normalized variants`() {
        assertTrue(ConfigNameNormalizer.matches("api_key", "APIKey"))
        assertTrue(ConfigNameNormalizer.matches("base-url", "baseURL"))
        assertTrue(ConfigNameNormalizer.matches("model", "Model"))
    }
}
```

- [ ] **Step 2: Run the normalizer test and verify it fails**

Run:

```bash
./gradlew :core:tomljump-core:test --tests com.tomljump.core.ConfigNameNormalizerTest
```

Expected: compilation fails because `ConfigNameNormalizer` does not exist.

- [ ] **Step 3: Implement the normalizer**

Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/ConfigNameNormalizer.kt`:

```kotlin
package com.tomljump.core

object ConfigNameNormalizer {
    fun normalize(value: String): String {
        return value
            .replace(Regex("[^A-Za-z0-9]"), "")
            .lowercase()
    }

    fun matches(configName: String, codeName: String): Boolean {
        return normalize(configName) == normalize(codeName)
    }
}
```

- [ ] **Step 4: Run the focused test and verify it passes**

Run:

```bash
./gradlew :core:tomljump-core:test --tests com.tomljump.core.ConfigNameNormalizerTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the normalizer**

Run:

```bash
git add core/tomljump-core/src/main/kotlin/com/tomljump/core/ConfigNameNormalizer.kt core/tomljump-core/src/test/kotlin/com/tomljump/core/ConfigNameNormalizerTest.kt
git commit -m "feat: add config name normalization"
```

## Task 3: TOML Key Path Extraction

**Files:**
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/ExtractedConfigKeyPath.kt`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigKeyPathExtractor.kt`
- Create: `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigKeyPathExtractorTest.kt`

- [ ] **Step 1: Write failing extractor tests**

Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigKeyPathExtractorTest.kt`:

```kotlin
package com.tomljump.jetbrains.config

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TomlConfigKeyPathExtractorTest : BasePlatformTestCase() {
    fun testExtractsTablePath() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_key = "secret"
            """.trimIndent(),
        )

        val extracted = TomlConfigKeyPathExtractor.extract(findElementAt("openai"))

        assertEquals(listOf("openai"), extracted?.keyPath?.segments)
        assertEquals("openai", extracted?.text)
    }

    fun testExtractsKeyPathInsideTable() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_key = "secret"
            """.trimIndent(),
        )

        val extracted = TomlConfigKeyPathExtractor.extract(findElementAt("api_key"))

        assertEquals(listOf("openai", "api_key"), extracted?.keyPath?.segments)
        assertEquals("api_key", extracted?.text)
    }

    fun testExtractsNestedTableKeyPath() {
        myFixture.configureByText(
            "app.toml",
            """
            [servers.production]
            host = "127.0.0.1"
            """.trimIndent(),
        )

        val extracted = TomlConfigKeyPathExtractor.extract(findElementAt("host"))

        assertEquals(listOf("servers", "production", "host"), extracted?.keyPath?.segments)
        assertEquals("host", extracted?.text)
    }

    fun testIgnoresStringValue() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_key = "secret"
            """.trimIndent(),
        )

        assertNull(TomlConfigKeyPathExtractor.extract(findElementAt("secret")))
    }

    private fun findElementAt(marker: String): PsiElement {
        val offset = myFixture.file.text.indexOf(marker)
        check(offset >= 0) { "Missing marker: $marker" }
        return myFixture.file.findElementAt(offset) ?: error("No PSI element at offset $offset")
    }
}
```

- [ ] **Step 2: Run the extractor test and verify it fails**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.TomlConfigKeyPathExtractorTest
```

Expected: compilation fails because the extractor classes do not exist.

- [ ] **Step 3: Implement the extracted model**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/ExtractedConfigKeyPath.kt`:

```kotlin
package com.tomljump.jetbrains.config

import com.intellij.openapi.util.TextRange
import com.tomljump.core.ConfigKeyPath

data class ExtractedConfigKeyPath(
    val keyPath: ConfigKeyPath,
    val text: String,
    val rangeInElement: TextRange,
)
```

- [ ] **Step 4: Implement the TOML key path extractor**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigKeyPathExtractor.kt`:

```kotlin
package com.tomljump.jetbrains.config

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

object TomlConfigKeyPathExtractor {
    private val tablePattern = Regex("""^\s*\[([A-Za-z0-9_.-]+)]\s*$""")
    private val keyPattern = Regex("""^\s*([A-Za-z0-9_-]+)\s*=""")

    fun extract(element: PsiElement): ExtractedConfigKeyPath? {
        if (!isTomlFile(element)) return null
        val text = element.text ?: return null
        if (!isConfigIdentifier(text)) return null

        val fileText = element.containingFile?.text ?: return null
        val elementStart = element.textRange.startOffset
        val line = lineAt(fileText, elementStart)
        val lineText = fileText.substring(line.start, line.end)
        val offsetInLine = elementStart - line.start

        tablePattern.matchEntire(lineText)?.let { match ->
            val tableText = match.groupValues[1]
            val tableStart = lineText.indexOf(tableText)
            val tableEnd = tableStart + tableText.length
            if (offsetInLine in tableStart until tableEnd && tableText.split('.').contains(text)) {
                return ExtractedConfigKeyPath(
                    keyPath = ConfigKeyPath.from(tableText.split('.')),
                    text = text,
                    rangeInElement = TextRange.from(0, text.length),
                )
            }
        }

        keyPattern.find(lineText)?.let { match ->
            val keyText = match.groupValues[1]
            val keyStart = lineText.indexOf(keyText)
            val keyEnd = keyStart + keyText.length
            if (offsetInLine in keyStart until keyEnd && keyText == text) {
                val tableSegments = currentTableSegments(fileText, line.start)
                return ExtractedConfigKeyPath(
                    keyPath = ConfigKeyPath.from(tableSegments + keyText),
                    text = keyText,
                    rangeInElement = TextRange.from(0, text.length),
                )
            }
        }

        return null
    }

    private fun currentTableSegments(fileText: String, beforeOffset: Int): List<String> {
        val before = fileText.substring(0, beforeOffset)
        return before.lineSequence()
            .mapNotNull { tablePattern.matchEntire(it.trim())?.groupValues?.get(1) }
            .lastOrNull()
            ?.split('.')
            ?: emptyList()
    }

    private fun lineAt(fileText: String, offset: Int): LineRange {
        val start = fileText.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val end = fileText.indexOf('\n', offset).let { if (it < 0) fileText.length else it }
        return LineRange(start, end)
    }

    private fun isConfigIdentifier(value: String): Boolean {
        return value.isNotEmpty() && value.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }

    private fun isTomlFile(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val virtualFile = file.virtualFile
        return when {
            virtualFile != null -> virtualFile.extension.equals("toml", ignoreCase = true)
            else -> file.name.endsWith(".toml", ignoreCase = true)
        }
    }

    private data class LineRange(val start: Int, val end: Int)
}
```

- [ ] **Step 5: Run the extractor test and verify it passes**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.TomlConfigKeyPathExtractorTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit TOML key path extraction**

Run:

```bash
git add plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/ExtractedConfigKeyPath.kt plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigKeyPathExtractor.kt plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigKeyPathExtractorTest.kt
git commit -m "feat: extract TOML config key paths"
```

## Task 4: Config Reference Registration

**Files:**
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigReference.kt`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributor.kt`
- Modify: `plugins/jetbrains/src/main/resources/META-INF/plugin.xml`
- Create: `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributorTest.kt`

- [ ] **Step 1: Write failing reference registration tests**

Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributorTest.kt`:

```kotlin
package com.tomljump.jetbrains.config

import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TomlConfigReferenceContributorTest : BasePlatformTestCase() {
    fun testCreatesReferenceForTableName() {
        myFixture.configureByText(
            "app.toml",
            """
            [ope<caret>nai]
            api_key = "secret"
            """.trimIndent(),
        )

        val reference = referenceAtCaret()

        assertNotNull(reference)
        assertEquals("openai", reference.canonicalText)
    }

    fun testCreatesReferenceForKeyInsideTable() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_<caret>key = "secret"
            """.trimIndent(),
        )

        val reference = referenceAtCaret()

        assertNotNull(reference)
        assertEquals("openai.api_key", reference.canonicalText)
    }

    fun testDoesNotCreateReferenceForStringValue() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_key = "sec<caret>ret"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    private fun referenceAtCaret(): PsiReference? {
        val offset = myFixture.editor.caretModel.offset
        return myFixture.file.findReferenceAt(offset)
    }
}
```

- [ ] **Step 2: Run the reference registration test and verify it fails**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.TomlConfigReferenceContributorTest
```

Expected: tests fail because no config reference contributor is registered.

- [ ] **Step 3: Implement `TomlConfigReference`**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigReference.kt`:

```kotlin
package com.tomljump.jetbrains.config

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.tomljump.core.ConfigKeyPath
import com.tomljump.jetbrains.config.resolver.TomlConfigTargetResolver

class TomlConfigReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val keyPath: ConfigKeyPath,
    private val resolver: TomlConfigTargetResolver = TomlConfigTargetResolver(),
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, false) {
    override fun resolve(): PsiElement? = resolver.resolve(element, keyPath).firstOrNull()

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return resolver.resolve(element, keyPath)
            .map(::PsiElementResolveResult)
            .toTypedArray()
    }

    override fun getCanonicalText(): String = keyPath.display

    override fun getVariants(): Array<Any> = emptyArray()
}
```

- [ ] **Step 4: Add a temporary empty target resolver**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/TomlConfigTargetResolver.kt`:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

class TomlConfigTargetResolver {
    fun resolve(element: PsiElement, keyPath: ConfigKeyPath): List<PsiElement> = emptyList()
}
```

- [ ] **Step 5: Implement `TomlConfigReferenceContributor`**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributor.kt`:

```kotlin
package com.tomljump.jetbrains.config

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

class TomlConfigReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiElement::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val extracted = TomlConfigKeyPathExtractor.extract(element) ?: return PsiReference.EMPTY_ARRAY
                    return arrayOf(
                        TomlConfigReference(
                            element = element,
                            rangeInElement = extracted.rangeInElement,
                            keyPath = extracted.keyPath,
                        ),
                    )
                }
            },
        )
    }
}
```

- [ ] **Step 6: Register the contributor**

Modify `plugins/jetbrains/src/main/resources/META-INF/plugin.xml` so the extensions block contains both contributors:

```xml
    <extensions defaultExtensionNs="com.intellij">
        <psi.referenceContributor implementation="com.tomljump.jetbrains.TomlJumpReferenceContributor"/>
        <psi.referenceContributor implementation="com.tomljump.jetbrains.config.TomlConfigReferenceContributor"/>
    </extensions>
```

- [ ] **Step 7: Run the reference registration test and verify it passes**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.TomlConfigReferenceContributorTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit config reference registration**

Run:

```bash
git add plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config plugins/jetbrains/src/main/resources/META-INF/plugin.xml plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributorTest.kt
git commit -m "feat: register TOML config references"
```

## Task 5: Shared Source Pattern Resolver

**Files:**
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolver.kt`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/ConfigSourceTarget.kt`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/SourcePatternResolver.kt`
- Modify: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/TomlConfigTargetResolver.kt`

- [ ] **Step 1: Write failing resolver tests**

Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolverTest.kt`:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.tomljump.core.ConfigKeyPath
import kotlin.test.assertEquals

class LanguageConfigResolverTest : BasePlatformTestCase() {
    fun testGoResolverFindsTomlTag() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                APIKey string `toml:"api_key"`
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("APIKey"), targets.map { it.text })
    }
}
```

- [ ] **Step 2: Run the resolver test and verify it fails**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.resolver.LanguageConfigResolverTest
```

Expected: compilation fails because resolver classes do not exist.

- [ ] **Step 3: Implement resolver interfaces and target model**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolver.kt`:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

interface LanguageConfigResolver {
    fun resolve(project: Project, keyPath: ConfigKeyPath): List<PsiElement>
}
```

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/ConfigSourceTarget.kt`:

```kotlin
package com.tomljump.jetbrains.config.resolver

data class ConfigSourceTarget(
    val offset: Int,
    val label: String,
)
```

- [ ] **Step 4: Implement project source scanning**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/SourcePatternResolver.kt`:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.tomljump.core.ConfigKeyPath

abstract class SourcePatternResolver(
    private val extensions: Set<String>,
) : LanguageConfigResolver {
    final override fun resolve(project: Project, keyPath: ConfigKeyPath): List<PsiElement> {
        val scope = GlobalSearchScope.projectScope(project)
        return extensions.flatMap { extension ->
            FilenameIndex.getAllFilesByExt(project, extension, scope).flatMap { virtualFile ->
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@flatMap emptyList()
                findTargets(psiFile.text, keyPath).mapNotNull { target ->
                    psiFile.findElementAt(target.offset)?.let { leaf -> targetElement(leaf) }
                }
            }
        }.distinctBy { it.containingFile?.virtualFile?.path.orEmpty() + ":" + it.textRange.startOffset }
    }

    protected abstract fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget>

    private fun targetElement(leaf: PsiElement): PsiElement {
        var current = leaf
        while (current.parent != null && current.parent.textRange == current.textRange) {
            current = current.parent
        }
        return current
    }
}
```

- [ ] **Step 5: Update `TomlConfigTargetResolver` to coordinate resolvers**

Replace `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/TomlConfigTargetResolver.kt` with:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

class TomlConfigTargetResolver(
    private val resolvers: List<LanguageConfigResolver> = listOf(
        GoConfigResolver(),
        PythonConfigResolver(),
        JavaConfigResolver(),
        TypeScriptConfigResolver(),
        JavaScriptConfigResolver(),
    ),
) {
    fun resolve(element: PsiElement, keyPath: ConfigKeyPath): List<PsiElement> {
        val project = element.project
        return resolvers.flatMap { resolver ->
            try {
                resolver.resolve(project, keyPath)
            } catch (error: Throwable) {
                logger.warn("TomlJump config resolver failed for ${keyPath.display}", error)
                emptyList()
            }
        }.distinctBy { it.containingFile?.virtualFile?.path.orEmpty() + ":" + it.textRange.startOffset }
    }

    companion object {
        private val logger = Logger.getInstance(TomlConfigTargetResolver::class.java)
    }
}
```

- [ ] **Step 6: Add temporary empty language resolver classes**

Create each file with this exact class shape, changing the class name:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath

class PythonConfigResolver : SourcePatternResolver(setOf("py")) {
    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> = emptyList()
}
```

Files and class names:

- `GoConfigResolver.kt`: `class GoConfigResolver : SourcePatternResolver(setOf("go"))`
- `PythonConfigResolver.kt`: `class PythonConfigResolver : SourcePatternResolver(setOf("py"))`
- `JavaConfigResolver.kt`: `class JavaConfigResolver : SourcePatternResolver(setOf("java"))`
- `TypeScriptConfigResolver.kt`: `class TypeScriptConfigResolver : SourcePatternResolver(setOf("ts", "tsx"))`
- `JavaScriptConfigResolver.kt`: `class JavaScriptConfigResolver : SourcePatternResolver(setOf("js", "jsx"))`

The Go resolver remains intentionally failing until Task 6.

## Task 6: Go Resolver

**Files:**
- Modify: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/GoConfigResolver.kt`
- Modify: `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolverTest.kt`

- [ ] **Step 1: Extend Go tests before implementation**

Add this test to `LanguageConfigResolverTest`:

```kotlin
    fun testGoResolverFindsNormalizedFieldName() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                BaseURL string
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "base_url"))

        assertEquals(listOf("BaseURL"), targets.map { it.text })
    }
```

- [ ] **Step 2: Run Go resolver tests and verify they fail**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.resolver.LanguageConfigResolverTest
```

Expected: Go resolver tests fail because the resolver returns no targets.

- [ ] **Step 3: Implement Go tag and field matching**

Replace `GoConfigResolver.kt` with:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class GoConfigResolver : SourcePatternResolver(setOf("go")) {
    private val tagPattern = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s+[^`\n]+`([^`]+)`""")
    private val fieldPattern = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s+[A-Za-z_][A-Za-z0-9_.*\[\]]*""")
    private val aliasPattern = Regex("""(?:toml|json|mapstructure|yaml):"([^",]+)""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        val leaf = keyPath.leaf
        val tagMatches = tagPattern.findAll(sourceText).mapNotNull { match ->
            val fieldName = match.groupValues[1]
            val tagText = match.groupValues[2]
            val aliases = aliasPattern.findAll(tagText).map { it.groupValues[1] }.toList()
            if (aliases.any { it == leaf }) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(fieldName), fieldName)
            } else {
                null
            }
        }.toList()

        if (tagMatches.isNotEmpty()) return tagMatches

        return fieldPattern.findAll(sourceText).mapNotNull { match ->
            val fieldName = match.groupValues[1]
            if (ConfigNameNormalizer.matches(leaf, fieldName)) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(fieldName), fieldName)
            } else {
                null
            }
        }.toList()
    }
}
```

- [ ] **Step 4: Run Go resolver tests and verify they pass**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.resolver.LanguageConfigResolverTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit resolver framework and Go support**

Run:

```bash
git add plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolverTest.kt
git commit -m "feat: resolve TOML config keys to Go fields"
```

## Task 7: Python, Java, TypeScript, and JavaScript Resolvers

**Files:**
- Modify: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/PythonConfigResolver.kt`
- Modify: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/JavaConfigResolver.kt`
- Modify: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/TypeScriptConfigResolver.kt`
- Modify: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/JavaScriptConfigResolver.kt`
- Modify: `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolverTest.kt`

- [ ] **Step 1: Add failing tests for the remaining languages**

Add these tests to `LanguageConfigResolverTest`:

```kotlin
    fun testPythonResolverFindsAnnotatedField() {
        myFixture.addFileToProject(
            "config.py",
            """
            from dataclasses import dataclass

            @dataclass
            class OpenAIConfig:
                api_key: str
            """.trimIndent(),
        )

        val targets = PythonConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("api_key"), targets.map { it.text })
    }

    fun testJavaResolverFindsJsonPropertyAnnotation() {
        myFixture.addFileToProject(
            "OpenAIConfig.java",
            """
            package config;

            class OpenAIConfig {
                @JsonProperty("api_key")
                private String apiKey;
            }
            """.trimIndent(),
        )

        val targets = JavaConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("apiKey"), targets.map { it.text })
    }

    fun testTypeScriptResolverFindsInterfaceProperty() {
        myFixture.addFileToProject(
            "config.ts",
            """
            interface OpenAIConfig {
                apiKey: string
            }
            """.trimIndent(),
        )

        val targets = TypeScriptConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("apiKey"), targets.map { it.text })
    }

    fun testJavaScriptResolverFindsQuotedObjectProperty() {
        myFixture.addFileToProject(
            "config.js",
            """
            const openai = {
                "api_key": ""
            }
            """.trimIndent(),
        )

        val targets = JavaScriptConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("api_key"), targets.map { it.text.trim('"') })
    }
```

- [ ] **Step 2: Run resolver tests and verify the new tests fail**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.resolver.LanguageConfigResolverTest
```

Expected: Python, Java, TypeScript, and JavaScript tests fail because their resolvers return no targets.

- [ ] **Step 3: Implement Python resolver**

Replace `PythonConfigResolver.kt` with:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class PythonConfigResolver : SourcePatternResolver(setOf("py")) {
    private val fieldPattern = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s*(?::|=)""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        return fieldPattern.findAll(sourceText).mapNotNull { match ->
            val name = match.groupValues[1]
            if (ConfigNameNormalizer.matches(keyPath.leaf, name)) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(name), name)
            } else {
                null
            }
        }.toList()
    }
}
```

- [ ] **Step 4: Implement Java resolver**

Replace `JavaConfigResolver.kt` with:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class JavaConfigResolver : SourcePatternResolver(setOf("java")) {
    private val annotatedFieldPattern = Regex("""(?s)@JsonProperty\("([^"]+)"\)\s+(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)""")
    private val fieldPattern = Regex("""(?m)^\s*(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        val annotated = annotatedFieldPattern.findAll(sourceText).mapNotNull { match ->
            val alias = match.groupValues[1]
            val name = match.groupValues[2]
            if (alias == keyPath.leaf) {
                ConfigSourceTarget(match.range.first + match.value.lastIndexOf(name), name)
            } else {
                null
            }
        }.toList()

        if (annotated.isNotEmpty()) return annotated

        return fieldPattern.findAll(sourceText).mapNotNull { match ->
            val name = match.groupValues[1]
            if (ConfigNameNormalizer.matches(keyPath.leaf, name)) {
                ConfigSourceTarget(match.range.first + match.value.lastIndexOf(name), name)
            } else {
                null
            }
        }.toList()
    }
}
```

- [ ] **Step 5: Implement TypeScript resolver**

Replace `TypeScriptConfigResolver.kt` with:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class TypeScriptConfigResolver : SourcePatternResolver(setOf("ts", "tsx")) {
    private val propertyPattern = Regex("""(?m)^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z_][A-Za-z0-9_]*))\??\s*:""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        return propertyPattern.findAll(sourceText).mapNotNull { match ->
            val quotedDouble = match.groupValues[1]
            val quotedSingle = match.groupValues[2]
            val identifier = match.groupValues[3]
            val name = quotedDouble.ifEmpty { quotedSingle.ifEmpty { identifier } }
            if (name == keyPath.leaf || ConfigNameNormalizer.matches(keyPath.leaf, name)) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(name), name)
            } else {
                null
            }
        }.toList()
    }
}
```

- [ ] **Step 6: Implement JavaScript resolver**

Replace `JavaScriptConfigResolver.kt` with:

```kotlin
package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class JavaScriptConfigResolver : SourcePatternResolver(setOf("js", "jsx")) {
    private val propertyPattern = Regex("""(?m)^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z_][A-Za-z0-9_]*))\s*:""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        return propertyPattern.findAll(sourceText).mapNotNull { match ->
            val quotedDouble = match.groupValues[1]
            val quotedSingle = match.groupValues[2]
            val identifier = match.groupValues[3]
            val name = quotedDouble.ifEmpty { quotedSingle.ifEmpty { identifier } }
            if (name == keyPath.leaf || ConfigNameNormalizer.matches(keyPath.leaf, name)) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(name), name)
            } else {
                null
            }
        }.toList()
    }
}
```

- [ ] **Step 7: Run all resolver tests and verify they pass**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.resolver.LanguageConfigResolverTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit remaining language resolver support**

Run:

```bash
git add plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolverTest.kt
git commit -m "feat: resolve TOML config keys across languages"
```

## Task 8: End-to-End Config Navigation

**Files:**
- Modify: `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributorTest.kt`

- [ ] **Step 1: Add failing end-to-end Go navigation test**

Add this test to `TomlConfigReferenceContributorTest`:

```kotlin
    fun testResolvesTomlKeyToGoStructTag() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                APIKey string `toml:"api_key"`
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_<caret>key = "secret"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve()

        assertEquals("APIKey", resolved?.text)
    }
```

- [ ] **Step 2: Run the end-to-end test and verify it passes**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.config.TomlConfigReferenceContributorTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run existing file-path navigation tests**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.TomlJumpReferenceContributorTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit end-to-end navigation coverage**

Run:

```bash
git add plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/TomlConfigReferenceContributorTest.kt
git commit -m "test: cover TOML config key navigation"
```

## Task 9: Fixtures and Documentation

**Files:**
- Modify: `fixtures/go/handlers/user.go`
- Modify: `fixtures/python/app/user.py`
- Modify: `fixtures/java/src/main/java/com/example/PaymentClient.java`
- Modify: `fixtures/typescript/src/jobs/sync.ts`
- Modify: `fixtures/javascript/src/jobs/sync.js`
- Modify: `AGENT.md`
- Modify: `TODO.md`

- [ ] **Step 1: Add compact fixture examples**

Add these snippets to the relevant fixture files without removing existing fixture content.

Go fixture:

```go
type OpenAIConfig struct {
	APIKey  string `toml:"api_key"`
	Model   string `toml:"model"`
	BaseURL string `toml:"base_url"`
}
```

Python fixture:

```python
class OpenAIConfig:
    api_key: str
    model: str
    base_url: str
```

Java fixture:

```java
class OpenAIConfig {
    @JsonProperty("api_key")
    private String apiKey;
    private String model;
    private String baseUrl;
}
```

TypeScript fixture:

```typescript
interface OpenAIConfig {
  apiKey: string
  model: string
  baseUrl: string
}
```

JavaScript fixture:

```javascript
const openaiConfig = {
  "api_key": "",
  model: "",
  baseUrl: "",
}
```

- [ ] **Step 2: Update maintainer notes**

In `AGENT.md`, replace the current MVP limitation:

```text
- The current JetBrains MVP publishes file-path references only. Symbol-shaped strings are classified in core but not exposed as PSI references until language-specific resolvers exist.
```

with:

```text
- The JetBrains plugin supports TOML string file-path navigation and TOML table/key navigation to high-confidence config declarations in Go, Python, Java, TypeScript, and JavaScript source files.
- Config key navigation intentionally uses optional resolver adapters so missing language plugins do not prevent plugin startup.
```

- [ ] **Step 3: Update backlog**

In `TODO.md`, replace:

```text
- Go struct tag mapping
```

with:

```text
- Deeper Go struct graph traversal for nested TOML table paths
```

- [ ] **Step 4: Run documentation diff check**

Run:

```bash
git diff -- AGENT.md TODO.md fixtures
```

Expected: only fixture examples and maintainer/backlog wording changed.

- [ ] **Step 5: Commit fixtures and docs**

Run:

```bash
git add AGENT.md TODO.md fixtures
git commit -m "docs: describe config key navigation support"
```

## Task 10: Full Verification and Packaging

**Files:**
- No code changes expected.

- [ ] **Step 1: Run full JetBrains verification**

Run:

```bash
./scripts/verify/jetbrains.sh
```

Expected:

```text
BUILD SUCCESSFUL
```

for core tests, JetBrains tests, and `:plugins:jetbrains:buildPlugin`.

- [ ] **Step 2: Confirm plugin ZIP exists**

Run:

```bash
ls -lt plugins/jetbrains/build/distributions
```

Expected: a current `jetbrains-0.1.0.zip` file exists.

- [ ] **Step 3: Inspect final status**

Run:

```bash
git status --short
```

Expected: no unstaged or staged changes.

## Self-Review

- Spec coverage: the plan covers key path extraction, TOML key/table references, resolver coordination, Go priority support, Python/Java/TypeScript/JavaScript MVP support, optional startup behavior, tests, fixtures, docs, and packaging verification.
- Placeholder scan: no placeholder tasks remain. Each implementation task names the exact files and commands.
- Type consistency: `ConfigKeyPath`, `ConfigNameNormalizer`, `ExtractedConfigKeyPath`, `TomlConfigReference`, `LanguageConfigResolver`, `ConfigSourceTarget`, `SourcePatternResolver`, and `TomlConfigTargetResolver` are introduced before any later task uses them.
