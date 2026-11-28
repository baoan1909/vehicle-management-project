import { createHash } from 'node:crypto';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { syncKnowledge, metadata } from '../sync-ai-knowledge.mjs';

const backend = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const readJson = file => JSON.parse(readFileSync(file,'utf8').replace(/^\uFEFF/,''));
const rows = file => readFileSync(file,'utf8').trim().split(/\r?\n/).map(JSON.parse);
export function validateCase(row) {
  const required = ['caseId','scenarioId','question','variantType','documentSlug','referenceAnswer','requiredIdeas',
    'requiredPermissions','expectedAccessScope','expectedIntent','expectedTool','forbiddenBehavior','codeReferences'];
  const errors = required.filter(key => !Object.hasOwn(row,key)).map(key => `Missing ${key}`);
  if (row.expectedTool && row.expectedDatabaseEffect !== 'NONE' && !row.confirmationRequired) errors.push('Mutation requires confirmation');
  if (!Array.isArray(row.requiredIdeas) || !row.requiredIdeas.length) errors.push('Missing semantic ideas');
  if (!Array.isArray(row.codeReferences) || !row.codeReferences.length) errors.push('Missing code provenance');
  return errors;
}
export function validateBatch5() {
  const phase=path.join(backend,'docs/ai-knowledge/phase-0/authoring'), evaluation=path.join(phase,'evaluation');
  const errors=[...syncKnowledge().errors];
  const original=readJson(path.join(evaluation,'evidence/batch-05/baseline-inventory.json'));
  for (const file of original.files) if (sha(readFileSync(path.join(backend,file.path)))!==file.sha256) errors.push(`Historical fixture changed: ${file.path}`);
  const main=rows(path.join(evaluation,'batch-05-questions.jsonl'));
  const holdoutFile=path.join(evaluation,'batch-05-holdout.jsonl'), holdout=rows(holdoutFile);
  const frozen=readJson(path.join(evaluation,'batch-05-holdout-freeze.json'));
  const frozenHash=frozen.sha256 ?? frozen.holdoutSha256 ?? frozen.datasetSha256;
  if (sha(readFileSync(holdoutFile))!==frozenHash) errors.push('Holdout freeze checksum differs');
  if (main.length!==240 || holdout.length!==40) errors.push('Cohort count differs');
  const topics=Object.fromEntries(['REGISTRATION','WORKFORCE','TOPOLOGY','CATALOG'].map(t=>[t,main.filter(r=>r.topic===t).length]));
  if (Object.values(topics).some(n=>n!==60)) errors.push('Need 60 main cases per topic');
  if (new Set([...main,...holdout].map(r=>r.caseId)).size!==280) errors.push('Duplicate main/holdout ID');
  for (const row of main) {
    errors.push(...validateCase(row).map(e=>`${row.caseId}: ${e}`));
    for (const source of row.codeReferences) if (!existsSync(path.join(backend,source.path))) errors.push(`Missing code: ${source.path}`);
  }
  const config=readJson(path.join(phase,'knowledge-batches.json')).batches.find(b=>b.batch===5);
  for (const slug of config.documentSlugs) {
    const file=path.join(phase,'knowledge',slug+'.md'), text=readFileSync(file,'utf8');
    const values=metadata(text).values;
    for (const field of ['title','slug','documentVersion','status','language','accessScope','ownerPermission','sourceOfTruth','effectiveFrom','reviewAt','lastVerifiedAt'])
      if (!Object.hasOwn(values,field)) errors.push(`${slug}: missing metadata ${field}`);
    if (values.status!=='DRAFT') errors.push(`${slug}: publication not authorized`);
    const expectedScope=slug==='partner-registration-and-organization'?'PUBLIC':'EMPLOYEE';
    if (values.accessScope!==expectedScope) errors.push(`${slug}: incorrect scope`);
    const exported=readFileSync(path.join(backend,'docs/ai-knowledge/phase-0/knowledge',slug+'.md'),'utf8');
    if (/B5-(?:H|REG|EMP|LAY|CAT)|caseId|referenceAnswer|sourceSection.*requiredIdeas/.test(exported)) errors.push(`${slug}: evaluation data leaked into upload source`);
  }
  return {baselineDocuments:original.documents,baselineQuestions:original.baselineAndBatches1To4,additionalSeparate:24,
    documents:syncKnowledge().knowledgeDocuments,mainCases:main.length,holdoutCases:holdout.length,topics,
    historicalHashesUnchanged:!errors.some(e=>e.startsWith('Historical')),holdoutFrozen:sha(readFileSync(holdoutFile))===frozenHash,
    semanticAcceptance:false,googleAcceptance:false,errors};
}
if (process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  const result=validateBatch5();console.log(JSON.stringify(result));if(result.errors.length)process.exitCode=1;
}
