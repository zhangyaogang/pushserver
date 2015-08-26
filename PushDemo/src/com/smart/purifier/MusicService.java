package com.smart.purifier;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

public class MusicService extends Service {
	private MediaPlayer player;
	private AudioManager audioManager = null; // 音频
	private String command;
	private static File MUSIC_PATH = null;// 找到music存放的路径。
	public List<String> musicList;// 存放找到的所有mp3的绝对路径
	public int songNum; // 当前播放的歌曲在List中的下标
	public String songName; // 当前播放的歌曲名
	public Long appointedMusicId = 0L;//指定音乐Id

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (player == null) {
			player = new MediaPlayer();
		}
		audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
		musicList = new ArrayList<String>();
		File sdcardDir = Environment.getExternalStorageDirectory();
		String path = sdcardDir.getPath() + "/myVideos";
		createSDCardDir();
		MUSIC_PATH = new File(path);
		if (MUSIC_PATH.listFiles(new MusicFilter()).length > 0) {
			for (File file : MUSIC_PATH.listFiles(new MusicFilter())) {
				musicList.add(file.getAbsolutePath());
			}
		}
		if (musicList.size() <= 0) {
			Toast.makeText(MusicService.this, "无音乐", Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			player.reset(); // 重置多媒体
			String dataSource = musicList.get(songNum);// 得到当前播放音乐的路径
			setPlayName(dataSource);// 截取歌名
			player.setDataSource(dataSource);
			player.prepare();// 准备播放
			// 当当前多媒体对象播放完成时发生的事件
			player.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer arg0) {
					next();// 如果当前歌曲播放完毕,自动播放下一首.
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 过滤文件格式mp3
	 * 
	 * @author zygwin8
	 * 
	 */
	class MusicFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".mp3"));// 返回当前目录所有以.mp3结尾的文件
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
	 * 设置播放文件名
	 * 
	 * @param dataSource
	 */
	public void setPlayName(String dataSource) {
		File file = new File(dataSource);// 假设为D:\\mm.mp3
		String name = file.getName();// name=mm.mp3
		int index = name.lastIndexOf(".");// 找到最后一个.
		songName = name.substring(0, index);// 截取为mm
	}
	
	/**
	 * 播放指定musicId的音乐
	 * @param appointedMusicId
	 */
	private void playAppointedMusic(Long musicId){
		File sdcardDir = Environment.getExternalStorageDirectory();
		String path = sdcardDir.getPath() + "/myVideos";
		String filePah = path+"/"+musicId+".mp3";
		for(int i=0;i<musicList.size();i++){
			if(filePah.equals(musicList.get(i))){
				songNum = i;
			}
		}
		start();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		try {
			command = intent.getStringExtra("command");
	
			if (command.equals("play")) {
				start();
			} else if (command.equals("pause")) {
				pause();
			} else if (command.equals("next")) {
				next();
			} else if (command.equals("pre")) {
				pre();
			} else if (command.equals("volumeIncrease")) {
				audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); // 调高声音
			} else if (command.equals("volumeReduce")) {
				// 第一个参数：声音类型,第二个参数：调整音量的方向,第三个参数：可选的标志位
				audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);// 调低声音
			}else if(command.equals("playAppointed")){//播放指定音乐
				appointedMusicId = intent.getLongExtra("musicId", 0L);
				playAppointedMusic(appointedMusicId);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		player.stop();
	}

	public void next() {
		songNum = songNum == musicList.size() - 1 ? 0 : songNum + 1;
		start();
	}

	public void pre() {
		songNum = songNum == 0 ? musicList.size() - 1 : songNum - 1;
		start();
	}

	private void start() {
		if (musicList.size() <= 0) {
			Toast.makeText(MusicService.this, "无音乐", Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			player.reset(); // 重置多媒体
			String dataSource = musicList.get(songNum);// 得到当前播放音乐的路径
			setPlayName(dataSource);// 截取歌名
			player.setDataSource(dataSource);
			player.prepare();// 准备播放
			player.start();// 开始播放
			// 当当前多媒体对象播放完成时发生的事件
			player.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer arg0) {
					next();// 如果当前歌曲播放完毕,自动播放下一首.
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void last() {
		songNum = songNum == 0 ? musicList.size() - 1 : songNum - 1;
		start();
	}

	public void pause() {
		if (player.isPlaying())
			player.pause();
		else
			player.start();
	}

	public void stop() {
		if (player.isPlaying()) {
			player.stop();
		}
	}

}
