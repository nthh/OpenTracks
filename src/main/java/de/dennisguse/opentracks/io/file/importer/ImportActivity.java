/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.io.file.importer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.fragments.FileTypeDialogFragment;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.DialogUtils;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * An activity to import files from the external storage.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends FragmentActivity implements FileTypeDialogFragment.FileTypeCaller {

    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 6;

    private static final int DIALOG_PROGRESS_ID = 0;
    private static final int DIALOG_RESULT_ID = 1;

    private String directoryDisplayName;

    private ImportAsyncTask importAsyncTask;
    private ProgressDialog progressDialog;

    private int importedTrackCount;
    private int totalTrackCount;

    private Uri directoryUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                directoryUri = resultData.getData();
                fileTypeDialogStart();
            } else {
                Toast.makeText(this, R.string.external_storage_not_writable, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    private void fileTypeDialogStart() {
        FileTypeDialogFragment.showDialog(getSupportFragmentManager(), R.string.import_selection_title, R.string.import_selection_option);
    }

    @Override
    public void onFileTypeDone(TrackFileFormat trackFileFormat) {
        DocumentFile pickedDirectory = DocumentFile.fromTreeUri(this, directoryUri);

        directoryDisplayName = FileUtils.getPath(pickedDirectory);

        importAsyncTask = new ImportAsyncTask(this, trackFileFormat, pickedDirectory);
        importAsyncTask.execute();
    }

    @Override
    public void onDismissed() {
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS_ID:
                progressDialog = DialogUtils.createHorizontalProgressDialog(
                        this, R.string.import_progress_message, new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                importAsyncTask.cancel(true);
                                dialog.dismiss();
                                onDismissed();
                            }
                        }, directoryDisplayName);
                return progressDialog;
            case DIALOG_RESULT_ID:
                int iconId;
                int titleId;
                String message;
                String totalFiles = getResources()
                        .getQuantityString(R.plurals.files, totalTrackCount, totalTrackCount);
                if (importedTrackCount == totalTrackCount) {
                    if (totalTrackCount == 0) {
                        iconId = R.drawable.ic_dialog_info_24dp;
                        titleId = R.string.import_no_file_title;
                        message = getString(R.string.import_no_file, directoryDisplayName);
                    } else {
                        iconId = R.drawable.ic_dialog_success_24dp;
                        titleId = R.string.generic_success_title;
                        message = getString(R.string.import_success, totalFiles, directoryDisplayName);
                    }
                } else {
                    iconId = R.drawable.ic_dialog_error_24dp;
                    titleId = R.string.generic_error_title;
                    message = getString(R.string.import_error, importedTrackCount, totalFiles, directoryDisplayName);
                }
                return new AlertDialog.Builder(this).setCancelable(true).setIcon(iconId)
                        .setMessage(message).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                dialogInterface.dismiss();
                                onDismissed();
                            }
                        }).setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                dialogInterface.dismiss();
                                onDismissed();
                            }
                        }).setTitle(titleId).create();
            default:
                return null;
        }
    }

    /**
     * Invokes when the associated AsyncTask completes.
     *
     * @param aSuccessCount the number of files successfully imported
     * @param aTotalCount   the number of files to import
     */
    public void onAsyncTaskCompleted(int aSuccessCount, int aTotalCount) {
        importedTrackCount = aSuccessCount;
        totalTrackCount = aTotalCount;
        removeDialog(DIALOG_PROGRESS_ID);
        showDialog(DIALOG_RESULT_ID);
    }

    /**
     * Shows the progress dialog.
     */
    public void showProgressDialog() {
        showDialog(DIALOG_PROGRESS_ID);
    }

    /**
     * Sets the progress dialog value.
     *
     * @param number the number of files imported
     * @param max    the maximum number of files
     */
    public void setProgressDialogValue(int number, int max) {
        if (progressDialog != null) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(max);
            progressDialog.setProgress(Math.min(number, max));
        }
    }
}
