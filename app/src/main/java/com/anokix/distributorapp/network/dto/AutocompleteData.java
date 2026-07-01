package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/common/google-auto-complete – a Google Places
 * autocomplete response proxied by the backend.
 */
public class AutocompleteData {
    public String status;
    public List<Prediction> predictions;

    public static class Prediction {
        public String description;
        public String place_id;
    }
}
