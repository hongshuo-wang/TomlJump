import { spawnSync } from "node:child_process";
import path from "node:path";

import { describe, expect, test } from "vitest";

const extensionRoot = path.resolve(__dirname, "../..");
const repositoryRoot = path.resolve(extensionRoot, "../..");
const validator = path.join(extensionRoot, "scripts/validate-release.mjs");

describe("release target validation", () => {
  test("accepts the configured JetBrains version", () => {
    const result = validate("jetbrains", "jetbrains-v1.4.0");

    expect(result.status).toBe(0);
    expect(result.stdout).toContain('"version":"1.4.0"');
  });

  test("accepts the configured VS Code version", () => {
    const result = validate("vscode", "vscode-v1.4.0");

    expect(result.status).toBe(0);
    expect(result.stdout).toContain('"version":"1.4.0"');
  });

  test("rejects cross-target tags before building", () => {
    const result = validate("jetbrains", "vscode-v1.4.0");

    expect(result.status).toBe(1);
    expect(result.stderr).toContain("does not match release target jetbrains");
  });

  test("rejects version mismatches and non-SemVer tags", () => {
    expect(validate("vscode", "vscode-v9.9.9").stderr).toContain(
      "does not match configured vscode version 1.4.0",
    );
    expect(validate("vscode", "vscode-v01.0.0").stderr).toContain("must use Semantic Versioning 2.0.0");
  });

  test("rejects unsupported release targets", () => {
    expect(validate("all", "vscode-v1.4.0").stderr).toContain("Unsupported release target all");
  });
});

function validate(target: string, tag: string): ReturnType<typeof spawnSync> {
  return spawnSync(process.execPath, [validator, target, tag, repositoryRoot], {
    encoding: "utf8",
  });
}
