package test

import at.martinahrer.cd.Application
import at.martinahrer.cd.CompanyRepository
import at.martinahrer.cd.model.Company
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.hateoas.MediaTypes
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import shared.CompanyResourceFactory

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import static org.springframework.restdocs.payload.PayloadDocumentation.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith([RestDocumentationExtension, SpringExtension])
@SpringBootTest(classes = Application)
@WebAppConfiguration
class CompanyApiDocumentation {

    Closure<String> VALUE_WRITER = { def content -> this.objectMapper.writeValueAsString(content) }

    @Autowired
    private ObjectMapper objectMapper

    private MockMvc mockMvc

    @Autowired
    private CompanyRepository companyRepository

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation)
                .operationPreprocessors()
                    .withRequestDefaults(prettyPrint())
                    .withResponseDefaults(prettyPrint()))
                .build()

        createExampleCompany()
    }

    private void createExampleCompany() {
        def objectGraphBuilder = new ObjectGraphBuilder(classLoader: this.class.classLoader, classNameResolver: 'at.martinahrer.cd.model')
        def Company acme = objectGraphBuilder."company"(name: 'ACME') {
            address(line1: "Post street 1", line2: "Post Street 2")
        }

        acme = companyRepository.save(acme)
    }

    @Test
    void listExample() {
        this.mockMvc.perform(get("/api/companies").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(document("{class-name}/{method-name}",
                    links(
                        linkWithRel("self").description("This <<resources-companies,companies>>"),
                        linkWithRel("profile").description("The <<resources-companies-profile, companies profile resource>>")),

                    responseFields(
                            subsectionWithPath("_embedded.companies").description("An array of <<resources-companies, Company resources>>"),
                            subsectionWithPath("_links").description("<<resources-companies-links,Links>> to other resources"),
                            subsectionWithPath("page").description("The pagination information")))
        )
    }

    @Test
    void getByIdExample() {
        this.mockMvc.perform(get("/api/companies/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(document("{class-name}/{method-name}",
                    links(
                        linkWithRel("self").description("This <<resources-companies,company>>"),
                        linkWithRel("company").description("This <<resources-companies,company>>")
                    ),
                    responseFields(
                        fieldWithPath("new").ignored(),
                            fieldWithPath("name").description("The name of the company"),
                            subsectionWithPath("address").description("The address of the company"),
                            subsectionWithPath("_links").description("<<resources-companies-links,Links>> to other resources")))
        )
    }

    @Test
    void createExample() throws Exception {
        def resource = new CompanyResourceFactory(contentWriter: VALUE_WRITER).newResource();

        this.mockMvc.perform(
                post("/api/companies").contentType(MediaTypes.HAL_JSON).content(resource))
                .andExpect(status().isCreated())
                .andDo(document("{class-name}/{method-name}",
                    requestFields(
                            fieldWithPath("name")
                                    .description("The name of the company"),
                            subsectionWithPath("address")
                                    .description("The address of the company")
                                    .optional(),
                    )))
    }
}
