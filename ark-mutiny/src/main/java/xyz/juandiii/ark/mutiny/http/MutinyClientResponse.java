package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;

/**
 * Interface for Mutiny response extraction.
 *
 * @author Juan Diego Lopez V.
 */
public interface MutinyClientResponse {

    <T> Uni<T> body(TypeRef<T> type);

    <T> Uni<T> body(Class<T> type);

    <T> Uni<ArkResponse<T>> toEntity(TypeRef<T> type);

    <T> Uni<ArkResponse<T>> toEntity(Class<T> type);

    Uni<ArkResponse<Void>> toBodilessEntity();
}
