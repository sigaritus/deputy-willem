package info.dourok.dict.provider;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class ListProvider extends Provider {
	private ArrayList<Provider> mProviders;
	private int mCount;
	private final static int MSG_ONE_PROVIDER_SHOWN = MSG_MIN_VALUE + 1;
	private ListUI mUI;

	public ListProvider(Context context) {
		super(context);
		mProviders = new ArrayList<Provider>();
		mUI = new ListUI();
	}

	public void addProvider(Provider provider) {
		mProviders.add(provider);
	}

	public ArrayList<Provider> getProviders() {
		mCount++;
		return mProviders;

	}

	@Override
	public void dispatchMessage(Message msg) {
		for (Provider p : mProviders) {
			System.out.println(p.mName + ":" + Integer.toHexString(msg.what)
					+ "  " + Integer.toHexString(msg.what >> 16));
			if ((msg.what >> 16) == p.mMsgSpace) {
				p.dispatchMessage(msg);
				break;
			}
		}
	}

	@Override
	public void onServiceConnected(Messenger service) {
		for (Provider p : mProviders) {
			p.onServiceConnected(service);
		}
	}

	@Override
	protected void onUpdate() {

	}

	@Override
	protected void onQuery(CharSequence chars) {
		for (Provider p : mProviders) {
			if (p.filter(chars)) {
				mUI.show(p);
				p.query(chars);
			}
		}

	}

	@Override
	protected void onHandleMessage(Message msg, int newWhat) {
		// 废弃
	}

	@Override
	public void destroy() {
		for (Provider p : mProviders) {
			p.destroy();
		}
	}

	@Override
	public boolean filter(CharSequence chars) {
		return true;
	}

	@Override
	public UI getUI() {
		return mUI;
	}

	class ListUI implements UI {
		ListView mListView;
		ProviderAdapter mProviderAdapter;

		public ListUI() {
			mListView = new ListView(mContext);
			mProviderAdapter = new ProviderAdapter();
			mListView.setAdapter(mProviderAdapter);
		}

		public void show(Provider p) {
			try {
				mMessenger
						.send(Message.obtain(null, MSG_ONE_PROVIDER_SHOWN, p));
			} catch (RemoteException e) {
				Log.w(getClass().getName(), e);
			}
		}

		@Override
		public void busy() {
			mCount = 0;
			mProviderAdapter.notifyDataSetChanged();
		}


		@Override
		public View getView() {
			return mListView;
		}

		final Messenger mMessenger = new Messenger(new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_ONE_PROVIDER_SHOWN:
					((Provider) msg.obj).getUI().busy();
					mCount++;
					mProviderAdapter.notifyDataSetChanged();
					break;
				default:
					break;
				}
			}
		});

	}

	class ProviderAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mCount;
		}

		@Override
		public Object getItem(int position) {
			return mProviders.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mProviders.get(position).mMsgSpace;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mProviders.get(position).getUI().getView();
		}

	}

}
