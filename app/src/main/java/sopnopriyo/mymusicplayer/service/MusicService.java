package sopnopriyo.mymusicplayer.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import sopnopriyo.mymusicplayer.R;
import sopnopriyo.mymusicplayer.util.CodeUtil;
import sopnopriyo.mymusicplayer.util.DBUtil;
import sopnopriyo.mymusicplayer.util.TimeUitl;
import sopnopriyo.mymusicplayer.util.ToastUtil;

/**
 * Created by sopnopriyo on 2014/5/4.
 */
public class MusicService extends Service {
    //State quantity
    public static String title;
    public static String album;
    public static String artist;
    public static boolean isFileLoaded = false;
    public static boolean isPlaying = false;
    //Receiving call status changes BroadcastReceiver
    private BroadcastReceiver phoneStatusChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.PHONE_STATE")) {
                if (isFileLoaded) {
                    if (isPlaying) {
                        pauseMusic();
                    }
                    TelephonyManager tm = (TelephonyManager) MusicService.this.getSystemService(Service.TELEPHONY_SERVICE);
                    tm.listen(new PhoneStateListener() {
                        @Override
                        public void onCallStateChanged(int state, String incomingNumber) {
                            super.onCallStateChanged(state, incomingNumber);
                            switch (state) {
                                case TelephonyManager.CALL_STATE_IDLE:
                                    if (!isPlaying) {
                                        startMusic();
                                    }
                                    break;
                            }
                        }
                    }, PhoneStateListener.LISTEN_CALL_STATE);
                }
            }
        }
    };
    public static boolean isRunning = false;
    public static boolean isSeekBarChanging = false;
    public static int position;
    public static String sql = "";
    private boolean isFavorite;
    //Music Player control module
    private ImageView imageViewPlayMusic;
    private ImageView imageViewPreviousMusic;
    private ImageView imageViewNextMusic;
    private ImageView imageViewMusicPlayMode;
    private ImageView imageViewMusicControlFavorite;
    private SeekBar seekBarMusic;
    private TextView textViewPlayTimeTotal;
    //Music playback component
    private MediaPlayer mp;
    private MusicBinder musicBinder;
    private int playModeCurrent = 0;
    //List of songs being played
    private ArrayList<HashMap<String, String>> musicList = null;
    //Playing order of picture
    private int[] playModeImgRes = {
            R.mipmap.playmode_default,
            R.mipmap.playmode_list_repeat,
            R.mipmap.playmode_single_repeat,
            R.mipmap.playmode_random};
    //Playing order mode name
    private String[] playModeTitle = {
            "Order of play",
            "List cycle",
            "Single cycle",
            "Shuffle Playback"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        musicBinder = new MusicBinder();
        mp = new MediaPlayer();
        //Sign up to receive the call change in state Receiver
        registerReceiver(phoneStatusChangedReceiver, new IntentFilter("android.intent.action.PHONE_STATE"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBinder;
    }

    private void pauseMusic() {
        new Runnable() {
            @Override
            public void run() {
                mp.pause();
                isPlaying = false;
                imageViewPlayMusic.setImageResource(R.mipmap.music_control_play);
            }
        }.run();
    }

    private void startMusic() {
        new Runnable() {
            @Override
            public void run() {
                mp.seekTo(seekBarMusic.getProgress());
                mp.start();
                isPlaying = true;
                imageViewPlayMusic.setImageResource(R.mipmap.music_control_pause);
            }
        }.run();
    }

    private void playMusic(final String fileAbsolutePath) {
        try {
            //Determine whether the record player information
            if (getSharedPreferences("settings", 0).getBoolean(CodeUtil.EXIT_MUSIC_STATUS, false)) {
                SharedPreferences.Editor editor = getSharedPreferences("data", 0).edit();
                editor.putString("sql", sql);
                editor.putInt("position", position);
                editor.commit();
            }
            mp.reset();
            mp = MediaPlayer.create(MusicService.this, Uri.parse(fileAbsolutePath));
            isFileLoaded = true;
            mp.start();
            isPlaying = true;
            title = musicList.get(position).get("title");
            album = musicList.get(position).get("album");
            artist = musicList.get(position).get("artist");
            musicBinder.refreshView();
            sendBroadcast(new Intent(CodeUtil.MUSIC_CHANGE_ACTION));
            //Play action after completion
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isFileLoaded = false;
                    isPlaying = false;
                    switch (playModeCurrent) {
                        case 0://Order of play
                            if (position == musicList.size() - 1) {
                                mp.release();
                                ToastUtil.showMessage(MusicService.this, "It has finished playing the current listing");
                            } else {
                                playMusic(musicList.get(++position).get("path"));
                            }
                            break;
                        case 1://List cycle
                            if (position == musicList.size() - 1) {
                                position = 0;
                                playMusic(musicList.get(position).get("path"));
                            } else {
                                playMusic(musicList.get(++position).get("path"));
                            }
                            break;
                        case 2://Single cycle
                            playMusic(musicList.get(position).get("path"));
                            break;
                        case 3://Shuffle Playback
                            position = (int) (Math.random() * musicList.size());
                            playMusic(musicList.get(position).get("path"));
                            break;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtil.showMessage(MusicService.this, "Broadcast " + fileAbsolutePath + " An error occurred");
            mp = new MediaPlayer();
            imageViewNextMusic.performClick();
        }
    }

    private void playMusicList(ArrayList<HashMap<String, String>> musicList, int position, String sql) {
        this.musicList = musicList;
        MusicService.position = position;
        MusicService.sql = sql;
        playMusic(musicList.get(position).get("path"));
    }

    private void playMusicList(int position) {
        MusicService.position = position;
        playMusic(musicList.get(position).get("path"));
    }

    //Button Click event listeners
    class OnClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == imageViewPlayMusic) {
                if (isFileLoaded) {
                    if (isPlaying) {
                        pauseMusic();
                    } else {
                        startMusic();
                    }
                } else {
                    new Runnable() {
                        @Override
                        public void run() {
                            String sql = "select path,title,pinyin,album,artist from " + DBUtil.T_MusicFile_Name + " order by pinyin";
                            musicList = CodeUtil.getMusicList(MusicService.this, sql, null);
                            if (musicList.size() > 0) {
                                playMusicList(musicList, (int) (Math.random() * musicList.size()), sql);
                            } else {
                                ToastUtil.showMessage(MusicService.this, "Not retrieve any song");
                            }
                        }
                    }.run();
                }
            } else if (v == imageViewPreviousMusic) {
                if (isFileLoaded) {
                    switch (playModeCurrent) {
                        case 0://Order of play
                            if (position == 0) {
                                ToastUtil.showMessage(MusicService.this, "Head has to Playlist");
                            } else {
                                playMusic(musicList.get(--position).get("path"));
                            }
                            break;
                        case 1://List cycle
                        case 2://Single cycle
                            if (position == 0) {
                                position = musicList.size() - 1;
                                playMusic(musicList.get(position).get("path"));
                            } else {
                                playMusic(musicList.get(--position).get("path"));
                            }
                            break;
                        case 3://Shuffle Playback
                            position = (int) (Math.random() * musicList.size());
                            playMusic(musicList.get(position).get("path"));
                            break;
                    }
                } else {
                    ToastUtil.showMessage(MusicService.this, "Current playlist does not have any tracks");
                }
            } else if (v == imageViewNextMusic) {
                if (isFileLoaded) {
                    switch (playModeCurrent) {
                        case 0://Order of play
                            if (position == musicList.size() - 1) {
                                ToastUtil.showMessage(MusicService.this, "Tail has to Playlist");
                            } else {
                                playMusic(musicList.get(++position).get("path"));
                            }
                            break;
                        case 1://List cycle
                        case 2://Single cycle
                            if (position == musicList.size() - 1) {
                                position = 0;
                                playMusic(musicList.get(position).get("path"));
                            } else {
                                playMusic(musicList.get(++position).get("path"));
                            }
                            break;
                        case 3://Shuffle Playback
                            position = (int) (Math.random() * musicList.size());
                            playMusic(musicList.get(position).get("path"));
                            break;
                    }
                } else {
                    ToastUtil.showMessage(MusicService.this, "Current playlist does not have any tracks");
                }
            } else if (v == imageViewMusicPlayMode) {
                playModeCurrent = playModeCurrent == 3 ? 0 : ++playModeCurrent;
                imageViewMusicPlayMode.setImageResource(playModeImgRes[playModeCurrent]);
                ToastUtil.showMessage(MusicService.this, playModeTitle[playModeCurrent]);
            } else if (v == imageViewMusicControlFavorite) {
                if (isFileLoaded) {
                    if (isFavorite) {
                        DBUtil.execSqlDatabase(MusicService.this, DBUtil.databaseName, "update " + DBUtil.T_MusicFile_Name + " set favorite=0 where path='" + musicList.get(position).get("path").replace("'", "''") + "'");
                        isFavorite = false;
                        imageViewMusicControlFavorite.setImageResource(R.mipmap.music_unlove);
                        ToastUtil.showMessage(MusicService.this, "Remove from my favorite in");
                    } else {
                        DBUtil.execSqlDatabase(MusicService.this, DBUtil.databaseName, "update " + DBUtil.T_MusicFile_Name + " set favorite=1 where path='" + musicList.get(position).get("path").replace("'", "''") + "'");
                        isFavorite = true;
                        imageViewMusicControlFavorite.setImageResource(R.mipmap.music_love);
                        ToastUtil.showMessage(MusicService.this, "Add to my favorite");
                    }
                }
            }
        }
    }

    public class MusicBinder extends Binder {
        public void playMusicList(ArrayList<HashMap<String, String>> musicList, int position, String sql) {
            MusicService.this.playMusicList(musicList, position, sql);
        }

        public void playMusicList(int position) {
            MusicService.this.playMusicList(position);
        }

        //Returns current playlist
        public ArrayList<HashMap<String, String>> getCurrentMusicList() {
            return musicList;
        }

        //Get playback information when exit
        public void getExitMusicStatus() {
            SharedPreferences sp = getSharedPreferences("data", 0);
            MusicService.sql = sp.getString("sql", "");
            MusicService.position = sp.getInt("position", 0);
            musicList = CodeUtil.getMusicList(MusicService.this, sql, null);
            if (musicList.size() > 0) {
                mp.reset();
                mp = MediaPlayer.create(MusicService.this, Uri.parse(musicList.get(position).get("path")));
                isFileLoaded = true;
                title = musicList.get(position).get("title");
                album = musicList.get(position).get("album");
                artist = musicList.get(position).get("artist");
                refreshView();
                sendBroadcast(new Intent(CodeUtil.MUSIC_CHANGE_ACTION));
                //Play action after completion
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        isFileLoaded = false;
                        isPlaying = false;
                        switch (playModeCurrent) {
                            case 0://Order of play
                                if (position == musicList.size() - 1) {
                                    mp.release();
                                    ToastUtil.showMessage(MusicService.this, "Has finished playing the current listing");
                                } else {
                                    playMusic(musicList.get(++position).get("path"));
                                }
                                break;
                            case 1://List cycle
                                if (position == musicList.size() - 1) {
                                    position = 0;
                                    playMusic(musicList.get(position).get("path"));
                                } else {
                                    playMusic(musicList.get(++position).get("path"));
                                }
                                break;
                            case 2://Single cycle
                                playMusic(musicList.get(position).get("path"));
                                break;
                            case 3://Shuffle Playback
                                position = (int) (Math.random() * musicList.size());
                                playMusic(musicList.get(position).get("path"));
                                break;
                        }
                    }
                });
            }
        }

        //Refresh player interface controls
        public void refreshView() {
            imageViewMusicPlayMode.setImageResource(playModeImgRes[playModeCurrent]);
            if (isFileLoaded) {
                int duration = mp.getDuration();
                seekBarMusic.setMax(duration);
                seekBarMusic.setProgress(mp.getCurrentPosition());
                textViewPlayTimeTotal.setText(TimeUitl.changeMillsToDateTime(duration));
                if (isPlaying) {
                    imageViewPlayMusic.setImageResource(R.mipmap.music_control_pause);
                } else {
                    imageViewPlayMusic.setImageResource(R.mipmap.music_control_play);
                }
                //Gets whether the song is set to love
                SQLiteDatabase db = null;
                Cursor cursor = null;
                try {
                    db = DBUtil.getReadableDB(MusicService.this, DBUtil.databaseName);
                    cursor = DBUtil.rawQueryCursor(db, "select favorite from " + DBUtil.T_MusicFile_Name + " where path='" + musicList.get(position).get("path").replace("'", "''") + "'", null);
                    if (cursor.moveToNext()) {
                        if (cursor.getString(cursor.getColumnIndex("favorite")).equals("1")) {
                            isFavorite = true;
                            imageViewMusicControlFavorite.setImageResource(R.mipmap.music_love);
                        } else {
                            isFavorite = false;
                            imageViewMusicControlFavorite.setImageResource(R.mipmap.music_unlove);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (db != null) {
                        db.close();
                    }
                }
            }
        }

        public int getMPCurrentPosition() {
            return mp.getCurrentPosition();
        }

        public void setView(final ImageView _imageViewPlayMusic, final ImageView _imageViewPreviousMusic, final ImageView _imageViewNextMusic, final ImageView _imageViewMusicPlayMode, final ImageView _imageViewMusicControlFavorite, final SeekBar _seekBarMusic, final TextView _textViewPlayTimeNow, final TextView _textViewPlayTimeTotal) {
            imageViewPlayMusic = _imageViewPlayMusic;
            imageViewPreviousMusic = _imageViewPreviousMusic;
            imageViewNextMusic = _imageViewNextMusic;
            imageViewMusicPlayMode = _imageViewMusicPlayMode;
            imageViewMusicControlFavorite = _imageViewMusicControlFavorite;
            seekBarMusic = _seekBarMusic;
            textViewPlayTimeTotal = _textViewPlayTimeTotal;
            //设置监听器
            OnClickEvent onClickEvent = new OnClickEvent();
            imageViewPlayMusic.setOnClickListener(onClickEvent);
            imageViewPreviousMusic.setOnClickListener(onClickEvent);
            imageViewNextMusic.setOnClickListener(onClickEvent);
            imageViewMusicPlayMode.setOnClickListener(onClickEvent);
            imageViewMusicControlFavorite.setOnClickListener(onClickEvent);
            seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    _textViewPlayTimeNow.setText(TimeUitl.changeMillsToDateTime(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isSeekBarChanging = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isSeekBarChanging = false;
                    if (isPlaying) {
                        mp.seekTo(seekBar.getProgress());
                    }
                }
            });
        }
    }
}