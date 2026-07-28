package com.anokix.distributorapp.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Value formatting for the anokiX wallet screen. The web portal renders South African
 * money as {@code R41 418,50} — space thousands, comma decimals — so the app matches it
 * rather than the {@code R41,418.50} used by the older finance screens.
 */
public final class WalletFormat {

    private WalletFormat() { }

    /** {@code 41418.5} → {@code "R41 418,50"}; negatives come back as {@code "-R480,00"}. */
    public static String money(double value) {
        String digits = String.format(Locale.US, "%,.2f", Math.abs(value))
                .replace(',', ' ')
                .replace('.', ',');
        return (value < 0 ? "-R" : "R") + digits;
    }

    /** Same as {@link #money} but credits keep an explicit {@code +}. */
    public static String signedMoney(double value) {
        return value > 0 ? "+" + money(value) : money(value);
    }

    /** The API sends transaction amounts as a signed string; bad input reads as 0. */
    public static double parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(raw.trim().replace(" ", "").replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** ISO-8601 ledger timestamp → {@link Date}, or null when unparseable. */
    public static Date parseIso(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        // "2026-07-28T15:32:55+02:00" — strip the colon in the offset for SimpleDateFormat's Z.
        String normalised = iso.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2").replace("Z", "+0000");
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.US).parse(normalised);
            } catch (Exception ignored) {
                // try the next shape
            }
        }
        return null;
    }

    /** "Today, 15:32" / "Yesterday, 08:11" / "15 Jul 2026, 15:58". */
    public static String relativeDateTime(String iso) {
        Date date = parseIso(iso);
        if (date == null) return iso == null ? "" : iso;

        Calendar then = Calendar.getInstance();
        then.setTime(date);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        String time = new SimpleDateFormat("HH:mm", Locale.US).format(date);
        if (isSameDay(then, today)) return "Today, " + time;
        if (isSameDay(then, yesterday)) return "Yesterday, " + time;
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(date);
    }

    /** "as of" clock time from the balance timestamp; "" when unparseable. */
    public static String asOfTime(String iso) {
        Date date = parseIso(iso);
        if (date == null) return "";
        return new SimpleDateFormat("HH:mm", Locale.US).format(date);
    }

    /** Account number → the masked form shown on the settlement card ({@code ***3345}). */
    public static String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "***";
        return "***" + accountNumber.substring(accountNumber.length() - 4);
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
