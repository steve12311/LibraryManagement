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

@Service
public class ToolsLoader {
    private static final String TOOLS_PACKAGE_SUFFIX = "tools";

    private final ApplicationContext applicationContext;
    private volatile List<Object> cachedTools;

    public ToolsLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

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

    private TypeFilter matchAllFilter() {
        return (metadataReader, metadataReaderFactory) -> true;
    }

    private String resolveToolsPackage() {
        String basePackage = ToolsLoader.class.getPackageName();
        if (basePackage.endsWith("." + TOOLS_PACKAGE_SUFFIX)) {
            return basePackage;
        }
        return basePackage + "." + TOOLS_PACKAGE_SUFFIX;
    }

    private boolean hasToolMethods(Class<?> toolClass) {
        for (var method : toolClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                return true;
            }
        }
        return false;
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}
