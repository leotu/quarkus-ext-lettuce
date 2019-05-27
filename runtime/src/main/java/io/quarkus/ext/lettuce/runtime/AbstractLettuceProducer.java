package io.quarkus.ext.lettuce.runtime;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Qualifier;

import org.jboss.logging.Logger;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;

/**
 * Produces Lettuce RedisClient
 * 
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 */
public abstract class AbstractLettuceProducer {
    private static final Logger log = Logger.getLogger(AbstractLettuceProducer.class);

    private LettuceConfig lettuceConfig;

    public LettuceRedisClient create(Optional<LettuceItemConfig> itemConfigOptional) {
        Objects.requireNonNull(itemConfigOptional, "itemConfigOptional");
        if (!itemConfigOptional.isPresent()) {
            throw new RuntimeException("!itemConfigOptional.isPresent()");
        }

        LettuceItemConfig itemConfig = itemConfigOptional.get();
        
        RedisURI redisURI = RedisURI.create(itemConfig.uri);
        itemConfig.host.ifPresent(redisURI::setHost);
        itemConfig.port.ifPresent(redisURI::setPort);
        itemConfig.socket.ifPresent(redisURI::setSocket);
        itemConfig.password.ifPresent(redisURI::setPassword);
        itemConfig.database.ifPresent(redisURI::setDatabase);
        itemConfig.sentinelMasterId.ifPresent(redisURI::setSentinelMasterId);
        itemConfig.clientName.ifPresent(redisURI::setClientName);
        itemConfig.ssl.ifPresent(redisURI::setSsl);
        itemConfig.startTls.ifPresent(redisURI::setStartTls);
        itemConfig.verifyPeer.ifPresent(redisURI::setVerifyPeer);
        itemConfig.timeout.ifPresent(redisURI::setTimeout);
        try {
            RedisClient client = RedisClient.create(redisURI);
            return new LettuceRedisClient(client);
        } catch (Exception e) {
            log.error(itemConfig, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Build Time
     */
    public void setBuildTimeLettuceConfig(LettuceConfig lettuceConfig) {
        this.lettuceConfig = lettuceConfig;
    }

    /**
     * Runtime
     */
    public Optional<LettuceItemConfig> getDefaultItemConfig() {
        if (lettuceConfig.defaultConfig.uri == null || lettuceConfig.defaultConfig.uri.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(lettuceConfig.defaultConfig);
        }
    }

    /**
     * Runtime
     */
    public Optional<LettuceItemConfig> getNamedItemConfig(String name) {
        return Optional.ofNullable(lettuceConfig.namedConfig.get(name));
    }

    /**
     * CDI: Ambiguous dependencies
     */
    @Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Qualifier
    static public @interface RedisClientQualifier {

        String value();
    }
}
