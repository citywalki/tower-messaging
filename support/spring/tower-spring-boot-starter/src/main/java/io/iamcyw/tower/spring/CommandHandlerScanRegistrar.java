package io.iamcyw.tower.spring;

import io.iamcyw.tower.messaging.CommandHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Set;

/**
 * Scans the classpath for {@link CommandHandler} implementations and registers them
 * as Spring bean definitions.
 *
 * <p>This enables Clean Architecture patterns where handler classes do not carry
 * Spring annotations ({@code @Component}, {@code @Service}, etc.). Handlers are
 * discovered purely by implementing the {@link CommandHandler} interface.</p>
 *
 * <p>Scanning is performed on the packages returned by
 * {@link AutoConfigurationPackages} (i.e., the package of the
 * {@code @SpringBootApplication} class and its sub-packages).</p>
 *
 * <p>If a handler is already registered (e.g., via explicit {@code @Bean} definition),
 * it will not be registered again.</p>
 *
 * @since 2.0
 */
class CommandHandlerScanRegistrar
        implements ImportBeanDefinitionRegistrar, BeanFactoryAware, EnvironmentAware, ResourceLoaderAware {

    private BeanFactory beanFactory;
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!AutoConfigurationPackages.has(this.beanFactory)) {
            return;
        }

        var scanner = new ClassPathScanningCandidateComponentProvider(false, this.environment);
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AssignableTypeFilter(CommandHandler.class));

        for (String basePackage : AutoConfigurationPackages.get(this.beanFactory)) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                String beanName = deriveBeanName(candidate.getBeanClassName());
                if (!registry.containsBeanDefinition(beanName)) {
                    registry.registerBeanDefinition(beanName, candidate);
                }
            }
        }
    }

    /**
     * Derives a bean name from a fully qualified class name, following Spring's
     * default convention (short class name with lowercase first letter).
     */
    private String deriveBeanName(String fqcn) {
        String shortName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        return Character.toLowerCase(shortName.charAt(0)) + shortName.substring(1);
    }

}
