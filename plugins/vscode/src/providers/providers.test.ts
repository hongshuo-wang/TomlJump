import { describe, expect, test } from "vitest";

import {
  definitionsFromSource,
  definitionsFromToml,
  NavigationWorkspace,
  TextDocumentSnapshot,
  WorkspaceTextFile,
} from "../navigation";

describe("TOML definition navigation", () => {
  test("resolves a relative file path", async () => {
    const workspace = new MemoryWorkspace([
      file("file:///workspace/schemas/user.json", "/workspace/schemas/user.json", "{}"),
    ]);
    const document = doc("file:///workspace/app.toml", "/workspace/app.toml", "schema = \"./schemas/user.json\"\n");

    const definitions = await definitionsFromToml(document, 20, workspace);

    expect(definitions).toEqual([
      { fileId: "file:///workspace/schemas/user.json", range: { start: 0, end: 0 } },
    ]);
  });

  test("resolves a TOML key to all credible source declarations", async () => {
    const workspace = new MemoryWorkspace([
      file(
        "file:///workspace/config.go",
        "/workspace/config.go",
        "package config\n\ntype OpenAIConfig struct {\n    Model string\n}\n",
      ),
      file(
        "file:///workspace/config.py",
        "/workspace/config.py",
        "class OpenAIConfig:\n    model: str\n",
      ),
    ]);
    const document = doc("file:///workspace/app.toml", "/workspace/app.toml", "[openai]\nmodel = \"gpt\"\n");

    const definitions = await definitionsFromToml(document, 11, workspace);

    expect(definitions.map((definition) => definition.fileId)).toEqual([
      "file:///workspace/config.go",
      "file:///workspace/config.py",
    ]);
    expect(definitions.map((definition) => definition.range.start)).toEqual([47, 24]);
    expect(workspace.requestedLimits.every((limit) => limit === 500)).toBe(true);
  });

  test("resolves project script module and callable segments separately", async () => {
    const python = `def helper():
    pass

async def main():
    pass
`;
    const workspace = new MemoryWorkspace([
      file("file:///workspace/package/cli.py", "/workspace/package/cli.py", python),
    ]);
    const source = "[project.scripts]\nserve = \"package.cli:main\"\n";
    const document = doc("file:///workspace/pyproject.toml", "/workspace/pyproject.toml", source);

    expect(await definitionsFromToml(document, source.indexOf("package") + 2, workspace)).toEqual([
      { fileId: "file:///workspace/package/cli.py", range: { start: 0, end: 0 } },
    ]);
    expect(await definitionsFromToml(document, source.indexOf("main") + 1, workspace)).toEqual([
      { fileId: "file:///workspace/package/cli.py", range: { start: 34, end: 38 } },
    ]);
  });

  test("stays quiet for mismatched owners, large files, read failures, and cancellation", async () => {
    const workspace = new MemoryWorkspace([
      file(
        "file:///workspace/config.go",
        "/workspace/config.go",
        "package config\n\ntype Credentials struct {\n    Host string\n}\n",
      ),
      { ...file("file:///workspace/large.go", "/workspace/large.go", "type DatabaseConfig struct {}"), size: 1_000_001 },
      { ...file("file:///workspace/broken.go", "/workspace/broken.go", ""), readError: true },
    ]);
    const document = doc(
      "file:///workspace/app.toml",
      "/workspace/app.toml",
      "[app.database]\nhost = \"localhost\"\n",
    );

    expect(await definitionsFromToml(document, 16, workspace)).toEqual([]);
    expect(await definitionsFromToml(document, 16, workspace, { isCancellationRequested: true })).toEqual([]);
  });
});

describe("source definition navigation", () => {
  test("resolves a source field to matching keys in multiple TOML files", async () => {
    const workspace = new MemoryWorkspace([
      file("file:///workspace/dev.toml", "/workspace/dev.toml", "[openai]\nmodel = \"dev\"\n"),
      file("file:///workspace/prod.toml", "/workspace/prod.toml", "[openai]\nmodel = \"prod\"\n"),
    ]);
    const source = "interface OpenAIConfig {\n  model: string;\n}\n";
    const document = doc("file:///workspace/config.ts", "/workspace/config.ts", source);

    const definitions = await definitionsFromSource(document, source.indexOf("model") + 2, workspace);

    expect(definitions).toEqual([
      { fileId: "file:///workspace/dev.toml", range: { start: 9, end: 14 } },
      { fileId: "file:///workspace/prod.toml", range: { start: 9, end: 14 } },
    ]);
  });

  test("does not intercept locals or unsupported source extensions", async () => {
    const workspace = new MemoryWorkspace([
      file("file:///workspace/app.toml", "/workspace/app.toml", "[openai]\nmodel = \"gpt\"\n"),
    ]);
    const source = `class OpenAIConfig:
    model: str
    def load(self):
        model = "local"
`;

    expect(
      await definitionsFromSource(
        doc("file:///workspace/config.py", "/workspace/config.py", source),
        source.lastIndexOf("model") + 1,
        workspace,
      ),
    ).toEqual([]);
    expect(
      await definitionsFromSource(
        doc("file:///workspace/config.rs", "/workspace/config.rs", "struct Config {}"),
        8,
        workspace,
      ),
    ).toEqual([]);
  });
});

class MemoryWorkspace implements NavigationWorkspace {
  readonly requestedLimits: number[] = [];

  constructor(private readonly files: readonly MemoryFile[]) {}

  findFilesByExtension(extension: string, maxResults: number): Promise<readonly WorkspaceTextFile[]> {
    this.requestedLimits.push(maxResults);
    return Promise.resolve(
      this.files.filter((entry) => entry.path.endsWith(`.${extension}`)).slice(0, maxResults),
    );
  }

  readText(fileToRead: WorkspaceTextFile): Promise<string> {
    const entry = this.files.find((candidate) => candidate.id === fileToRead.id);
    if (entry?.readError === true) {
      return Promise.reject(new Error("read failed"));
    }
    return Promise.resolve(entry?.text ?? "");
  }

  resolveRelativeFile(
    document: TextDocumentSnapshot,
    relativePath: string,
  ): Promise<WorkspaceTextFile | undefined> {
    const base = document.path.slice(0, document.path.lastIndexOf("/") + 1);
    return Promise.resolve(this.files.find((entry) => entry.path === base + relativePath));
  }
}

interface MemoryFile extends WorkspaceTextFile {
  readonly readError?: boolean;
  readonly text: string;
}

function file(id: string, path: string, text: string): MemoryFile {
  return { id, path, size: Buffer.byteLength(text), text };
}

function doc(id: string, path: string, text: string): TextDocumentSnapshot {
  return {
    extension: path.slice(path.lastIndexOf(".") + 1),
    fileName: path.slice(path.lastIndexOf("/") + 1),
    id,
    path,
    text,
  };
}
