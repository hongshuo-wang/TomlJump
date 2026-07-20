import { describe, expect, test } from "vitest";

import { parseTomlDocument, targetAtOffset } from "./toml";

describe("parseTomlDocument", () => {
  test("extracts standard table and owned key ranges", () => {
    const source = "[openai]\nmodel = \"gpt-4\"\n";

    const parsed = parseTomlDocument(source);

    expect(parsed.navigationTargets).toEqual([
      { kind: "table", path: ["openai"], range: { start: 1, end: 7 } },
      { kind: "key", path: ["openai", "model"], range: { start: 9, end: 14 } },
    ]);
    expect(parsed.stringTargets).toEqual([
      {
        keyPath: ["openai", "model"],
        range: { start: 18, end: 23 },
        value: "gpt-4",
      },
    ]);
  });

  test("extracts quoted nested paths and only the leaf segment", () => {
    const source = "[app.\"database.prod\"]\n\"base-url\" = 'https://example.test'\n";

    const parsed = parseTomlDocument(source);

    expect(parsed.navigationTargets).toEqual([
      {
        kind: "table",
        path: ["app", "database.prod"],
        range: { start: 6, end: 19 },
      },
      {
        kind: "key",
        path: ["app", "database.prod", "base-url"],
        range: { start: 23, end: 31 },
      },
    ]);
  });

  test("extracts root dotted key and array table without parser indexes", () => {
    const source = "app.database.host = \"localhost\"\n[[products]]\nname = \"Hammer\"\n";

    const parsed = parseTomlDocument(source);

    expect(parsed.navigationTargets).toEqual([
      { kind: "key", path: ["app", "database", "host"], range: { start: 13, end: 17 } },
      { kind: "table", path: ["products"], range: { start: 34, end: 42 } },
      { kind: "key", path: ["products", "name"], range: { start: 45, end: 49 } },
    ]);
  });

  test("keeps a project script string available with its semantic key path", () => {
    const source = "[project.scripts]\nserve = \"package.cli:main\"\n";

    const parsed = parseTomlDocument(source);

    expect(parsed.stringTargets).toEqual([
      {
        keyPath: ["project", "scripts", "serve"],
        range: { start: 27, end: 43 },
        value: "package.cli:main",
      },
    ]);
  });

  test("ignores inline-table keys and multiline strings", () => {
    const source = "config = { host = \"localhost\" }\nmessage = \"\"\"hello\nworld\"\"\"\n";

    const parsed = parseTomlDocument(source);

    expect(parsed.navigationTargets).toEqual([
      { kind: "key", path: ["config"], range: { start: 0, end: 6 } },
      { kind: "key", path: ["message"], range: { start: 32, end: 39 } },
    ]);
    expect(parsed.stringTargets).toEqual([]);
  });

  test("returns no targets for malformed TOML", () => {
    expect(parseTomlDocument("[openai\nmodel = \"gpt\"\n")).toEqual({
      navigationTargets: [],
      stringTargets: [],
    });
  });
});

describe("targetAtOffset", () => {
  test("finds only leaf keys and string contents", () => {
    const source = "app.database.host = \"localhost\"\n";
    const parsed = parseTomlDocument(source);

    expect(targetAtOffset(parsed.navigationTargets, 5)).toBeUndefined();
    expect(targetAtOffset(parsed.navigationTargets, 14)?.path).toEqual(["app", "database", "host"]);
    expect(targetAtOffset(parsed.stringTargets, 23)?.value).toBe("localhost");
  });

  test("accepts the position immediately after a target like editor definition requests", () => {
    const source = "[openai]\nmodel = \"gpt\"\n";
    const parsed = parseTomlDocument(source);

    expect(targetAtOffset(parsed.navigationTargets, 14)?.path).toEqual(["openai", "model"]);
  });
});
