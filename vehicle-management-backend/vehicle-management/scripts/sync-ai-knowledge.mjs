/** Canonical docs -> upload-safe Markdown and manifest; no database or provider access. */
import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const backend = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const read = file => readFileSync(file, "utf8").replace(/^\uFEFF/, "").replace(/\r\n/g, "\n");
const json = file => JSON.parse(read(file));
const digest = file => createHash("sha256").update(readFileSync(file)).digest("hex");
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
const files = directory => readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
  const file = path.join(directory, entry.name);
  return entry.isDirectory() ? files(file) : entry.isFile() ? [file] : [];
});

export function metadata(text) {
  const match = /^---\n([\s\S]*?)\n---(?:\n|$)([\s\S]*)$/.exec(text.replace(/\r\n/g, "\n"));
  if (!match) throw new Error("Missing YAML frontmatter");
  const header = match[1].trim();
  const values = Object.fromEntries(header.split("\n").filter(line => line.includes(": ")).map(line => {
    const separator = line.indexOf(": ");
    return [line.slice(0, separator), line.slice(separator + 2)];
  }));
  return { values, header, body: match[2].trimStart() };
}

export function exportBody(source, destination, text) {
  let hidden = false, visible = "";
  for (const part of text.split(/(<!-- authoring-only:start -->|<!-- authoring-only:end -->)/)) {
    if (part === "<!-- authoring-only:start -->") {
      if (hidden) throw new Error(`Nested authoring-only block: ${path.basename(source)}`);
      hidden = true;
    } else if (part === "<!-- authoring-only:end -->") {
      if (!hidden) throw new Error(`Unexpected authoring-only end: ${path.basename(source)}`);
      hidden = false;
    } else if (!hidden) visible += part;
  }
  if (hidden) throw new Error(`Unclosed authoring-only block: ${path.basename(source)}`);
  return visible.replace(/(\[[^\]]+\]\()([^)]+)(\))/g, (match, open, target, close) => {
    if (/^(?:https?:|mailto:|#)/.test(target)) return match;
    const marker = target.indexOf("#");
    const file = marker < 0 ? target : target.slice(0, marker);
    if (!file) return match;
    const resolved = path.resolve(path.dirname(source), decodeURIComponent(file));
    const relative = path.relative(path.dirname(destination), resolved).replaceAll(path.sep, "/");
    return open + relative + (marker < 0 ? "" : target.slice(marker)) + close;
  });
}

export function syncKnowledge({ write = false, phase = path.join(backend, "docs/ai-knowledge/phase-0/authoring"), exports = path.join(backend, "docs/ai-knowledge/phase-0/knowledge") } = {}) {
  const config = json(path.join(phase, "knowledge-batches.json"));
  const batchBySlug = new Map(), errors = [], dates = [], documents = [];
  for (const batch of config.batches) for (const slug of batch.documentSlugs) {
    if (batchBySlug.has(slug)) throw new Error(`Document belongs to multiple batches: ${slug}`);
    batchBySlug.set(slug, batch.batch);
  }
  if (write) mkdirSync(exports, { recursive: true });
  const knowledge = path.join(phase, "knowledge"), evaluation = path.join(phase, "evaluation");
  for (const name of readdirSync(knowledge).filter(name => name.endsWith(".md")).sort()) {
    const source = path.join(knowledge, name), exported = path.join(exports, name);
    const { values, header, body } = metadata(read(source));
    if (values.lastVerifiedAt) dates.push(values.lastVerifiedAt);
    const expected = `<!--\n${header}\n-->\n\n# ${values.title}\n\n${exportBody(source, exported, body)}`.trimEnd() + "\n";
    if (!existsSync(exported) || read(exported).trimEnd() !== expected.trimEnd()) {
      if (write) writeFileSync(exported, expected, "utf8");
      else errors.push(`Import copy differs: ${name}`);
    }
    documents.push({ slug: values.slug, file: name, accessScope: values.accessScope,
      documentVersion: values.documentVersion, status: values.status, batch: batchBySlug.get(values.slug) ?? 0,
      sourceSha256: digest(source), exportSha256: existsSync(exported) ? digest(exported) : null });
  }
  const slugs = documents.map(row => row.slug);
  if (new Set(slugs).size !== slugs.length) errors.push("Duplicate document slug");
  for (const slug of batchBySlug.keys()) if (!slugs.includes(slug)) errors.push(`Configured document is missing: ${slug}`);
  const datasets = [ ...config.baselineDatasets.map(row => ({ ...row })), ...config.batches.map(batch => ({ file: batch.dataset, kind: "batch", batch: batch.batch, expectedCount: batch.scenarioCount * batch.variantsPerScenario })) ];
  for (const dataset of datasets) dataset.sha256 = digest(path.join(evaluation, dataset.file));
  const manifest = { schemaVersion: 2, verifiedAt: dates.sort().at(-1) ?? "UNKNOWN",
    sourceRoot: "docs/ai-knowledge/phase-0/authoring/knowledge", exportRoot: "docs/ai-knowledge/phase-0/knowledge", documents, datasets };
  const manifestFile = path.join(phase, "knowledge-manifest.json");
  if (write) writeFileSync(manifestFile, JSON.stringify(manifest, null, 2) + "\n", "utf8");
  else if (!existsSync(manifestFile) || !same(json(manifestFile), manifest)) errors.push("Manifest/checksum differs; review changes then run --write");
  const questions = [];
  for (const dataset of datasets) {
    const rows = read(path.join(evaluation, dataset.file)).split("\n").filter(line => line.trim()).map(line => JSON.parse(line));
    if (rows.length !== dataset.expectedCount) errors.push(`Dataset count mismatch: ${dataset.file}`);
    questions.push(...rows);
    if (dataset.kind !== "batch") continue;
    const batch = config.batches.find(row => row.batch === dataset.batch), families = new Map();
    for (const row of rows) {
      const required = ["caseId", "scenarioId", "documentSlug", "question", "context", "expectedIntent", "expectedTool", "split", "requiredIdeas", "forbiddenBehavior", "requiredPermissions", "expectedDatabaseEffect"];
      if (required.some(key => !Object.hasOwn(row, key))) errors.push(`Missing batch fields: ${row.caseId ?? "?"}`);
      if (!batch.documentSlugs.includes(row.documentSlug)) errors.push(`Out of batch: ${row.caseId}`);
      if (row.expectedIntent === "WRITE_ACTION" && row.expectedTool && !row.confirmationRequired) errors.push("Write without confirmation");
      if (!families.has(row.scenarioId)) families.set(row.scenarioId, []);
      families.get(row.scenarioId).push(row);
    }
    if (families.size !== batch.scenarioCount) errors.push(`Scenario count mismatch: ${dataset.file}`);
    for (const [name, group] of families) {
      if (group.length !== batch.variantsPerScenario || new Set(group.map(row => row.question)).size !== batch.variantsPerScenario) errors.push(`Variant coverage: ${name}`);
      if (new Set(group.map(row => row.split)).size !== 1) errors.push(`Scenario leaks between splits: ${name}`);
      if (group.some(row => !["regression", "acceptance"].includes(row.split))) errors.push(`Invalid split: ${name}`);
    }
    let scenarios = json(path.join(evaluation, batch.scenarios));
    if (!Array.isArray(scenarios)) scenarios = scenarios.scenarios;
    if (scenarios.length !== batch.scenarioCount) errors.push(`Scenario file count mismatch: ${batch.scenarios}`);
    const ids = scenarios.map(row => row.scenarioId);
    if (new Set(ids).size !== ids.length) errors.push(`Duplicate scenario ID: ${batch.scenarios}`);
    if (!same([...new Set(ids)].sort(), [...families.keys()].sort())) errors.push(`Scenario IDs differ from dataset: ${batch.scenarios}`);
    for (const scenario of scenarios) {
      const group = families.get(scenario.scenarioId) ?? [];
      if (!same(group.map(row => row.question), scenario.questions)) errors.push(`Scenario questions differ from dataset: ${scenario.scenarioId}`);
      if (group.some(row => row.documentSlug !== scenario.documentSlug || row.expectedIntent !== scenario.expectedIntent)) errors.push(`Scenario expectations differ from dataset: ${scenario.scenarioId}`);
    }
  }
  if (new Set(questions.map(row => row.caseId)).size !== questions.length) errors.push("Duplicate caseId across datasets");
  let localLinks = 0;
  const markdown = [...files(phase).filter(file => file.endsWith(".md")), ...(existsSync(exports) ? files(exports).filter(file => file.endsWith(".md")) : [])];
  for (const document of markdown) for (const match of read(document).matchAll(/\[[^\]]+\]\(([^)]+)\)/g)) {
    if (/^(?:https?:|mailto:|#)/.test(match[1])) continue;
    const target = decodeURIComponent(match[1].split("#", 1)[0]);
    if (!target) continue;
    localLinks++;
    if (!existsSync(path.resolve(path.dirname(document), target))) errors.push(`Broken local link: ${path.relative(phase, document)} -> ${target}`);
  }
  const reviewPath = path.join(evaluation, "legacy-120-review.json");
  if (existsSync(reviewPath)) {
    const review = json(reviewPath);
    if (review.length !== 120 || new Set(review.map(row => row.caseId)).size !== 120) errors.push("Legacy review count/IDs mismatch");
  }
  return { knowledgeDocuments: documents.length, batchDocuments: batchBySlug.size, batches: config.batches.length,
    questions: questions.length, localLinks, errors, databaseTouched: false };
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const arguments_ = process.argv.slice(2);
    if (arguments_.some(value => value !== "--write")) throw new Error("Usage: node scripts/sync-ai-knowledge.mjs [--write]");
    const result = syncKnowledge({ write: arguments_.includes("--write") });
    console.log(JSON.stringify(result));
    if (result.errors.length) process.exitCode = 1;
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
