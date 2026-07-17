/**
 * Trusted Meticulous asset upload for workflow_run contexts.
 *
 * The official upload-assets GitHub Action refuses workflow_run events, but the
 * underlying @alwaysmeticulous/remote-replay-launcher package supports upload
 * from any Node process. This script only uploads assets and triggers a test
 * run — it does not wait for / redispatch base builds.
 */
import { readFileSync } from "node:fs";
import { uploadAssetsAndTriggerTestRun } from "@alwaysmeticulous/remote-replay-launcher";

function requireEnv(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

/**
 * Prefer the PR base SHA from the upstream workflow_run payload when present.
 * Push / workflow_dispatch runs omit this and upload without an explicit base.
 */
function resolveBaseShaFromEvent() {
  const event = JSON.parse(readFileSync(requireEnv("GITHUB_EVENT_PATH"), "utf8"));
  const upstream = event.workflow_run;
  if (!upstream) {
    throw new Error("Expected workflow_run payload");
  }
  const pr = upstream.pull_requests?.[0];
  return pr?.base?.sha || undefined;
}

async function main() {
  const apiToken = requireEnv("METICULOUS_API_TOKEN");
  const appDirectory = requireEnv("APP_DIRECTORY");
  const commitSha = requireEnv("COMMIT_SHA");
  const baseSha = resolveBaseShaFromEvent();

  console.log(
    `Uploading assets from ${appDirectory} for commit ${commitSha}` +
      (baseSha ? ` (base ${baseSha})` : ""),
  );

  await uploadAssetsAndTriggerTestRun({
    apiToken,
    appDirectory,
    commitSha,
    baseSha,
    waitForBase: false,
  });

  console.log("Meticulous upload completed successfully");
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
});
