package com.maityp394.studentapi.config;

import com.maityp394.studentapi.config.properties.CacheProperties;
import com.maityp394.studentapi.dto.response.StudentResponse;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/** Configuration class setting up Redis-backed caching with Jackson 3 serialization. */
@Configuration
@EnableCaching
public class CacheConfig {

  /** Cache name for student profiles. */
  public static final String STUDENT_PROFILE_CACHE = "student-profile";

  /**
   * Configures the transaction-aware {@link RedisCacheManager} with JSON value serialization.
   *
   * @param connectionFactory the Redis connection factory
   * @param objectMapper the application-configured Jackson 3 object mapper
   * @param cacheProperties the strongly typed cache configuration properties
   * @return transaction-aware {@link RedisCacheManager}
   */
  @Bean
  @ConditionalOnMissingBean(CacheManager.class)
  public RedisCacheManager cacheManager(
      RedisConnectionFactory connectionFactory,
      ObjectMapper objectMapper,
      CacheProperties cacheProperties) {

    Duration ttl = cacheProperties.timeToLive();

    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .disableCachingNullValues()
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                SerializationPair.fromSerializer(
                    new GenericJacksonJsonRedisSerializer(objectMapper)));

    RedisCacheConfiguration studentProfileConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .disableCachingNullValues()
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                SerializationPair.fromSerializer(
                    new JacksonJsonRedisSerializer<>(objectMapper, StudentResponse.class)));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withCacheConfiguration(STUDENT_PROFILE_CACHE, studentProfileConfig)
        .transactionAware()
        .enableStatistics()
        .build();
  }
}
