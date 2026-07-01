package com.anokix.distributorapp.network;

/**
 * Result callback delivered on the main thread.
 */
public interface ApiCallback<T> {
    void onSuccess(T data);

    void onError(String message);
}
