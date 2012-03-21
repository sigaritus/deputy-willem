package info.dourok.dict;

import info.dourok.dict.provider.Provider;
import info.dourok.dict.provider.shanbay.ShanbayProvider;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class MainActivity extends Activity implements View.OnClickListener,
		DictCommunication {

	EditText mWordEditText;
	// Button mQueryButton;

	// ProviderList mProvider;
	ShanbayProvider mProvider;
	Provider.MessageObj mProviderMessageObj;
	ClipboardAgency mClipboardAgency;

	// public static PersistentCookieStore cookieStore;
	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mClipboardAgency = ClipboardAgency.getClipboardAgency(this);
		setContentView(R.layout.main);
		AppCookieStore.load(this);

		mProvider = new ShanbayProvider(this);
		// mProvider = new ProviderList(this);
		// Provider p= new ShanbayProvider(this);
		// mProvider.addProvider(p);
		// p= new ShanbayProvider(this);
		// mProvider.addProvider(p);
		// p= new ShanbayProvider(this);
		// mProvider.addProvider(p);
		// p= new ShanbayProvider(this);
		// mProvider.addProvider(p);
		//
		// p= new ShanbayNoteProvider((ShanbayProvider)p, this);
		// mProvider.addProvider(p);
		// p= new ShanbayProvider(this);
		// mProvider.addProvider(p);
		// p= new ShanbayProvider(this);
		// mProvider.addProvider(p);
		// p= new ShanbayProvider(this);
		// mProvider.addProvider(p);

		LinearLayout container = (LinearLayout) findViewById(R.id.word_container);

		ScrollView sv = new ScrollView(this);
		sv.addView(mProvider.getUI().getView());
		container.addView(sv, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		// container.addView(mProvider.getUI().getView(),new LayoutParams(
		// LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		mProviderMessageObj = new Provider.MessageObj();
		mProviderMessageObj.provider = mProvider;
		// mWordView = new ShanbayView(this);
		// container.addView(mWordView.getView());
		buildClearButtonMode();
	}

	private Drawable x;

	void buildClearButtonMode() {
		mWordEditText = (EditText) findViewById(R.id.word);
		x = getResources().getDrawable(android.R.drawable.presence_offline);
		x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());
		mWordEditText.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mWordEditText.getCompoundDrawables()[2] == null) {
					return false;
				}
				if (event.getAction() != MotionEvent.ACTION_UP) {
					return false;
				}
				if (event.getX() > mWordEditText.getWidth()
						- mWordEditText.getPaddingRight()
						- x.getIntrinsicWidth()) {
					mWordEditText.setText("");
					mWordEditText.setCompoundDrawables(null, null, null, null);
				}
				return false;
			}
		});
		mWordEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				mWordEditText.setCompoundDrawables(null, null, mWordEditText
						.getText().toString().equals("") ? null : x, null);
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
		});

		mWordEditText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					query();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
	}

	@Override
	protected void onStart() {
		super.onStart();
		doBindService();
		
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	protected void onStop() {
		doUnbindService();
		super.onStop();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case MENU_UNBIND:
			Message msg;
			try {
				mService.send(Message.obtain(null, MSG_KILL_YOURSELF));
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			finish();
			break;
		case MENU_CLIPBOARD:

			msg = Message.obtain(null,
					mClipboardAgency.isBinding() ? MSG_UNBIND_CLIPBOARD
							: MSG_BIND_CLIPBOARD);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;
		case MENU_NOTIFICATION:
			msg = Message.obtain(null,
					DictService.sBindingNotification ? MSG_UNBIND_NOTIFICATION
							: MSG_BIND_NOTIFICATION);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case MENU_SHANBAY_LOGOUT:
			new AlertDialog.Builder(this).setTitle("确认登出扇贝网?")
				.setPositiveButton(getString(android.R.string.yes), new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mProvider.logout();
					}
				})
				.setNegativeButton(getString(android.R.string.cancel), null).show();
			
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuItem item = menu.findItem(MENU_CLIPBOARD);
		if (item != null) {
			item.setTitle(mClipboardAgency.isBinding() ? R.string.menu_remove_clipboard_listener
					: R.string.menu_set_clipboard_listener);
		}
		item = menu.findItem(MENU_NOTIFICATION);
		if (item != null) {
			item.setTitle(DictService.sBindingNotification ? R.string.menu_unbind_notification
					: R.string.menu_bind_notification);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	private final static int MENU_UNBIND = 2;
	private final static int MENU_CLIPBOARD = 3;
	private final static int MENU_NOTIFICATION = 4;
	private final static int MENU_SHANBAY_LOGOUT = 5;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// menu.add(Menu.NONE, MENU_UNBIND, Menu.NONE, "UNBIND Server");
		if (ClipboardAgency.hasNewClipboard) {
			menu.add(Menu.NONE, MENU_CLIPBOARD, Menu.NONE, "");
		}
		menu.add(Menu.NONE, MENU_NOTIFICATION, Menu.NONE, "");
		menu.add(Menu.NONE, MENU_SHANBAY_LOGOUT, Menu.NONE, "登出扇贝");
		return true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		System.out.println("onRestart");
	}

	@Override
	protected void onDestroy() {
		AppCookieStore.save(this);
		mProvider.destroy();
		super.onDestroy();
		System.out.println("onDestroy");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		System.out.println("onNewIntent");
		if (DictService.ACTION_QUERY_CLIPBOARD.equals(intent.getAction())) {
			queryClipBoardText();
			super.onNewIntent(intent);
		} else if (DictService.ACTION_CANCEL_BINDING_CLIPBOARD.equals(intent
				.getAction())) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("remove clipboard listener?")
					.setPositiveButton("Yes", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							Message msg = Message.obtain(null,
									MSG_UNBIND_CLIPBOARD);
							try {
								mService.send(msg);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}).setNegativeButton("No", null).show();
		}
	}

	private void query(){
		// mWordView.busy(true); // FIXME 通知所有Provider 准备
		String w = mWordEditText.getText().toString();
		mProviderMessageObj.obj = w;
					Message msg = Message.obtain(null, MSG_QUERY, mProviderMessageObj);
					// msg.replyTo = mMessenger;
					try {
						mProvider.getUI().busy();
						mService.send(msg);
						
					} catch (RemoteException ex) {
						Log.w("Send MSG_QUERY_WORD", ex);
					}
	}
	
	@Override
	public void onClick(View v) {
//		if (v == mQueryButton) {
//			query();
//			}
//		}
	}

	Messenger mService = null;
	// final Messenger mMessenger = new Messenger(new Handler() {
	// @Override
	// public void handleMessage(Message msg) {
	// switch (msg.what) {
	// case MSG_QUERY_FINISHED:
	// mWordView.setWordWrapper((ShanbayDict.WordWrapper) msg.obj);
	// mWordEditText.setSelection(0, mWordEditText.getText().length());
	// break;
	//
	// }
	// }
	// });
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);
			mProvider.onServiceConnected(mService);

			System.out.println("binded");

			Intent i = getIntent();
			System.out.println(i);
			if (i != null
					&& DictService.ACTION_QUERY_CLIPBOARD.equals(i.getAction())) {
				queryClipBoardText();
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
		}
	};

	public void queryClipBoardText() {
		ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		CharSequence c = cm.getText();
		// System.out.println("detect clipboard:"+ c);
		if (c != null) {
			if (c != null && c.length() < 25 && MiscUtils.isAcsii(c)) {
				c.length();
				mWordEditText.setText(c);
				query();
			}
		}
	}

	void doBindService() {
		System.out.println("Binde service");
		bindService(new Intent(this, DictService.class), mConnection,
				BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		unbindService(mConnection);
	}

}
