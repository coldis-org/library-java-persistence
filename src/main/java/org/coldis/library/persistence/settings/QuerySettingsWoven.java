package org.coldis.library.persistence.settings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker the AspectJ weaver adds (through {@code QuerySettingsWovenMarker}, next to {@link QuerySettingsAspect}) to every
 * {@link QuerySettings}-annotated class and method it weaves. The startup check uses it to fail when
 * an annotated bean was compiled without the aspect, which would otherwise ignore the annotation
 * silently. Not meant to be written by hand.
 */
@Documented
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface QuerySettingsWoven {

}
