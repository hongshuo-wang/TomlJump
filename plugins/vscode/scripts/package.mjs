import { mkdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { createVSIX } from "@vscode/vsce";

const extensionRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const manifest = JSON.parse(await readFile(path.join(extensionRoot, "package.json"), "utf8"));
const buildDirectory = path.join(extensionRoot, "build");
await mkdir(buildDirectory, { recursive: true });
await createVSIX({
  baseImagesUrl: "https://github.com/hongshuo-wang/TomlJump/raw/main/plugins/vscode",
  cwd: extensionRoot,
  packagePath: path.join(buildDirectory, `tomljump-vscode-${manifest.version}.vsix`),
});
