package com.anokix.distributorapp.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.SupportArticlesData;
import com.anokix.distributorapp.network.dto.SupportOverviewData;
import com.anokix.distributorapp.ui.util.SupportSheets;

import java.util.ArrayList;
import java.util.List;

/**
 * Support Centre (New-Design/Support Centre/1.Dashboard.png).
 *
 * <p>{@code GET support/overview} answers with everything: which contact channels are
 * switched on, the help-article catalogue, and this account's tickets. Contact cards are
 * built from the enabled channels only — a disabled channel gets no card rather than a
 * dead one — and the help section stays hidden until there is something to show.
 *
 * <p>The web page's System Health and Knowledge Base panels are not ported; the
 * requirements mark both as unused, and on a phone they would push the two things that
 * matter below the fold.
 */
public class SupportActivity extends BaseSecondaryActivity {

    /** Wait for a pause in typing before spending a request on the keyword. */
    private static final long SEARCH_DEBOUNCE_MS = 400;

    private LinearLayout channelsContainer;
    private LinearLayout articlesSection;
    private LinearLayout articlesContainer;
    private LinearLayout ticketsContainer;
    private TextView articlesTitle;
    private TextView articlesEmpty;
    private TextView ticketsEmpty;
    private TextView supportHours;
    private EditText searchInput;
    private View searchClear;

    private SupportOverviewData overview;
    private String search = "";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    /** A reply or a close changes the ticket's state, so refresh the list on the way back. */
    private final ActivityResultLauncher<Intent> ticketLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadData();
                }
            });

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_support;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.support_centre);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.support_subtitle);
    }

    @Override
    protected void bindViews() {
        channelsContainer = findViewById(R.id.channelsContainer);
        articlesSection = findViewById(R.id.articlesSection);
        articlesContainer = findViewById(R.id.articlesContainer);
        articlesTitle = findViewById(R.id.articlesTitle);
        articlesEmpty = findViewById(R.id.articlesEmpty);
        ticketsContainer = findViewById(R.id.ticketsContainer);
        ticketsEmpty = findViewById(R.id.ticketsEmpty);
        supportHours = findViewById(R.id.supportHours);

        findViewById(R.id.newTicketButton).setOnClickListener(v -> openCreateTicket());
        bindSearch();
    }

    private void bindSearch() {
        searchInput = findViewById(R.id.searchInput);
        searchClear = findViewById(R.id.searchClear);
        searchClear.setOnClickListener(v -> searchInput.setText(""));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s == null ? "" : s.toString().trim();
                searchClear.setVisibility(keyword.isEmpty() ? View.GONE : View.VISIBLE);
                if (keyword.equals(search)) {
                    return;
                }
                if (pendingSearch != null) {
                    searchHandler.removeCallbacks(pendingSearch);
                }
                pendingSearch = () -> {
                    search = keyword;
                    if (search.isEmpty()) {
                        // Back to the popular list rather than an empty result set.
                        renderArticles(overview == null ? null : overview.popular, false);
                    } else {
                        searchArticles(search);
                    }
                };
                searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getSupportOverview(new ApiCallback<SupportOverviewData>() {
            @Override
            public void onSuccess(SupportOverviewData data) {
                overview = data;
                renderChannels();
                renderTickets();
                if (search.isEmpty()) {
                    renderArticles(data == null ? null : data.popular, false);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SupportActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Contact channels ------------------------------------------------

    /**
     * One card per enabled channel, in the order the web page lists them. Nothing is
     * rendered for a channel the API reports as off, so the page never shows a control
     * that cannot do anything.
     */
    private void renderChannels() {
        channelsContainer.removeAllViews();
        SupportOverviewData.Channels channels = overview == null ? null : overview.channels;
        if (channels == null) {
            return;
        }

        if (channels.whatsapp != null && channels.whatsapp.enabled) {
            String display = isBlank(channels.whatsapp.display)
                    ? getString(R.string.channel_whatsapp_sub) : channels.whatsapp.display;
            addChannel(R.drawable.ic_whatsapp, R.string.channel_whatsapp, display,
                    R.color.success, R.drawable.bg_kpi_icon_green,
                    v -> openWhatsApp(channels.whatsapp));
        }
        if (channels.callback != null && channels.callback.enabled) {
            addChannel(R.drawable.ic_phone, R.string.channel_callback,
                    getString(R.string.channel_callback_sub),
                    R.color.purple_primary, R.drawable.bg_kpi_icon_purple,
                    v -> openCallbackRequest());
        }
        if (channels.tickets != null && channels.tickets.enabled) {
            addChannel(R.drawable.ic_document, R.string.channel_ticket,
                    getString(R.string.channel_ticket_sub),
                    R.color.purple_primary, R.drawable.bg_kpi_icon_purple,
                    v -> openCreateTicket());
        }
        if (channels.phone != null && channels.phone.enabled && !isBlank(channels.phone.number)) {
            addChannel(R.drawable.ic_headset, R.string.channel_phone, channels.phone.number,
                    R.color.info, R.drawable.bg_kpi_icon_blue,
                    v -> dial(channels.phone.number));
        }
        if (channels.email != null && channels.email.enabled && !isBlank(channels.email.address)) {
            addChannel(R.drawable.ic_email, R.string.channel_email, channels.email.address,
                    R.color.info, R.drawable.bg_kpi_icon_blue,
                    v -> email(channels.email.address));
        }
        if (channels.live_chat != null && channels.live_chat.enabled) {
            addChannel(R.drawable.ic_chat, R.string.channel_live_chat,
                    getString(R.string.channel_live_chat_sub),
                    R.color.warning, R.drawable.bg_kpi_icon_orange,
                    v -> Toast.makeText(this, getString(R.string.feature_coming_soon,
                            getString(R.string.channel_live_chat)), Toast.LENGTH_SHORT).show());
        }

        boolean hasHours = !isBlank(channels.hours);
        supportHours.setVisibility(hasHours ? View.VISIBLE : View.GONE);
        if (hasHours) {
            supportHours.setText(getString(R.string.support_hours, channels.hours.trim()));
        }
    }

    private void addChannel(int iconRes, int titleRes, String subtitle,
                            int tintRes, int tileRes, View.OnClickListener onClick) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_support_channel, channelsContainer, false);
        ImageView icon = card.findViewById(R.id.channelIcon);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ContextCompat.getColorStateList(this, tintRes));
        card.findViewById(R.id.channelIconTile).setBackgroundResource(tileRes);
        ((TextView) card.findViewById(R.id.channelTitle)).setText(titleRes);
        ((TextView) card.findViewById(R.id.channelSubtitle)).setText(subtitle);
        card.setOnClickListener(onClick);
        channelsContainer.addView(card);
    }

    // ---- Help articles ---------------------------------------------------

    private void searchArticles(String keyword) {
        ApiClient.get(this).searchSupportArticles(keyword, new ApiCallback<SupportArticlesData>() {
            @Override
            public void onSuccess(SupportArticlesData data) {
                // A slow response for an abandoned keyword must not replace the current one.
                if (!keyword.equals(search)) {
                    return;
                }
                renderArticles(data == null ? null : data.articles, true);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SupportActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * @param searching true when showing search results — the section then keeps its
     *                  place and explains a miss, instead of silently disappearing.
     */
    private void renderArticles(List<SupportOverviewData.Article> articles, boolean searching) {
        List<SupportOverviewData.Article> items = articles == null
                ? new ArrayList<>() : articles;

        // With nothing typed and no popular articles there is nothing worth a heading.
        if (!searching && items.isEmpty()) {
            articlesSection.setVisibility(View.GONE);
            return;
        }
        articlesSection.setVisibility(View.VISIBLE);
        articlesTitle.setText(searching
                ? getString(R.string.support_search_results, search)
                : getString(R.string.support_popular_topics));

        articlesContainer.removeAllViews();
        for (SupportOverviewData.Article article : items) {
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_support_article, articlesContainer, false);
            ((TextView) card.findViewById(R.id.articleTitle))
                    .setText(article.title == null ? "" : article.title);
            TextView excerpt = card.findViewById(R.id.articleExcerpt);
            boolean hasExcerpt = !isBlank(article.excerpt);
            excerpt.setVisibility(hasExcerpt ? View.VISIBLE : View.GONE);
            excerpt.setText(hasExcerpt ? article.excerpt : "");
            card.setOnClickListener(v -> openArticle(article));
            articlesContainer.addView(card);
        }

        boolean empty = items.isEmpty();
        articlesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        articlesEmpty.setText(empty ? getString(R.string.support_search_empty, search) : "");
    }

    private void openArticle(SupportOverviewData.Article article) {
        if (isBlank(article.url)) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(article.url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.err_no_browser, Toast.LENGTH_LONG).show();
        }
    }

    // ---- Tickets ---------------------------------------------------------

    private void renderTickets() {
        ticketsContainer.removeAllViews();
        List<SupportOverviewData.Ticket> tickets = overview == null || overview.tickets == null
                ? new ArrayList<>() : overview.tickets;

        ticketsEmpty.setVisibility(tickets.isEmpty() ? View.VISIBLE : View.GONE);
        for (SupportOverviewData.Ticket ticket : tickets) {
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_support_ticket, ticketsContainer, false);

            ((TextView) card.findViewById(R.id.ticketNumber))
                    .setText(ticket.ticket_number == null ? "" : ticket.ticket_number);
            ((TextView) card.findViewById(R.id.ticketSubject))
                    .setText(ticket.subject == null ? "" : ticket.subject);
            ((TextView) card.findViewById(R.id.ticketTime)).setText(ticket.lastActivityLabel());

            TextView badge = card.findViewById(R.id.ticketStatusBadge);
            badge.setText(ticket.statusLabel());
            badge.setTextColor(ContextCompat.getColor(this,
                    ticket.is_open ? R.color.danger : R.color.success));
            badge.setBackgroundResource(ticket.is_open
                    ? R.drawable.bg_badge_danger : R.drawable.bg_badge_success);

            ((TextView) card.findViewById(R.id.ticketState)).setText(stateLabel(ticket));

            card.setOnClickListener(v -> ticketLauncher.launch(
                    SupportTicketActivity.intentFor(this, ticket.id, ticket.ticket_number,
                            ticket.subject)));
            ticketsContainer.addView(card);
        }
    }

    /** Who the ticket is waiting on — the line the web row shows under the subject. */
    private String stateLabel(SupportOverviewData.Ticket ticket) {
        if (!ticket.is_open) {
            return getString(R.string.ticket_resolved);
        }
        return getString(ticket.isWaitingForSupport()
                ? R.string.ticket_waiting_support : R.string.ticket_support_replied);
    }

    // ---- Actions ---------------------------------------------------------

    private void openCallbackRequest() {
        SupportSheets.showCallbackRequest(this, message -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            loadData();   // the new ticket belongs in the list straight away
        });
    }

    private void openCreateTicket() {
        SupportSheets.showCreateTicket(this,
                overview == null ? null : overview.categories, message -> {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    loadData();
                });
    }

    /**
     * Opens the conversation in the WhatsApp app rather than the browser: the wa.me link
     * is handed straight to the WhatsApp package, and only if that is not installed do we
     * fall back to letting the system resolve the URL.
     */
    private void openWhatsApp(SupportOverviewData.Whatsapp whatsapp) {
        String link = isBlank(whatsapp.link) ? "" : whatsapp.link.trim();
        if (link.isEmpty()) {
            Toast.makeText(this, R.string.err_no_whatsapp, Toast.LENGTH_LONG).show();
            return;
        }
        Intent direct = new Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage("com.whatsapp");
        try {
            startActivity(direct);
            return;
        } catch (ActivityNotFoundException ignored) {
            // WhatsApp Business registers a different package; let the system choose.
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.err_no_whatsapp, Toast.LENGTH_LONG).show();
        }
    }

    private void dial(String number) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.trim())));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.err_no_dialer, Toast.LENGTH_LONG).show();
        }
    }

    private void email(String address) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address.trim()));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_subject));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.err_no_email_app, Toast.LENGTH_LONG).show();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void onDestroy() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        super.onDestroy();
    }
}
