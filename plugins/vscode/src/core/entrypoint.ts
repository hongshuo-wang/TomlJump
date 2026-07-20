import { isCodeOffset } from "./source";
import { OffsetRange } from "./types";

export interface PyProjectEntryPoint {
  readonly member: string;
  readonly memberRange: OffsetRange;
  readonly qualifier: string;
  readonly qualifierRange: OffsetRange;
}

export function parsePyProjectEntryPoint(
  fileName: string,
  keyPath: readonly string[],
  value: string,
  valueRange: OffsetRange,
): PyProjectEntryPoint | undefined {
  if (fileName !== "pyproject.toml" || !samePath(keyPath.slice(0, -1), ["project", "scripts"])) {
    return undefined;
  }
  const separatorIndex = value.indexOf(":");
  if (
    separatorIndex <= 0 ||
    separatorIndex === value.length - 1 ||
    value.indexOf(":", separatorIndex + 1) >= 0
  ) {
    return undefined;
  }
  const qualifier = value.slice(0, separatorIndex);
  const member = value.slice(separatorIndex + 1);
  if (!qualifier.split(".").every(isIdentifier) || !isIdentifier(member)) {
    return undefined;
  }
  return {
    member,
    memberRange: {
      start: valueRange.start + separatorIndex + 1,
      end: valueRange.start + value.length,
    },
    qualifier,
    qualifierRange: {
      start: valueRange.start,
      end: valueRange.start + separatorIndex,
    },
  };
}

export function isPythonModulePath(filePath: string, qualifier: string): boolean {
  const normalized = filePath.replaceAll("\\", "/");
  const modulePath = qualifier.replaceAll(".", "/");
  return (
    normalized === `${modulePath}.py` ||
    normalized.endsWith(`/${modulePath}.py`) ||
    normalized === `${modulePath}/__init__.py` ||
    normalized.endsWith(`/${modulePath}/__init__.py`)
  );
}

export function findTopLevelPythonCallable(source: string, member: string): OffsetRange | undefined {
  const pattern = new RegExp(`^(?:async[ \\t]+)?def[ \\t]+(${escapeRegExp(member)})[ \\t]*\\(`, "gm");
  for (const match of source.matchAll(pattern)) {
    const start = match.index;
    if (start === undefined) {
      continue;
    }
    const offset = start + match[0].indexOf(member);
    if (isCodeOffset(source, offset, true)) {
      return { start: offset, end: offset + member.length };
    }
  }
  return undefined;
}

function isIdentifier(value: string): boolean {
  return /^[A-Za-z_][A-Za-z0-9_]*$/.test(value);
}

function samePath(left: readonly string[], right: readonly string[]): boolean {
  return left.length === right.length && left.every((segment, index) => segment === right[index]);
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
