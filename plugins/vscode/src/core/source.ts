import { OffsetRange } from "./types";

export interface SourceDeclaration {
  readonly aliases: readonly string[];
  readonly kind: "container" | "field";
  readonly label: string;
  readonly ownerLabel?: string;
  readonly range: OffsetRange;
}

export interface SourceContainerBlock {
  readonly closeBraceOffset: number;
  readonly declaration: SourceDeclaration;
  readonly openBraceOffset: number;
}

export type SourceDeclarationScanner = (source: string) => readonly SourceDeclaration[];

export function braceContainers(source: string, pattern: RegExp): SourceContainerBlock[] {
  const containers: SourceContainerBlock[] = [];
  for (const match of source.matchAll(pattern)) {
    const start = match.index;
    const label = match[1];
    if (start === undefined || label === undefined || braceDepthAt(source, 0, start) !== 0) {
      continue;
    }
    const relativeOpenBrace = match[0].lastIndexOf("{");
    const openBraceOffset = start + relativeOpenBrace;
    const closeBraceOffset = findMatchingBrace(source, openBraceOffset);
    if (relativeOpenBrace < 0 || closeBraceOffset === undefined) {
      continue;
    }
    containers.push({
      closeBraceOffset,
      declaration: sourceDeclaration(source, label, "container", start),
      openBraceOffset,
    });
  }
  return containers;
}

export function directOwnerAt(
  source: string,
  containers: readonly SourceContainerBlock[],
  offset: number,
): SourceDeclaration | undefined {
  return containers
    .filter((container) => offset > container.openBraceOffset && offset < container.closeBraceOffset)
    .filter((container) => braceDepthAt(source, container.openBraceOffset + 1, offset) === 0)
    .sort(
      (left, right) =>
        left.closeBraceOffset - left.openBraceOffset - (right.closeBraceOffset - right.openBraceOffset),
    )[0]?.declaration;
}

export function isCodeOffset(source: string, offset: number, hashLineComments = false): boolean {
  return braceDepthAt(source, 0, offset, hashLineComments) !== undefined;
}

export function sourceDeclaration(
  source: string,
  label: string,
  kind: SourceDeclaration["kind"],
  searchStart: number,
  ownerLabel?: string,
  aliases: readonly string[] = [],
  useLastOccurrence = false,
): SourceDeclaration {
  const relativeOffset = useLastOccurrence
    ? source.slice(searchStart).lastIndexOf(label)
    : source.indexOf(label, searchStart) - searchStart;
  const start = searchStart + relativeOffset;
  return {
    aliases,
    kind,
    label,
    ownerLabel,
    range: { start, end: start + label.length },
  };
}

function findMatchingBrace(source: string, openBraceOffset: number): number | undefined {
  if (source[openBraceOffset] !== "{") {
    return undefined;
  }
  let depth = 1;
  return scanCode(source, openBraceOffset + 1, source.length, false, (character) => {
    if (character === "{") {
      depth += 1;
    } else if (character === "}") {
      depth -= 1;
    }
    return depth === 0;
  }).stopOffset;
}

function braceDepthAt(
  source: string,
  startOffset: number,
  endOffset: number,
  hashLineComments = false,
): number | undefined {
  let depth = 0;
  const result = scanCode(source, startOffset, endOffset, hashLineComments, (character) => {
    if (character === "{") {
      depth += 1;
    } else if (character === "}") {
      depth -= 1;
    }
    return false;
  });
  return result.finalState === ScanState.Code ? depth : undefined;
}

interface ScanResult {
  readonly finalState: ScanState;
  readonly stopOffset?: number;
}

enum ScanState {
  Code,
  LineComment,
  BlockComment,
  SingleQuote,
  DoubleQuote,
  TripleSingleQuote,
  TripleDoubleQuote,
  Backtick,
}

function scanCode(
  source: string,
  startOffset: number,
  endOffset: number,
  hashLineComments: boolean,
  stopAfter: (character: string) => boolean,
): ScanResult {
  let state = ScanState.Code;
  let escaped = false;
  for (let index = startOffset; index < endOffset; index += 1) {
    const character = source[index];
    const next = source[index + 1];
    const nextNext = source[index + 2];
    if (character === undefined) {
      break;
    }
    switch (state) {
      case ScanState.Code:
        if (hashLineComments && character === "#") {
          state = ScanState.LineComment;
        } else if (character === "/" && next === "/") {
          state = ScanState.LineComment;
          index += 1;
        } else if (character === "/" && next === "*") {
          state = ScanState.BlockComment;
          index += 1;
        } else if (character === "'" && next === "'" && nextNext === "'") {
          state = ScanState.TripleSingleQuote;
          index += 2;
        } else if (character === '"' && next === '"' && nextNext === '"') {
          state = ScanState.TripleDoubleQuote;
          index += 2;
        } else if (character === "'") {
          state = ScanState.SingleQuote;
        } else if (character === '"') {
          state = ScanState.DoubleQuote;
        } else if (character === "`") {
          state = ScanState.Backtick;
        } else if (stopAfter(character)) {
          return { finalState: state, stopOffset: index };
        }
        break;
      case ScanState.LineComment:
        if (character === "\n") {
          state = ScanState.Code;
        }
        break;
      case ScanState.BlockComment:
        if (character === "*" && next === "/") {
          state = ScanState.Code;
          index += 1;
        }
        break;
      case ScanState.SingleQuote:
      case ScanState.DoubleQuote:
      case ScanState.Backtick: {
        const closing =
          state === ScanState.SingleQuote ? "'" : state === ScanState.DoubleQuote ? '"' : "`";
        if (escaped) {
          escaped = false;
        } else if (character === "\\") {
          escaped = true;
        } else if (character === closing) {
          state = ScanState.Code;
        }
        break;
      }
      case ScanState.TripleSingleQuote:
      case ScanState.TripleDoubleQuote: {
        const closing = state === ScanState.TripleSingleQuote ? "'" : '"';
        if (escaped) {
          escaped = false;
        } else if (character === "\\") {
          escaped = true;
        } else if (character === closing && next === closing && nextNext === closing) {
          state = ScanState.Code;
          index += 2;
        }
        break;
      }
    }
  }
  return { finalState: state };
}
