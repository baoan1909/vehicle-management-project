/** Report actual API observations; reviewed semantic grades are supplied separately. */
import { createHash } from "node:crypto";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const readJson = file => JSON.parse(readFileSync(file, "utf8").replace(/^\uFEFF/, ""));
export const answerHash = row => createHash("sha256").update(String(row.answer ?? "")).digest("hex");
const count = values => values.reduce((result, value) => { result[value] = (result[value] ?? 0) + 1; return result; }, {});

export function observedPath(row) {
  if ((row.modelHttp ?? []).some(call => call.operation === "GENERATION")) return "QWEN_GENERATION_ATTEMPT";
  if ((row.retrievalAudits ?? []).some(audit => audit.retrieval_method === "COMMON_FAQ")) return "COMMON_FAQ_PATH";
  if ((row.toolCalls ?? []).some(call => call.executed_at)) return "BACKEND_TOOL_EXECUTED";
  if (row.retrievalAudits?.length) return "RETRIEVAL_WITHOUT_GENERATION";
  return row.diagnosticCode ? `DIRECT_${row.diagnosticCode}` : "DIRECT_RESPONSE_OR_UNCLASSIFIED";
}

function latency(rows) {
  const values = rows.map(row => row.elapsedMs).filter(value => typeof value === "number").sort((a, b) => a - b);
  if (!values.length) return { count: 0 };
  const middle = Math.floor(values.length / 2);
  return { count: values.length, p50Ms: values.length % 2 ? values[middle] : (values[middle - 1] + values[middle]) / 2,
    p95Ms: values[Math.ceil(values.length * 0.95) - 1], maxMs: values.at(-1),
    sampleCaveat: values.length < 20 ? "Descriptive only; fewer than20 samples" : null };
}

export function summarize(rows, plannedCount, grades = {}) {
  if (plannedCount < rows.length) throw new Error("Recorded count exceeds frozen planned denominator");
  const ids = rows.map(row => row.case.caseId);
  if (new Set(ids).size !== ids.length) throw new Error("Duplicate case ID in cohort");
  if (Object.keys(grades).some(id => !ids.includes(id))) throw new Error("Grade IDs outside actual cohort");
  const details = rows.map(row => {
    const caseId = row.case.caseId, grade = grades[caseId];
    if (grade && (grade.answerSha256 !== answerHash(row) || !["PASS", "FAIL", "INCOMPLETE"].includes(grade.verdict) || !grade.reason)) {
      throw new Error(`Stale or incomplete semantic grade: ${caseId}`);
    }
    return { caseId, document: row.case.documentSlug, variant: row.case.variantType, persona: row.case.persona,
      question: row.case.question, answer: row.answer, answerSha256: answerHash(row), observedPath: observedPath(row),
      actualIntent: row.actualIntent ?? null, expectedIntent: row.case.expectedIntent,
      transportCompleted: row.transportCompleted === true, infrastructureFailure: row.infrastructureFailure === true,
      semanticGrade: grade?.verdict ?? "NOT_GRADED", reason: grade?.reason ?? "Await qualitative review of actual API output",
      sourceEvidence: row.sourceEvidence ?? [], elapsedMs: row.elapsedMs };
  });
  const semanticCounts = count(details.map(row => row.semanticGrade));
  const graded = details.filter(row => row.semanticGrade !== "NOT_GRADED").length;
  return { kind: "ACTUAL_CHATBOX_API_EVIDENCE_SUMMARY", qualityAccepted: false, plannedCases: plannedCount,
    recordedCases: rows.length, notRunCases: plannedCount - rows.length,
    transportCompleted: details.filter(row => row.transportCompleted).length,
    infrastructureFailures: details.filter(row => row.infrastructureFailure).length,
    semanticCounts, semanticallyGradedCases: graded, notSemanticallyGradedCases: plannedCount - graded,
    observedPaths: count(details.map(row => row.observedPath)), apiLatency: latency(rows),
    latencyByPath: Object.fromEntries([...new Set(rows.map(observedPath))].sort().map(kind => [kind, latency(rows.filter(row => observedPath(row) === kind))])),
    googleHttpObserved: rows.flatMap(row => row.modelHttp ?? []).filter(call => call.provider === "GEMINI").length,
    routeAccuracy: null, retrievalHitAt5: null, citationAndPermissionAcceptance: null,
    note: "HTTP success and path evidence are not semantic PASS. Unrun/ungraded cases stay in the denominator; routing/source/permission gates require separate review.", details };
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const [run, flag, gradeFile] = process.argv.slice(2);
    if (!run || (flag && (flag !== "--grades" || !gradeFile))) throw new Error("Usage: node scripts/ai-knowledge/summarize-qwen-api.mjs RUN [--grades GRADES.json]");
    const metadata = readJson(path.join(run, "metadata.json")), transcript = path.join(run, "api-observations.json");
    const rows = existsSync(transcript) ? readJson(transcript) : [];
    const result = summarize(rows, metadata.selection.caseIds.length, gradeFile ? readJson(gradeFile) : {});
    writeFileSync(path.join(run, "api-summary.json"), JSON.stringify(result, null, 2) + "\n", "utf8");
    const fields = ["caseId", "document", "variant", "persona", "question", "answer", "observedPath", "semanticGrade", "reason", "elapsedMs"];
    const cell = value => {
      let text = String(value ?? "");
      if (/^[=+@-]/.test(text)) text = "'" + text;
      return '"' + text.replaceAll('"', '""') + '"';
    };
    const csv = [fields.join(","), ...result.details.map(row => fields.map(field => cell(row[field])).join(","))].join("\r\n") + "\r\n";
    writeFileSync(path.join(run, "answers-and-grades.csv"), "\uFEFF" + csv, "utf8");
    const { details, ...summary } = result;
    console.log(JSON.stringify(summary, null, 2));
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
