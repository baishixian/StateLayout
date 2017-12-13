package gdut.bsx.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * DemoActivity
 * @author baishixian
 */
public class DemoActivity extends AppCompatActivity {

    StateLayout stateLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        stateLayout = findViewById(R.id.statelayout);
        stateLayout.setOnEmptyRetryListener(new StateLayout.OnEmptyContentRetryListener() {
            @Override
            public void onEmptyContentRetry() {
                Toast.makeText(DemoActivity.this, "重试中", Toast.LENGTH_SHORT).show();
                loadData();
            }
        });

        stateLayout.setOnErrorRetryListener(new StateLayout.OnErrorRetryListener() {
            @Override
            public void onErrorRetry() {
                Toast.makeText(DemoActivity.this, "失败重试中", Toast.LENGTH_SHORT).show();
                loadData();
            }
        });

        stateLayout.showEmptyContent();

    }

    private void loadData(){
        stateLayout.showLoading();

        stateLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                stateLayout.showContent();
            }
        }, 3000);
    }
}
