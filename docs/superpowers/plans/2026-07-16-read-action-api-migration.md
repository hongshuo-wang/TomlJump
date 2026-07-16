# Read Action API Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** Remove the IntelliJ 2026 deprecated `ReadAction.compute(ThrowableComputable)` usage without changing synchronous source-target resolution behavior or dropping the 2024.3 compile baseline.

**Architecture:** Keep `SourcePatternResolver.resolve` as a synchronous read-action boundary. Use `ApplicationManager.getApplication().runReadAction(Computable)` because that overload exists in both the current 2024.3 compile platform and the installed IntelliJ IDEA 2026.1 platform; leave all resolver logic inside `resolveInReadAction` unchanged.

**Tech Stack:** Kotlin/JVM, IntelliJ Platform Gradle Plugin 2.11.0, IntelliJ Platform 2024.3 compile dependency, IntelliJ IDEA 2026.1.4 verifier/runtime.

---

### Task 1: Establish the failing deprecated-API regression check

**Files:**
- Inspect: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/SourcePatternResolver.kt:3,17`

- [ ] **Step 1: Run the source-level regression assertion before changing code**

Run:

```bash
test "$(rg -n 'ReadAction\\.compute' plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/SourcePatternResolver.kt | wc -l | tr -d ' ')" -eq 0
```

Expected: FAIL with exit code 1 because the current implementation contains the Marketplace-reported deprecated call.

### Task 2: Replace the deprecated read-action call

**Files:**
- Modify: `plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/SourcePatternResolver.kt:3,17-19`

- [ ] **Step 1: Replace the import and wrapper with the non-deprecated cross-version overload**

Change the imports to include:

```kotlin
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
```

Remove the `ReadAction` import and make `resolve` read:

```kotlin
final override fun resolve(project: Project, keyPath: ConfigKeyPath): List<PsiElement> {
    return ApplicationManager.getApplication().runReadAction(
        Computable { resolveInReadAction(project, keyPath) },
    )
}
```

Do not change `resolveInReadAction`, cancellation checks, file limits, target validation, or deduplication.

- [ ] **Step 2: Re-run the regression assertion**

Run the command from Task 1.

Expected: PASS with exit code 0 and no `ReadAction.compute` source reference.

### Task 3: Verify behavior and build compatibility

**Files:**
- Test: existing `plugins/jetbrains/src/test/kotlin/com/tomljump/jetbrains/config/resolver/LanguageConfigResolverTest.kt`

- [ ] **Step 1: Run the focused JetBrains resolver tests with Java 21**

Run:

```bash
JAVA_HOME=/Users/harrison/Applications/IntelliJ\ IDEA.app/Contents/jbr/Contents/Home ./gradlew :plugins:jetbrains:test
```

Expected: exit code 0 and all existing resolver tests pass.

- [ ] **Step 2: Build the plugin against the configured 2024.3 compile platform**

Run:

```bash
JAVA_HOME=/Users/harrison/Applications/IntelliJ\ IDEA.app/Contents/jbr/Contents/Home ./gradlew :plugins:jetbrains:buildPlugin
```

Expected: exit code 0 and a plugin ZIP is produced under `plugins/jetbrains/build/distributions/`.

- [ ] **Step 3: Inspect compiled references for the replacement API**

Run:

```bash
unzip -p plugins/jetbrains/build/distributions/*.zip lib/tomljump-jetbrains-*.jar > /tmp/tomljump-jetbrains.jar
javap -classpath /tmp/tomljump-jetbrains.jar -c -p com.tomljump.jetbrains.config.resolver.SourcePatternResolver
```

Expected: the `resolve` method delegates through `Application.runReadAction` and has no `ReadAction.compute` call.

- [ ] **Step 4: Run the repository's complete JetBrains verification**

Run:

```bash
JAVA_HOME=/Users/harrison/Applications/IntelliJ\ IDEA.app/Contents/jbr/Contents/Home ./scripts/verify/jetbrains.sh
```

Expected: core tests, JetBrains tests, and plugin build all exit successfully.

- [ ] **Step 5: Commit the implementation**

```bash
git add plugins/jetbrains/src/main/kotlin/com/tomljump/jetbrains/config/resolver/SourcePatternResolver.kt
git commit -m "fix: replace deprecated read action API"
```
