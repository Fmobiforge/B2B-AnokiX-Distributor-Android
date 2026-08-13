package com.anokix.distributorapp.ui.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.Http;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

/**
 * Saving and then opening or sharing a downloaded report.
 *
 * <p>Files go to the app's own external Downloads folder, which needs no runtime
 * permission on any supported API level, and are handed to other apps through the
 * {@code fileprovider} already declared in the manifest — a raw {@code file://} Uri
 * would trip {@code FileUriExposedException} from Android 7 onwards.
 */
public final class ReportFiles {

    private static final String TAG = "AxReports";

    private ReportFiles() {
    }

    /**
     * Writes the downloaded bytes to disk and offers View / Share.
     *
     * @param fallbackName used when the server sent no Content-Disposition filename.
     */
    public static void saveAndOffer(@NonNull Activity activity,
                                    @NonNull Http.Download download,
                                    @NonNull String fallbackName) {
        File file = save(activity, download, fallbackName);
        if (file == null) {
            Toast.makeText(activity, R.string.err_save_download, Toast.LENGTH_LONG).show();
            return;
        }
        String mimeType = mimeTypeOf(download, file);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.report_downloaded_title)
                .setMessage(activity.getString(R.string.report_downloaded_message, file.getName()))
                .setNeutralButton(R.string.report_share, (d, w) -> share(activity, file, mimeType))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.report_view, (d, w) -> view(activity, file, mimeType))
                .show();
    }

    @Nullable
    private static File save(Activity activity, Http.Download download, String fallbackName) {
        File directory = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (directory == null) {
            directory = new File(activity.getFilesDir(), "downloads");
        }
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(TAG, "could not create " + directory);
            return null;
        }
        String name = download.fileName != null && !download.fileName.trim().isEmpty()
                ? download.fileName.trim() : fallbackName;
        File file = new File(directory, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(download.content);
            return file;
        } catch (Exception e) {
            Log.e(TAG, "could not write " + file, e);
            return null;
        }
    }

    /** Prefer what the server said; a bare octet-stream tells other apps nothing. */
    private static String mimeTypeOf(Http.Download download, File file) {
        String declared = download.mimeType == null ? "" : download.mimeType.split(";")[0].trim();
        if (!declared.isEmpty() && !declared.equalsIgnoreCase("application/octet-stream")) {
            return declared;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            return "text/csv";
        }
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (name.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        return "application/octet-stream";
    }

    private static void view(Activity activity, File file, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(shareUri(activity, file), mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Plenty of phones ship without a CSV viewer — say so instead of crashing.
            Toast.makeText(activity, activity.getString(R.string.err_no_viewer,
                    extensionOf(file)), Toast.LENGTH_LONG).show();
        }
    }

    private static void share(Activity activity, File file, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_STREAM, shareUri(activity, file))
                .putExtra(Intent.EXTRA_SUBJECT, file.getName())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(intent,
                activity.getString(R.string.report_share_title)));
    }

    private static Uri shareUri(Activity activity, File file) {
        return FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".fileprovider", file);
    }

    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1).toUpperCase(Locale.ROOT);
    }
}
