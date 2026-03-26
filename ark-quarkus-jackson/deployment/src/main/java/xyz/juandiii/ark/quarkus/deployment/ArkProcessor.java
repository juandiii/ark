package xyz.juandiii.ark.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import xyz.juandiii.ark.quarkus.ArkProducer;
import xyz.juandiii.ark.quarkus.ArkRecorder;

import java.util.List;

/**
 * Quarkus deployment processor for the Ark HTTP client extension.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkProcessor {

    private static final String FEATURE = "ark-jackson";
    private static final DotName ARK_CLIENT = DotName.createSimple("xyz.juandiii.ark.proxy.RegisterArkClient");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.unremovableOf(ArkProducer.class);
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

    @BuildStep
    List<NativeImageProxyDefinitionBuildItem> registerArkProxyInterfaces(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        return index.getAnnotations(ARK_CLIENT).stream()
                .map(AnnotationInstance::target)
                .map(target -> target.asClass())
                .map(ClassInfo::name)
                .map(name -> new NativeImageProxyDefinitionBuildItem(name.toString()))
                .toList();
    }

    @BuildStep
    ReflectiveClassBuildItem proxyProviderClasses() {
        return ReflectiveClassBuildItem.builder(
                "xyz.juandiii.ark.proxy.SyncExecutionModelProvider",
                "xyz.juandiii.ark.proxy.jaxrs.JaxRsProxyProvider",
                "xyz.juandiii.ark.proxy.jaxrs.JaxRsAnnotationResolver",
                "xyz.juandiii.ark.proxy.jaxrs.JaxRsParameterBinder",
                "xyz.juandiii.ark.mutiny.proxy.MutinyExecutionModelProvider",
                "xyz.juandiii.ark.mutiny.proxy.MutinyDispatchers",
                "xyz.juandiii.ark.mutiny.proxy.MutinyReturnTypeHandler"
        ).constructors(true).methods(true).build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createArkClientBeans(ArkRecorder recorder,
                              CombinedIndexBuildItem combinedIndex,
                              BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        IndexView index = combinedIndex.getIndex();

        for (AnnotationInstance instance : index.getAnnotations(ARK_CLIENT)) {
            ClassInfo classInfo = instance.target().asClass();
            String baseUrl = stringValue(instance, "baseUrl", "");
            if (baseUrl.isEmpty()) continue;

            String httpVersion = enumValue(instance, "httpVersion", "HTTP_1_1");
            int connectTimeout = intValue(instance, "connectTimeout", 10);
            int readTimeout = intValue(instance, "readTimeout", 30);
            String className = classInfo.name().toString();

            syntheticBeans.produce(
                    SyntheticBeanBuildItem.configure(DotName.createSimple(className))
                            .scope(ApplicationScoped.class)
                            .unremovable()
                            .setRuntimeInit()
                            .supplier(recorder.createArkClient(
                                    className, baseUrl, httpVersion,
                                    connectTimeout, readTimeout))
                            .done()
            );
        }
    }

    private static String stringValue(AnnotationInstance instance, String name, String defaultValue) {
        AnnotationValue value = instance.value(name);
        return value != null ? value.asString() : defaultValue;
    }

    private static String enumValue(AnnotationInstance instance, String name, String defaultValue) {
        AnnotationValue value = instance.value(name);
        return value != null ? value.asEnum() : defaultValue;
    }

    private static int intValue(AnnotationInstance instance, String name, int defaultValue) {
        AnnotationValue value = instance.value(name);
        return value != null ? value.asInt() : defaultValue;
    }
}
