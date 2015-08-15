package com.smart.purifier;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ab.http.AbFileHttpResponseListener;
import com.ab.http.AbHttpUtil;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

/*
 * 云推送Demo主Activity。
 * 代码中，注释以Push标注开头的，表示接下来的代码块是Push接口调用示例
 */
public class MainActivity extends Activity implements View.OnClickListener {

	private static final String TAG = MainActivity.class.getSimpleName();
	RelativeLayout mainLayout = null;
	Button play = null;
	Button pause = null;
	Button next = null;
	Button pre = null;
	Button all = null;
	Button single = null;
	Button volumeIncrease = null;
	Button volumeReduce = null;
	Button btn_clear_log = null;
	TextView logText = null;
	ScrollView scrollView = null;
	private boolean isLogin = false;

	private AbHttpUtil mAbHttpUtil = null;
	protected boolean isDownload = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.logStringCache = Utils.getLogText(getApplicationContext());
		// 获取Http工具类
		mAbHttpUtil = AbHttpUtil.getInstance(this);
		mAbHttpUtil.setDebug(true);

		Resources resource = this.getResources();
		setContentView(R.layout.main);

		play = (Button) findViewById(R.id.play);
		pause = (Button) findViewById(R.id.pause);
		next = (Button) findViewById(R.id.next);
		pre = (Button) findViewById(R.id.pre);
		all = (Button) findViewById(R.id.all);
		single = (Button) findViewById(R.id.single);
		volumeIncrease = (Button) findViewById(R.id.volumeIncrease);
		volumeReduce = (Button) findViewById(R.id.volumeReduce);
		btn_clear_log = (Button) findViewById(R.id.btn_clear_log);

		logText = (TextView) findViewById(R.id.text_log);
		scrollView = (ScrollView) findViewById(R.id.stroll_text);

		play.setOnClickListener(this);
		pause.setOnClickListener(this);
		next.setOnClickListener(this);
		pre.setOnClickListener(this);
		all.setOnClickListener(this);
		single.setOnClickListener(this);
		volumeIncrease.setOnClickListener(this);
		volumeReduce.setOnClickListener(this);
		btn_clear_log.setOnClickListener(this);

		// Push: 以apikey的方式登录，一般放在主Activity的onCreate中。
		// 这里把apikey存放于manifest文件中，只是一种存放方式，
		// 您可以用自定义常量等其它方式实现，来替换参数中的Utils.getMetaValue(PushDemoActivity.this,
		// "api_key")
		PushManager.startWork(getApplicationContext(),
				PushConstants.LOGIN_TYPE_API_KEY,
				Utils.getMetaValue(MainActivity.this, "api_key"));
		// Push: 如果想基于地理位置推送，可以打开支持地理位置的推送的开关
		// PushManager.enableLbs(getApplicationContext());
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.play) {
			Intent intent = new Intent(this, MusicService.class);
			intent.putExtra("command", "play");
			startService(intent);
		} else if (v.getId() == R.id.pause) {
			Intent intent = new Intent(this, MusicService.class);
			intent.putExtra("command", "pause");
			startService(intent);
		} else if (v.getId() == R.id.next) {
			Intent intent = new Intent(this, MusicService.class);
			intent.putExtra("command", "next");
			startService(intent);
		} else if (v.getId() == R.id.pre) {
			Intent intent = new Intent(this, MusicService.class);
			intent.putExtra("command", "pre");
			startService(intent);
		} else if (v.getId() == R.id.all) {
			// deleteTags();
		} else if (v.getId() == R.id.volumeIncrease) {
			Intent intent = new Intent(this, MusicService.class);
			intent.putExtra("command", "volumeIncrease");
			startService(intent);
			// Utils.logStringCache = "";
			// Utils.setLogText(getApplicationContext(), Utils.logStringCache);
			// updateDisplay();
		} else if (v.getId() == R.id.volumeReduce) {
			Intent intent = new Intent(this, MusicService.class);
			intent.putExtra("command", "volumeReduce");
			startService(intent);
		} else if (v.getId() == R.id.btn_clear_log) {
			Utils.setLogText(getApplicationContext(), "");
			Utils.logStringCache = "";
			updateDisplay();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		updateDisplay();
		checkCommand();
	}

	private void checkCommand() {
		if (Utils.commandCache.length() > 0) {
			String command = Utils.commandCache;
			if (command.equals("download")) {
				String downUrl = Utils.contentCache;
				String musicId = Utils.musicIdCache;
				downloadFile(downUrl, musicId);
			} else {

			}
			Utils.clearHistory();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		String action = intent.getAction();
		updateDisplay();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		Utils.setLogText(getApplicationContext(), Utils.logStringCache);
		super.onDestroy();
	}

	// 更新界面显示内容
	private void updateDisplay() {
		Log.d(TAG, "updateDisplay, logText:" + logText + " cache: "
				+ Utils.logStringCache);
		if (logText != null) {
			logText.setText(Utils.logStringCache);
		}
		if (scrollView != null) {
			scrollView.fullScroll(ScrollView.FOCUS_DOWN);
		}
	}

	private void createSDCardDir() {
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			File sdcardDir = Environment.getExternalStorageDirectory();
			String path = sdcardDir.getPath() + "/myVideos";
			File path1 = new File(path);
			if (!path1.exists()) {// 若不存在，创建目录
				path1.mkdirs();
			}
		} else {
			return;
		}
	}

	/**
	 * 下载并重命名为id.mp3
	 * 
	 * @param url
	 * @param id
	 */
	private void downloadFile(String url, final String id) {
		// String url = "http://192.168.1.100:9000/public/music/兄弟抱一下.mp3";
		mAbHttpUtil.get(url, new AbFileHttpResponseListener(url) {

			// 获取数据成功会调用这里
			@Override
			public void onSuccess(int statusCode, File file) {
				File sdcardDir = Environment.getExternalStorageDirectory();
				String path = sdcardDir.getPath() + "/myVideos/" + id + ".mp3";
				createSDCardDir();
				File outFile = new File(path);
				boolean success = file.renameTo(outFile);
				if(success){
					Toast.makeText(MainActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(MainActivity.this, "本地已存在", Toast.LENGTH_SHORT).show();
				}
			}

			// 开始执行前
			@Override
			public void onStart() {
				Log.d(TAG, "onStart");
				// 开始下载
				isDownload = true;
				Toast.makeText(MainActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
			}

			// 失败，调用
			@Override
			public void onFailure(int statusCode, String content,
					Throwable error) {
				Log.d(TAG, "onFailure");
				isDownload = false;
				Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
			}

			// 下载进度
			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				// maxText.setText(bytesWritten / (totalSize / max) + "/" +
				// max);
				// mAbProgressBar.setProgress(bytesWritten / (totalSize / max));
			}

			// 完成后调用，失败，成功
			public void onFinish() {
				// 下载完成取消进度框
				isDownload = false;
				Log.d(TAG, "onFinish");
			};

		});
	}

}
