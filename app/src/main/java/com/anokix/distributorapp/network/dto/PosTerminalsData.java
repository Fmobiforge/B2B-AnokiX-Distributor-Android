package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/common/pos/terminals — the PayCloud card machines linked to
 * this account. {@code configured} is false when the portal has no PayCloud credentials.
 */
public class PosTerminalsData {
    public boolean configured;
    public List<Terminal> terminals;

    public static class Terminal {
        public long id;
        public String terminal_sn;
        public String label;
        public String store_no;
        public boolean is_active;
        public String last_used_at;

        /** Falls back to the serial when the machine was saved without a name. */
        public String displayName() {
            return label == null || label.trim().isEmpty() ? terminal_sn : label;
        }
    }
}
