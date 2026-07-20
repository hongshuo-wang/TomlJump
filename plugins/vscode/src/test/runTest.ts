import fs from "node:fs";
import path from "node:path";

import { runTests } from "@vscode/test-electron";

async function main(): Promise<void> {
  const extensionDevelopmentPath = path.resolve(__dirname, "../..");
  const extensionTestsPath = path.resolve(__dirname, "suite/index");
  const workspacePath = path.resolve(extensionDevelopmentPath, "../../examples/navigation-demo");
  if (!fs.statSync(extensionDevelopmentPath).isDirectory() || !fs.statSync(workspacePath).isDirectory()) {
    throw new Error("Extension or navigation demo path is not a directory");
  }

  await runTests({
    extensionDevelopmentPath,
    extensionTestsPath,
    launchArgs: [
      "--disable-extensions",
      "--user-data-dir=/private/tmp/tomljump-vscode-test-user-data",
      workspacePath,
    ],
    version: "1.85.2",
  });
}

void main().catch((error: unknown) => {
  console.error(error);
  process.exitCode = 1;
});
