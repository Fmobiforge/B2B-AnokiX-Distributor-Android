package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/inventory/history?product_id=&limit=.
 */
public class StockHistoryData {
    public List<StockMovement> movements;
    public int total;
}
