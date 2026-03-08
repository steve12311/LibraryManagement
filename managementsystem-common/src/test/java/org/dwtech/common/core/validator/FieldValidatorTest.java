package org.dwtech.common.core.validator;

import org.dwtech.common.annontation.ValidField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldValidatorTest {

    @Mock
    private ValidField validField;

    private FieldValidator fieldValidator;

    @BeforeEach
    void setUp() {
        when(validField.allowedValues()).thenReturn(new String[]{"ENABLE", "DISABLE"});
        fieldValidator = new FieldValidator();
        fieldValidator.initialize(validField);
    }

    @Test
    void shouldReturnTrueForNullValue() {
        assertThat(fieldValidator.isValid(null, null)).isTrue();
    }

    @Test
    void shouldReturnTrueForAllowedValue() {
        assertThat(fieldValidator.isValid("ENABLE", null)).isTrue();
    }

    @Test
    void shouldReturnFalseForNotAllowedValue() {
        assertThat(fieldValidator.isValid("UNKNOWN", null)).isFalse();
    }
}
