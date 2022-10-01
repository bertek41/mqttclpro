package in.dc297.mqttclpro.activity;


import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.preferences.MyBrokerPreferences;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
public class GeneralPreferenceFragment extends PreferenceFragment {

    private static final int PICK_CACRT_FILE_REQUEST_CODE=200;
    private static final int PICK_CLIENTCRT_FILE_REQUEST_CODE=201;
    private static final int PICK_ClIENTKEY_FILE_REQUEST_CODE=202;
    private static final int PICK_CLIENTP12CRT_FILE_REQUEST_CODE=203;

    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public GeneralPreferenceFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general_broker);
        setHasOptionsMenu(true);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("Host"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("Port"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("Username"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("ClientId"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("CACrt"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("ClientCrt"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("ClientKey"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("ClientP12Crt"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("LastWillTopic"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("LastWillMessage"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("LastWillQOS"));
        AddEditBrokersActivity.bindPreferenceSummaryToValue(findPreference("NickName"));

        Preference filePicker = findPreference("CACrt");
        filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_CACRT_FILE_REQUEST_CODE);
                return true;
            }
        });

        filePicker = findPreference("ClientCrt");
        filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_CLIENTCRT_FILE_REQUEST_CODE);
                return true;
            }
        });

        filePicker = findPreference("ClientKey");
        filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_ClIENTKEY_FILE_REQUEST_CODE);
                return true;
            }
        });

        filePicker = findPreference("ClientP12Crt");
        filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_CLIENTP12CRT_FILE_REQUEST_CODE);
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // get the new value from Intent data
        String fileName;
        Uri uri=Uri.parse("");
        if (data==null)
            // In case the back button has been pressed, the file is removed
            fileName = "";
        else {
            String tmp = data.getDataString();
            uri = Uri.parse(tmp);
            fileName = getFileName(getActivity(), uri);
        }

        // Request read permission if not already granted
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }

        // Get the previous file name
        String key="CACrt";
        if (requestCode==PICK_CLIENTCRT_FILE_REQUEST_CODE)
            key="ClientCrt";
        else if (requestCode==PICK_ClIENTKEY_FILE_REQUEST_CODE)
            key="ClientKey";
        else if (requestCode==PICK_CLIENTP12CRT_FILE_REQUEST_CODE)
            key="ClientP12Crt";
        Preference preference=findPreference(key);
        String prevFileName = (String) preference.getSummary();

        // Check if there is already a file with identical name
        if (fileName.length()!=0 && !fileName.equals(prevFileName)) {
            File directory = getActivity().getFilesDir();
            File[] files = directory.listFiles();
            Log.v("Files", "Size: " + files.length);
            for (int i = 0; i < files.length; i++) {
                if (fileName.equals(files[i].getName()))
                {
                    Toast.makeText(getActivity(),"A file with similar name has been already imported. Change the file name before importing.",Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        // Delete the previous file
        if (prevFileName.length()!=0) {
            File fdelete = new File(getActivity().getFilesDir(), prevFileName);
            if (fdelete.exists())
                fdelete.delete();
        }

        // Copy the new file into the home directory of the app
        if (fileName.length()!=0) {
            try {
                InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                File file = new File(getActivity().getFilesDir(), fileName);
                FileOutputStream outputStream = new FileOutputStream(file);
                copy(inputStream, outputStream);
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                fileName="";
            }
        }

        // Update the value displayed in the widget
        preference.setSummary(fileName);

        // Modify the value in the database
        MyBrokerPreferences.Editor prefs = (MyBrokerPreferences.Editor) getActivity().getSharedPreferences("", MODE_PRIVATE).edit();
        prefs.putString(key,fileName);
        prefs.commit();

        // Show a toast to indicate that the config file has been deleted
        if (fileName.length()==0 && prevFileName.length()!=0) {
            if (requestCode==PICK_CACRT_FILE_REQUEST_CODE)
                Toast.makeText(getActivity(), "The CA Crt file has been remove.", Toast.LENGTH_SHORT).show();
            else if (requestCode==PICK_CLIENTCRT_FILE_REQUEST_CODE)
                Toast.makeText(getActivity(),"The Client Certificate file has been remove.",Toast.LENGTH_SHORT).show();
            else if (requestCode==PICK_ClIENTKEY_FILE_REQUEST_CODE)
                Toast.makeText(getActivity(),"The Client Key file has been remove.",Toast.LENGTH_SHORT).show();
            else if (requestCode==PICK_CLIENTP12CRT_FILE_REQUEST_CODE)
                Toast.makeText(getActivity(),"The Client .p12 file has been remove.",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), AddEditBrokersActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("Range")
    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf(File.separator);
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private static long copy(InputStream input, OutputStream output) throws IOException {
        long count = 0;
        int n;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
