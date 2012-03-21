package info.dourok.dict.provider;

import info.dourok.dict.DictCommunication;
import info.dourok.dict.R;
import info.dourok.dict.TeaSharedPreferences;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ViewSwitcher;

public abstract class Provider implements DictCommunication {
	protected String mName;
	protected int mType;
	private static int GLOBAL_PROVIDER_COUNT = 1;
	final int mMsgSpace;
	protected Context mContext;

	private Messenger mService;

	// 每个provider的消息范围不能超过这个限制
	public final static int MSG_MIN_VALUE = MSG_KILL_YOURSELF + 1;
	public final static int MSG_MAN_VALUE = 0xFFFF;

	// protected UI mUI;
	public Provider(Context context) {
		this.mContext = context;
		mMsgSpace = nextMsgSpace();
	}

	public final String getName() {
		return mName;
	}

	public final int getType() {
		return mType;
	}

	public SharedPreferences getSharedPreferences() {
		return new TeaSharedPreferences(mContext,
				mContext.getSharedPreferences(mName, Context.MODE_PRIVATE));
	}

	/**
	 * 当载体与服务连接后会调用这个方法,表示可以向服务发送消息.
	 * 
	 * @param service
	 */
	public void onServiceConnected(Messenger service) {
		this.mService = service;
	}

	/**
	 * 除了Provider Manager外其他子类不应该重写这个方法. 对消息的处理在
	 * {@link #onHandleMessage(Message,int)} 中实现.
	 * 
	 * @param msg
	 */
	public void dispatchMessage(Message msg) {
		switch (msg.what) {
		case MSG_QUERY_FINISHED:
			update();
			break;
		default:
			onHandleMessage(msg, msg.what & MSG_MAN_VALUE);
			break;
		}

	}

	/**
	 * 发送一个任务都后台线程
	 * 
	 * @param msg
	 */
	protected final void sendMessage(Message msg) {
		msg.what |= (mMsgSpace << 16);
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.w("Provider", mName + ":Send message failed", e);
		}
	}

	public final void query(CharSequence chars) {
		onQuery(chars);
		Message msg = Message.obtain(null, MSG_QUERY_FINISHED);
		try {
			mMessenger.send(msg);
		} catch (RemoteException e) {
			Log.w("Provider", mName + ":send to UI thread falied", e);
		}
	}

	final void update() {
		onUpdate();
	}

	public void destroy() {
		mContext = null;
		mService = null;
	}

	/**
	 * 更新结果,可安全地更新UI状态
	 */
	protected abstract void onUpdate();

	/**
	 * 执行查询动作
	 * 
	 * @param chars
	 */
	protected abstract void onQuery(CharSequence chars);

	/**
	 * 子provider对消息的处理函数,注意这个函数是在后台线程执行的. 适合执行耗时操作 使用newWhat 代替 msg.what来做判断
	 * 
	 * @param msg
	 * @param newWhat
	 */
	protected abstract void onHandleMessage(Message msg, int newWhat);

	/**
	 * 验证请求是否合法,目前provider本身不调用这个方法
	 * 
	 * @param chars
	 * @return
	 */
	public abstract boolean isValid(CharSequence chars);

	public abstract UI getUI();

	// 确保是在UI线程初始化的
	final Messenger mMessenger = new Messenger(new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_QUERY_FINISHED:
				update();
				break;
			}
		}
	});

	public interface UI {

		public void busy();

		public View getView();
	}

	/**
	 * 通用的UI实现,View是一个ViewSwitcher,当Busy的时候,切换到Progress界面
	 * 
	 * @author DouO
	 * 
	 */
	protected static class CommUIImpl implements UI {
		private Context mContext;
		protected View mContentView;
		ViewSwitcher mViewSwitcher;

		public CommUIImpl(Context context,int contentViewId) {
			this.mContext = context;
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mViewSwitcher = (ViewSwitcher) inflater.inflate(
					R.layout.provider_comm_ui, null);
			mContentView =inflater.inflate(contentViewId, null);
			mViewSwitcher.addView(mContentView);
		}

		/**
		 * 只能在构造函数中调用 当初始化完 mContentView后,必须调用这个方法把它添加到ViewSwitcher中.
		 */
//		protected void inflate() {
//			LayoutInflater inflater = (LayoutInflater) mContext
//					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//			mViewSwitcher = (ViewSwitcher) inflater.inflate(
//					R.layout.provider_comm_ui, null);
//			mViewSwitcher.addView(mContentView);
//		}

		@Override
		public void busy() {
			if (mContentView == mViewSwitcher.getCurrentView()) {
				mViewSwitcher.showNext();
			}
		}

		public void show() {
			if (mContentView != mViewSwitcher.getCurrentView()) {
				mViewSwitcher.showNext();
			}
		}

		@Override
		public View getView() {
			// TODO Auto-generated method stub
			return mViewSwitcher;
		}

	}

	/**
	 * A wrapper class wrap a provider in use of message.obj
	 * 
	 * @author DouO
	 * 
	 */
	public final static class MessageObj {
		public MessageObj() {
			// TODO Auto-generated constructor stub
		}

		public MessageObj(Object obj, Provider provider) {
			this.obj = obj;
			this.provider = provider;
		}

		public Object obj;
		public Provider provider;
	}

	private static int nextMsgSpace() {
		return ++GLOBAL_PROVIDER_COUNT;
	}
}
