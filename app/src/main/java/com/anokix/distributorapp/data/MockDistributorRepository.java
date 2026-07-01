package com.anokix.distributorapp.data;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.AppPreferences;
import com.anokix.distributorapp.model.BusinessSettings;
import com.anokix.distributorapp.model.DeliveryItem;
import com.anokix.distributorapp.model.FinanceSummary;
import com.anokix.distributorapp.model.FinanceTransaction;
import com.anokix.distributorapp.model.NotificationItem;
import com.anokix.distributorapp.model.NotificationPreference;
import com.anokix.distributorapp.model.ProfileSettings;
import com.anokix.distributorapp.model.PromotionItem;
import com.anokix.distributorapp.model.ReportHistoryItem;
import com.anokix.distributorapp.model.ReportTypeItem;
import com.anokix.distributorapp.model.ReturnItem;
import com.anokix.distributorapp.model.SettingsMenuEntry;
import com.anokix.distributorapp.model.SettingsProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDistributorRepository implements DistributorRepository {

    private final List<ReturnItem> returns = Arrays.asList(
            new ReturnItem("RET-10032", "Zim Wholesale", "Damaged Goods", "R4,560.00",
                    "21 May 2025 • 10:30 AM", "pending"),
            new ReturnItem("RET-10028", "Township Store", "Expired Products", "R560.00",
                    "20 May 2025 • 14:15 PM", "pending"),
            new ReturnItem("RET-10025", "Banim Wholesale", "Wrong Items", "R3,780.00",
                    "19 May 2025 • 09:00 AM", "approved"),
            new ReturnItem("RET-10020", "Spaza Sunrise", "Damaged Packaging", "R1,980.00",
                    "18 May 2025 • 16:45 PM", "rejected")
    );

    private final List<NotificationItem> notifications = new ArrayList<>(Arrays.asList(
            new NotificationItem("n1", "New order received",
                    "Order #ORD-10456 has been placed by Sunrise Traders.",
                    "2m ago", "orders",
                    R.drawable.ic_orders, R.drawable.bg_kpi_icon_purple,
                    R.color.purple_primary, "View order", true, false),
            new NotificationItem("n2", "Low stock alert",
                    "5 products are low on stock. Please restock soon.",
                    "1h ago", "inventory",
                    R.drawable.ic_warning_box, R.drawable.bg_kpi_icon_orange,
                    R.color.warning, "View items", true, false),
            new NotificationItem("n3", "Delivery delayed",
                    "Delivery for order #ORD-10432 has been delayed.",
                    "3h ago", "deliveries",
                    R.drawable.ic_deliveries, R.drawable.bg_kpi_icon_red,
                    R.color.danger, "View order", true, true),
            new NotificationItem("n4", "Order delivered",
                    "Order #ORD-10428 has been delivered successfully.",
                    "Yesterday, 6:20 PM", "orders",
                    R.drawable.ic_check_green, R.drawable.bg_kpi_icon_green,
                    R.color.success, "", false, false)
    ));

    private final List<NotificationPreference> preferences = new ArrayList<>(Arrays.asList(
            new NotificationPreference("order_alerts", "Order Alerts",
                    "Get notified when new orders are placed.", true),
            new NotificationPreference("low_stock", "Low Stock Alerts",
                    "Receive alerts when products fall below minimum stock.", true),
            new NotificationPreference("delivery_updates", "Delivery Updates",
                    "Track delivery status changes and delays.", true),
            new NotificationPreference("finance_reminders", "Finance Reminders",
                    "Reminders for overdue invoices and payments.", true),
            new NotificationPreference("report_ready", "Report Ready",
                    "Notify when scheduled reports are generated.", false)
    ));

    private ProfileSettings profileSettings = new ProfileSettings(
            "Chang", "CongCong", "test@gmail.com", "", "comp1");
    private BusinessSettings businessSettings = new BusinessSettings(
            "comp1", "", "ca",
            "Be\u015Ftepe, A\u015FT\u0130, Yenimahalle/Ankara, T\u00FCrkiye", "",
            39.9306, 32.7439);
    private AppPreferences appPreferences = new AppPreferences(
            "English", "South African Rand (ZAR)", "Africa/Johannesburg", "19 May 2024");

    @Override
    public List<ReturnItem> getReturns(String statusFilter) {
        if (statusFilter == null || "all".equals(statusFilter)) {
            return new ArrayList<>(returns);
        }
        List<ReturnItem> filtered = new ArrayList<>();
        for (ReturnItem item : returns) {
            if (item.status.equalsIgnoreCase(statusFilter)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    @Override
    public Map<String, Integer> getReturnSummaryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("pending", 12);
        counts.put("approved", 18);
        counts.put("rejected", 4);
        return counts;
    }

    @Override
    public List<NotificationItem> getNotifications(String filter) {
        if ("unread".equals(filter)) {
            List<NotificationItem> unread = new ArrayList<>();
            for (NotificationItem n : notifications) {
                if (n.unread) unread.add(n);
            }
            return unread;
        }
        if ("orders".equals(filter)) {
            List<NotificationItem> orderItems = new ArrayList<>();
            for (NotificationItem n : notifications) {
                if ("orders".equals(n.type)) orderItems.add(n);
            }
            return orderItems;
        }
        return new ArrayList<>(notifications);
    }

    @Override
    public int getUnreadNotificationCount() {
        int count = 0;
        for (NotificationItem n : notifications) {
            if (n.unread) count++;
        }
        return count;
    }

    @Override
    public void markAllNotificationsRead() {
        for (int i = 0; i < notifications.size(); i++) {
            NotificationItem n = notifications.get(i);
            notifications.set(i, new NotificationItem(n.id, n.title, n.message, n.relativeTime,
                    n.type, n.iconRes, n.iconBgRes, n.iconTintRes, n.actionLabel, false, n.important));
        }
    }

    @Override
    public List<PromotionItem> getPromotions() {
        return Arrays.asList(
                new PromotionItem("p1", "Boost Sales Campaign", "best_deal", "Best Deal", "active",
                        "Active", "15 Jun – 30 Jun 2026", "R3,000", "245,678", "8,420", "3.45%"),
                new PromotionItem("p2", "Max Power Launch", "promotion", "Promotion", "active",
                        "Active", "15 Jun – 30 Jun 2026", "R1,800", "182,450", "5,240", "2.87%"),
                new PromotionItem("p3", "Summer Mega Deals", "featured_store", "Featured Store", "draft",
                        "Draft", "10 Jun – 24 Jun 2026", "R900", "81,203", "1,230", "1.52%")
        );
    }

    @Override
    public List<FinanceSummary> getFinanceSummaries() {
        return Arrays.asList(
                new FinanceSummary("Revenue This Month", "R2.4M", "↑ 12.5% vs last month", true,
                        R.color.text_primary, R.color.purple_primary),
                new FinanceSummary("Invoices Paid", "R1.8M", "↑ 8.3% vs last month", true,
                        R.color.text_primary, R.color.success),
                new FinanceSummary("Pending Payments", "R620K", "↓ 4.6% vs last month", false,
                        R.color.text_primary, R.color.warning)
        );
    }

    @Override
    public List<FinanceTransaction> getFinanceTransactions(String typeFilter) {
        List<FinanceTransaction> all = Arrays.asList(
                new FinanceTransaction("f1", "Payment from Mega Traders", "INV-10023",
                        "24 May 2025", "+R250,000", true, "Paid", R.drawable.ic_document),
                new FinanceTransaction("f2", "Stock Purchase - May", "EXP-20015",
                        "24 May 2025", "-R75,000", false, "Paid", R.drawable.ic_package),
                new FinanceTransaction("f3", "Payment from Quick Retail", "INV-10020",
                        "23 May 2025", "+R180,000", true, "Paid", R.drawable.ic_document),
                new FinanceTransaction("f4", "Transport Fuel", "EXP-20012",
                        "22 May 2025", "-R12,500", false, "Paid", R.drawable.ic_deliveries),
                new FinanceTransaction("f5", "Payment from Queenland Store", "INV-10018",
                        "21 May 2025", "+R95,000", true, "Paid", R.drawable.ic_document)
        );
        if ("income".equals(typeFilter)) {
            List<FinanceTransaction> income = new ArrayList<>();
            for (FinanceTransaction t : all) {
                if (t.isIncome) income.add(t);
            }
            return income;
        }
        if ("expenses".equals(typeFilter)) {
            List<FinanceTransaction> expenses = new ArrayList<>();
            for (FinanceTransaction t : all) {
                if (!t.isIncome) expenses.add(t);
            }
            return expenses;
        }
        return all;
    }

    @Override
    public List<ReportTypeItem> getReportTypes() {
        return Arrays.asList(
                new ReportTypeItem("sales", "Sales Report",
                        "View sales performance", R.drawable.ic_reports),
                new ReportTypeItem("inventory", "Inventory Report",
                        "Track stock levels", R.drawable.ic_package),
                new ReportTypeItem("traders", "Traders Report",
                        "Analyze trader activity", R.drawable.ic_traders),
                new ReportTypeItem("finance", "Finance Report",
                        "Financial summaries", R.drawable.ic_finance_report)
        );
    }

    @Override
    public List<ReportHistoryItem> getRecentReports() {
        return Arrays.asList(
                new ReportHistoryItem("r1", "Sales Report - May 2025", "24 May 2025", "PDF"),
                new ReportHistoryItem("r2", "Inventory Report - May 2025", "24 May 2025", "PDF"),
                new ReportHistoryItem("r3", "Traders Report - May 2025", "23 May 2025", "PDF"),
                new ReportHistoryItem("r4", "Finance Report - May 2025", "23 May 2025", "PDF"),
                new ReportHistoryItem("r5", "Sales Report - April 2025", "30 Apr 2025", "PDF")
        );
    }

    @Override
    public List<DeliveryItem> getDeliveries(String statusFilter) {
        List<DeliveryItem> all = Arrays.asList(
                new DeliveryItem("DL-10045", "Mega Traders", "Thabo M.",
                        "Today, 15:30 – 16:00", "in_transit"),
                new DeliveryItem("DL-10044", "Quick Retail", "Sipho N.",
                        "24 May 2025, 11:20", "delivered"),
                new DeliveryItem("DL-10043", "Queenland Store", "John K.",
                        "Today, 17:00 – 17:30", "in_transit"),
                new DeliveryItem("DL-10042", "Rockingham Store", "Peter S.",
                        "ETA Tomorrow", "delayed")
        );
        if (statusFilter == null || "all".equals(statusFilter)) {
            return all;
        }
        List<DeliveryItem> filtered = new ArrayList<>();
        for (DeliveryItem item : all) {
            if (item.status.equalsIgnoreCase(statusFilter)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    @Override
    public SettingsProfile getSettingsProfile() {
        return new SettingsProfile("Cong Distributor", "congdistributor@gmail.com", "C", true);
    }

    @Override
    public List<SettingsMenuEntry> getSettingsMenuEntries() {
        return Arrays.asList(
                new SettingsMenuEntry("profile", "Profile", "", R.drawable.ic_person),
                new SettingsMenuEntry("business", "Business", "", R.drawable.ic_briefcase),
                new SettingsMenuEntry("notifications", "Notifications", "", R.drawable.ic_notifications),
                new SettingsMenuEntry("security", "Security", "", R.drawable.ic_shield),
                new SettingsMenuEntry("preferences", "Preferences", "", R.drawable.ic_tune)
        );
    }

    @Override
    public ProfileSettings getProfileSettings() {
        return profileSettings;
    }

    @Override
    public void saveProfileSettings(ProfileSettings settings) {
        profileSettings = settings;
    }

    @Override
    public BusinessSettings getBusinessSettings() {
        return businessSettings;
    }

    @Override
    public void saveBusinessSettings(BusinessSettings settings) {
        businessSettings = settings;
    }

    @Override
    public String changePassword(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            return "Please enter your current password.";
        }
        if (newPassword == null || newPassword.length() < 8) {
            return "New password must be at least 8 characters.";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "New passwords do not match.";
        }
        return null;
    }

    @Override
    public AppPreferences getAppPreferences() {
        return appPreferences;
    }

    @Override
    public void saveAppPreferences(AppPreferences preferences) {
        appPreferences = preferences;
    }

    @Override
    public List<NotificationPreference> getNotificationPreferences() {
        return new ArrayList<>(preferences);
    }

    @Override
    public void saveNotificationPreferences(List<NotificationPreference> updated) {
        preferences.clear();
        preferences.addAll(updated);
    }
}
