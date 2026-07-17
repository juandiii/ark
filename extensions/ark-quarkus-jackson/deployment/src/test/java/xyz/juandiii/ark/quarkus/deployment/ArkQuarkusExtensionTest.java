package xyz.juandiii.ark.quarkus.deployment;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import xyz.juandiii.ark.core.ArkClient;
import xyz.juandiii.ark.core.JsonSerializer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ArkQuarkusExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    JsonSerializer jsonSerializer;

    @Inject
    ArkClient.Builder arkClientBuilder;

    @Inject
    ArkClient.Builder anotherArkClientBuilder;

    @Test
    void givenExtension_whenStarted_thenJsonSerializerIsProduced() {
        assertNotNull(jsonSerializer);
    }

    @Test
    void givenExtension_whenStarted_thenArkClientBuilderIsProduced() {
        assertNotNull(arkClientBuilder);
    }

    @Test
    void givenTwoInjections_whenCompared_thenBuildersAreDifferentInstances() {
        assertNotSame(arkClientBuilder, anotherArkClientBuilder);
    }

    @Test
    void givenSyncBuilder_whenBuild_thenCreatesClient() {
        var client = arkClientBuilder.baseUrl("http://localhost:8080").build();
        assertNotNull(client);
    }
}
