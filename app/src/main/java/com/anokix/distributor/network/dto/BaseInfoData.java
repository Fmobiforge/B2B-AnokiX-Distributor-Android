package com.anokix.distributor.network.dto;

import java.util.List;

/**
 * data block of GET /api/common/base-info. Only the lists the store editor needs
 * (currencies + timezones) are modelled.
 */
public class BaseInfoData {
    public List<Currency> currencies;
    public List<Timezone> timezones;

    public static class Currency {
        public String id;
        public String code;
        public String name;
        public String symbol;
    }

    public static class Timezone {
        public String name;
        public String region;
        public String label;
        public String time_zone;
        public long time_offset;
    }
}
