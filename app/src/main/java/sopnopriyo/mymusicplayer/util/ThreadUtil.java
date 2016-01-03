package sopnopriyo.mymusicplayer.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sopnopriyo on 2014/5/7.
 */
public class ThreadUtil {
    public interface OnGetPathUpgrade {
        void onUpgrade(String path);
    }

    public static class DatabaseAsyncTask extends AsyncTask<String, String, String> {
        private Context context;
        private ProgressDialog progressDialog;
        private ArrayList<HashMap<String, String>> musicFileList = new ArrayList<>();
        private ArrayList<String> pathList = new ArrayList<>();
        private ArrayList<String> fileNameList = new ArrayList<>();
        private boolean isFirstTime;
        private OnGetPathUpgrade onGetPathUpgrade;

        public DatabaseAsyncTask(Context context, boolean isFirstTime) {
            this.context = context;
            this.isFirstTime = isFirstTime;
            //Without this one, reading music files Tag will be a wide range of distortion
//            TagOptionSingleton.getInstance().setAndroid(true);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Searching for music files");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            //If not the first time to retrieve the song information, you need to clear the data
            if (!isFirstTime) {
                DBUtil.execSqlDatabase(context, DBUtil.databaseName, new String[]{
                        "drop table " + DBUtil.T_MusicFile_Name,
                        "drop table " + DBUtil.T_PlayListFile_Name,
                        "drop table " + DBUtil.T_PlayList_Name});
            }

            //Read song files path
            if (onGetPathUpgrade == null) {
                onGetPathUpgrade = new OnGetPathUpgrade() {
                    @Override
                    public void onUpgrade(String absolutePath) {
                        publishProgress(absolutePath);
                    }
                };
            }
            getAllMusicFilePath(params[0], onGetPathUpgrade);
            publishProgress("Resolving song files");

            //Read song files information stored musicList list
            for (int i = 0; i < pathList.size(); i++) {
                String path = pathList.get(i);
                String fileName = fileNameList.get(i);
                publishProgress(fileName, String.valueOf(i + 1));
                String db_title = fileName.substring(0, fileName.lastIndexOf("."));
                String db_album = "(unknown)";
                String db_artist = "(unknown)";
                try {
                    final AudioFile audioFile = AudioFileIO.read(new File(path + fileName));
                    final Tag tag = audioFile.getTag();
                    if (tag != null) {
                        tag.setEncoding("UTF-8");
                        final String titleTemp = tag.getFirst(FieldKey.TITLE);
                        final String albumTemp = tag.getFirst(FieldKey.ALBUM);
                        final String artistTemp = tag.getFirst(FieldKey.ARTIST);
                        if (!titleTemp.trim().equals("")) {
                            db_title = titleTemp;
                        }
                        if (!albumTemp.trim().equals("")) {
                            db_album = albumTemp;
                        }
                        if (!artistTemp.trim().equals("")) {
                            db_artist = artistTemp;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("--->Something wrong with " + fileName);
                    e.printStackTrace();
                }
                //Basic information of music files
                String db_path = path + fileName;
                String db_folder = path.substring(0, path.length() - 1);
                String db_pinyin = HanZiToPinYin.toUpperPinYin(db_title);
                char c = db_pinyin.charAt(0);
                if (c < 'A' || c > 'Z') {
                    db_pinyin = "#" + db_pinyin;
                }
                //It will be stored in the database information added music FileList
                HashMap<String, String> map = new HashMap<>();
                map.put("path", db_path);
                map.put("title", db_title);
                map.put("pinyin", db_pinyin);
                map.put("folder", db_folder);
                map.put("album", db_album);
                map.put("artist", db_artist);
                musicFileList.add(map);
            }
            //Read music FileList information, splicing sql statement
            String[] sql = new String[musicFileList.size()];
            for (int i = 0; i < musicFileList.size(); i++) {
                HashMap<String, String> map = musicFileList.get(i);
                sql[i] = "insert or ignore into " + DBUtil.T_MusicFile_Name +
                        " (path,title,pinyin,folder,album,artist,favorite)values('" +
                        map.get("path").replace("'", "''") + "','" +
                        map.get("title").replace("'", "''") + "','" +
                        map.get("pinyin").replace("'", "''") + "','" +
                        map.get("folder").replace("'", "''") + "','" +
                        map.get("album").replace("'", "''") + "','" +
                        map.get("artist").replace("'", "''") + "',0)";
            }
            try {
                DBUtil.execSqlDatabase(context, DBUtil.databaseName, sql);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error updating song，If you want，Please contact the Author：18607006059", Toast.LENGTH_LONG).show();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values[0].equals("Resolving song files")) {
                progressDialog.dismiss();

                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle("Resolving song files");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(fileNameList.size());
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
            progressDialog.setMessage(values[0]);
            if (values.length > 1) {
                progressDialog.setProgress(Integer.parseInt(values[1]));
            }
        }

        @Override
        protected void onPostExecute(String s) {
            progressDialog.dismiss();
            Toast.makeText(context, "The song has been updated", Toast.LENGTH_SHORT).show();
        }

        long lastUpdateTime = 0;

        //Get all song files path
        private void getAllMusicFilePath(String path, OnGetPathUpgrade onGetPathUpgrade) {
            try {
                String[] fileList = new File(path).list();
                for (String fileListItem : fileList) {
                    if (System.currentTimeMillis() - lastUpdateTime > 500) {
                        lastUpdateTime = System.currentTimeMillis();
                        onGetPathUpgrade.onUpgrade(path + fileListItem);
                    }
                    if (FileUtil.isDir(path + fileListItem)) {
                        if (FileUtil.isAccessDir(path + fileListItem)) {
                            getAllMusicFilePath(path + fileListItem + File.separator, onGetPathUpgrade);
                        }
                    } else if (fileListItem.contains(".")) {
                        String fileType = fileListItem.substring(fileListItem.lastIndexOf(".") + 1, fileListItem.length());
                        //Tag information read music files
                        if (fileType.equals("mp3") || fileType.equals("wav") || fileType.equals("flac") || fileType.equals("ape")) {
                            pathList.add(path);
                            fileNameList.add(fileListItem);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}