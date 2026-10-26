package com.anokix.distributorapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.dto.HcLibraryData;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Hirt &amp; Carter library grid: one card per catalogue product, plus a full-width footer
 * that shows a spinner while the next page loads and the data attribution after it.
 */
public class HcLibraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClick {
        void onItemClick(HcLibraryData.Item item);
    }

    /**
     * Read timeout for Hirt &amp; Carter CDN images. The CDN renders each size on first request
     * (measured 2.6–6.4 s uncached, ~0.35 s once cached), so Glide's default 2.5 s timeout
     * abandons most first loads with "SocketTimeoutException: Read timed out".
     */
    public static final int HC_IMAGE_TIMEOUT_MS = 15_000;

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private final List<HcLibraryData.Item> items = new ArrayList<>();
    private final OnItemClick listener;
    private boolean loadingMore;
    /** GTIN being looked up: its card shows a spinner and every card is disabled. */
    @Nullable private String busyGtin;

    public HcLibraryAdapter(OnItemClick listener) {
        this.listener = listener;
    }

    public void setItems(List<HcLibraryData.Item> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setLoadingMore(boolean value) {
        if (loadingMore == value) return;
        loadingMore = value;
        if (!items.isEmpty()) notifyItemChanged(items.size());
    }

    public void setBusyGtin(@Nullable String gtin) {
        busyGtin = gtin;
        notifyItemRangeChanged(0, items.size());
    }

    public boolean isFooter(int position) {
        return position == items.size();
    }

    @Override
    public int getItemCount() {
        return items.isEmpty() ? 0 : items.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return isFooter(position) ? TYPE_FOOTER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOOTER) {
            return new FooterHolder(inflater.inflate(R.layout.item_hc_footer, parent, false));
        }
        return new ItemHolder(inflater.inflate(R.layout.item_hc_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterHolder) {
            FooterHolder f = (FooterHolder) holder;
            f.progress.setVisibility(loadingMore ? View.VISIBLE : View.GONE);
            f.attribution.setVisibility(loadingMore ? View.GONE : View.VISIBLE);
            return;
        }
        ItemHolder h = (ItemHolder) holder;
        HcLibraryData.Item item = items.get(position);

        h.brand.setText(item.brand == null ? "" : item.brand);
        h.brand.setVisibility(item.brand == null || item.brand.trim().isEmpty() ? View.GONE : View.VISIBLE);
        h.name.setText(item.name);
        String size = ((item.size == null ? "" : item.size) + (item.uom == null ? "" : item.uom)).trim();
        h.size.setText(size);
        h.size.setVisibility(size.isEmpty() ? View.GONE : View.VISIBLE);
        h.gtin.setText(item.gtin);
        h.tagNoImage.setVisibility(item.has_images ? View.GONE : View.VISIBLE);
        h.tagAdded.setVisibility(item.already_added ? View.VISIBLE : View.GONE);

        // Products already in the catalogue stay selectable but read as secondary.
        h.card.setBackgroundResource(item.already_added ? R.drawable.bg_hc_card_muted : R.drawable.bg_hc_card);
        h.content.setAlpha(item.already_added ? 0.7f : 1f);
        h.card.setContentDescription(((item.brand == null ? "" : item.brand + " ") + item.name)
                + (item.already_added
                ? " (" + h.itemView.getContext().getString(R.string.hc_tag_added) + ")" : ""));

        if (item.thumb_url != null && !item.thumb_url.trim().isEmpty()) {
            h.boundUrl = item.thumb_url;
            h.thumb.setVisibility(View.VISIBLE);
            h.placeholder.setVisibility(View.GONE);
            Glide.with(h.thumb).load(item.thumb_url).fitCenter()
                    .timeout(HC_IMAGE_TIMEOUT_MS)
                    .listener(new PlaceholderOnError(h, item.thumb_url)).into(h.thumb);
        } else {
            h.boundUrl = null;
            Glide.with(h.thumb).clear(h.thumb);
            h.thumb.setVisibility(View.GONE);
            h.placeholder.setVisibility(View.VISIBLE);
        }

        boolean isBusy = busyGtin != null && busyGtin.equals(item.gtin);
        h.overlay.setVisibility(isBusy ? View.VISIBLE : View.GONE);
        h.card.setEnabled(busyGtin == null);
        h.card.setOnClickListener(v -> {
            if (busyGtin == null && listener != null) listener.onItemClick(item);
        });
    }

    /** Swap a broken image for the placeholder icon, as the portal does. */
    private static final class PlaceholderOnError
            implements com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
        private final ItemHolder holder;
        private final String url;

        PlaceholderOnError(ItemHolder holder, String url) {
            this.holder = holder;
            this.url = url;
        }

        @Override
        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model,
                                    @NonNull com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                    boolean isFirstResource) {
            holder.thumb.post(() -> {
                if (!url.equals(holder.boundUrl)) return;   // the card was recycled meanwhile
                holder.thumb.setVisibility(View.GONE);
                holder.placeholder.setVisibility(View.VISIBLE);
            });
            return true;
        }

        @Override
        public boolean onResourceReady(@NonNull android.graphics.drawable.Drawable resource, @NonNull Object model,
                                       com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                       @NonNull com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    }

    static final class ItemHolder extends RecyclerView.ViewHolder {
        final View card, content, overlay;
        final ImageView thumb, placeholder;
        final TextView brand, name, size, gtin, tagNoImage, tagAdded;
        @Nullable String boundUrl;

        ItemHolder(View v) {
            super(v);
            card = v.findViewById(R.id.card);
            content = v.findViewById(R.id.cardContent);
            overlay = v.findViewById(R.id.busyOverlay);
            thumb = v.findViewById(R.id.thumb);
            placeholder = v.findViewById(R.id.thumbPlaceholder);
            brand = v.findViewById(R.id.brand);
            name = v.findViewById(R.id.name);
            size = v.findViewById(R.id.size);
            gtin = v.findViewById(R.id.gtin);
            tagNoImage = v.findViewById(R.id.tagNoImage);
            tagAdded = v.findViewById(R.id.tagAdded);
            v.findViewById(R.id.thumbWell).setClipToOutline(true);
        }
    }

    static final class FooterHolder extends RecyclerView.ViewHolder {
        final View progress, attribution;

        FooterHolder(View v) {
            super(v);
            progress = v.findViewById(R.id.footerProgress);
            attribution = v.findViewById(R.id.footerAttribution);
        }
    }
}
