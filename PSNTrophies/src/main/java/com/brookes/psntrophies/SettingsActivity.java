package com.brookes.psntrophies;

import java.io.File;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.util.Log;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;


public class SettingsActivity extends PreferenceActivity{
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;
    private static Context context;
    private static File folder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupSimplePreferencesScreen();
        context = getApplicationContext();
        folder = new File(getExternalFilesDir(null), "/");
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	@SuppressWarnings("deprecation")
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// Add blank list so we can display General header
		addPreferencesFromResource(R.xml.pref_blank);
		
		// Add 'general' preferences and header.
		PreferenceCategory fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_general);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_general);

        Preference button = (Preference)findPreference("delete_button");
        if (button != null) {
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { //When user clicks button to delete images
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    boolean deletedImages = deleteImages(); //Try to delete images from SD Card
                    //Create a successful and error message
                    Toast errorMsg = Toast.makeText(context, "Unable to access SD Card", Toast.LENGTH_SHORT);
                    Toast successMsg = Toast.makeText(context, "Images have been deleted", Toast.LENGTH_SHORT);
                    if(deletedImages){ //If images successfully deleted
                        successMsg.show(); //Show a success message
                    }
                    else{
                        errorMsg.show(); //Show an error message
                    }
                    return true;
                }
            });
        }

        //Create account manager and list of accounts
        final AccountManager mAccountManager = AccountManager.get(getBaseContext());
        Account[] accounts = mAccountManager.getAccounts();

        final EditTextPreference emailPreference = (EditTextPreference)findPreference("email");
        if (emailPreference != null) {
            Account account = null; //Account to use
            for(int i=0; i<accounts.length;i++){ //Iterate through accounts
                Account tempAccount = accounts[i]; //Create a temporary account variable
                if(tempAccount.type.equals(AccountGeneral.ACCOUNT_TYPE)){ //If it is a PSN Account
                    account = tempAccount;
                }
            }

            emailPreference.setPersistent(false); //Don't save email in shared preference
            emailPreference.setSummary(account.name);
            emailPreference.setText(account.name);

            //Create listener for change in email
            final Account finalAccount = account;
            Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    //When preference is changed
                    String newEmail = (String)newValue; //Cast object to string
                    //Retrieve old username & password
                    String password = mAccountManager.getPassword(finalAccount);
                    String username = mAccountManager.peekAuthToken(finalAccount, "");

                    mAccountManager.removeAccount(finalAccount, null, null); //Delete old account
                    Account newAccount = new Account(newEmail, AccountGeneral.ACCOUNT_TYPE); //Create new account
                    mAccountManager.addAccountExplicitly(newAccount, password, new Bundle()); //Add account
                    mAccountManager.setAuthToken(newAccount, "", username); //Set username as authtoken

                    //Make account sync automatically
                    getContentResolver().setSyncAutomatically(newAccount, "com.brookes.psntrophies.provider", true);

                    //Update text and summary
                    emailPreference.setText(newEmail);
                    emailPreference.setSummary(newEmail);
                    return false;
                }
            };
            emailPreference.setOnPreferenceChangeListener(listener); //Apply listener
        }

        final EditTextPreference passwordPreference = (EditTextPreference)findPreference("password");
        if(passwordPreference != null){
            Account account = null; //Account to use
            for(int i=0; i<accounts.length;i++){ //Iterate through accounts
                Account tempAccount = accounts[i]; //Create a temporary account variable
                if(tempAccount.type.equals(AccountGeneral.ACCOUNT_TYPE)){ //If it is a PSN Account
                    account = tempAccount;
                }
            }
            passwordPreference.setPersistent(false); //Make sure password is not saved in shared preference

            String password = mAccountManager.getPassword(account); //Retrieve password

            //Create a masked password same length as proper password
            int passwordLength = password.length();
            String maskedPassword = "";
            for(int j=0; j<passwordLength; j++){
                maskedPassword += "*";
            }

            passwordPreference.setSummary(maskedPassword); //Set masked password as summary
            passwordPreference.setText(password); //Set actual password as text to be changed

            //Create listener for change in password
            final Account finalAccount = account;
            Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    //When preference is changed
                    String newPassword = (String)newValue; //Cast object to string
                    mAccountManager.setPassword(finalAccount, newPassword); //Set password in account manager
                    passwordPreference.setText(newPassword);

                    //Create masked password
                    int passwordLength = newPassword.length();
                    String maskedPassword = "";
                    for(int j=0; j<passwordLength; j++){
                        maskedPassword += "*";
                    }

                    passwordPreference.setSummary(maskedPassword); //Set masked password as summary
                    return false;
                }
            };
            passwordPreference.setOnPreferenceChangeListener(listener); //Apply listener
        }



        // Add 'games' preferences, and a corresponding header.
		fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_games);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_games);
				
		// Add 'trophies' preferences, and a corresponding header.
		fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_trophies);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_trophies);

		// Add 'notifications' preferences, and a corresponding header.
		fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_notifications);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_notification);

		// Add 'data and sync' preferences, and a corresponding header.
		fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_data_sync);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_data_sync);

		// Bind the summaries of EditText/List/Dialog/Ringtone preferences to
		// their values. When their values change, their summaries are updated
		// to reflect the new value, per the Android Design guidelines.
		bindPreferenceSummaryToValue(findPreference("filter_games"));
		bindPreferenceSummaryToValue(findPreference("sort_games"));
        bindPreferenceSummaryToValue(findPreference("delete_frequency"));
        bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
		bindPreferenceSummaryToValue(findPreference("sync_frequency"));
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isLargeTablet(context);
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference
						.setSummary(index >= 0 ? listPreference.getEntries()[index]
								: null);

			} else if (preference instanceof RingtonePreference) {
				// For ringtone preferences, look up the correct display value
				// using RingtoneManager.
				if (TextUtils.isEmpty(stringValue)) {
					// Empty values correspond to 'silent' (no ringtone).
					preference.setSummary(R.string.pref_ringtone_silent);

				} else {
					Ringtone ringtone = RingtoneManager.getRingtone(
							preference.getContext(), Uri.parse(stringValue));

					if (ringtone == null) {
						// Clear the summary if there was a lookup error.
						preference.setSummary(null);
					} else {
						// Set the summary to reflect the new ringtone display
						// name.
						String name = ringtone
								.getTitle(preference.getContext());
						preference.setSummary(name);
					}
				}

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference
				.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString(preference.getKey(),
						""));
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
            bindPreferenceSummaryToValue(findPreference("delete_frequency"));
            Preference button = (Preference)findPreference("delete_button");
            if (button != null) {
                button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { //When user clicks button to delete images
                    @Override
                    public boolean onPreferenceClick(Preference arg0) {
                        boolean deletedImages = deleteImages(); //Try to delete images from SD Card
                        //Create a successful and error message
                        Toast errorMsg = Toast.makeText(context, "Unable to access SD Card", Toast.LENGTH_SHORT);
                        Toast successMsg = Toast.makeText(context, "Images have been deleted", Toast.LENGTH_SHORT);
                        if(deletedImages){ //If images successfully deleted
                            successMsg.show(); //Show a success message
                        }
                        else{
                            errorMsg.show(); //Show an error message
                        }
                        return true;
                    }
                });
            }

            Preference usernamePreference = (Preference)findPreference("username");

            //Create account manager and list of accounts
            final AccountManager mAccountManager = AccountManager.get(context);
            Account[] accounts = mAccountManager.getAccounts();

            final EditTextPreference emailPreference = (EditTextPreference)findPreference("email");
            if (emailPreference != null) {
                Account account = null; //Account to use
                for(int i=0; i<accounts.length;i++){ //Iterate through accounts
                    Account tempAccount = accounts[i]; //Create a temporary account variable
                    if(tempAccount.type.equals(AccountGeneral.ACCOUNT_TYPE)){ //If it is a PSN Account
                        account = tempAccount;
                    }
                }

                emailPreference.setPersistent(false); //Don't save email in shared preference
                emailPreference.setSummary(account.name);
                emailPreference.setText(account.name);

                //Create listener for change in email
                final Account finalAccount = account;
                Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        //When preference is changed
                        String newEmail = (String)newValue; //Cast object to string
                        //Retrieve old username & password
                        String password = mAccountManager.getPassword(finalAccount);
                        String username = mAccountManager.peekAuthToken(finalAccount, "");

                        mAccountManager.removeAccount(finalAccount, null, null); //Delete old account
                        Account newAccount = new Account(newEmail, AccountGeneral.ACCOUNT_TYPE); //Create new account
                        mAccountManager.addAccountExplicitly(newAccount, password, new Bundle()); //Add account
                        mAccountManager.setAuthToken(newAccount, "", username); //Set username as authtoken

                        //Make account sync automatically
                        ContentResolver.setSyncAutomatically(newAccount, "com.brookes.psntrophies.provider", true);

                        //Update text and summary
                        emailPreference.setText(newEmail);
                        emailPreference.setSummary(newEmail);
                        return false;
                    }
                };
                emailPreference.setOnPreferenceChangeListener(listener); //Apply listener
            }

            final EditTextPreference passwordPreference = (EditTextPreference)findPreference("password");
            if(passwordPreference != null){
                Account account = null; //Account to use
                for(int i=0; i<accounts.length;i++){ //Iterate through accounts
                    Account tempAccount = accounts[i]; //Create a temporary account variable
                    if(tempAccount.type.equals(AccountGeneral.ACCOUNT_TYPE)){ //If it is a PSN Account
                        account = tempAccount;
                    }
                }
                passwordPreference.setPersistent(false); //Make sure password is not saved in shared preference

                String password = mAccountManager.getPassword(account); //Retrieve password

                //Create a masked password same length as proper password
                int passwordLength = password.length();
                String maskedPassword = "";
                for(int j=0; j<passwordLength; j++){
                    maskedPassword += "*";
                }

                passwordPreference.setSummary(maskedPassword); //Set masked password as summary
                passwordPreference.setText(password); //Set actual password as text to be changed

                //Create listener for change in password
                final Account finalAccount = account;
                Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        //When preference is changed
                        String newPassword = (String)newValue; //Cast object to string
                        mAccountManager.setPassword(finalAccount, newPassword); //Set password in account manager
                        passwordPreference.setText(newPassword);

                        //Create masked password
                        int passwordLength = newPassword.length();
                        String maskedPassword = "";
                        for(int j=0; j<passwordLength; j++){
                            maskedPassword += "*";
                        }

                        passwordPreference.setSummary(maskedPassword); //Set masked password as summary
                        return false;
                    }
                };
                passwordPreference.setOnPreferenceChangeListener(listener); //Apply listener
            }
		}
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GamesPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_games);
			bindPreferenceSummaryToValue(findPreference("filter_games"));
			bindPreferenceSummaryToValue(findPreference("sort_games"));
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class TrophiesPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_trophies);
		}
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class NotificationPreferenceFragment extends
			PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_notification);

			bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
		}
	}

	/**
	 * This fragment shows data and sync preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class DataSyncPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_data_sync);

			bindPreferenceSummaryToValue(findPreference("sync_frequency"));
		}
	}
	
	private static boolean deleteImages(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String storageState = Environment.getExternalStorageState();
        //Checks if external storage is mounted and what access rights the app has
        if (Environment.MEDIA_MOUNTED.equals(storageState)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        boolean error = false; //Flag is changed if an error occurs

        if(mExternalStorageAvailable && mExternalStorageWriteable){ //If can read and write to SD Card
            boolean success = new DeleteImages(context).deleteImages(folder.getPath());
            if(success){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            return false; //Report failure
        }
    }
	@Override
    public boolean onOptionsItemSelected(MenuItem item){
         
        switch (item.getItemId()){
        	case android.R.id.home:
        		finish();
        		return true;
        	default:
        		return true;       	
        }
	}
}
