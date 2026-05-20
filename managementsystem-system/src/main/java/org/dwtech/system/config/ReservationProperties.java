package org.dwtech.system.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@ConfigurationProperties(prefix = "reservation")
@Validated
public class ReservationProperties {
    @Min(value = 1)
    private int pickupDays = 3;
    @Min(value = 1)
    private int maxPerUser = 3;
}
