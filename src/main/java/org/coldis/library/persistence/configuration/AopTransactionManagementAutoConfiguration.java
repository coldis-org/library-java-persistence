package org.coldis.library.persistence.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration;

/**
 * AOP transaction manager configuration.
 */
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@ConditionalOnProperty(
		name = "org.coldis.configuration.aspectj-enabled",
		havingValue = "true",
		matchIfMissing = true
)
@ConditionalOnClass(value = { AspectJTransactionManagementConfiguration.class, EnableTransactionManagement.class })
public class AopTransactionManagementAutoConfiguration {

}
