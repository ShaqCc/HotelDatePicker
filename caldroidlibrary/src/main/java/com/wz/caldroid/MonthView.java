// Copyright 2012 Square, Inc.
package com.wz.caldroid;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wz.caldroid.util.Utils;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthView extends LinearLayout {
    TextView title;
    CalendarGridView grid;
    private Listener listener;
    private List<CalendarCellDecorator> decorators;
    private boolean isRtl;
    private Locale locale;

    public static MonthView create(ViewGroup parent, LayoutInflater inflater,
                                   DateFormat weekdayNameFormat, Listener listener, Calendar today, int dividerColor,
                                   int dayBackgroundResId, int dayTextColorResId, int titleTextColor, boolean displayHeader,
                                   int headerTextColor, Locale locale, DayViewAdapter adapter) {
        return create(parent, inflater, weekdayNameFormat, listener, today, dividerColor,
                dayBackgroundResId, dayTextColorResId, titleTextColor, displayHeader, headerTextColor, null,
                locale, adapter);
    }

    public static MonthView create(ViewGroup parent, LayoutInflater inflater,
                                   DateFormat weekdayNameFormat, Listener listener, Calendar today, int dividerColor,
                                   int dayBackgroundResId, int dayTextColorResId, int titleTextColor, boolean displayHeader,
                                   int headerTextColor, List<CalendarCellDecorator> decorators, Locale locale,
                                   DayViewAdapter adapter) {
        final MonthView view = (MonthView) inflater.inflate(R.layout.month, parent, false);
        view.setDayViewAdapter(adapter);
        view.setDividerColor(dividerColor);
        view.setDayTextColor(dayTextColorResId);
        view.setTitleTextColor(titleTextColor);
        view.setDisplayHeader(displayHeader);
        view.setHeaderTextColor(headerTextColor);

        if (dayBackgroundResId != 0) {
            view.setDayBackground(dayBackgroundResId);
        }

        final int originalDayOfWeek = today.get(Calendar.DAY_OF_WEEK);

        view.isRtl = isRtl(locale);
        view.locale = locale;
        int firstDayOfWeek = today.getFirstDayOfWeek();
        final CalendarRowView headerRow = (CalendarRowView) view.grid.getChildAt(0);
        for (int offset = 0; offset < 7; offset++) {
            today.set(Calendar.DAY_OF_WEEK, getDayOfWeek(firstDayOfWeek, offset, view.isRtl));
            final TextView textView = (TextView) headerRow.getChildAt(offset);
            //设置星期几
            textView.setText(weekdayNameFormat.format(today.getTime()));
        }
        today.set(Calendar.DAY_OF_WEEK, originalDayOfWeek);
        view.listener = listener;
        view.decorators = decorators;
        return view;
    }

    private static int getDayOfWeek(int firstDayOfWeek, int offset, boolean isRtl) {
        int dayOfWeek = firstDayOfWeek + offset;
        if (isRtl) {
            return 8 - dayOfWeek;
        }
        return dayOfWeek;
    }

    private static boolean isRtl(Locale locale) {
        // TODO convert the build to gradle and use getLayoutDirection instead of this (on 17+)?
        final int directionality = Character.getDirectionality(locale.getDisplayName(locale).charAt(0));
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    public MonthView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDecorators(List<CalendarCellDecorator> decorators) {
        this.decorators = decorators;
    }

    public List<CalendarCellDecorator> getDecorators() {
        return decorators;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        title = (TextView) findViewById(R.id.title);
        grid = (CalendarGridView) findViewById(R.id.calendar_grid);
    }


    /**
     * 设置单元格参数，价格，颜色，是否可点击
     *
     * @param month
     * @param cells
     * @param displayOnly
     * @param titleTypeface
     * @param dateTypeface
     */
    public void init(MonthDescriptor month, List<List<MonthCellDescriptor>> cells,
                     boolean displayOnly, Typeface titleTypeface, Typeface dateTypeface) {
        Logr.d("Initializing MonthView (%d) for %s", System.identityHashCode(this), month);
        long start = System.currentTimeMillis();
        title.setText(month.getLabel());
        NumberFormat numberFormatter = NumberFormat.getInstance(locale);

        final int numRows = cells.size();
        grid.setNumRows(numRows);
        for (int i = 0; i < 6; i++) {
            CalendarRowView weekRow = (CalendarRowView) grid.getChildAt(i + 1);
            weekRow.setListener(listener);
            if (i < numRows) {
                weekRow.setVisibility(VISIBLE);
                List<MonthCellDescriptor> week = cells.get(i);
                for (int c = 0; c < week.size(); c++) {
                    MonthCellDescriptor cell = week.get(isRtl ? 6 - c : c);
                    CalendarCellView cellView = (CalendarCellView) weekRow.getChildAt(c);

                    String cellDate = numberFormatter.format(cell.getValue());
                    if (!cellView.getDayOfMonthTextView().getText().equals(cellDate)) {
                        cellView.getDayOfMonthTextView().setText(cellDate);
                    }
                    cellView.setEnabled(cell.isCurrentMonth());
                    if (!cell.isCurrentMonth()) cellView.setVisibility(INVISIBLE);
                    cellView.setClickable(!displayOnly);

                    cellView.setSelectable(cell.isSelectable());
                    cellView.setSelected(cell.isSelected());
                    cellView.setCurrentMonth(cell.isCurrentMonth());
                    cellView.setToday(cell.isToday());
                    cellView.setRangeState(cell.getRangeState());
                    cellView.setHighlighted(cell.isHighlighted());
                    if (cell.isHighlighted()) {
                        cellView.getDayOfMonthTextView().setTextColor(Color.parseColor("#666666"));
                        cellView.getStateTextView().setTextColor(Color.parseColor("#666666"));
                    }
                    if (!cell.isSelectable()) {
                        cellView.getDayOfMonthTextView().setTextColor(Color.parseColor("#aaaaaa"));
                        cellView.getStateTextView().setTextColor(Color.parseColor("#aaaaaa"));
                    }

                    if (cellView.isToday()) {
                        cellView.getDayOfMonthTextView().setText("今天");
                    }
                    if (cell.isBooked()) {
                        cellView.getStateTextView().setText("已租");
                    } else {
                        cellView.getStateTextView().setText("¥" + Utils.FormatDotNumber(String.valueOf(cell.getPrice())));
                    }
                    cellView.setTag(cell);

                    if (null != decorators) {
                        for (CalendarCellDecorator decorator : decorators) {
                            decorator.decorate(cellView, cell.getDate());
                        }
                    }
                }
            } else {
                weekRow.setVisibility(GONE);
            }
        }

        if (titleTypeface != null) {
            title.setTypeface(titleTypeface);
        }
        if (dateTypeface != null) {
            grid.setTypeface(dateTypeface);
        }

        Logr.d("MonthView.init took %d ms", System.currentTimeMillis() - start);
    }

    public void setDividerColor(int color) {
        grid.setDividerColor(color);
    }

    public void setDayBackground(int resId) {
        grid.setDayBackground(resId);
    }

    public void setDayTextColor(int resId) {
        grid.setDayTextColor(resId);
    }

    public void setDayViewAdapter(DayViewAdapter adapter) {
        grid.setDayViewAdapter(adapter);
    }

    public void setTitleTextColor(int color) {
        title.setTextColor(color);
    }

    public void setDisplayHeader(boolean displayHeader) {
        grid.setDisplayHeader(displayHeader);
    }

    public void setHeaderTextColor(int color) {
        grid.setHeaderTextColor(color);
    }

    public interface Listener {
        void handleClick(MonthCellDescriptor cell);
    }
}
