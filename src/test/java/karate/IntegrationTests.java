package karate;

import com.intuit.karate.junit5.Karate;
import edu.northeastern.gatewayapplication.GatewayapplicationApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {GatewayapplicationApplication.class})
public class IntegrationTests {

    @Karate.Test
    Karate healthEndpointTest() {
        return Karate.run("classpath:karate/HealthEndpoint.feature");
    }
}
