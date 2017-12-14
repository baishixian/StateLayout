package gdut.bsx.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

/**
 * DemoActivity
 * @author baishixian
 */
public class DemoActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int STATE_STEP_1 = 1;
    private static final int STATE_STEP_2 = 2;
    private static final String TAG = "DemoActivity";

    StateLayout stateLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        stateLayout = findViewById(R.id.state_layout);

        // init state layout content item
        findViewById(R.id.bt_start_step).setOnClickListener(this);
        findViewById(R.id.bt_refresh_content).setOnClickListener(this);

        // init loading content custom item view
        findViewById(R.id.bt_load_error).setOnClickListener(this);
        findViewById(R.id.bt_load_stop).setOnClickListener(this);
        findViewById(R.id.bt_load_complete).setOnClickListener(this);

        // init error state and empty content state retry click listener
        initRetryListener();

        // add custom item state layout
        addCustomStateLayout();

        // show content
        stateLayout.showContent();

    }

    private void initRetryListener() {
        stateLayout.setOnEmptyRetryListener(new StateLayout.OnEmptyContentRetryListener() {
            @Override
            public void onEmptyContentRetry() {
                loadData();
            }
        });

        stateLayout.setOnErrorRetryListener(new StateLayout.OnErrorRetryListener() {
            @Override
            public void onErrorRetry() {
                loadData();
            }
        });
    }

    private void addCustomStateLayout() {
        // add stateLayout item immediately
        stateLayout.addItemLayout(STATE_STEP_1, R.layout.layout_custom_step_1);
        findViewById(R.id.bt_step_1_next).setOnClickListener(this);

        // add stateLayout item that will be lazy inflate
        // 注意：由于状态 STATE_STEP_2 是使用 addLazyInflateStateLayout 添加的，其布局加载是在 changeState(STATE_STEP_2) 时进行
        // 调用该方法后还不能立即操作状态 STATE_STEP_2 布局的子控件
        stateLayout.addLazyInflateStateLayout(STATE_STEP_2, R.layout.layout_custom_step_2);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_start_step :
                showStep1Layout();
                break;
            case R.id.bt_refresh_content :
                loadData();
                break;
            case R.id.bt_load_error :
                stateLayout.showError();
                break;
            case R.id.bt_load_stop :
                stateLayout.showEmptyContent();
                break;
            case R.id.bt_load_complete :
                stateLayout.showContent();
                break;
            case R.id.bt_step_1_next :
                showStep2Layout();
                break;
            case R.id.bt_step_2_previous :
                showStep1Layout();
                break;
            case R.id.bt_step_2_complete :
                stateLayout.showContent();
                break;
            default:break;
        }
    }

    private void loadData(){
        Log.d(TAG, "loadData");
        stateLayout.showLoading();
    }

    private void showStep1Layout() {
        Log.d(TAG, "showStep1Layout");
        stateLayout.changeState(STATE_STEP_1);
    }

    private void showStep2Layout() {
        Log.d(TAG, "showStep2Layout");
        stateLayout.changeState(STATE_STEP_2);
        // 由于状态 STATE_STEP_2 是使用 addLazyInflateStateLayout 添加的，调用 changeState 切换到对应状态后才开始加载布局
        // 切换完后，此时才可以操作该布局的子控件
        findViewById(R.id.bt_step_2_previous).setOnClickListener(this);
        findViewById(R.id.bt_step_2_complete).setOnClickListener(this);
    }
}
