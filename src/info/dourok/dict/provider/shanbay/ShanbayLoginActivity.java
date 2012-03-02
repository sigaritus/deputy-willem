package info.dourok.dict.provider.shanbay;

import info.dourok.dict.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ShanbayLoginActivity extends Activity {
	EditText mUsername;
	EditText mPassword;
	String username;
	String password;
	static ShanbayProvider sShanbayProvider;
	ProgressDialog dialog ;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (sShanbayProvider == null) {
			finish();
			return;
		}
		sShanbayProvider.mShanbayLoginActivity = this;
		setContentView(R.layout.shanbay_login);
		mUsername = (EditText) findViewById(R.id.username);
		mPassword = (EditText) findViewById(R.id.password);
		dialog = new ProgressDialog(this);
		dialog.setCancelable(false);
		dialog.setMessage("Please wait...");
	}

	public void onClick(View v) {
		dialog.show();
		username = mUsername.getText().toString().trim();
		password = mPassword.getText().toString().trim();
		sShanbayProvider.sendLogin(username, password);
		
	}

	@Override
	public void finish() {
		//显性释放引用防止内存泄漏
		sShanbayProvider.mShanbayLoginActivity = null;
		sShanbayProvider = null; 
		super.finish();
	}
	
	void onResultReceive(boolean result) {
		dialog.dismiss();
		if (result) {
			Toast.makeText(this, "success", 3000).show();
			finish();
		} else {
			Toast.makeText(this, "failed", 3000).show();
		}

	}
}
