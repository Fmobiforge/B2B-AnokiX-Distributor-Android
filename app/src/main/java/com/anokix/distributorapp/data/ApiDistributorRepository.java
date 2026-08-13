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
import com.anokix.distributorapp.model.ReturnItem;
import com.anokix.distributorapp.model.SettingsMenuEntry;
import com.anokix.distributorapp.model.SettingsProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Skeleton for a live API-backed repository. Implement each method with your
 * HTTP client (Retrofit, OkHttp, etc.) and register at app startup:
 *
 * RepositoryProvider.set(new ApiDistributorRepository(apiClient));
 */
public class ApiDistributorRepository implements DistributorRepository {

    @Override
    public List<ReturnItem> getReturns(String statusFilter) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Integer> getReturnSummaryCounts() {
        return Collections.emptyMap();
    }

    @Override
    public List<NotificationItem> getNotifications(String filter) {
        return Collections.emptyList();
    }

    @Override
    public int getUnreadNotificationCount() {
        return 0;
    }

    @Override
    public void markAllNotificationsRead() {
        // POST /notifications/mark-all-read
    }

    @Override
    public List<PromotionItem> getPromotions() {
        return Collections.emptyList();
    }

    @Override
    public List<FinanceSummary> getFinanceSummaries() {
        return Collections.emptyList();
    }

    @Override
    public List<FinanceTransaction> getFinanceTransactions(String typeFilter) {
        return Collections.emptyList();
    }

    @Override
    public List<DeliveryItem> getDeliveries(String statusFilter) {
        return Collections.emptyList();
    }

    @Override
    public SettingsProfile getSettingsProfile() {
        return new SettingsProfile("", "", "?", false);
    }

    @Override
    public List<SettingsMenuEntry> getSettingsMenuEntries() {
        return Collections.emptyList();
    }

    @Override
    public ProfileSettings getProfileSettings() {
        return new ProfileSettings("", "", "", "", "");
    }

    @Override
    public void saveProfileSettings(ProfileSettings settings) {
        // PUT /settings/profile
    }

    @Override
    public BusinessSettings getBusinessSettings() {
        return new BusinessSettings("", "", "", "", "", null, null);
    }

    @Override
    public void saveBusinessSettings(BusinessSettings settings) {
        // PUT /settings/business
    }

    @Override
    public String changePassword(String currentPassword, String newPassword, String confirmPassword) {
        // POST /settings/change-password
        return null;
    }

    @Override
    public AppPreferences getAppPreferences() {
        return new AppPreferences("English", "", "", "");
    }

    @Override
    public void saveAppPreferences(AppPreferences preferences) {
        // PUT /settings/preferences
    }

    @Override
    public List<NotificationPreference> getNotificationPreferences() {
        return Collections.emptyList();
    }

    @Override
    public void saveNotificationPreferences(List<NotificationPreference> preferences) {
        // PUT /settings/notification-preferences
    }
}
