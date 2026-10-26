package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.dto.HcLookupData;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * "Found in the Hirt &amp; Carter catalogue" — one catalogue record with its images and
 * product content, reached from the library or from a barcode lookup.
 *
 * The highlighted thumbnail is the image the server will import as the product picture.
 * "Use this product" opens the Create Product wizard prefilled from the record;
 * "Not this product" returns {@link AddProductFlow#RESULT_REJECTED}.
 */
public class HcProductActivity extends AppCompatActivity {

    private HcLookupData.Product product;
    private final List<HcLookupData.Image> images = new ArrayList<>();
    private int selectedImage;
    private boolean contentOpen;

    private ImageView mainImage;
    private View mainPlaceholder;
    private LinearLayout thumbsRow;

    private final ActivityResultLauncher<Intent> createLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hc_product);

        product = (HcLookupData.Product) getIntent().getSerializableExtra(AddProductFlow.EXTRA_HC_PRODUCT);
        if (product == null) {
            finish();
            return;
        }

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        AddProductFlow.clipToOutline(findViewById(R.id.panel));
        findViewById(R.id.mainImageBox).setClipToOutline(true);

        mainImage = findViewById(R.id.mainImage);
        mainPlaceholder = findViewById(R.id.mainPlaceholder);
        thumbsRow = findViewById(R.id.thumbsRow);

        if (product.images != null) {
            for (HcLookupData.Image image : product.images) {
                if (image != null && image.previewable && !AddProductFlow.isBlank(image.preview_url)) {
                    images.add(image);
                }
            }
        }

        ((TextView) findViewById(R.id.productName)).setText(
                AddProductFlow.isBlank(product.name) ? getString(R.string.hc_unnamed) : product.name);
        buildDetails();
        buildContent();
        renderImages();

        findViewById(R.id.btnUse).setOnClickListener(v -> useProduct());
        findViewById(R.id.btnReject).setOnClickListener(v -> {
            setResult(AddProductFlow.RESULT_REJECTED);
            finish();
        });
    }

    // ---- Images ------------------------------------------------------------

    private void renderImages() {
        HcLookupData.Image current = currentImage();
        if (current != null) {
            mainPlaceholder.setVisibility(View.GONE);
            mainImage.setVisibility(View.VISIBLE);
            mainImage.setContentDescription(imageAlt(current));
            Glide.with(this).load(current.preview_url).fitCenter().into(mainImage);
        } else {
            mainImage.setVisibility(View.GONE);
            mainPlaceholder.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.thumbsScroll).setVisibility(images.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.noImages).setVisibility(images.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.imageHint).setVisibility(current != null ? View.VISIBLE : View.GONE);

        thumbsRow.removeAllViews();
        for (int i = 0; i < images.size(); i++) {
            thumbsRow.addView(buildThumb(images.get(i), i));
        }
    }

    private View buildThumb(HcLookupData.Image image, int index) {
        boolean selected = index == selectedImage;
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(4), dp(4), dp(4), dp(4));
        tile.setBackgroundResource(selected ? R.drawable.bg_hc_thumb_selected : R.drawable.bg_hc_thumb);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(76), ViewGroup.LayoutParams.WRAP_CONTENT);
        if (index > 0) lp.setMarginStart(dp(10));
        tile.setLayoutParams(lp);
        tile.setContentDescription(imageAlt(image));
        tile.setSelected(selected);
        tile.setOnClickListener(v -> {
            selectedImage = index;
            renderImages();
        });

        ImageView thumb = new ImageView(this);
        thumb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(this).load(image.preview_url).fitCenter().into(thumb);
        tile.addView(thumb);

        RobotoTextView caption = new RobotoTextView(this);
        caption.setText(AddProductFlow.isBlank(image.angle_name)
                ? getString(R.string.hc_image_n, index + 1) : image.angle_name);
        caption.setTextSize(11);
        caption.setSingleLine(true);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        caption.setTextColor(color(R.color.text_secondary));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(4);
        caption.setLayoutParams(clp);
        tile.addView(caption);
        return tile;
    }

    @Nullable
    private HcLookupData.Image currentImage() {
        return selectedImage >= 0 && selectedImage < images.size() ? images.get(selectedImage) : null;
    }

    private String imageAlt(HcLookupData.Image image) {
        String name = product.name == null ? "" : product.name;
        return AddProductFlow.isBlank(image.angle_name) ? name : name + " - " + image.angle_name;
    }

    // ---- Details -----------------------------------------------------------

    private void buildDetails() {
        LinearLayout container = findViewById(R.id.detailsContainer);
        String size = join(product.size, product.uom);
        addDetail(container, getString(R.string.label_brand), product.brand);
        addDetail(container, getString(R.string.hc_label_size), size);
        addDetail(container, getString(R.string.hc_label_gtin), product.gtin);
        addDetail(container, getString(R.string.hc_label_article), product.article_number);
    }

    private void addDetail(LinearLayout container, String label, @Nullable String value) {
        if (AddProductFlow.isBlank(value)) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(9), 0, dp(9));

        RobotoTextView labelView = new RobotoTextView(this);
        labelView.setText(label);
        labelView.setTextSize(14);
        labelView.setTextColor(color(R.color.text_secondary));
        labelView.setLayoutParams(new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(labelView);

        RobotoBoldTextView valueView = new RobotoBoldTextView(this);
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(color(R.color.text_primary));
        valueView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(valueView);

        container.addView(row);
        container.addView(divider());
    }

    // ---- Product content ---------------------------------------------------

    private void buildContent() {
        List<HcLookupData.Content> rows = new ArrayList<>();
        if (product.content != null) {
            for (HcLookupData.Content c : product.content) {
                if (c != null && !(AddProductFlow.isBlank(c.value) && AddProductFlow.isBlank(c.name))) {
                    rows.add(c);
                }
            }
        }
        View section = findViewById(R.id.contentSection);
        if (rows.isEmpty()) {
            section.setVisibility(View.GONE);
            return;
        }
        ((TextView) findViewById(R.id.contentTitle)).setText(getString(R.string.hc_product_content, rows.size()));

        LinearLayout list = findViewById(R.id.contentList);
        for (HcLookupData.Content c : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(9), 0, dp(9));

            if (!AddProductFlow.isBlank(c.heading)) {
                RobotoTextView heading = new RobotoTextView(this);
                heading.setText(c.heading);
                heading.setAllCaps(true);
                heading.setTextSize(11);
                heading.setTextColor(color(R.color.text_secondary));
                row.addView(heading);
            }
            if (!AddProductFlow.isBlank(c.name)) {
                RobotoBoldTextView name = new RobotoBoldTextView(this);
                name.setText(c.name);
                name.setTextSize(13);
                name.setTextColor(color(R.color.text_primary));
                row.addView(name);
            }
            RobotoTextView value = new RobotoTextView(this);
            value.setText(c.value == null ? "" : c.value);
            value.setTextSize(13);
            value.setLineSpacing(0, 1.2f);
            value.setTextColor(color(R.color.text_primary));
            value.setTextIsSelectable(true);
            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            vlp.topMargin = dp(3);
            value.setLayoutParams(vlp);
            row.addView(value);

            list.addView(row);
            list.addView(divider());
        }

        View chevron = findViewById(R.id.contentChevron);
        findViewById(R.id.contentHeader).setOnClickListener(v -> {
            contentOpen = !contentOpen;
            list.setVisibility(contentOpen ? View.VISIBLE : View.GONE);
            chevron.animate().rotation(contentOpen ? 90f : 0f).setDuration(150).start();
        });
    }

    // ---- Use ---------------------------------------------------------------

    /** Prefill the Create Product wizard from this record, as the portal's "Use this product". */
    private void useProduct() {
        String fallback = getIntent().getStringExtra(AddProductFlow.EXTRA_BARCODE);
        String gtin = AddProductFlow.isBlank(product.gtin) ? fallback : product.gtin;

        Intent i = new Intent(this, CreateProductActivity.class);
        i.putExtra(CreateProductActivity.EXTRA_BARCODE, gtin);
        i.putExtra(CreateProductActivity.EXTRA_HC_NAME, product.name);
        i.putExtra(CreateProductActivity.EXTRA_HC_BRAND, product.brand);
        i.putExtra(CreateProductActivity.EXTRA_HC_SIZE, product.size);
        i.putExtra(CreateProductActivity.EXTRA_HC_UOM, product.uom);
        HcLookupData.Image image = currentImage();
        if (image != null) {
            i.putExtra(CreateProductActivity.EXTRA_HC_IMAGE_GTIN, gtin);
            i.putExtra(CreateProductActivity.EXTRA_HC_IMAGE_FILE, image.file_name);
            i.putExtra(CreateProductActivity.EXTRA_HC_IMAGE_URL, image.preview_url);
        }
        createLauncher.launch(i);
    }

    // ---- Helpers -----------------------------------------------------------

    private View divider() {
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        line.setBackgroundColor(color(R.color.border));
        return line;
    }

    private static String join(@Nullable String a, @Nullable String b) {
        StringBuilder sb = new StringBuilder();
        if (!AddProductFlow.isBlank(a)) sb.append(a.trim());
        if (!AddProductFlow.isBlank(b)) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(b.trim());
        }
        return sb.toString();
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }

    private int dp(int v) {
        return AddProductFlow.dp(this, v);
    }
}
