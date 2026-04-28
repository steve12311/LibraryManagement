package org.dwtech.framework.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
/**
 * ToolsLoader
 *
 * @author steve12311
 * @since 2026-02-24
 */

@Service
public class ToolsLoader {
    private static final String TOOLS_PACKAGE_SUFFIX = "tools";

    private final ApplicationContext applicationContext;
    private volatile List<Object> cachedTools;

    /**
     * 构造 ToolsLoader，注入 ApplicationContext 用于后续扫描和创建 Tool Bean。
     *
     * @param applicationContext Spring 应用上下文
     */
    public ToolsLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取所有 AI Tool 实例（带缓存，线程安全）。
     *
     * @return 不可修改的 AI Tool 实例列表
     */
    public List<Object> getAllTools() {
        List<Object> tools = cachedTools;
        if (tools != null) {
            return tools;
        }
        synchronized (this) {
            if (cachedTools == null) {
                cachedTools = Collections.unmodifiableList(scanTools());
            }
            return cachedTools;
        }
    }

    /**
     * 扫描 tools 包下所有包含 @Tool 注解方法的类，并创建其实例。
     *
     * @return 扫描到的 Tool 实例列表
     */
    private List<Object> scanTools() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(matchAllFilter());

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(resolveToolsPackage());
        List<Object> tools = new ArrayList<>();

        for (BeanDefinition candidate : candidates) {
            String className = candidate.getBeanClassName();
            if (className == null) {
                continue;
            }
            Class<?> toolClass = loadClass(className);
            if (toolClass == null || toolClass.isInterface() || Modifier.isAbstract(toolClass.getModifiers())) {
                continue;
            }
            if (!hasToolMethods(toolClass)) {
                continue;
            }
            Object tool = applicationContext
                    .getAutowireCapableBeanFactory()
                    .createBean(toolClass);
            tools.add(tool);
        }

        return tools;
    }

    /**
     * 返回匹配所有类的 TypeFilter。
     *
     * @return 始终返回 true 的 TypeFilter
     */
    private TypeFilter matchAllFilter() {
        return (metadataReader, metadataReaderFactory) -> true;
    }

    /**
     * 解析 Tool 类所在的扫描包路径。
     *
     * @return 扫描的基础包名
     */
    private String resolveToolsPackage() {
        String basePackage = ToolsLoader.class.getPackageName();
        if (basePackage.endsWith("." + TOOLS_PACKAGE_SUFFIX)) {
            return basePackage;
        }
        return basePackage + "." + TOOLS_PACKAGE_SUFFIX;
    }

    /**
     * 检查指定类中是否声明了带有 @Tool 注解的方法。
     *
     * @param toolClass 待检查的 Class
     * @return 包含 @Tool 注解方法返回 true
     */
    private boolean hasToolMethods(Class<?> toolClass) {
        for (var method : toolClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据全限定类名加载 Class 对象。
     *
     * @param className 类的全限定名
     * @return Class 对象，加载失败返回 null
     */
    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}
