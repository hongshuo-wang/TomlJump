import { describe, expect, test } from "vitest";

import {
  matchSourceDeclarationToToml,
  matchTomlTargetToSources,
  matchesContainerName,
  normalizeConfigName,
} from "./matcher";
import { SourceDeclaration } from "./source";
import { TomlNavigationTarget } from "./types";

describe("configuration name matching", () => {
  test("normalizes separators and case and accepts Config suffix", () => {
    expect(normalizeConfigName("base-url_value")).toBe("baseurlvalue");
    expect(matchesContainerName("open-ai", "OpenAIConfig")).toBe(true);
    expect(matchesContainerName("database", "Credentials")).toBe(false);
  });
});

describe("forward matching", () => {
  test("matches tables to credible source containers", () => {
    const files = [{ id: "config.go", declarations: [container("OpenAIConfig"), container("Database")] }];

    const matches = matchTomlTargetToSources(toml("table", ["app", "open-ai"]), files);

    expect(matches.map((match) => match.declaration.label)).toEqual(["OpenAIConfig"]);
  });

  test("uses aliases before normalized field names", () => {
    const files = [
      {
        id: "config.go",
        declarations: [
          container("OpenAIConfig"),
          field("BaseURL", "OpenAIConfig", ["base-url"]),
          field("base_url", "OpenAIConfig"),
        ],
      },
    ];

    const matches = matchTomlTargetToSources(toml("key", ["openai", "base-url"]), files);

    expect(matches.map((match) => match.declaration.label)).toEqual(["BaseURL"]);
  });

  test("requires the nearest container for duplicate fields", () => {
    const files = [
      {
        id: "config.py",
        declarations: [
          container("OpenAIConfig"),
          field("model", "OpenAIConfig"),
          container("DatabaseConfig"),
          field("model", "DatabaseConfig"),
        ],
      },
    ];

    const matches = matchTomlTargetToSources(toml("key", ["app", "database", "model"]), files);

    expect(matches.map((match) => match.declaration.ownerLabel)).toEqual(["DatabaseConfig"]);
  });

  test("prefers candidate files that also contain the matching container", () => {
    const files = [
      {
        id: "primary.go",
        declarations: [container("OpenAIConfig"), field("model", "OpenAIConfig")],
      },
      {
        id: "secondary.go",
        declarations: [field("model", "OpenAIConfig")],
      },
    ];

    const matches = matchTomlTargetToSources(toml("key", ["openai", "model"]), files);

    expect(matches.map((match) => match.fileId)).toEqual(["primary.go"]);
  });

  test("keeps unsupported owner paths unresolved", () => {
    const files = [
      {
        id: "config.go",
        declarations: [container("Credentials"), field("host", "Credentials")],
      },
    ];

    expect(matchTomlTargetToSources(toml("key", ["app", "database", "host"]), files)).toEqual([]);
  });
});

describe("reverse matching", () => {
  const targets = [
    toml("table", ["openai"], 1),
    toml("key", ["openai", "model"], 10),
    toml("table", ["app", "database"], 20),
    toml("key", ["app", "database", "host"], 30),
  ];

  test("matches a container to all credible TOML tables", () => {
    expect(matchSourceDeclarationToToml(container("OpenAIConfig"), targets)).toEqual([targets[0]]);
  });

  test("matches a field using normalized name and direct owner", () => {
    expect(matchSourceDeclarationToToml(field("Host", "DatabaseConfig"), targets)).toEqual([targets[3]]);
  });

  test("uses an explicit alias and rejects mismatched owners", () => {
    expect(matchSourceDeclarationToToml(field("baseUrl", "OpenAIConfig", ["model"]), targets)).toEqual([
      targets[1],
    ]);
    expect(matchSourceDeclarationToToml(field("Host", "Credentials"), targets)).toEqual([]);
  });
});

function toml(
  kind: TomlNavigationTarget["kind"],
  path: readonly string[],
  start = 0,
): TomlNavigationTarget {
  return { kind, path, range: { start, end: start + path.at(-1)!.length } };
}

function container(label: string): SourceDeclaration {
  return declaration("container", label);
}

function field(label: string, ownerLabel: string, aliases: readonly string[] = []): SourceDeclaration {
  return declaration("field", label, ownerLabel, aliases);
}

function declaration(
  kind: SourceDeclaration["kind"],
  label: string,
  ownerLabel?: string,
  aliases: readonly string[] = [],
): SourceDeclaration {
  return { aliases, kind, label, ownerLabel, range: { start: 0, end: label.length } };
}
