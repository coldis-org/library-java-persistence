package org.coldis.library.persistence.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Proxy transaction manager configuration.
 */
@EnableTransactionManagement(mode = AdviceMode.PROXY)
@ConditionalOnClass(value = { EnableTransactionManagement.class })
@ConditionalOnMissingBean(value = AopTransactionManagementAutoConfiguration.class)
@AutoConfigureAfter(value = { AopTransactionManagementAutoConfiguration.class })
public class ProxyTransactionManagementAutoConfiguration {

}
