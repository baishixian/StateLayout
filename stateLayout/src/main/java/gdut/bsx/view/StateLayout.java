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
     * LOADING_STATE
     */
    public static final int STATE_LOADING = 8602;

    /**
     * EMPTY_CONTENT_STATE
     */
    public static final int STATE_EMPTY_CONTENT = 8603;

    /**
     * ERROR
     */
    public static final int STATE_ERROR = 8604;

    /**
     * 正常显示
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
     * 懒加载子布局，需要时再进行 inflate
     */
    private SparseArray<ViewStub> stateLayoutItems = new SparseArray<>();

    /**
     * 记录已经加载过的子布局
     */
    private SparseArray<View> mCurrentViewItems = new SparseArray<>();

    /**
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

    /**
     * 添加状态布局
     * @param state 状态
     * @param layoutId 布局 id
     */
    private void addItemLayout(int state, @LayoutRes int layoutId){

        ItemView itemView = new ItemView(state, layoutId);
        mSavedItemViews.add(itemView);

        ViewStub itemViewStub = new ViewStub(getContext());
        itemViewStub.setLayoutResource(layoutId);
        addView(itemViewStub);
        stateLayoutItems.append(state, itemViewStub);
    }

    /**
     * 添加自定义状态布局
     * @param state 状态
     * @param layoutId 布局 id
     */
    public void addCustomItemLayout(int state, @LayoutRes int layoutId){
        addItemLayout(state, layoutId);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!mHasInit) {
            // 初始化
            if (mContentView == null) {
                if (getChildCount() > 0) {
                    mContentView = getChildAt(0);
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
    
    /**
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
        ViewStub itemViewStub = stateLayoutItems.get(state);
        if(itemViewStub != null) {
            // 当 ViewStub 被 inflate 后，getParent 返回值是 null
            // 注意 ViewStub 只能被 inflate 一次（setVisibility 有同样效果），完成 inflate 后 ViewStub 会被移除，功成身退
            if (itemViewStub.getParent() != null) {
                itemView = itemViewStub.inflate();

                switch (state) {
                    case STATE_ERROR :
                        initErrorListener(itemView);
                        break;
                    case STATE_EMPTY_CONTENT :
                        initEmptyContentListener(itemView);
                        break;
                    default:break;
                }

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

    public void showEmptyContent(){
        changeState(STATE_EMPTY_CONTENT);
    }

    public void showError(){
        changeState(STATE_ERROR);
    }

    public void showLoading(){
        changeState(STATE_LOADING);
    }

    public void showContent(){

        if (mCurState == STATE_CONTENT) {
            Log.d(TAG, "showContent() miss, current layout already is ContentLayout");
            return;
        }

        hideAllItemViews();

        showItemView(STATE_CONTENT, mContentView);
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
     * 获取视图
     * @param state 状态
     * @return view
     */
    public @Nullable View getCurrentItemView(int state) {
        return mCurrentViewItems.get(state);
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

    /**
     * 隐藏所有子布局
     */
    protected void hideAllItemViews() {
        if (mContentView != null && mContentView.getVisibility() == VISIBLE) {
            mContentView.setVisibility(GONE);
        }

        for (int index = 0; index < mCurrentViewItems.size(); index++) {
            View view = mCurrentViewItems.valueAt(index);
            view.setVisibility(GONE);
        }
    }


    /*********************************** 状态恢复 *************************************/

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

    /**
     * 错误重试监听器
     */
    public interface OnErrorRetryListener {
        void onErrorRetry();
    }

    /**
     * 内容为空重试
     */
    public interface OnEmptyContentRetryListener {
        void onEmptyContentRetry();
    }
}
