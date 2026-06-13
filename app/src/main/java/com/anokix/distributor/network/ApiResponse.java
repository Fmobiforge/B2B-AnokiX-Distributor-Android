package com.anokix.distributor.network;

/**
 * Envelope every endpoint returns: { "status": bool, "message": str, "data": {...} }.
 */
public class ApiResponse<T> {
    public boolean status;
    public String message;
    public T data;
}
