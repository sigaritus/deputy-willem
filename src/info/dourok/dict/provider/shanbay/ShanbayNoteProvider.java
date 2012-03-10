package info.dourok.dict.provider.shanbay;

import android.content.Context;
import android.os.Message;
import android.util.Log;
import info.dourok.dict.R;
import info.dourok.dict.provider.Provider;
import info.dourok.dict.provider.shanbay.ShanbayDict.Word;

public class ShanbayNoteProvider extends Provider {
	ShanbayProvider mShanbayProvider;
	Word mWordWrapper;
	
	public ShanbayNoteProvider(ShanbayProvider shanbayProvider,Context context) {
		super(context);
		mUI = new MyView(context);
		if(shanbayProvider==null){
			throw new IllegalArgumentException("shanbayProvider can not be null");
		}
		mShanbayProvider = shanbayProvider;
		
	}

	@Override
	protected void onUpdate() {

		mUI.show();
	}

	@Override
	protected void onQuery(CharSequence chars) {
		//
		
		mWordWrapper = mShanbayProvider.mWordWrapper;
		
	}

	@Override
	protected void onHandleMessage(Message msg, int newWhat) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean filter(CharSequence chars) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public UI getUI() {
		// TODO Auto-generated method stub
		return mUI;
	}
	
	MyView mUI;
	class MyView extends CommUIImpl{

		public MyView(Context context) {
			super(context,R.layout.shanbay_note);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void busy() {
			Log.d("NoteProvider","busy");
			super.busy();
		}
		
		
	}

}
