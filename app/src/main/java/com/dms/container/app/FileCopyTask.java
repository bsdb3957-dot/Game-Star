package com.dms.container.app;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import net_62v.external.MetaApplicationInstaller;
import net_62v.external.MetaActivityManager;
import net_62v.external.MetaPackageManager;
import net_62v.external.MetaStorageManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import androidx.documentfile.provider.DocumentFile;
import java.io.*;

public class FileCopyTask {
    private Context context;
    private ProgressDialog progressDialog;
    private String erro;

    public FileCopyTask(Context context) {
        this.context = context;
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Copying file...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
    }

    public void copyObbAsync(final String uris, final String sourceFileName, final String destinationPath) {
        new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected void onPreExecute() {
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    // Get the SAF directory URI
                    Uri directoryUri = Uri.parse(uris);
                    DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);

                    // Find the specific file within the directory
                    DocumentFile obbFile = null;
                    for (DocumentFile file : directory.listFiles()) {
                        if (sourceFileName.equals(file.getName())) {
                            obbFile = file;
                            break;
                        }
                    }

                    if (obbFile == null) {
                        return false; // File not found in the directory
                    }

                    ContentResolver contentResolver = context.getContentResolver();

                    // Check if the destination directory exists
                    File destinationDirectory = new File(Environment.getExternalStorageDirectory(), destinationPath);
                    if (!destinationDirectory.exists()) {
                        if (!destinationDirectory.mkdirs()) {
                            return false; // Failed to create the destination directory
                        }
                    }

                    // Create an InputStream for the source file
                    Uri sourceUri = obbFile.getUri();
                    InputStream inputStream = contentResolver.openInputStream(sourceUri);

                    // Create an OutputStream for the destination file in external storage
                    File destinationFile = new File(destinationDirectory, sourceFileName);
                    OutputStream outputStream = new FileOutputStream(destinationFile);

                    // Copy data from InputStream to OutputStream with progress update
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytes = obbFile.length(); // Get the total bytes for progress calculation
                    long copiedBytes = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        copiedBytes += bytesRead;
                        int progress = (int) ((copiedBytes * 100) / totalBytes);
                        publishProgress(progress);
                    }

                    // Close streams
                    inputStream.close();
                    outputStream.close();

                    return true; // File successfully copied
                } catch (IOException e) {
                    e.printStackTrace();
                    erro = e.getMessage();
                    return false; // Error occurred during file copy
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progressDialog.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                progressDialog.dismiss();
                if (result) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Success");
                    builder.setMessage("The Game has been installed, Please restart");
                    builder.setPositiveButton("OK", null);
                    builder.setCancelable(false);
                    builder.show();
                    //FCore.get().installPackageAsUser("com.pubg.imobile",0);
                    MetaApplicationInstaller.cloneApp("com.pubg.imobile");
                    // File successfully copied
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Error");
                    builder.setMessage(erro);
                    builder.setPositiveButton("OK", null);
                    builder.show();
                }
            }
        }.execute();
    }
}
