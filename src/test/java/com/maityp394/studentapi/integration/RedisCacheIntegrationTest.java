package com.maityp394.studentapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.maityp394.studentapi.config.CacheConfig;
import com.maityp394.studentapi.config.properties.CacheProperties;
import com.maityp394.studentapi.dto.response.StudentResponse;
import com.maityp394.studentapi.entity.Responsibility;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Integration test verifying real Redis caching behavior, physical key structure, JSON value
 * serialization (avoiding JDK binary serialization), and cache lifecycle.
 */
@SpringBootTest(
    classes = RedisCacheIntegrationTest.TestConfig.class,
    properties = {
      "spring.data.redis.host=localhost",
      "spring.data.redis.port=6379",
      "spring.data.redis.password=redispassword",
      "spring.cache.type=redis",
      "spring.cache.redis.time-to-live=15m",
      "app.cache.time-to-live=15m"
    })
class RedisCacheIntegrationTest {

  @Configuration
  @EnableAutoConfiguration
  @Import(CacheConfig.class)
  @EnableConfigurationProperties(CacheProperties.class)
  static class TestConfig {}

  @Autowired private RedisCacheManager cacheManager;
  @Autowired private RedisConnectionFactory redisConnectionFactory;
  @Autowired private StringRedisTemplate stringRedisTemplate;

  @BeforeEach
  void verifyRedisConnectivity() {
    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isSynchronizationActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager
          .clearSynchronization();
    }
    org.springframework.transaction.support.TransactionSynchronizationManager.clear();
    try {
      redisConnectionFactory.getConnection().ping();
    } catch (Exception e) {
      Assumptions.abort(
          "Real Redis server is not reachable on localhost:6379, skipping test: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("StudentResponse is serialized as JSON in Redis and can be deserialized accurately")
  void studentResponse_StoredAsJsonInRedis_AndRetrievedSuccessfully() {
    Cache cache = cacheManager.getCache(CacheConfig.STUDENT_PROFILE_CACHE);
    assertThat(cache).isNotNull();

    UUID studentId = UUID.randomUUID();
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    StudentResponse original =
        new StudentResponse(
            studentId, "Pritam Maity", "pritam@example.com", Responsibility.STUDENT, now, now);

    try {
      // 1. Store in cache via Spring Cache abstraction
      cache.put(studentId, original);

      // 2. Verify raw Redis storage format and key pattern
      String redisKey = CacheConfig.STUDENT_PROFILE_CACHE + "::" + studentId;
      String rawJson = stringRedisTemplate.opsForValue().get(redisKey);

      assertThat(rawJson).isNotNull();
      // Verify it is NOT JDK binary serialization
      assertThat(rawJson).doesNotContain("\u00ac\u00ed");
      // Verify valid JSON content
      assertThat(rawJson).contains("\"id\":\"" + studentId + "\"");
      assertThat(rawJson).contains("\"email\":\"pritam@example.com\"");
      assertThat(rawJson).contains("\"responsibility\":\"STUDENT\"");

      // 3. Verify deserialization through Spring Cache abstraction
      StudentResponse retrieved = cache.get(studentId, StudentResponse.class);
      assertThat(retrieved).isNotNull();
      assertThat(retrieved.id()).isEqualTo(original.id());
      assertThat(retrieved.name()).isEqualTo(original.name());
      assertThat(retrieved.email()).isEqualTo(original.email());
      assertThat(retrieved.responsibility()).isEqualTo(original.responsibility());

      // 4. Verify eviction removes the key from Redis
      if (org.springframework.transaction.support.TransactionSynchronizationManager
          .isSynchronizationActive()) {
        org.springframework.transaction.support.TransactionSynchronizationManager
            .clearSynchronization();
      }
      cache.evict(studentId);
      String afterEviction = stringRedisTemplate.opsForValue().get(redisKey);
      if (afterEviction != null) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
        afterEviction = stringRedisTemplate.opsForValue().get(redisKey);
      }
      assertThat(afterEviction).isNull();
    } finally {
      if (org.springframework.transaction.support.TransactionSynchronizationManager
          .isSynchronizationActive()) {
        org.springframework.transaction.support.TransactionSynchronizationManager
            .clearSynchronization();
      }
      cache.evict(studentId);
    }
  }

  @org.junit.jupiter.api.AfterEach
  void cleanupSynchronization() {
    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isSynchronizationActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager
          .clearSynchronization();
    }
    org.springframework.transaction.support.TransactionSynchronizationManager.clear();
  }
}
