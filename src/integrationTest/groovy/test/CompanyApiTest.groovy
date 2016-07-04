package test

import at.martinahrer.cd.Application
import at.martinahrer.cd.model.Company
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.boot.test.TestRestTemplate
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import spock.lang.Narrative
import spock.lang.Specification

@SpringApplicationConfiguration(classes = Application)
@WebIntegrationTest(randomPort = true)
// equals to ("server.port:0")

@Narrative("As a customer relations manager I want to add a company")
class CompanyApiTest extends Specification {

    String host = "localhost"

    @Value('${local.server.port}')
    int port;

    RestTemplate restTemplate = new TestRestTemplate();

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
        def response=restTemplate.postForEntity("http://$host:$port/api/companies", request, Object)

        then:
        response.statusCode == HttpStatus.CREATED
    }

}

