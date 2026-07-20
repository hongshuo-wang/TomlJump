import {
  braceContainers,
  directOwnerAt,
  isCodeOffset,
  SourceDeclaration,
  sourceDeclaration,
} from "../source";

const OBJECT_PATTERN = /^\s*(?:export\s+)?(?:const|let|var)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*\{/gm;
const PROPERTY_PATTERN = /^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z_][A-Za-z0-9_]*))\s*:/gm;

export function javaScriptDeclarations(source: string): readonly SourceDeclaration[] {
  const blocks = braceContainers(source, OBJECT_PATTERN);
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
