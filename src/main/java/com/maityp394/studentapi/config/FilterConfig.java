package com.maityp394.studentapi.config;

import com.maityp394.studentapi.filter.RequestIdFilter;
import com.maityp394.studentapi.filter.RequestLoggingFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class registering Servlet filters with explicit ordering, URL pattern mapping, and
 * dispatcher types.
 */
@Configuration
public class FilterConfig {

  /**
   * Default order of Spring Security's filter chain in the Servlet container (-100). Filters
   * ordered before this value execute prior to Spring Security checks.
   */
  private static final int SECURITY_FILTER_ORDER = -100;

  /**
   * Registers {@link RequestIdFilter} to intercept requests to {@code /api/*} before Spring
   * Security.
   *
   * @return the configured {@link FilterRegistrationBean}
   */
  @Bean
  FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration() {
    FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new RequestIdFilter());
    registration.addUrlPatterns("/api/*");
    registration.setOrder(SECURITY_FILTER_ORDER - 2);
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    return registration;
  }

  /**
   * Registers {@link RequestLoggingFilter} to intercept requests to {@code /api/*} before Spring
   * Security.
   *
   * @return the configured {@link FilterRegistrationBean}
   */
  @Bean
  FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration() {
    FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new RequestLoggingFilter());
    registration.addUrlPatterns("/api/*");
    registration.setOrder(SECURITY_FILTER_ORDER - 1);
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    return registration;
  }
}
