package de.tu_berlin.snet.cellactivity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class TabFragment extends Fragment {

    public static TabLayout mTabLayout;
    public static ViewPager mViewPager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /**
         *Inflate timelines_tab_layout and setup Views.
         */
        View tab_layout =  inflater.inflate(R.layout.timelines_tab_layout,null);
        mTabLayout = (TabLayout) tab_layout.findViewById(R.id.tabs);
        mViewPager = (ViewPager) tab_layout.findViewById(R.id.viewpager);

        /**
         *Set an Apater for the View Pager
         */

        DatabaseHelper myDb = DatabaseHelper.getInstance(getActivity());
        FragmentTimelineTabs tabs = new FragmentTimelineTabs(getChildFragmentManager());

        for (String date : myDb.getLastThreeDates()) tabs.addTimeline(date);

        mViewPager.setAdapter(tabs);

        /**
         * Now , this is a workaround ,
         * The setupWithViewPager dose't works without the runnable .
         * Maybe a Support Library Bug .
         */

        mTabLayout.post(new Runnable() {
            @Override
            public void run() {
                mTabLayout.setupWithViewPager(mViewPager);
            }
        });

        return tab_layout;

    }



    class FragmentTimelineTabs extends FragmentPagerAdapter{

        private final ArrayList<CharSequence> mTabTitles = new ArrayList<>();
        private final ArrayList<Fragment> mTabFragments = new ArrayList<>();

        public FragmentTimelineTabs(FragmentManager fm) {
            super(fm);
        }

        public void addTimeline(String date) {
            mTabTitles.add(date);
            mTabFragments.add(TimeLineFragment.newInstance(date));
            notifyDataSetChanged();
        }

        /**
         * Return fragment with respect to Position .
         */

        @Override
        public Fragment getItem(int position)
        {
            return mTabFragments.get(position);
        }

        @Override
        public int getCount() {
            return mTabTitles.size();
        }

        /**
         * This method returns the title of the tab according to the position.
         */

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles.get(position);
        }
    }

}