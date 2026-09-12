package com.maityp394.studentapi.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * In-memory test configuration providing mock {@link RedisConnectionFactory} and {@link
 * StringRedisTemplate} beans for fast, isolated integration testing without an external Redis
 * server.
 */
@TestConfiguration
public class TestRedisConfig {

  private final Map<String, String> stringStore = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> setStore = new ConcurrentHashMap<>();

  public void clear() {
    stringStore.clear();
    setStore.clear();
  }

  @Bean
  @Primary
  public RedisConnectionFactory redisConnectionFactory() {
    return mock(RedisConnectionFactory.class);
  }

  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public StringRedisTemplate stringRedisTemplate() {
    StringRedisTemplate template = mock(StringRedisTemplate.class);
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    SetOperations<String, String> setOps = mock(SetOperations.class);

    when(template.opsForValue()).thenReturn(valueOps);
    when(template.opsForSet()).thenReturn(setOps);

    // Value operations
    doAnswer(
            inv -> {
              String key = inv.getArgument(0);
              String val = inv.getArgument(1);
              stringStore.put(key, val);
              return null;
            })
        .when(valueOps)
        .set(anyString(), anyString(), any(java.time.Duration.class));

    when(valueOps.get(anyString()))
        .thenAnswer(
            inv -> {
              String key = inv.getArgument(0);
              return stringStore.get(key);
            });

    when(valueOps.getAndDelete(anyString()))
        .thenAnswer(
            inv -> {
              String key = inv.getArgument(0);
              return stringStore.remove(key);
            });

    // Set operations
    when(setOps.add(anyString(), any()))
        .thenAnswer(
            inv -> {
              String key = inv.getArgument(0);
              Object arg1 = inv.getArgument(1);
              if (arg1 instanceof String[] arr) {
                setStore
                    .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                    .addAll(List.of(arr));
                return (long) arr.length;
              } else if (arg1 instanceof Object[] arr) {
                for (Object o : arr) {
                  if (o != null) {
                    setStore
                        .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                        .add(o.toString());
                  }
                }
                return (long) arr.length;
              } else if (arg1 != null) {
                setStore
                    .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                    .add(arg1.toString());
                return 1L;
              }
              return 0L;
            });

    when(setOps.members(anyString()))
        .thenAnswer(
            inv -> {
              String key = inv.getArgument(0);
              Set<String> set = setStore.get(key);
              return set != null ? new HashSet<>(set) : Collections.emptySet();
            });

    when(setOps.remove(anyString(), any()))
        .thenAnswer(
            inv -> {
              String key = inv.getArgument(0);
              Object arg1 = inv.getArgument(1);
              Set<String> set = setStore.get(key);
              if (set != null) {
                if (arg1 instanceof Object[] arr) {
                  for (Object o : arr) {
                    set.remove(o);
                  }
                } else if (arg1 != null) {
                  set.remove(arg1.toString());
                }
              }
              return 1L;
            });

    // Delete operations
    when(template.delete(anyString()))
        .thenAnswer(
            inv -> {
              String key = inv.getArgument(0);
              stringStore.remove(key);
              setStore.remove(key);
              return true;
            });

    when(template.delete(anyCollection()))
        .thenAnswer(
            inv -> {
              Collection<String> keys = inv.getArgument(0);
              for (String k : keys) {
                stringStore.remove(k);
                setStore.remove(k);
              }
              return (long) keys.size();
            });

    // Script execution (simulates atomic rotate Lua script)
    when(template.execute(
            any(RedisScript.class), any(List.class), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              List<String> keys = inv.getArgument(1);
              String key = keys.getFirst();
              String expectedOldHash = inv.getArgument(2);
              String newHash = inv.getArgument(3);
              String raw = stringStore.get(key);
              if (raw == null) {
                return -1L;
              }
              if (raw.contains(expectedOldHash)) {
                String updated = raw.replace(expectedOldHash, newHash);
                stringStore.put(key, updated);
                return 1L;
              } else {
                return 0L; // replay detected
              }
            });

    return template;
  }
}
