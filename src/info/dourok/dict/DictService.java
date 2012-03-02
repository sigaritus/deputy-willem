/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package info.dourok.dict;

import info.dourok.dict.provider.Provider;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 * @author DouO
 */
public class DictService extends Service implements DictCommunication {

	Worker mQueryWorker;
	QueryHandler mQueryHandler;
	Messenger mMessenger;
	public static final String ACTION_QUERY_CLIPBOARD = "info.dourok.tools.shanbay.ACTION_QUERY_CLIPBOARD";
	public static final String ACTION_CANCEL_BINDING_CLIPBOARD = "info.dourok.tools.shanbay.ACTION_CANCEL_BINDING_CLIPBOARD";
	public static boolean sBindingNotification;
	public ClipboardAgency mClipboardAgency;

	@Override
	public void onCreate() {
		super.onCreate();
		System.out.println("service Create");
		mClipboardAgency = ClipboardAgency.getClipboardAgency(this);
		mQueryWorker = new Worker("QueryWorker");
		mQueryHandler = new QueryHandler(mQueryWorker.getLooper());
		mMessenger = new Messenger(mQueryHandler);
	}

	@Override
	public void onDestroy() {
		System.out.println("service destroy");
		// unbindClipboard();

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	class QueryHandler extends Handler {
		public QueryHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void dispatchMessage(Message msg) {
			switch (msg.what) {
			case MSG_QUERY:
				Provider.MessageObj obj = (Provider.MessageObj) msg.obj;
				String w = (String) obj.obj;
				w = w.trim();
				Log.d("query", w);
				obj.provider.query(w);
				// try {
				// msg.replyTo.send(Message.obtain(null,
				// MSG_QUERY_FINISHED));
				// } catch (RemoteException ex) {
				// Log.w("MSG_QUERY_WORD", ex);
				// }
				break;
			case MSG_BIND_CLIPBOARD:
				bindClipboard();
				break;
			case MSG_UNBIND_CLIPBOARD:
				unbindClipboard();
				break;
			case MSG_KILL_YOURSELF:
				stopSelf();
				break;
			case MSG_BIND_NOTIFICATION:
				bindNotification();
				break;
			case MSG_UNBIND_NOTIFICATION:
				unbindNotification();
				break;
			default:
				if (msg.obj instanceof Provider.MessageObj) {
					obj = (Provider.MessageObj) msg.obj;
					obj.provider.dispatchMessage(msg);
				}
			}
		}
	}

	public void bindClipboard() {
		mClipboardAgency.bindClipboard();
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		int icon = R.drawable.touch_icon;
		CharSequence tickerText = "Listenning Clipboard";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		Context context = getApplicationContext();
		CharSequence contentTitle = "DeputyDict";
		CharSequence contentText = "Listenning Clipboard";
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setAction(ACTION_CANCEL_BINDING_CLIPBOARD);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		nm.notify(DICT_CANCEL_CLIPBOARD_NOTIFICATION, notification);
		// Toast.makeText(this, "Listenning Clipboard", 5000).show();

	}

	public void unbindClipboard() {
		mClipboardAgency.unbindClipboard();
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(DICT_CANCEL_CLIPBOARD_NOTIFICATION);
		Toast.makeText(this, "Stop listenning Clipboard", 5000).show();
	}

	private static final int DICT_NOTIFICATION = 0x141;
	private static final int DICT_CANCEL_CLIPBOARD_NOTIFICATION = 0x142;

	public void bindNotification() {

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		int icon = R.drawable.touch_icon;
		CharSequence tickerText = "Hello";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		Context context = getApplicationContext();
		CharSequence contentTitle = "My notification";
		CharSequence contentText = "Hello World!";
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setAction(ACTION_QUERY_CLIPBOARD);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		nm.notify(DICT_NOTIFICATION, notification);
		sBindingNotification = true;
	}

	public void unbindNotification() {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(DICT_NOTIFICATION);
		sBindingNotification = false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mMessenger.getBinder();
	}
}
