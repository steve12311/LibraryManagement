package org.dwtech.system.model.vo;

import lombok.Data;

/**
 * 公开书架图书摘要
 *
 * @author steve12311
 * @since 2026-05-10
 */
@Data
public class PublicShelfBookVO {

    private Long shelfId;

    private String isbn;

    private String coverUrl;

    private String name;
}
