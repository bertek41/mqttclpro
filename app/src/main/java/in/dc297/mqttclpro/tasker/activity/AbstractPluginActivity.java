/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package in.dc297.mqttclpro.tasker.activity;

import android.annotation.TargetApi;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.tasker.BreadCrumber;
import in.dc297.mqttclpro.tasker.Constants;

/**
 * Superclass for plug-in Activities. This class takes care of initializing aspects of the plug-in's UI to
 * look more integrated with the plug-in host.
 */
public abstract class AbstractPluginActivity extends AppCompatActivity
{

    boolean mIsCancelled = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            setupTitleApi11();
        }
        else
        {
            setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(),
                    getString(R.string.title_activity_action_edit)));
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupTitleApi11()
    {
        setTitle(getString(R.string.title_activity_action_edit));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.twofortyfouram_locale_help_save_dontsave, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            setupActionBarApi11();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            setupActionBarApi14();
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBarApi11()
    {
        getSupportActionBar().setSubtitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(),
                getString(R.string.plugin_name)));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setupActionBarApi14()
    {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /*
         * Note: There is a small TOCTOU error here, in that the host could be uninstalled right after
         * launching the plug-in. That would cause getApplicationIcon() to return the default application
         * icon. It won't fail, but it will return an incorrect icon.
         *
         * In practice, the chances that the host will be uninstalled while the plug-in UI is running are very
         * slim.
         */
        try
        {
            getSupportActionBar().setIcon(getPackageManager().getApplicationIcon(getCallingPackage()));
        }
        catch (final NameNotFoundException e)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "An error occurred loading the host's icon", e); //$NON-NLS-1$
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        final int id = item.getItemId();

        if (android.R.id.home == id)
        {
            finish();
            return true;
        }
        else if (R.id.twofortyfouram_locale_menu_dontsave == id)
        {
            mIsCancelled = true;
            finish();
            return true;
        }
        else if (R.id.twofortyfouram_locale_menu_save == id)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * During {@link #finish()}, subclasses can call this method to determine whether the Activity was
     * canceled.
     *
     * @return True if the Activity was canceled. False if the Activity was not canceled.
     */
    protected boolean isCanceled()
    {
        return mIsCancelled;
    }
}