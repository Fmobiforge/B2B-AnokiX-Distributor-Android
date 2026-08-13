package com.anokix.distributorapp.data;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.ListItem;
import com.anokix.distributorapp.model.MenuItem;
import com.anokix.distributorapp.model.OrderItem;
import com.anokix.distributorapp.model.ProductItem;
import com.anokix.distributorapp.model.StoreItem;
import com.anokix.distributorapp.network.dto.DashboardData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MockData {
    private MockData() {}

    public static List<OrderItem> getOrders() {
        List<OrderItem> orders = new ArrayList<>();
        orders.add(new OrderItem("ORD-10548", "Spaza Sunrise Umlazi", "SS", "Umlazi, Durban, 4031",
                "R8,560.00", "Wallet", "10 Jun 2026, 14:32", "new",
                "2 mins ago", "Airtime x10  ·  Data 10GB x5  ·  Electricity x3  ·  +3", true));
        orders.add(new OrderItem("ORD-10547", "Township Store", "TS", "Soweto, Johannesburg",
                "R4,320.00", "Card", "10 Jun 2026, 13:15", "new",
                "15 mins ago", "Airtime x5  ·  Data 5GB x2  ·  +1", true));
        orders.add(new OrderItem("ORD-10546", "Zulu Corner", "ZC", "Pinetown, Durban",
                "R12,450.00", "Cash", "9 Jun 2026, 11:48", "new",
                "1 hour ago", "Electricity x8  ·  Airtime x20", false));
        orders.add(new OrderItem("ORD-10545", "Sipho Spaza Store", "SS", "Durban Central", "R2,890.00", "Credit", "9 Jun 2026, 10:20", "processing"));
        orders.add(new OrderItem("ORD-10542", "Spaza Sunrise", "SS", "Umlazi, Durban", "R9,240.00", "Cash", "5 Jun 2026, 09:45", "delivered"));
        return orders;
    }

    public static List<ListItem> getInventory() {
        return Arrays.asList(
                new ListItem("anokiX Maize Meal 10kg", "AX-MM-10KG", 186, 85, "In Stock"),
                new ListItem("anokiX Cooking Oil 5L", "AX-CO-5L", 42, 35, "Low Stock"),
                new ListItem("anokiX Sugar 2kg", "AX-SG-2KG", 12, 15, "Low Stock"),
                new ListItem("anokiX Rice 10kg", "AX-RC-10KG", 0, 0, "Out of Stock"),
                new ListItem("anokiX Flour 5kg", "AX-FL-5KG", 88, 70, "In Stock"),
                new ListItem("Sunlight 2kg", "AX-SL-2KG", 5, 8, "Low Stock"),
                new ListItem("Coca-Cola 2L", "AX-CC-2L", 0, 0, "Out of Stock"),
                new ListItem("anokiX Spring Water 5L", "AX-SW-5L", 134, 90, "In Stock"),
                new ListItem("anokiX Milk Powder 400g", "AX-MP-400G", 3, 5, "Expired"),
                new ListItem("anokiX Tomato Paste 410g", "AX-TP-410G", 0, 0, "Expired")
        );
    }

    public static List<StoreItem> getStoreItems() {
        return Arrays.asList(
                new StoreItem("s1", "Mabopane Super Store",
                        "1239 Block VV, Mabopane, Pretoria", 48, 12, "Active", "Gauteng"),
                new StoreItem("s2", "Emthombeni Super Store",
                        "456 Block C, Emthombeni, Durban", 36, 8, "Active", "KZN"),
                new StoreItem("s3", "Rockingham Super Store",
                        "981 Block 6, Rockingham, Durban", 52, 21, "Active", "KZN"),
                new StoreItem("s4", "Zandile's Value Store",
                        "78 Main Road, Pinetown, Durban", 29, 5, "Active", "KZN"),
                new StoreItem("s5", "Township Store",
                        "12 Vilakazi St, Soweto, Johannesburg", 41, 17, "Active", "Gauteng"),
                new StoreItem("s6", "Zwelisha Community Store",
                        "5 Church Street, Verulam, KZN", 22, 3, "Inactive", "KZN")
        );
    }

    /** Trader status filter — matches the options in the web Status dropdown. */
    public static String[] getFilterStatus() {
        return new String[]{"All Status", "New", "Active", "Inactive", "Suspended"};
    }

    /** Trader credit standing filter — matches the web Credit Status dropdown. */
    public static String[] getFilterCreditStatus() {
        return new String[]{"All Credit Status", "Good Standing", "Warning", "Overdue", "Suspended"};
    }

    public static String[] getFilterRegistrationStatus() {
        return new String[]{"All Status", "Draft", "Pending Approval", "Approved", "Rejected", "Needs Information"};
    }

    public static String[] getFilterDrivers() {
        return new String[]{"All Drivers", "Thabo M.", "Sipho N.", "John K.", "Peter S."};
    }

    public static List<ProductItem> getProductItems() {
        return Arrays.asList(
                new ProductItem("p1", "anokiX Maize Meal 10kg", "AX-MM-10KG",
                        "Staples", "R 112.50", "In Stock"),
                new ProductItem("p2", "anokiX Cooking Oil 5L", "AX-CO-5L",
                        "Staples", "R 89.99", "In Stock"),
                new ProductItem("p3", "anokiX Sugar 2kg", "AX-SG-2KG",
                        "Staples", "R 34.50", "Low Stock"),
                new ProductItem("p4", "anokiX Rice 10kg", "AX-RC-10KG",
                        "Staples", "R 145.00", "Out of Stock"),
                new ProductItem("p5", "anokiX Spring Water 5L", "AX-SW-5L",
                        "Beverages", "R 28.00", "In Stock"),
                new ProductItem("p6", "Simba Chips 125g", "AX-SC-125G",
                        "Snacks", "R 12.99", "In Stock")
        );
    }

    public static List<ListItem> getStores() {
        return Arrays.asList(
            new ListItem("Spaza Sunrise", "Umlazi, Durban · 2.4 km", "Active"),
            new ListItem("Emthombeni Super Store", "Ntuzuma, Durban · 6.7 km", "Active"),
            new ListItem("Rockingham Super Store", "981 Block 6, Rockingham · 3.1 km", "Active"),
            new ListItem("Zandile's Value Store", "Pinetown, Durban · 5.3 km", "Active"),
            new ListItem("Township Store", "Soweto · 8.1 km", "Active"),
            new ListItem("Zwelisha Community Store", "Verulam, KZN · 11.2 km", "Inactive")
        );
    }

    public static List<ListItem> getTraders() {
        return Arrays.asList(
            new ListItem("John Mahlangu", "Johannesburg, Pretoria · Spaza Owner", "Active"),
            new ListItem("Sarah Dube", "KwaZulu-Natal · Trader", "Active"),
            new ListItem("Thabiso Khan", "Gauteng · Reseller", "Active"),
            new ListItem("Nomvula Phuphuthi", "Limpopo · Spaza Owner", "Active"),
            new ListItem("Lunga Mthembu", "Mpumalanga · Trader", "Inactive"),
            new ListItem("Peter Situngu", "Eastern Cape · Reseller", "Active")
        );
    }

    public static List<ListItem> getProducts() {
        return Arrays.asList(
            new ListItem("anokiX Neat Thai Ting", "R72.50 · In Stock", "Beverages"),
            new ListItem("anokiX Spring Jug", "R36.95 · Low Stock", "Kitchen"),
            new ListItem("Simba Chips", "R72.90 · In Stock", "Snacks"),
            new ListItem("Coca-Cola 2L for Dr Jo", "R64.00 · In Stock", "Beverages"),
            new ListItem("anokiX Tec Nife", "R28.68 · Low Stock", "Tools")
        );
    }

    public static List<ListItem> getPromotions() {
        return Arrays.asList(
            new ListItem("Boost Sales Campaign", "1 Jun – 30 Jun · 68% achieved", "Active"),
            new ListItem("Max Power Launch", "15 Jun – 15 Jul · 12% achieved", "Active"),
            new ListItem("Summer Mega Deals", "1 Jul – 31 Aug · 4.5% achieved", "Paused")
        );
    }

    public static List<ListItem> getFinances() {
        return Arrays.asList(
            new ListItem("Payment from Spaza Sunrise", "R8,560.00 · 10 Jun 2026", "Sale"),
            new ListItem("Payment from Quick Retail", "R4,320.00 · 10 Jun 2026", "Receipt"),
            new ListItem("Payment from South Retail Pvt", "R12,000.00 · 9 Jun 2026", "Sale"),
            new ListItem("Payment from Queenland Store", "R5,800.00 · 9 Jun 2026", "Sale"),
            new ListItem("Payment from Quick Retail Plus", "R3,400.00 · 8 Jun 2026", "Receipt")
        );
    }

    public static List<ListItem> getReports() {
        return Arrays.asList(
            new ListItem("Sales Report", "Generated 10 Jun 2026", "PDF"),
            new ListItem("Inventory Report", "Generated 8 Jun 2026", "XLSX"),
            new ListItem("Traders Report", "Generated 5 Jun 2026", "PDF"),
            new ListItem("Finance Report", "Generated 3 Jun 2026", "PDF")
        );
    }

    public static List<ListItem> getDeliveries() {
        return Arrays.asList(
            new ListItem("DEL-2041", "Township Store · In Transit", "ETA 15:30"),
            new ListItem("DEL-2040", "Spaza Sunrise · Delivered", "10 Jun 11:20"),
            new ListItem("DEL-2039", "Zulu Corner · In Transit", "ETA 17:00"),
            new ListItem("DEL-2038", "Emthombeni Super · Returned", "9 Jun 14:00"),
            new ListItem("DEL-2037", "Rockingham Store · Delayed", "ETA Tomorrow")
        );
    }

    public static List<ListItem> getReturns() {
        return Arrays.asList(
            new ListItem("RET-RT-0041", "Zim Wholesale · Damaged", "R1,980.00"),
            new ListItem("RET-RT-0042", "Township Store · Expired", "R560.00"),
            new ListItem("RET-RT-0045", "Banim Wholesale · Rejected", "R3,780.00")
        );
    }

    public static List<ListItem> getNotifications() {
        return Arrays.asList(
            new ListItem("New order received", "Order #ORD-10548 has been placed", "2h ago"),
            new ListItem("Low stock alert", "Sunlight 2kg is below minimum stock level", "5h ago"),
            new ListItem("Delivery delayed", "Delivery to DEL-2041 running 45 min late", "8h ago"),
            new ListItem("Order delivered", "Order #ORD-10542 delivered successfully", "1d ago")
        );
    }

    public static List<ListItem> getSettingsItems() {
        return Arrays.asList(
            new ListItem("Profile", "Update personal information", ""),
            new ListItem("Business", "Manage business details", ""),
            new ListItem("Security", "Password & authentication", ""),
            new ListItem("Preferences", "App settings & language", "")
        );
    }

    public static List<MenuItem> getMoreMenuItems() {
        return Arrays.asList(
            new MenuItem("stores", "Stores", R.drawable.ic_stores),
            new MenuItem("traders", "Traders", R.drawable.ic_traders),
            new MenuItem("products", "Products", R.drawable.ic_products),
            new MenuItem("promotions", "Ads & Promotions", R.drawable.ic_promotions),
            new MenuItem("finances", "Finances", R.drawable.ic_finances),
            new MenuItem("wallet", "anokiX wallet", R.drawable.ic_wallet, "Powered by IMB"),
            new MenuItem("airtime", "Airtime & VAS", R.drawable.ic_vas, "Powered by Limes"),
            new MenuItem("reports", "Reports", R.drawable.ic_reports),
            new MenuItem("deliveries", "Deliveries", R.drawable.ic_deliveries),
            new MenuItem("returns", "Returns", R.drawable.ic_returns),
            new MenuItem("notifications", "Notifications", R.drawable.ic_notifications),
            new MenuItem("settings", "Settings", R.drawable.ic_settings)
        );
    }

    /**
     * Drawer navigation, in the order agreed for the new design (New-Design/Menu).
     * Every entry routes to a screen; MainActivity answers anything unmapped with a
     * "coming soon" message.
     */
    public static List<MenuItem> getDrawerMenuItems() {
        return Arrays.asList(
            new MenuItem("home", "Dashboard", R.drawable.ic_dashboard),
            new MenuItem("stores", "Stores", R.drawable.ic_stores),
            new MenuItem("products", "Products", R.drawable.ic_shopping_bag),
            new MenuItem("promotions", "Ads & Promotions", R.drawable.ic_promotions),
            new MenuItem("orders", "Orders", R.drawable.ic_orders),
            new MenuItem("deliveries", "Deliveries", R.drawable.ic_deliveries),
            new MenuItem("returns", "Returns", R.drawable.ic_returns),
            new MenuItem("inventory", "Inventory", R.drawable.ic_inventory),
            new MenuItem("finances", "Finances", R.drawable.ic_finances),
            new MenuItem("card_machine", "Card Machine", R.drawable.ic_card, "Powered by PayCloud"),
            new MenuItem("wallet", "anokiX wallet", R.drawable.ic_wallet, "Powered by IMB"),
            new MenuItem("airtime", "Airtime & VAS", R.drawable.ic_vas, "Powered by Limes"),
            new MenuItem("reports", "Reports", R.drawable.ic_reports),
            new MenuItem("traders", "Traders", R.drawable.ic_traders),
            new MenuItem("warehouse_staff", "Warehouse Staff", R.drawable.ic_person_add,
                    "Who can pick and pack"),
            new MenuItem("notifications", "Notifications", R.drawable.ic_notifications),
            new MenuItem("settings", "Settings", R.drawable.ic_settings),
            new MenuItem("support", "Support Centre", R.drawable.ic_headset)
        );
    }

    /** Placeholder summary KPI cards shown before the dashboard API responds. */
    public static List<DashboardData.SummaryCard> fallbackSummaryCards() {
        List<DashboardData.SummaryCard> cards = new ArrayList<>();
        cards.add(summaryCard("total_revenue", "Total Revenue", "R0.00", "↑ 0.0%", "purple",
                new float[]{4.2f, 3.8f, 5.1f, 4.7f, 5.6f, 4.9f, 6.2f}));
        cards.add(summaryCard("total_orders", "Total Orders", "0", "↑ 0.0%", "blue",
                new float[]{9.8f, 10.2f, 10.5f, 11f, 11.5f, 12f, 12.86f}));
        cards.add(summaryCard("active_traders", "Active Traders", "0", "↑ 0.0%", "green",
                new float[]{21f, 21.5f, 22f, 22.8f, 23.2f, 23.8f, 24.53f}));
        cards.add(summaryCard("products_sold", "Products Sold", "0", "↑ 0.0%", "orange",
                new float[]{20.1f, 20.8f, 21.4f, 22f, 22.9f, 23.6f, 24.56f}));
        return cards;
    }

    private static DashboardData.SummaryCard summaryCard(String key, String label, String value,
                                                         String trendLabel, String color, float[] spark) {
        DashboardData.SummaryCard c = new DashboardData.SummaryCard();
        c.key = key;
        c.label = label;
        c.formatted_value = value;
        c.color = color;
        c.trend = new DashboardData.Trend();
        c.trend.direction = "up";
        c.trend.label = trendLabel;
        List<Float> list = new ArrayList<>();
        for (float v : spark) list.add(v);
        c.sparkline = list;
        return c;
    }

    public static List<String> getKpiTitles() {
        return Arrays.asList("Pending Orders", "Out for Delivery", "Low Stock", "New Traders");
    }

    public static List<String> getKpiValues() {
        return Arrays.asList("0", "0", "0", "0");
    }

    public static List<Integer> getKpiIcons() {
        return Arrays.asList(
            R.drawable.ic_shopping_bag,
            R.drawable.ic_deliveries,
            R.drawable.ic_warning_box,
            R.drawable.ic_traders
        );
    }

    public static List<Integer> getKpiIconBackgrounds() {
        return Arrays.asList(
            R.drawable.bg_kpi_icon_orange,
            R.drawable.bg_kpi_icon_purple,
            R.drawable.bg_kpi_icon_red,
            R.drawable.bg_kpi_icon_green
        );
    }

    public static List<Integer> getKpiValueColors() {
        return Arrays.asList(
            R.color.warning,
            R.color.purple_primary,
            R.color.danger,
            R.color.success
        );
    }

    public static List<Integer> getKpiIconTints() {
        return Arrays.asList(
            R.color.warning,
            R.color.purple_primary,
            R.color.danger,
            R.color.success
        );
    }

    public static List<String> getKpiActionTitles() {
        return Arrays.asList("View orders", "Track orders", "View inventory", "View traders");
    }
}
