/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package info.dourok.dict;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;

/**
 *
 * @author DouO
 */
public class AppCookieStore extends BasicCookieStore {

    private static AppCookieStore singleton;
    private static final String FILENAME = "cookies.txt";
    private AppCookieStore() {
    }

    
	@SuppressWarnings("unchecked")
	public static void load(Context c) {

        if (singleton == null) {
            singleton = new AppCookieStore();
        } else {
            singleton.clear();
        }
        System.out.println("load");
        List<Cookie> list = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(c.getExternalFilesDir(null),
                    FILENAME)));
            
            // type safe
            list = (List<Cookie>) ois.readObject();
            ois.close();
            
//            for(Cookie cc: list){
//                System.out.println(cc.getName()+":"+cc.getValue());
//            }

        } catch (Exception ex) {
            Log.d("AppCookieStore", "Load Cookies file fail",ex);
        }
        if (list != null) {
            int n = list.size();
            Cookie[] cs = new Cookie[n];
            singleton.addCookies(list.toArray(cs));
        }
    }

    public static void save(Context context) {
        try {
            System.out.println("save");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(context.getExternalFilesDir(null),
                    FILENAME)));
            List<Cookie> list =singleton.getCookies();
            if(list!=null){
                List<SerializbleCookie> nlist = new ArrayList<SerializbleCookie>(list.size());
                for(Cookie c:list){
                    if(c instanceof SerializbleCookie){
                       nlist.add((SerializbleCookie)c);
                    }else{
                        nlist.add(new SerializbleCookie(c));
                    }
                }
                oos.writeObject(nlist);
                oos.close();
//                for(Cookie cc: list){
//                System.out.println(cc.getName()+":"+cc.getValue());
//                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AppCookieStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static AppCookieStore getInstance() {
        return singleton;
    }

    final static class SerializbleCookie implements Serializable, Cookie {

        /**
		 * 
		 */
		private static final long serialVersionUID = 3785554939446835096L;
		
		transient Cookie cookie;

        public SerializbleCookie(Cookie cookie) {
            this.cookie = cookie;
            this.name = cookie.getName();
            this.value = cookie.getValue();
            this.comment = cookie.getComment();
            this.commentURL = cookie.getCommentURL();
            this.expiryDate = cookie.getExpiryDate();
            this.persistent = cookie.isPersistent();
            this.domain = cookie.getDomain();
            this.path = cookie.getPath();
            this.ports = cookie.getPorts();
            this.secure = cookie.isSecure();
            this.version = cookie.getVersion();
        }
        private String name;
        private String value;
        private String comment;
        private String commentURL;
        private Date expiryDate;
        private boolean persistent;
        private String domain;
        private String path;
        private int[] ports;
        private boolean secure;
        private int version;

        @Override
		public String getComment() {
            return comment;
        }

        @Override
		public String getCommentURL() {
            return commentURL;
        }

        public Cookie getCookie() {
            return cookie;
        }

        @Override
		public String getDomain() {
            return domain;
        }

        @Override
		public Date getExpiryDate() {
            return expiryDate;
        }

        @Override
		public String getName() {
            return name;
        }

        @Override
		public String getPath() {
            return path;
        }

        @Override
		public boolean isPersistent() {
            return persistent;
        }

        @Override
		public int[] getPorts() {
            return ports;
        }

        @Override
		public boolean isSecure() {
            return secure;
        }

        @Override
		public String getValue() {
            return value;
        }

        @Override
		public int getVersion() {
            return version;
        }

        @Override
		public boolean isExpired(Date date) {
            if (cookie != null) {
                return cookie.isExpired(date);
            }
            return false;
        }
    }
}
