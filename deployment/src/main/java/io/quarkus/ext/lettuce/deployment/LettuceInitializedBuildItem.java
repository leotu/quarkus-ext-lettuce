package io.quarkus.ext.lettuce.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item indicating the Lettuce has been fully initialized.
 * 
 * @author Leo Tu
 */
public final class LettuceInitializedBuildItem extends SimpleBuildItem {

    public LettuceInitializedBuildItem() {
    }
}
