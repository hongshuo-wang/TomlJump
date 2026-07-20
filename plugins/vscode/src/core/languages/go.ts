import {
  braceContainers,
  directOwnerAt,
  isCodeOffset,
  SourceDeclaration,
  sourceDeclaration,
} from "../source";

const TYPE_PATTERN = /^\s*type\s+([A-Za-z_][A-Za-z0-9_]*)\s+struct\s*\{/gm;
const TAG_PATTERN = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s+[^`\n]+`([^`]+)`/gm;
const FIELD_PATTERN = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s+[A-Za-z_][A-Za-z0-9_.*[\]]*/gm;
const ALIAS_PATTERN = /(?:toml|json|mapstructure|yaml):"([^",]+)"/g;

export function goDeclarations(source: string): readonly SourceDeclaration[] {
  const blocks = braceContainers(source, TYPE_PATTERN);
  const taggedFields: SourceDeclaration[] = [];
  for (const match of source.matchAll(TAG_PATTERN)) {
    const start = match.index;
    const label = match[1];
    const tagText = match[2];
    if (start === undefined || label === undefined || tagText === undefined) {
      continue;
    }
    const offset = start + match[0].indexOf(label);
    const owner = directOwnerAt(source, blocks, offset);
    if (!isCodeOffset(source, offset) || owner === undefined) {
      continue;
    }
    const aliases = [...tagText.matchAll(ALIAS_PATTERN)].flatMap((alias) =>
      alias[1] === undefined ? [] : [alias[1]],
    );
    taggedFields.push(sourceDeclaration(source, label, "field", offset, owner.label, aliases));
  }

  const taggedOffsets = new Set(taggedFields.map((field) => field.range.start));
  const fields: SourceDeclaration[] = [];
  for (const match of source.matchAll(FIELD_PATTERN)) {
    const start = match.index;
    const label = match[1];
    if (start === undefined || label === undefined) {
      continue;
    }
    const offset = start + match[0].indexOf(label);
    const owner = directOwnerAt(source, blocks, offset);
    if (taggedOffsets.has(offset) || !isCodeOffset(source, offset) || owner === undefined) {
      continue;
    }
    fields.push(sourceDeclaration(source, label, "field", offset, owner.label));
  }

  return [...blocks.map((block) => block.declaration), ...taggedFields, ...fields];
}
