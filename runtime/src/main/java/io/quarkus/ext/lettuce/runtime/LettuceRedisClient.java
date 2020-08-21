package io.quarkus.ext.lettuce.runtime;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;

/**
 * Wrapper for RedisClient default constructor NPE error
 * 
 * @author Leo Tu
 */
public class LettuceRedisClient {
    private RedisClient delegate;

    public LettuceRedisClient() {
    }

    public LettuceRedisClient(RedisClient delegate) {
        this.delegate = delegate;
    }

    public RedisClient getDelegate() {
        return delegate;
    }

    public void setDefaultTimeout(Duration timeout) {
        delegate.setDefaultTimeout(timeout);
    }

    @Deprecated
    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        delegate.setDefaultTimeout(timeout, unit);
    }

    public StatefulRedisConnection<String, String> connect() {
        return delegate.connect();
    }

    public <K, V> StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec) {
        return delegate.connect(codec);
    }

    public StatefulRedisConnection<String, String> connect(RedisURI redisURI) {
        return delegate.connect(redisURI);
    }

    public <K, V> StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec, RedisURI redisURI) {
        return delegate.connect(codec, redisURI);
    }

    public <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {
        return delegate.connectAsync(codec, redisURI);
    }

    public StatefulRedisPubSubConnection<String, String> connectPubSub() {
        return delegate.connectPubSub();
    }

    public StatefulRedisPubSubConnection<String, String> connectPubSub(RedisURI redisURI) {
        return delegate.connectPubSub(redisURI);
    }

    public void shutdown() {
        delegate.shutdown();
    }

    public <K, V> StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec) {
        return delegate.connectPubSub(codec);
    }

    public void shutdown(Duration quietPeriod, Duration timeout) {
        delegate.shutdown(quietPeriod, timeout);
    }

    public <K, V> StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec, RedisURI redisURI) {
        return delegate.connectPubSub(codec, redisURI);
    }

    public void shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
        delegate.shutdown(quietPeriod, timeout, timeUnit);
    }

    public <K, V> ConnectionFuture<StatefulRedisPubSubConnection<K, V>> connectPubSubAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {
        return delegate.connectPubSubAsync(codec, redisURI);
    }

    public CompletableFuture<Void> shutdownAsync() {
        return delegate.shutdownAsync();
    }

    public CompletableFuture<Void> shutdownAsync(long quietPeriod, long timeout, TimeUnit timeUnit) {
        return delegate.shutdownAsync(quietPeriod, timeout, timeUnit);
    }

    public StatefulRedisSentinelConnection<String, String> connectSentinel() {
        return delegate.connectSentinel();
    }

    public <K, V> StatefulRedisSentinelConnection<K, V> connectSentinel(RedisCodec<K, V> codec) {
        return delegate.connectSentinel(codec);
    }

    public StatefulRedisSentinelConnection<String, String> connectSentinel(RedisURI redisURI) {
        return delegate.connectSentinel(redisURI);
    }

    public <K, V> StatefulRedisSentinelConnection<K, V> connectSentinel(RedisCodec<K, V> codec, RedisURI redisURI) {
        return delegate.connectSentinel(codec, redisURI);
    }

    public void addListener(RedisConnectionStateListener listener) {
        delegate.addListener(listener);
    }

    public <K, V> CompletableFuture<StatefulRedisSentinelConnection<K, V>> connectSentinelAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {
        return delegate.connectSentinelAsync(codec, redisURI);
    }

    public void removeListener(RedisConnectionStateListener listener) {
        delegate.removeListener(listener);
    }

    public ClientOptions getOptions() {
        return delegate.getOptions();
    }

    public void setOptions(ClientOptions clientOptions) {
        delegate.setOptions(clientOptions);
    }

    public ClientResources getResources() {
        return delegate.getResources();
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public String toString() {
        return delegate.toString();
    }

}
