package xyz.juandiii.spring;

import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import xyz.juandiii.ark.core.proxy.RegisterArkClient;

/**
 * AOT processor that discovers @ArkClient interfaces from bean definitions
 * and registers them as JDK proxy definitions for GraalVM native image.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkClientAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        String[] beanNames = beanFactory.getBeanDefinitionNames();

        return (generationContext, beanFactoryInitializationCode) -> {
            for (String beanName : beanNames) {
                try {
                    Class<?> beanType = beanFactory.getType(beanName);
                    if (beanType != null && beanType.isInterface() && beanType.isAnnotationPresent(RegisterArkClient.class)) {
                        generationContext.getRuntimeHints().proxies().registerJdkProxy(beanType);
                    }
                } catch (Exception ignored) {
                }
            }
        };
    }
}