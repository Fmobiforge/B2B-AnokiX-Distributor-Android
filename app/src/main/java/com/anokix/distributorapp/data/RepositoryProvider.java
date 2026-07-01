package com.anokix.distributorapp.data;

public final class RepositoryProvider {

    private static DistributorRepository repository = new MockDistributorRepository();

    private RepositoryProvider() {}

    public static DistributorRepository get() {
        return repository;
    }

    /** Replace mock data with a live API repository at app startup. */
    public static void set(DistributorRepository impl) {
        repository = impl;
    }
}
