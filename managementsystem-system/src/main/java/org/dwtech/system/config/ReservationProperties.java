package org.dwtech.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "reservation")
public class ReservationProperties {
    private int pickupDays = 3;
    private int maxPerUser = 3;
}
