/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.mms.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.Message;
import android.os.Handler;
import android.os.Bundle;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.SearchRecentSuggestions;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.PhoneConstants;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.Recycler;
import java.util.ArrayList;


/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class MessagingPreferenceActivity extends PreferenceActivity
            implements OnPreferenceChangeListener {
    private static final String TAG = "MessagingPreferenceActivity";
    // Symbolic names for the keys used for preference lookup
    public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";
    public static final String EXPIRY_TIME              = "pref_key_mms_expiry";
    public static final String EXPIRY_TIME_SLOT1        = "pref_key_mms_expiry_slot1";
    public static final String EXPIRY_TIME_SLOT2        = "pref_key_mms_expiry_slot2";
    public static final String PRIORITY                 = "pref_key_mms_priority";
    public static final String READ_REPORT_MODE         = "pref_key_mms_read_reports";
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";
    public static final String SMS_DELIVERY_REPORT_SUB1 = "pref_key_sms_delivery_reports_slot1";
    public static final String SMS_DELIVERY_REPORT_SUB2 = "pref_key_sms_delivery_reports_slot2";
    public static final String NOTIFICATION_ENABLED     = "pref_key_enable_notifications";
    public static final String NOTIFICATION_VIBRATE     = "pref_key_vibrate";
    public static final String NOTIFICATION_VIBRATE_WHEN= "pref_key_vibrateWhen";
    public static final String NOTIFICATION_RINGTONE    = "pref_key_ringtone";
    public static final String AUTO_RETRIEVAL           = "pref_key_mms_auto_retrieval";
    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";
    public static final String AUTO_DELETE              = "pref_key_auto_delete";
    public static final String GROUP_MMS_MODE           = "pref_key_mms_group_mms";
    public static final String SMS_CDMA_PRIORITY        = "pref_key_sms_cdma_priority";

    // Expiry of MMS
    private final static String EXPIRY_ONE_WEEK = "604800"; // 7 * 24 * 60 * 60
    private final static String EXPIRY_TWO_DAYS = "172800"; // 2 * 24 * 60 * 60

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS    = 1;

    // Preferences for enabling and disabling SMS
    private Preference mSmsDisabledPref;
    private Preference mSmsEnabledPref;

    private PreferenceCategory mStoragePrefCategory;
    private PreferenceCategory mSmsPrefCategory;
    private PreferenceCategory mMmsPrefCategory;
    private PreferenceCategory mNotificationPrefCategory;
    private PreferenceCategory mSmscPrefCate;

    private Preference mSmsLimitPref;
    private Preference mSmsDeliveryReportPref;
    private Preference mSmsDeliveryReportPrefSub1;
    private Preference mSmsDeliveryReportPrefSub2;
    private Preference mMmsLimitPref;
    private Preference mMmsDeliveryReportPref;
    private Preference mMmsGroupMmsPref;
    private Preference mMmsReadReportPref;
    private Preference mManageSimPref;
    private Preference mManageSim1Pref;
    private Preference mManageSim2Pref;
    private Preference mClearHistoryPref;
    private CheckBoxPreference mVibratePref;
    private CheckBoxPreference mEnableNotificationsPref;
    private CheckBoxPreference mMmsAutoRetrievialPref;
    private ListPreference mMmsExpiryPref;
    private ListPreference mMmsExpiryCard1Pref;
    private ListPreference mMmsExpiryCard2Pref;
    private RingtonePreference mRingtonePref;
    private ListPreference mSmsStorePref;
    private ListPreference mSmsStoreCard1Pref;
    private ListPreference mSmsStoreCard2Pref;
    private ListPreference mSmsValidityPref;
    private ListPreference mSmsValidityCard1Pref;
    private ListPreference mSmsValidityCard2Pref;
    private Recycler mSmsRecycler;
    private Recycler mMmsRecycler;
    private Preference mSmsTemplate;
    private CheckBoxPreference mSmsSignaturePref;
    private EditTextPreference mSmsSignatureEditPref;
    private ArrayList<Preference> mSmscPrefList = new ArrayList<Preference>();
    private static final int CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG = 3;

    // Whether or not we are currently enabled for SMS. This field is updated in onResume to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;
    private static final String SMSC_DIALOG_TITLE = "title";
    private static final String SMSC_DIALOG_NUMBER = "smsc";
    private static final String SMSC_DIALOG_SUB = "sub";
    private static final int EVENT_SET_SMSC_DONE = 0;
    private static final int EVENT_GET_SMSC_DONE = 1;
    private static final String EXTRA_EXCEPTION = "exception";
    private static SmscHandler mHandler = null;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                    PreferenceCategory smsCategory =
                            (PreferenceCategory)findPreference("pref_key_sms_settings");
                    if (smsCategory != null) {
                        updateSIMSMSPref();
                    }
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                    updateSMSCPref();
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new SmscHandler(this);
        loadPrefs();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        if (isSmsEnabled != mIsSmsEnabled) {
            mIsSmsEnabled = isSmsEnabled;
            invalidateOptionsMenu();
        }

        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
        setEnabledNotificationsPref();
        // Initialize the sms signature
        updateSignatureStatus();
        registerListeners();
        updateSmsEnabledState();
        updateSMSCPref();
    }

    private void updateSmsEnabledState() {
        // Show the right pref (SMS Disabled or SMS Enabled)
        PreferenceScreen prefRoot = (PreferenceScreen)findPreference("pref_key_root");
        if (!mIsSmsEnabled) {
            prefRoot.addPreference(mSmsDisabledPref);
            prefRoot.removePreference(mSmsEnabledPref);
        } else {
            prefRoot.removePreference(mSmsDisabledPref);
            prefRoot.addPreference(mSmsEnabledPref);
        }

        // Enable or Disable the settings as appropriate
        mStoragePrefCategory.setEnabled(mIsSmsEnabled);
        mSmsPrefCategory.setEnabled(mIsSmsEnabled);
        mMmsPrefCategory.setEnabled(mIsSmsEnabled);
        mNotificationPrefCategory.setEnabled(mIsSmsEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private void loadPrefs() {
        addPreferencesFromResource(R.xml.preferences);

        mSmsDisabledPref = findPreference("pref_key_sms_disabled");
        mSmsEnabledPref = findPreference("pref_key_sms_enabled");

        mStoragePrefCategory = (PreferenceCategory)findPreference("pref_key_storage_settings");
        mSmsPrefCategory = (PreferenceCategory)findPreference("pref_key_sms_settings");
        mMmsPrefCategory = (PreferenceCategory)findPreference("pref_key_mms_settings");
        mNotificationPrefCategory =
                (PreferenceCategory)findPreference("pref_key_notification_settings");

        mManageSimPref = findPreference("pref_key_manage_sim_messages");
        mManageSim1Pref = findPreference("pref_key_manage_sim_messages_slot1");
        mManageSim2Pref = findPreference("pref_key_manage_sim_messages_slot2");
        mSmsLimitPref = findPreference("pref_key_sms_delete_limit");
        mSmsDeliveryReportPref = findPreference("pref_key_sms_delivery_reports");
        mSmsDeliveryReportPrefSub1 = findPreference("pref_key_sms_delivery_reports_slot1");
        mSmsDeliveryReportPrefSub2 = findPreference("pref_key_sms_delivery_reports_slot2");
        mMmsDeliveryReportPref = findPreference("pref_key_mms_delivery_reports");
        mMmsGroupMmsPref = findPreference("pref_key_mms_group_mms");
        mMmsReadReportPref = findPreference("pref_key_mms_read_reports");
        mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
        mClearHistoryPref = findPreference("pref_key_mms_clear_history");
        mEnableNotificationsPref = (CheckBoxPreference) findPreference(NOTIFICATION_ENABLED);
        mMmsAutoRetrievialPref = (CheckBoxPreference) findPreference(AUTO_RETRIEVAL);
        mMmsExpiryPref = (ListPreference) findPreference("pref_key_mms_expiry");
        mMmsExpiryCard1Pref = (ListPreference) findPreference("pref_key_mms_expiry_slot1");
        mMmsExpiryCard2Pref = (ListPreference) findPreference("pref_key_mms_expiry_slot2");
        mSmsSignaturePref = (CheckBoxPreference) findPreference("pref_key_enable_signature");
        mSmsSignatureEditPref = (EditTextPreference) findPreference("pref_key_edit_signature");
        mVibratePref = (CheckBoxPreference) findPreference(NOTIFICATION_VIBRATE);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibratePref != null && (vibrator == null || !vibrator.hasVibrator())) {
            mNotificationPrefCategory.removePreference(mVibratePref);
            mVibratePref = null;
        }
        mRingtonePref = (RingtonePreference) findPreference(NOTIFICATION_RINGTONE);
        mSmsTemplate = findPreference("pref_key_message_template");
        mSmsStorePref = (ListPreference) findPreference("pref_key_sms_store");
        mSmsStoreCard1Pref = (ListPreference) findPreference("pref_key_sms_store_card1");
        mSmsStoreCard2Pref = (ListPreference) findPreference("pref_key_sms_store_card2");
        mSmsValidityPref = (ListPreference) findPreference("pref_key_sms_validity_period");
        mSmsValidityCard1Pref
            = (ListPreference) findPreference("pref_key_sms_validity_period_slot1");
        mSmsValidityCard2Pref
            = (ListPreference) findPreference("pref_key_sms_validity_period_slot2");

        setMessagePreferences();
    }

    private void restoreDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        setPreferenceScreen(null);
        // Reset the SMSC preference.
        mSmscPrefList.clear();
        mSmscPrefCate.removeAll();
        loadPrefs();
        updateSmsEnabledState();

        // NOTE: After restoring preferences, the auto delete function (i.e. message recycler)
        // will be turned off by default. However, we really want the default to be turned on.
        // Because all the prefs are cleared, that'll cause:
        // ConversationList.runOneTimeStorageLimitCheckForLegacyMessages to get executed the
        // next time the user runs the Messaging app and it will either turn on the setting
        // by default, or if the user is over the limits, encourage them to turn on the setting
        // manually.
    }

    private void setMessagePreferences() {
        updateSignatureStatus();

        mSmscPrefCate = (PreferenceCategory) findPreference("pref_key_smsc");
        showSmscPref();
        setMessagePriorityPref();

        // Set SIM card SMS management preference
        updateSIMSMSPref();

        if (!MmsConfig.getSMSDeliveryReportsEnabled()) {
            mSmsPrefCategory.removePreference(mSmsDeliveryReportPref);
            mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefSub1);
            mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefSub2);
            if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                getPreferenceScreen().removePreference(mSmsPrefCategory);
            }
        } else {
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPref);
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                    mSmsDeliveryReportPrefSub1.setEnabled(false);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mSmsDeliveryReportPrefSub2.setEnabled(false);
                }
            } else {
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefSub1);
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefSub2);
            }
        }

        setMmsRelatedPref();

        setEnabledNotificationsPref();

        if (getResources().getBoolean(R.bool.config_savelocation)) {
            if (MessageUtils.isMultiSimEnabledMms()) {
                PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
                storageOptions.removePreference(mSmsStorePref);

                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                    mSmsStoreCard1Pref.setEnabled(false);
                } else {
                    setSmsPreferStoreSummary(MessageUtils.SUB1);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mSmsStoreCard2Pref.setEnabled(false);
                } else {
                    setSmsPreferStoreSummary(MessageUtils.SUB2);
                }
            } else {
                PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
                storageOptions.removePreference(mSmsStoreCard1Pref);
                storageOptions.removePreference(mSmsStoreCard2Pref);

                if (!MessageUtils.hasIccCard()) {
                    mSmsStorePref.setEnabled(false);
                } else {
                    setSmsPreferStoreSummary();
                }
            }
        } else {
            PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
            storageOptions.removePreference(mSmsStorePref);
            storageOptions.removePreference(mSmsStoreCard1Pref);
            storageOptions.removePreference(mSmsStoreCard2Pref);
        }
        setSmsValidityPeriodPref();

        // If needed, migrate vibration setting from the previous tri-state setting stored in
        // NOTIFICATION_VIBRATE_WHEN to the boolean setting stored in NOTIFICATION_VIBRATE.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.contains(NOTIFICATION_VIBRATE_WHEN)) {
            String vibrateWhen = sharedPreferences.
                    getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN, null);
            boolean vibrate = "always".equals(vibrateWhen);
            SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
            prefsEditor.putBoolean(NOTIFICATION_VIBRATE, vibrate);
            prefsEditor.remove(NOTIFICATION_VIBRATE_WHEN);  // remove obsolete setting
            prefsEditor.apply();
            mVibratePref.setChecked(vibrate);
        }

        mSmsRecycler = Recycler.getSmsRecycler();
        mMmsRecycler = Recycler.getMmsRecycler();

        // Fix up the recycler's summary with the correct values
        setSmsDisplayLimit();
        setMmsDisplayLimit();
        setMmsExpiryPref();

        String soundValue = sharedPreferences.getString(NOTIFICATION_RINGTONE, null);
        setRingtoneSummary(soundValue);
    }

    private void setMmsRelatedPref() {
        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            getPreferenceScreen().removePreference(mMmsPrefCategory);

            mStoragePrefCategory.removePreference(findPreference("pref_key_mms_delete_limit"));
        } else {
            if (!MmsConfig.getMMSDeliveryReportsEnabled()) {
                mMmsPrefCategory.removePreference(mMmsDeliveryReportPref);
            }
            if (!MmsConfig.getMMSReadReportsEnabled()) {
                mMmsPrefCategory.removePreference(mMmsReadReportPref);
            }
            // If the phone's SIM doesn't know it's own number, disable group mms.
            if (!MmsConfig.getGroupMmsEnabled() ||
                    TextUtils.isEmpty(MessageUtils.getLocalNumber())) {
                mMmsPrefCategory.removePreference(mMmsGroupMmsPref);
            }
        }

        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            if(MessageUtils.getActivatedIccCardCount() < PhoneConstants.MAX_PHONE_COUNT_DUAL_SIM) {
                long subId = SmsManager.getDefault().getDefaultSmsSubId();
                int phoneId = SubscriptionManager.getPhoneId(subId);
                mManageSimPref.setSummary(
                        getString(R.string.pref_summary_manage_sim_messages_slot,
                                phoneId + 1));
            } else {
                mManageSimPref.setSummary(
                        getString(R.string.pref_summary_manage_sim_messages));
            }
            mMmsPrefCategory.removePreference(mMmsExpiryPref);
        } else {
            mMmsPrefCategory.removePreference(mMmsExpiryCard1Pref);
            mMmsPrefCategory.removePreference(mMmsExpiryCard2Pref);
        }
    }

    private void setMessagePriorityPref() {
        // !getResources().getBoolean(R.bool.support_sms_priority)
        if (false) {
            Preference priorotySettings = findPreference(SMS_CDMA_PRIORITY);
            PreferenceScreen prefSet = getPreferenceScreen();
            prefSet.removePreference(priorotySettings);
        }
    }

    private void setSmsValidityPeriodPref() {
        PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_sms_settings");
        if (getResources().getBoolean(R.bool.config_sms_validity)) {
            if (MessageUtils.isMultiSimEnabledMms()) {
                storageOptions.removePreference(mSmsValidityPref);
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                    mSmsValidityCard1Pref.setEnabled(false);
                } else {
                    setSmsPreferValiditySummary(MessageUtils.SUB1);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mSmsValidityCard2Pref.setEnabled(false);
                } else {
                    setSmsPreferValiditySummary(MessageUtils.SUB2);
                }
            } else {
                storageOptions.removePreference(mSmsValidityCard1Pref);
                storageOptions.removePreference(mSmsValidityCard2Pref);
                setSmsPreferValiditySummary(MessageUtils.SUB_INVALID);
            }
        } else {
            storageOptions.removePreference(mSmsValidityPref);
            storageOptions.removePreference(mSmsValidityCard1Pref);
            storageOptions.removePreference(mSmsValidityCard2Pref);
        }
    }

    private void setRingtoneSummary(String soundValue) {
        Uri soundUri = TextUtils.isEmpty(soundValue) ? null : Uri.parse(soundValue);
        Ringtone tone = soundUri != null ? RingtoneManager.getRingtone(this, soundUri) : null;
        mRingtonePref.setSummary(tone != null ? tone.getTitle(this)
                : getResources().getString(R.string.silent_ringtone));
    }

    private void showSmscPref() {
        int count = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < count; i++) {
            final Preference pref = new Preference(this);
            pref.setKey(String.valueOf(i));
            pref.setTitle(getSMSCDialogTitle(count, i));

            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MyEditDialogFragment dialog = MyEditDialogFragment.newInstance(
                            MessagingPreferenceActivity.this,
                            preference.getTitle(),
                            preference.getSummary(),
                            Integer.valueOf(preference.getKey()));
                    dialog.show(getFragmentManager(), "dialog");
                    return true;
                }
            });

            mSmscPrefCate.addPreference(pref);
            mSmscPrefList.add(pref);
        }
        updateSMSCPref();
    }

    private void updateSIMSMSPref() {
        if (MessageUtils.isMultiSimEnabledMms()) {
            if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                mManageSim1Pref.setEnabled(false);
            }
            if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                mManageSim2Pref.setEnabled(false);
            }
            mSmsPrefCategory.removePreference(mManageSimPref);
        } else {
            if (!MessageUtils.hasIccCard()) {
                mManageSimPref.setEnabled(false);
            }
            mSmsPrefCategory.removePreference(mManageSim1Pref);
            mSmsPrefCategory.removePreference(mManageSim2Pref);
        }
    }

    private boolean isAirPlaneModeOn() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private String getSMSCDialogTitle(int count, int index) {
        String title = TelephonyManager.getDefault().isMultiSimEnabled()
                ? getString(R.string.pref_more_smcs, index + 1)
                : getString(R.string.pref_one_smcs);
        return title;
    }

    private void setSmsPreferStoreSummary() {
        mSmsStorePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mSmsStorePref.findIndexOfValue(summary);
                mSmsStorePref.setSummary(mSmsStorePref.getEntries()[index]);
                mSmsStorePref.setValue(summary);
                return true;
            }
        });
        mSmsStorePref.setSummary(mSmsStorePref.getEntry());
    }

    private void setSmsPreferStoreSummary(int subscription) {
        if (MessageUtils.SUB1 == subscription) {
            mSmsStoreCard1Pref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mSmsStoreCard1Pref.findIndexOfValue(summary);
                    mSmsStoreCard1Pref.setSummary(mSmsStoreCard1Pref.getEntries()[index]);
                    mSmsStoreCard1Pref.setValue(summary);
                    return false;
                }
            });
            mSmsStoreCard1Pref.setSummary(mSmsStoreCard1Pref.getEntry());
        } else {
            mSmsStoreCard2Pref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mSmsStoreCard2Pref.findIndexOfValue(summary);
                    mSmsStoreCard2Pref.setSummary(mSmsStoreCard2Pref.getEntries()[index]);
                    mSmsStoreCard2Pref.setValue(summary);
                    return false;
                }
            });
            mSmsStoreCard2Pref.setSummary(mSmsStoreCard2Pref.getEntry());
        }
    }

    private void setSmsPreferValiditySummary(int subscription) {
        switch (subscription) {
            case MessageUtils.SUB_INVALID:
                mSmsValidityPref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String summary = newValue.toString();
                        int index = mSmsValidityPref.findIndexOfValue(summary);
                        mSmsValidityPref.setSummary(mSmsValidityPref.getEntries()[index]);
                        mSmsValidityPref.setValue(summary);
                        return true;
                    }
                });
                mSmsValidityPref.setSummary(mSmsValidityPref.getEntry());
                break;
            case MessageUtils.SUB1:
                mSmsValidityCard1Pref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String summary = newValue.toString();
                        int index = mSmsValidityCard1Pref.findIndexOfValue(summary);
                        mSmsValidityCard1Pref.setSummary(mSmsValidityCard1Pref.getEntries()[index]);
                        mSmsValidityCard1Pref.setValue(summary);
                        return true;
                    }
                });
                mSmsValidityCard1Pref.setSummary(mSmsValidityCard1Pref.getEntry());
                break;
            case MessageUtils.SUB2:
                mSmsValidityCard2Pref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String summary = newValue.toString();
                        int index = mSmsValidityCard2Pref.findIndexOfValue(summary);
                        mSmsValidityCard2Pref.setSummary(mSmsValidityCard2Pref.getEntries()[index]);
                        mSmsValidityCard2Pref.setValue(summary);
                        return true;
                    }
                });
                mSmsValidityCard2Pref.setSummary(mSmsValidityCard2Pref.getEntry());
                break;
            default:
                break;
        }
    }

    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }

    private void setSmsDisplayLimit() {
        mSmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mSmsRecycler.getMessageLimit(this)));
    }

    private void setMmsDisplayLimit() {
        mMmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mMmsRecycler.getMessageLimit(this)));
    }

    private void setMmsExpiryPref() {
        PreferenceCategory mmsSettings =
                (PreferenceCategory)findPreference("pref_key_mms_settings");
        if (MessageUtils.isMultiSimEnabledMms()) {
            mmsSettings.removePreference(mMmsExpiryPref);
            if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                mMmsExpiryCard1Pref.setEnabled(false);
            } else {
                setMmsExpirySummary(PhoneConstants.SUB1);
            }
            if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                mMmsExpiryCard2Pref.setEnabled(false);
            } else {
                setMmsExpirySummary(PhoneConstants.SUB2);
            }
        } else {
            mmsSettings.removePreference(mMmsExpiryCard1Pref);
            mmsSettings.removePreference(mMmsExpiryCard2Pref);
            setMmsExpirySummary(MessageUtils.SUB_INVALID);
        }
    }

    private void setMmsExpirySummary(int subscription) {
        switch (subscription) {
            case MessageUtils.SUB_INVALID:
                mMmsExpiryPref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String value = newValue.toString();
                        int index = mMmsExpiryPref.findIndexOfValue(value);
                        mMmsExpiryPref.setValue(value);
                        mMmsExpiryPref.setSummary(mMmsExpiryPref.getEntries()[index]);
                        return false;
                    }
                });
                mMmsExpiryPref.setSummary(mMmsExpiryPref.getEntry());
                break;
            case PhoneConstants.SUB1:
                mMmsExpiryCard1Pref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String value = newValue.toString();
                        int index = mMmsExpiryCard1Pref.findIndexOfValue(value);
                        mMmsExpiryCard1Pref.setValue(value);
                        mMmsExpiryCard1Pref.setSummary(mMmsExpiryCard1Pref.getEntries()[index]);
                        return false;
                    }
                });
                mMmsExpiryCard1Pref.setSummary(mMmsExpiryCard1Pref.getEntry());
                break;
            case PhoneConstants.SUB2:
                mMmsExpiryCard2Pref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String value = newValue.toString();
                        int index = mMmsExpiryCard2Pref.findIndexOfValue(value);
                        mMmsExpiryCard2Pref.setValue(value);
                        mMmsExpiryCard2Pref.setSummary(mMmsExpiryCard2Pref.getEntries()[index]);
                        return false;
                    }
                });
                mMmsExpiryCard2Pref.setSummary(mMmsExpiryCard2Pref.getEntry());
                break;
            default:
                break;
        }
    }

    private void updateSignatureStatus() {
        // If the signature CheckBox is checked, we should set the signature EditText
        // enable, and disable when it's not checked.
        boolean isChecked = mSmsSignaturePref.isChecked();
        mSmsSignatureEditPref.setEnabled(isChecked);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        if (mIsSmsEnabled) {
            menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESTORE_DEFAULTS:
                restoreDefaultPreferences();
                return true;

            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mSmsLimitPref) {
            new NumberPickerDialog(this,
                    AlertDialog.THEME_MATERIAL_DARK,
                    mSmsLimitListener,
                    mSmsRecycler.getMessageLimit(this),
                    mSmsRecycler.getMessageMinLimit(),
                    mSmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_sms_delete).show();
        } else if (preference == mMmsLimitPref) {
            new NumberPickerDialog(this,
                    AlertDialog.THEME_MATERIAL_DARK,
                    mMmsLimitListener,
                    mMmsRecycler.getMessageLimit(this),
                    mMmsRecycler.getMessageMinLimit(),
                    mMmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_mms_delete).show();
        } else if (preference == mSmsTemplate) {
            startActivity(new Intent(this, MessageTemplate.class));
        } else if (preference == mManageSimPref) {
            startActivity(new Intent(this, ManageSimMessages.class));
        } else if (preference == mManageSim1Pref) {
            Intent intent = new Intent(this, ManageSimMessages.class);
            intent.putExtra(MessageUtils.SUBSCRIPTION_KEY, MessageUtils.SUB1);
            startActivity(intent);
        } else if (preference == mManageSim2Pref) {
            Intent intent = new Intent(this, ManageSimMessages.class);
            intent.putExtra(MessageUtils.SUBSCRIPTION_KEY, MessageUtils.SUB2);
            startActivity(intent);
        } else if (preference == mClearHistoryPref) {
            showDialog(CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG);
            return true;
        } else if (preference == mEnableNotificationsPref) {
            // Update the actual "enable notifications" value that is stored in secure settings.
            enableNotifications(mEnableNotificationsPref.isChecked(), this);
        } else if (preference == mSmsSignaturePref) {
            updateSignatureStatus();
        } else if (preference == mMmsAutoRetrievialPref) {
            if (mMmsAutoRetrievialPref.isChecked()) {
                startMmsDownload();
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trigger the TransactionService to download any outstanding messages.
     */
    private void startMmsDownload() {
        startService(new Intent(TransactionService.ACTION_ENABLE_AUTO_RETRIEVE, null, this,
                TransactionService.class));
    }

    NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                mSmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setSmsDisplayLimit();
            }
    };

    NumberPickerDialog.OnNumberSetListener mMmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                mMmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setMmsDisplayLimit();
            }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG:
                return new AlertDialog.Builder(MessagingPreferenceActivity.this)
                    .setTitle(R.string.confirm_clear_search_title)
                    .setMessage(R.string.confirm_clear_search_text)
                    .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SearchRecentSuggestions recent =
                                ((MmsApp)getApplication()).getRecentSuggestions();
                            if (recent != null) {
                                recent.clearHistory();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .create();
        }
        return super.onCreateDialog(id);
    }

    public static boolean getNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true);
        return notificationsEnabled;
    }

    public static void enableNotifications(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, enabled);

        editor.apply();
    }

    private void registerListeners() {
        mRingtonePref.setOnPreferenceChangeListener(this);
        final IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        if (preference == mRingtonePref) {
            setRingtoneSummary((String)newValue);
            result = true;
        }
        return result;
    }


    private void showToast(int id) {
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
    }

    /**
     * Set the SMSC preference enable or disable.
     *
     * @param id  the subscription of the slot, if the value is ALL_SUB, update all the SMSC
     *            preference
     * @param airplaneModeIsOn  the state of the airplane mode
     */
    private void setSMSCPrefState(int id, boolean prefEnabled) {
        // We need update the preference summary.
        if (prefEnabled) {
            Log.d(TAG, "get SMSC from sub= " + id);
            final Message callback = mHandler.obtainMessage(EVENT_GET_SMSC_DONE);
            Bundle userParams = new Bundle();
            userParams.putInt(PhoneConstants.SLOT_KEY, id);
            callback.obj = userParams;
            MessageUtils.getSmscFromSub(this, id, callback);
        } else {
            mSmscPrefList.get(id).setSummary(null);
        }
        mSmscPrefList.get(id).setEnabled(prefEnabled);
    }

    private void updateSMSCPref() {
        if (mSmscPrefList == null || mSmscPrefList.size() == 0) {
            return;
        }
        int count = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < count; i++) {
            setSMSCPrefState(i, !isAirPlaneModeOn() &&
                    (TelephonyManager.getDefault().isMultiSimEnabled()
                    ? MessageUtils.isIccCardActivated(i)
                    : TelephonyManager.getDefault().hasIccCard()));
        }
    }

    private void updateSmscFromBundle(Bundle bundle) {
        if (bundle != null) {
            int sub = bundle.getInt(PhoneConstants.SLOT_KEY, -1);
            if (sub != -1) {
                String summary = bundle.getString(MessageUtils.EXTRA_SMSC, null);
                if (summary == null) {
                    return;
                }
                Log.d(TAG, "Update SMSC: sub= " + sub + " SMSC= " + summary);
                int end = summary.lastIndexOf("\"");
                mSmscPrefList.get(sub).setSummary(summary.substring(1, end));
            }
        }
    }

    private static final class SmscHandler extends Handler {
        MessagingPreferenceActivity mOwner;
        public SmscHandler(MessagingPreferenceActivity owner) {
            super(Looper.getMainLooper());
            mOwner = owner;
        }
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = (Bundle) msg.obj;
            if (bundle == null) {
                return;
            }
            Throwable exception = (Throwable)bundle.getSerializable(EXTRA_EXCEPTION);
            if (exception != null) {
                Log.d(TAG, "Error: " + exception);
                mOwner.showToast(R.string.set_smsc_error);
                return;
            }

            Bundle userParams = (Bundle)bundle.getParcelable("userobj");
            if (userParams == null) {
                Log.d(TAG, "userParams = null");
                return;
            }
            switch (msg.what) {
                case EVENT_SET_SMSC_DONE:
                    Log.d(TAG, "Set SMSC successfully");
                    mOwner.showToast(R.string.set_smsc_success);
                    mOwner.updateSmscFromBundle(userParams);
                    break;
                case EVENT_GET_SMSC_DONE:
                    Log.d(TAG, "Get SMSC successfully");
                    int sub = userParams.getInt(PhoneConstants.SLOT_KEY, -1);
                    if (sub != -1) {
                        bundle.putInt(PhoneConstants.SLOT_KEY, sub);
                        mOwner.updateSmscFromBundle(bundle);
                    }
                    break;
            }
        }
    }

    public static class MyEditDialogFragment extends DialogFragment {
        private MessagingPreferenceActivity mActivity;

        public static MyEditDialogFragment newInstance(MessagingPreferenceActivity activity,
                CharSequence title, CharSequence smsc, int sub) {
            MyEditDialogFragment dialog = new MyEditDialogFragment();
            dialog.mActivity = activity;

            Bundle args = new Bundle();
            args.putCharSequence(SMSC_DIALOG_TITLE, title);
            args.putCharSequence(SMSC_DIALOG_NUMBER, smsc);
            args.putInt(SMSC_DIALOG_SUB, sub);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int sub = getArguments().getInt(SMSC_DIALOG_SUB);
            if (null == mActivity) {
                mActivity = (MessagingPreferenceActivity) getActivity();
                dismiss();
            }
            final EditText edit = new EditText(mActivity);
            edit.setPadding(15, 15, 15, 15);
            edit.setText(getArguments().getCharSequence(SMSC_DIALOG_NUMBER));

            Dialog alert = new AlertDialog.Builder(mActivity)
                    .setTitle(getArguments().getCharSequence(SMSC_DIALOG_TITLE))
                    .setView(edit)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            MyAlertDialogFragment newFragment = MyAlertDialogFragment.newInstance(
                                    mActivity, sub, edit.getText().toString());
                            newFragment.show(getFragmentManager(), "dialog");
                            dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(true)
                    .create();
            alert.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            return alert;
        }
    }

    /**
     * All subclasses of Fragment must include a public empty constructor. The
     * framework will often re-instantiate a fragment class when needed, in
     * particular during state restore, and needs to be able to find this
     * constructor to instantiate it. If the empty constructor is not available,
     * a runtime exception will occur in some cases during state restore.
     */
    public static class MyAlertDialogFragment extends DialogFragment {
        private MessagingPreferenceActivity mActivity;

        public static MyAlertDialogFragment newInstance(MessagingPreferenceActivity activity,
                                                        int sub, String smsc) {
            MyAlertDialogFragment dialog = new MyAlertDialogFragment();
            dialog.mActivity = activity;

            Bundle args = new Bundle();
            args.putInt(SMSC_DIALOG_SUB, sub);
            args.putString(SMSC_DIALOG_NUMBER, smsc);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int sub = getArguments().getInt(SMSC_DIALOG_SUB);
            final String displayedSMSC = getArguments().getString(SMSC_DIALOG_NUMBER);

            // When framework re-instantiate this fragment by public empty
            // constructor and call onCreateDialog(Bundle savedInstanceState) ,
            // we should make sure mActivity not null.
            if (null == mActivity) {
                mActivity = (MessagingPreferenceActivity) getActivity();
            }

            final String actualSMSC = mActivity.adjustSMSC(displayedSMSC);

            return new AlertDialog.Builder(mActivity)
                    .setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                            R.string.set_smsc_confirm_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                           Log.d(TAG, "set SMSC from sub= " +sub + " SMSC= " + displayedSMSC);
                           final Message callback = mHandler.obtainMessage(EVENT_SET_SMSC_DONE);
                           Bundle userParams = new Bundle();
                           userParams.putInt(PhoneConstants.SLOT_KEY, sub);
                           userParams.putString(MessageUtils.EXTRA_SMSC,actualSMSC);
                           callback.obj = userParams;
                           MessageUtils.setSmscForSub(mActivity, sub, actualSMSC, callback);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(true)
                    .create();
        }
    }

    private String adjustSMSC(String smsc) {
        String actualSMSC = "\"" + smsc + "\"";
        return actualSMSC;
    }

    // For the group mms feature to be enabled, the following must be true:
    //  1. the feature is enabled in mms_config.xml (currently on by default)
    //  2. the feature is enabled in the mms settings page
    //  3. the SIM knows its own phone number
    public static boolean getIsGroupMmsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean groupMmsPrefOn = prefs.getBoolean(
                MessagingPreferenceActivity.GROUP_MMS_MODE, true);
        return MmsConfig.getGroupMmsEnabled() &&
                groupMmsPrefOn &&
                !TextUtils.isEmpty(MessageUtils.getLocalNumber());
    }
}
