import esbuild from "esbuild";

await esbuild.build({
  bundle: true,
  entryPoints: ["src/extension.ts"],
  external: ["vscode"],
  format: "cjs",
  logLevel: "info",
  minify: false,
  outfile: "dist/extension.js",
  platform: "node",
  sourcemap: false,
  target: "node18",
});
