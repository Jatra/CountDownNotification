package uk.co.jatra.countdownnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchService(60 * 15 * 1000L);
            }
        });
        Button button30 = (Button) findViewById(R.id.button30);
        button30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchService(30 * 1000L);
            }
        });
        Button button140 = (Button) findViewById(R.id.button140);
        button140.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchService(140 * 1000L);
            }
        });

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter(CountDownService.CANCEL_SCAN);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent launch = new Intent(MainActivity.this, Main2Activity.class);
                launch.putExtra(CountDownService.COUNTDOWNSERVICE_RESULT_EXTRA, CountDownService.Result.CANCELLED);
                context.startActivity(launch);
            }
        };
        localBroadcastManager.registerReceiver(receiver, intentFilter);
    }

    private void launchService(long timeToLive) {
        CountDownService.startCountDownService(MainActivity.this, Main2Activity.class, timeToLive);
        finish();
    }
}
