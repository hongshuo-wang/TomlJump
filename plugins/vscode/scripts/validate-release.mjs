import { readFile } from "node:fs/promises";
import path from "node:path";

const [target, tag, repositoryRoot = path.resolve(import.meta.dirname, "../../..")] = process.argv.slice(2);
const supportedTargets = new Set(["jetbrains", "vscode", "all"]);
const semverPattern =
  "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*)(?:\\.(?:0|[1-9]\\d*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*))*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?";

try {
  if (!supportedTargets.has(target)) {
    throw new Error(`Unsupported release target ${target ?? "<missing>"}`);
  }
  if (!tag?.startsWith("v") || /^(?:jetbrains|vscode)-v/.test(tag)) {
    throw new Error(`Tag ${tag ?? "<missing>"} must use shared tag v<version>`);
  }
  const version = tag.slice(1);
  if (!new RegExp(`^${semverPattern}$`).test(version)) {
    throw new Error(`Tag ${tag} must use Semantic Versioning 2.0.0`);
  }
  const configuredVersions = await readConfiguredVersions(repositoryRoot);
  if (version !== configuredVersions.jetbrains || version !== configuredVersions.vscode) {
    throw new Error(
      `Tag version ${version} does not match configured versions: ` +
        `jetbrains=${configuredVersions.jetbrains}, vscode=${configuredVersions.vscode}`,
    );
  }
  process.stdout.write(`${JSON.stringify({ tag, target, version })}\n`);
} catch (error) {
  process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
  process.exitCode = 1;
}

async function readConfiguredVersions(root) {
  const [manifestText, properties] = await Promise.all([
    readFile(path.join(root, "plugins/vscode/package.json"), "utf8"),
    readFile(path.join(root, "gradle.properties"), "utf8"),
  ]);
  const manifest = JSON.parse(manifestText);
  const match = properties.match(/^pluginVersion=(.+)$/m);
  if (match?.[1] === undefined) {
    throw new Error("Could not read pluginVersion from gradle.properties");
  }
  return {
    jetbrains: match[1].trim(),
    vscode: manifest.version,
  };
}
