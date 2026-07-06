# TomlJump Plugin Design

Date: 2026-07-06

## Summary

TomlJump is a monorepo-maintained IDE plugin project for jumping from TOML configuration values to project files and code symbols. The first implementation target is a universal JetBrains plugin. A VS Code extension will be added later in the same repository, using the same product model and shared fixtures.

TomlJump must not reimplement general TOML language support such as syntax highlighting, formatting, validation, or schema completion. Existing plugins already cover that space well. TomlJump focuses on cross-file and cross-language navigation from TOML string references.

## Product Scope

The first version focuses on TOML string value navigation:

- File path references, such as `schema = "./schemas/user.json"` or `template = "templates/email.html"`.
- Module and package references used by Go, Python, Java, TypeScript, and JavaScript projects.
- Function or class style references where a string points to a callable or type, such as `handler = "app.user:create_user"` or `factory = "com.example.PaymentClient"`.

The first version should avoid language-specific deep magic unless it naturally falls out of the platform APIs. Advanced features are tracked separately so the MVP stays useful and maintainable.

## Supported IDE Strategy

TomlJump will ship as one JetBrains plugin, not separate plugins for GoLand, PyCharm, WebStorm, and IntelliJ IDEA.

The plugin should be compatible with JetBrains IDEs that provide the needed platform and language features:

- GoLand or IntelliJ IDEA with Go support.
- PyCharm or IntelliJ IDEA with Python support.
- WebStorm or IntelliJ IDEA with JavaScript/TypeScript support.
- IntelliJ IDEA with Java support.

Language integrations must be optional. If a language plugin or IDE feature is unavailable, TomlJump should disable that adapter cleanly instead of failing plugin startup.

## Repository Layout

The repository should be structured for long-term maintenance of both JetBrains and VS Code implementations:

```text
TomlJump/
  README.md
  LICENSE
  AGENT.md
  TODO.md
  docs/
    product/
    publishing/
    specs/
  core/
    tomljump-core/
      src/
      tests/
      fixtures/
  plugins/
    jetbrains/
      build.gradle.kts
      src/main/kotlin/
      src/test/kotlin/
      resources/
    vscode/
      package.json
      src/
      test/
  fixtures/
    go/
    python/
    java/
    typescript/
    javascript/
    mixed/
  scripts/
    verify/
    release/
```

`AGENT.md` is a required implementation deliverable. It should explain repository conventions for future coding agents and maintainers, including build commands, test commands, project boundaries, and rules for updating fixtures.

`TODO.md` is a required implementation deliverable. It should track advanced features and maintenance tasks that are intentionally out of the MVP.

## Architecture

### Core

The core package owns editor-agnostic logic:

- Detecting TOML string references.
- Classifying references as file path, module path, package path, class reference, or function reference.
- Normalizing relative paths against workspace/project roots.
- Producing candidate lookup requests for IDE-specific adapters.
- Keeping shared fixtures for behavior tests.

The core should not depend on JetBrains or VS Code APIs. If sharing compiled code across Kotlin and TypeScript becomes expensive, the first version may share behavior through fixtures and documented algorithms rather than a shared binary library.

### JetBrains Plugin

The JetBrains plugin owns IntelliJ Platform integration:

- Register references for TOML PSI string values.
- Resolve file path references through IntelliJ virtual file/project APIs.
- Resolve language symbols through optional language adapters.
- Provide Go to Declaration / Go to Definition behavior.
- Provide minimal settings only where necessary.
- Show no intrusive UI during normal editing.

Implementation should use standard IntelliJ Platform mechanisms such as PSI references and reference contributors. TOML syntax support itself should rely on the existing JetBrains TOML plugin/platform support.

### VS Code Extension

The VS Code extension is a later implementation target in the same repository:

- Register a DefinitionProvider for TOML documents.
- Reuse the same fixture corpus and reference classification behavior.
- Integrate with VS Code workspace files and language symbol providers where possible.

The VS Code project folder should exist early or be reserved in the repository layout, but the first build target is JetBrains.

## Data Flow

1. User invokes navigation on a TOML string value.
2. The IDE plugin extracts the current TOML string and file context.
3. Core classification identifies possible reference types.
4. The plugin tries resolvers in a stable order:
   - Existing file path.
   - Language-specific module/package path.
   - Language-specific class/function symbol.
   - User-configured custom rule, once settings exist.
5. If exactly one target is found, the IDE opens it.
6. If multiple targets are found, the IDE shows the platform-native target chooser.
7. If no target is found, navigation falls back to normal IDE behavior without noisy errors.

## Language Adapter Priorities

The MVP should support these language families:

- Go
- Python
- Java
- TypeScript
- JavaScript

The initial adapters should prefer high-confidence navigation. For example, path-like strings should resolve only to existing files, and symbol-like strings should resolve only when the IDE can provide a credible declaration target.

## Error Handling

TomlJump should fail quietly for unresolved references. It should not display warnings for ordinary unresolved strings because TOML files often contain arbitrary values.

Plugin startup should tolerate missing optional language dependencies. Adapter registration should be conditional or defensive.

Unexpected resolver errors should be logged through the IDE logging system with enough context for debugging, but they should not interrupt editor use.

## Testing

Testing should be fixture-driven:

- Shared TOML fixture files for common reference formats.
- Per-language sample projects under `fixtures/`.
- JetBrains plugin tests for PSI reference registration and target resolution.
- Later VS Code tests for DefinitionProvider behavior.

Important test cases:

- Relative file path resolution.
- Absolute or workspace-relative path resolution.
- Missing file with no noisy diagnostic.
- Python module and callable strings.
- Java package/class strings.
- Go package or function-like strings.
- TypeScript/JavaScript path and module strings.
- Multiple target candidates.
- Missing optional language plugin behavior.

## Publishing And Maintenance Docs

The repository should include publishing notes before public release:

- `docs/publishing/jetbrains.md` for JetBrains Marketplace build, signing, upload, and release steps.
- `docs/publishing/vscode.md` for VS Code Marketplace and Open VSX release steps when the VS Code extension is added.

These docs should record the exact commands used by the project instead of generic marketplace theory.

## Advanced Feature Backlog

The following items belong in `TODO.md` and should not block the MVP:

- Go struct tag mapping, such as TOML keys jumping to `toml:"key"` fields.
- Python `pyproject.toml` entry point navigation.
- TypeScript `tsconfig.json` path alias support.
- Java Spring or framework-specific TOML/config mapping.
- Custom user-defined TOML key patterns and resolver rules.
- Code lens or inline hints for resolvable TOML values.
- Reverse navigation from code symbols back to TOML references.
- Rename support where a code symbol rename updates TOML references.
- Find usages from code symbols into TOML files.
- Marketplace screenshots, demo GIFs, and release checklist.

## Non-Goals

TomlJump will not initially provide:

- TOML syntax highlighting.
- TOML formatting.
- TOML schema validation.
- General TOML autocomplete.
- A standalone language server.
- Separate JetBrains plugins per IDE.

## Implementation Direction

The first implementation should use Kotlin-first core logic consumed directly by the JetBrains plugin. Shared fixtures and documented reference classification behavior will act as the cross-editor contract for the later VS Code extension. This keeps the first target simple while still preventing the VS Code implementation from drifting into different behavior.
