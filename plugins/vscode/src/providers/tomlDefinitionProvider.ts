import * as vscode from "vscode";

import { definitionsFromToml } from "../navigation";
import { snapshotOf, toLocations, VsCodeNavigationWorkspace } from "../workspace";

export class TomlDefinitionProvider implements vscode.DefinitionProvider {
  constructor(private readonly workspace = new VsCodeNavigationWorkspace()) {}

  async provideDefinition(
    document: vscode.TextDocument,
    position: vscode.Position,
    token: vscode.CancellationToken,
  ): Promise<vscode.Definition | undefined> {
    try {
      const targets = await definitionsFromToml(
        snapshotOf(document),
        document.offsetAt(position),
        this.workspace,
        token,
      );
      const locations = await toLocations(targets);
      return locations.length === 0 ? undefined : locations;
    } catch {
      return undefined;
    }
  }
}
