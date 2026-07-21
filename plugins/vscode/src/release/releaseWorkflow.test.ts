import { readFileSync } from "node:fs";
import path from "node:path";

import { describe, expect, test } from "vitest";

const repositoryRoot = path.resolve(__dirname, "../../../..");
const workflow = readFileSync(path.join(repositoryRoot, ".github/workflows/release.yml"), "utf8");

describe("release workflow retries", () => {
  test("skips versions already published to Visual Studio Marketplace", () => {
    expect(workflow).toContain(
      'npx vsce publish\n          --skip-duplicate\n          --packagePath',
    );
  });

  test("skips versions already published to Open VSX", () => {
    expect(workflow).toContain(
      'npx ovsx publish\n          --skip-duplicate\n          "../../verified-extension',
    );
  });
});

describe("shared release workflow", () => {
  test("offers all platforms under one shared tag", () => {
    expect(workflow).toContain("          - all");
    expect(workflow).toContain("description: Existing shared v<version> release tag");
    expect(workflow).toContain("group: release-${{ inputs.tag }}");
  });

  test("gates platform jobs and release assets for its own target or all", () => {
    expect(occurrences("if: inputs.target != 'vscode'")).toBe(5);
    expect(occurrences("if: inputs.target != 'jetbrains'")).toBe(4);
  });

  test("creates exactly one shared GitHub release after selected publications", () => {
    expect(workflow).not.toContain("Create JetBrains GitHub release");
    expect(workflow).not.toContain("Create VS Code GitHub release");
    expect(occurrences("name: Create shared GitHub release")).toBe(1);
    expect(workflow).toContain("github-release:");
    expect(workflow).toContain("if: always()");
  });
});

function occurrences(expected: string): number {
  return workflow.split(expected).length - 1;
}
