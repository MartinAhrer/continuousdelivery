package test

import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.test.context.ContextConfiguration


@Configuration
class IntegrationTestApplication {
    @Bean
    public TestRestTemplate  testRestTemplate () {
        return new TestRestTemplate();
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return  new PropertySourcesPlaceholderConfigurer();
    }
}
