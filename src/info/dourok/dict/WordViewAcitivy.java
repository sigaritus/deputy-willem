/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dourok.dict;

import info.dourok.dict.provider.Provider;
import info.dourok.dict.provider.shanbay.ShanbayProvider;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Window;

/**
 * 
 * @author DouO
 */
public class WordViewAcitivy extends Activity implements
		DialogInterface.OnCancelListener, DictCommunication {
	public static final String ACTION = "info.dourok.tools.shanbay.Word";
	private String mWord;
	private ShanbayProvider mShanbayProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		if (i == null) {
			finish();
		}
		mWord = i.getExtras().getString(ACTION);
		if (mWord != null) {
			mShanbayProvider = new ShanbayProvider(this);
			Dialog d = new Dialog(this);
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);
			d.setContentView(mShanbayProvider.getUI().getView());
			d.setCanceledOnTouchOutside(true);
			d.setOnCancelListener(this);
			d.show();
		} else {
			finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		doBindService();
	}

	@Override
	protected void onStop() {
		doUnbindService();
		super.onStop();
	}
	@Override
	protected void onDestroy() {
		mShanbayProvider.destroy();
		super.onDestroy();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}

	Messenger mService = null;
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mShanbayProvider.onServiceConnected(mService);

			Message msg = Message.obtain(null, MSG_QUERY,
					new Provider.MessageObj(mWord, mShanbayProvider));
			mShanbayProvider.getUI().busy();
//			msg.replyTo = mMessenger;
			// 启动后立刻发送查词消息
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	void doBindService() {
		bindService(new Intent(this, DictService.class), mConnection,
				BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		unbindService(mConnection);
	}

//	final Messenger mMessenger = new Messenger(new Handler() {
//		@Override
//		public void handleMessage(Message msg) {
//			switch (msg.what) {
//			case MSG_QUERY_FINISHED:
//				mWordView.setWordWrapper((ShanbayDict.WordWrapper) msg.obj);
//				break;
//			}
//		}
//	});

	public static void showWord(String word, Context context) {
		Intent myIntent = new Intent(context, WordViewAcitivy.class);
		Bundle bundle = new Bundle();
		bundle.putString(ACTION, word);
		myIntent.putExtras(bundle);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		context.startActivity(myIntent);
	}

}
