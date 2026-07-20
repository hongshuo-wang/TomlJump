import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const extensionRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const repositoryRoot = path.resolve(extensionRoot, "../..");
const [manifestText, readme, changelog, packageScript, jetbrainsBuild, jetbrainsMetadata] = await Promise.all([
  readFile(path.join(extensionRoot, "package.json"), "utf8"),
  readFile(path.join(extensionRoot, "README.md"), "utf8"),
  readFile(path.join(extensionRoot, "CHANGELOG.md"), "utf8"),
  readFile(path.join(extensionRoot, "scripts/package.mjs"), "utf8"),
  readFile(path.join(repositoryRoot, "plugins/jetbrains/build.gradle.kts"), "utf8"),
  readFile(
    path.join(repositoryRoot, "plugins/jetbrains/src/main/resources/META-INF/plugin.xml"),
    "utf8",
  ),
]);
const manifest = JSON.parse(manifestText);

requireAsciiAndChinese(manifest.description, "VS Code extension description");
if (manifest.repository?.directory !== "plugins/vscode") {
  throw new Error("VS Code repository metadata must declare the plugins/vscode monorepo directory");
}
const languageAliases = manifest.contributes?.languages?.flatMap((language) => language.aliases ?? []) ?? [];
if (!languageAliases.some((alias) => containsAscii(alias) && containsChinese(alias))) {
  throw new Error("VS Code language contribution must include a bilingual feature label");
}

for (const [name, content] of [
  ["VS Code README", readme],
  ["VS Code changelog", changelog],
]) {
  if (/^#{1,6}\s+(?:English|中文)\s*$/mu.test(content)) {
    throw new Error(`${name} must not display standalone language labels`);
  }
}
requireText(readme, "## Features", "VS Code README feature section");
requireText(readme, "## 功能", "VS Code README feature section");
requireText(
  readme,
  '<img src="https://plugins.jetbrains.com/files/32933/1111804/icon/default.png"',
  "VS Code README shared JetBrains Marketplace PNG logo",
);
if (/<img\s+[^>]*src=["'][^"']+\.svg(?:[?#][^"']*)?["']/iu.test(readme)) {
  throw new Error("VS Code README must not embed SVG images because VSCE rejects them");
}
requireText(
  packageScript,
  "https://github.com/hongshuo-wang/TomlJump/raw/main/plugins/vscode",
  "VS Code monorepo image base URL",
);

const sharedCapabilities = [
  {
    english: "TOML string file paths",
    chinese: "TOML 字符串文件路径",
  },
  {
    english: "Navigate both ways between TOML tables and keys",
    chinese: "在 TOML table、key 与匹配的源码配置声明之间双向跳转",
  },
  {
    english: "pyproject.toml project scripts",
    chinese: "pyproject.toml 的 project scripts",
  },
  {
    english: "Go, Python, Java, TypeScript, and JavaScript",
    chinese: "Go、Python、Java、TypeScript、JavaScript",
  },
  {
    english: "stays quiet",
    chinese: "静默跳过",
  },
];

for (const capability of sharedCapabilities) {
  for (const [sourceName, source] of [
    ["JetBrains Gradle metadata", jetbrainsBuild],
    ["JetBrains plugin.xml", jetbrainsMetadata],
  ]) {
    requireVisibleText(source, capability.english, `${sourceName} English capability: ${capability.english}`);
    requireVisibleText(source, capability.chinese, `${sourceName} Chinese capability: ${capability.chinese}`);
  }
  requireVisibleText(readme, capability.english, `VS Code English capability: ${capability.english}`);
  requireVisibleText(readme, capability.chinese, `VS Code Chinese capability: ${capability.chinese}`);
}

for (const source of [jetbrainsBuild, jetbrainsMetadata]) {
  requireVisibleText(
    source,
    "新增从 pyproject.toml 的 project scripts 跳转到 Python 模块和顶层 callable",
    "JetBrains Chinese project-scripts change note",
  );
  requireVisibleText(
    source,
    "Added navigation from pyproject.toml project scripts to Python modules and top-level callables",
    "JetBrains English project-scripts change note",
  );
}

process.stdout.write("Validated bilingual JetBrains and VS Code Marketplace copy\n");

function requireAsciiAndChinese(value, label) {
  if (typeof value !== "string" || !containsAscii(value) || !containsChinese(value)) {
    throw new Error(`${label} must contain both English and Chinese text`);
  }
}

function containsAscii(value) {
  return /[A-Za-z]/.test(value);
}

function containsChinese(value) {
  return /[\u3400-\u9fff]/u.test(value);
}

function requireText(content, expected, label) {
  if (!content.includes(expected)) {
    throw new Error(`${label} is missing`);
  }
}

function requireVisibleText(content, expected, label) {
  requireText(content.replaceAll("`", ""), expected, label);
}
