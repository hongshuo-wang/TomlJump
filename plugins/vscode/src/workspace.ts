import * as vscode from "vscode";

import {
  DefinitionTarget,
  NavigationWorkspace,
  TextDocumentSnapshot,
  WorkspaceTextFile,
} from "./navigation";

const EXCLUDED_DIRECTORIES = "**/{.git,.idea,node_modules,build,dist,out}/**";

export class VsCodeNavigationWorkspace implements NavigationWorkspace {
  async findFilesByExtension(
    extension: string,
    maxResults: number,
  ): Promise<readonly WorkspaceTextFile[]> {
    const uris = await vscode.workspace.findFiles(`**/*.${extension}`, EXCLUDED_DIRECTORIES, maxResults);
    const files = await Promise.all(
      uris.map(async (uri): Promise<WorkspaceTextFile | undefined> => {
        try {
          const stat = await vscode.workspace.fs.stat(uri);
          if ((stat.type & vscode.FileType.Directory) !== 0) {
            return undefined;
          }
          return { id: uri.toString(), path: uri.path, size: stat.size };
        } catch {
          return undefined;
        }
      }),
    );
    return files.filter((file): file is WorkspaceTextFile => file !== undefined);
  }

  async readText(file: WorkspaceTextFile): Promise<string> {
    const uri = vscode.Uri.parse(file.id);
    const openDocument = vscode.workspace.textDocuments.find(
      (document) => document.uri.toString() === file.id,
    );
    if (openDocument !== undefined) {
      return openDocument.getText();
    }
    return new TextDecoder().decode(await vscode.workspace.fs.readFile(uri));
  }

  async resolveRelativeFile(
    document: TextDocumentSnapshot,
    relativePath: string,
  ): Promise<WorkspaceTextFile | undefined> {
    const uri = vscode.Uri.joinPath(vscode.Uri.parse(document.id), "..", relativePath);
    try {
      const stat = await vscode.workspace.fs.stat(uri);
      if ((stat.type & vscode.FileType.Directory) !== 0) {
        return undefined;
      }
      return { id: uri.toString(), path: uri.path, size: stat.size };
    } catch {
      return undefined;
    }
  }
}

export function snapshotOf(document: vscode.TextDocument): TextDocumentSnapshot {
  const path = document.uri.path;
  const fileName = path.slice(path.lastIndexOf("/") + 1);
  const dot = fileName.lastIndexOf(".");
  return {
    extension: dot < 0 ? "" : fileName.slice(dot + 1),
    fileName,
    id: document.uri.toString(),
    path,
    text: document.getText(),
  };
}

export async function toLocations(targets: readonly DefinitionTarget[]): Promise<vscode.Location[]> {
  const documentCache = new Map<string, vscode.TextDocument>();
  const locations: vscode.Location[] = [];
  for (const target of targets) {
    try {
      const uri = vscode.Uri.parse(target.fileId);
      let document = documentCache.get(target.fileId);
      if (document === undefined) {
        document = await vscode.workspace.openTextDocument(uri);
        documentCache.set(target.fileId, document);
      }
      locations.push(
        new vscode.Location(
          uri,
          new vscode.Range(document.positionAt(target.range.start), document.positionAt(target.range.end)),
        ),
      );
    } catch {
      // A file may disappear between workspace search and location conversion.
    }
  }
  return locations;
}
