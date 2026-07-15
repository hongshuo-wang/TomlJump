# Read Action API Migration Design

## Goal

Remove TomlJump's use of the deprecated `ReadAction.compute(ThrowableComputable)` API reported by the JetBrains Marketplace verifier for IntelliJ IDEA 2026.1 and 2026.2.

## Compatibility

- Prioritize a warning-free IntelliJ 2026 implementation.
- Preserve the current IntelliJ 2024.3 minimum build when the replacement API is available there.
- Keep source resolution synchronous and protected by a read action.

## Design

Change only `SourcePatternResolver.resolve`:

- Replace `ReadAction.compute` with `ApplicationManager.getApplication().runReadAction`.
- Pass an explicit `Computable<List<PsiElement>>` so Kotlin selects the non-throwing overload, which is present and not deprecated in both IntelliJ 2024.3 and 2026.1.
- Leave `resolveInReadAction` and all target matching, cancellation, indexing, and deduplication behavior unchanged.

Using `ReadAction.computeBlocking` was rejected because it is unavailable in IntelliJ 2024.3 and would force an unnecessary minimum-version increase. Converting resolution to `ReadAction.nonBlocking` was rejected because it would change the synchronous resolver contract and expand the scope of this maintenance fix.

## Verification

1. Establish a failing regression check that detects the deprecated method reference in the built plugin or source.
2. Apply the single API migration and confirm that check passes.
3. Run JetBrains plugin tests and build the plugin against the existing 2024.3 compile baseline.
4. Inspect the resulting bytecode to confirm it references `Application.runReadAction(Computable)` rather than `ReadAction.compute(ThrowableComputable)`.
5. Run the repository's full JetBrains verification script.

## Non-Goals

- No asynchronous resolver redesign.
- No change to `pluginSinceBuild` or plugin version.
- No unrelated source resolver refactoring.
- No changes for optional `intellij.platform.frontend.split` dependency messages, which are verifier dependency-tree diagnostics rather than TomlJump API incompatibilities.
