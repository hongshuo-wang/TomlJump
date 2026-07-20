import { AST, parseTOML } from "toml-eslint-parser";

import {
  OffsetRange,
  ParsedTomlDocument,
  TomlNavigationTarget,
  TomlStringTarget,
} from "./types";

const EMPTY_DOCUMENT: ParsedTomlDocument = {
  navigationTargets: [],
  stringTargets: [],
};

export function parseTomlDocument(source: string): ParsedTomlDocument {
  try {
    const program = parseTOML(source, { tomlVersion: "1.0" });
    const navigationTargets: TomlNavigationTarget[] = [];
    const stringTargets: TomlStringTarget[] = [];
    const topLevel = program.body[0];

    for (const node of topLevel.body) {
      if (node.type === "TOMLTable") {
        const tablePath = node.resolvedKey.filter((segment): segment is string => typeof segment === "string");
        const leaf = node.key.keys.at(-1);
        if (tablePath.length === 0 || leaf === undefined) {
          continue;
        }
        navigationTargets.push({ kind: "table", path: tablePath, range: semanticRange(leaf) });
        for (const entry of node.body) {
          collectKeyValue(source, entry, tablePath, navigationTargets, stringTargets);
        }
      } else {
        collectKeyValue(source, node, [], navigationTargets, stringTargets);
      }
    }

    return { navigationTargets, stringTargets };
  } catch {
    return EMPTY_DOCUMENT;
  }
}

export function targetAtOffset<T extends { readonly range: OffsetRange }>(
  targets: readonly T[],
  offset: number,
): T | undefined {
  return targets.find((target) => offset >= target.range.start && offset <= target.range.end);
}

function collectKeyValue(
  source: string,
  entry: AST.TOMLKeyValue,
  ownerPath: readonly string[],
  navigationTargets: TomlNavigationTarget[],
  stringTargets: TomlStringTarget[],
): void {
  if (entry.parent.type === "TOMLInlineTable") {
    return;
  }
  const segments = entry.key.keys.map(keyName);
  const leaf = entry.key.keys.at(-1);
  if (segments.length === 0 || leaf === undefined) {
    return;
  }
  const keyPath = [...ownerPath, ...segments];
  navigationTargets.push({ kind: "key", path: keyPath, range: semanticRange(leaf) });

  if (entry.value.type !== "TOMLValue" || entry.value.kind !== "string" || entry.value.multiline) {
    return;
  }
  const range = semanticRange(entry.value);
  stringTargets.push({ keyPath, range, value: source.slice(range.start, range.end) });
}

function keyName(key: AST.TOMLBare | AST.TOMLQuoted): string {
  return key.type === "TOMLBare" ? key.name : key.value;
}

function semanticRange(node: AST.TOMLBare | AST.TOMLQuoted | AST.TOMLStringValue): OffsetRange {
  if (node.type === "TOMLBare") {
    return { start: node.range[0], end: node.range[1] };
  }
  return { start: node.range[0] + 1, end: node.range[1] - 1 };
}
