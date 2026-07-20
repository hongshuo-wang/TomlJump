import { describe, expect, test } from "vitest";

import {
  findTopLevelPythonCallable,
  isPythonModulePath,
  parsePyProjectEntryPoint,
} from "./entrypoint";

describe("parsePyProjectEntryPoint", () => {
  test("returns separate module and callable ranges", () => {
    expect(
      parsePyProjectEntryPoint(
        "pyproject.toml",
        ["project", "scripts", "serve"],
        "package.cli:main",
        { start: 27, end: 43 },
      ),
    ).toEqual({
      member: "main",
      memberRange: { start: 39, end: 43 },
      qualifier: "package.cli",
      qualifierRange: { start: 27, end: 38 },
    });
  });

  test("rejects unsupported files, contexts, separators, and identifiers", () => {
    const validPath = ["project", "scripts", "serve"];
    const range = { start: 0, end: 20 };
    expect(parsePyProjectEntryPoint("other.toml", validPath, "package.cli:main", range)).toBeUndefined();
    expect(
      parsePyProjectEntryPoint("pyproject.toml", ["tool", "scripts", "serve"], "package.cli:main", range),
    ).toBeUndefined();
    for (const value of ["package.cli", ":main", "package.cli:", "package.cli:main:extra", "包.cli:main"]) {
      expect(parsePyProjectEntryPoint("pyproject.toml", validPath, value, range), value).toBeUndefined();
    }
  });
});

describe("Python entry-point resolution", () => {
  test("matches module files and package initializers", () => {
    expect(isPythonModulePath("/workspace/package/cli.py", "package.cli")).toBe(true);
    expect(isPythonModulePath("C:\\workspace\\package\\cli\\__init__.py", "package.cli")).toBe(true);
    expect(isPythonModulePath("/workspace/package/other.py", "package.cli")).toBe(false);
  });

  test("finds normal and async top-level callables", () => {
    const source = `def main():
    pass

async def serve ():
    pass
`;
    expect(findTopLevelPythonCallable(source, "main")).toEqual({ start: 4, end: 8 });
    expect(findTopLevelPythonCallable(source, "serve")).toEqual({ start: 32, end: 37 });
  });

  test("rejects nested, commented, and string-contained callables", () => {
    const source = `class Commands:
    def main(self):
        pass

# def main():
EXAMPLE = """
def main():
"""
`;
    expect(findTopLevelPythonCallable(source, "main")).toBeUndefined();
  });
});
