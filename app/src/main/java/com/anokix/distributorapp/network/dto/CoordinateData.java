package com.anokix.distributorapp.network.dto;

/**
 * data block of GET /api/common/get-coordinate-from-placeid –
 * the resolved location for a selected autocomplete prediction.
 */
public class CoordinateData {
    public double latitude;
    public double longitude;
    public String formatted_address;
}
