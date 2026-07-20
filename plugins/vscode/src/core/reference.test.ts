import { describe, expect, test } from "vitest";

import { classifyFilePath } from "./reference";

describe("classifyFilePath", () => {
  test("classifies relative and workspace-relative paths", () => {
    expect(classifyFilePath("./schemas/user.json")).toEqual({
      lookupValue: "schemas/user.json",
      rawValue: "./schemas/user.json",
    });
    expect(classifyFilePath("templates/email.html")).toEqual({
      lookupValue: "templates/email.html",
      rawValue: "templates/email.html",
    });
  });

  test("rejects bare filenames, traversal, empty segments, whitespace, and numeric ratios", () => {
    for (const value of [
      "config.toml",
      "../secret.toml",
      "schemas//user.json",
      "schemas/user profile.json",
      "2024/07/20",
      "",
    ]) {
      expect(classifyFilePath(value), value).toBeUndefined();
    }
  });
});
