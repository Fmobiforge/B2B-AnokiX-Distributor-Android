package com.anokix.distributorapp.network.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * {@code GET api/distributor/reports} — the Reports dashboard in one call.
 *
 * <p>{@link #available} is the catalogue the Generate dialog offers, {@link #reports} is
 * everything already generated, and {@link #summary} feeds the four KPI cards. The
 * endpoint takes no query parameters, so the screen's search and category filters run
 * over this list locally rather than round-tripping.
 */
public class ReportsData {

    public List<Available> available;
    public List<Report> reports;
    public Summary summary;

    /** A report the distributor may generate — the options in the Report dropdown. */
    public static class Available {
        public String key;
        public String name;
        public String category;
        public String about;
    }

    /** One generated report: the web table's row. */
    public static class Report {
        public String id;
        public String name;
        public String category;
        public String period_from;
        public String period_to;
        public String format;
        public long size_bytes;
        public int row_count;
        public String status;
        public String error;
        public int downloads;
        public String created_at;

        /** "14 Jul – 13 Aug 2026" — the year is only repeated when the range crosses one. */
        public String periodLabel() {
            Date from = parseDate(period_from);
            Date to = parseDate(period_to);
            if (from == null || to == null) {
                return join(period_from, period_to);
            }
            return sameYear(from, to)
                    ? format(from, "d MMM") + " – " + format(to, "d MMM yyyy")
                    : format(from, "d MMM yyyy") + " – " + format(to, "d MMM yyyy");
        }

        /** "13 Aug 2026" — the day the file was produced. */
        public String generatedDateLabel() {
            Date when = parseDateTime(created_at);
            return when == null ? safe(created_at) : format(when, "d MMM yyyy");
        }

        /** "10:51" — shown under the date, as the web table does. */
        public String generatedTimeLabel() {
            Date when = parseDateTime(created_at);
            return when == null ? "" : format(when, "HH:mm");
        }

        /** "933 B" / "12,4 KB" — file sizes here are small, so B/KB/MB is enough. */
        public String sizeLabel() {
            if (size_bytes <= 0) {
                return "—";
            }
            if (size_bytes < 1024) {
                return size_bytes + " B";
            }
            if (size_bytes < 1024 * 1024) {
                return String.format(Locale.getDefault(), "%.1f KB", size_bytes / 1024f);
            }
            return String.format(Locale.getDefault(), "%.1f MB", size_bytes / (1024f * 1024f));
        }

        public String formatLabel() {
            return isBlank(format) ? "CSV" : format.toUpperCase(Locale.ROOT);
        }

        /** Only a "ready" report has a file behind it, so only then may we download. */
        public boolean isReady() {
            return status == null || "ready".equalsIgnoreCase(status.trim());
        }

        /**
         * Name for the downloaded file. The server sends its own in Content-Disposition;
         * this is the fallback, and stays unique per report so two downloads of different
         * reports never overwrite each other.
         */
        public String suggestedFileName() {
            String base = isBlank(name) ? "report" : name.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            return base + "-" + safe(id) + "." + formatLabel().toLowerCase(Locale.ROOT);
        }
    }

    public static class Summary {
        public int generated;
        public int downloads;
        public int this_month;
        public int failed;
    }

    /**
     * Resolves a generated report back to its catalogue entry so "Generate again" can
     * send a {@code report_key}. The row only carries the display name, so match on that
     * first and fall back to the category — two reports never share one category here.
     */
    public Available findAvailable(String name, String category) {
        if (available == null) {
            return null;
        }
        for (Available option : available) {
            if (!isBlank(name) && name.equalsIgnoreCase(safe(option.name))) {
                return option;
            }
        }
        for (Available option : available) {
            if (!isBlank(category) && category.equalsIgnoreCase(safe(option.category))) {
                return option;
            }
        }
        return null;
    }

    // ---- Formatting helpers ---------------------------------------------

    private static Date parseDate(String raw) {
        return parse(raw, "yyyy-MM-dd");
    }

    private static Date parseDateTime(String raw) {
        Date exact = parse(raw, "yyyy-MM-dd HH:mm:ss");
        return exact != null ? exact : parseDate(raw);
    }

    private static Date parse(String raw, String pattern) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            return new SimpleDateFormat(pattern, Locale.US).parse(raw.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private static String format(Date date, String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
    }

    private static boolean sameYear(Date a, Date b) {
        return format(a, "yyyy").equals(format(b, "yyyy"));
    }

    private static String join(String from, String to) {
        if (isBlank(from)) {
            return safe(to);
        }
        return isBlank(to) ? safe(from) : from + " – " + to;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
