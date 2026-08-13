package com.anokix.distributorapp.ui.util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.SupportOverviewData;
import com.anokix.distributorapp.network.dto.SupportTicketData;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The two ways of raising a ticket from the Support Centre:
 * "Request a call back" (2.Call-Request.png) and "Tell us what's wrong"
 * (3.Create-Ticket.png).
 *
 * <p>Both post to the same {@code support/tickets} endpoint and differ only in their
 * {@code channel} — {@code callback} carries the number to ring, {@code web} carries a
 * priority. Bottom sheets rather than centre dialogs so the submit button stays above the
 * keyboard the text fields raise.
 */
public final class SupportSheets {

    /** Called after a ticket is raised, with the server's confirmation message. */
    public interface OnSubmitted {
        void onSubmitted(@NonNull String message);
    }

    /** The urgency options, in the order and wording the requirements specify. */
    private static final String[] PRIORITY_VALUES = {"low", "normal", "high", "urgent"};
    private static final int[] PRIORITY_LABELS = {
            R.string.priority_low, R.string.priority_normal,
            R.string.priority_high, R.string.priority_urgent
    };
    /** "Normal" — the default the web dialog opens with. */
    private static final int DEFAULT_PRIORITY = 1;

    private SupportSheets() {
    }

    // ---- Call back -------------------------------------------------------

    public static void showCallbackRequest(@NonNull Activity activity,
                                           @NonNull OnSubmitted onSubmitted) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_callback_request, null, false);
        dialog.setContentView(content);

        EditText phone = content.findViewById(R.id.callbackPhone);
        EditText subject = content.findViewById(R.id.callbackSubject);
        EditText message = content.findViewById(R.id.callbackMessage);
        View submit = content.findViewById(R.id.sheetSubmit);

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        submit.setOnClickListener(v -> {
            String number = text(phone);
            if (number.isEmpty()) {
                phone.setError(activity.getString(R.string.err_callback_number));
                phone.requestFocus();
                return;
            }

            Map<String, String> form = new HashMap<>();
            form.put("channel", "callback");
            form.put("callback_phone", number);
            // The subject is optional on screen but the ticket needs a title, so an empty
            // one becomes the same default the API uses for call-backs.
            String title = text(subject);
            form.put("subject", title.isEmpty()
                    ? activity.getString(R.string.callback_default_subject) : title);
            form.put("message", text(message));

            submitTicket(activity, dialog, submit, form,
                    R.string.requesting_call, R.string.callback_submit, onSubmitted);
        });

        dialog.show();
    }

    // ---- Create ticket ---------------------------------------------------

    /**
     * @param categories the topic list from the overview call. The Topic row is hidden
     *                   when this is empty — it is optional, and an empty picker would be
     *                   a control that cannot be used.
     */
    public static void showCreateTicket(@NonNull Activity activity,
                                        @Nullable List<SupportOverviewData.Category> categories,
                                        @NonNull OnSubmitted onSubmitted) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_create_ticket, null, false);
        dialog.setContentView(content);

        EditText subject = content.findViewById(R.id.ticketSubject);
        EditText message = content.findViewById(R.id.ticketMessage);
        TextView priorityValue = content.findViewById(R.id.priorityValue);
        TextView topicValue = content.findViewById(R.id.topicValue);
        View submit = content.findViewById(R.id.sheetSubmit);

        final int[] priority = {DEFAULT_PRIORITY};
        priorityValue.setText(PRIORITY_LABELS[priority[0]]);

        content.findViewById(R.id.priorityField).setOnClickListener(v -> {
            String[] labels = new String[PRIORITY_LABELS.length];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = activity.getString(PRIORITY_LABELS[i]);
            }
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.ticket_priority_label)
                    .setItems(labels, (d, which) -> {
                        priority[0] = which;
                        priorityValue.setText(PRIORITY_LABELS[which]);
                    })
                    .show();
        });

        final SupportOverviewData.Category[] topic = {null};
        boolean hasTopics = categories != null && !categories.isEmpty();
        content.findViewById(R.id.topicGroup)
                .setVisibility(hasTopics ? View.VISIBLE : View.GONE);
        if (hasTopics) {
            content.findViewById(R.id.topicField).setOnClickListener(v -> {
                String[] labels = new String[categories.size()];
                for (int i = 0; i < categories.size(); i++) {
                    labels[i] = categories.get(i).name;
                }
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.ticket_topic_label)
                        .setItems(labels, (d, which) -> {
                            topic[0] = categories.get(which);
                            topicValue.setText(topic[0].name);
                        })
                        .show();
            });
        }

        content.findViewById(R.id.sheetClose).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.sheetCancel).setOnClickListener(v -> dialog.dismiss());

        submit.setOnClickListener(v -> {
            String title = text(subject);
            if (title.isEmpty()) {
                subject.setError(activity.getString(R.string.err_ticket_subject));
                subject.requestFocus();
                return;
            }
            String body = text(message);
            if (body.isEmpty()) {
                message.setError(activity.getString(R.string.err_ticket_message));
                message.requestFocus();
                return;
            }

            Map<String, String> form = new HashMap<>();
            form.put("channel", "web");
            form.put("subject", title);
            form.put("message", body);
            form.put("priority", PRIORITY_VALUES[priority[0]]);
            if (topic[0] != null && topic[0].id != null) {
                form.put("category", topic[0].id);
            }

            submitTicket(activity, dialog, submit, form,
                    R.string.sending_request, R.string.ticket_submit, onSubmitted);
        });

        dialog.show();
    }

    // ---- Shared ----------------------------------------------------------

    private static void submitTicket(Activity activity, BottomSheetDialog dialog, View submit,
                                     Map<String, String> form, int busyLabel, int idleLabel,
                                     OnSubmitted onSubmitted) {
        submit.setEnabled(false);
        ((TextView) submit).setText(busyLabel);
        ApiClient.get(activity).createSupportTicket(form, new ApiCallback<SupportTicketData>() {
            @Override
            public void onSuccess(SupportTicketData data) {
                dialog.dismiss();
                String number = data != null && data.ticket != null
                        && data.ticket.ticket_number != null ? data.ticket.ticket_number : "";
                onSubmitted.onSubmitted(number.isEmpty()
                        ? activity.getString(R.string.ticket_logged)
                        : activity.getString(R.string.ticket_logged_numbered, number));
            }

            @Override
            public void onError(String message) {
                submit.setEnabled(true);
                ((TextView) submit).setText(idleLabel);
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
