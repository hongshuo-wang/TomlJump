import { findTopLevelPythonCallable, isPythonModulePath, parsePyProjectEntryPoint } from "./core/entrypoint";
import { declarationsForExtension, SUPPORTED_SOURCE_EXTENSIONS } from "./core/languages";
import {
  matchSourceDeclarationToToml,
  matchTomlTargetToSources,
  SourceFileDeclarations,
} from "./core/matcher";
import { classifyFilePath } from "./core/reference";
import { parseTomlDocument, targetAtOffset } from "./core/toml";
import { OffsetRange, TomlNavigationTarget } from "./core/types";

const MAX_FILES_PER_EXTENSION = 500;
const MAX_FILE_BYTES = 1_000_000;

export interface CancellationSignal {
  readonly isCancellationRequested: boolean;
}

export interface TextDocumentSnapshot {
  readonly extension: string;
  readonly fileName: string;
  readonly id: string;
  readonly path: string;
  readonly text: string;
}

export interface WorkspaceTextFile {
  readonly id: string;
  readonly path: string;
  readonly size: number;
}

export interface NavigationWorkspace {
  findFilesByExtension(extension: string, maxResults: number): Promise<readonly WorkspaceTextFile[]>;
  readText(file: WorkspaceTextFile): Promise<string>;
  resolveRelativeFile(
    document: TextDocumentSnapshot,
    relativePath: string,
  ): Promise<WorkspaceTextFile | undefined>;
}

export interface DefinitionTarget {
  readonly fileId: string;
  readonly range: OffsetRange;
}

export async function definitionsFromToml(
  document: TextDocumentSnapshot,
  offset: number,
  workspace: NavigationWorkspace,
  cancellation: CancellationSignal = { isCancellationRequested: false },
): Promise<readonly DefinitionTarget[]> {
  if (cancellation.isCancellationRequested) {
    return [];
  }
  const parsed = parseTomlDocument(document.text);
  const stringTarget = targetAtOffset(parsed.stringTargets, offset);
  if (stringTarget !== undefined) {
    const entryPoint = parsePyProjectEntryPoint(
      document.fileName,
      stringTarget.keyPath,
      stringTarget.value,
      stringTarget.range,
    );
    if (entryPoint !== undefined) {
      const memberRequested = offset > entryPoint.qualifierRange.end;
      return resolvePythonEntryPoint(
        entryPoint.qualifier,
        memberRequested ? entryPoint.member : undefined,
        workspace,
        cancellation,
      );
    }
    const filePath = classifyFilePath(stringTarget.value);
    if (filePath !== undefined) {
      try {
        const target = await workspace.resolveRelativeFile(document, filePath.lookupValue);
        return target === undefined || cancellation.isCancellationRequested
          ? []
          : [{ fileId: target.id, range: { start: 0, end: 0 } }];
      } catch {
        return [];
      }
    }
  }

  const navigationTarget = targetAtOffset(parsed.navigationTargets, offset);
  if (navigationTarget === undefined) {
    return [];
  }
  const sourceFiles = await collectSourceDeclarations(workspace, cancellation);
  if (cancellation.isCancellationRequested) {
    return [];
  }
  return deduplicateDefinitions(
    matchTomlTargetToSources(navigationTarget, sourceFiles).map((match) => ({
      fileId: match.fileId,
      range: match.declaration.range,
    })),
  );
}

export async function definitionsFromSource(
  document: TextDocumentSnapshot,
  offset: number,
  workspace: NavigationWorkspace,
  cancellation: CancellationSignal = { isCancellationRequested: false },
): Promise<readonly DefinitionTarget[]> {
  if (cancellation.isCancellationRequested) {
    return [];
  }
  const declarations = declarationsForExtension(document.extension, document.text).filter(
    (declaration) => offset >= declaration.range.start && offset <= declaration.range.end,
  );
  if (declarations.length === 0) {
    return [];
  }
  const tomlFiles = await readableFiles(workspace, "toml", cancellation);
  const targetsByFile: Array<{ fileId: string; targets: readonly TomlNavigationTarget[] }> = [];
  for (const file of tomlFiles) {
    if (cancellation.isCancellationRequested) {
      return [];
    }
    const text = await safeRead(workspace, file);
    if (text !== undefined) {
      targetsByFile.push({ fileId: file.id, targets: parseTomlDocument(text).navigationTargets });
    }
  }
  return deduplicateDefinitions(
    declarations.flatMap((declaration) =>
      targetsByFile.flatMap((file) =>
        matchSourceDeclarationToToml(declaration, file.targets).map((target) => ({
          fileId: file.fileId,
          range: target.range,
        })),
      ),
    ),
  );
}

async function collectSourceDeclarations(
  workspace: NavigationWorkspace,
  cancellation: CancellationSignal,
): Promise<SourceFileDeclarations[]> {
  const result: SourceFileDeclarations[] = [];
  for (const extension of SUPPORTED_SOURCE_EXTENSIONS) {
    const files = await readableFiles(workspace, extension, cancellation);
    for (const file of files) {
      if (cancellation.isCancellationRequested) {
        return [];
      }
      const text = await safeRead(workspace, file);
      if (text !== undefined) {
        result.push({ declarations: declarationsForExtension(extension, text), id: file.id });
      }
    }
  }
  return result;
}

async function resolvePythonEntryPoint(
  qualifier: string,
  member: string | undefined,
  workspace: NavigationWorkspace,
  cancellation: CancellationSignal,
): Promise<DefinitionTarget[]> {
  const files = await readableFiles(workspace, "py", cancellation);
  const definitions: DefinitionTarget[] = [];
  for (const file of files.filter((candidate) => isPythonModulePath(candidate.path, qualifier))) {
    if (cancellation.isCancellationRequested) {
      return [];
    }
    if (member === undefined) {
      definitions.push({ fileId: file.id, range: { start: 0, end: 0 } });
      continue;
    }
    const text = await safeRead(workspace, file);
    const range = text === undefined ? undefined : findTopLevelPythonCallable(text, member);
    if (range !== undefined) {
      definitions.push({ fileId: file.id, range });
    }
  }
  return deduplicateDefinitions(definitions);
}

async function readableFiles(
  workspace: NavigationWorkspace,
  extension: string,
  cancellation: CancellationSignal,
): Promise<readonly WorkspaceTextFile[]> {
  if (cancellation.isCancellationRequested) {
    return [];
  }
  try {
    return (await workspace.findFilesByExtension(extension, MAX_FILES_PER_EXTENSION)).filter(
      (file) => file.size <= MAX_FILE_BYTES,
    );
  } catch {
    return [];
  }
}

async function safeRead(
  workspace: NavigationWorkspace,
  file: WorkspaceTextFile,
): Promise<string | undefined> {
  try {
    return await workspace.readText(file);
  } catch {
    return undefined;
  }
}

function deduplicateDefinitions(definitions: readonly DefinitionTarget[]): DefinitionTarget[] {
  const seen = new Set<string>();
  return definitions.filter((definition) => {
    const key = `${definition.fileId}:${definition.range.start}:${definition.range.end}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}
