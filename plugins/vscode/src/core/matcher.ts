import { SourceDeclaration } from "./source";
import { TomlNavigationTarget } from "./types";

export interface SourceFileDeclarations {
  readonly declarations: readonly SourceDeclaration[];
  readonly id: string;
}

export interface SourceMatch {
  readonly declaration: SourceDeclaration;
  readonly fileId: string;
}

export function normalizeConfigName(value: string): string {
  return value.replace(/[^A-Za-z0-9]/g, "").toLowerCase();
}

export function matchesContainerName(configName: string, sourceName: string): boolean {
  const normalizedConfig = normalizeConfigName(configName);
  const normalizedSource = normalizeConfigName(sourceName);
  return normalizedSource === normalizedConfig || normalizedSource === `${normalizedConfig}config`;
}

export function matchTomlTargetToSources(
  target: TomlNavigationTarget,
  files: readonly SourceFileDeclarations[],
): readonly SourceMatch[] {
  const leaf = target.path.at(-1);
  if (leaf === undefined) {
    return [];
  }
  if (target.kind === "table") {
    return deduplicateSourceMatches(
      allDeclarations(files).filter(
        (match) =>
          match.declaration.kind === "container" && matchesContainerName(leaf, match.declaration.label),
      ),
    );
  }

  const ownerName = target.path.length > 1 ? target.path.at(-2) : undefined;
  const fields = allDeclarations(files).filter((match) => {
    if (match.declaration.kind !== "field") {
      return false;
    }
    return (
      ownerName === undefined ||
      (match.declaration.ownerLabel !== undefined &&
        matchesContainerName(ownerName, match.declaration.ownerLabel))
    );
  });
  const aliasMatches = fields.filter((match) => match.declaration.aliases.includes(leaf));
  let candidates =
    aliasMatches.length > 0
      ? aliasMatches
      : fields.filter((match) => normalizeConfigName(leaf) === normalizeConfigName(match.declaration.label));

  if (ownerName !== undefined && candidates.length > 0) {
    const containerFiles = new Set(
      allDeclarations(files)
        .filter(
          (match) =>
            match.declaration.kind === "container" &&
            matchesContainerName(ownerName, match.declaration.label),
        )
        .map((match) => match.fileId),
    );
    if (containerFiles.size > 0) {
      candidates = candidates.filter((match) => containerFiles.has(match.fileId));
    }
  }
  return deduplicateSourceMatches(candidates);
}

export function matchSourceDeclarationToToml(
  declaration: SourceDeclaration,
  targets: readonly TomlNavigationTarget[],
): readonly TomlNavigationTarget[] {
  if (declaration.kind === "container") {
    return targets.filter(
      (target) =>
        target.kind === "table" &&
        target.path.length > 0 &&
        matchesContainerName(target.path.at(-1)!, declaration.label),
    );
  }
  if (declaration.ownerLabel === undefined) {
    return [];
  }
  return targets.filter((target) => {
    if (target.kind !== "key" || target.path.length < 2) {
      return false;
    }
    const leaf = target.path.at(-1)!;
    const owner = target.path.at(-2)!;
    const fieldMatches =
      declaration.aliases.length > 0
        ? declaration.aliases.includes(leaf)
        : normalizeConfigName(leaf) === normalizeConfigName(declaration.label);
    return fieldMatches && matchesContainerName(owner, declaration.ownerLabel!);
  });
}

function allDeclarations(files: readonly SourceFileDeclarations[]): SourceMatch[] {
  return files.flatMap((file) =>
    file.declarations.map((declaration) => ({ declaration, fileId: file.id })),
  );
}

function deduplicateSourceMatches(matches: readonly SourceMatch[]): SourceMatch[] {
  const seen = new Set<string>();
  return matches.filter((match) => {
    const key = `${match.fileId}:${match.declaration.range.start}:${match.declaration.range.end}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}
