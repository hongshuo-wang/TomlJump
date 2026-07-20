import * as vscode from "vscode";

import { SourceDefinitionProvider } from "./providers/sourceDefinitionProvider";
import { TomlDefinitionProvider } from "./providers/tomlDefinitionProvider";

export function activate(context: vscode.ExtensionContext): void {
  const sourceLanguages = [
    "go",
    "python",
    "java",
    "typescript",
    "typescriptreact",
    "javascript",
    "javascriptreact",
  ].map((language) => ({ language }));

  context.subscriptions.push(
    vscode.languages.registerDefinitionProvider({ language: "toml" }, new TomlDefinitionProvider()),
    vscode.languages.registerDefinitionProvider(sourceLanguages, new SourceDefinitionProvider()),
  );
}

export function deactivate(): void {}
