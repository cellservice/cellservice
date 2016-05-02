package de.tu_berlin.snet.cellactivity.view.fragment;
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

import java.sql.Date;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.tu_berlin.snet.cellactivity.R;
import de.tu_berlin.snet.cellactivity.model.database.GeoDatabaseHelper;

public class TabFragment extends Fragment {
    @Bind(R.id.tabs)
    TabLayout tabLayout;

    @Bind(R.id.viewpager)
    ViewPager viewPager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View tab_layout = inflater.inflate(R.layout.timelines_tab_layout, container, false);
        ButterKnife.bind(this, tab_layout);

        GeoDatabaseHelper myDb = GeoDatabaseHelper.getInstance(getActivity());
        FragmentTimelineTabs tabs = new FragmentTimelineTabs(getChildFragmentManager());

        for (Date date : myDb.getLastThreeDates()) tabs.addTimeline(date);

        viewPager.setAdapter(tabs);

        /**
         * Now , this is a workaround ,
         * The setupWithViewPager dose't works without the runnable .
         * Maybe a Support Library Bug .
         */
        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                tabLayout.setupWithViewPager(viewPager);
            }
        });

        return tab_layout;
    }

    class FragmentTimelineTabs extends FragmentPagerAdapter {

        private final ArrayList<Date> tabDates = new ArrayList<>();
        private final ArrayList<Fragment> tabFragments = new ArrayList<>();

        public FragmentTimelineTabs(FragmentManager fm) {
            super(fm);
        }

        public void addTimeline(Date date) {
            tabDates.add(date);
            tabFragments.add(TimeLineFragment.newInstance(date));
            notifyDataSetChanged();
        }

        /**
         * Return fragment with respect to Position .
         */
        @Override
        public Fragment getItem(int position)
        {
            return tabFragments.get(position);
        }

        @Override
        public int getCount() {
            return tabDates.size();
        }

        /**
         * This method returns the title of the tab according to the position.
         */
        @Override
        public CharSequence getPageTitle(int position) {
            return tabDates.get(position).toString();
        }
    }

}