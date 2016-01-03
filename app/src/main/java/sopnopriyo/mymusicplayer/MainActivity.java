package sopnopriyo.mymusicplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import sopnopriyo.mymusicplayer.fragment.CurrentMusicList;
import sopnopriyo.mymusicplayer.fragment.MusicInfo;
import sopnopriyo.mymusicplayer.service.MusicService;
import sopnopriyo.mymusicplayer.util.CodeUtil;
import sopnopriyo.mymusicplayer.util.FileUtil;
import sopnopriyo.mymusicplayer.util.ThreadUtil;

public class MainActivity extends FragmentActivity {
    //Music Player Service
    private static MusicService.MusicBinder musicBinder;
    //BroadcastReceiver module that receives change playing a song
    private BroadcastReceiver musicChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CodeUtil.MUSIC_CHANGE_ACTION)) {
                musicInfo.showMusicInfo();
                currentMusicList.showCurrentMusicList();
            }
        }
    };
    //Controls
    private ImageView imageViewSearch;
    private TextView textViewMusicList;
    private TextView textViewPlayList;
    private ImageView imageViewMenu;
    private ViewPager viewPagerMusicInfo;
    //Music playback controls
    private ImageView imageViewPlayMusic;
    private ImageView imageViewPreviousMusic;
    private ImageView imageViewNextMusic;
    private ImageView imageViewMusicPlayMode;
    private ImageView imageViewMusicControlFavorite;
    private SeekBar seekBarMusic;
    private TextView textViewPlayTimeNow;
    private TextView textViewPlayTimeTotal;
    //Fragment Module
    private ArrayList<Fragment> fragmentList;
    private MusicInfo musicInfo;
    private CurrentMusicList currentMusicList;
    private ServiceConnection sc;
    private boolean isBind = false;

    public static MusicService.MusicBinder getMusicBinder() {
        return musicBinder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Register BroadcastReceiver
        registerReceiver(musicChangeReceiver, new IntentFilter(CodeUtil.MUSIC_CHANGE_ACTION));
        //Bound control
        imageViewSearch = (ImageView) findViewById(R.id.imageViewSearch);
        textViewMusicList = (TextView) findViewById(R.id.textViewMusicList);
        textViewPlayList = (TextView) findViewById(R.id.textViewPlayList);
        imageViewMenu = (ImageView) findViewById(R.id.imageViewMenu);
        viewPagerMusicInfo = (ViewPager) findViewById(R.id.viewPagerMusicInfo);
        //Bind music playback controls
        imageViewPlayMusic = (ImageView) findViewById(R.id.imageViewPlayMusic);
        imageViewPreviousMusic = (ImageView) findViewById(R.id.imageViewPreviousMusic);
        imageViewNextMusic = (ImageView) findViewById(R.id.imageViewNextMusic);
        imageViewMusicPlayMode = (ImageView) findViewById(R.id.imageViewMusicPlayMode);
        imageViewMusicControlFavorite = (ImageView) findViewById(R.id.imageViewMusicControlFavorite);
        seekBarMusic = (SeekBar) findViewById(R.id.seekBarMusic);
        textViewPlayTimeNow = (TextView) findViewById(R.id.textViewPlayTimeNow);
        textViewPlayTimeTotal = (TextView) findViewById(R.id.textViewPlayTimeTotal);
        //Bind Listeners
        OnClickEvent onClickEvent = new OnClickEvent();
        imageViewSearch.setOnClickListener(onClickEvent);
        textViewMusicList.setOnClickListener(onClickEvent);
        textViewPlayList.setOnClickListener(onClickEvent);
        imageViewMenu.setOnClickListener(onClickEvent);
        imageViewMusicPlayMode.setOnClickListener(onClickEvent);
        //Configuration ViewPager
        initFragmentList();
        viewPagerMusicInfo.setAdapter(new MusicInfoViewPagerAdapter(getSupportFragmentManager()));

        //Binding music service
        sc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                musicBinder = (MusicService.MusicBinder) service;
                isBind = true;
                //The control is passed to the Music Binder
                musicBinder.setView(imageViewPlayMusic, imageViewPreviousMusic, imageViewNextMusic, imageViewMusicPlayMode, imageViewMusicControlFavorite, seekBarMusic, textViewPlayTimeNow, textViewPlayTimeTotal);
                //Analyzing MusicService is running
                if (MusicService.isRunning) {
                    //Refresh control state / display
                    musicBinder.refreshView();
                } else {
                    MusicService.isRunning = true;
                    //Remember when exiting determine whether the song playing
                    if (getSharedPreferences("settings", 0).getBoolean(CodeUtil.EXIT_MUSIC_STATUS, false)) {
                        //getExitMusicStatus () method contains refreshView () method
                        musicBinder.getExitMusicStatus();
                    } else {
                       // Refresh control state / display
                        musicBinder.refreshView();
                    }
                }
                //Initialize the timer
                Timer mTimer = new Timer();
                TimerTask mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (MusicService.isPlaying) {
                            final int currentPosition = musicBinder.getMPCurrentPosition();
                            if (!MusicService.isSeekBarChanging) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        seekBarMusic.setProgress(currentPosition);
                                    }
                                });
                            }
                        }
                    }
                };
                mTimer.schedule(mTimerTask, 0, 500);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        if (!isBind) {
            startService(new Intent(MainActivity.this, MusicService.class));
            bindService(new Intent(MainActivity.this, MusicService.class), sc, Context.BIND_AUTO_CREATE);
        }

        //Get Starts
        new Runnable() {
            @Override
            public void run() {
                SharedPreferences spData = getSharedPreferences("data", 0);
                int startCount = spData.getInt("startCount", 0);
                if (startCount == 0) {
                    //Modify the number of starts
                    SharedPreferences.Editor editor = spData.edit();
                    editor.putInt("startCount", 1);
                    editor.commit();
                    //Read all music files and information stored in the database
                    new ThreadUtil.DatabaseAsyncTask(MainActivity.this, true).execute(FileUtil.getRootPath());
                }
            }
        }.run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewPagerMusicInfo.setCurrentItem(1);
    }

    @Override
    protected void onDestroy() {
        //Unbundling BroadcastReceiver
        unregisterReceiver(musicChangeReceiver);
        //Unbundling Music Player service
        unbindService(sc);
        super.onDestroy();
    }

    private void initFragmentList() {
        fragmentList = new ArrayList<Fragment>();
        currentMusicList = new CurrentMusicList();
        musicInfo = new MusicInfo();
        fragmentList.add(currentMusicList);
        fragmentList.add(musicInfo);
    }

    //Click event listeners
    private class OnClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == imageViewSearch) {
                new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, Search.class));
                    }
                }.run();
            } else if (v == textViewMusicList) {
                new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, MusicSort.class));
                    }
                }.run();
            } else if (v == textViewPlayList) {
                new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, PlayList.class));
                    }
                }.run();
            } else if (v == imageViewMenu) {
                new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, Settings.class));
                    }
                }.run();
            }
        }
    }

    //ViewPagerAdapter
    private class MusicInfoViewPagerAdapter extends FragmentPagerAdapter {
        public MusicInfoViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }
    }
}
