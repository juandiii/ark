package xyz.juandiii.ark.reactor.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.core.exceptions.ArkException;
import xyz.juandiii.ark.core.proxy.RequestDispatcher;
import xyz.juandiii.ark.reactor.ReactorArk;
import xyz.juandiii.ark.reactor.http.ReactorClientRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactorDispatchersTest {

    @Mock ReactorArk ark;
    @Mock ReactorClientRequest request;

    @Test
    void givenGet_thenDispatches() {
        when(ark.get("/path")).thenReturn(request);
        RequestDispatcher dispatcher = ReactorDispatchers.reactor(ark);
        assertSame(request, dispatcher.dispatch("GET", "/path"));
    }

    @Test
    void givenPost_thenDispatches() {
        when(ark.post("/path")).thenReturn(request);
        assertSame(request, ReactorDispatchers.reactor(ark).dispatch("POST", "/path"));
    }

    @Test
    void givenPut_thenDispatches() {
        when(ark.put("/path")).thenReturn(request);
        assertSame(request, ReactorDispatchers.reactor(ark).dispatch("PUT", "/path"));
    }

    @Test
    void givenPatch_thenDispatches() {
        when(ark.patch("/path")).thenReturn(request);
        assertSame(request, ReactorDispatchers.reactor(ark).dispatch("PATCH", "/path"));
    }

    @Test
    void givenDelete_thenDispatches() {
        when(ark.delete("/path")).thenReturn(request);
        assertSame(request, ReactorDispatchers.reactor(ark).dispatch("DELETE", "/path"));
    }

    @Test
    void givenUnsupported_thenThrows() {
        assertThrows(ArkException.class, () ->
                ReactorDispatchers.reactor(ark).dispatch("OPTIONS", "/path"));
    }
}