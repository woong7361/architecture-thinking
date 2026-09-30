package com.thinking.tennis.app;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AvailabilityServiceConstructorWiringConfiguration
        implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition("availabilityService")) {
            registry.getBeanDefinition("availabilityService")
                    .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // Constructor autowiring is configured at the bean-definition stage.
    }
}
