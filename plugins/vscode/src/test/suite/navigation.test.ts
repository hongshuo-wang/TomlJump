import assert from "node:assert/strict";
import path from "node:path";

import * as vscode from "vscode";

export async function runNavigationTests(): Promise<void> {
  const workspace = vscode.workspace.workspaceFolders?.[0];
  assert.ok(workspace, "The navigation demo workspace must be open");
  const extension = vscode.extensions.getExtension("harrisonwang.tomljump");
  assert.ok(extension, "TomlJump development extension must be installed");
  await extension.activate();

  await assertDefinition(workspace.uri, "app.toml", "go_token", "go/config.go", "GoToken");
  await assertDefinition(workspace.uri, "go/config.go", "GoToken", "app.toml", "go_token", true);
  await assertDefinition(workspace.uri, "app.toml", "./schemas/user.json", "schemas/user.json", "");
  await assertDefinition(workspace.uri, "pyproject.toml", "main", "python/cli.py", "main");
}

async function assertDefinition(
  workspaceUri: vscode.Uri,
  sourcePath: string,
  sourceNeedle: string,
  expectedTargetPath: string,
  expectedTargetNeedle: string,
  useLastOccurrence = false,
): Promise<void> {
  const sourceUri = vscode.Uri.joinPath(workspaceUri, sourcePath);
  const document = await vscode.workspace.openTextDocument(sourceUri);
  const sourceOffset = useLastOccurrence
    ? document.getText().lastIndexOf(sourceNeedle)
    : document.getText().indexOf(sourceNeedle);
  assert.notEqual(sourceOffset, -1, `Missing ${sourceNeedle} in ${sourcePath}`);
  const definitions =
    (await vscode.commands.executeCommand<readonly (vscode.Location | vscode.LocationLink)[]>(
      "vscode.executeDefinitionProvider",
      sourceUri,
      document.positionAt(sourceOffset + Math.max(0, Math.floor(sourceNeedle.length / 2))),
    )) ?? [];

  const expectedSuffix = path.normalize(expectedTargetPath);
  const matching = definitions.find((definition) => {
    const uri = "uri" in definition ? definition.uri : definition.targetUri;
    return path.normalize(uri.fsPath).endsWith(expectedSuffix);
  });
  assert.ok(matching, `Expected ${sourcePath}:${sourceNeedle} to resolve to ${expectedTargetPath}`);

  if (expectedTargetNeedle.length > 0) {
    const targetUri = "uri" in matching ? matching.uri : matching.targetUri;
    const targetRange = "range" in matching ? matching.range : matching.targetSelectionRange;
    const targetDocument = await vscode.workspace.openTextDocument(targetUri);
    assert.equal(targetDocument.getText(targetRange), expectedTargetNeedle);
  }
}
