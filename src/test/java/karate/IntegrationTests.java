package karate;

import com.intuit.karate.junit5.Karate;
import edu.northeastern.gatewayapplication.Application;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {Application.class})
public class IntegrationTests {

    @Karate.Test
    Karate healthEndpointTest() {
        return Karate.run("classpath:karate/HealthEndpoint.feature");
    }
}
