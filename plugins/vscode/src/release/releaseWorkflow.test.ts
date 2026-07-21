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
