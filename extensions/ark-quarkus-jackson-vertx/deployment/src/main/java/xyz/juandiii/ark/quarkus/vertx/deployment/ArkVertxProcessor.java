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
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
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
    private static final DotName MUTINY_UNI = DotName.createSimple("io.smallrye.mutiny.Uni");
    private static final DotName MUTINY_MULTI = DotName.createSimple("io.smallrye.mutiny.Multi");

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
            if (!hasReactiveReturnType(classInfo)) continue;
            String className = classInfo.name().toString();
            String configKey = stringValue(instance, "configKey", "");

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

    private static boolean hasReactiveReturnType(ClassInfo classInfo) {
        for (MethodInfo method : classInfo.methods()) {
            DotName returnName = method.returnType().name();
            if (MUTINY_UNI.equals(returnName) || MUTINY_MULTI.equals(returnName)) {
                return true;
            }
        }
        return false;
    }

    private static String stringValue(AnnotationInstance instance, String name, String defaultValue) {
        AnnotationValue value = instance.value(name);
        return value != null ? value.asString() : defaultValue;
    }
}
