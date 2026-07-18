package com.anokix.distributorapp.network.dto;

/**
 * data block of GET /api/common/wallet — the distributor's anokiX (IMB) wallet.
 * Only the fields the dashboard wallet card + detail panel consume are modelled.
 */
public class WalletData {
    public String status;
    public String account_number;
    public String activated_at;
    public String last_error;
    public boolean configured;
    public boolean payments_enabled;
    public Balance balance;

    public static class Balance {
        public double current;
        public double available;
        public double pending;
        public String as_of;
        public boolean stale;
    }
}
