package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.ban.vehicle_management.application.ai.service.CommonKnowledgeService;
import com.ban.vehicle_management.application.ai.port.out.CommonKnowledgePortOut;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.infrastructure.mapper.ai.CommonKnowledgePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.adapter.ai.CommonKnowledgePersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeChunkEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import java.math.BigDecimal;
import java.util.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.*;

/** Small isolated JPA/SQL context. No application schedulers or AI HTTP clients are constructed. */
@SpringJUnitConfig(CommonKnowledgePersistenceTest.Config.class)
@Transactional
class CommonKnowledgePersistenceTest extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {
    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses=KnowledgeChunkRepository.class,
            excludeFilters=@ComponentScan.Filter(type=FilterType.REGEX,pattern="(?!.*KnowledgeChunkRepository$).*"))
    static class Config {
        @Bean DataSource dataSource(Environment env) {
            return new DriverManagerDataSource(env.getRequiredProperty("spring.datasource.url"),
                    env.getRequiredProperty("spring.datasource.username"),env.getRequiredProperty("spring.datasource.password"));
        }
        @Bean(initMethod="migrate") Flyway flyway(DataSource source) {
            return Flyway.configure().dataSource(source).locations("classpath:db/migration").load();
        }
        @Bean @DependsOn("flyway") LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(source);
            factory.setManagedTypes(PersistenceManagedTypes.of(KnowledgeChunkEntity.class.getName()));
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());return factory;
        }
        @Bean JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }
        @Bean CommonKnowledgePortOut commonPort(KnowledgeChunkRepository repository) {
            return new CommonKnowledgePersistenceAdapter(repository,Mappers.getMapper(CommonKnowledgePersistenceMapper.class));
        }
        @Bean CommonKnowledgeService commonService(CommonKnowledgePortOut port) { return new CommonKnowledgeService(port); }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired CommonKnowledgePortOut port;
    @Autowired CommonKnowledgeService service;
    private record Fixture(UUID source,UUID document,UUID chunk,UUID key) { }

    private Fixture fixture(String scope,UUID tenant,String status,boolean reviewed,String answer) {
        UUID source=UUID.randomUUID(),document=UUID.randomUUID(),chunk=UUID.randomUUID(),key=UUID.randomUUID();
        jdbc.update("insert into ai.knowledge_sources(source_id,title,access_scope,tenant_id,status) values (?,?,?,?, 'ACTIVE')",source,"ISOLATED COMMON FAQ",scope,tenant);
        jdbc.update("insert into ai.knowledge_documents(document_id,source_id,document_key,title,document_version,access_scope,tenant_id,status,reviewed_at,reviewed_by) values (?,?,?,'Tài khoản',1,?,?,?,case when ? then now() else null end,?)",
                document,source,key,scope,tenant,status,reviewed,reviewed?UUID.randomUUID():null);
        jdbc.update("insert into ai.knowledge_chunks(chunk_id,document_id,title,content,chunk_index,access_scope,tenant_id,document_version,status,source_section,start_block_index,end_block_index) values (?,?,'Tài khoản',?,0,?,?,1,'READY','FAQ',0,0)",
                chunk,document,"Làm sao đăng ký CoParking? "+answer,scope,tenant);
        jdbc.update("insert into ai.knowledge_document_blocks(block_id,document_id,doc_version,block_index,kind,source_section,content) values (?,?,1,0,'PARAGRAPH','FAQ',?)",
                UUID.randomUUID(),document,"Làm sao đăng ký CoParking? "+answer);
        return new Fixture(source,document,chunk,key);
    }
    private List<CommonKnowledgeCandidate> search(UUID tenant,List<String> scopes) {
        return port.searchReviewedFaqs(tenant,scopes,"dang | ky | tai | khoan",33);
    }
    @Test void realUnaccentSearchAndMapperNeedNoVectorIndexOrEmbedding() {
        var fixture=fixture("PUBLIC",null,"READY",true,"Mở Đăng ký và xác minh email.");
        var rows=search(null,List.of("PUBLIC"));assertEquals(1,rows.size());assertEquals(fixture.chunk(),rows.getFirst().chunkId());
        assertNotNull(rows.getFirst().reviewedAt());assertEquals(1,rows.getFirst().documentVersion());
        for(String question:List.of("Đăng ký tài khoản như thế nào?","dang ky tai khoan nhu the nao","dk tai khoan can lam gi")) {
            var result=service.lookup(KnowledgeRetrievalContext.global(),question,List.of("PUBLIC")).orElseThrow();
            assertEquals("Mở Đăng ký và xác minh email.",result.answer());
            assertTrue(result.snapshot().contains(fixture.document()+"@1"));
        }
        assertEquals(0,jdbc.queryForObject("select count(*) from ai.knowledge_embeddings where chunk_id=?",Integer.class,fixture.chunk()));
    }
    @Test void reviewSourceStatusAndAllThreeScopesAreMandatory() {
        fixture("PUBLIC",null,"REVIEW",true,"Draft.");fixture("PUBLIC",null,"READY",false,"Unreviewed.");
        var restricted=fixture("CUSTOMER",null,"READY",true,"Customer only.");
        var mixed=fixture("PUBLIC",null,"READY",true,"Source private.");
        jdbc.update("update ai.knowledge_sources set access_scope='CUSTOMER' where source_id=?",mixed.source());
        assertTrue(search(null,List.of("PUBLIC")).isEmpty());
        assertEquals(2,search(null,List.of("PUBLIC","CUSTOMER")).size());
        jdbc.update("update ai.knowledge_sources set status='INACTIVE' where source_id=?",restricted.source());
        assertEquals(1,search(null,List.of("PUBLIC","CUSTOMER")).size());
    }
    @Test void tenantFiltersApplyAtChunkDocumentAndSource() {
        UUID own=UUID.randomUUID(),other=UUID.randomUUID();
        var first=fixture("TENANT_PRIVATE",own,"READY",true,"Own tenant.");
        fixture("TENANT_PRIVATE",other,"READY",true,"Other tenant.");
        var mixed=fixture("TENANT_PRIVATE",own,"READY",true,"Mismatched chunk.");
        jdbc.update("update ai.knowledge_chunks set tenant_id=? where chunk_id=?",other,mixed.chunk());
        assertTrue(search(null,List.of("TENANT_PRIVATE")).isEmpty());
        var rows=search(own,List.of("TENANT_PRIVATE"));assertEquals(1,rows.size());assertEquals(first.chunk(),rows.getFirst().chunkId());
        assertTrue(search(own,List.of("PUBLIC")).isEmpty());
    }
    @Test void changedSourceVersionExpiryAndPermissionInvalidateSelection() {
        var f=fixture("PUBLIC",null,"READY",true,"Cần email đã xác minh.");
        var match=service.lookup(KnowledgeRetrievalContext.global(),"Đăng ký tài khoản như thế nào?",List.of("PUBLIC")).orElseThrow();
        var result=new KnowledgeRetrievalResult(null,null,List.of(match.source().evidence()),UUID.randomUUID(),BigDecimal.ONE,true,"q","p","COMMON_FAQ",match.snapshot(),match.answer());
        assertTrue(service.verify(KnowledgeRetrievalContext.global(),List.of("PUBLIC"),result));
        jdbc.update("update ai.knowledge_sources set access_scope='ADMIN' where source_id=?",f.source());
        assertFalse(service.verify(KnowledgeRetrievalContext.global(),List.of("PUBLIC"),result));
        jdbc.update("update ai.knowledge_sources set access_scope='PUBLIC' where source_id=?",f.source());
        jdbc.update("update ai.knowledge_documents set effective_to=now()-interval '1 second' where document_id=?",f.document());
        assertTrue(search(null,List.of("PUBLIC")).isEmpty());
        jdbc.update("update ai.knowledge_documents set effective_to=null where document_id=?",f.document());
        UUID newer=UUID.randomUUID();
        jdbc.update("insert into ai.knowledge_documents(document_id,source_id,document_key,title,document_version,access_scope,status,reviewed_at,reviewed_by) values (?,?,?,'Newer',2,'PUBLIC','READY',now(),?)",newer,f.source(),f.key(),UUID.randomUUID());
        assertTrue(search(null,List.of("PUBLIC")).isEmpty());
        assertFalse(service.verify(KnowledgeRetrievalContext.global(),List.of("PUBLIC"),result));
    }
    @Test void wrongSectionAndWrongChunkVersionAreNotCommonEvidence() {
        var wrongSection=fixture("PUBLIC",null,"READY",true,"Not a reviewed FAQ section.");
        jdbc.update("update ai.knowledge_document_blocks set source_section='Ví dụ' where document_id=?",wrongSection.document());
        var stale=fixture("PUBLIC",null,"READY",true,"Wrong chunk version.");
        jdbc.update("update ai.knowledge_chunks set document_version=2 where chunk_id=?",stale.chunk());
        assertTrue(search(null,List.of("PUBLIC")).isEmpty());
    }
    @Test void mixedChunkCannotPromoteExamplesOutsideFaqBlocks() {
        var f=fixture("PUBLIC",null,"READY",true,"Mở Đăng ký và xác minh email.");
        jdbc.update("insert into ai.knowledge_document_blocks(block_id,document_id,doc_version,block_index,kind,source_section,content) values (?,?,1,1,'PARAGRAPH','Ví dụ','Khôi phục mật khẩu như thế nào? Sai: gửi mật khẩu trong chat.')",UUID.randomUUID(),f.document());
        jdbc.update("update ai.knowledge_chunks set end_block_index=1,content=content||chr(10)||'Khôi phục mật khẩu như thế nào? Sai: gửi mật khẩu trong chat.' where chunk_id=?",f.chunk());
        var rows=port.searchReviewedFaqs(null,List.of("PUBLIC"),"mat | khau",33);
        assertEquals(1,rows.size());assertFalse(rows.getFirst().content().contains("gửi mật khẩu trong chat"));
        assertTrue(service.lookup(KnowledgeRetrievalContext.global(),"Khôi phục mật khẩu như thế nào?",List.of("PUBLIC")).isEmpty());
        jdbc.update("update ai.knowledge_chunks set start_block_index=null,end_block_index=null where chunk_id=?",f.chunk());
        assertTrue(search(null,List.of("PUBLIC")).isEmpty());
    }
}
