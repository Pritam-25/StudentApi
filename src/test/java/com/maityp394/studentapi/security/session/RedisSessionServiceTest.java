package com.maityp394.studentapi.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maityp394.studentapi.config.properties.RedisSessionProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import tools.jackson.databind.ObjectMapper;

@DisplayName("RedisSessionService Unit Tests")
class RedisSessionServiceTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private SetOperations<String, String> setOperations;
  private ObjectMapper objectMapper;
  private RedisSessionProperties sessionProperties;

  @SuppressWarnings("unchecked")
  private RedisScript<Long> script = mock(RedisScript.class);

  private RedisSessionService sessionService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    setOperations = mock(SetOperations.class);
    objectMapper = new ObjectMapper();
    sessionProperties = new RedisSessionProperties(604800L, 2592000L);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);

    sessionService =
        new RedisSessionService(redisTemplate, objectMapper, sessionProperties, script);
  }

  @Test
  @DisplayName("createSession should prune expired session IDs and set user sessions TTL")
  void shouldPruneExpiredSessionsAndSetTtlOnCreate() {
    UUID userId = UUID.randomUUID();
    UUID newSessionId = UUID.randomUUID();
    UUID activeSessionId = UUID.randomUUID();
    UUID expiredSessionId = UUID.randomUUID();

    String userKey = "user_sessions:" + userId;
    when(setOperations.members(userKey))
        .thenReturn(Set.of(activeSessionId.toString(), expiredSessionId.toString()));
    when(redisTemplate.hasKey("session:" + activeSessionId)).thenReturn(true);
    when(redisTemplate.hasKey("session:" + expiredSessionId)).thenReturn(false);

    UserSession created =
        sessionService.createSession(newSessionId, userId, "dummyHash", "Mozilla/5.0", "127.0.0.1");

    assertThat(created).isNotNull();
    assertThat(created.getSessionId()).isEqualTo(newSessionId);

    // Verify pruning of expired session
    verify(setOperations)
        .remove(eq(userKey), (Object[]) eq(new String[] {expiredSessionId.toString()}));

    // Verify adding new session
    verify(setOperations).add(userKey, newSessionId.toString());

    // Verify setting/refreshing TTL on user sessions set
    verify(redisTemplate).expire(userKey, Duration.ofSeconds(2592000L));

    // Verify saving session with idle timeout
    verify(valueOperations)
        .set(eq("session:" + newSessionId), anyString(), eq(Duration.ofSeconds(604800L)));
  }

  @Test
  @DisplayName("createSession should not prune when user has no existing sessions")
  void shouldHandleEmptyExistingSessions() {
    UUID userId = UUID.randomUUID();
    UUID newSessionId = UUID.randomUUID();

    String userKey = "user_sessions:" + userId;
    when(setOperations.members(userKey)).thenReturn(Collections.emptySet());

    UserSession created =
        sessionService.createSession(newSessionId, userId, "dummyHash", "Mozilla/5.0", "127.0.0.1");

    assertThat(created).isNotNull();
    verify(setOperations, never()).remove(eq(userKey), any(Object[].class));
    verify(setOperations).add(userKey, newSessionId.toString());
    verify(redisTemplate).expire(userKey, Duration.ofSeconds(2592000L));
  }
}
