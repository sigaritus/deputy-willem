package info.dourok.dict;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class MiscUtils {
	/**
	 * 是否是 ' '(空格) 到 '~' 之间的字符
	 * 
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

	public static Bitmap toNotificationIcon(Context context, int id) {
		Drawable d = context.getResources().getDrawable(id);
		Bitmap bm = ((BitmapDrawable)d).getBitmap();
		return toNotificationIcon(context, bm);
	}
	public static Bitmap toNotificationIcon(Context context, Bitmap bm) {
		if (bm == null)
			throw new IllegalArgumentException("bm:" + bm);

		int nWidth = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 24f, context.getResources()
						.getDisplayMetrics());
		int nHeight = nWidth;
		float scalex = (float) nWidth / bm.getWidth();
		float scaley = (float) nHeight / bm.getHeight();

		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		Bitmap nbg = Bitmap.createBitmap(nWidth, nHeight, config);
		Canvas c = new Canvas(nbg);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
//		paint.setFilterBitmap(true);
		Matrix matrix = new Matrix();
		matrix.postScale(scalex, scaley);
		c.drawBitmap(bm, matrix, paint);
		return bm;
	}
}
