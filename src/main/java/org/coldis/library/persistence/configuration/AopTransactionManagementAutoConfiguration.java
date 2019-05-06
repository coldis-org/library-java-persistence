package org.coldis.library.persistence.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration;

/**
 * AOP transaction manager configuration.
 */
@Configuration
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@ConditionalOnExpression(value = "${org.coldis.configuration.aspectj-enabled:true}")
@ConditionalOnClass(value = { AspectJTransactionManagementConfiguration.class, EnableTransactionManagement.class })
public class AopTransactionManagementAutoConfiguration {

}
