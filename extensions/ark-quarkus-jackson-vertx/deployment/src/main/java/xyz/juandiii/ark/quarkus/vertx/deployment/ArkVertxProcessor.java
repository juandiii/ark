package xyz.juandiii.ark.quarkus.vertx.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import xyz.juandiii.ark.quarkus.deployment.ArkProcessor;
import xyz.juandiii.ark.quarkus.vertx.ArkVertxProducer;
import xyz.juandiii.ark.quarkus.vertx.ArkVertxRecorder;
import xyz.juandiii.ark.quarkus.vertx.QuarkusVertxTlsResolver;

/**
 * Quarkus deployment processor for the Ark Vert.x Mutiny transport add-on.
 * Registers Mutiny CDI producers, Vertx TLS resolver, native image hints for the
 * Mutiny proxy provider classes, and synthetic beans for @RegisterArkClient
 * interfaces whose methods return Uni/Multi.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkVertxProcessor {

    private static final String FEATURE = "ark-jackson-vertx";
    private static final DotName ARK_CLIENT = DotName.createSimple("xyz.juandiii.ark.core.proxy.RegisterArkClient");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerVertxBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        ArkVertxProducer.class,
                        QuarkusVertxTlsResolver.class
                )
                .setUnremovable()
                .build();
    }

    @BuildStep
    NativeImageResourceBuildItem vertxNativeResources() {
        return new NativeImageResourceBuildItem(
                "META-INF/vertx/vertx-version.txt",
                "vertx-version.txt"
        );
    }

    @BuildStep
    ReflectiveClassBuildItem mutinyProxyProviderClasses() {
        return ReflectiveClassBuildItem.builder(
                "xyz.juandiii.ark.mutiny.proxy.MutinyExecutionModelProvider",
                "xyz.juandiii.ark.mutiny.proxy.MutinyDispatchers",
                "xyz.juandiii.ark.mutiny.proxy.MutinyReturnTypeHandler"
        ).constructors(true)
                .methods(true)
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createReactiveArkClientBeans(ArkVertxRecorder recorder,
                                       CombinedIndexBuildItem combinedIndex,
                                       BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        IndexView index = combinedIndex.getIndex();

        for (AnnotationInstance instance : index.getAnnotations(ARK_CLIENT)) {
            ClassInfo classInfo = instance.target().asClass();
            // Only register interfaces with Uni/Multi return types — non-reactive
            // interfaces are registered by the slim ark-quarkus-jackson extension.
            if (!ArkProcessor.hasReactiveReturnType(classInfo)) continue;
            String className = classInfo.name().toString();
            String configKey = ArkProcessor.stringValue(instance, "configKey", "");

            syntheticBeans.produce(
                    SyntheticBeanBuildItem.configure(DotName.createSimple(className))
                            .scope(ApplicationScoped.class)
                            .unremovable()
                            .setRuntimeInit()
                            .supplier(recorder.createMutinyArkClient(className, configKey))
                            .done()
            );
        }
    }
}
