package io.quarkus.ext.lettuce.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * https://lettuce.io/core/release/reference/index.html#redisuri.uri-syntax
 * 
 * @author Leo Tu
 */
@ConfigGroup
public class LettuceItemConfig {

    /**
     * The Connection URI
     */
    @ConfigItem
    public String uri;

    /**
     * The Connection host
     */
    @ConfigItem
    public Optional<String> host;

    /**
     * The Redis port
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * The Redis the Unix Domain Socket path.
     */
    @ConfigItem
    public Optional<String> socket;

    /**
     * The Redis password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The Redis database
     */
    @ConfigItem
    public OptionalInt database;

    /**
     * The Redis sentinel master id
     */
    @ConfigItem
    public Optional<String> sentinelMasterId;

    /**
     * The Redis client name
     */
    @ConfigItem
    public Optional<String> clientName;

    /**
     * The Redis ssl
     */
    @ConfigItem
    public Optional<Boolean> ssl;

    /**
     * The Redis startTls
     */
    @ConfigItem
    public Optional<Boolean> startTls;

    /**
     * The Redis verify peer
     */
    @ConfigItem
    public Optional<Boolean> verifyPeer;

    /**
     * The Redis the command timeout for synchronous command execution.
     */
    @ConfigItem
    public Optional<Duration> timeout;

    @Override
    public String toString() {
        return super.toString().toString() + "[uri=" + uri + ", host=" + host + ", port=" + port + ", socket=" + socket
                + ", password=" + password + ", database=" + database + ", sentinelMasterId=" + sentinelMasterId
                + ", clientName=" + clientName + ", ssl=" + ssl + ", startTls=" + startTls + ", verifyPeer="
                + verifyPeer + ", timeout=" + timeout + "]";
    }

}
