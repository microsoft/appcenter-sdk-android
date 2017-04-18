package com.microsoft.azure.mobile.sasquatch.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.microsoft.azure.mobile.distribute.Distribute;
import com.microsoft.azure.mobile.distribute.DistributeListener;
import com.microsoft.azure.mobile.distribute.ReleaseDetails;
import com.microsoft.azure.mobile.distribute.UserUpdateAction;

public class SasquatchDistributeListener implements DistributeListener {

    @Override
    public boolean onNewReleaseAvailable(Activity activity, ReleaseDetails releaseDetails) {
        final String releaseNotes = releaseDetails.getReleaseNotes();
        boolean custom = releaseNotes != null && releaseNotes.toLowerCase().contains("custom");
        if (custom) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            dialogBuilder.setTitle("Version " + releaseDetails.getShortVersion() + " available!");
            if (TextUtils.isEmpty(releaseNotes))
                dialogBuilder.setMessage(com.microsoft.azure.mobile.distribute.R.string.mobile_center_distribute_update_dialog_message);
            else
                dialogBuilder.setMessage(releaseNotes);
            dialogBuilder.setPositiveButton(com.microsoft.azure.mobile.distribute.R.string.mobile_center_distribute_update_dialog_download, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Distribute.notifyUserUpdateAction(UserUpdateAction.DOWNLOAD);
                }
            });
            if (releaseDetails.isMandatoryUpdate()) {
                dialogBuilder.setCancelable(false);
            } else {
                dialogBuilder.setNegativeButton(com.microsoft.azure.mobile.distribute.R.string.mobile_center_distribute_update_dialog_ignore, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Distribute.notifyUserUpdateAction(UserUpdateAction.IGNORE);
                    }
                });
                dialogBuilder.setNeutralButton(com.microsoft.azure.mobile.distribute.R.string.mobile_center_distribute_update_dialog_postpone, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Distribute.notifyUserUpdateAction(UserUpdateAction.POSTPONE);
                    }
                });
                dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Distribute.notifyUserUpdateAction(UserUpdateAction.POSTPONE);
                    }
                });
            }
            dialogBuilder.create().show();
        }
        return custom;
    }
}
