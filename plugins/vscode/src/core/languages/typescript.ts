import {
  braceContainers,
  directOwnerAt,
  isCodeOffset,
  SourceDeclaration,
  sourceDeclaration,
} from "../source";

const DECLARATION_PATTERNS = [
  /^\s*(?:export\s+)?(?:interface|class)\s+([A-Za-z_][A-Za-z0-9_]*)\b[^{;]*\{/gm,
  /^\s*(?:export\s+)?type\s+([A-Za-z_][A-Za-z0-9_]*)\b[^=;]*=\s*\{/gm,
];
const PROPERTY_PATTERN = /^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z_][A-Za-z0-9_]*))\??\s*:/gm;

export function typeScriptDeclarations(source: string): readonly SourceDeclaration[] {
  const blocks = DECLARATION_PATTERNS.flatMap((pattern) => braceContainers(source, pattern)).sort(
    (left, right) => left.declaration.range.start - right.declaration.range.start,
  );
  const fields: SourceDeclaration[] = [];
  for (const match of source.matchAll(PROPERTY_PATTERN)) {
    const start = match.index;
    const label = match[1] ?? match[2] ?? match[3];
    if (start === undefined || label === undefined) {
      continue;
    }
    const offset = start + match[0].indexOf(label);
    const firstCodeCharacter = match[0].search(/\S/);
    const declarationOffset = start + firstCodeCharacter;
    const owner = directOwnerAt(source, blocks, declarationOffset);
    if (firstCodeCharacter < 0 || !isCodeOffset(source, declarationOffset) || owner === undefined) {
      continue;
    }
    fields.push(sourceDeclaration(source, label, "field", offset, owner.label));
  }
  return [...blocks.map((block) => block.declaration), ...fields];
}
