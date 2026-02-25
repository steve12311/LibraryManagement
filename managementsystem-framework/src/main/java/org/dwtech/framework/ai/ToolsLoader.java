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
     * 用途：创建 ToolsLoader 实例。
     * 
     * @param applicationContext application context
     * 返回：无。
     */
    public ToolsLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 用途：获取 all tools 信息。
     * 
     * 入参：无。
     * @return 结果列表
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
     * 用途：执行 scan tools 操作。
     * 
     * 入参：无。
     * @return 结果列表
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
     * 用途：执行 match all filter 操作。
     * 
     * 入参：无。
     * @return 返回结果
     */
    private TypeFilter matchAllFilter() {
        return (metadataReader, metadataReaderFactory) -> true;
    }

    /**
     * 用途：执行 resolve tools package 操作。
     * 
     * 入参：无。
     * @return 结果字符串
     */
    private String resolveToolsPackage() {
        String basePackage = ToolsLoader.class.getPackageName();
        if (basePackage.endsWith("." + TOOLS_PACKAGE_SUFFIX)) {
            return basePackage;
        }
        return basePackage + "." + TOOLS_PACKAGE_SUFFIX;
    }

    /**
     * 用途：判断是否存在 tool methods。
     * 
     * @param toolClass tool class
     * @return 操作结果，true 表示成功，false 表示失败
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
     * 用途：加载 class。
     * 
     * @param className class name
     * @return 返回结果
     */
    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}
