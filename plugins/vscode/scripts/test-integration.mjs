import { spawnSync } from "node:child_process";

const testRunner = "out-test/test/runTest.js";
const command = process.platform === "linux" ? "xvfb-run" : process.execPath;
const args = process.platform === "linux" ? ["-a", process.execPath, testRunner] : [testRunner];
const result = spawnSync(command, args, { stdio: "inherit" });

if (result.error) {
  throw result.error;
}
process.exitCode = result.status ?? 1;
