package org.dwtech.system.converter;

import org.dwtech.system.model.bo.StockBO;
import org.dwtech.system.model.vo.PublicBookPageVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockConverterTest {

    private final StockConverter stockConverter = new StockConverterImpl();

    @Test
    void shouldConvertStockToPublicBookPageWhenCurrentStockPositive() {
        StockBO stockBO = new StockBO();
        stockBO.setCover("/101");
        stockBO.setName("深入理解 Java");
        stockBO.setIsbn("9787300000001");
        stockBO.setCurrentStock(3);
        stockBO.setIntro("Java 核心原理");
        stockBO.setCategoryName("编程语言");
        stockBO.setPressName("人民邮电出版社");
        stockBO.setAuthor("李四");

        PublicBookPageVO publicBook = stockConverter.toPublicPageVo(stockBO);

        assertThat(publicBook.getCoverUrl()).isEqualTo("/api/v1/files/101");
        assertThat(publicBook.getName()).isEqualTo("深入理解 Java");
        assertThat(publicBook.getIsbn()).isEqualTo("9787300000001");
        assertThat(publicBook.getAvailable()).isTrue();
        assertThat(publicBook.getIntro()).isEqualTo("Java 核心原理");
        assertThat(publicBook.getCategoryName()).isEqualTo("编程语言");
        assertThat(publicBook.getPublishName()).isEqualTo("人民邮电出版社");
        assertThat(publicBook.getAuthor()).isEqualTo("李四");
    }

    @Test
    void shouldMarkPublicBookUnavailableWhenCurrentStockMissing() {
        StockBO stockBO = new StockBO();
        stockBO.setCurrentStock(null);

        PublicBookPageVO publicBook = stockConverter.toPublicPageVo(stockBO);

        assertThat(publicBook.getAvailable()).isFalse();
    }

    @Test
    void shouldKeepFullCoverUrlWhenAlreadyNormalized() {
        StockBO stockBO = new StockBO();
        stockBO.setCover("/api/v1/files/301");

        PublicBookPageVO publicBook = stockConverter.toPublicPageVo(stockBO);

        assertThat(publicBook.getCoverUrl()).isEqualTo("/api/v1/files/301");
    }
}
