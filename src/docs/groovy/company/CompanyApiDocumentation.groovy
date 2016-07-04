package documentation.company

import at.martinahrer.cd.Application
import at.martinahrer.cd.CompanyRepository
import at.martinahrer.cd.model.Company
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.hateoas.MediaTypes
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentation
import org.springframework.restdocs.constraints.ConstraintDescriptions
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.util.StringUtils
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
import static org.springframework.restdocs.snippet.Attributes.key
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application)
@WebAppConfiguration
class CompanyApiDocumentation {

    Closure<String> VALUE_WRITER = { def content -> this.objectMapper.writeValueAsString(content) }

    @Rule
    public RestDocumentation restDocumentation = new RestDocumentation("build/generated-snippets/company");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private RestDocumentationResultHandler document;

    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;


    @Before
    public void setUp() {
        this.document = document("{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()));

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation))
                .alwaysDo(this.document)
                .build();

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
    public void listExample() {
        this.document.snippets(
                links(
                        linkWithRel("self").description("This <<resources-companies,companies>>"),
                        linkWithRel("profile").description("The <<resources-companies-profile, companies profile resource>>")),

                responseFields(
                        fieldWithPath("_embedded.companies").description("An array of <<resources-companies, Company resources>>"),
                        fieldWithPath("_links").description("<<resources-companies-links,Links>> to other resources"),
                        fieldWithPath("page").description("The pagination information")));

        this.mockMvc.perform(get("/api/companies").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getByIdExample() {
        ConstrainedFields fields = new ConstrainedFields(Company);

        this.document.snippets(
                links(
                        linkWithRel("self").description("This <<resources-companies,company>>"),
                        linkWithRel("company").description("This <<resources-companies,company>>")
                ),
                responseFields(
                        fieldWithPath("new").ignored(),
                        fields.withPath("name").description("The name of the company"),
                        fields.withPath("address").description("The address of the company"),
                        fieldWithPath("_links").description("<<resources-companies-links,Links>> to other resources")));

        this.mockMvc.perform(get("/api/companies/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void createExample() throws Exception {
        def resource = new CompanyResourceFactory(contentWriter: VALUE_WRITER).newResource();

        ConstrainedFields fields = new ConstrainedFields(Company);

        this.document.snippets(
                requestFields(
                        fields.withPath("name").description("The name of the company"),
                        fields.withPath("address").optional().description("The address of the company")));

        this.mockMvc.perform(
                post("/api/companies").contentType(MediaTypes.HAL_JSON).content(resource))
                .andExpect(status().isCreated());
    }

    private static class ConstrainedFields {

        private final ConstraintDescriptions constraintDescriptions;

        ConstrainedFields(Class<?> input) {
            this.constraintDescriptions = new ConstraintDescriptions(input);
        }

        private FieldDescriptor withPath(String path) {
            return fieldWithPath(path).attributes(key("constraints").value(StringUtils
                    .collectionToDelimitedString(this.constraintDescriptions
                    .descriptionsForProperty(path), ". ")));
        }
    }
}
