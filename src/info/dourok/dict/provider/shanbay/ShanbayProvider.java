package info.dourok.dict.provider.shanbay;

import org.json.JSONException;
import org.json.JSONObject;

import info.dourok.dict.MiscUtils;
import info.dourok.dict.R;
import info.dourok.dict.provider.Provider;
import info.dourok.dict.provider.shanbay.ShanbayDict.Examples;
import info.dourok.dict.provider.shanbay.ShanbayDict.Examples.Example;
import info.dourok.dict.provider.shanbay.ShanbayDict.Word;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

public class ShanbayProvider extends Provider {

	ShanbayDict mShanbayDict;
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "passwrod";
	private static final String KEY_NICKNAME = "nickname";
	private static final String KEY_STORE_PASSWORD = "store_password";

	private final static int MSG_ADD_WORD = MSG_MIN_VALUE + 1;
	private final static int MSG_LOADING_AUDIO = MSG_MIN_VALUE + 2;
	private final static int MSG_ADD_WORD_FINISHED = MSG_MIN_VALUE + 3;
	private final static int MSG_LOGIN = MSG_MIN_VALUE + 4;
	private final static int MSG_LOGIN_SUCCESS = MSG_MIN_VALUE + 5;
	private final static int MSG_LOGIN_FAILED = MSG_MIN_VALUE + 6;
	private final static int MSG_GET_EXAMPLE = MSG_MIN_VALUE + 7;
	private final static int MSG_GET_EXAMPLE_FINISHED = MSG_MIN_VALUE + 8;
	private final static int MSG_ADD_NOTE = MSG_MIN_VALUE + 9;
	private final static int MSG_ADD_NOTE_FINISHED = MSG_MIN_VALUE + 10;
	private final static int MSG_AUTO_LOGIN_FAILED = MSG_MIN_VALUE + 11;

	String mUsername;
	String mNickname;

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
		mNickname = preferences.getString(KEY_NICKNAME, null);
		String psw = preferences.getString(KEY_PASSWORD, null);
		Log.d(getClass().getCanonicalName(), "Username:" + mUsername + " psw:"
				+ psw);
		if (mUsername != null) {
			mShanbayDict = new ShanbayDict(this);
			mShanbayView.showBlankPanel();
		} else {
			mShanbayView.mLoginPanel.showPanel();
		}
	}
	
	//FIXME 不应该提供公共方法
	public void logout(){
		if(mShanbayDict!=null){
			mShanbayDict.logout();
		}
		mUsername = null;
		mNickname = null;		
		Editor editor = getSharedPreferences().edit();
		editor.remove(KEY_USERNAME);
		editor.remove(KEY_PASSWORD);
		editor.remove(KEY_NICKNAME);
		editor.commit();
	}

	void saveUser(String usr, String psw, String nick) {
		Editor editor = getSharedPreferences().edit();
		mUsername = usr;
		mNickname = nick;
		editor.putString(KEY_USERNAME, usr);
		editor.putString(KEY_PASSWORD, psw);
		editor.putString(KEY_NICKNAME, nick);
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
		Log.d(getClass().getName(),"pswRequest");
		SharedPreferences preferences = getSharedPreferences();
		boolean hasPsw = preferences.getBoolean(KEY_STORE_PASSWORD, true);
		if (hasPsw) {
			String psw = preferences.getString(KEY_PASSWORD, null);
			return psw;
		} else {
			return null;
		}
	}

	boolean needLogin;

	void loginRequest() {
		try {
			needLogin = true;
			mShanbayView.mMessenger.send(Message.obtain(null,
					MSG_AUTO_LOGIN_FAILED));
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
		case MSG_GET_EXAMPLE:
			mShanbayView.mExamplePanel.handleBgMsg(msg);
			break;
		case MSG_ADD_NOTE:
			mShanbayView.mNotePanel.handleBgMsg(msg);
			break;
		case MSG_LOGIN:
			mShanbayView.mLoginPanel.handleBgMsg(msg);
		default:
			break;
		}

	}

	private void playAudio(Word wordWrapper) {
		Uri uri = Uri.parse(wordWrapper.mAudioUrl);
		if (uri == null) {
			return;
		}

		try {
			if (mp == null) {
				mp = MediaPlayer.create(mContext, uri);
				mp.start();
			} else if (!mp.isPlaying()) {
				mp.start();
			}
		} catch (Exception ex) {
			Log.w("Media", ex);
		}
	}

	MediaPlayer mp;

	@Override
	protected void onQuery(CharSequence chars) {
		if (mp != null) {
			if (mp.isPlaying()) {
				mp.stop();
				mp.reset();
			}
			mp.release();
			mp = null;
		}
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
		ImageButton mAudioButton;

		Button mSwitchButton;
		Button mExampleButton;
		Button mNoteButton;
		boolean mSwitchButtonState; // false zh , true en

		// private ShanbayDict.WordWrapper mWordWrapper;
		private View mWordPanel;
		private TextView mBlankPanel;

		public ShanbayView(Context context) {
			super(context, R.layout.shanbay_word);
			root = (ViewAnimator) mContentView;
			mWordPanel = root.findViewById(R.id.word);
			mBlankPanel = (TextView) root.findViewById(R.id.blank);

			mAddButton = (Button) mWordPanel.findViewById(R.id.add);
			mAudioButton = (ImageButton) mWordPanel.findViewById(R.id.sound);
			mSwitchButton = (Button) mWordPanel
					.findViewById(R.id.switch_button);
			mExampleButton = (Button) mWordPanel.findViewById(R.id.example);
			mNoteButton = (Button) mWordPanel.findViewById(R.id.note);
			mAddButton.setOnClickListener(this);

			mAudioButton.setOnClickListener(this);
			mSwitchButton.setOnClickListener(this);
			mExampleButton.setOnClickListener(this);
			mNoteButton.setOnClickListener(this);
			mDefinitionText = (TextView) mWordPanel
					.findViewById(R.id.definition);
			mPronText = (TextView) mWordPanel.findViewById(R.id.pron);
			mContentText = (TextView) mWordPanel.findViewById(R.id.content);

			Typeface tf = Typeface.createFromAsset(context.getAssets(),
					"fonts/segoeui.ttf");
			mPronText.setTypeface(tf);
			showBlankPanel();
		}

		public void setWordWrapper(ShanbayDict.Word wordWrapper) {
			// 登录失败的消息,比查词结束的消息先到
			if (mShanbayDict == null) {
				mShanbayView.mLoginPanel.showPanel();
			} else if (!needLogin) { // FIXME
				mWordWrapper = wordWrapper;
				mExamplePanel.reset();
				mNotePanel.reset();
				update();
				showWordPanel();
			}
		}

		private void showWordPanel() {
			show();
			if (root.getCurrentView() != mWordPanel) {
				root.setDisplayedChild(root.indexOfChild(mWordPanel));

			}
		}

		private void showBlankPanel() {
			show();
			mBlankPanel.setText(Html.fromHtml(
					String.format(mContext.getString(R.string.shanbay_welcome_template, mNickname))));
			if (root.getCurrentView() != mBlankPanel) {
				root.setDisplayedChild(root.indexOfChild(mBlankPanel));
			}
		}

		private void update() {
			if (mWordWrapper != null) {
				mContentText.setText(mWordWrapper.mContent);
				mPronText
						.setText(Html.fromHtml('[' + mWordWrapper.mPron + ']'));
				mDefinitionText.setText(mWordWrapper.mDefinition);
				mAudioButton.setVisibility(View.VISIBLE);
				mSwitchButton.setVisibility(View.VISIBLE);
				mSwitchButton.setText(R.string.en_definition);
				mSwitchButtonState = false;
				if (mWordWrapper.isLearning()) {
					mAddButton.setVisibility(View.GONE);
					mExampleButton.setVisibility(View.VISIBLE);
					mNoteButton.setVisibility(View.VISIBLE);
				} else {
					mAddButton.setVisibility(View.VISIBLE);
					mExampleButton.setVisibility(View.GONE);
					mNoteButton.setVisibility(View.GONE);
				}
			} else {
				mContentText.setText("");
				mPronText.setText("");
				mDefinitionText.setText(R.string.notexist);
				mAddButton.setVisibility(View.GONE);
				mAudioButton.setVisibility(View.GONE);
				mSwitchButton.setVisibility(View.GONE);
				mExampleButton.setVisibility(View.GONE);
				mNoteButton.setVisibility(View.GONE);
				mSwitchButton.setText(R.string.en_definition);
				mSwitchButtonState = false;
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
			} else if (v == mSwitchButton) {
				if (true == mSwitchButtonState) {
					mSwitchButtonState = false;
					mDefinitionText.setText(mWordWrapper.mDefinition);
					mSwitchButton.setText(R.string.en_definition);
				} else {
					mSwitchButtonState = true;
					mDefinitionText.setText(mWordWrapper.mEnDefinition);
					mSwitchButton.setText(R.string.definition);
				}
			} else if (v == mExampleButton) {
				mExamplePanel.getExmaple();
			} else if (v == mNoteButton) {
				mNotePanel.showPanel();
			} else if (mExamplePanel.handleButton(v)) {
				return;
			} else if (mNotePanel.handleButton(v)) {
				return;
			} else if (mLoginPanel.handleButton(v)) {
				return;
			}
		}

		final Messenger mMessenger = new Messenger(new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_ADD_WORD_FINISHED:
					update();
					break;
				case MSG_GET_EXAMPLE_FINISHED:
					mExamplePanel.handleFgMsg(msg);
					break;
				case MSG_ADD_NOTE_FINISHED:
					mNotePanel.handleFgMsg(msg);
					break;
				case MSG_LOGIN_SUCCESS:
				case MSG_LOGIN_FAILED:
				case MSG_AUTO_LOGIN_FAILED:
					mLoginPanel.handleFgMsg(msg);
					break;
				}
			}
		});
		LoginPanel mLoginPanel = new LoginPanel();

		class LoginPanel extends Panel {
			/**
			 * LoginPanel
			 */
			TextView mUserView;
			TextView mPswView;
			Button mLoginButton;
			ProgressDialog mDialog;
			View mPanelView;

			@Override
			void init() {
				ViewStub stub = (ViewStub) root.findViewById(R.id.stub_login);
				mPanelView = stub.inflate();
				mUserView = (EditText) mPanelView.findViewById(R.id.username);
				mPswView = (EditText) mPanelView.findViewById(R.id.password);
				mLoginButton = (Button) mPanelView
						.findViewById(R.id.login_button);
				mLoginButton.setOnClickListener(mShanbayView);

			}

			@Override
			void showPanel() {
				if (mPanelView == null)
					init();
				show();
				root.setDisplayedChild(root.indexOfChild(mPanelView));
			}

			@Override
			void update() {
			}

			@Override
			void reset() {
			}

			@Override
			void handleBgMsg(Message msg) {
				try {
					Provider.MessageObj obj = (Provider.MessageObj) msg.obj;
					String ss[] = (String[]) obj.obj;
					String usr = ss[0];
					String psw = ss[1];
					Log.d(getName(), "handle login:" + usr);
					JSONObject userInfo = null;
					if (usr != null && psw != null) {
						if (mShanbayDict == null) {
							mShanbayDict = new ShanbayDict(ShanbayProvider.this);
						}
						userInfo = mShanbayDict.loginAndGetUserInfo(usr,
								psw);
						if (userInfo != null) {
							int r;
							try {
								r = userInfo.getInt("result");
								if (r == 1) {
									saveUser(usr, psw,
											userInfo.getString("nickname"));
									msg.replyTo.send(Message.obtain(null,
											MSG_LOGIN_SUCCESS));
									return;
								}
							} catch (JSONException e) {
								
								e.printStackTrace();
							}
						}
						msg.replyTo
								.send(Message.obtain(null, MSG_LOGIN_FAILED));

					}

				} catch (RemoteException ex) {
					Log.w("MSG_ADD_WORD", ex);
				}

			}

			@Override
			void handleFgMsg(Message msg) {
				switch (msg.what) {
				case MSG_LOGIN_SUCCESS:
					needLogin = false;
					showBlankPanel();
					if (true) {
						mDialog.dismiss();
						Toast.makeText(mContext, "success", 3000).show();
					}
					break;
				case MSG_LOGIN_FAILED:
					mDialog.dismiss();
					Toast.makeText(mContext, "failed", 3000).show();
					break;
				case MSG_AUTO_LOGIN_FAILED:
					showPanel();
					break;
				}
			}

			@Override
			boolean handleButton(View v) {
				if (v == mLoginButton) {
					if (mDialog == null) {
						mDialog = new ProgressDialog(mContext);
						mDialog.setCancelable(false);
						mDialog.setMessage("Please wait...");
					}
					mDialog.show();
					String username = mUserView.getText().toString().trim();
					String password = mPswView.getText().toString().trim();
					sendLogin(username, password);
					return true;
				}
				return false;
			}
		}

		NotePanel mNotePanel = new NotePanel();

		class NotePanel extends Panel {
			EditText mNoteInput;
			Button mComfirmButton;
			Button mCancelButton;
			View mPanelView;

			@Override
			void init() {
				ViewStub stub = (ViewStub) root.findViewById(R.id.stub_note);
				mPanelView = stub.inflate();
				mNoteInput = (EditText) mPanelView
						.findViewById(R.id.note_input);
				mComfirmButton = (Button) mPanelView.findViewById(R.id.comfirm);
				mCancelButton = (Button) mPanelView.findViewById(R.id.cancel);
				mComfirmButton.setOnClickListener(mShanbayView);
				mCancelButton.setOnClickListener(mShanbayView);
			}

			@Override
			void showPanel() {
				if (mPanelView == null)
					init();
				show();
				root.setDisplayedChild(root.indexOfChild(mPanelView));

			}

			@Override
			void update() {
				// TODO 处理异常情况
				mNoteInput.setEnabled(false);
				mComfirmButton.setVisibility(View.GONE);
			}

			@Override
			void reset() {
				if (mPanelView != null) {
					mNoteInput.setText("");
					mNoteInput.setEnabled(true);
					mComfirmButton.setVisibility(View.VISIBLE);
				}
			}

			@Override
			void handleBgMsg(Message msg) {
				Provider.MessageObj obj = (Provider.MessageObj) msg.obj;
				Word word = (Word) obj.obj;
				String note = mNoteInput.getText().toString();
				try {
					mShanbayDict.addNote(word, note);
					Toast.makeText(mContext, "添加成功:", 5000);
					try {
						msg.replyTo.send(Message.obtain(null,
								MSG_ADD_NOTE_FINISHED, Boolean.TRUE));
					} catch (RemoteException ex) {
						Log.w("MSG_ADD_NOTE", ex);
					}
				} catch (ShanbayException e) {
					Toast.makeText(mContext, "添加失败:" + e.getMessage(), 5000);
					try {
						msg.replyTo.send(Message.obtain(null,
								MSG_ADD_NOTE_FINISHED, Boolean.FALSE));
					} catch (RemoteException ex) {
						Log.w("MSG_ADD_NOTE", ex);
					}
					return;
				}
			}

			@Override
			void handleFgMsg(Message msg) {
				if ((Boolean) msg.obj) {
					update();
				}
				showPanel();
			}

			boolean isValid() {
				int l = mNoteInput.getText().length();
				boolean v = l > 0 && l < 300;
				Log.d("isValid", v + "");

				return v;
			}

			@Override
			boolean handleButton(View v) {
				if (v == mCancelButton) {
					showWordPanel();
					return true;
				} else if (v == mComfirmButton) {
					if (isValid()) {
						Message msg = Message.obtain(null, MSG_ADD_NOTE,
								new Provider.MessageObj(mWordWrapper,
										ShanbayProvider.this));
						msg.replyTo = mMessenger;
						busy();
						sendMessage(msg);
					}
				}

				return false;
			}

		}

		ExamplePanel mExamplePanel = new ExamplePanel();

		class ExamplePanel extends Panel implements View.OnClickListener {
			TextView mExample1;
			TextView mExample2;
			Examples mExamples;
			Button mBackButton;
			View mPanelView;
			String mTemplate;
			boolean mShowTranslation1;
			boolean mShowTranslation2;

			@Override
			public void init() {
				ViewStub stub = (ViewStub) root.findViewById(R.id.stub_example);
				mPanelView = stub.inflate();
				mExample1 = (TextView) mPanelView.findViewById(R.id.example_1);
				mExample1.setOnClickListener(this);
				mExample2 = (TextView) mPanelView.findViewById(R.id.example_2);
				mExample2.setOnClickListener(this);
				mBackButton = (Button) mPanelView.findViewById(R.id.back);
				mBackButton.setOnClickListener(mShanbayView);
				mTemplate = mContext.getResources().getString(
						R.string.shanbay_example_template);
			}

			@Override
			public void showPanel() {
				if (mPanelView == null)
					init();
				show();
				root.setDisplayedChild(root.indexOfChild(mPanelView));
			}

			@Override
			void update() {

				if (mPanelView == null)
					init();
				if (mExamples != null) {
					assert (mExamples.mExamples.length == 2);
					Example e1 = mExamples.mExamples[0];
					Example e2 = mExamples.mExamples[1];
					if (e1 != null) {
						String spanned = String.format(mTemplate, e1.mFirst,
								e1.mMid, e1.mLast,
								mShowTranslation1 ? e1.mTranslation : "");
						mExample1.setText(Html.fromHtml(spanned));
					} else {
						mExample1.setVisibility(View.GONE);
					}
					if (e2 != null) {
						String spanned = String.format(mTemplate, e2.mFirst,
								e2.mMid, e2.mLast,
								mShowTranslation2 ? e2.mTranslation : "");
						mExample2.setText(Html.fromHtml(spanned));
					} else {
						mExample2.setVisibility(View.GONE);
					}
				}
			}

			@Override
			public void reset() {
				if (mPanelView != null) {
					mExamples = null;
					mExample1.setVisibility(View.VISIBLE);
					mShowTranslation1 = false;
					mExample2.setVisibility(View.VISIBLE);
					mShowTranslation2 = false;
				}
			}

			@Override
			public void handleBgMsg(Message msg) {
				Provider.MessageObj obj = (Provider.MessageObj) msg.obj;
				Word wordWrapper = (Word) obj.obj;
				Examples exs = mShanbayDict.getExample(wordWrapper.mLearningId);
				try {
					msg.replyTo.send(Message.obtain(null,
							MSG_GET_EXAMPLE_FINISHED, exs));
				} catch (RemoteException ex) {
					Log.w("MSG_GET_EXAMPL", ex);
				}
			}

			@Override
			public void handleFgMsg(Message msg) {
				mExamples = (Examples) msg.obj;
				update();
				showPanel();
			}

			private void getExmaple() {
				if (mExamples != null) {
					showPanel();
				} else {

					Message msg = Message.obtain(null, MSG_GET_EXAMPLE,
							new Provider.MessageObj(mWordWrapper,
									ShanbayProvider.this));
					msg.replyTo = mMessenger;
					busy();
					sendMessage(msg);
				}
			}

			@Override
			public boolean handleButton(View v) {
				if (v == mBackButton) {
					showWordPanel();
					return true;
				}
				return false;
			}

			@Override
			public void onClick(View v) {
				if (v == mExample1) {
					mShowTranslation1 = !mShowTranslation1;
					update();
				} else if (v == mExample2) {
					mShowTranslation2 = !mShowTranslation2;
					update();
				}

			}
		}

		abstract class Panel {
			abstract void init();

			abstract void showPanel();

			abstract void update();

			abstract void reset();

			abstract void handleBgMsg(Message msg);

			abstract void handleFgMsg(Message msg);

			abstract boolean handleButton(View v);
		}
	}

}
