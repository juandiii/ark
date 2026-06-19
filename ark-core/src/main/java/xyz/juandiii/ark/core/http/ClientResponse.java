package xyz.juandiii.ark.core.http;

import xyz.juandiii.ark.core.TypeRef;

/**
 * Synchronous response extraction. Returned by {@link ClientRequest#retrieve()}
 * after a successful HTTP call. Methods deserialize the body or expose the full
 * response via {@code ArkResponse}.
 *
 * @author Juan Diego Lopez V.
 */
public interface ClientResponse {

    /**
     * Deserialize the response body into the given generic type.
     *
     * @param type runtime type token capturing generics (e.g. {@code TypeRef.ofList(User.class)})
     * @param <T>  target type
     * @return deserialized instance
     * @throws xyz.juandiii.ark.core.exceptions.ArkException if deserialization fails
     */
    <T> T body(TypeRef<T> type);

    /**
     * Deserialize the response body into the given class.
     *
     * @param type target Java class
     * @param <T>  target type
     * @return deserialized instance
     * @throws xyz.juandiii.ark.core.exceptions.ArkException if deserialization fails
     */
    <T> T body(Class<T> type);

    /**
     * Wrap the response into an {@code ArkResponse} (status, headers, body).
     *
     * @param type runtime type token for the body
     * @param <T>  body type
     * @return full response wrapper
     */
    <T> ArkResponse<T> toEntity(TypeRef<T> type);

    /**
     * Wrap the response into an {@code ArkResponse} (status, headers, body).
     *
     * @param type body class
     * @param <T>  body type
     * @return full response wrapper
     */
    <T> ArkResponse<T> toEntity(Class<T> type);

    /**
     * Discard the response body and return only status + headers.
     *
     * @return response wrapper with a {@code Void} body
     */
    ArkResponse<Void> toBodilessEntity();
}
