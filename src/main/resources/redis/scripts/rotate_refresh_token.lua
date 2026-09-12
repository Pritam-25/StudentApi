-- Atomic compare-and-swap refresh token rotation with theft/reuse detection
-- KEYS[1]: session key (e.g. session:<sessionId>)
-- ARGV[1]: expected current refresh token hash
-- ARGV[2]: new refresh token hash
-- ARGV[3]: ISO-8601 last activity timestamp
-- ARGV[4]: client user agent (if non-empty)
-- ARGV[5]: client ip address (if non-empty)
-- ARGV[6]: remaining TTL in seconds

local raw = redis.call('GET', KEYS[1])
if not raw then
    return -1
end

local session = cjson.decode(raw)
if session['status'] ~= 'ACTIVE' then
    return -1
end

-- If presented hash does not match current active hash, replay/theft is detected!
if session['refreshTokenHash'] ~= ARGV[1] then
    session['status'] = 'REVOKED'
    redis.call('SET', KEYS[1], cjson.encode(session), 'KEEPTTL')
    return 0
end

-- Legitimate rotation: advance hash, update timestamp and client info
session['refreshTokenHash'] = ARGV[2]
session['lastActivityAt'] = ARGV[3]

if ARGV[4] ~= '' then
    session['userAgent'] = ARGV[4]
end

if ARGV[5] ~= '' then
    session['ipAddress'] = ARGV[5]
end

redis.call('SET', KEYS[1], cjson.encode(session))

local ttl = tonumber(ARGV[6])
if ttl > 0 then
    redis.call('EXPIRE', KEYS[1], ttl)
end

return 1
