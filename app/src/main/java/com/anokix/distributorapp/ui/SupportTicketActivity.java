package com.anokix.distributorapp.ui;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.SupportOverviewData;
import com.anokix.distributorapp.network.dto.SupportTicketData;
import com.anokix.distributorapp.ui.adapter.TicketMessageAdapter;

import java.util.List;
import java.util.Locale;

/**
 * One support ticket (New-Design/Support Centre/4.Open-Ticket.png, 5.Closed-Ticket.png).
 *
 * <p>The conversation reads as a chat thread. An open ticket gets the composer with
 * Close ticket / Send reply; a closed one replaces it with the closing notice, since
 * neither action is available any more.
 *
 * <p>Replying and closing both answer with the ticket in its new state, so the thread is
 * repainted from the response rather than re-fetched.
 */
public class SupportTicketActivity extends BaseSecondaryActivity {

    private static final String EXTRA_ID = "ticket_id";
    private static final String EXTRA_NUMBER = "ticket_number";
    private static final String EXTRA_SUBJECT = "ticket_subject";

    /**
     * The number and subject come from the row that was tapped so the header is right
     * before the detail call lands — otherwise the screen opens with an empty title.
     */
    public static Intent intentFor(Context context, String id, String number, String subject) {
        return new Intent(context, SupportTicketActivity.class)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_NUMBER, number)
                .putExtra(EXTRA_SUBJECT, subject);
    }

    private TicketMessageAdapter adapter;
    private RecyclerView messagesList;
    private EditText replyInput;
    private View composer;
    private View closedNotice;
    private TextView statusBadge;
    private TextView openedLabel;
    private TextView priorityBadge;
    private TextView sendButton;

    private SupportOverviewData.Ticket ticket;
    /** Set once a reply or close lands, so the list behind us knows to reload. */
    private boolean changed;

    private String ticketId() {
        String id = getIntent().getStringExtra(EXTRA_ID);
        return id == null ? "" : id;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_support_ticket;
    }

    @Override
    protected String getScreenTitle() {
        String number = getIntent().getStringExtra(EXTRA_NUMBER);
        return number == null || number.trim().isEmpty()
                ? getString(R.string.support_centre) : number;
    }

    @Override
    protected String getScreenSubtitle() {
        String subject = getIntent().getStringExtra(EXTRA_SUBJECT);
        return subject == null || subject.trim().isEmpty() ? null : subject;
    }

    @Override
    protected void bindViews() {
        messagesList = findViewById(R.id.messagesList);
        messagesList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TicketMessageAdapter();
        messagesList.setAdapter(adapter);

        replyInput = findViewById(R.id.replyInput);
        composer = findViewById(R.id.composer);
        closedNotice = findViewById(R.id.closedNotice);
        statusBadge = findViewById(R.id.ticketStatusBadge);
        openedLabel = findViewById(R.id.ticketOpened);
        priorityBadge = findViewById(R.id.ticketPriority);
        sendButton = findViewById(R.id.sendReplyButton);

        sendButton.setOnClickListener(v -> sendReply());
        findViewById(R.id.closeTicketButton).setOnClickListener(v -> confirmClose());
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getSupportTicket(ticketId(), new ApiCallback<SupportTicketData>() {
            @Override
            public void onSuccess(SupportTicketData data) {
                render(data == null ? null : data.ticket);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SupportTicketActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void render(SupportOverviewData.Ticket data) {
        if (data == null) {
            return;
        }
        ticket = data;

        statusBadge.setText(data.statusLabel());
        statusBadge.setTextColor(ContextCompat.getColor(this,
                data.is_open ? R.color.danger : R.color.success));
        statusBadge.setBackgroundResource(data.is_open
                ? R.drawable.bg_badge_danger : R.drawable.bg_badge_success);

        openedLabel.setText(getString(R.string.ticket_opened, data.openedLabel()));

        // Priority only earns a badge when it is out of the ordinary.
        boolean notable = data.priority != null
                && ("high".equalsIgnoreCase(data.priority.trim())
                    || "urgent".equalsIgnoreCase(data.priority.trim()));
        priorityBadge.setVisibility(notable ? View.VISIBLE : View.GONE);
        if (notable) {
            String label = data.priority.trim();
            priorityBadge.setText(label.substring(0, 1).toUpperCase(Locale.getDefault())
                    + label.substring(1));
            boolean urgent = "urgent".equalsIgnoreCase(label);
            priorityBadge.setTextColor(ContextCompat.getColor(this,
                    urgent ? R.color.danger : R.color.warning));
            priorityBadge.setBackgroundResource(urgent
                    ? R.drawable.bg_badge_danger : R.drawable.bg_badge_warning);
        }

        List<SupportOverviewData.Message> messages = data.messages;
        adapter.setItems(messages);
        if (messages != null && !messages.isEmpty()) {
            // The newest message is the one worth seeing on arrival.
            messagesList.scrollToPosition(messages.size() - 1);
        }

        composer.setVisibility(data.is_open ? View.VISIBLE : View.GONE);
        closedNotice.setVisibility(data.is_open ? View.GONE : View.VISIBLE);
    }

    // ---- Actions ---------------------------------------------------------

    private void sendReply() {
        String message = replyInput.getText() == null ? "" : replyInput.getText().toString().trim();
        if (message.isEmpty()) {
            replyInput.setError(getString(R.string.err_reply_empty));
            replyInput.requestFocus();
            return;
        }

        sendButton.setEnabled(false);
        sendButton.setText(R.string.sending_reply);
        ApiClient.get(this).replySupportTicket(ticketId(), message,
                new ApiCallback<SupportTicketData>() {
                    @Override
                    public void onSuccess(SupportTicketData data) {
                        sendButton.setEnabled(true);
                        sendButton.setText(R.string.ticket_send_reply);
                        replyInput.setText("");
                        changed = true;
                        // The response already carries the appended message.
                        render(data == null ? null : data.ticket);
                    }

                    @Override
                    public void onError(String message) {
                        sendButton.setEnabled(true);
                        sendButton.setText(R.string.ticket_send_reply);
                        Toast.makeText(SupportTicketActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void confirmClose() {
        String subject = ticket != null && ticket.subject != null ? ticket.subject : "";
        new AlertDialog.Builder(this)
                .setTitle(R.string.ticket_close_title)
                .setMessage(getString(R.string.ticket_close_message, subject))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.ticket_close, (d, w) -> closeTicket())
                .show();
    }

    private void closeTicket() {
        ApiClient.get(this).closeSupportTicket(ticketId(), new ApiCallback<SupportTicketData>() {
            @Override
            public void onSuccess(SupportTicketData data) {
                changed = true;
                Toast.makeText(SupportTicketActivity.this,
                        R.string.ticket_closed_notice, Toast.LENGTH_SHORT).show();
                render(data == null ? null : data.ticket);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SupportTicketActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void finish() {
        // Tell the Support Centre to reload only when something actually changed.
        setResult(changed ? RESULT_OK : RESULT_CANCELED);
        super.finish();
    }
}
