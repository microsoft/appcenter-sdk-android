package com.microsoft.appcenter.sasquatch.listeners;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.DistributeListener;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.UpdateAction;
import com.microsoft.appcenter.sasquatch.R;

public class SasquatchDistributeListener implements DistributeListener {

    @Override
    public boolean onReleaseAvailable(Activity activity, ReleaseDetails releaseDetails) {
        final String releaseNotes = releaseDetails.getReleaseNotes();
        boolean custom = releaseNotes != null && releaseNotes.toLowerCase().contains("custom");
        if (custom) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            dialogBuilder.setTitle(String.format(activity.getString(R.string.version_x_available), releaseDetails.getShortVersion()));
            dialogBuilder.setMessage(releaseNotes);
            dialogBuilder.setPositiveButton(com.microsoft.appcenter.distribute.R.string.appcenter_distribute_update_dialog_download, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Distribute.notifyUpdateAction(UpdateAction.UPDATE);
                }
            });
            dialogBuilder.setCancelable(false);
            if (!releaseDetails.isMandatoryUpdate()) {
                dialogBuilder.setNegativeButton(com.microsoft.appcenter.distribute.R.string.appcenter_distribute_update_dialog_postpone, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Distribute.notifyUpdateAction(UpdateAction.POSTPONE);
                    }
                });
            }
            dialogBuilder.create().show();
        }
        return custom;
    }
}
