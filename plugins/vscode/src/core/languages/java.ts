import {
  braceContainers,
  directOwnerAt,
  isCodeOffset,
  SourceDeclaration,
  sourceDeclaration,
} from "../source";

const CLASS_PATTERN = /^\s*(?:public\s+)?(?:final\s+)?class\s+([A-Za-z_][A-Za-z0-9_]*)\b[^{;]*\{/gm;
const ANNOTATED_FIELD_PATTERN =
  /@JsonProperty\("([^"]+)"\)\s+(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)/gs;
const FIELD_PATTERN =
  /^\s*(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)/gm;

export function javaDeclarations(source: string): readonly SourceDeclaration[] {
  const blocks = braceContainers(source, CLASS_PATTERN);
  const annotated: SourceDeclaration[] = [];
  for (const match of source.matchAll(ANNOTATED_FIELD_PATTERN)) {
    const start = match.index;
    const alias = match[1];
    const label = match[2];
    if (start === undefined || alias === undefined || label === undefined) {
      continue;
    }
    const offset = start + match[0].lastIndexOf(label);
    const owner = directOwnerAt(source, blocks, offset);
    if (!isCodeOffset(source, offset) || owner === undefined) {
      continue;
    }
    annotated.push(sourceDeclaration(source, label, "field", offset, owner.label, [alias]));
  }

  const annotatedOffsets = new Set(annotated.map((field) => field.range.start));
  const fields: SourceDeclaration[] = [];
  for (const match of source.matchAll(FIELD_PATTERN)) {
    const start = match.index;
    const label = match[1];
    if (start === undefined || label === undefined) {
      continue;
    }
    const offset = start + match[0].lastIndexOf(label);
    const owner = directOwnerAt(source, blocks, offset);
    if (annotatedOffsets.has(offset) || !isCodeOffset(source, offset) || owner === undefined) {
      continue;
    }
    fields.push(sourceDeclaration(source, label, "field", offset, owner.label));
  }

  return [...blocks.map((block) => block.declaration), ...annotated, ...fields];
}
