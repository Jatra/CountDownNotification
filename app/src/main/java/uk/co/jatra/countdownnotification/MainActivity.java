package uk.co.jatra.countdownnotification;

import android.content.Intent;
import android.os.Bundle;
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
                Intent serviceIntent = new Intent(MainActivity.this, CountDownService.class);
                serviceIntent.putExtra(CountDownService.RESERVATION_HOLD_TIME_EXTRA, 60 * 15 * 1000L);
                Class<Main2Activity> main2ActivityClass = Main2Activity.class;
                serviceIntent.putExtra(CountDownService.OPEN_ACTIVITY_CLASSNAME_EXTRA, Main2Activity.class);
                startService(serviceIntent);
                finish();
            }
        });
    }
}
