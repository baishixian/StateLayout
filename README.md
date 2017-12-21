# StateLayout

StateLayout 支持添加自定义的状态布局。

> 通常为了解决数据页面加载时的四个状态：Loading（加载中）、Error （加载错误）、Empty（无内容）、Content（显示内容）的切换 ，把这种根据业务状态动态切换视图内容的组件称为状态布局，StateLayout 在此基础上加入了自定义业务状态的支持。

StateLayout supports different states to match different layouts for Android.

![Sample](https://github.com/baishixian/StateLayout/blob/master/Sample.gif)

掘金：[支持自定义的状态布局 StateLayout](https://juejin.im/post/5a372a4951882527a13d9575)

## Add dependencies

**Project build.gradle**

```xml
repositories {
    jcenter()
}
```

**Module build.gradle**

```xml
compile 'gdut.bsx:stateLayout:1.1.0'
```

## Sample
[Sample App](https://github.com/baishixian/StateLayout/blob/master/Sample.apk)

## How To Use

**Add layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
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
```

**Use StateLayout**

```java
private static final int STATE_STEP_1 = 1;
private static final int STATE_STEP_2 = 2;
    
StateLayout stateLayout = findViewById(R.id.state_layout);
// show content
stateLayout.showContent();
// show loading
stateLayout.showLoading();

// addItemLayout
stateLayout.addItemLayout(STATE_STEP_1, R.layout.layout_custom_step_1);
stateLayout.changeState(STATE_STEP_1);

// addLazyInflateStateLayout
stateLayout.addLazyInflateStateLayout(STATE_STEP_2, R.layout.layout_custom_step_2);
stateLayout.changeState(STATE_STEP_2);

```

## 说明

**`StateLayout` 继承了 `FrameLayout`，可用于在同个窗口种或局部视图内按照不同业务状态切换不同布局。**

> 一般情况下，建议在使用 xml 文件中定义 `StateLayout` 布局时在该布局标签下添加一个内容视图，作为默认的业务内容视图。

**1. 比如在加载数据完成，需要展示内容视图时，我们可以调用：**

```java
/**
 * show content 从其他状态切换到展示内容状态
 * 展示 xml 布局文件中定义在该布局标签中的视图内容
 */
public void showContent()
```
**2. 内置状态支持**

`StateLayout` 按照一般业务需求，内置了 3 种状态：`Error State, Loading State, Empty Content State`。
通过 `StateLayout` 的自定义属性可以指定上面状态对应的布局样式：

```xml
<gdut.bsx.view.StateLayout
          android:id="@+id/state_layout"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          app:emptyContentLayoutResId="@layout/layout_empty"
          app:errorLayoutResId="@layout/layout_error"
          app:loadingLayoutResId="@layout/layout_loading">
</gdut.bsx.view.StateLayout>
```

> 内置的状态可以根据自身实际业务合理配置，未指定布局的状态不会进行加载，所以不会影响原本视图性能。

```java
/**
 * change to empty content state.
 */
public void showEmptyContent()

/**
 * change to error state.
 */
public void showError()

/**
 * change to empty content state.
 */
public void showLoading()

```
**3. 页面重试加载支持**

为方便在状态异常时引导用户即使重试刷新页面，在 `Error State 和 Empty Content State` 时支持重试点击事件监听，你可以指定布局中的某个控件作为点击触发重试的载体：

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
             android:id="@+id/error_state_layout"
             android:layout_width="match_parent"
             android:layout_height="match_parent">
   
  <!-- 指定布局中的某个控件作为点击触发重试的载体，注意使用 @id/ 的方式应用 StateLayout 提供的 id -->
  <Button
            android:id="@id/sl_error_retry"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Retry"
            android:textAllCaps="false"
            android:layout_gravity="center"
            android:textSize="16sp"/>
 </FrameLayout>
```

> `Error State` 对应关联的 id 为 `sl_error_retry`

> `Empty Content State` 对应关联的 id 为 `sl_empty_content_retry`


```java
// 绑定监听器
stateLayout.setOnEmptyRetryListener(new StateLayout.OnEmptyContentRetryListener() {
         @Override
         public void onEmptyContentRetry() {
             retryLoad();
         }
});
```

```java
/**
 * 设置错误重试监听
 * @param listener 重试监听
 */
public void setOnErrorRetryListener(OnErrorRetryListener listener) 
 
 /**
  * 设置空内容重试监听
  * @param listener 重试监听
  */
public void setOnEmptyRetryListener(OnEmptyContentRetryListener listener)
```

**4. 添加自定义的状态布局**

`StateLayout` 支持状态布局拓展，可以根据自身业务需求添加自定义状态和对应布局，同时 `StateLayout` 也提供了布局懒加载的支撑，方便自由取舍需求和性能要求：

```java
 /**
   * 添加状态布局并立即初始化
   */
public void addItemLayout(int state, int layoutId)
    
 /**
   * 添加懒加载的状态布局，会先缓存布局存根但并不立即初始化，只有切换到特定的状态时才初始化其对应布局，降低性能影响
   * 注意：如果使用该方法添加状态布局，需要在调用切换状态方法 changeState(int state) 后才能使用布局内的控件
   * @param state 状态
   * @param layoutId 布局 id
   */
 public void addLazyInflateStateLayout(int state, @LayoutRes int layoutId);
```

具体使用：

```java
 private static final int STATE_STEP_1 = 1;
 private static final int STATE_STEP_2 = 2;
 
 // add stateLayout item immediately
 // 添加自定义状态布局
 stateLayout.addItemLayout(STATE_STEP_1, R.layout.layout_custom_step_1);
 // 为布局控件添加监听
 findViewById(R.id.bt_step_1_next).setOnClickListener(listener);
 // 切换到自定义布局
 stateLayout.changeState(STATE_STEP_1);
 
 // add stateLayout item that will be lazy inflate
 // 注意：由于状态 STATE_STEP_2 是使用 addLazyInflateStateLayout 添加的，其布局加载是在 changeState(STATE_STEP_2) 时进行
 // 调用该方法后还不能立即操作状态 STATE_STEP_2 布局的子控件
 stateLayout.addLazyInflateStateLayout(STATE_STEP_2, R.layout.layout_custom_step_2);
 // 切换到自定义布局，由于状态 STATE_STEP_2 是使用 addLazyInflateStateLayout 添加的，调用 changeState 切换到对应状态后才开始加载布局
 stateLayout.changeState(STATE_STEP_2);
 // 切换完后，此时才可以操作该布局的子控件
 findViewById(R.id.bt_step_2_previous).setOnClickListener(listener);
```

**5. 是否启用状态布局切换动画**

```xml 
 <!-- xml 布局内定义属性-->
 app:enableAnim="true"
```

```java
public void enableAnim(boolean enable)
```

## 更多的使用示例可参考 [Sample Code](https://github.com/baishixian/StateLayout/tree/master/app)
