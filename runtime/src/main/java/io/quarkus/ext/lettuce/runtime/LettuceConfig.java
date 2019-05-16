package io.quarkus.ext.lettuce.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Read from application.properties file
 * 
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 *
 */
@ConfigRoot(name = "lettuce", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LettuceConfig {

    /**
     * The default config.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public LettuceItemConfig defaultConfig;

    /**
     * Additional configs.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, LettuceItemConfig> namedConfig;

}
