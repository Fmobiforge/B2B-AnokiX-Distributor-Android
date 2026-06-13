package com.anokix.distributor.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/stores/all.
 */
public class StoresData {
    public int total_count;
    public String keyword;
    public List<Store> stores;

    public static class Store {
        public long id;
        public String name;
        public String address;
        public String tag_list;
        public boolean is_click_collect;
        public boolean is_click_deliver;
        public int follower_count;
        public int review_count;
    }
}
