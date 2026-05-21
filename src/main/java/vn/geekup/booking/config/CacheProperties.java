package vn.geekup.booking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cache")
@Getter
@Setter
public class CacheProperties {

    private int concertTtlSeconds = 600;
    private int ticketStaticTtlSeconds = 600;
    private int ticketDynamicTtlSeconds = 600;
    private int bookingLockTtlSeconds = 30;
}
