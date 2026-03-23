package xyz.juandiii.ark.http;

import xyz.juandiii.ark.TypeRef;

/**
 * Interface for sync response extraction.
 *
 * @author Juan Diego Lopez V.
 */
public interface ClientResponse {

    <T> T body(TypeRef<T> type);

    <T> T body(Class<T> type);

    <T> ArkResponse<T> toEntity(TypeRef<T> type);

    <T> ArkResponse<T> toEntity(Class<T> type);

    ArkResponse<Void> toBodilessEntity();
}
