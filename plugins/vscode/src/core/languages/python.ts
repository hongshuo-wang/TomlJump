import { isCodeOffset, SourceDeclaration, sourceDeclaration } from "../source";

const CLASS_PATTERN = /^class[ \t]+([A-Za-z_][A-Za-z0-9_]*)[ \t]*(?:\([^)]*\))?[ \t]*:/gms;
const FIELD_PATTERN = /^([ \t]*)([A-Za-z_][A-Za-z0-9_]*)\s*(?::|=)/gm;
const TAB_WIDTH = 4;

interface PythonContainerBlock {
  readonly bodyEndOffset: number;
  readonly bodyStartOffset: number;
  readonly declaration: SourceDeclaration;
  readonly directBodyIndentation: number;
}

export function pythonDeclarations(source: string): readonly SourceDeclaration[] {
  const blocks = [...source.matchAll(CLASS_PATTERN)].flatMap((match) => {
    const block = pythonContainerBlock(source, match);
    return block === undefined ? [] : [block];
  });
  const fields: SourceDeclaration[] = [];
  for (const match of source.matchAll(FIELD_PATTERN)) {
    const start = match.index;
    const indentation = match[1];
    const label = match[2];
    if (start === undefined || indentation === undefined || label === undefined) {
      continue;
    }
    const offset = start + match[0].indexOf(label);
    if (!isCodeOffset(source, offset, true)) {
      continue;
    }
    const width = indentationWidth(indentation);
    const owner = blocks
      .filter((block) => offset >= block.bodyStartOffset && offset < block.bodyEndOffset)
      .filter((block) => width === block.directBodyIndentation)
      .sort(
        (left, right) =>
          left.bodyEndOffset - left.bodyStartOffset - (right.bodyEndOffset - right.bodyStartOffset),
      )[0];
    if (owner === undefined) {
      continue;
    }
    fields.push(sourceDeclaration(source, label, "field", offset, owner.declaration.label));
  }
  return [...blocks.map((block) => block.declaration), ...fields];
}

function pythonContainerBlock(source: string, match: RegExpMatchArray): PythonContainerBlock | undefined {
  const start = match.index;
  const label = match[1];
  if (start === undefined || label === undefined || !isCodeOffset(source, start, true)) {
    return undefined;
  }
  const headerEnd = start + match[0].length;
  const newline = source.indexOf("\n", headerEnd - 1);
  if (newline < 0) {
    return undefined;
  }
  const bodyStartOffset = newline + 1;
  let bodyEndOffset = source.length;
  let directBodyIndentation: number | undefined;
  let lineStart = bodyStartOffset;
  while (lineStart < source.length) {
    const nextNewline = source.indexOf("\n", lineStart);
    const lineEnd = nextNewline < 0 ? source.length : nextNewline;
    const line = source.slice(lineStart, lineEnd);
    const trimmed = line.trim();
    if (trimmed.length > 0 && !trimmed.startsWith("#")) {
      const indentation = indentationWidth(line.match(/^[ \t]*/)?.[0] ?? "");
      if (indentation <= 0) {
        bodyEndOffset = lineStart;
        break;
      }
      directBodyIndentation = Math.min(directBodyIndentation ?? indentation, indentation);
    }
    lineStart = lineEnd + 1;
  }
  if (directBodyIndentation === undefined) {
    return undefined;
  }
  return {
    bodyEndOffset,
    bodyStartOffset,
    declaration: sourceDeclaration(source, label, "container", start),
    directBodyIndentation,
  };
}

function indentationWidth(indentation: string): number {
  return [...indentation].reduce((width, character) => width + (character === "\t" ? TAB_WIDTH : 1), 0);
}
