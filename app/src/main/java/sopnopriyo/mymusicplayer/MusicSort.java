package sopnopriyo.mymusicplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import sopnopriyo.mymusicplayer.fragment.MusicList;
import sopnopriyo.mymusicplayer.fragment.SortList;
import sopnopriyo.mymusicplayer.util.FileUtil;

/**
 * Created by sopnopriyo on 2014/5/7.
 */
public class MusicSort extends ActionBarActivity {
    //Controls
    private ViewPager viewPager;
    private View viewLineAllMusic;
    private View viewLineFolder;
    private View viewLineAlbum;
    private View viewLineArtist;
    private RelativeLayout relativeLayoutMusicSortBannerAll;
    private RelativeLayout relativeLayoutMusicSortBannerFolder;
    private RelativeLayout relativeLayoutMusicSortBannerAlbum;
    private RelativeLayout relativeLayoutMusicSortBannerArtist;
    //ViewPager the Fragment list
    private ArrayList<Fragment> fragmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musicsort);
        //Configuration ActionBar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.music_file);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        //Bound control
        viewPager = (ViewPager) findViewById(R.id.viewPagerMusicList);
        viewLineAllMusic = findViewById(R.id.viewLineAllMusic);
        viewLineFolder = findViewById(R.id.viewLineFolder);
        viewLineAlbum = findViewById(R.id.viewLineAlbum);
        viewLineArtist = findViewById(R.id.viewLineArtist);
        relativeLayoutMusicSortBannerAll = (RelativeLayout) findViewById(R.id.relativeLayoutMusicSortBannerAll);
        relativeLayoutMusicSortBannerFolder = (RelativeLayout) findViewById(R.id.relativeLayoutMusicSortBannerFolder);
        relativeLayoutMusicSortBannerAlbum = (RelativeLayout) findViewById(R.id.relativeLayoutMusicSortBannerAlbum);
        relativeLayoutMusicSortBannerArtist = (RelativeLayout) findViewById(R.id.relativeLayoutMusicSortBannerArtist);
        OnClickEvent onClickEvent = new OnClickEvent();
        relativeLayoutMusicSortBannerAll.setOnClickListener(onClickEvent);
        relativeLayoutMusicSortBannerFolder.setOnClickListener(onClickEvent);
        relativeLayoutMusicSortBannerAlbum.setOnClickListener(onClickEvent);
        relativeLayoutMusicSortBannerArtist.setOnClickListener(onClickEvent);
        new Runnable() {
            @Override
            public void run() {
                //Configuration ViewPager
                initFragmentList();
                viewPager.setAdapter(new MusicSortViewPagerAdapter(getSupportFragmentManager()));
                viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {
                        switch (position) {
                            case 0:
                                viewLineAllMusic.setVisibility(View.VISIBLE);
                                viewLineFolder.setVisibility(View.INVISIBLE);
                                viewLineAlbum.setVisibility(View.INVISIBLE);
                                viewLineArtist.setVisibility(View.INVISIBLE);
                                break;
                            case 1:
                                viewLineAllMusic.setVisibility(View.INVISIBLE);
                                viewLineFolder.setVisibility(View.VISIBLE);
                                viewLineAlbum.setVisibility(View.INVISIBLE);
                                viewLineArtist.setVisibility(View.INVISIBLE);
                                break;
                            case 2:
                                viewLineAllMusic.setVisibility(View.INVISIBLE);
                                viewLineFolder.setVisibility(View.INVISIBLE);
                                viewLineAlbum.setVisibility(View.VISIBLE);
                                viewLineArtist.setVisibility(View.INVISIBLE);
                                break;
                            case 3:
                                viewLineAllMusic.setVisibility(View.INVISIBLE);
                                viewLineFolder.setVisibility(View.INVISIBLE);
                                viewLineAlbum.setVisibility(View.INVISIBLE);
                                viewLineArtist.setVisibility(View.VISIBLE);
                                break;
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });
            }
        }.run();
    }

    public void initFragmentList() {
        fragmentList = new ArrayList<Fragment>();

        Bundle b1 = new Bundle();
        b1.putString("sortKey", FileUtil.SortKey.All.name());
        b1.putString("keyStr", null);
        MusicList musicList = new MusicList();
        musicList.setArguments(b1);
        fragmentList.add(musicList);

        Bundle b2 = new Bundle();
        b2.putString("sortKey", FileUtil.SortKey.Folder.name());
        SortList sortListFolder = new SortList();
        sortListFolder.setArguments(b2);
        fragmentList.add(sortListFolder);

        Bundle b3 = new Bundle();
        b3.putString("sortKey", FileUtil.SortKey.Album.name());
        SortList sortListAlbum = new SortList();
        sortListAlbum.setArguments(b3);
        fragmentList.add(sortListAlbum);

        Bundle b4 = new Bundle();
        b4.putString("sortKey", FileUtil.SortKey.Artist.name());
        SortList sortListArtist = new SortList();
        sortListArtist.setArguments(b4);
        fragmentList.add(sortListArtist);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class OnClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == relativeLayoutMusicSortBannerAll) {
                viewPager.setCurrentItem(0);
            } else if (v == relativeLayoutMusicSortBannerFolder) {
                viewPager.setCurrentItem(1);
            } else if (v == relativeLayoutMusicSortBannerAlbum) {
                viewPager.setCurrentItem(2);
            } else if (v == relativeLayoutMusicSortBannerArtist) {
                viewPager.setCurrentItem(3);
            }
        }
    }

    class MusicSortViewPagerAdapter extends FragmentPagerAdapter {
        public MusicSortViewPagerAdapter(FragmentManager fm) {
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

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //super.destroyItem(container, position, object);
        }
    }
}
