/** Preserve observed evidence and fixed denominators. This tool never awards semantic PASS. */
import { createHash } from 'node:crypto';
import { existsSync, readdirSync, readFileSync, writeFileSync, mkdirSync, copyFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateBatch5 } from './validate-batch05.mjs';
const backend=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const evaluation=path.join(backend,'docs/ai-knowledge/phase-0/authoring/evaluation');
const evidence=path.join(evaluation,'evidence/batch-05');
const runRoot=path.join(backend,'target/batch05-google-live');
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const json=file=>JSON.parse(readFileSync(file,'utf8').replace(/^\uFEFF/,''));
const lines=file=>readFileSync(file,'utf8').trim().split(/\r?\n/).map(JSON.parse);
const write=(file,value)=>writeFileSync(file,JSON.stringify(value,null,2)+'\n','utf8');
mkdirSync(evidence,{recursive:true});
const secrets=[];
const envFile=path.join(backend,'.env');
if(existsSync(envFile))for(const line of readFileSync(envFile,'utf8').split(/\r?\n/)){
  const m=/^\s*GEMINI_API_KEY(?:_\d+)?\s*=\s*(.+)$/.exec(line);
  if(m){const value=m[1].trim().replace(/^["']|["']$/g,'');if(value.length>10)secrets.push(value);}
}
function safeCopy(from,to){
  let text=readFileSync(from,'utf8');
  for(const secret of secrets)text=text.replaceAll(secret,'[REDACTED]');
  writeFileSync(to,text,'utf8');
}
const runs=[];
if(existsSync(runRoot))for(const name of readdirSync(runRoot).filter(n=>n.startsWith('cohort-')).sort()){
  const input=path.join(runRoot,name), output=path.join(evidence,name);mkdirSync(output,{recursive:true});
  const checkpoint=existsSync(path.join(input,'checkpoint.json'))?json(path.join(input,'checkpoint.json')):null;
  const wire=existsSync(path.join(input,'wire-attempts.json'))?json(path.join(input,'wire-attempts.json')):[];
  const ingestion=existsSync(path.join(input,'ingestion.json'))?json(path.join(input,'ingestion.json')):[];
  const transcript=existsSync(path.join(input,'transcript.json'))?json(path.join(input,'transcript.json')):[];
  const files=[];
  for(const file of ['checkpoint.json','cohort.json','wire-attempts.json','credential-diagnostics.json','credential-failures.json','ingestion.json','transcript.json','approved-model-registry.json','catalog-preflight.json','model-catalog.json','api-security-checks.json','index-before-build.json','index-duplicate-check.json','index-active.json']){
    const from=path.join(input,file);if(!existsSync(from))continue;
    safeCopy(from,path.join(output,file));files.push({file,sha256:sha(readFileSync(path.join(output,file)))});
  }
  const log=path.join(input,'run.log');
  if(existsSync(log)){
    const raw=readFileSync(log), text=raw[0]===255&&raw[1]===254?raw.toString('utf16le'):raw.toString('utf8');
    const cooldowns=[...text.matchAll(/credential=(GEMINI_API_KEY(?:_\d+)?) category=(AUTH|DAILY_QUOTA|RATE_LIMIT|MODEL|TRANSIENT|REQUEST) operation=([A-Za-z]+) model=([A-Za-z0-9_.-]+) retryAt=([0-9TZ:+.\-]+|null)/g)].map(m=>({credentialVariable:m[1],kind:m[2],operation:m[3],model:m[4],retryAt:m[5]}));
    write(path.join(output,'cooldowns.json'),cooldowns);
  }
  if(existsSync(log))files.push({privateLog:path.relative(backend,log).replaceAll(path.sep,'/'),sha256:sha(readFileSync(log)),copiedToGit:false});
  if(existsSync(path.join(output,'independent-semantic-review.json')))files.push({file:'independent-semantic-review.json',sha256:sha(readFileSync(path.join(output,'independent-semantic-review.json')))});
  write(path.join(output,'artifact-manifest.json'),files);
  const category=x=>x.path.endsWith(':embedContent')?'embedding':x.path.endsWith(':generateContent')?'generation':'catalog';
  runs.push({cohort:name,status:checkpoint?.status??'NO_CHECKPOINT',stopReason:checkpoint?.stopReason??null,
    actualGoogleHttpRequests:wire.length,checkpointHttpCount:checkpoint?.actualHttpRequests??null,httpCountConsistent:checkpoint?.actualHttpRequests===wire.length,inFlightObservations:wire.every(x=>Object.hasOwn(x,'observationStatus'))?wire.filter(x=>x.observationStatus==='UPSTREAM_SEND_STARTED').length:null,wireByTask:Object.fromEntries(['catalog','embedding','generation'].map(k=>[k,wire.filter(x=>category(x)===k).length])),
    credentialsAttempted:[...new Set(wire.map(x=>x.credentialVariable))],documentsUploaded:ingestion.filter(x=>x.uploadHttpStatus===202||x.uploadHttpStatus===200).length,
    documentsPublished:ingestion.filter(x=>x.publishedStatus==='READY').length,completedCaseIds:checkpoint?.caseIdsCompleted??[],
    transcriptRows:transcript.length,files});
}
const current=runs.at(-1);
const lastTranscript=current&&existsSync(path.join(evidence,current.cohort,'transcript.json'))?json(path.join(evidence,current.cohort,'transcript.json')):[];
const observed=new Map(lastTranscript.map(r=>[r.caseId,r]));
const lastWire=current&&existsSync(path.join(evidence,current.cohort,'wire-attempts.json'))?json(path.join(evidence,current.cohort,'wire-attempts.json')):[];
const generationWire=lastWire.filter(r=>r.path.endsWith(':generateContent'));
const generationAliases=[...new Set(generationWire.map(r=>r.credentialVariable))];
const configured=current&&existsSync(path.join(evidence,current.cohort,'credential-diagnostics.json'))?json(path.join(evidence,current.cohort,'credential-diagnostics.json')).map(r=>r.variable):[];
const generationMissing=configured.filter(alias=>!generationAliases.includes(alias));
const generationStatuses=Object.fromEntries([...new Set(generationWire.map(r=>String(r.httpStatus??'NO_RESPONSE')))].map(status=>[status,generationWire.filter(r=>String(r.httpStatus??'NO_RESPONSE')===status).length]));
const deliveredGeneration=lastTranscript.filter(r=>r.executionStatus==='COMPLETED_UNGRADED'&&r.runs?.some(run=>['SUCCESS','SUCCEEDED','COMPLETED'].includes(run.status))).length;
const unitEvidence=existsSync(path.join(evidence,'java-tests.json'))?json(path.join(evidence,'java-tests.json')):null;
const datasets=[['LEGACY_1104','golden-questions.jsonl'],...['01','02','03','04'].map(n=>['LEGACY_1104',`batch-${n}-questions.jsonl`]),['BATCH5_MAIN_240','batch-05-questions.jsonl'],['ADDITIONAL_24','additional-evaluation.jsonl'],['BATCH5_HOLDOUT_40','batch-05-holdout.jsonl'],['GENERATION_PROBES_4','batch-05-generation-probes.jsonl']];
const results=[];
for(const [cohort,file]of datasets)for(const row of lines(path.join(evaluation,file))){
  const actual=observed.get(row.caseId),messages=actual?.messages??[];
  const answers=messages.filter(m=>m.relatedSchema==='ai'&&!m.senderAccountId).map(m=>m.content).filter(Boolean);
  const answer=answers.at(-1)??null;
  results.push({cohort,caseId:row.caseId,scenarioId:row.scenarioId??null,topic:row.topic??row.domain??null,
    documentSlug:row.documentSlug??row.expectedDocumentSlugs?.[0]??null,variantType:row.variantType??null,question:row.question,
    actor:row.actor??row.persona??null,permissions:row.requiredPermissions??row.requiredPermission??[],
    organizationContext:row.organizationContext??null,parkingLotContext:row.parkingLotContext??null,
    targetOrganization:row.targetOrganization??null,targetParkingLot:row.targetParkingLot??null,
    executionStatus:actual?.executionStatus??(actual?'EXECUTED_STATUS_UNVERIFIED':'NOT_RUN'),gradingStatus:'NOT_GRADED',
    actualAnswer:answer,answerSha256:answer?sha(Buffer.from(answer)):null,sourceVersion:actual?.citationResponse?.data??actual?.retrieval??null,citationResponse:actual?.citationResponse??null,actualFixturePermissions:actual?.actualFixturePermissions??null,
    route:actual?.retrieval?.map(x=>x.retrieval_method)??[],model:actual?.runs?.map(x=>x.model_id)??[],
    keyAliases:actual?.providerAttempts?.map(x=>x.credentialVariable)??[],httpStatus:actual?.httpStatus??null,latencyMs:actual?.latencyMs??null,
    score:null,reviewer:null,reason:actual?'Await independent semantic/source/permission review':current?.stopReason??'Not executed',
    goldReview:cohort==='BATCH5_HOLDOUT_40'?'INDEPENDENT_GOLD_ADJUDICATION_PENDING':'SOURCE_DERIVED_DRAFT'});
}
writeFileSync(path.join(evidence,'case-results.jsonl'),results.map(x=>JSON.stringify(x)).join('\n')+'\n');
const primary=results.filter(r=>r.cohort==='BATCH5_MAIN_240');
const grouped=field=>Object.fromEntries([...new Set(primary.map(r=>r[field]??'UNKNOWN'))].map(key=>{const group=primary.filter(r=>(r[field]??'UNKNOWN')===key);return[key,{planned:group.length,observed:group.filter(r=>r.executionStatus!=='NOT_RUN').length,completedUngraded:group.filter(r=>r.executionStatus==='COMPLETED_UNGRADED').length,notRun:group.filter(r=>r.executionStatus==='NOT_RUN').length,graded:0,pass:0,semanticScore:null,gate:'PENDING'}];}));
write(path.join(evidence,'coverage-by-topic.json'),{cohort:current?.cohort,groups:grouped('topic')});
write(path.join(evidence,'coverage-by-document.json'),{cohort:current?.cohort,groups:grouped('documentSlug')});
write(path.join(evidence,'coverage-by-language.json'),{cohort:current?.cohort,groups:grouped('variantType')});
write(path.join(evidence,'coverage-by-model.json'),{cohort:current?.cohort,models:[...new Set(results.flatMap(r=>r.model))].map(model=>({model,observedCases:results.filter(r=>r.model.includes(model)).map(r=>r.caseId),graded:0,semanticScore:null,gate:'PENDING'})),faqWithoutGeneration:lastTranscript.filter(r=>r.executionStatus==='COMPLETED_UNGRADED'&&!(r.runs?.length)).length,note:'FAQ and failed provider attempts do not demonstrate accepted generation quality.'});

const totals=Object.fromEntries([...new Set(results.map(r=>r.cohort))].map(c=>[c,{planned:results.filter(r=>r.cohort===c).length,observed:results.filter(r=>r.cohort===c&&r.executionStatus!=='NOT_RUN').length,graded:0,pass:0}]));
const pending=results.filter(r=>['BATCH5_MAIN_240','BATCH5_HOLDOUT_40'].includes(r.cohort)&&r.executionStatus==='NOT_RUN').map(r=>r.caseId);
writeFileSync(path.join(evidence,'not-run-case-ids.txt'),pending.join('\n')+'\n');
const validation=validateBatch5();write(path.join(evidence,'offline-validation.json'),validation);
write(path.join(evidence,'cohort-summary.json'),{runs,totals,semanticAcceptance:false,googleChatboxAcceptance:false});
if(!existsSync(path.join(evaluation,'batch-05-gold-review-status.json')))write(path.join(evaluation,'batch-05-gold-review-status.json'),{holdoutFrozen:true,holdoutEdited:false,usedForRuntimeTuning:false,author:'Independent AI agent',humanReviewer:null,status:'GOLD_REVIEW_REQUIRED',reason:'The independent author reported that a holdout expectation may rely on surviving organization-profile endpoints before checking the retirement migration. Keep the frozen dataset; adjudicate gold against code before grading. Do not count it as runtime failure or PASS without independent review.'});
const report=`# Báo cáo đợt 5 — 2026-11-27\n\nĐã tạo 5 nguồn DRAFT cho 4 chủ đề: đăng ký công khai và quản trị tổ chức nội bộ tách scope, cộng nhân sự/phân quyền, cấu trúc/sơ đồ và danh mục. Tổng kho 21 tài liệu, gồm nguyên 16 nguồn cũ. Bản xuất và manifest đã đồng bộ. Chưa nghiệm thu chất lượng Google chatbox.\n\n## Bộ dữ liệu và mẫu số\n\n| Tập | Dự kiến | Có transcript | Đã chấm |\n|---|---:|---:|---:|\n${Object.entries(totals).map(([k,v])=>`| ${k} | ${v.planned} | ${v.observed} | ${v.graded} |`).join('\n')}\n\nBốn đợt cũ 984 + nền 120 = 1.104 giữ nguyên hash. Tập chính 240 = 60/chủ đề; holdout 40 = 10/chủ đề; 24 bổ sung cũ và 4 probe generation giữ riêng. Không có tỷ lệ đúng toàn chatbot khi chưa chấm.\n\n## Kiểm tra offline và fallback\n\nCác lượt test Java phù hợp ghi nhận ${unitEvidence?.tests??"chưa tổng hợp"} test, ${unitEvidence?.failures??"?"} failure, ${unitEvidence?.errors??"?"} error; không phải toàn suite repository. 12 test Node đã qua. Xem [JUnit](evidence/batch-05/java-tests.json).\n\nValidation cấu trúc: ${validation.errors.length===0?'PASS':'FAIL'}, lịch sử giữ nguyên: ${validation.historicalHashesUnchanged}, holdout đúng hash freeze: ${validation.holdoutFrozen}. Đây không phải điểm ngữ nghĩa. Timeout Google mặc định đã tăng generation từ 4 lên 20 giây, provider plan từ 12 lên 60 giây sau bằng chứng đáp ứng khoảng 16 giây; .env cục bộ đổi đúng dòng generation timeout. Proxy observer mới không chặn nối đuôi và đóng băng count sau drain; vẫn chỉ một case/worker. Bộ HTTP giả lập kiểm tra thứ tự key/model có báo cáo riêng tại [fallback-report.md](evidence/batch-05/fallback-report.md).\n\n## Google live qua API\n\n| Cohort | Proxy observations Google | Catalog / embedding / generation | Upload / duyệt | Kết thúc |\n|---|---:|---|---|---|\n${runs.map(r=>`| ${r.cohort} | ${r.actualGoogleHttpRequests} | ${r.wireByTask.catalog} / ${r.wireByTask.embedding} / ${r.wireByTask.generation} | ${r.documentsUploaded} / ${r.documentsPublished} | ${r.status} |`).join('\n')}\n\nGeneration ở cohort cuối: ${generationWire.length} quan sát, HTTP/status ${JSON.stringify(generationStatuses)}, ${deliveredGeneration} phản hồi generation được giao theo run thành công. Aliases quan sát: ${generationAliases.join(", ")||"chưa có"}; chưa có quan sát generation: ${generationMissing.join(", ")||"không"}. Không suy hết quota hoặc đã thử hết mọi credential từ lỗi/deadline. Bộ đếm và budget đọc riêng từng cohort.\n\nĐiểm dừng cuối: ${current?.stopReason??'Chưa có checkpoint'}. Không suy quota còn lại. Số trong bảng là dòng quan sát proxy; đối chiếu riêng checkpointHttpCount/httpCountConsistent và in-flight observations. Cohort cũ có thể có proxy hoàn thành muộn sau checkpoint, không coi đó là tổng gửi cuối cùng. Lỗi harness, provider, cooldown và model được lưu theo từng cohort; không gộp các corpus/prompt/model khác nhau.\n\nHarness dùng HTTP socket thật, DB Testcontainers riêng, luồng upload/worker/preview/publish/index và chatbox thật. Chỉ thay bộ xác minh JWT bằng định danh test và kho MinIO bằng filesystem riêng; không mock router, retrieval, provider hay business ports. PUBLIC ở harness là tài khoản xác thực chỉ có quyền hỗ trợ và scope công khai; chưa chứng minh luồng khách ẩn danh.\n\n## Bảng từng case và bằng chứng\n\n[Case results](evidence/batch-05/case-results.jsonl), [tổng hợp cohort](evidence/batch-05/cohort-summary.json), [theo chủ đề](evidence/batch-05/coverage-by-topic.json), [theo tài liệu](evidence/batch-05/coverage-by-document.json), [theo ngôn ngữ](evidence/batch-05/coverage-by-language.json), [theo model](evidence/batch-05/coverage-by-model.json), [kiểm kê gốc](evidence/batch-05/baseline-inventory.json), [validation](evidence/batch-05/offline-validation.json). Mỗi thư mục cohort có transcript, checkpoint, HTTP attempts và artifact hash. Log thô riêng nằm dưới target, chỉ giữ hash và đường dẫn, không đưa lên Git.\n\n## Gate còn thiếu\n\n- Hướng xử lý ≥98%, nguồn top 5 ≥95%, đủ ý ≥95% từng nguồn/nhóm ngôn ngữ: PENDING khi chưa đủ live và review.\n- Nguồn/phiên bản/quyền 100%, lộ dữ liệu/vượt quyền/thao tác ngoài ý muốn bằng 0: chưa công nhận chỉ từ câu hỏi an toàn. Bốn fixture API đã qua: đơn vị của mình 200, đơn vị khác 403, tài khoản công khai không quản trị tri thức 403, tài khoản DISABLED không mở hỗ trợ 403; tạo index khi có candidate trả 409. Chưa có ma trận đầy đủ theo actor/bãi và 240/40 case để công nhận toàn bộ gate; tám câu hoàn thành không làm đổi các bảng nghiệp vụ được lấy hash.\n- Migration hiện chỉ cấp AI_KNOWLEDGE_READ_EMPLOYEE mặc định cho EMPLOYEE/PARKING_MANAGER; không cấp mặc định cho PARTNER_ADMIN. Fixture live cấp quyền đọc này cho vai trò kiểm thử riêng để đo đường có quyền; không chứng minh đối tác mặc định có quyền đọc bốn nguồn nội bộ. Không tự mở scope hay cấp thêm quyền trên DB dùng chung; cần review chính sách cấp quyền trước phát hành.
- Holdout do AI khác biên soạn và đóng băng; human review PENDING. Có [ghi nhận cần adjudicate gold](batch-05-gold-review-status.json) từ tác giả; không sửa holdout, không dùng kết quả để tuning runtime.\n- Chất lượng từng model dự phòng và độ phủ generation đủ 4 chủ đề: chưa công nhận khi không có transcript đã chấm phù hợp.\n- Lịch sử hội thoại được dựng bằng tin nhắn fixture lưu thật trong DB ở harness mới; chỉ công nhận nhóm CONTEXTUAL sau khi có transcript đúng phiên bản và được chấm.\n\n## Thay đổi và chạy lại\n\nThêm nguồn, dataset, scenario, rubric, traceability, holdout, validator, trình tạo tài liệu và harness API. Bổ sung từ vựng miền quản trị đối tác và nhận diện câu thiếu thông tin trong AssistantIntentRouter; không cấp quyền hoặc thêm công cụ nghiệp vụ. Yêu cầu đọc/ghi dữ liệu quản trị chưa có tool được chặn trước khi chọn nhầm chức năng dành cho khách hàng. Tạo index mới khi đã có candidate dùng khóa đồng bộ hiện có và trả Conflict thay vì lỗi ràng buộc DB/HTTP 500. Corpus offline test dùng resolver scope thật theo quyền khai báo thay vì chỉ giả lập PUBLIC/CUSTOMER. Không khôi phục provider local, không sửa migration cũ hay dữ liệu dev, không commit/push.\n\nCollector citation API và freeze/drain proxy được bổ sung sau cohort 20261126T192655Z; xem artifact của cohort tương ứng trước khi công nhận collector live. Ở lần bàn giao này collector mới chỉ được biên dịch/kiểm tra offline. Cần cohort mới để kiểm source/version đầy đủ, không kế thừa PASS từ 8 FAQ còn thiếu bằng chứng citation.\n\nXem [phân tích code](batch-05-code-analysis.md), [runbook DB sạch](batch-05-runbook.md) và [review nội dung độc lập](batch-05-independent-review.md).\n`;
writeFileSync(path.join(evaluation,'batch-05-report.md'),report,'utf8');
console.log(JSON.stringify({cohorts:runs.length,totals,lastStop:current?.stopReason??null,semanticAcceptance:false}));
