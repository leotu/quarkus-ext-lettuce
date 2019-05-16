package io.quarkus.ext.lettuce.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item indicating the QuerySQL has been fully initialized.
 */
public final class LettuceInitializedBuildItem extends SimpleBuildItem {

    public LettuceInitializedBuildItem() {
    }
}
