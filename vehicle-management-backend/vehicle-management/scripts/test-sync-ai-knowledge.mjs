import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { exportBody, syncKnowledge } from "./sync-ai-knowledge.mjs";

function fixture(t) {
  const root = mkdtempSync(path.join(os.tmpdir(), "vehicle-knowledge-sync-"));
  const tempRoot = path.resolve(os.tmpdir()) + path.sep;
  assert.ok(path.resolve(root).startsWith(tempRoot));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const phase = path.join(root, "authoring"), exports = path.join(root, "exports");
  const knowledge = path.join(phase, "knowledge"), evaluation = path.join(phase, "evaluation");
  mkdirSync(knowledge, { recursive: true }); mkdirSync(evaluation);
  const config = { baselineDatasets: [{ file: "baseline.jsonl", kind: "baseline", expectedCount: 1 }], batches: [] };
  const writeJson = (name, value) => writeFileSync(path.join(evaluation, name), JSON.stringify(value), "utf8");
  const writeRows = (name, rows) => writeFileSync(path.join(evaluation, name), rows.map(row => JSON.stringify(row)).join("\n") + "\n", "utf8");
  const save = () => writeFileSync(path.join(phase, "knowledge-batches.json"), JSON.stringify(config), "utf8");
  writeRows("baseline.jsonl", [{ caseId: "BASE-1" }]);
  function addBatch(number) {
    const slug = `document-${number}`, scenario = `B${number}`;
    writeFileSync(path.join(knowledge, `${slug}.md`), `---\ntitle: Document ${number}\nslug: ${slug}\naccessScope: CUSTOMER\ndocumentVersion: 2.0.0\nstatus: DRAFT\n---\n\nExample guidance.\n`, "utf8");
    const batch = { batch: number, documentSlugs: [slug], dataset: `batch-${number}.jsonl`, scenarios: `scenarios-${number}.json`, scenarioCount: 1, variantsPerScenario: 2 };
    config.batches.push(batch);
    const rows = [0, 1].map(variant => ({ caseId: `${scenario}-${variant}`, scenarioId: scenario, documentSlug: slug,
      question: `question ${number} variant ${variant}`, context: [], expectedIntent: "STATIC_KNOWLEDGE", expectedTool: null,
      confirmationRequired: false, split: "regression", requiredIdeas: ["guide"], forbiddenBehavior: ["private data"],
      requiredPermissions: [], expectedDatabaseEffect: "NONE" }));
    writeRows(batch.dataset, rows);
    writeJson(batch.scenarios, [{ scenarioId: scenario, documentSlug: slug, expectedIntent: "STATIC_KNOWLEDGE", questions: rows.map(row => row.question) }]);
    save(); return { batch, rows };
  }
  addBatch(1);
  return { phase, exports, knowledge, evaluation, config, addBatch, writeRows, writeJson, save,
    run: write => syncKnowledge({ phase, exports, write }) };
}

test("new batches preserve previous exports and validate without writing", t => {
  const f = fixture(t); assert.deepEqual(f.run(true).errors, []);
  const before = readFileSync(path.join(f.exports, "document-1.md"));
  f.addBatch(2); const result = f.run(true);
  assert.deepEqual(result.errors, []); assert.equal(result.questions, 5); assert.equal(result.databaseTouched, false);
  assert.deepEqual(readFileSync(path.join(f.exports, "document-1.md")), before);
  assert.deepEqual(f.run(false).errors, []);
});

test("authoring-only notes cannot leak into upload-safe knowledge", t => {
  const f = fixture(t), source = path.join(f.knowledge, "document-1.md");
  writeFileSync(source, readFileSync(source, "utf8") + "\n<!-- authoring-only:start -->\nInternal SOP\n<!-- authoring-only:end -->\nPublic FAQ.\n", "utf8");
  assert.deepEqual(f.run(true).errors, []);
  const exported = readFileSync(path.join(f.exports, "document-1.md"), "utf8");
  assert.ok(!exported.includes("Internal SOP")); assert.ok(exported.includes("Public FAQ."));
});

test("malformed authoring-only markers fail closed", () => {
  for (const text of ["<!-- authoring-only:start -->Secret", "<!-- authoring-only:end -->",
    "<!-- authoring-only:start --><!-- authoring-only:start --><!-- authoring-only:end -->"]) {
    assert.throws(() => exportBody("source.md", "export.md", text), /authoring-only/);
  }
});

test("relative evidence links remain valid after export", t => {
  const f = fixture(t), source = path.join(f.knowledge, "document-1.md");
  writeFileSync(path.join(f.evaluation, "proof.md"), "# Evidence\n", "utf8");
  writeFileSync(source, readFileSync(source, "utf8") + "\n[Evidence](../evaluation/proof.md)\n", "utf8");
  assert.deepEqual(f.run(true).errors, []);
  assert.match(readFileSync(path.join(f.exports, "document-1.md"), "utf8"), /\.\.\/authoring\/evaluation\/proof\.md/);
  assert.deepEqual(f.run(false).errors, []);
});

test("question changes require manifest review", t => {
  const f = fixture(t); f.run(true);
  const p = path.join(f.evaluation, "batch-1.jsonl");
  writeFileSync(p, readFileSync(p, "utf8").replace("question 1 variant 0", "changed question"), "utf8");
  assert.ok(f.run(false).errors.some(error => error.includes("Manifest/checksum")));
});

test("one scenario cannot leak across regression and acceptance splits", t => {
  const f = fixture(t), { batch, rows } = f.addBatch(2); rows[1].split = "acceptance";
  f.writeRows(batch.dataset, rows);
  assert.ok(f.run(true).errors.some(error => error.includes("leaks between splits")));
});

test("scenario questions must equal dataset questions", t => {
  const f = fixture(t); f.run(true);
  const p = path.join(f.evaluation, "scenarios-1.json");
  writeFileSync(p, readFileSync(p, "utf8").replace("question 1 variant 0", "different question"), "utf8");
  assert.ok(f.run(false).errors.some(error => error.includes("Scenario questions differ")));
});

test("case IDs remain unique across batches", t => {
  const f = fixture(t), { batch, rows } = f.addBatch(2); rows[0].caseId = "B1-0";
  f.writeRows(batch.dataset, rows);
  assert.ok(f.run(true).errors.includes("Duplicate caseId across datasets"));
});

test("write tools still require confirmation", t => {
  const f = fixture(t), { batch, rows } = f.addBatch(2);
  rows[0].expectedIntent = "WRITE_ACTION"; rows[0].expectedTool = "create_support_ticket";
  f.writeRows(batch.dataset, rows);
  assert.ok(f.run(true).errors.includes("Write without confirmation"));
});
