package xyz.juandiii.ark.mutiny.http;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.http.ArkResponse;

import java.util.List;

/**
 * Interface for Mutiny response extraction.
 *
 * @author Juan Diego Lopez V.
 */
public interface MutinyClientResponse {

    <T> Uni<T> body(TypeRef<T> type);

    <T> Uni<T> body(Class<T> type);

    @SuppressWarnings("unchecked")
    default <T> Multi<T> bodyAsMulti(TypeRef<T> type) {
        return ((Uni<List<T>>) (Uni<?>) body(TypeRef.ofList(type)))
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @SuppressWarnings("unchecked")
    default <T> Multi<T> bodyAsMulti(Class<T> type) {
        return ((Uni<List<T>>) (Uni<?>) body(TypeRef.ofList(type)))
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    <T> Uni<ArkResponse<T>> toEntity(TypeRef<T> type);

    <T> Uni<ArkResponse<T>> toEntity(Class<T> type);

    Uni<ArkResponse<Void>> toBodilessEntity();
}
