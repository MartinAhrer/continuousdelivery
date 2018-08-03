package test

import at.martinahrer.cd.Application
import at.martinahrer.cd.CompanyRepository
import at.martinahrer.cd.model.Company
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.hateoas.MediaTypes
import org.springframework.restdocs.JUnitRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*
import static org.springframework.restdocs.payload.PayloadDocumentation.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application)
@WebAppConfiguration
class CompanyApiDocumentation {

    Closure<String> VALUE_WRITER = { def content -> this.objectMapper.writeValueAsString(content) }

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets/company")

    @Autowired
    private WebApplicationContext context

    @Autowired
    private ObjectMapper objectMapper

    private RestDocumentationResultHandler documentationHandler

    private MockMvc mockMvc

    @Autowired
    private CompanyRepository companyRepository


    @Before
    void setUp() {
        this.documentationHandler = document("{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()))

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation))
                .alwaysDo(this.documentationHandler)
                .build()

        createExampleCompany()
        return
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
                .andDo(documentationHandler.document(
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
                .andDo(documentationHandler.document(
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
                .andDo(documentationHandler.document(
                    requestFields(
                            fieldWithPath("name")
                                    .description("The name of the company"),
                            subsectionWithPath("address")
                                    .description("The address of the company")
                                    .optional(),
                    )))
    }
}
