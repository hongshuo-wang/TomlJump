import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    exclude: ["src/test/suite/**"],
    include: ["src/**/*.test.ts"],
  },
});
