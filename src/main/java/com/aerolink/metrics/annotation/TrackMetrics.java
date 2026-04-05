package com.aerolink.metrics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for automated metric tracking. Used by {@link
 * com.aerolink.metrics.aspect.MetricTrackingAspect}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackMetrics {
  /** Optional name for the operation being tracked. */
  String value() default "";
}
