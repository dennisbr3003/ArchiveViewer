package com.dennisbrink.mt.global.mypackedfileviewer.libraries;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import com.dennisbrink.mt.global.mypackedfileviewer.IZipApplication;
import com.dennisbrink.mt.global.mypackedfileviewer.R;
import com.dennisbrink.mt.global.mypackedfileviewer.events.DialogResultActionEvent;
import com.dennisbrink.mt.global.mypackedfileviewer.events.OpenZipLibraryEvent;

import org.greenrobot.eventbus.EventBus;

public class ZipDialogs implements IZipApplication {
    public static void createAndShowDialog(Context context, String title, String text, String action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(title);
        builder.setMessage(text);
        builder.setIcon(R.drawable.confirm_delete);

        builder.setPositiveButton("Yes", (dialog, which) -> {
            // we send "yes" to the receiver
            dialog.dismiss();
            // context.sendBroadcast(createBooleanAnswer(action, ANSWER, true));
            EventBus.getDefault().post(new DialogResultActionEvent(action));
        });

        builder.setNegativeButton("No", (dialog, which) -> {
            // we do not send "no" to the receiver, there will be no action
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static Intent createBooleanAnswer(String action, String name, Boolean value) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(name, value);
        return intent;
    }
}
