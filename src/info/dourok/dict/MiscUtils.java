package info.dourok.dict;

public class MiscUtils {
	/**
	 * 是否是 ' '(空格) 到 '~' 之间的字符
	 * @param s
	 * @return
	 */
	public static boolean isAcsii(CharSequence s) {
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) > '~' || s.charAt(i) < ' ')
				return false;
		}
		return true;
	}
}
