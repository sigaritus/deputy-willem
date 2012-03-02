package info.dourok.dict;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import android.content.Context;
import android.util.Log;

public class ClipboardAgency {
	Object mClipboard;
	Object mOnPrimaryClipChangedListenerImpl;
	Method addLinstener;
	Method removeLinstener;
	Method getPrimaryClip;
	Method getItemAt;
	Method getText;
	Context mApplicationContext;
	boolean binding;
	static boolean hasNewClipboard;
	static {
		try {
			Class.forName("android.content.ClipboardManager");
			hasNewClipboard = true;
		} catch (ClassNotFoundException e) {
			hasNewClipboard = false;
		}
	}
	PrimaryClipChangedListener mListener;
	private static ClipboardAgency sigleton;

	public interface PrimaryClipChangedListener {
		public void onPrimaryClipChanged(String newString);
	}

	public static ClipboardAgency getClipboardAgency(Context context) {
		if (sigleton == null) {
			sigleton = new ClipboardAgency(context);
		}
		return sigleton;
	}

	private ClipboardAgency(Context context) {
		mApplicationContext = context.getApplicationContext();
		initClipboard();
		tempShanbay();
	}

	private void initClipboard() {
		try {
			Object clipboard = mApplicationContext
					.getSystemService(Context.CLIPBOARD_SERVICE);
			Class<?> cClipboardManager = Class
					.forName("android.content.ClipboardManager");
			Class<?> cListener = Class
					.forName("android.content.ClipboardManager$OnPrimaryClipChangedListener");
			Class<?> cClipData = Class.forName("android.content.ClipData");
			Class<?> cItem = Class.forName("android.content.ClipData$Item");
			if (cClipboardManager.isInstance(clipboard)) {
				hasNewClipboard = true;
				addLinstener = cClipboardManager.getMethod(
						"addPrimaryClipChangedListener", cListener);
				removeLinstener = cClipboardManager.getMethod(
						"removePrimaryClipChangedListener", cListener);
				getPrimaryClip = cClipboardManager.getMethod("getPrimaryClip",
						(Class[]) null);
				getItemAt = cClipData.getMethod("getItemAt", int.class);
				getText = cItem.getMethod("getText", (Class[]) null);

				mOnPrimaryClipChangedListenerImpl = Proxy.newProxyInstance(
						ClassLoader.getSystemClassLoader(),
						new Class[] { cListener },
						new OnPrimaryClipChangedListenerImpl());
				mClipboard = clipboard;
			} else {
				hasNewClipboard = false;
			}
		} catch (Exception e) {
			Log.w(DictService.class.getName(), e);
		}
	}

	public void setPrimaryClipChangedListener(
			PrimaryClipChangedListener listener) {
		this.mListener = listener;
	}

	private class OnPrimaryClipChangedListenerImpl implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (method.getName().equals("onPrimaryClipChanged")) {

				if (mListener != null) {
					Object clipData = getPrimaryClip.invoke(mClipboard,
							(Object[]) null);
					Object item = getItemAt.invoke(clipData, 0);
					CharSequence charseq = (CharSequence) getText.invoke(item,
							(Object[]) null);
					final String s = charseq.toString();
					mListener.onPrimaryClipChanged(s);
				}
				return (Void) null;
			} else if (method.getName().equals("equals")) {
				return proxy == args[0];
			} else {
				return method.invoke(this, args);
			}
		}
	}

	public void bindClipboard() {
		Log.d(getClass().getName(), "BindClipborad");
		try {
			if (binding == false) {
				addLinstener.invoke(mClipboard,
						mOnPrimaryClipChangedListenerImpl);
				binding = true;
			}
		} catch (Exception e) {
			Log.w(getClass().getName(), e);
		}

	}

	public boolean isBinding() {
		return binding;
	}

	public void unbindClipboard() {
		Log.d(getClass().getName(), "BindClipborad");
		try {
			if (binding) {
				removeLinstener.invoke(mClipboard,
						mOnPrimaryClipChangedListenerImpl);
				binding = false;
			}
		} catch (Exception e) {
			Log.w(getClass().getName(), e);
		}
	}
	
	//FIXME 下面两个方法作临时之用.
	private void tempShanbay(){
		mListener = new PrimaryClipChangedListener() {
			
			@Override
			public void onPrimaryClipChanged(String newString) {
				final String s = newString;

				if (filter(s)) {
					new Thread() {
						@Override
						public void run() {
							 Log.d("shanbay", "query:" + s.trim());
							WordViewAcitivy.showWord(s, mApplicationContext);
						}
					}.start();
				}
			}
		};
	}
	private boolean filter(String s) {
		return s != null && s.length() < 25 && MiscUtils.isAcsii(s);
	}
}
