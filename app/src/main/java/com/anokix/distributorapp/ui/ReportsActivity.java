package com.anokix.distributorapp.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.Http;
import com.anokix.distributorapp.network.dto.ReportsData;
import com.anokix.distributorapp.ui.adapter.ReportAdapter;
import com.anokix.distributorapp.ui.util.ContextMenuPopup;
import com.anokix.distributorapp.ui.util.FilterUiHelper;
import com.anokix.distributorapp.ui.util.GenerateReportSheet;
import com.anokix.distributorapp.ui.util.ReportFiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reports (New-Design/Report/1.Dashboard.png).
 *
 * <p>{@code GET /reports} answers with everything at once — the catalogue of reports that
 * can be generated, the ones already generated, and the KPI summary — and takes no query
 * parameters. So the keyword box and the Category dropdown filter the loaded list in
 * memory rather than round-tripping; results are instant and no request is wasted on a
 * keystroke. The web layout's tab strip is not ported: it offers the same six categories
 * as the dropdown beside it, and a second scrolling row would cost a thumb-height of
 * phone screen for nothing.
 *
 * <p>Each card downloads its file, and its overflow opens Generate again / Delete
 * (2.Item-Context-Menu.png).
 */
public class ReportsActivity extends BaseSecondaryActivity {

    /** The category filter's options, in the order the web dropdown lists them. */
    private static final String[] CATEGORY_FILTER = {
            "All Categories", "Sales", "Traders", "Inventory", "Deliveries", "Finance", "Products"
    };

    private ReportAdapter adapter;
    private TextView countLabel;
    private EditText searchInput;
    private View searchClear;

    private ReportsData data;
    private final List<ReportsData.Report> visible = new ArrayList<>();

    private String search = "";
    private String selectedCategory = "";

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_reports;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.reports);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.reports_page_subtitle);
    }

    @Override
    protected void bindViews() {
        RecyclerView list = findViewById(R.id.reportsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter();
        adapter.setOnRowActionListener(new ReportAdapter.OnRowAction() {
            @Override
            public void onDownload(ReportsData.Report report) {
                download(report);
            }

            @Override
            public void onMore(ReportsData.Report report, View anchor) {
                showRowMenu(report, anchor);
            }
        });
        // The KPI cards ride in the list header so the report cards keep the rest.
        adapter.setHeaderBinder(this::bindSummaryHeader);
        list.setAdapter(adapter);

        countLabel = findViewById(R.id.reportCount);

        findViewById(R.id.generateReportButton).setOnClickListener(v -> generate(null));

        bindSearch();
        bindCategoryFilter();
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
                // Filtering is local, so there is nothing to debounce.
                search = keyword;
                render();
            }
        });
    }

    private void bindCategoryFilter() {
        TextView label = findViewById(R.id.categoryFilterLabel);
        FilterUiHelper.setDropdownContentDescription(label,
                getString(R.string.report_category_label));
        FilterUiHelper.bindDropdown(
                findViewById(R.id.categoryFilterButton),
                label,
                CATEGORY_FILTER,
                0,
                (index, value) -> {
                    selectedCategory = index == 0 ? "" : value;
                    render();
                });
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getReports(new ApiCallback<ReportsData>() {
            @Override
            public void onSuccess(ReportsData result) {
                data = result;
                render();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ReportsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Filtering -------------------------------------------------------

    private void render() {
        visible.clear();
        List<ReportsData.Report> all = data == null || data.reports == null
                ? new ArrayList<>() : data.reports;
        for (ReportsData.Report report : all) {
            if (matchesCategory(report) && matchesSearch(report)) {
                visible.add(report);
            }
        }

        boolean filtering = !search.isEmpty() || !selectedCategory.isEmpty();
        // Rebinds every row, so the KPI header at position 0 picks up the summary too.
        adapter.setItems(visible, getString(filtering
                ? R.string.reports_empty_filtered : R.string.reports_empty));
        countLabel.setText(getString(R.string.reports_showing_count, visible.size(), all.size()));
    }

    private boolean matchesCategory(ReportsData.Report report) {
        return selectedCategory.isEmpty()
                || (report.category != null
                    && report.category.trim().equalsIgnoreCase(selectedCategory));
    }

    /** The hint promises "name or period", so both are searched — as shown to the user. */
    private boolean matchesSearch(ReportsData.Report report) {
        if (search.isEmpty()) {
            return true;
        }
        String keyword = search.toLowerCase(Locale.getDefault());
        return contains(report.name, keyword)
                || contains(report.category, keyword)
                || contains(report.periodLabel(), keyword)
                || contains(report.generatedDateLabel(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(keyword);
    }

    /** Paints the four KPI cards from the summary block the list call returns. */
    private void bindSummaryHeader(View header) {
        ReportsData.Summary summary = data == null ? null : data.summary;
        setKpi(header, R.id.kpiReportsGenerated, summary == null ? 0 : summary.generated);
        setKpi(header, R.id.kpiReportDownloads, summary == null ? 0 : summary.downloads);
        setKpi(header, R.id.kpiReportsThisMonth, summary == null ? 0 : summary.this_month);

        int failed = summary == null ? 0 : summary.failed;
        setKpi(header, R.id.kpiReportsFailed, failed);
        ((TextView) header.findViewById(R.id.kpiReportsFailedCaption)).setText(
                failed == 0 ? R.string.kpi_reports_failed_none : R.string.kpi_reports_failed_some);
    }

    private void setKpi(View header, int valueId, int value) {
        ((TextView) header.findViewById(valueId))
                .setText(String.format(Locale.getDefault(), "%,d", value));
    }

    // ---- Row actions -----------------------------------------------------

    private void showRowMenu(ReportsData.Report report, View anchor) {
        ContextMenuPopup.show(this, anchor,
                ContextMenuPopup.MenuItem.of(getString(R.string.report_generate_again),
                        () -> generate(report)),
                ContextMenuPopup.MenuItem.destructive(getString(R.string.report_delete),
                        () -> confirmDelete(report)));
    }

    /**
     * @param repeat non-null for "Generate again" — the sheet opens with that report's
     *               category, report and original period already chosen.
     */
    private void generate(ReportsData.Report repeat) {
        if (data == null) {
            return; // the catalogue arrives with the list; nothing to offer yet
        }
        GenerateReportSheet.show(this, data, repeat, message -> {
            Toast.makeText(this,
                    message == null || message.trim().isEmpty()
                            ? getString(R.string.generate_report_title) : message,
                    Toast.LENGTH_LONG).show();
            loadData();
        });
    }

    private void confirmDelete(ReportsData.Report report) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.report_delete_title)
                .setMessage(getString(R.string.report_delete_message,
                        report.name == null ? "" : report.name))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.report_delete, (d, w) -> delete(report))
                .show();
    }

    private void delete(ReportsData.Report report) {
        ApiClient.get(this).deleteReport(report.id, new ApiCallback<String>() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(ReportsActivity.this,
                        message == null || message.trim().isEmpty()
                                ? getString(R.string.report_deleted_msg) : message,
                        Toast.LENGTH_SHORT).show();
                loadData();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ReportsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void download(ReportsData.Report report) {
        Toast.makeText(this, R.string.report_downloading, Toast.LENGTH_SHORT).show();
        ApiClient.get(this).downloadReport(report.id, new ApiCallback<Http.Download>() {
            @Override
            public void onSuccess(Http.Download download) {
                ReportFiles.saveAndOffer(ReportsActivity.this, download,
                        report.suggestedFileName());
                // The server counts the download, so refresh the KPI and the row's meta.
                loadData();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ReportsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
