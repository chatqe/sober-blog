package io.charqe.qs.modules.file.controller;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.modules.file.controller.admin.vo.FileDetailRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileVariantsVO;
import io.charqe.qs.modules.file.controller.web.FileController;
import io.charqe.qs.modules.file.service.FileMediaService;
import io.charqe.qs.modules.user.auth.SessionScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 详情端点的 HTTP 契约：访问地址与派生图、ETag/缓存头、条件请求 304、HEAD 与 404 语义。
 */
class FileDetailHttpContractTest {

    private static final String HASH = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    private FileMediaService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(FileMediaService.class);
        var controller = new FileController();
        ReflectionTestUtils.setField(controller, "fileMediaService", service);
        ReflectionTestUtils.setField(controller, "detailCacheControl", "public, max-age=300");
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private FileDetailRespVO sample() {
        var variants = new FileVariantsVO();
        variants.setThumb("https://img.soberup.top/a.png?imageView2/2/w/320/q/85");
        variants.setCard("https://img.soberup.top/a.png?imageView2/2/w/800/q/85");
        variants.setHero("https://img.soberup.top/a.png?imageView2/2/w/1600/q/85");

        var detail = new FileDetailRespVO();
        detail.setId(9L);
        detail.setOriginalName("cover.png");
        detail.setStoragePath("avatar/3/202610/a.png");
        detail.setMimeType("image/png");
        detail.setThirdPartyName(3);
        detail.setScene("avatar");
        detail.setStatus(1);
        detail.setRefCount(0);
        detail.setVisitPath("https://img.soberup.top/avatar/3/202610/a.png");
        detail.setEtag(HASH);
        detail.setVariantMode("transform");
        detail.setVariants(variants);
        return detail;
    }

    @Test
    void detailReturnsVisitPathAndVariants() throws Exception {
        when(service.detail(9L)).thenReturn(sample());

        mvc.perform(get("/file/detail/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.visitPath").value("https://img.soberup.top/avatar/3/202610/a.png"))
                .andExpect(jsonPath("$.data.variantMode").value("transform"))
                .andExpect(jsonPath("$.data.variants.thumb")
                        .value("https://img.soberup.top/a.png?imageView2/2/w/320/q/85"))
                .andExpect(jsonPath("$.data.variants.hero")
                        .value("https://img.soberup.top/a.png?imageView2/2/w/1600/q/85"));

        verify(service).detail(9L);
    }

    @Test
    void detailSetsStrongEtagAndCacheControl() throws Exception {
        when(service.detail(9L)).thenReturn(sample());

        mvc.perform(get("/file/detail/9"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"" + HASH + "\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=300"));
    }

    @Test
    void matchingIfNoneMatchReturnsNotModified() throws Exception {
        when(service.detail(9L)).thenReturn(sample());

        mvc.perform(get("/file/detail/9").header(HttpHeaders.IF_NONE_MATCH, "\"" + HASH + "\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, "\"" + HASH + "\""))
                .andExpect(content().string(""));
    }

    @Test
    void weakEtagAndWildcardAlsoMatch() throws Exception {
        when(service.detail(9L)).thenReturn(sample());

        mvc.perform(get("/file/detail/9").header(HttpHeaders.IF_NONE_MATCH, "W/\"" + HASH + "\""))
                .andExpect(status().isNotModified());

        mvc.perform(get("/file/detail/9").header(HttpHeaders.IF_NONE_MATCH, "*"))
                .andExpect(status().isNotModified());
    }

    @Test
    void staleIfNoneMatchReturnsFullBody() throws Exception {
        when(service.detail(9L)).thenReturn(sample());

        mvc.perform(get("/file/detail/9").header(HttpHeaders.IF_NONE_MATCH, "\"stale\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(9));
    }

    @Test
    void detailWithoutHashOmitsEtag() throws Exception {
        var detail = sample();
        detail.setEtag(null);
        when(service.detail(9L)).thenReturn(detail);

        mvc.perform(get("/file/detail/9"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.data.etag").doesNotExist());
    }

    @Test
    void headReturnsHeadersWithoutBody() throws Exception {
        when(service.detail(9L)).thenReturn(sample());

        mvc.perform(head("/file/detail/9"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"" + HASH + "\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=300"))
                .andExpect(content().string(""));
    }

    @Test
    void unknownFileKeepsEnvelopeWith404Code() throws Exception {
        when(service.detail(404L)).thenReturn(null);

        mvc.perform(get("/file/detail/404"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("文件不存在"))
                .andExpect(header().doesNotExist(HttpHeaders.ETAG));
    }

    @Test
    void detailRequiresAdminSession() throws Exception {
        var method = FileController.class.getMethod("detail", Long.class, String.class);
        var login = method.getAnnotation(LoginCheck.class);
        assertEquals(1, login.value());
        assertEquals(SessionScope.ADMIN, login.scope());
        verifyNoInteractions(service);
    }
}
