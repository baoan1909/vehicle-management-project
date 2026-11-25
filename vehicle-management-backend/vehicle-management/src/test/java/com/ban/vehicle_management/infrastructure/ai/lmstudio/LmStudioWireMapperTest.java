package com.ban.vehicle_management.infrastructure.ai.lmstudio;

import static org.junit.jupiter.api.Assertions.*;
import com.ban.vehicle_management.domain.ai.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class LmStudioWireMapperTest {
    private final ObjectMapper json = new ObjectMapper();
    private final LmStudioWireMapper mapper = new LmStudioWireMapper(json);
    @Test void disablesThinkingAndPreservesSchema() throws Exception {
        var config=new AiModelConfiguration();config.setModelId("qwen");
        var request=new AiRequest("safe",List.of(new AiRequestMessage("user","question")),true,
                "{\"type\":\"object\"}",List.of(),List.of(),List.of());
        var body=mapper.chat(request,config,512);
        assertFalse(body.path("chat_template_kwargs").path("enable_thinking").asBoolean(true));
        assertEquals(512,body.path("max_tokens").asInt());
        assertTrue(body.path("response_format").path("json_schema").path("schema").isObject());
    }
    @Test void rejectsTruncatedReasoningAndEmptyOutputs() {
        for(String body:List.of("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"partial\"}}]}",
                "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"<think>secret</think>answer\"}}]}",
                "{\"choices\":[]}","not json")) assertFalse(mapper.answer(200,body).success());
        assertFalse(mapper.answer(401,"secret").retryable());assertNull(mapper.answer(401,"secret").rawBody());
    }
    @Test void preservesSuccessfulUsage() {
        var result=mapper.answer(200,"{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"answer\"}}],\"usage\":{\"prompt_tokens\":45,\"completion_tokens\":12}}");
        assertTrue(result.success());assertEquals(45,result.inputTokens());assertEquals(12,result.outputTokens());
    }
    @Test void documentedMrlProjectionNormalizesFirst768Components() throws Exception {
        var root=json.createObjectNode();var values=root.putArray("data").addObject().putArray("embedding");
        for(int i=0;i<1024;i++)values.add(i==0?3:i==1?4:0);
        var result=mapper.embedding(200,json.writeValueAsString(root),"qwen-embedding",768,true);
        assertTrue(result.isSuccess());assertEquals(768,result.getVector().dimension());
        assertEquals(.6,result.getVector().values()[0],1e-12);assertEquals(.8,result.getVector().values()[1],1e-12);
        assertFalse(mapper.embedding(200,json.writeValueAsString(root),"qwen-embedding",768,false).isSuccess());
    }
    @Test void rejectsWrongDimensionsNonNumericAndZeroVectors() {
        for(String value:List.of("[1,2]","[\"secret\",2]","[0,0]"))
            assertFalse(mapper.embedding(200,"{\"data\":[{\"embedding\":"+value+"}]}","qwen",768,true).isSuccess());
    }
    @Test void classifiesLocalOperationalErrorsWithoutReturningProviderBody() {
        assertEquals("LOCAL_OUT_OF_MEMORY",LmStudioWireMapper.errorCode(500,"failed to allocate memory"));
        assertEquals("LOCAL_MODEL_NOT_READY",LmStudioWireMapper.errorCode(400,"model is not loaded"));
        assertEquals("LOCAL_CONTEXT_LIMIT",LmStudioWireMapper.errorCode(400,"context length exceeded"));
        assertEquals("LOCAL_AUTH_FAILED",LmStudioWireMapper.errorCode(401,"model is not loaded"));
        assertEquals("MODEL_NOT_FOUND",LmStudioWireMapper.errorCode(404,"missing"));
    }
}
