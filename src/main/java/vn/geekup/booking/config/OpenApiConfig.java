package vn.geekup.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Concert Ticket Booking API")
                        .description("REST API for concert ticket booking platform. "
                                + "Supports concert browsing, ticket reservation with idempotency, "
                                + "payment webhook processing, and admin management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("GeekUp")
                                .email("dev@geekup.vn")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")));
    }
}
