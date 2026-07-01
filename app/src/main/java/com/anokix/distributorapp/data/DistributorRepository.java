package com.anokix.distributorapp.data;

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

    ProfileSettings getProfileSettings();

    void saveProfileSettings(ProfileSettings settings);

    BusinessSettings getBusinessSettings();

    void saveBusinessSettings(BusinessSettings settings);

    String changePassword(String currentPassword, String newPassword, String confirmPassword);

    AppPreferences getAppPreferences();

    void saveAppPreferences(AppPreferences preferences);

    List<NotificationPreference> getNotificationPreferences();

    void saveNotificationPreferences(List<NotificationPreference> preferences);
}
