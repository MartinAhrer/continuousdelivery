package at.martinahrer.cd.test

import at.martinahrer.cd.Application
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import spock.lang.Narrative
import spock.lang.Specification

@SpringBootTest(classes = [Application], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfiguration)
@Narrative("As a customer relations manager I want to add a company")
class CompanyApiTest extends Specification {

    String host = "localhost"

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    def "posting a valid entity"() {
        given:
        JsonBuilder builder = new JsonBuilder()
        builder {
            name 'ACME'
        }
        // better done with rest-assured
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        def request = new HttpEntity<String>(builder.toPrettyString(), headers)

        when:
        def response = restTemplate.postForEntity("http://$host:$port/api/companies", request, Object)

        then:
        response.statusCode == HttpStatus.CREATED
    }

    static @Configuration
    class TestConfiguration {
        @Bean
        TestRestTemplate  testRestTemplate () {
            return new TestRestTemplate();
        }

        @Bean
        PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
            return  new PropertySourcesPlaceholderConfigurer();
        }
    }
}

