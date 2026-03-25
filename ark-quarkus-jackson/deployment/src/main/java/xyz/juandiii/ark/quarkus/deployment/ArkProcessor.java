package xyz.juandiii.ark.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Quarkus deployment processor for the Ark HTTP client extension.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkProcessor {

    private static final String FEATURE = "ark-jackson";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem(
                "ark-version.properties",
                "META-INF/vertx/vertx-version.txt",
                "vertx-version.txt"
        );
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return ReflectiveClassBuildItem.builder(
                "xyz.juandiii.ark.TypeRef",
                "xyz.juandiii.ark.exceptions.ApiException",
                "xyz.juandiii.ark.exceptions.ArkException",
                "xyz.juandiii.ark.http.RawResponse"
        ).methods(true).fields(true).build();
    }
}
