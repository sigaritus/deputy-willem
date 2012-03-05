package info.dourok.dict.provider.shanbay;

import android.content.Context;
import android.os.Message;
import info.dourok.dict.provider.Provider;
import info.dourok.dict.provider.shanbay.ShanbayDict.Word;

public class ShanbayNoteProvider extends Provider {
	ShanbayProvider mShanbayProvider;
	Word mWordWrapper;
	
	public ShanbayNoteProvider(ShanbayProvider shanbayProvider,Context context) {
		super(context);
		if(shanbayProvider==null){
			throw new IllegalArgumentException("shanbayProvider is null");
		}
		mShanbayProvider = shanbayProvider;
		
	}

	@Override
	protected void onUpdate() {
		// TODO Auto-generated method stub

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
		return false;
	}

	@Override
	public UI getUI() {
		// TODO Auto-generated method stub
		return null;
	}

}
