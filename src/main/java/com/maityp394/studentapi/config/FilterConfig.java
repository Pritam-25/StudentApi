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
   * Registers {@link RequestIdFilter} to intercept requests to {@code /api/*} at Order 1.
   *
   * @return the configured {@link FilterRegistrationBean}
   */
  @Bean
  public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration() {
    FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new RequestIdFilter());
    registration.addUrlPatterns("/api/*");
    registration.setOrder(1);
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    return registration;
  }

  /**
   * Registers {@link RequestLoggingFilter} to intercept requests to {@code /api/*} at Order 2.
   *
   * @return the configured {@link FilterRegistrationBean}
   */
  @Bean
  public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration() {
    FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new RequestLoggingFilter());
    registration.addUrlPatterns("/api/*");
    registration.setOrder(2);
    registration.setDispatcherTypes(DispatcherType.REQUEST);
    return registration;
  }
}
