package in.dc297.mqttclpro.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.dc297.mqttclpro.R;
import in.dc297.mqttclpro.databinding.BrokerListItemBinding;
import in.dc297.mqttclpro.entity.BrokerEntity;
import in.dc297.mqttclpro.services.MyMqttService;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;

public class BrokersListActivity extends AppCompatActivity {

    private ReactiveEntityStore<Persistable> data;
    private ExecutorService executor;
    private BrokersListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brokers_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(getResources().getString(R.string.title_activity_brokers_list));
        //start service
        Intent svc = new Intent(this, MyMqttService.class);
        startService(svc);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AddEditBrokersActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, GeneralPreferenceFragment.class.getName());
                intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                startActivity(intent);
            }
        });
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        data = ((MQTTClientApplication) getApplication()).getData();
        executor = Executors.newSingleThreadExecutor();
        adapter = new BrokersListAdapter();
        adapter.setExecutor(executor);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        data.count(BrokerEntity.class).get().single().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
                        if(integer == 0) {
                            Toast.makeText(BrokersListActivity.this, "Please add a broker!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        if(!shouldShowRequestPermissionRationale("112")) {
            getNotificationPermission();
        }
    }

    public void getNotificationPermission() {
        try {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    112);
        } catch(Exception e) {

        }
    }

    @Override
    protected void onResume() {
        adapter.queryAsync();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        adapter.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_brokers_list, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class BrokersListAdapter extends QueryRecyclerAdapter<BrokerEntity, BindingHolder<BrokerListItemBinding>> implements View.OnClickListener, View.OnLongClickListener {

        BrokersListAdapter() {
            super(BrokerEntity.$TYPE);
        }

        @Override
        public void onClick(View v) {
            BrokerListItemBinding binding = (BrokerListItemBinding) v.getTag();
            if(binding != null) {
                Intent intent = new Intent(v.getContext(), SubscribedTopicsActivity.class);
                intent.putExtra(SubscribedTopicsActivity.EXTRA_BROKER_ID, binding.getBroker().getId());
                startActivity(intent);
            }
        }

        @Override
        public Result<BrokerEntity> performQuery() {
            return data.select(BrokerEntity.class).get();
        }

        @Override
        public void onBindViewHolder(BrokerEntity broker, BindingHolder<BrokerListItemBinding> brokerListItemBindingBindingHolder, int i) {
            brokerListItemBindingBindingHolder.binding.setBroker(broker);
        }

        @Override
        public BindingHolder<BrokerListItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            BrokerListItemBinding binding = BrokerListItemBinding.inflate(inflater, parent, false);
            binding.getRoot().setTag(binding);
            binding.getRoot().setOnClickListener(this);
            binding.getRoot().setLongClickable(true);
            binding.getRoot().setOnLongClickListener(this);
            return new BindingHolder<>(binding);
        }

        @Override
        public boolean onLongClick(View v) {
            BrokerListItemBinding binding = (BrokerListItemBinding) v.getTag();
            if(binding != null) {
                Intent intent = new Intent(v.getContext(), AddEditBrokersActivity.class);
                intent.putExtra(AddEditBrokersActivity.EXTRA_BROKER_ID, binding.getBroker().getId());
                //Toast.makeText(v.getContext(),binding.getBroker().toString(),Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
            return true;
        }
    }

}