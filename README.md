# StateLayout
 StateLayout supports different states to match different layouts for Android.

## Add dependencies

**Project build.gradle**

```xml
repositories {
    maven {
        url 'https://dl.bintray.com/bsx/maven'
    }
}
```

**Module build.gradle**

```xml
compile 'gdut.bsx:stateLayout:1.0.1'
```

## Sample
[Sample App](https://github.com/baishixian/StateLayout/blob/master/Sample.apk)

## How To Use

**Add layout**

```
<?xml version="1.0" encoding="utf-8"?>
<merge
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context="gdut.bsx.view.DemoActivity">

    <gdut.bsx.view.StateLayout
            android:id="@+id/state_layout"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:enableAnim="true"
            app:emptyContentLayoutResId="@layout/layout_empty"
            app:errorLayoutResId="@layout/layout_error"
            app:loadingLayoutResId="@layout/layout_loading">

        <include layout="@layout/layout_content"/>

    </gdut.bsx.view.StateLayout>

</merge>
```
**Use StateLayout**

```
    private static final int STATE_STEP_1 = 1;
    private static final int STATE_STEP_2 = 2;

    StateLayout stateLayout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        stateLayout = findViewById(R.id.state_layout);
        findViewById(R.id.bt_start_step).setOnClickListener(this);
        findViewById(R.id.bt_refresh_content).setOnClickListener(this);

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

        // add custom item layout
        stateLayout.addCustomItemLayout(STATE_STEP_1, R.layout.layout_custom_step_1);
        stateLayout.addCustomItemLayout(STATE_STEP_2, R.layout.layout_custom_step_2);

        // show content
        stateLayout.showContent();
  }
  
   private void loadData(){
        Log.d(TAG, "loadData");
        stateLayout.showLoading();
        findViewById(R.id.bt_load_error).setOnClickListener(this);
        findViewById(R.id.bt_load_stop).setOnClickListener(this);
        findViewById(R.id.bt_load_complete).setOnClickListener(this);
    }

    private void showStep1Layout() {
        Log.d(TAG, "showStep1Layout");
        stateLayout.changeState(STATE_STEP_1);
        findViewById(R.id.bt_step_1_next).setOnClickListener(this);
    }
  
```

