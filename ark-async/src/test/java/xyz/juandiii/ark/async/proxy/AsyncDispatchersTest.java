package xyz.juandiii.ark.async.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.juandiii.ark.async.AsyncArk;
import xyz.juandiii.ark.async.http.AsyncClientRequest;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.proxy.RequestDispatcher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncDispatchersTest {

    @Mock AsyncArk ark;
    @Mock AsyncClientRequest request;

    @Test
    void givenGet_thenDispatches() {
        when(ark.get("/path")).thenReturn(request);
        RequestDispatcher dispatcher = AsyncDispatchers.async(ark);
        assertSame(request, dispatcher.dispatch("GET", "/path"));
    }

    @Test
    void givenPost_thenDispatches() {
        when(ark.post("/path")).thenReturn(request);
        assertSame(request, AsyncDispatchers.async(ark).dispatch("POST", "/path"));
    }

    @Test
    void givenPut_thenDispatches() {
        when(ark.put("/path")).thenReturn(request);
        assertSame(request, AsyncDispatchers.async(ark).dispatch("PUT", "/path"));
    }

    @Test
    void givenPatch_thenDispatches() {
        when(ark.patch("/path")).thenReturn(request);
        assertSame(request, AsyncDispatchers.async(ark).dispatch("PATCH", "/path"));
    }

    @Test
    void givenDelete_thenDispatches() {
        when(ark.delete("/path")).thenReturn(request);
        assertSame(request, AsyncDispatchers.async(ark).dispatch("DELETE", "/path"));
    }

    @Test
    void givenUnsupported_thenThrows() {
        assertThrows(ArkException.class, () ->
                AsyncDispatchers.async(ark).dispatch("OPTIONS", "/path"));
    }
}