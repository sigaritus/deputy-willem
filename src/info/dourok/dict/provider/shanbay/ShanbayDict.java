/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package info.dourok.dict.provider.shanbay;

import info.dourok.dict.AppCookieStore;
import java.io.IOException;
import java.util.ArrayList;
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
	private final static String USER_AGENT = "Mozilla/5.0 (X11; U; Linux "
			+ "i686; en-US; rv:1.8.1.6) Gecko/20061201 Firefox/2.0.0.6 (Ubuntu-feisty)";

	/**
	 * @deprecated
	 */
	private String mUsername;
	/**
	 * @deprecated
	 */
	private String mPsw;

	private AndroidHttpClient httpClient;
	private HttpContext mLocalContext; // 线程安全未验证,只能通过一个worker工作
	private ShanbayProvider mShanbayProvider;

	/**
	 * @deprecated
	 * @param shanbayProvider
	 * @param name
	 * @param psw
	 */
	public ShanbayDict(ShanbayProvider shanbayProvider, String name, String psw) {
		this.mShanbayProvider = shanbayProvider;
		this.mUsername = name;
		this.mPsw = psw;
		mLocalContext = new BasicHttpContext();
		mLocalContext.setAttribute(ClientContext.COOKIE_STORE,
				AppCookieStore.getInstance());

	}

	public ShanbayDict(ShanbayProvider shanbayProvider) {
		this.mShanbayProvider = shanbayProvider;
		mLocalContext = new BasicHttpContext();
		mLocalContext.setAttribute(ClientContext.COOKIE_STORE,
				AppCookieStore.getInstance());
	}

	private HttpClient getClient() {
		if (httpClient == null) {
			httpClient = AndroidHttpClient.newInstance(USER_AGENT);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getHome.abort();
		return false;
	}

	boolean login() {
		String psw = mShanbayProvider.pswRequest();
		if (psw == null) {
			return false;
		}
		return login(psw);
	}

	boolean login(String psw) {
		String usr = mShanbayProvider.mUsername;

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
		// 告诉Provider 登录失败,原先保存的密码失效.
		mShanbayProvider.needLogin();
		return false;
	}

	/**
	 * @deprecated
	 * @param usr
	 * @param psw
	 * @return
	 */
	boolean login(String usr, String psw) {
		// System.out.println("login");
		// String query =
		// "username=%u&next=&continue=home&u=1&csrfmiddlewaretoken=%c&password=%p&login=%B5%C7%C2%BC";

		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair("username", usr));
		// qparams.add(new BasicNameValuePair("next", ""));
		// qparams.add(new BasicNameValuePair("continue", "home"));
		//
		// qparams.add(new BasicNameValuePair("u", "1"));
		// qparams.add(new BasicNameValuePair("login", ""/* "登录" */));

		qparams.add(new BasicNameValuePair("password", psw));

		// String csrftoken = "";
		// HttpGet getLogin = new HttpGet(LOING_URL);
		HttpResponse re;
		try {
			// re = getClient().execute(getLogin, mLocalContext);
			//
			// Header[] hs = re.getHeaders("Set-Cookie");
			// for (Header h : hs) {
			// String s = h.getValue();
			// System.out.println(h.getName() + ": " + h.getValue());
			// if (s.indexOf("csrftoken") != -1) {
			// csrftoken = s.substring(s.indexOf('=') + 1, s.indexOf(';'));
			// break;
			// }
			// }
			// getLogin.abort();
			// qparams.add(new BasicNameValuePair("csrfmiddlewaretoken",
			// csrftoken));
			// login
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
						mUsername = usr;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		// Log.d("query url",QUERY_URL + word);
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

	public void addNote(Word word, String note) {
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
		}
	}

	private void addNote(Word word, String note, boolean recall)
			throws IOException {
		note = note.trim();
		HttpGet getQuery = new HttpGet(API_QUERY.replace("{{learning_id}}",
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
		}
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
			mLearningId = json.getInt("learning_id");
			Object tmp = json.get("voc");
			if (tmp instanceof JSONObject) {
				JSONObject voc = (JSONObject) tmp;
				// mContentId = voc.getInt("content_id");
				mContent = voc.getString("content");
				mDefinition = voc.getString("definition");
				JSONObject en = voc.getJSONObject("en_definitions");
				Iterator<?> itr = en.keys();
				System.out.println(en);

				while (itr.hasNext()) {
					String k = (String) itr.next();
					mEnDefinition = k + " : ";
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
				mExamples = new Example[array.length()];
				for (int i = 0; i < array.length(); i++) {
					mExamples[i] = new Example(array.getJSONObject(i));
				}
			} else {
				throw new JSONException("example status exception");
			}
		}

		@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			// TODO NOTSUPPORTEDYET
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
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void writeToParcel(Parcel dest, int flags) {
				// TODO Auto-generated method stub

			}

		}
	}
}
