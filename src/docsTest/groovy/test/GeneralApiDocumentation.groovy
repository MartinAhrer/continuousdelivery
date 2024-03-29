package test

import at.martinahrer.cd.Application
import at.martinahrer.cd.CompanyRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import jakarta.servlet.RequestDispatcher

import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import static org.springframework.restdocs.payload.PayloadDocumentation.*
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith([RestDocumentationExtension, SpringExtension])
@SpringBootTest(classes = Application)
@WebAppConfiguration
class GeneralApiDocumentation {

    Closure<String> VALUE_WRITER = { def content -> this.objectMapper.writeValueAsString(content) }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build()
    }

    @Test
    void headersExample() throws Exception {
        this.mockMvc.perform(get("/api/"))
                .andExpect(status().isOk())
                .andDo(document("{class-name}/{method-name}", responseHeaders(
                        headerWithName("Content-Type").description("The Content-Type of the payload, e.g. `application/hal+json`")))
                )
    }

    @Test
    void errorExample() throws Exception {
        this.mockMvc
                .perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI,
                                "/api/companies")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE,
                                "The company 'http://localhost:8080/api/companies/123' does not exist"))
                .andDo(print()).andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("error", is("Bad Request")))
                .andExpect(MockMvcResultMatchers.jsonPath("timestamp", is(notNullValue())))
                .andExpect(MockMvcResultMatchers.jsonPath("status", is(400)))
                .andExpect(MockMvcResultMatchers.jsonPath("path", is(notNullValue())))
                .andDo(document("{class-name}/{method-name}", responseFields(
                        fieldWithPath("error").description("The HTTP error that occurred, e.g. `Bad Request`"),
                        fieldWithPath("path").description("The path to which the request was made"),
                        fieldWithPath("status").description("The HTTP status code, e.g. `400`"),
                        fieldWithPath("timestamp").description("The time, in milliseconds, at which the error occurred"))))
    }

    @Test
    void indexExample() throws Exception {
        this.mockMvc.perform(get("/api/"))
                .andExpect(status().isOk())
                .andDo(
                        document("{class-name}/{method-name}",
                                links(
                                        linkWithRel("companies").description("The <<resources-companies,Companies resource>>"),
                                        linkWithRel("profile").description("The <<resources-profile,Profile resource>>")),
                                responseFields(
                                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")))
                )
    }
}
