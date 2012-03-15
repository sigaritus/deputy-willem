/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package info.dourok.dict.provider.shanbay;

import info.dourok.dict.AppCookieStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * 
 * @author DouO
 */
public class ShanbayDict {

	private final static String HOST = "www.shanbay.com";
	private final static String LOING_URL = "http://" + HOST + "/wap/login/";
	private final static String HOME_URL = "http://" + HOST + "/home/";

	private final static String API_QUERY = "http://" + HOST + "/api/word/";
	private final static String API_ADD_WORD = "http://" + HOST
			+ "/api/learning/add/";
	private final static String API_USER_INFO = "http://" + HOST
			+ "/api/user/info/";
	private final static String API_ADD_NOTE = "http://" + HOST
			+ "/api/note/add/{{learning_id}}?note=";
	private final static String API_GET_EXAMPLE = "http://" + HOST
			+ "/api/learning/examples/";
	private final static String USER_AGENT = "Mozilla/5.0 (X11; U; Linux "
			+ "i686; en-US; rv:1.8.1.6) Gecko/20061201 Firefox/2.0.0.6 (Ubuntu-feisty)";

	private AndroidHttpClient httpClient;
	private HttpContext mLocalContext; // 线程安全未验证,只能通过一个worker工作
	private ShanbayProvider mShanbayProvider;

	public ShanbayDict(ShanbayProvider shanbayProvider) {
		this.mShanbayProvider = shanbayProvider;
		mLocalContext = new BasicHttpContext();

		mLocalContext.setAttribute(ClientContext.COOKIE_STORE,
				AppCookieStore.getInstance());
	}

	private HttpClient getClient() {
		if (httpClient == null) {
			httpClient = AndroidHttpClient.newInstance(USER_AGENT);
			HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),
					7000);
		}
		return httpClient;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	boolean checkLogin() {
		HttpGet getHome = new HttpGet(HOME_URL);
		HttpResponse re;
		try {
			re = getClient().execute(getHome, mLocalContext);
			return re.getStatusLine().getStatusCode() == 200;
		} catch (ClientProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		getHome.abort();
		return false;
	}

	private boolean login() {
		String psw = mShanbayProvider.pswRequest();
		String usr = mShanbayProvider.mUsername;
		if (psw == null) {
			mShanbayProvider.loginRequest();
			return false;
		}
		boolean result = login(usr, psw);
		if (!result) {
			// 告诉Provider 登录失败,原先保存的密码失效.
			mShanbayProvider.loginRequest();
		}
		return result;
	}

	/**
	 * 
	 * @param usr
	 * @param psw
	 * @return 可能为null
	 */
	JSONObject loginAndGetUserInfo(String usr, String psw) {
		if (login(usr, psw)) {
			return getUserInfo();
		} else {
			return null;
		}
	}

	void logout() {
		AppCookieStore.getInstance().clear();
	}

	private boolean login(String usr, String psw) {
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair("username", usr));
		qparams.add(new BasicNameValuePair("password", psw));
		HttpResponse re;
		try {
			HttpPost postLogin = new HttpPost(LOING_URL);
			StringEntity postEntity = new StringEntity(URLEncodedUtils.format(
					qparams, "utf-8"));
			System.out.println(EntityUtils.toString(postEntity));
			postLogin.setEntity(postEntity);
			re = getClient().execute(postLogin, mLocalContext);

			postLogin.abort();
			// 如果返回有效的sessionid则登录成功.
			for (Header h : re.getHeaders("Set-Cookie")) {
				// Log.d(getClass().getName(), "login Cookie : " + h);
				String s = h.getValue();
				if (s.contains("sessionid")) {
					s = s.substring(s.indexOf('=') + 1, s.indexOf(';'));
					if (s.length() > 0) {
						Log.i(getClass().getName(), "login success ,sessionid:"
								+ s);
						return true;
					}
				}
			}
			// 因为如果已经登录,那么再次登录不会再设置新的sessionid.
			// 所以登录后跳转到home,也算登录成功
			if (re.getStatusLine().getStatusCode() == 302) {
				return true;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d(getClass().getName(), "login failed");
		return false;
	}

	public void addWord(Word word) {
		if (word == null) {
			throw new IllegalArgumentException("不能为空");
		}
		try {
			addWord(word, false);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void addWord(Word word, boolean recall) throws IOException {
		try {
			HttpGet getAdd = new HttpGet(API_ADD_WORD + word.mContent);
			JSONObject json = getClient().execute(getAdd, responseHandler,
					mLocalContext);
			getAdd.abort();
			if (json == null) {
				// json解析是吧,可能是session过期了.重新登录再请求
				if (!recall) {
					if (login()) {
						addWord(word, true);
					} else {
						// 登录失败 暂时无须做任何处理,在login中已经通知Provider了
					}
				}

			} else {
				word.updateLearningId(json);
			}
		} catch (JSONException ex) {
		}
	}

	public Word query(String word) {
		if (word == null || word.equals("")) {
			return null;
			// throw new IllegalArgumentException("不能为空");
		}
		try {
			try {
				return query(word, false);
			} catch (JSONException ex) {
				ex.printStackTrace();
				return null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * 见 {@link #addWord(Word, boolean)}
	 * 
	 * @param word
	 * @param recall
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	private Word query(String word, boolean recall) throws IOException,
			JSONException {
		word = word.trim();
		HttpGet getQuery = new HttpGet(API_QUERY + word);
		Log.d("query url", API_QUERY + word);
		JSONObject json = getClient().execute(getQuery, responseHandler,
				mLocalContext);

		getQuery.abort();

		if (json == null) {
			if (!recall) {
				if (login()) {
					return query(word, true);
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return new Word(json);
		}
	}

	public JSONObject getUserInfo() {

		try {
			return getUserInfo(false);
		} catch (Exception ex) {
			ex.printStackTrace();
			return new JSONObject();
		}
	}

	private JSONObject getUserInfo(boolean recall) throws IOException,
			JSONException {
		HttpGet getQuery = new HttpGet(API_USER_INFO);
		JSONObject json = getClient().execute(getQuery, responseHandler,
				mLocalContext);
		getQuery.abort();
		if (json == null) {
			if (!recall) {
				if (login()) {
					return getUserInfo(true);
				}
			}
			json = new JSONObject();
		}
		return json;
	}

	public void addNote(Word word, String note) throws ShanbayException {
		if (word == null || note == null) {
			throw new IllegalArgumentException("不能为空");
		}
		if (note.length() > 300) {
			throw new IllegalArgumentException("笔记总长度不能超过300个字符");
		}
		try {
			addNote(word, note, false);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (ex instanceof ShanbayException)
				throw (ShanbayException) ex;
		}
	}

	private void addNote(Word word, String note, boolean recall)
			throws IOException, ShanbayException {
		note = note.trim();
		HttpGet getQuery = new HttpGet(API_ADD_NOTE.replace("{{learning_id}}",
				Integer.toString(word.mLearningId)).concat(note));

		JSONObject json = getClient().execute(getQuery, responseHandler,
				mLocalContext);

		getQuery.abort();

		if (json == null) {
			if (!recall) {
				if (login()) {
					addNote(word, note, true);
				}
			}
		} else {
			int s;
			String info = null;
			try {
				s = json.getInt("note_status");
				if (s == 1) {
					return;
				}
				info = ShanbayException.NOTE_STATUS.get(s);
				if (info == null) {
					info = ShanbayException.UNDEFINE;
				}

			} catch (JSONException e) {
				e.getMessage();
			}
			throw new ShanbayException(info);

		}
		Log.d(getClass().getName(), json + "");
	}

	public Examples getExample(int learnId) {
		try {
			return getExample(learnId, false);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Examples getExample(int learnId, boolean recall)
			throws IOException, JSONException {

		HttpGet getQuery = new HttpGet(API_GET_EXAMPLE + learnId);

		JSONObject json = getClient().execute(getQuery, responseHandler,
				mLocalContext);
		getQuery.abort();
		System.out.println(json);
		if (json == null) {
			if (!recall) {
				if (login()) {
					return getExample(learnId, true);
				}
			}
		} else {
			return new Examples(json);
		}
		return null;
	}

	JSONResponseHandler responseHandler = new JSONResponseHandler();

	final static class JSONResponseHandler implements
			ResponseHandler<JSONObject> {

		@Override
		public JSONObject handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			HttpEntity entity = response.getEntity();
			JSONObject obj = null;
			try {
				if (entity != null) {
					obj = new JSONObject(EntityUtils.toString(entity));
				}
			} catch (JSONException ex) {
			}
			return obj;
		}
	}

	public void destory() {
		Log.d(getClass().getName(), "destory");
		if (httpClient != null)
			httpClient.close();
	}

	/**
	 * 封装单词对象
	 * 
	 * @author DouO
	 * 
	 */
	public static class Word implements Parcelable {
		static enum ATTITYPE {

		}

		static enum ContentType {
			vocabulary, sentence;
		}

		int mLearningId;
		int mContentId;
		ContentType mContentType;
		String mAudioUrl;
		String mPron;
		String mDefinition;
		String mContent;
		String mEnDefinition;

		public Word(JSONObject json) throws JSONException {
			Log.v("query", json.toString());
			mLearningId = json.getInt("learning_id");
			Object tmp = json.get("voc");
			if (tmp instanceof JSONObject) {
				JSONObject voc = (JSONObject) tmp;
				// mContentId = voc.getInt("content_id");
				mContent = voc.getString("content");
				mDefinition = voc.getString("definition");
				JSONObject en = voc.getJSONObject("en_definitions");
				Iterator<?> itr = en.keys();
				mEnDefinition = "";
				while (itr.hasNext()) {
					String k = (String) itr.next();
					mEnDefinition += k + " : ";
					JSONArray array = en.getJSONArray(k);
					for (int i = 0; i < array.length(); i++)
						mEnDefinition += array.getString(i) + "\n";
				}

				mPron = voc.getString("pron");
				mAudioUrl = voc.getString("audio");
				String type = voc.getString("content_type");
				try {
					mContentType = ContentType.valueOf(type);
				} catch (IllegalArgumentException e) {
				}

			} else {
				throw new JSONException("can't load content");
			}
		}

		public void updateLearningId(JSONObject json) throws JSONException {
			mLearningId = json.getInt("id");
		}

		public boolean isLearning() {
			return mLearningId != 0;
		}

		public static final Parcelable.Creator<Word> CREATOR = new Creator<Word>() {

			@Override
			public Word createFromParcel(Parcel parcel) {
				return new Word(parcel);
			}

			@Override
			public Word[] newArray(int i) {
				return new Word[i];
			}
		};

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int i) {
			parcel.writeInt(mLearningId);
			parcel.writeInt(mContentId);
			parcel.writeString(mContentType.name());
			parcel.writeString(mAudioUrl);
			parcel.writeString(mPron);
			parcel.writeString(mDefinition);
			parcel.writeString(mContent);
			parcel.writeString(mEnDefinition);
		}

		private void readFromParcel(Parcel parcel) {
			mLearningId = parcel.readInt();
			mContentId = parcel.readInt();
			mContentType = ContentType.valueOf(parcel.readString());
			mAudioUrl = parcel.readString();
			mPron = parcel.readString();
			mDefinition = parcel.readString();
			mContent = parcel.readString();
			mEnDefinition = parcel.readString();
		}

		private Word(Parcel parcel) {
			readFromParcel(parcel);
		}
	}

	public static class Examples implements Parcelable {
		int mExamplesStatus;
		Example[] mExamples;

		public Examples(JSONObject json) throws JSONException {
			mExamplesStatus = json.getInt("examples_status");
			if (mExamplesStatus == 1) {
				JSONArray array = json.getJSONArray("examples");
				mExamples = new Example[array.length() > 2 ? array.length() : 2];
				for (int i = 0; i < array.length(); i++) {
					mExamples[i] = new Example(array.getJSONObject(i));
				}
			} else {
				throw new JSONException("example status exception");
			}
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
		}

		public static class Example implements Parcelable {
			int mId;
			String mUsername;
			int mUserid;
			String mFirst;
			String mMid;
			String mLast;
			String mTranslation;
			int mVersion;
			int mLikes;
			int mUnlikes;

			public Example(JSONObject json) throws JSONException {
				mId = json.getInt("id");
				mUsername = json.getString("username");
				mUserid = json.getInt("userid");
				mFirst = json.getString("first");
				mMid = json.getString("mid");
				mLast = json.getString("last");
				mTranslation = json.getString("translation");
				mVersion = json.getInt("version");
				mLikes = json.getInt("likes");
				mUnlikes = json.getInt("unlikes");
			}

			@Override
			public int describeContents() {
				return 0;
			}

			@Override
			public void writeToParcel(Parcel dest, int flags) {
				throw new UnsupportedOperationException();
			}

		}
	}
}

class ShanbayException extends Exception {

	private static final long serialVersionUID = 5056500455997298920L;
	final static String UNDEFINE = "未知错误";
	final static HashMap<Integer, String> EXAMPLE_STATUS = new HashMap<Integer, String>();
	static {
		EXAMPLE_STATUS.put(-1, "指定词汇学习记录实例不存在，或者用户无权查看其内容");
		EXAMPLE_STATUS.put(0, "该单词尚未有例句");
		EXAMPLE_STATUS.put(1, "成功返回例句");
	}
	final static HashMap<Integer, String> NOTE_STATUS = new HashMap<Integer, String>();
	static {
		NOTE_STATUS.put(-1, "指定词汇学习记录实例不存在，或者用户无权查看其内容");
		NOTE_STATUS.put(0, "笔记总长为0");
		NOTE_STATUS.put(1, "笔记添加成功");
		NOTE_STATUS.put(300, "笔记总长超过300个字符");
	}

	public ShanbayException(String info) {
		super(info);
	}
}
