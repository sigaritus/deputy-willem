package info.dourok.dict.provider.shanbay;

import info.dourok.dict.R;
import info.dourok.dict.provider.Provider;
import info.dourok.dict.provider.shanbay.ShanbayDict.Examples;
import info.dourok.dict.provider.shanbay.ShanbayDict.Examples.Example;
import info.dourok.dict.provider.shanbay.ShanbayDict.Word;
import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class ShanbayNoteProvider extends Provider {
	ShanbayProvider mShanbayProvider;
	Word mWordWrapper;
	Examples mExamples;

	public ShanbayNoteProvider(ShanbayProvider shanbayProvider, Context context) {
		super(context);
		mUI = new MyView(context);
		if (shanbayProvider == null) {
			throw new IllegalArgumentException(
					"shanbayProvider can not be null");
		}
		mShanbayProvider = shanbayProvider;

	}

	@Override
	protected void onUpdate() {
		mUI.update(mExamples);
		mUI.show();
	}

	@Override
	protected void onQuery(CharSequence chars) {
		//
		mWordWrapper = mShanbayProvider.mWordWrapper;
		if (mWordWrapper != null) {
			mExamples = mShanbayProvider.mShanbayDict
					.getExample(mWordWrapper.mLearningId);
			System.out.println(mExamples);
		}
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

	class MyView extends CommUIImpl {
		TextView mExample1;
		TextView mExampleZh1;
		TextView mExample2;
		TextView mExampleZh2;
		ViewSwitcher root;

		public MyView(Context context) {
			super(context, R.layout.shanbay_note);
			root = (ViewSwitcher) mContentView;
			mExample1 = (TextView) root.findViewById(R.id.example_1);
			mExample2 = (TextView) root.findViewById(R.id.example_2);
			mExampleZh1 = (TextView) root.findViewById(R.id.example_zh_1);
			mExampleZh2 = (TextView) root.findViewById(R.id.example_zh_2);
		}

		public void update(Examples ex) {
			if (ex != null) {
				Example e1 = ex.mExamples[0];
				Example e2 = ex.mExamples[1];
				if (e1 != null) {
					mExample1.setText(e1.mFirst + e1.mMid + e1.mLast);
					mExampleZh1.setText(e1.mTranslation);
				}
				if (e2 != null) {
					mExample2.setText(e2.mFirst + e2.mMid + e2.mLast);
					mExampleZh2.setText(e2.mTranslation);
				}
			}
		}

		@Override
		public void busy() {
			Log.d("NoteProvider", "busy");
			super.busy();
		}

	}

}
