import { SourceDeclaration, SourceDeclarationScanner } from "../source";
import { goDeclarations } from "./go";
import { javaDeclarations } from "./java";
import { javaScriptDeclarations } from "./javascript";
import { pythonDeclarations } from "./python";
import { typeScriptDeclarations } from "./typescript";

const SCANNERS: Readonly<Record<string, SourceDeclarationScanner>> = {
  go: goDeclarations,
  java: javaDeclarations,
  js: javaScriptDeclarations,
  jsx: javaScriptDeclarations,
  py: pythonDeclarations,
  ts: typeScriptDeclarations,
  tsx: typeScriptDeclarations,
};

export const SUPPORTED_SOURCE_EXTENSIONS = Object.freeze(["go", "py", "java", "ts", "tsx", "js", "jsx"]);

export function declarationsForExtension(extension: string, source: string): readonly SourceDeclaration[] {
  return SCANNERS[extension.toLowerCase()]?.(source) ?? [];
}
