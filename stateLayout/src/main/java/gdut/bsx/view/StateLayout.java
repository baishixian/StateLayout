package gdut.bsx.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import gdut.bsx.view.stateLayout.R;

/**
 * StateLayout
 *
 * @author baishixian
 * @date 2017/12/12 下午3:55
 */

public class StateLayout extends FrameLayout {

    static final String TAG = "StateLayout";

    /**
     * OnErrorRetryListener, clickable view id is sl_error_retry.
     * 匹配可以点击的 view id 是 sl_error_retry
     */
    public interface OnErrorRetryListener {
        /**
         * Error retry clicked.
         * 错误时点击重试
         */
        void onErrorRetry();
    }

    /**
     * OnEmptyContentRetryListener, clickable view id is sl_empty_content_retry.
     * 匹配可以点击的 view id 是 sl_empty_content_retry
     */
    public interface OnEmptyContentRetryListener {
        /**
         * Empty content retry clicked.
         * 内容为空时点击重试
         */
        void onEmptyContentRetry();
    }


    /**
     * LOADING_STATE
     */
    public static final int STATE_LOADING = 8602;

    /**
     * EMPTY_CONTENT_STATE
     */
    public static final int STATE_EMPTY_CONTENT = 8603;

    /**
     * ERROR_STATE
     */
    public static final int STATE_ERROR = 8604;

    /**
     * CONTENT_STATE
     */
    public static final int STATE_CONTENT = 8605;

    /**
     * Loading layout id
     */
    protected int mLoadingLayoutId = -1;

    /**
     * Empty Content layout id
     */
    protected int mEmptyContentLayoutId = -1;

    /**
     * Error layout id
     */
    protected int mErrorLayoutId = -1;

    /**
     * Lazy inflate item layout, cache ViewStub.
     * 懒加载子布局，需要时再进行 inflate
     */
    private SparseArray<ViewStub> mStateLayoutStubItems = new SparseArray<>();

    /**
     * Cache already loaded layout.
     * 记录已经加载过的子布局
     */
    private SparseArray<View> mCurrentViewItems = new SparseArray<>();

    /**
     *
     * 用于恢复布局内容时使用
      */
    private List<ItemView> mSavedItemViews = new ArrayList<>();

    /**
     * 当前状态
     */
    private int mCurState;

    /**
     * 状态切换时视图渐显动画
     */
    private boolean mEnableContentAnim = true;

    /**
     * 是否已经加载过
     */
    protected boolean mHasInit= false;

    /**
     * 重试按钮监听
     */
    protected OnEmptyContentRetryListener mEmptyContentRetryListener;
    protected OnErrorRetryListener mErrorRetryListener;

    /**
     * 内容视图（置于该自定义布局标签内的子视图）
     */
    protected View mContentView;

    /**
     * 添加懒加载的状态布局，会先缓存布局存根但并不立即初始化，只有切换到特定的状态时才初始化其对应布局
     * 注意：如果使用该方法添加状态布局，需要在调用切换状态方法 changeState(int state) 后才能使用布局内的控件
     * @param state 状态
     * @param layoutId 布局 id
     */
    public void addLazyInflateStateLayout(int state, @LayoutRes int layoutId){
        ItemView itemView = new ItemView(state, layoutId);
        mSavedItemViews.add(itemView);

        ViewStub itemViewStub = new ViewStub(getContext());
        itemViewStub.setLayoutResource(layoutId);
        addView(itemViewStub);
        mStateLayoutStubItems.append(state, itemViewStub);
    }

    /**
     * 添加状态布局并立即初始化
     */
    public void addItemLayout(int state, int layoutId){

        addLazyInflateStateLayout(state, layoutId);
        ViewStub itemViewStub = mStateLayoutStubItems.get(state);

        // 当 ViewStub 被 inflate 后，getParent 返回值是 null
        if (itemViewStub != null && itemViewStub.getParent() != null) {
            View itemLayout = itemViewStub.inflate();
            mCurrentViewItems.append(state, itemLayout);
            itemLayout.setVisibility(GONE);

            switch (state) {
                case STATE_ERROR :
                    initErrorListener(itemLayout);
                    break;
                case STATE_EMPTY_CONTENT :
                    initEmptyContentListener(itemLayout);
                    break;
                default:break;
            }
        } else {
            Log.d(TAG, "addItemLayout state is "  + state + ", this item view already inflate.");
        }
    }

    /**
     * change to empty content state.
     */
    public void showEmptyContent(){
        changeState(STATE_EMPTY_CONTENT);
    }

    /**
     * change to error state.
     */
    public void showError(){
        changeState(STATE_ERROR);
    }

    /**
     * change to empty content state.
     */
    public void showLoading(){
        changeState(STATE_LOADING);
    }

    /**
     * show content
     * 展示 xml 布局文件中定义在该布局标签中的视图内容
     */
    public void showContent(){
        changeState(STATE_CONTENT);
    }

    /**
     * change state
     * 改变状态
     * @param state
     */
    public void changeState(int state){

        if (mCurState == state) {
            Log.d(TAG, "changeState() miss, current state already is " + state);
            return;
        }

        hideAllItemViews();

        // 如果已经子布局加载到当前的视图中
        View itemView = mCurrentViewItems.get(state);
        if (itemView != null) {
            showItemView(state, itemView);
            return;
        }

        // 获取缓存的 ViewStub 对象进行布局懒加载，实现只在需要时才加载布局
        ViewStub itemViewStub = mStateLayoutStubItems.get(state);
        if(itemViewStub != null) {
            // 当 ViewStub 被 inflate 后，getParent 返回值是 null
            // 注意 ViewStub 只能被 inflate 一次（setVisibility 有同样效果），完成 inflate 后 ViewStub 会被移除，功成身退
            if (itemViewStub.getParent() != null) {
                itemView = itemViewStub.inflate();
                mCurrentViewItems.append(state, itemView);
                showItemView(state, itemView);
            }
        } else {
            // 查找恢复数据中是否包含该状态视图
            for (ItemView saveItemView : mSavedItemViews) {
                if (saveItemView != null && state == saveItemView.getState()) {
                    addItemLayout(state, saveItemView.getLayoutId());
                    changeState(state);
                    return;
                }
            }
            Log.e(TAG, "StateLayout change to state " + state + ", but can't match the correct layout.");
        }
    }

    /**
     * 获取当前状态
     */
    public int getCurrentState() {
        return mCurState;
    }

    /**
     * 是否开启布局切换动画
     */
    public void enableAnim(boolean enable) {
        this.mEnableContentAnim = enable;
    }

    /**
     * 设置错误重试监听
     * @param listener 重试监听
     */
    public void setOnErrorRetryListener(OnErrorRetryListener listener) {
        this.mErrorRetryListener = listener;
    }

    /**
     * 设置空内容重试监听
     * @param listener 重试监听
     */
    public void setOnEmptyRetryListener(OnEmptyContentRetryListener listener) {
        this.mEmptyContentRetryListener = listener;
    }


    public StateLayout(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public StateLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StateLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public StateLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (attrs == null) {
            Log.d(TAG, "StateLayout attributeSet is null.");
            return;
        }

        // 获取自定义属性
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.StateLayout);
        if (ta != null) {

            int count = ta.getIndexCount();
            for (int i = 0; i < count; i++) {
                int index = ta.getIndex(i);
                if (index == R.styleable.StateLayout_loadingLayoutResId) {
                    mLoadingLayoutId = ta.getResourceId(index, -1);
                } else if (index == R.styleable.StateLayout_emptyContentLayoutResId) {
                    mEmptyContentLayoutId = ta.getResourceId(index, -1);
                } else if (index == R.styleable.StateLayout_errorLayoutResId) {
                    mErrorLayoutId = ta.getResourceId(index, -1);
                } else if (index == R.styleable.StateLayout_enableAnim) {
                    mEnableContentAnim = ta.getBoolean(index, true);
                }
            }

            ta.recycle();
        }
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!mHasInit) {
            // 初始化
            if (mContentView == null) {
                if (getChildCount() > 0) {
                    mContentView = getChildAt(0);
                    mCurrentViewItems.append(STATE_CONTENT, mContentView);
                }
            }

            if (mLoadingLayoutId != -1) {
                addItemLayout(STATE_LOADING, mLoadingLayoutId);
            }

            if (mEmptyContentLayoutId != -1) {
                addItemLayout(STATE_EMPTY_CONTENT, mEmptyContentLayoutId);
            }

            if (mErrorLayoutId != -1) {
                addItemLayout(STATE_ERROR, mErrorLayoutId);
            }
            mHasInit = true;
        }
    }


    private void initErrorListener(View itemView) {
        if (itemView == null) {
            return;
        }

        View v = itemView.findViewById(R.id.sl_error_retry);
        if (v != null) {
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mErrorRetryListener != null) {
                        mErrorRetryListener.onErrorRetry();
                    }
                }
            });
        }
    }

    private void initEmptyContentListener(View itemView) {
        if (itemView == null) {
            return;
        }

        View v = itemView.findViewById(R.id.sl_empty_content_retry);
        if (v != null) {
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mEmptyContentRetryListener != null) {
                        mEmptyContentRetryListener.onEmptyContentRetry();
                    }
                }
            });
        }
    }

    private void showItemView(int state, View itemView) {
        if (itemView != null) {
            if (mEnableContentAnim) {
                AlphaAnimation animation = new AlphaAnimation(0f, 1.0f);
                animation.setDuration(200);
                itemView.startAnimation(animation);
            }
            itemView.setVisibility(View.VISIBLE);
            mCurState = state;
        } else {
            Log.e(TAG, "StateLayout showItemView view is null.");
        }
    }

    /**
     * 隐藏所有子布局
     */
    protected void hideAllItemViews() {
        for (int index = 0; index < mCurrentViewItems.size(); index++) {
            View view = mCurrentViewItems.valueAt(index);
            if (view != null) {
                view.setVisibility(GONE);
            }
        }
    }

    /******************** 视图状态保存和恢复 *******************/
    @Override
    protected Parcelable onSaveInstanceState() {
        SavedViewState state = new SavedViewState(super.onSaveInstanceState());
        state.itemViewList = mSavedItemViews;
        state.lastState = mCurState;
        state.enableAnim = mEnableContentAnim;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedViewState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedViewState ss = (SavedViewState) state;
        super.onRestoreInstanceState(ss);

        mEnableContentAnim = ss.enableAnim;
        mSavedItemViews = ss.itemViewList;
        mCurState = ss.lastState;
        changeState(ss.lastState);
    }

    static class SavedViewState extends BaseSavedState {
        int lastState;
        boolean enableAnim;
        List<ItemView> itemViewList;

        SavedViewState(Parcelable superState) {
            super(superState);
        }

        private SavedViewState(Parcel source) {
            super(source);

            if(itemViewList == null){
                // avoid NullPointException
                itemViewList = new ArrayList<>();
            }

            source.readTypedList(itemViewList, ItemView.CREATOR);
            lastState = source.readInt();
            enableAnim = source.readByte() == (byte) 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeTypedList(itemViewList);
            out.writeInt(lastState);
            out.writeByte(enableAnim ? (byte) 1 : (byte) 0);
        }

        public static final Parcelable.Creator<SavedViewState> CREATOR = new Creator<SavedViewState>() {
            @Override
            public SavedViewState createFromParcel(Parcel source) {
                return new SavedViewState(source);
            }

            @Override
            public SavedViewState[] newArray(int size) {
                return new SavedViewState[size];
            }
        };

    }

    /**
     * ItemView 用于把子视图的 state 和 layoutId 的匹配关系变成可序列化缓存的对象
     */
    static class ItemView implements Parcelable {
        private final int state;
        private final int layoutId;

        ItemView(int state, @LayoutRes int layoutId) {
            this.state = state;
            this.layoutId = layoutId;
        }

        private int getState() {
            return state;
        }

        private int getLayoutId() {
            return layoutId;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.state);
            dest.writeInt(this.layoutId);
        }

        ItemView(Parcel in) {
            this.state = in.readInt();
            this.layoutId = in.readInt();
        }

        static final Parcelable.Creator<ItemView> CREATOR = new Parcelable.Creator<ItemView>() {
            @Override
            public ItemView createFromParcel(Parcel source) {
                return new ItemView(source);
            }

            @Override
            public ItemView[] newArray(int size) {
                return new ItemView[size];
            }
        };
    }
}
