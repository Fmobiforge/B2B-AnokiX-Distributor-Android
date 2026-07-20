package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/common/wallet — the distributor's anokiX (IMB) wallet.
 * Consumed by the dashboard wallet card + detail panel and by Settings → Business → Wallet.
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
        public List<Transaction> transactions;
    }

    /** One movement on the wallet ledger. {@code amount} arrives as a signed string. */
    public static class Transaction {
        public String date;
        public String amount;
        public long transaction_id;
        public boolean proof_of_payment;
        public boolean pending;
    }
}
