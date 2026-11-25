package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.*;
import com.ban.vehicle_management.application.ai.service.KnowledgeDocumentChunker;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.infrastructure.ai.extraction.MarkdownDocumentExtractor;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Offline matcher/route evaluation of DRAFT authoring fixtures, never a publication or model acceptance. */
class CommonKnowledgeCorpusTest {
    private static final Path ROOT=Path.of("docs/ai-knowledge/phase-0/authoring");
    private static final Path OUTPUT=Path.of("target/common-knowledge-work/corpus-evaluation");
    private final ObjectMapper json=new ObjectMapper();
    private String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    @Test void evaluateAllExistingCasesWithoutProviderOrSyntheticVector() throws Exception {
        Files.createDirectories(OUTPUT);var manifest=json.readTree(ROOT.resolve("knowledge-manifest.json").toFile());
        Map<UUID,String> slugs=new HashMap<>(),scopes=new HashMap<>();List<KnowledgeSearchResult> sources=new ArrayList<>();
        Map<String,String> fileHashes=new TreeMap<>();
        for(var doc:manifest.path("documents")) {
            if(doc.path("batch").asInt()==0)continue;
            String slug=doc.path("slug").asText();UUID document=UUID.nameUUIDFromBytes(slug.getBytes(StandardCharsets.UTF_8));
            Path file=ROOT.resolve("knowledge").resolve(slug+".md");String text=Files.readString(file);
            fileHashes.put(slug+".md",hash(text));slugs.put(document,slug);scopes.put(document,doc.path("accessScope").asText());
            String title=text.lines().filter(l->l.startsWith("title: ")).findFirst().orElseThrow().substring(7);
            var extracted=new MarkdownDocumentExtractor().extract(Files.readAllBytes(file),"md","text/markdown");
            var chunker=new KnowledgeDocumentChunker();var blocks=chunker.createBlocks(document,2,extracted);
            for(var chunk:chunker.createChunks(document,2,title,blocks)) {
                var faqBlocks=blocks.stream().filter(block->block.getBlockIndex()>=chunk.getStartBlockIndex()
                        && block.getBlockIndex()<=chunk.getEndBlockIndex() && GroundedFaqLookup.isFaqSection(block.getSourceSection())).toList();
                if(faqBlocks.isEmpty())continue;
                UUID id=UUID.nameUUIDFromBytes((slug+"/"+chunk.getChunkIndex()).getBytes(StandardCharsets.UTF_8));
                String faqText=String.join("\n",faqBlocks.stream().map(block->block.getContent()).toList());
                var first=faqBlocks.getFirst();
                sources.add(new KnowledgeSearchResult(document,id,title,faqText,null,first.getSourcePage(),first.getSourceSection(),BigDecimal.ONE));
            }
        }
        List<Map<String,Object>> observations=new ArrayList<>();
        for(var dataset:manifest.path("datasets")) {
            if(!"batch".equals(dataset.path("kind").asText()))continue;
            Path file=ROOT.resolve("evaluation").resolve(dataset.path("file").asText());fileHashes.put(file.getFileName().toString(),hash(Files.readString(file)));
            for(String line:Files.readAllLines(file)) {
                if(line.isBlank())continue;
                var row=json.readTree(line);String question=row.path("question").asText();List<String> history=new ArrayList<>();
                for(var item:row.path("context"))history.add(item.asText());
                long tick=System.nanoTime();var resolved=AssistantQuestionResolver.resolve(question,history);
                var allowed=sources.stream().filter(s->"PUBLIC".equals(scopes.get(s.documentId()))
                        || (!"PUBLIC".equals(row.path("persona").asText())&&"CUSTOMER".equals(scopes.get(s.documentId())))).toList();
                var match=GroundedFaqLookup.findCommon(resolved.query(),allowed);var observation=new LinkedHashMap<String,Object>();
                observation.put("case",row);observation.put("resolvedQuery",resolved.query());observation.put("route",resolved.decision().intent().name());
                observation.put("routeMatchesExpected",row.path("expectedIntent").asText().equals(resolved.decision().intent().name()));
                observation.put("canonicalMatch",match.match()!=null);observation.put("conflict",match.conflict());
                observation.put("matcherMicros",(System.nanoTime()-tick)/1000);
                if(match.match()!=null) {
                    var answer=match.match();var source=allowed.stream().filter(s->s.chunkId().equals(answer.chunkId())).findFirst().orElseThrow();
                    observation.put("answer",answer.answer());observation.put("sourceSlug",slugs.get(source.documentId()));
                    observation.put("sourceContent",source.content());observation.put("sourceSection",source.sourceSection());
                    observation.put("expectedSourceMatched",row.path("documentSlug").asText().equals(slugs.get(source.documentId())));
                    var envelope=new AssistantResponseEnvelope(answer.answer(),List.of(
                            new AssistantResponseEnvelope.AssistantCitation("C1",source.chunkId(),source.documentId(),source.title(),source.sourcePage(),source.sourceSection())),List.of(),1.0,false);
                    var validation=AssistantOutputValidator.validate(envelope,new AssistantOutputValidator.ValidationContext(
                            Set.of("C1"),Set.of(source.chunkId()),question,List.of(source.content()),List.of(),false,true));
                    observation.put("outputValidatorPassed",validation.valid());observation.put("outputViolations",validation.violations());
                    List<String> missing=new ArrayList<>();String folded=VietnameseQueryNormalizer.fold(answer.answer());
                    for(var idea:row.path("requiredIdeas"))if(!folded.contains(VietnameseQueryNormalizer.fold(idea.asText())))missing.add(idea.asText());
                    observation.put("missingLiteralIdeas",missing);
                    assertTrue(GroundedFaqLookup.isFaqSection(source.sourceSection()));
                    assertTrue(source.content().contains(answer.answer()),"Canonical content must be verbatim source data");
                }
                observations.add(observation);
            }
        }
        json.writerWithDefaultPrettyPrinter().writeValue(OUTPUT.resolve("observations.json").toFile(),observations);
        var metadata=new LinkedHashMap<String,Object>();metadata.put("kind","OFFLINE_DRAFT_FIXTURE_MATCHER_AND_ROUTE");
        metadata.put("noProviderHttp",true);metadata.put("dbQueryEvaluated",false);metadata.put("qualityAccepted",false);
        metadata.put("note","In-memory scope fixture and domain matching only. No production publication, SQL ranking, synthesis, or provider acceptance is inferred.");
        metadata.put("sourceHashes",fileHashes);metadata.put("documents",slugs.size());metadata.put("faqChunkProjections",sources.size());metadata.put("cases",observations.size());
        json.writerWithDefaultPrettyPrinter().writeValue(OUTPUT.resolve("metadata.json").toFile(),metadata);
        assertEquals(java.util.stream.StreamSupport.stream(manifest.path("datasets").spliterator(),false).filter(d->"batch".equals(d.path("kind").asText())).mapToInt(d->d.path("expectedCount").asInt()).sum(),observations.size());
    }
}
