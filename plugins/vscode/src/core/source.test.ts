import { describe, expect, test } from "vitest";

import { declarationsForExtension } from "./languages";

describe("source declaration scanners", () => {
  test("extracts Go containers, direct fields, and tag aliases", () => {
    const source = `package config

type OpenAIConfig struct {
    BaseURL string \`toml:"base-url" json:"base_url"\`
    Model string
}
`;

    expect(declarationsForExtension("go", source)).toEqual([
      declaration(source, "OpenAIConfig", "container"),
      declaration(source, "BaseURL", "field", "OpenAIConfig", ["base-url", "base_url"]),
      declaration(source, "Model", "field", "OpenAIConfig"),
    ]);
  });

  test("keeps duplicate Python fields attached to their direct class", () => {
    const source = `class OpenAIConfig:
    model: str

class DatabaseConfig:
    model: str
    def load(self):
        model = "local"
`;

    expect(declarationsForExtension("py", source)).toEqual([
      declaration(source, "OpenAIConfig", "container"),
      declaration(source, "DatabaseConfig", "container"),
      declaration(source, "model", "field", "OpenAIConfig", [], 0),
      declaration(source, "model", "field", "DatabaseConfig", [], 1),
    ]);
  });

  test("ignores nested and triple-quoted Python declaration text", () => {
    const source = `EXAMPLE = """
class FakeConfig:
    model: str
"""

def build():
    class LocalConfig:
        value: str
`;

    expect(declarationsForExtension("py", source)).toEqual([]);
  });

  test("extracts Java fields and JsonProperty aliases", () => {
    const source = `class OpenAIConfig {
    @JsonProperty("base-url")
    private String baseUrl;
    private String model;

    void load() {
        String model;
    }
}
`;

    expect(declarationsForExtension("java", source)).toEqual([
      declaration(source, "OpenAIConfig", "container"),
      declaration(source, "baseUrl", "field", "OpenAIConfig", ["base-url"]),
      declaration(source, "model", "field", "OpenAIConfig"),
    ]);
  });

  test("ignores Java declaration text in comments and text blocks", () => {
    const source = `/* class CommentedConfig { String model; } */
class RealConfig {
    String example = """
        class FakeConfig {
            String model;
        }
        """;
}
`;

    expect(declarationsForExtension("java", source)).toEqual([
      declaration(source, "RealConfig", "container"),
      declaration(source, "example", "field", "RealConfig"),
    ]);
  });

  test("extracts TypeScript interface and type properties with direct owners", () => {
    const source = `export interface OpenAIConfig {
  model?: string;
}

type DatabaseConfig = {
  "base-url": string;
  nested: {
    model: string;
  };
};
`;

    expect(declarationsForExtension("tsx", source)).toEqual([
      declaration(source, "OpenAIConfig", "container"),
      declaration(source, "DatabaseConfig", "container"),
      declaration(source, "model", "field", "OpenAIConfig"),
      declaration(source, "base-url", "field", "DatabaseConfig"),
      declaration(source, "nested", "field", "DatabaseConfig"),
    ]);
  });

  test("extracts only top-level JavaScript config objects and direct properties", () => {
    const source = `export const openAIConfig = {
  model: "gpt",
  "base-url": "https://example.test",
  nested: {
    model: "local"
  }
};

function build() {
  const localConfig = {
    model: "local"
  };
}
`;

    expect(declarationsForExtension("jsx", source)).toEqual([
      declaration(source, "openAIConfig", "container"),
      declaration(source, "model", "field", "openAIConfig"),
      declaration(source, "base-url", "field", "openAIConfig"),
      declaration(source, "nested", "field", "openAIConfig"),
    ]);
  });

  test("ignores commented Go declarations and unsupported extensions", () => {
    const source = `package config
/*
type FakeConfig struct {
    Model string
}
*/
`;

    expect(declarationsForExtension("go", source)).toEqual([]);
    expect(declarationsForExtension("rs", "struct Config {}")).toEqual([]);
  });
});

function declaration(
  source: string,
  label: string,
  kind: "container" | "field",
  ownerLabel?: string,
  aliases: readonly string[] = [],
  occurrence = 0,
): object {
  let start = -1;
  for (let index = 0; index <= occurrence; index += 1) {
    start = source.indexOf(label, start + 1);
  }
  return {
    aliases,
    kind,
    label,
    ownerLabel,
    range: { start, end: start + label.length },
  };
}
