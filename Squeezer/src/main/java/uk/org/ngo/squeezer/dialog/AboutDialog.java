/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import de.cketti.library.changelog.ChangeLog;
import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.License;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;
import uk.org.ngo.squeezer.R;

public class AboutDialog extends DialogFragment {

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint({"InflateParams"}) // OK, as view is passed to AlertDialog.Builder.setView()
        final View view = getActivity().getLayoutInflater().inflate(R.layout.about_dialog, null);
        final TextView titleText = (TextView) view.findViewById(R.id.about_title);
        final TextView versionText = (TextView) view.findViewById(R.id.version_text);

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo info;
        try {
            info = pm.getPackageInfo(getActivity().getPackageName(), 0);
            versionText.setText(info.versionName);
        } catch (NameNotFoundException e) {
            titleText.setText(getString(R.string.app_name));
        }

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton(R.string.changelog_full_title, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ChangeLog changeLog = new ChangeLog(getActivity());
                changeLog.getFullLogDialog().show();
            }
        });
        builder.setNegativeButton(R.string.dialog_license, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                final Notices notices = new Notices();
                notices.addNotice(new Notice(
                        "Squeezer",
                        "https://github.com/nikclayton/android-squeezer",
                        "Copyright (c) 2009 The Squeezer Authors",
                        new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice(
                        "Guava",
                        "https://github.com/google/guava",
                        "The Guava Authors",
                        new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice(
                        "ckChangeLog",
                        "https://github.com/cketti/ckChangeLog",
                        "Copyright (c) 2013 cketti",
                        new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice(
                        "Crashlytics",
                        "http://www.crashlytics.com",
                        "Copyright (c) 2014 Crashlytics",
                        new CrashlyticsSoftwareLicense()));
                LicensesDialog.Builder builder = new LicensesDialog.Builder(getActivity());

                builder.setNotices(notices)
                        .setIncludeOwnLicense(true)
                        .setShowFullLicenseText(false)
                        .build()
                        .show();
            }
        });
        return builder.create();
    }

    private class CrashlyticsSoftwareLicense extends License {

        private static final long serialVersionUID = 2260887278518124969L;

        @Override
        public String getName() {
            return "Crashlytics Software License";
        }

        public String getSummaryText(Context context) {
            return getContent(context, R.raw.crashlytics_summary);
        }

        public String getFullText(Context context) {
            return getSummaryText(context);
        }

        @Override
        public String getVersion() {
            return "2014-07-15";
        }

        @Override
        public String getUrl() {
            return "http://try.crashlytics.com/terms/";
        }
    }
}
