export interface FilePathReference {
  readonly lookupValue: string;
  readonly rawValue: string;
}

export function classifyFilePath(rawValue: string): FilePathReference | undefined {
  if (rawValue.length === 0 || [...rawValue].some((character) => /\s/.test(character))) {
    return undefined;
  }
  if (!rawValue.includes("/")) {
    return undefined;
  }
  const lookupValue = rawValue.startsWith("./") ? rawValue.slice(2) : rawValue;
  const segments = lookupValue.split("/");
  if (segments.some((segment) => segment.length === 0 || segment === "." || segment === "..")) {
    return undefined;
  }
  if (!segments.slice(1).some(hasMeaningfulPathSegment)) {
    return undefined;
  }
  return { lookupValue, rawValue };
}

function hasMeaningfulPathSegment(segment: string): boolean {
  return hasDotExtension(segment) || /[A-Za-z]/.test(segment);
}

function hasDotExtension(segment: string): boolean {
  const dotIndex = segment.lastIndexOf(".");
  return (
    dotIndex > 0 &&
    dotIndex < segment.length - 1 &&
    /^[A-Za-z]+$/.test(segment.slice(dotIndex + 1))
  );
}
