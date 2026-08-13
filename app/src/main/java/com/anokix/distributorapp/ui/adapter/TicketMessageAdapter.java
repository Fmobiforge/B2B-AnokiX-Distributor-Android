package com.anokix.distributorapp.ui.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.dto.SupportOverviewData;

import java.util.ArrayList;
import java.util.List;

/**
 * A ticket's conversation as a chat thread (New-Design/Support Centre/4.Open-Ticket.png).
 *
 * <p>Three shapes come out of one layout: our own messages right-aligned in a green
 * bubble, a support agent's left-aligned in white with their name, and a system note
 * ("Closed by the merchant.") centred as a small pill with no author or timestamp.
 */
public class TicketMessageAdapter extends RecyclerView.Adapter<TicketMessageAdapter.ViewHolder> {

    private final List<SupportOverviewData.Message> items = new ArrayList<>();

    public void setItems(List<SupportOverviewData.Message> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupportOverviewData.Message message = items.get(position);
        holder.body.setText(message.body == null ? "" : message.body);

        if (message.isSystem()) {
            bindSystem(holder);
            return;
        }
        boolean mine = message.isMine();
        holder.row.setGravity(mine ? Gravity.END : Gravity.START);
        holder.bubble.setBackgroundResource(mine
                ? R.drawable.bg_bubble_mine : R.drawable.bg_bubble_theirs);
        holder.body.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.text_primary));

        // Only an agent's message needs a name — our own are obviously ours.
        boolean showAuthor = !mine && message.author_name != null
                && !message.author_name.trim().isEmpty();
        holder.author.setVisibility(showAuthor ? View.VISIBLE : View.GONE);
        holder.author.setText(showAuthor ? message.author_name : "");

        holder.time.setVisibility(View.VISIBLE);
        holder.time.setText(message.timeLabel());
    }

    private void bindSystem(ViewHolder holder) {
        holder.row.setGravity(Gravity.CENTER_HORIZONTAL);
        holder.bubble.setBackgroundResource(R.drawable.bg_bubble_system);
        holder.body.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), R.color.text_secondary));
        holder.author.setVisibility(View.GONE);
        // A status note is not part of the conversation, so it carries no timestamp.
        holder.time.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout row, bubble;
        final TextView author, body, time;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.messageRow);
            bubble = itemView.findViewById(R.id.bubble);
            author = itemView.findViewById(R.id.messageAuthor);
            body = itemView.findViewById(R.id.messageBody);
            time = itemView.findViewById(R.id.messageTime);
        }
    }
}
