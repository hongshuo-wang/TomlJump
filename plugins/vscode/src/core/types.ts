export interface OffsetRange {
  readonly start: number;
  readonly end: number;
}

export interface TomlNavigationTarget {
  readonly kind: "table" | "key";
  readonly path: readonly string[];
  readonly range: OffsetRange;
}

export interface TomlStringTarget {
  readonly keyPath: readonly string[];
  readonly range: OffsetRange;
  readonly value: string;
}

export interface ParsedTomlDocument {
  readonly navigationTargets: readonly TomlNavigationTarget[];
  readonly stringTargets: readonly TomlStringTarget[];
}
