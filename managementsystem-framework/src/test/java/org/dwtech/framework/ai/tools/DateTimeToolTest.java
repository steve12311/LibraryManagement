package org.dwtech.framework.ai.tools;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThatCode;

class DateTimeToolTest {

    @Test
    void shouldReturnCurrentDateTimeInCurrentTimeZone() {
        LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            DateTimeTool dateTimeTool = new DateTimeTool();
            String currentDateTime = dateTimeTool.getCurrentDateTime();

            assertThatCode(() -> ZonedDateTime.parse(currentDateTime)).doesNotThrowAnyException();
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
