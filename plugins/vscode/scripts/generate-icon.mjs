import { mkdtemp, readFile, rename, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const extensionRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const repositoryRoot = path.resolve(extensionRoot, "../..");
const lightIcon = path.join(
  repositoryRoot,
  "plugins/jetbrains/src/main/resources/META-INF/pluginIcon.svg",
);
const darkIcon = path.join(
  repositoryRoot,
  "plugins/jetbrains/src/main/resources/META-INF/pluginIcon_dark.svg",
);
const targetIcon = path.join(extensionRoot, "icon.png");

const [lightSvg, darkSvg] = await Promise.all([readFile(lightIcon), readFile(darkIcon)]);
if (!lightSvg.equals(darkSvg)) {
  throw new Error("JetBrains light and dark icons differ; select the canonical VS Code source explicitly");
}

const temporaryDirectory = await mkdtemp(path.join(tmpdir(), "tomljump-icon-"));
const temporaryIcon = path.join(temporaryDirectory, "icon.png");
try {
  const result = spawnSync(
    "magick",
    [
      "-background",
      "none",
      "-density",
      "1536",
      lightIcon,
      "-resize",
      "256x256",
      "-depth",
      "8",
      "-strip",
      `PNG32:${temporaryIcon}`,
    ],
    { encoding: "utf8" },
  );
  if (result.error !== undefined) {
    throw new Error(`ImageMagick is required to generate the extension icon: ${result.error.message}`);
  }
  if (result.status !== 0) {
    throw new Error(result.stderr.trim() || "ImageMagick failed to generate the extension icon");
  }
  await rename(temporaryIcon, targetIcon);
} finally {
  await rm(temporaryDirectory, { force: true, recursive: true });
}

process.stdout.write(`Generated ${path.relative(repositoryRoot, targetIcon)} from ${path.relative(repositoryRoot, lightIcon)}\n`);
