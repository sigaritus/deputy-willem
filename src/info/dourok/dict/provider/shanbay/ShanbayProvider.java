package info.dourok.dict.provider.shanbay;

import info.dourok.dict.MiscUtils;
import info.dourok.dict.R;
import info.dourok.dict.provider.Provider;
import info.dourok.dict.provider.shanbay.ShanbayDict.Word;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewAnimator;

public class ShanbayProvider extends Provider {

	private ShanbayDict mShanbayDict;
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "passwrod";
	private static final String KEY_STORE_PASSWORD = "store_password";
	private final static int MSG_ADD_WORD = MSG_MIN_VALUE + 1;
	private final static int MSG_LOADING_AUDIO = MSG_MIN_VALUE + 2;
	private final static int MSG_ADD_WORD_FINISHED = MSG_MIN_VALUE + 3;
	private final static int MSG_LOGIN = MSG_MIN_VALUE + 4;
	private final static int MSG_LOGIN_SUCCESS = MSG_MIN_VALUE + 5;
	private final static int MSG_LOGIN_FAILED = MSG_MIN_VALUE + 6;

	String mUsername;
	
	Word mWordWrapper;
	ShanbayLoginActivity mShanbayLoginActivity;
	private ShanbayView mShanbayView;
	// Provider.MessageObj mProvider.MessageObj;

	public ShanbayProvider(Context context) {
		super(context);
		mName = "Shanbay";
		mShanbayView = new ShanbayView(context);
		SharedPreferences preferences = getSharedPreferences();
		// 读取用户名, 有值表示之前登录过,那么假设现在也在登录状态中
		// 因为sessionID被保存起来了. 一旦session ID 过期, Dict 会再次请求登录.
		mUsername = preferences.getString(KEY_USERNAME, null);
		String psw = preferences.getString(KEY_PASSWORD, null);
		Log.d(getClass().getCanonicalName(), "Username:" + mUsername + " psw:"
				+ psw);
		if (mUsername != null) {
			mShanbayDict = new ShanbayDict(this);
			mShanbayView.showBlankPanel();
		} else {
			mShanbayView.showLoginPanel();
		}
	}

	void saveUser(String usr, String psw) {
		Editor editor = getSharedPreferences().edit();
		editor.putString(KEY_USERNAME, usr);
		editor.putString(KEY_PASSWORD, psw);
		editor.commit();
	}

	void sendLogin(String usr, String psw) {
		String ss[] = new String[] { usr, psw };
		Message msg = Message.obtain(null, MSG_LOGIN, new Provider.MessageObj(
				ss, ShanbayProvider.this));
		msg.replyTo = mShanbayView.mMessenger;
		sendMessage(msg);
	}

	String pswRequest() {
		SharedPreferences preferences = getSharedPreferences();
		boolean hasPsw = preferences.getBoolean(KEY_STORE_PASSWORD, true);
		if (hasPsw) {
			String psw = preferences.getString(KEY_PASSWORD, null);
			return psw;
		} else {
			return null;
		}
	}

	void needLogin() {
		try {
			mShanbayView.mMessenger
					.send(Message.obtain(null, MSG_LOGIN_FAILED));
		} catch (RemoteException e) {
			Log.w(getName(), e);
		}
	}

	@Override
	public boolean filter(CharSequence chars) {
		return chars != null && chars.length() < 25 && MiscUtils.isAcsii(chars);
	}

	@Override
	public UI getUI() {
		return mShanbayView;
	}

	@Override
	public void onServiceConnected(Messenger service) {
		super.onServiceConnected(service);
	}

	@Override
	public void destroy() {
		if (mShanbayDict != null) {
			mShanbayDict.destory();
		}
		super.destroy();
	}

	@Override
	protected void onHandleMessage(Message msg, int newWhat) {
		switch (newWhat) {
		case MSG_ADD_WORD:
			Provider.MessageObj obj = (Provider.MessageObj) msg.obj;
			Word wordWrapper = (Word) obj.obj;
			mShanbayDict.addWord(wordWrapper);
			try {
				msg.replyTo.send(Message.obtain(null, MSG_ADD_WORD_FINISHED,
						wordWrapper));
			} catch (RemoteException ex) {
				Log.w("MSG_ADD_WORD", ex);
			}
			break;
		case MSG_LOADING_AUDIO:
			obj = (Provider.MessageObj) msg.obj;
			wordWrapper = (Word) obj.obj;
			playAudio(wordWrapper);
			break;
		case MSG_LOGIN:
			try {

				obj = (Provider.MessageObj) msg.obj;
				String ss[] = (String[]) obj.obj;
				String usr = ss[0];
				String psw = ss[1];
				Log.d(getName(), "handle login:" + usr);
				boolean result = false;
				if (usr != null && psw != null) {
					if (mShanbayDict == null) {
						mShanbayDict = new ShanbayDict(this);
					}
					mUsername = usr;
					result = mShanbayDict.login(psw);
					if (result) {
						saveUser(usr, psw);
						msg.replyTo.send(Message
								.obtain(null, MSG_LOGIN_SUCCESS));
					} else {
						msg.replyTo
								.send(Message.obtain(null, MSG_LOGIN_FAILED));
					}
				}

			} catch (RemoteException ex) {
				Log.w("MSG_ADD_WORD", ex);
			}
			break;
		default:
			break;
		}

	}

	private void playAudio(Word wordWrapper) {
		Uri uri = Uri.parse(wordWrapper.mAudioUrl);
		if (uri == null) {
			return;
		}
		if (mp != null) {
			if (mp.isPlaying()) {
				mp.stop();
				mp.reset();
			}
			mp.release();
		}
		try {
			mp = MediaPlayer.create(mContext, uri);
			mp.start();
		} catch (Exception ex) {
			Log.w("Media", ex);
		}
	}

	MediaPlayer mp;

	@Override
	protected void onQuery(CharSequence chars) {
		if (mShanbayDict != null)
			mWordWrapper = mShanbayDict.query(chars.toString());
	}

	@Override
	protected void onUpdate() {
		mShanbayView.setWordWrapper(mWordWrapper);
	}

	class ShanbayView extends CommUIImpl implements View.OnClickListener {

		private ViewAnimator root;
		TextView mDefinitionText;
		TextView mPronText;
		TextView mContentText;

		Button mAddButton;
		Button mAudioButton;
		Button mLoginButton;

		//private ShanbayDict.WordWrapper mWordWrapper;
		private View mWordContainer;
		private View mLoginContainer;
		private View mBlankContainer;

		public ShanbayView(Context context) {
			super(context);
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			root = (ViewAnimator) inflater.inflate(R.layout.word_view, null);
			mWordContainer = root.findViewById(R.id.word);
			mLoginContainer = root.findViewById(R.id.login);
			mBlankContainer = root.findViewById(R.id.blank);
			mAddButton = (Button) root.findViewById(R.id.add);
			mAudioButton = (Button) root.findViewById(R.id.sound);
			mLoginButton = (Button) root.findViewById(R.id.login_button);
			mAddButton.setOnClickListener(this);
			mAddButton.setEnabled(false);
			mAudioButton.setEnabled(false);
			mAudioButton.setOnClickListener(this);
			mLoginButton.setOnClickListener(this);

			mDefinitionText = (TextView) root.findViewById(R.id.definition);
			mPronText = (TextView) root.findViewById(R.id.pron);
			mContentText = (TextView) root.findViewById(R.id.content);

			Typeface tf = Typeface.createFromAsset(context.getAssets(),
					"fonts/segoeui.ttf");

			mPronText.setTypeface(tf);

			mContentView = root;
			inflate();

			showBlankPanel();
		}

		public void setWordWrapper(ShanbayDict.Word wordWrapper) {
			// FIXME 登录失败的消息,比查词结束的消息先到,这样写应该没问题,但是最好还是设置个needLogin变量
			if (root.getCurrentView() != mLoginContainer) {
				mWordWrapper = wordWrapper;
				update();
				showWordPanel();
			}
		}

		private void showLoginPanel() {
			show();
			if (root.getCurrentView() != mLoginContainer) {
				root.setDisplayedChild(root.indexOfChild(mLoginContainer));

			}
		}

		private void showWordPanel() {
			show();
			if (root.getCurrentView() != mWordContainer) {
				root.setDisplayedChild(root.indexOfChild(mWordContainer));

			}
		}

		private void showBlankPanel() {
			show();
			if (root.getCurrentView() != mBlankContainer) {
				root.setDisplayedChild(root.indexOfChild(mBlankContainer));
			}
		}

		private void update() {

			if (mWordWrapper != null) {
				mContentText.setText(mWordWrapper.mContent);
				mPronText
						.setText(Html.fromHtml('[' + mWordWrapper.mPron + ']'));
				mDefinitionText.setText(mWordWrapper.mDefinition);
				mAddButton.setEnabled(!mWordWrapper.isLearning());
				mAudioButton.setEnabled(true);
			} else {
				mContentText.setText("");
				mPronText.setText("");
				mDefinitionText.setText(R.string.notexist);
				mAddButton.setEnabled(false);
				mAudioButton.setEnabled(false);
			}

		}

		@Override
		public void onClick(View v) {
			if (v == mAddButton) {
				Message msg = Message.obtain(null, MSG_ADD_WORD,
						new Provider.MessageObj(mWordWrapper,
								ShanbayProvider.this));
				msg.replyTo = mMessenger;
				sendMessage(msg);
			} else if (v == mAudioButton) {

				Message msg = Message.obtain(null, MSG_LOADING_AUDIO,
						new Provider.MessageObj(mWordWrapper,
								ShanbayProvider.this));
				msg.replyTo = mMessenger;
				sendMessage(msg);
			} else if (v == mLoginButton) {
				Intent intent = new Intent(mContext, ShanbayLoginActivity.class);
				ShanbayLoginActivity.sShanbayProvider = ShanbayProvider.this;
				mContext.startActivity(intent);
			}
		}

		final Messenger mMessenger = new Messenger(new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_ADD_WORD_FINISHED:
					update();
					break;
				case MSG_LOGIN_SUCCESS:
					showBlankPanel();
					if (mShanbayLoginActivity != null) {
						mShanbayLoginActivity.onResultReceive(true);
					}
					break;
				case MSG_LOGIN_FAILED:
					if (mShanbayLoginActivity != null) {
						mShanbayLoginActivity.onResultReceive(false);
					} else {
						showLoginPanel();
					}
					break;
				}
			}
		});

	}
}
