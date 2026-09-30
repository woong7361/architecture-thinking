package com.thinking.tennis.app;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects constructor autowiring for application services that expose more than
 * one construction path, including test-friendly overloads.
 */
@Configuration(proxyBeanMethods = false)
class SpringConstructorWiringConfiguration {

    @Bean
    static BeanDefinitionRegistryPostProcessor alertServiceConstructorAutowiring() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition("alertService")) {
                    AbstractBeanDefinition definition =
                            (AbstractBeanDefinition) registry.getBeanDefinition("alertService");
                    definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
                    throws BeansException {
                // Constructor selection is the only adjustment required here.
            }
        };
    }
}
