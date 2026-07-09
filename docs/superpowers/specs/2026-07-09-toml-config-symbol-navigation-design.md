# TOML Config Symbol Navigation Design

Date: 2026-07-09

## Summary

TomlJump will add JetBrains IDE navigation from TOML table names and keys to configuration declarations in project source code. The first implementation will use IntelliJ Platform PSI and language-aware resolvers, with Go as the deepest supported language and Python, Java, TypeScript, and JavaScript as conservative MVP adapters.

This feature extends the current MVP. Existing TOML string file-path navigation remains unchanged.

## Goals

- Navigate from TOML table names such as `[openai]` to matching code declarations.
- Navigate from TOML keys such as `api_key` under `[openai]` to matching code fields or properties.
- Use JetBrains PSI and language indexes where available instead of only raw text scanning.
- Keep every language resolver optional so missing language plugins do not break plugin startup.
- Prefer high-confidence targets and fail silently when no credible target exists.

## Non-Goals

- TOML schema validation, formatting, completion, or highlighting.
- Full framework inference for every config library.
- Runtime JavaScript analysis.
- Deep `@ConfigurationProperties` object graph traversal in Java.
- Cross-editor VS Code implementation in this phase.
- Reverse navigation, rename, or find usages.

## Example

Given this TOML:

```toml
[openai]
api_key = "..."
model = "deepseek-v4-flash"
base_url = "https://api.deepseek.com"
```

Go navigation should be able to resolve to declarations like:

```go
type Config struct {
    OpenAI OpenAIConfig `toml:"openai"`
}

type OpenAIConfig struct {
    APIKey  string `toml:"api_key"`
    Model   string `toml:"model"`
    BaseURL string `toml:"base_url"`
}
```

The same TOML key path should also resolve to credible fields or properties in Python, Java, TypeScript, and JavaScript when the relevant language support is installed and the source declaration is statically discoverable.

## User-Facing Behavior

Users can invoke normal JetBrains navigation actions on TOML table names and keys:

- `Go to Declaration`
- Command/control click where the IDE exposes references
- Multi-target chooser when several credible declarations match

Unresolved TOML keys do nothing and should not display warnings. TOML files often contain arbitrary values, so noisy diagnostics would be incorrect.

## Architecture

The JetBrains plugin will have two independent reference paths:

- Existing path: TOML string literal values to file paths.
- New path: TOML key and table PSI elements to source code config declarations.

New components:

- `TomlConfigReferenceContributor`: registers references for TOML table names and keys.
- `TomlConfigReference`: JetBrains `PsiReferenceBase.Poly` implementation for config symbols.
- `TomlConfigKeyPathExtractor`: extracts normalized TOML key paths from PSI context.
- `TomlConfigTargetResolver`: coordinates language-specific resolvers.
- `LanguageConfigResolver`: small interface implemented by each language resolver.
- `GoConfigResolver`: resolves Go struct fields and tags.
- `PythonConfigResolver`: resolves Python class, dataclass, and Pydantic-style fields.
- `JavaConfigResolver`: resolves Java fields and simple annotation-backed aliases.
- `TypeScriptConfigResolver`: resolves TypeScript interface, type literal, and class properties.
- `JavaScriptConfigResolver`: resolves JavaScript object literal and class properties.

The core module will own editor-neutral model and matching utilities:

- `ConfigKeyPath`: immutable TOML key path model.
- `ConfigNameNormalizer`: matching helpers for snake case, kebab case, camel case, Pascal case, and common acronym forms.
- Tests for normalization and path construction behavior that do not depend on JetBrains APIs.

## Key Path Extraction

The extractor will produce stable key paths:

- `[openai]` produces `["openai"]`.
- `api_key = "..."` inside `[openai]` produces `["openai", "api_key"]`.
- `[servers.production]` produces `["servers", "production"]`.
- `host = "..."` inside `[servers.production]` produces `["servers", "production", "host"]`.

The first implementation will focus on standard TOML tables and scalar keys. Arrays of tables and dotted inline keys can be added later if PSI support and tests are clear.

## Matching Rules

Resolvers receive a `ConfigKeyPath` and may return zero or more PSI targets.

Matching is high-confidence first:

1. Explicit config aliases:
   - Go tags such as `toml:"api_key"`, `json:"api_key"`, `mapstructure:"api_key"`.
   - Java annotations such as `@JsonProperty("api_key")`.
   - TypeScript or JavaScript quoted properties such as `"api_key": string`.
2. Direct normalized name matches:
   - `api_key`, `api-key`, `apiKey`, `APIKey`.
   - `base_url`, `base-url`, `baseUrl`, `BaseURL`.
3. Multi-target result:
   - If several credible targets exist, return all and let JetBrains show the native chooser.
4. No target:
   - Return no references or unresolved references silently.

Resolvers should avoid broad guesses. For example, a random string occurrence in a comment is not a target.

## Language Resolver Scope

### Go

Go gets the deepest first implementation:

- Match struct tags for `toml`, `json`, `mapstructure`, and `yaml`.
- Match exported field names after normalization.
- Prefer tag matches over field-name matches.
- For nested TOML paths, match each segment against fields where practical.
- If full type traversal is unavailable from installed Go PSI APIs, resolve the final segment to high-confidence matching struct fields and return multiple targets.

### Python

Python MVP:

- Match class attributes, annotated fields, dataclass fields, and common Pydantic-style fields.
- Match `api_key: str`, `api_key = ...`, and normalized field variants.
- Alias support is not required in the first pass unless the PSI form is straightforward.

### Java

Java MVP:

- Match fields by normalized name, such as `api_key` to `apiKey`.
- Match simple alias annotations, especially `@JsonProperty("api_key")`.
- Do not implement Spring `@ConfigurationProperties` graph inference in this phase.

### TypeScript

TypeScript MVP:

- Match interface properties, type literal properties, and class properties.
- Match quoted string properties such as `"api_key": string`.
- Match normalized identifiers such as `apiKey`.

### JavaScript

JavaScript MVP:

- Match object literal properties and class fields when statically visible in PSI.
- Match quoted properties and normalized identifiers.
- Do not infer dynamic runtime object construction.

## Optional Language Dependencies

Language resolvers must not introduce hard plugin dependencies that break TomlJump in IDEs without a language plugin. Each resolver must either:

- Use optional extension registration and dependency declarations supported by IntelliJ, or
- Use defensive reflection/adapters so missing PSI classes are skipped.

The selected implementation should favor maintainability and reliable plugin startup over resolver cleverness.

## Error Handling

- Missing language plugin: skip that resolver.
- Unsupported PSI shape: return no targets.
- Multiple targets: return all credible targets.
- Unexpected resolver exception: catch, log through IntelliJ logging, and continue with other resolvers.
- Unresolved references: no UI warning.

## Testing

Core tests:

- Build `ConfigKeyPath` values.
- Normalize common config names:
  - `api_key`
  - `api-key`
  - `apiKey`
  - `APIKey`
  - `base_url`
  - `BaseURL`

JetBrains tests:

- TOML table reference registration.
- TOML key reference registration inside a table.
- Existing string file-path navigation still works.

Language resolver tests:

- Go struct tag fixture.
- Python class or dataclass field fixture.
- Java field and `@JsonProperty` fixture where dependencies allow.
- TypeScript interface property fixture.
- JavaScript object property fixture.

If a language test cannot run in the default IntelliJ test fixture without adding an optional language dependency, the implementation should include a focused resolver unit test with fake PSI only when the abstraction supports it. The final verification script must still pass in the default project environment.

## Verification

The existing command remains the required full verification path:

```bash
./scripts/verify/jetbrains.sh
```

The implementation may extend this script only if new deterministic tasks are required. It must continue to run core tests, JetBrains tests, and plugin packaging.
