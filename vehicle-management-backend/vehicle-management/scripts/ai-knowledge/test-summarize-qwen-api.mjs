import assert from "node:assert/strict";
import test from "node:test";
import { answerHash, observedPath, summarize } from "./summarize-qwen-api.mjs";

const row = (id = "a") => ({ case: { caseId: id, question: "Hỏi", expectedIntent: "STATIC_KNOWLEDGE" }, answer: "Trả lời", elapsedMs: 20, transportCompleted: true });

test("HTTP200 never becomes semantic PASS; unrun cases remain", () => {
  const result = summarize([row()], 10);
  assert.equal(result.transportCompleted, 1); assert.equal(result.notRunCases, 9);
  assert.equal(result.notSemanticallyGradedCases, 10); assert.deepEqual(result.semanticCounts, { NOT_GRADED: 1 });
  assert.equal(result.routeAccuracy, null); assert.equal(result.qualityAccepted, false);
});

test("paths use actual HTTP and persistence evidence", () => {
  const faq = { ...row(), retrievalAudits: [{ retrieval_method: "COMMON_FAQ" }] };
  assert.equal(observedPath(faq), "COMMON_FAQ_PATH");
  assert.equal(observedPath({ ...faq, modelHttp: [{ operation: "GENERATION", http_status: 500 }] }), "QWEN_GENERATION_ATTEMPT");
  assert.equal(observedPath({ ...row(), toolCalls: [{ executed_at: "2026-11-25" }] }), "BACKEND_TOOL_EXECUTED");
});

test("reviewed semantic grades require the unchanged actual answer and reason", () => {
  const actual = row(), grades = { a: { answerSha256: answerHash(actual), verdict: "INCOMPLETE", reason: "Thiếu điều kiện ngày bắt đầu" } };
  const result = summarize([actual], 2, grades);
  assert.deepEqual(result.semanticCounts, { INCOMPLETE: 1 }); assert.equal(result.notSemanticallyGradedCases, 1);
  assert.throws(() => summarize([{ ...actual, answer: "Câu khác" }], 2, grades), /Stale/);
});

test("duplicate IDs, bad denominators and foreign grades are rejected", () => {
  assert.throws(() => summarize([row(), row()], 2), /Duplicate/);
  assert.throws(() => summarize([row()], 0), /denominator/);
  assert.throws(() => summarize([row()], 1, { other: { verdict: "PASS" } }), /outside/);
});
