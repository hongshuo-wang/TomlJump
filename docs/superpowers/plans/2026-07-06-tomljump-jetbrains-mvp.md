# TomlJump JetBrains MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first TomlJump MVP as a JetBrains-first monorepo with Kotlin core logic, a JetBrains plugin shell, shared fixtures, maintainer docs, and a reserved VS Code extension folder.

**Architecture:** The MVP uses Kotlin-first core logic under `core/tomljump-core` and consumes it from `plugins/jetbrains`. The JetBrains plugin registers TOML string references and resolves high-confidence targets, starting with file path references and lightweight symbol-shaped classification hooks for Go, Python, Java, TypeScript, and JavaScript. VS Code remains a reserved package folder with documentation only until the JetBrains MVP is stable.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, IntelliJ Platform Gradle Plugin 2.x, IntelliJ Platform PSI/reference APIs, JUnit 5/Kotlin test, shared fixture projects.

---

## File Structure

- Create `settings.gradle.kts`: declares the Gradle root project and includes `core:tomljump-core` and `plugins:jetbrains`.
- Create `build.gradle.kts`: root Gradle convention with Kotlin and IntelliJ Platform plugin versions.
- Create `gradle.properties`: JVM, Kotlin, and plugin metadata properties.
- Create `core/tomljump-core/build.gradle.kts`: Kotlin/JVM module for editor-agnostic reference classification.
- Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/ReferenceKind.kt`: enum for reference classes.
- Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/TomlReference.kt`: immutable model for classified TOML string references.
- Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/TomlReferenceClassifier.kt`: classifier for path, module, class, and callable string shapes.
- Create `core/tomljump-core/src/test/kotlin/com/tomljump/core/TomlReferenceClassifierTest.kt`: unit tests for common string reference classification.
- Create `plugins/jetbrains/build.gradle.kts`: JetBrains plugin module configuration.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlJumpReferenceContributor.kt`: registers reference provider for TOML PSI string elements.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlJumpStringReference.kt`: JetBrains `PsiReferenceBase` implementation for TOML strings.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlStringValueExtractor.kt`: extracts unquoted string text and text ranges from TOML PSI elements.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/TomlJumpResolver.kt`: tries file and symbol resolvers in stable order.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/FilePathResolver.kt`: resolves existing files through IntelliJ project roots and virtual files.
- Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/SymbolShapeResolver.kt`: conservative resolver that intentionally returns no targets for symbol-shaped references until language-specific PSI resolution is implemented.
- Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/TomlJumpReferenceContributorTest.kt`: fixture tests for TOML file path references.
- Create `plugins/jetbrains/src/main/resources/META-INF/plugin.xml`: plugin metadata and reference contributor registration.
- Create `fixtures/go`, `fixtures/python`, `fixtures/java`, `fixtures/typescript`, `fixtures/javascript`, and `fixtures/mixed`: sample projects used by tests and future manual QA.
- Create `plugins/vscode/README.md`: reserved VS Code extension scope and dependency on shared fixtures.
- Create `AGENT.md`: maintainer and agent instructions.
- Create `TODO.md`: advanced feature backlog.
- Create `docs/publishing/jetbrains.md`: project-specific JetBrains build, signing, and publishing notes.
- Create `docs/publishing/vscode.md`: reserved VS Code publishing notes.
- Create `scripts/verify/jetbrains.sh`: local verification script for Gradle checks.

---

### Task 1: Monorepo Gradle Skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `core/tomljump-core/build.gradle.kts`
- Create: `plugins/jetbrains/build.gradle.kts`

- [ ] **Step 1: Write the root Gradle settings**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

rootProject.name = "TomlJump"

include(":core:tomljump-core")
include(":plugins:jetbrains")
```

- [ ] **Step 2: Write the root Gradle build file**

Create `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.0.21" apply false
    id("org.jetbrains.intellij.platform") version "2.2.1" apply false
}

allprojects {
    group = "com.tomljump"
    version = providers.gradleProperty("pluginVersion").get()
}
```

- [ ] **Step 3: Write shared Gradle properties**

Create `gradle.properties`:

```properties
pluginVersion=0.1.0
pluginSinceBuild=243
pluginUntilBuild=253.*

kotlin.code.style=official
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
```

- [ ] **Step 4: Write the core module build file**

Create `core/tomljump-core/build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 5: Write the JetBrains module build file**

Create `plugins/jetbrains/build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:tomljump-core"))

    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("org.toml.lang")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation(kotlin("test"))
}

intellijPlatform {
    pluginConfiguration {
        id = "com.tomljump"
        name = "TomlJump"
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 6: Run Gradle project discovery**

Run:

```bash
./gradlew projects
```

Expected: Gradle lists root project `TomlJump` and subprojects `:core:tomljump-core` and `:plugins:jetbrains`.

- [ ] **Step 7: Commit the skeleton**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties core/tomljump-core/build.gradle.kts plugins/jetbrains/build.gradle.kts
git commit -m "build: scaffold TomlJump monorepo"
```

---

### Task 2: Core Reference Classification

**Files:**
- Create: `core/tomljump-core/src/main/kotlin/com/tomljump/core/ReferenceKind.kt`
- Create: `core/tomljump-core/src/main/kotlin/com/tomljump/core/TomlReference.kt`
- Create: `core/tomljump-core/src/main/kotlin/com/tomljump/core/TomlReferenceClassifier.kt`
- Create: `core/tomljump-core/src/test/kotlin/com/tomljump/core/TomlReferenceClassifierTest.kt`

- [ ] **Step 1: Write failing classifier tests**

Create `core/tomljump-core/src/test/kotlin/com/tomljump/core/TomlReferenceClassifierTest.kt`:

```kotlin
package com.tomljump.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TomlReferenceClassifierTest {
    private val classifier = TomlReferenceClassifier()

    @Test
    fun `classifies relative file path`() {
        val reference = classifier.classify("./schemas/user.json")

        assertEquals(ReferenceKind.FILE_PATH, reference?.kind)
        assertEquals("./schemas/user.json", reference?.rawValue)
        assertEquals("schemas/user.json", reference?.lookupValue)
    }

    @Test
    fun `classifies workspace relative file path`() {
        val reference = classifier.classify("templates/email.html")

        assertEquals(ReferenceKind.FILE_PATH, reference?.kind)
        assertEquals("templates/email.html", reference?.lookupValue)
    }

    @Test
    fun `classifies python callable reference`() {
        val reference = classifier.classify("app.user:create_user")

        assertEquals(ReferenceKind.CALLABLE, reference?.kind)
        assertEquals("app.user", reference?.qualifier)
        assertEquals("create_user", reference?.member)
    }

    @Test
    fun `classifies java class reference`() {
        val reference = classifier.classify("com.example.PaymentClient")

        assertEquals(ReferenceKind.CLASS_OR_MODULE, reference?.kind)
        assertEquals("com.example.PaymentClient", reference?.lookupValue)
    }

    @Test
    fun `ignores arbitrary prose`() {
        assertNull(classifier.classify("hello world"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :core:tomljump-core:test --tests com.tomljump.core.TomlReferenceClassifierTest
```

Expected: FAIL because `TomlReferenceClassifier`, `TomlReference`, and `ReferenceKind` do not exist.

- [ ] **Step 3: Add reference kind enum**

Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/ReferenceKind.kt`:

```kotlin
package com.tomljump.core

enum class ReferenceKind {
    FILE_PATH,
    CLASS_OR_MODULE,
    CALLABLE
}
```

- [ ] **Step 4: Add reference model**

Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/TomlReference.kt`:

```kotlin
package com.tomljump.core

data class TomlReference(
    val rawValue: String,
    val lookupValue: String,
    val kind: ReferenceKind,
    val qualifier: String? = null,
    val member: String? = null,
)
```

- [ ] **Step 5: Add classifier implementation**

Create `core/tomljump-core/src/main/kotlin/com/tomljump/core/TomlReferenceClassifier.kt`:

```kotlin
package com.tomljump.core

class TomlReferenceClassifier {
    private val pathLike = Regex("""^\.?\.?/?[\w@~.-]+(?:/[\w@~.-]+)+(?:\.[A-Za-z0-9]+)?$""")
    private val classOrModuleLike = Regex("""^[A-Za-z_][\w]*(?:\.[A-Za-z_][\w]*)+$""")
    private val callableLike = Regex("""^([A-Za-z_][\w]*(?:\.[A-Za-z_][\w]*)*)[:#]([A-Za-z_][\w]*)$""")

    fun classify(rawValue: String): TomlReference? {
        val value = rawValue.trim()
        if (value.isEmpty() || value.any(Char::isWhitespace)) {
            return null
        }

        callableLike.matchEntire(value)?.let { match ->
            val qualifier = match.groupValues[1]
            val member = match.groupValues[2]
            return TomlReference(
                rawValue = rawValue,
                lookupValue = value,
                kind = ReferenceKind.CALLABLE,
                qualifier = qualifier,
                member = member,
            )
        }

        if (pathLike.matches(value)) {
            return TomlReference(
                rawValue = rawValue,
                lookupValue = value.removePrefix("./"),
                kind = ReferenceKind.FILE_PATH,
            )
        }

        if (classOrModuleLike.matches(value)) {
            return TomlReference(
                rawValue = rawValue,
                lookupValue = value,
                kind = ReferenceKind.CLASS_OR_MODULE,
            )
        }

        return null
    }
}
```

- [ ] **Step 6: Run core tests**

Run:

```bash
./gradlew :core:tomljump-core:test
```

Expected: PASS.

- [ ] **Step 7: Commit core classifier**

```bash
git add core/tomljump-core/src/main/kotlin core/tomljump-core/src/test/kotlin
git commit -m "feat: classify TOML string references"
```

---

### Task 3: Shared Fixture Projects

**Files:**
- Create: `fixtures/mixed/app.toml`
- Create: `fixtures/mixed/schemas/user.json`
- Create: `fixtures/mixed/templates/email.html`
- Create: `fixtures/python/app/user.py`
- Create: `fixtures/java/src/main/java/com/example/PaymentClient.java`
- Create: `fixtures/go/handlers/user.go`
- Create: `fixtures/typescript/src/jobs/sync.ts`
- Create: `fixtures/javascript/src/jobs/sync.js`

- [ ] **Step 1: Create mixed TOML fixture**

Create `fixtures/mixed/app.toml`:

```toml
schema = "./schemas/user.json"
template = "templates/email.html"
python_handler = "app.user:create_user"
java_factory = "com.example.PaymentClient"
go_handler = "handlers.UserHandler"
ts_job = "src/jobs/sync"
js_job = "src/jobs/sync"
plain_value = "hello world"
```

- [ ] **Step 2: Create file path targets**

Create `fixtures/mixed/schemas/user.json`:

```json
{
  "type": "object",
  "required": ["id"]
}
```

Create `fixtures/mixed/templates/email.html`:

```html
<main>User email template</main>
```

- [ ] **Step 3: Create Python target**

Create `fixtures/python/app/user.py`:

```python
def create_user():
    return {"ok": True}
```

- [ ] **Step 4: Create Java target**

Create `fixtures/java/src/main/java/com/example/PaymentClient.java`:

```java
package com.example;

public class PaymentClient {
    public boolean ping() {
        return true;
    }
}
```

- [ ] **Step 5: Create Go target**

Create `fixtures/go/handlers/user.go`:

```go
package handlers

func UserHandler() bool {
	return true
}
```

- [ ] **Step 6: Create TypeScript and JavaScript targets**

Create `fixtures/typescript/src/jobs/sync.ts`:

```typescript
export function sync(): boolean {
  return true;
}
```

Create `fixtures/javascript/src/jobs/sync.js`:

```javascript
export function sync() {
  return true;
}
```

- [ ] **Step 7: Commit fixtures**

```bash
git add fixtures
git commit -m "test: add shared TomlJump fixtures"
```

---

### Task 4: JetBrains Plugin Metadata And Reference Registration

**Files:**
- Create: `plugins/jetbrains/src/main/resources/META-INF/plugin.xml`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlJumpReferenceContributor.kt`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlStringValueExtractor.kt`

- [ ] **Step 1: Write plugin metadata**

Create `plugins/jetbrains/src/main/resources/META-INF/plugin.xml`:

```xml
<idea-plugin>
    <id>com.tomljump</id>
    <name>TomlJump</name>
    <vendor email="support@tomljump.dev" url="https://github.com/harrison/TomlJump">TomlJump</vendor>

    <description><![CDATA[
        Navigate from TOML string references to project files and code symbols.
    ]]></description>

    <depends>com.intellij.modules.platform</depends>
    <depends>org.toml.lang</depends>

    <extensions defaultExtensionNs="com.intellij">
        <psi.referenceContributor implementation="com.tomljump.jetbrains.TomlJumpReferenceContributor"/>
    </extensions>
</idea-plugin>
```

- [ ] **Step 2: Add TOML string extractor**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlStringValueExtractor.kt`:

```kotlin
package com.tomljump.jetbrains

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

data class ExtractedTomlString(
    val value: String,
    val rangeInElement: TextRange,
)

object TomlStringValueExtractor {
    fun extract(element: PsiElement): ExtractedTomlString? {
        val text = element.text ?: return null
        if (text.length < 2) return null

        val quote = text.first()
        if ((quote != '"' && quote != '\'') || text.last() != quote) {
            return null
        }

        val value = text.substring(1, text.length - 1)
        return ExtractedTomlString(
            value = value,
            rangeInElement = TextRange(1, text.length - 1),
        )
    }
}
```

- [ ] **Step 3: Add reference contributor**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlJumpReferenceContributor.kt`:

```kotlin
package com.tomljump.jetbrains

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import com.tomljump.core.TomlReferenceClassifier
import com.tomljump.jetbrains.resolver.TomlJumpResolver

class TomlJumpReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            psiElement(),
            object : PsiReferenceProvider() {
                private val classifier = TomlReferenceClassifier()

                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    if (element.containingFile?.name?.endsWith(".toml") != true) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    val extracted = TomlStringValueExtractor.extract(element) ?: return PsiReference.EMPTY_ARRAY
                    val reference = classifier.classify(extracted.value) ?: return PsiReference.EMPTY_ARRAY

                    return arrayOf(
                        TomlJumpStringReference(
                            element = element,
                            rangeInElement = extracted.rangeInElement,
                            reference = reference,
                            resolver = TomlJumpResolver(),
                        ),
                    )
                }
            },
        )
    }
}
```

- [ ] **Step 4: Run JetBrains module compilation to verify missing classes**

Run:

```bash
./gradlew :plugins:jetbrains:compileKotlin
```

Expected: FAIL because `TomlJumpStringReference` and `TomlJumpResolver` are referenced but not implemented yet.

- [ ] **Step 5: Commit metadata and registration once Task 5 completes**

Do not commit this task alone if compilation is still failing. Include these files in the Task 5 commit after resolver and reference classes compile.

---

### Task 5: JetBrains Reference Resolution

**Files:**
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlJumpStringReference.kt`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/TomlJumpResolver.kt`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/FilePathResolver.kt`
- Create: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/SymbolShapeResolver.kt`

- [ ] **Step 1: Add resolver facade**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/TomlJumpResolver.kt`:

```kotlin
package com.tomljump.jetbrains.resolver

import com.intellij.psi.PsiElement
import com.tomljump.core.ReferenceKind
import com.tomljump.core.TomlReference

class TomlJumpResolver {
    private val filePathResolver = FilePathResolver()
    private val symbolShapeResolver = SymbolShapeResolver()

    fun resolve(context: PsiElement, reference: TomlReference): PsiElement? {
        return multiResolve(context, reference).firstOrNull()
    }

    fun multiResolve(context: PsiElement, reference: TomlReference): List<PsiElement> {
        return when (reference.kind) {
            ReferenceKind.FILE_PATH -> filePathResolver.resolve(context, reference)
            ReferenceKind.CLASS_OR_MODULE,
            ReferenceKind.CALLABLE,
            -> symbolShapeResolver.resolve(context, reference)
        }
    }
}
```

- [ ] **Step 2: Add file path resolver**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/FilePathResolver.kt`:

```kotlin
package com.tomljump.jetbrains.resolver

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.tomljump.core.TomlReference
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile

class FilePathResolver {
    fun resolve(context: PsiElement, reference: TomlReference): List<PsiElement> {
        val containingFile = context.containingFile?.virtualFile ?: return emptyList()
        val baseDir = containingFile.parent ?: return emptyList()

        val candidates = linkedSetOf<VirtualFile>()
        val relativeCandidate = baseDir.findFileByRelativePath(reference.lookupValue)
        if (relativeCandidate != null && !relativeCandidate.isDirectory) {
            candidates.add(relativeCandidate)
        }

        val localCandidate = resolveLocalPath(baseDir.path, reference.lookupValue)
        if (localCandidate != null && !localCandidate.isDirectory) {
            candidates.add(localCandidate)
        }

        val psiManager = PsiManager.getInstance(context.project)
        return candidates.mapNotNull { psiManager.findFile(it) }
    }

    private fun resolveLocalPath(baseDir: String, lookupValue: String): VirtualFile? {
        val path = Path.of(baseDir).resolve(lookupValue).normalize()
        if (!path.isRegularFile()) {
            return null
        }
        return LocalFileSystem.getInstance().findFileByPath(path.absolutePathString())
    }
}
```

- [ ] **Step 3: Add conservative symbol resolver**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/resolver/SymbolShapeResolver.kt`:

```kotlin
package com.tomljump.jetbrains.resolver

import com.intellij.psi.PsiElement
import com.tomljump.core.TomlReference

class SymbolShapeResolver {
    fun resolve(context: PsiElement, reference: TomlReference): List<PsiElement> {
        return emptyList()
    }
}
```

- [ ] **Step 4: Add PSI reference implementation**

Create `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/TomlJumpStringReference.kt`:

```kotlin
package com.tomljump.jetbrains

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.util.PsiUtilCore
import com.tomljump.core.TomlReference
import com.tomljump.jetbrains.resolver.TomlJumpResolver

class TomlJumpStringReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val reference: TomlReference,
    private val resolver: TomlJumpResolver,
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, false) {

    override fun resolve(): PsiElement? {
        return resolver.resolve(element, reference)
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return resolver.multiResolve(element, reference)
            .map { target -> PsiElementResolveResult(target) }
            .toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()

    override fun handleElementRename(newElementName: String): PsiElement {
        return element
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val resolved = resolve() ?: return false
        return resolved == element || PsiUtilCore.getVirtualFile(resolved) == PsiUtilCore.getVirtualFile(element)
    }
}
```

- [ ] **Step 5: Run compilation**

Run:

```bash
./gradlew :plugins:jetbrains:compileKotlin
```

Expected: PASS.

- [ ] **Step 6: Commit JetBrains reference code**

```bash
git add plugins/jetbrains/src/main plugins/jetbrains/build.gradle.kts
git commit -m "feat: add JetBrains TOML reference resolution"
```

---

### Task 6: JetBrains Reference Tests

**Files:**
- Create: `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/TomlStringValueExtractorTest.kt`
- Create: `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/TomlJumpReferenceContributorTest.kt`

- [ ] **Step 1: Add extractor tests**

Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/TomlStringValueExtractorTest.kt`:

```kotlin
package com.tomljump.jetbrains

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TomlStringValueExtractorTest : BasePlatformTestCase() {
    fun testExtractsDoubleQuotedStringValue() {
        val file = myFixture.configureByText("app.toml", "schema = \"./schemas/user.json\"")
        val element = file.findElementAt(file.text.indexOf("./schemas"))!!.parent

        val extracted = TomlStringValueExtractor.extract(element)

        assertEquals("./schemas/user.json", extracted?.value)
    }

    fun testIgnoresUnquotedElement() {
        val file = myFixture.configureByText("app.toml", "enabled = true")
        val element = file.findElementAt(file.text.indexOf("true"))!!.parent

        val extracted = TomlStringValueExtractor.extract(element)

        assertNull(extracted)
    }
}
```

- [ ] **Step 2: Add reference contributor tests**

Create `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/TomlJumpReferenceContributorTest.kt`:

```kotlin
package com.tomljump.jetbrains

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TomlJumpReferenceContributorTest : BasePlatformTestCase() {
    fun testResolvesRelativeFilePathFromTomlString() {
        myFixture.addFileToProject("schemas/user.json", """{"type":"object"}""")
        val file = myFixture.configureByText("app.toml", "schema = \"./schemas/user.json\"")
        val offset = file.text.indexOf("schemas/user.json")

        val reference = file.findReferenceAt(offset)
        val resolved = reference?.resolve()

        assertNotNull(reference)
        assertNotNull(resolved)
        assertEquals("user.json", resolved?.containingFile?.name)
    }

    fun testDoesNotCreateReferenceForArbitraryString() {
        val file = myFixture.configureByText("app.toml", "label = \"hello world\"")
        val offset = file.text.indexOf("hello")

        val reference = file.findReferenceAt(offset)

        assertNull(reference)
    }
}
```

- [ ] **Step 3: Run JetBrains tests to verify behavior**

Run:

```bash
./gradlew :plugins:jetbrains:test --tests com.tomljump.jetbrains.TomlStringValueExtractorTest --tests com.tomljump.jetbrains.TomlJumpReferenceContributorTest
```

Expected: PASS. If the TOML PSI string element is not the parent of the leaf found in tests, inspect `element.parent` levels in the debugger or test output and adjust `TomlStringValueExtractor.extract` calls in the test setup only; do not broaden the production provider to every PSI element without a failing test that justifies it.

- [ ] **Step 4: Commit tests**

```bash
git add plugins/jetbrains/src/test/kotlin
git commit -m "test: cover JetBrains TOML references"
```

---

### Task 7: Maintainer Docs, Backlog, And Reserved VS Code Folder

**Files:**
- Create: `AGENT.md`
- Create: `TODO.md`
- Create: `plugins/vscode/README.md`
- Create: `docs/publishing/jetbrains.md`
- Create: `docs/publishing/vscode.md`

- [ ] **Step 1: Write agent instructions**

Create `AGENT.md`:

```markdown
# TomlJump Agent Guide

## Project Shape

TomlJump is a monorepo for JetBrains and VS Code plugins. The JetBrains plugin is the first implementation target. The VS Code folder is reserved for a later extension that must reuse the same fixtures and reference classification behavior.

## Boundaries

- `core/tomljump-core` contains editor-agnostic Kotlin logic.
- `plugins/jetbrains` contains IntelliJ Platform integration only.
- `plugins/vscode` must not duplicate product decisions from the JetBrains plugin without updating shared fixtures.
- `fixtures` contains cross-editor sample projects.
- `docs/publishing` contains release process notes.

## Commands

- Run all checks: `./gradlew check`
- Run core tests: `./gradlew :core:tomljump-core:test`
- Run JetBrains tests: `./gradlew :plugins:jetbrains:test`
- Build JetBrains plugin: `./gradlew :plugins:jetbrains:buildPlugin`

## Rules

- Do not implement TOML formatting, syntax highlighting, or schema validation.
- Prefer high-confidence navigation over broad guesses.
- Add or update fixtures when changing reference classification behavior.
- Keep language-specific resolution optional so unsupported IDEs degrade cleanly.
- Do not remove the reserved VS Code folder when working on JetBrains tasks.
```

- [ ] **Step 2: Write advanced feature backlog**

Create `TODO.md`:

```markdown
# TomlJump Backlog

## Advanced Navigation

- Add Go struct tag mapping from TOML keys to fields tagged with `toml:"name"`.
- Add Python `pyproject.toml` entry point navigation.
- Add TypeScript `tsconfig.json` path alias resolution.
- Add Java Spring and framework-specific configuration mapping.
- Add user-defined TOML key pattern rules.

## Editor Features

- Add reverse navigation from code symbols back to TOML references.
- Add find usages from code symbols into TOML files.
- Add rename support for high-confidence references.
- Add inline hints for resolvable TOML values after MVP navigation is stable.

## Marketplace

- Prepare JetBrains Marketplace screenshots.
- Prepare JetBrains Marketplace demo GIF.
- Prepare VS Code Marketplace package once the VS Code extension is implemented.
- Prepare Open VSX publishing once the VS Code extension is implemented.
```

- [ ] **Step 3: Reserve VS Code extension folder**

Create `plugins/vscode/README.md`:

```markdown
# TomlJump VS Code Extension

This folder is reserved for the VS Code implementation. The first implementation target is the JetBrains plugin under `plugins/jetbrains`.

The VS Code extension must reuse the behavior expressed by:

- `core/tomljump-core` reference classification tests
- shared projects under `fixtures`
- product decisions in `docs/superpowers/specs`

Do not implement VS Code behavior until the JetBrains MVP has passing tests and a buildable plugin artifact.
```

- [ ] **Step 4: Write JetBrains publishing notes**

Create `docs/publishing/jetbrains.md`:

````markdown
# JetBrains Publishing

## Local Build

Run:

```bash
./gradlew :plugins:jetbrains:buildPlugin
```

The plugin ZIP is produced under `plugins/jetbrains/build/distributions/`.

## Verification

Run:

```bash
./gradlew :plugins:jetbrains:check
```

## Marketplace Release Notes

Before public release, add:

- plugin icon
- screenshots
- plugin description
- changelog
- signing certificate configuration
- publish token configuration

Keep signing and publish tokens out of git.
````

- [ ] **Step 5: Write VS Code publishing notes**

Create `docs/publishing/vscode.md`:

```markdown
# VS Code Publishing

The VS Code extension is not part of the JetBrains MVP.

When implemented, document exact commands for:

- packaging the VSIX
- publishing to Visual Studio Marketplace
- publishing to Open VSX
- running VS Code extension tests

The VS Code extension must use shared fixtures from this repository.
```

- [ ] **Step 6: Commit docs**

```bash
git add AGENT.md TODO.md plugins/vscode/README.md docs/publishing/jetbrains.md docs/publishing/vscode.md
git commit -m "docs: add maintainer and publishing notes"
```

---

### Task 8: Verification Script And Final Checks

**Files:**
- Create: `scripts/verify/jetbrains.sh`

- [ ] **Step 1: Add verification script**

Create `scripts/verify/jetbrains.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

./gradlew :core:tomljump-core:test
./gradlew :plugins:jetbrains:test
./gradlew :plugins:jetbrains:buildPlugin
```

- [ ] **Step 2: Make script executable**

Run:

```bash
chmod +x scripts/verify/jetbrains.sh
```

Expected: `scripts/verify/jetbrains.sh` is executable.

- [ ] **Step 3: Run full verification**

Run:

```bash
./scripts/verify/jetbrains.sh
```

Expected: core tests pass, JetBrains tests pass, and plugin ZIP is created in `plugins/jetbrains/build/distributions/`.

- [ ] **Step 4: Check repository status**

Run:

```bash
git status --short
```

Expected: only `scripts/verify/jetbrains.sh` is uncommitted.

- [ ] **Step 5: Commit verification script**

```bash
git add scripts/verify/jetbrains.sh
git commit -m "chore: add JetBrains verification script"
```

- [ ] **Step 6: Confirm final status**

Run:

```bash
git status --short
```

Expected: no output.

---

## Self-Review

Spec coverage:

- Monorepo with JetBrains and VS Code folders: covered by Task 1 and Task 7.
- JetBrains universal plugin first: covered by Tasks 1, 4, 5, 6, and 8.
- Go, Python, Java, TypeScript, JavaScript language scope: covered by Task 2 classification cases and Task 3 fixtures.
- TOML string reference navigation MVP: covered by Tasks 2, 4, 5, and 6.
- Required `AGENT.md`: covered by Task 7.
- Required advanced backlog file: covered by Task 7.
- Publishing docs: covered by Task 7.
- Verification before completion: covered by Task 8.

Placeholder scan:

- The only occurrence of `TODO` is the required `TODO.md` backlog filename, not an unfinished work marker.
- No implementation step uses undefined file paths.
- No step asks the implementer to fill in missing details.

Type consistency:

- `ReferenceKind`, `TomlReference`, and `TomlReferenceClassifier` are defined before use by JetBrains code.
- `ExtractedTomlString` is defined before use by `TomlJumpReferenceContributor`.
- `TomlJumpResolver`, `FilePathResolver`, and `SymbolShapeResolver` are defined before compilation is expected to pass.
