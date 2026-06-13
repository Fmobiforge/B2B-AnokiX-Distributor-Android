package com.anokix.distributor.data;

import com.anokix.distributor.model.DeliveryItem;
import com.anokix.distributor.model.FinanceSummary;
import com.anokix.distributor.model.FinanceTransaction;
import com.anokix.distributor.model.NotificationItem;
import com.anokix.distributor.model.NotificationPreference;
import com.anokix.distributor.model.PromotionItem;
import com.anokix.distributor.model.ReportHistoryItem;
import com.anokix.distributor.model.ReportTypeItem;
import com.anokix.distributor.model.ReturnItem;
import com.anokix.distributor.model.SettingsMenuEntry;
import com.anokix.distributor.model.SettingsProfile;

import java.util.List;
import java.util.Map;

/**
 * Data access contract for distributor screens. Swap {@link MockDistributorRepository}
 * with an API-backed implementation when backend endpoints are available.
 */
public interface DistributorRepository {

    List<ReturnItem> getReturns(String statusFilter);

    Map<String, Integer> getReturnSummaryCounts();

    List<NotificationItem> getNotifications(String filter);

    int getUnreadNotificationCount();

    void markAllNotificationsRead();

    List<PromotionItem> getPromotions();

    List<FinanceSummary> getFinanceSummaries();

    List<FinanceTransaction> getFinanceTransactions(String typeFilter);

    List<ReportTypeItem> getReportTypes();

    List<ReportHistoryItem> getRecentReports();

    List<DeliveryItem> getDeliveries(String statusFilter);

    SettingsProfile getSettingsProfile();

    List<SettingsMenuEntry> getSettingsMenuEntries();

    List<NotificationPreference> getNotificationPreferences();

    void saveNotificationPreferences(List<NotificationPreference> preferences);
}
