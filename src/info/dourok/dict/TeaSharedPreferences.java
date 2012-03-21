package info.dourok.dict;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
/**
 * 
 * @author douo
 *
 */
public class TeaSharedPreferences implements SharedPreferences {
	protected static final String CHARSET = "utf-8";
	private static final byte[] QUOTE = ("夫君子之行，静以修身，俭以养德。" +
			"非淡泊无以明志，非宁静无以致远。夫学须静也，才须学也，" +
			"非学无以广才，非志无以成学。淫慢则不能励精，险躁则不能治性。" +
			"年与时驰，意与日去，遂成枯落，多不接世，悲守穷庐，将复何及！")
			.getBytes(); // FIXME

	protected SharedPreferences mDelegate;
	protected Context mContext;
	private TEA mTea;

	public TeaSharedPreferences(Context context, SharedPreferences delegate) {
		this.mDelegate = delegate;
		this.mContext = context;
		mTea = new TEA(QUOTE);
	}

	private class Editor implements SharedPreferences.Editor {
		protected SharedPreferences.Editor delegate;

		public Editor() {
			this.delegate = TeaSharedPreferences.this.mDelegate.edit();
		}

		@Override
		public Editor putBoolean(String key, boolean value) {
			delegate.putString(key, encrypt(Boolean.toString(value)));
			return this;
		}

		@Override
		public Editor putFloat(String key, float value) {
			delegate.putString(key, encrypt(Float.toString(value)));
			return this;
		}

		@Override
		public Editor putInt(String key, int value) {
			delegate.putString(key, encrypt(Integer.toString(value)));
			return this;
		}

		@Override
		public Editor putLong(String key, long value) {
			delegate.putString(key, encrypt(Long.toString(value)));
			return this;
		}

		@Override
		public Editor putString(String key, String value) {
			delegate.putString(key, encrypt(value));
			return this;
		}

		@Override
		public boolean commit() {
			return delegate.commit();
		}

		@Override
		public Editor clear() {
			delegate.clear();
			return this;
		}

		@Override
		public Editor remove(String s) {
			delegate.remove(s);
			return this;
		}
	}

	@Override
	public Editor edit() {
		return new Editor();
	}

	@Override
	public Map<String, ?> getAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		final String v = mDelegate.getString(key, null);
		return v != null ? Boolean.parseBoolean(decrypt(v)) : defValue;
	}

	@Override
	public float getFloat(String key, float defValue) {
		final String v = mDelegate.getString(key, null);
		return v != null ? Float.parseFloat(decrypt(v)) : defValue;
	}

	@Override
	public int getInt(String key, int defValue) {
		final String v = mDelegate.getString(key, null);
		return v != null ? Integer.parseInt(decrypt(v)) : defValue;
	}

	@Override
	public long getLong(String key, long defValue) {
		final String v = mDelegate.getString(key, null);
		return v != null ? Long.parseLong(decrypt(v)) : defValue;
	}

	@Override
	public String getString(String key, String defValue) {
		final String v = mDelegate.getString(key, null);
		return v != null ? decrypt(v) : defValue;
	}

	@Override
	public boolean contains(String s) {
		return mDelegate.contains(s);
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
		mDelegate
				.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
		mDelegate
				.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	}

	protected String encrypt(String value) {
		try {
			String result = new String(Base64.encode(
					mTea.encrypt(value.getBytes(CHARSET)), Base64.DEFAULT),
					CHARSET);
			//Log.d(getClass().getName(),value+ " encrypt " + result);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	protected String decrypt(String value) {
		try {
			String result = new String(mTea.decrypt(Base64.decode(
					value.getBytes(CHARSET), Base64.DEFAULT)), CHARSET);
			//Log.d(getClass().getName(),value+ " decrypt " + result);
			return result;
		} catch (Exception e) {
			return null;
		}

	}

}