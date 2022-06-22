package org.coldis.library.persistence.bean;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Static context loader.
 */
@Component
public class StaticContextAccessor {

	/**
	 * Instance.
	 */
	private static StaticContextAccessor instance;

	/**
	 * Application context.
	 */
	@Autowired
	private ApplicationContext applicationContext;

	/**
	 * Instantiates the accessor.
	 */
	@PostConstruct
	public void registerInstance() {
		StaticContextAccessor.instance = this;
	}

	/**
	 * Gets a bean.
	 *
	 * @param  <T>   Type.
	 * @param  clazz Class.
	 * @return       The bean.
	 */
	public static <T> T getBean(
			final Class<T> clazz) {
		return StaticContextAccessor.instance.applicationContext.getBean(clazz);
	}

	/**
	 * Gets a bean.
	 *
	 * @param  beanName Bean name.
	 * @return          The bean.
	 */
	public static Object getBean(
			final String beanName) {
		return StaticContextAccessor.instance.applicationContext.getBean(beanName);
	}

}
