import { readFile } from "node:fs/promises";
import path from "node:path";

const [target, tag, repositoryRoot = path.resolve(import.meta.dirname, "../../..")] = process.argv.slice(2);
const supportedTargets = new Set(["jetbrains", "vscode"]);
const semverPattern =
  "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*)(?:\\.(?:0|[1-9]\\d*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*))*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?";

try {
  if (!supportedTargets.has(target)) {
    throw new Error(`Unsupported release target ${target ?? "<missing>"}`);
  }
  const prefix = `${target}-v`;
  if (!tag?.startsWith(prefix)) {
    throw new Error(`Tag ${tag ?? "<missing>"} does not match release target ${target}`);
  }
  const version = tag.slice(prefix.length);
  if (!new RegExp(`^${semverPattern}$`).test(version)) {
    throw new Error(`Tag ${tag} must use Semantic Versioning 2.0.0`);
  }
  const configuredVersion = await readConfiguredVersion(target, repositoryRoot);
  if (version !== configuredVersion) {
    throw new Error(
      `Tag version ${version} does not match configured ${target} version ${configuredVersion}`,
    );
  }
  process.stdout.write(`${JSON.stringify({ tag, target, version })}\n`);
} catch (error) {
  process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
  process.exitCode = 1;
}

async function readConfiguredVersion(releaseTarget, root) {
  if (releaseTarget === "vscode") {
    const manifest = JSON.parse(
      await readFile(path.join(root, "plugins/vscode/package.json"), "utf8"),
    );
    return manifest.version;
  }
  const properties = await readFile(path.join(root, "gradle.properties"), "utf8");
  const match = properties.match(/^pluginVersion=(.+)$/m);
  if (match?.[1] === undefined) {
    throw new Error("Could not read pluginVersion from gradle.properties");
  }
  return match[1].trim();
}
