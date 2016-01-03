package sopnopriyo.mymusicplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import sopnopriyo.mymusicplayer.MainActivity;
import sopnopriyo.mymusicplayer.R;
import sopnopriyo.mymusicplayer.dialog.PlayListDialog;
import sopnopriyo.mymusicplayer.util.CodeUtil;
import sopnopriyo.mymusicplayer.util.DBUtil;
import sopnopriyo.mymusicplayer.util.FileUtil;
import sopnopriyo.mymusicplayer.widget.IndexBar;
import sopnopriyo.mymusicplayer.widget.MyListView;

/**
 * Created by sopnopriyo on 2014/5/8.
 */
public class MusicList extends Fragment {
    private MusicListAdapter adapter;
    //控件
    private MyListView myListViewMusicList;
    private RelativeLayout relativeLayoutMusicListAction;
    private RelativeLayout relativeLayoutPlayListAction;
    private Button buttonMusicListAdd;
    private Button buttonMusicListDelete;
    private IndexBar indexBarMusicList;
    //Cancel the operation and select all music list
    private RelativeLayout relativeLayoutMusicListControl;
    private Button buttonMusicListActionCancel;
    private Button buttonMusicListChoseAll;
    //Category information
    private FileUtil.SortKey sortKey;
    private String keyStr;
    //List Data
    private ArrayList<HashMap<String, String>> musicList;
    private String sql = "";
    //Is selection mode
    private boolean isSelectedMode = false;
    //CheckBox select case
    private boolean[] checkArray;
    //Whether it is a state-wide election
    private boolean isAllSelected = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_musiclist, null);
        //The data gets passed
        Bundle b = getArguments();
        sortKey = FileUtil.SortKey.valueOf(b.getString("sortKey"));
        keyStr = b.getString("keyStr");
        //Bound control
        myListViewMusicList = (MyListView) view.findViewById(R.id.myListViewMusicList);
        relativeLayoutMusicListControl = (RelativeLayout) view.findViewById(R.id.relativeLayoutMusicListControl);
        relativeLayoutMusicListAction = (RelativeLayout) view.findViewById(R.id.relativeLayoutMusicListAction);
        relativeLayoutPlayListAction = (RelativeLayout) view.findViewById(R.id.relativeLayoutPlayListAction);
        indexBarMusicList = (IndexBar) view.findViewById(R.id.indexBarMusicList);
        buttonMusicListActionCancel = (Button) view.findViewById(R.id.buttonMusicListActionCancel);
        buttonMusicListChoseAll = (Button) view.findViewById(R.id.buttonMusicListChoseAll);
        buttonMusicListAdd = (Button) view.findViewById(R.id.buttonMusicListAdd);
        buttonMusicListDelete = (Button) view.findViewById(R.id.buttonMusicListDelete);
        //Setting Listener
        OnClickEvent onClickEvent = new OnClickEvent();
        buttonMusicListActionCancel.setOnClickListener(onClickEvent);
        buttonMusicListChoseAll.setOnClickListener(onClickEvent);
        buttonMusicListAdd.setOnClickListener(onClickEvent);
        buttonMusicListDelete.setOnClickListener(onClickEvent);
        //Open thread configuration ListView
        new Runnable() {
            @Override
            public void run() {
                initMusicList();
                //Initialization ListView adapter
                adapter = new MusicListAdapter();
                myListViewMusicList.setTitle(LayoutInflater.from(getActivity()).inflate(R.layout.list_item_tag, myListViewMusicList, false));
                //Set ListView animation
//                AnimationSet set = new AnimationSet(true);
//                Animation animation = AnimationUtils.loadAnimation(getActivity(),
//                        R.anim.list_item_music_in_alpha);
//                set.addAnimation(animation);
//                LayoutAnimationController controller = new LayoutAnimationController(
//                        set, 0.3f);
//                myListViewMusicList.setLayoutAnimation(controller);


                myListViewMusicList.setAdapter(adapter);
                myListViewMusicList.setOnScrollListener(adapter);
                //Initialization Index Bar
                indexBarMusicList.setListView(myListViewMusicList);
                myListViewMusicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                        if (isSelectedMode) {
                            checkArray[position] = !checkArray[position];
                            adapter.notifyDataSetChanged();
                        } else {
                            new Runnable() {
                                @Override
                                public void run() {
                                    //Play a song in the list position location
                                    MainActivity.getMusicBinder().playMusicList(musicList, position, sql);
                                    //Returns player interface
                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                }
                            }.run();
                        }
                    }
                });
                myListViewMusicList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if (!isSelectedMode) {
                            isSelectedMode = true;
                            checkArray[position] = true;
                            adapter.notifyDataSetChanged();
                        }
                        return true;
                    }
                });
            }
        }.run();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    //Get a list of data
    private void initMusicList() {
        switch (sortKey) {
            case All:
                sql = "select path,title,pinyin,album,artist from " + DBUtil.T_MusicFile_Name + " order by pinyin";
                break;
            case Folder:
                sql = "select path,title,pinyin,album,artist from " + DBUtil.T_MusicFile_Name + " where folder='" + keyStr.replace("'", "''") + "' order by pinyin";
                break;
            case Album:
                sql = "select path,title,pinyin,album,artist from " + DBUtil.T_MusicFile_Name + " where album='" + keyStr.replace("'", "''") + "' order by pinyin";
                break;
            case Artist:
                sql = "select path,title,pinyin,album,artist from " + DBUtil.T_MusicFile_Name + " where artist='" + keyStr.replace("'", "''") + "' order by pinyin";
                break;
            case PlayList:
                sql = "select path,title,pinyin,album,artist,playlist from " + DBUtil.T_PlayListFile_Name + " where playlist=" + keyStr.replace("'", "''") + " order by pinyin";
                break;
            case FavoriteList:
                sql = "select path,title,pinyin,album,artist from " + DBUtil.T_MusicFile_Name + " where favorite=1 order by pinyin";
                break;
        }
        musicList = CodeUtil.getMusicList(getActivity(), sql, sortKey);
        checkArray = new boolean[musicList.size()];
        for (int i = 0; i < checkArray.length; i++) {
            checkArray[i] = false;
        }
    }

    //Get the song list has been selected
    private ArrayList<HashMap<String, String>> getSelectedMusicList() {
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < checkArray.length; i++) {
            if (checkArray[i]) {
                list.add(musicList.get(i));
            }
        }
        return list;
    }

    private class OnClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == buttonMusicListActionCancel) {
                new Runnable() {
                    @Override
                    public void run() {
                        isSelectedMode = false;
                        for (int i = 0; i < checkArray.length; i++) {
                            checkArray[i] = false;
                        }
                        adapter.notifyDataSetChanged();
                    }
                }.run();
            } else if (v == buttonMusicListChoseAll) {
                new Runnable() {
                    @Override
                    public void run() {
                        isAllSelected = !isAllSelected;
                        if (isAllSelected) {
                            for (int i = 0; i < checkArray.length; i++) {
                                checkArray[i] = true;
                            }
                        } else {
                            for (int i = 0; i < checkArray.length; i++) {
                                checkArray[i] = false;
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }.run();
            } else if (v == buttonMusicListAdd) {
                new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<HashMap<String, String>> list = getSelectedMusicList();
                        PlayListDialog dialog = new PlayListDialog(getActivity(), list);
                        //Change the position and size of the dialog
//                        Window dialogWindow = dialog.getWindow();
//                        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
//                        DisplayMetrics dm = new DisplayMetrics();
//                        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
//                        lp.width = dm.widthPixels;
//                        dialogWindow.setGravity(Gravity.BOTTOM);

                        dialog.setTitle("Add to");
                        dialog.show();
                    }
                }.run();
            } else if (v == buttonMusicListDelete) {
                new Runnable() {
                    @Override
                    public void run() {
                        //Get the song list has been selected
                        ArrayList<HashMap<String, String>> list = getSelectedMusicList();
                        //Splicing sql statement
                        String[] sql = new String[list.size()];
                        for (int i = 0; i < sql.length; i++) {
                            sql[i] = ("delete from " + DBUtil.T_PlayListFile_Name + " where path='" + list.get(i).get("path").replace("'", "''") + "' and playlist=" + list.get(i).get("playlist"));
                        }
                        DBUtil.execSqlDatabase(getActivity(), DBUtil.databaseName, sql);
                        Toast.makeText(getActivity(), "Deleted" + list.size() + "Songs", Toast.LENGTH_SHORT).show();
                        initMusicList();
                        adapter.notifyDataSetChanged();
                    }
                }.run();
            }
        }
    }

    public class MusicListAdapter extends BaseAdapter implements SectionIndexer, AbsListView.OnScrollListener {
        private char[] indexChar = {'#', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        private ArrayList<Character> indexCharList;
        private ArrayList<Integer> indexIntList;
        private ViewHolder holder;
        //Records Item currently displayed maximum position, in order to optimize the display Animation
        //private int lastShownPosition = -1;
        //Item ListView in animation
        //private Animation itemAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.list_item_music_in_alpha);

        public MusicListAdapter() {
            initIndexChar();
        }

        private void initIndexChar() {
            indexCharList = new ArrayList<Character>();
            indexIntList = new ArrayList<Integer>();
            indexCharList.add('#');
            indexIntList.add(0);
            int x = 0;
            for (int i = 0; i < getCount(); i++) {
                if (musicList.get(i).get("pinyin").charAt(0) != indexCharList.get(x)) {
                    indexCharList.add(musicList.get(i).get("pinyin").charAt(0));
                    x++;
                    indexIntList.add(i);
                }
            }
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_music, parent, false);
                holder.textViewListItemTag = (TextView) convertView.findViewById(R.id.textViewListItemTag);
                holder.textViewMusicItem = (TextView) convertView.findViewById(R.id.textViewMusicItem);
                holder.checkBoxMusicItem = (CheckBox) convertView.findViewById(R.id.checkBoxMusicItem);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.textViewMusicItem.setText(musicList.get(position).get("title"));

            //Determine the current position of the index is the same as the previous letter, if different, the current location for the first letter of the next index, showing the current location of the index letters
            char cur = musicList.get(position).get("pinyin").charAt(0);
            char pre = position - 1 >= 0 ? musicList.get(position - 1).get("pinyin").charAt(0) : '@';
            if (cur != pre) {
                holder.textViewListItemTag.setText(String.valueOf(indexCharList.get(indexIntList.indexOf(position))));
                holder.textViewListItemTag.setVisibility(View.VISIBLE);
            } else {
                holder.textViewListItemTag.setVisibility(View.GONE);
            }
            if (isSelectedMode) {
                holder.checkBoxMusicItem.setVisibility(View.VISIBLE);
                holder.checkBoxMusicItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        myListViewMusicList.performItemClick(null, position, position);
                    }
                });
                holder.checkBoxMusicItem.setChecked(checkArray[position]);
            } else {
                holder.checkBoxMusicItem.setVisibility(View.GONE);
            }
//            if(position > lastShownPosition){
//                lastShownPosition = position;
//                convertView.startAnimation(itemAnimation);
//            }
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return musicList.get(position).hashCode();
        }

        @Override
        public Object getItem(int position) {
            return musicList.get(position);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            //If the list has changed, to re-initialize the index list
            if (indexChar.length != getCount()) {
                initIndexChar();
            }
            //Determine whether the selected state
            if (isSelectedMode) {
                relativeLayoutMusicListControl.setVisibility(View.VISIBLE);
                //It is judged whether the interface playlist
                if (sortKey == FileUtil.SortKey.PlayList) {
                    relativeLayoutPlayListAction.setVisibility(View.VISIBLE);
                } else {
                    relativeLayoutMusicListAction.setVisibility(View.VISIBLE);
                }
            } else {
                relativeLayoutMusicListControl.setVisibility(View.GONE);
                if (sortKey == FileUtil.SortKey.PlayList) {
                    relativeLayoutPlayListAction.setVisibility(View.GONE);
                } else {
                    relativeLayoutMusicListAction.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getCount() {
            return musicList.size();
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        @Override
        public int getPositionForSection(int section) {
            for (int i = 0; i < indexCharList.size(); i++) {
                if (indexChar[section] == indexCharList.get(i)) {
                    return indexIntList.get(i);
                }
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            char c = musicList.get(position).get("pinyin").charAt(0);
            for (int i = 0; i < indexChar.length; i++) {
                if (c == indexChar[i]) {
                    return i;
                }
            }
            return -1;
        }

        public int getTitleState(int position) {
            if (position < 0 || getCount() == 0) {
                return 0;
            }
            int section = getSectionForPosition(position);
            if (section == -1 || section > indexChar.length) {
                return 0;
            }
            int currentCharIndex = indexCharList.indexOf(indexChar[section]);
            //If you have a current position within the index Char Char index list, and the current index Char Char index is not within the list of the last one, and the index list within Int next index position in the ListView just after the current position of a Char index, returns 2: index sliding effect
            if (currentCharIndex != -1 && currentCharIndex != indexCharList.size() - 1 && position == indexIntList.get(currentCharIndex + 1) - 1) {
                return 2;
            }
            return 1;
        }

        public void setTitleText(View mHeader, int firstVisiblePosition) {
            String title = String.valueOf(musicList.get(firstVisiblePosition).get("pinyin").charAt(0));
            TextView sectionHeader = (TextView) mHeader;
            sectionHeader.setText(title);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (firstVisibleItem != 0 && view instanceof MyListView) {
                ((MyListView) view).titleLayout(firstVisibleItem);
            }
        }

        private final class ViewHolder {
            TextView textViewListItemTag;
            TextView textViewMusicItem;
            CheckBox checkBoxMusicItem;
        }
    }
}
