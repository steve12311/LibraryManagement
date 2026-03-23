package org.dwtech.system.model.query;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldRejectPageNumLessThanOne() {
        PublicBookPageQuery query = new PublicBookPageQuery();
        query.setPageNum(0);

        assertThat(validator.validate(query))
                .extracting(violation -> violation.getMessage())
                .contains("页码必须大于等于 1");
    }

    @Test
    void shouldRejectPageSizeGreaterThanLimit() {
        PublicBookPageQuery query = new PublicBookPageQuery();
        query.setPageSize(101);

        assertThat(validator.validate(query))
                .extracting(violation -> violation.getMessage())
                .contains("每页条数不能超过 100");
    }

    @Test
    void shouldRejectBorrowFieldOutsideWhitelist() {
        BorrowPageQuery query = new BorrowPageQuery();
        query.setField("bookName");

        assertThat(validator.validate(query))
                .extracting(violation -> violation.getMessage())
                .contains("非法字段");
    }

    @Test
    void shouldRejectPublishFieldOutsideWhitelist() {
        PublishPageQuery query = new PublishPageQuery();
        query.setField("country");

        assertThat(validator.validate(query))
                .extracting(violation -> violation.getMessage())
                .contains("非法字段");
    }
}
