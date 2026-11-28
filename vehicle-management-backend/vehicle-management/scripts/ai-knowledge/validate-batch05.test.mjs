import test from 'node:test';
import assert from 'node:assert/strict';
import { validateCase, validateBatch5 } from './validate-batch05.mjs';
test('requires source-derived semantic expectations and permissions instead of accepting a question alone',()=>{
  const errors=validateCase({caseId:'incomplete',question:'test'});
  assert.ok(errors.includes('Missing requiredPermissions'));
  assert.ok(errors.includes('Missing semantic ideas'));
  assert.ok(errors.includes('Missing code provenance'));
});
test('rejects an unconfirmed mutation rather than silently converting it to a read',()=>{
  const errors=validateCase({expectedTool:'write_tool',expectedDatabaseEffect:'UPDATE',confirmationRequired:false});
  assert.ok(errors.includes('Mutation requires confirmation'));
});
test('checks immutable historical fixtures, separate frozen holdout and public/internal export boundaries',()=>{
  const result=validateBatch5();assert.deepEqual(result.errors,[]);assert.equal(result.baselineQuestions,1104);
  assert.equal(result.mainCases,240);assert.equal(result.holdoutCases,40);assert.equal(result.holdoutFrozen,true);
});
