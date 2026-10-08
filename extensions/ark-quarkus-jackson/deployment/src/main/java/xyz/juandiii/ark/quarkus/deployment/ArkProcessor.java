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
import org.jboss.jandex.*;
import xyz.juandiii.ark.quarkus.ArkClients;
import xyz.juandiii.ark.quarkus.ArkProducer;
import xyz.juandiii.ark.quarkus.ArkRecorder;
import xyz.juandiii.ark.quarkus.QuarkusTlsResolver;

import java.util.List;

/**
 * Quarkus deployment processor for the Ark HTTP client extension.
 *
 * @author Juan Diego Lopez V.
 */
public class ArkProcessor {

    private static final String FEATURE = "ark-jackson";
    private static final DotName ARK_CLIENT = DotName.createSimple("xyz.juandiii.ark.core.proxy.RegisterArkClient");
    private static final DotName MUTINY_UNI = DotName.createSimple("io.smallrye.mutiny.Uni");
    private static final DotName MUTINY_MULTI = DotName.createSimple("io.smallrye.mutiny.Multi");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        ArkProducer.class,
                        ArkClients.class,
                        QuarkusTlsResolver.class
                )
                .setUnremovable()
                .build();
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem(
                "ark-version.properties"
        );
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return ReflectiveClassBuildItem.builder(
                "xyz.juandiii.ark.core.TypeRef",
                "xyz.juandiii.ark.core.exceptions.ApiException",
                "xyz.juandiii.ark.core.exceptions.ArkException",
                "xyz.juandiii.ark.core.http.RawResponse"
        ).methods(true).fields(true).build();
    }

    @BuildStep
    List<NativeImageProxyDefinitionBuildItem> registerArkProxyInterfaces(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        return index.getAnnotations(ARK_CLIENT)
                .stream()
                .map(AnnotationInstance::target)
                .map(AnnotationTarget::asClass)
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
                "xyz.juandiii.ark.jaxrs.JaxRsParameterBinder"
        ).constructors(true)
                .methods(true)
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createArkClientBeans(ArkRecorder recorder,
                              CombinedIndexBuildItem combinedIndex,
                              BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        IndexView index = combinedIndex.getIndex();

        for (AnnotationInstance instance : index.getAnnotations(ARK_CLIENT)) {
            ClassInfo classInfo = instance.target().asClass();
            // Skip interfaces with Uni/Multi return types — they are registered by
            // the ark-quarkus-jackson-vertx add-on's ArkVertxProcessor. If the add-on
            // is not on the classpath, the interface simply won't have a bean and
            // CDI will fail at injection time with a clear "no bean" message.
            if (hasReactiveReturnType(classInfo)) continue;
            String className = classInfo.name().toString();
            String configKey = stringValue(instance, "configKey", "");

            syntheticBeans.produce(
                    SyntheticBeanBuildItem.configure(DotName.createSimple(className))
                            .scope(ApplicationScoped.class)
                            .unremovable()
                            .setRuntimeInit()
                            .supplier(recorder.createArkClient(className, configKey))
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
