import { spawnSync } from "node:child_process";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";

import { afterEach, describe, expect, test } from "vitest";

const extensionRoot = path.resolve(__dirname, "../..");
const repositoryRoot = path.resolve(extensionRoot, "../..");
const validator = path.join(extensionRoot, "scripts/validate-release.mjs");
const temporaryRoots: string[] = [];

afterEach(() => {
  for (const root of temporaryRoots.splice(0)) {
    rmSync(root, { recursive: true, force: true });
  }
});

describe("release target validation", () => {
  test.each(["jetbrains", "vscode", "all"])("accepts shared tags for %s releases", (target) => {
    const result = validate(target, "v1.4.0");

    expect(result.status).toBe(0);
    expect(result.stdout).toContain(`"target":"${target}"`);
    expect(result.stdout).toContain('"version":"1.4.0"');
  });

  test.each(["jetbrains-v1.4.0", "vscode-v1.4.0"])("rejects platform tag %s", (tag) => {
    expect(validate("all", tag).stderr).toContain("must use shared tag v<version>");
  });

  test("rejects version mismatches and non-SemVer tags", () => {
    expect(validate("vscode", "v9.9.9").stderr).toContain(
      "does not match configured versions: jetbrains=1.4.0, vscode=1.4.0",
    );
    expect(validate("vscode", "v01.0.0").stderr).toContain("must use Semantic Versioning 2.0.0");
  });

  test("rejects unsupported release targets", () => {
    expect(validate("desktop", "v1.4.0").stderr).toContain("Unsupported release target desktop");
  });

  test("rejects a tag when either configured platform version diverges", () => {
    const root = fixtureRepository("1.4.0", "1.4.1");

    expect(validate("vscode", "v1.4.0", root).stderr).toContain(
      "does not match configured versions: jetbrains=1.4.0, vscode=1.4.1",
    );
  });
});

function validate(target: string, tag: string, root = repositoryRoot): ReturnType<typeof spawnSync> {
  return spawnSync(process.execPath, [validator, target, tag, root], {
    encoding: "utf8",
  });
}

function fixtureRepository(jetbrainsVersion: string, vscodeVersion: string): string {
  const root = mkdtempSync(path.join(os.tmpdir(), "tomljump-release-validator-"));
  temporaryRoots.push(root);
  mkdirSync(path.join(root, "plugins/vscode"), { recursive: true });
  writeFileSync(path.join(root, "gradle.properties"), `pluginVersion=${jetbrainsVersion}\n`);
  writeFileSync(
    path.join(root, "plugins/vscode/package.json"),
    `${JSON.stringify({ version: vscodeVersion })}\n`,
  );
  return root;
}
