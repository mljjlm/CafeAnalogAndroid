/*
 * Copyright 2016 Analog IO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.cafeanalog;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import dk.cafeanalog.networking.AnalogClient;
import dk.cafeanalog.views.OpeningHoursView;
import rx.functions.Action1;

/**
 * A fragment representing a list of Items.
 * <p/>
 */
public class OpeningsFragment extends Fragment {
    private static final String OPENING_CONTENT = "Opening_Content";

    private ArrayList<DayOfOpenings> mOpenings;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public OpeningsFragment() {
    }

    public static OpeningsFragment newInstance(List<DayOfOpenings> openings) {
        OpeningsFragment fragment = new OpeningsFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(OPENING_CONTENT, new ArrayList<>(openings));

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mOpenings = args.getParcelableArrayList(OPENING_CONTENT);
    }

    @BindView(R.id.refresher) SwipeRefreshLayout refresher;
    @BindView(R.id.list) RecyclerView listView;
    @BindView(R.id.empty_view) TextView emptyView;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(OPENING_CONTENT)) {
                mOpenings = savedInstanceState.getParcelableArrayList(OPENING_CONTENT);
            }
        }

        View view = inflater.inflate(R.layout.fragment_opening_list, container, false);

        ButterKnife.bind(this, view);

        if (mOpenings.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        refresher.setNestedScrollingEnabled(true);
        refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AnalogClient.getInstance().getDaysOfOpenings(
                        new Action1<List<DayOfOpenings>>() {
                            @Override
                            public void call(List<DayOfOpenings> dayOfOpenings) {
                                mOpenings.clear();
                                mOpenings.addAll(dayOfOpenings);
                                listView.getAdapter().notifyDataSetChanged();

                                if (mOpenings.isEmpty()) {
                                    emptyView.setVisibility(View.VISIBLE);
                                    listView.setVisibility(View.GONE);
                                } else {
                                    emptyView.setVisibility(View.GONE);
                                    listView.setVisibility(View.VISIBLE);
                                }

                                refresher.setRefreshing(false);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // Ignore
                            }
                        }
                );
            }
        });


        listView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(new RecyclerView.Adapter<DayHolder>() {
            @Override
            public DayHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View day = inflater.inflate(R.layout.day, parent, false);
                return new DayHolder(day);
            }

            @Override
            public void onBindViewHolder(DayHolder holder, int position) {
                DayOfOpenings day = mOpenings.get(position);

                int[] buffer = new int[day.getOpenings().size()];

                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = day.getOpenings().get(i);
                }

                holder.openingHours.setOpeningHour(buffer.clone());

                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = day.getClosings().get(i);
                }

                holder.openingHours.setClosingHour(buffer);

                holder.dayOfWeek.setText(getDayOfWeek(getActivity(), day.getDayOfWeek()));
            }

            @Override
            public int getItemCount() {
                return mOpenings.size();
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(OPENING_CONTENT, mOpenings);
    }

    class DayHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.day_of_week) TextView dayOfWeek;
        @BindView(R.id.opening_hours) OpeningHoursView openingHours;

        public DayHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private static String getDayOfWeek(Context context, int dayOfWeek) {
        switch (dayOfWeek) {
            case DayOfOpenings.SUNDAY: return context.getString(R.string.sunday);
            case DayOfOpenings.MONDAY: return context.getString(R.string.monday);
            case DayOfOpenings.TUESDAY: return context.getString(R.string.tuesday);
            case DayOfOpenings.WEDNESDAY: return context.getString(R.string.wednesday);
            case DayOfOpenings.THURSDAY: return context.getString(R.string.thursday);
            case DayOfOpenings.FRIDAY: return context.getString(R.string.friday);
            case DayOfOpenings.SATURDAY:
            default:                   return context.getString(R.string.saturday);
        }
    }
}
