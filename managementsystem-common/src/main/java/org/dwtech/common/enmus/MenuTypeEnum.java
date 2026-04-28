package org.dwtech.common.enmus;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 菜单类型枚举
 *
 * @author steve12311
 * @since 2025-11-18
 */
@Getter
public enum MenuTypeEnum implements IBaseEnum<Integer> {

    NULL(0, null),
    MENU(1, "菜单"),
    CATALOG(2, "目录"),
    EXTLINK(3, "外链"),
    BUTTON(4, "按钮");

    //  Mybatis-Plus 提供注解表示插入数据库时插入该值
    @EnumValue
    private final Integer value;

    // @JsonValue //  表示对枚举序列化时返回此字段
    private final String label;

    /**
     * 构造菜单类型枚举项。
     *
     * @param value 菜单类型值
     * @param label 菜单类型描述
     */
    MenuTypeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

}
