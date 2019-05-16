package io.quarkus.ext.lettuce.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * 
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 */
@ConfigGroup
public class LettuceItemConfig {

    /**
     * The Connection URI
     */
    @ConfigItem
    public String uri;

    /**
     * The Redis password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The Redis database
     */
    @ConfigItem
    public Optional<Integer> database;

    @Override
    public String toString() {
        return super.toString().toString() + "[uri=" + uri + ", password=" + password + ", database=" + database + "]";
    }

}
