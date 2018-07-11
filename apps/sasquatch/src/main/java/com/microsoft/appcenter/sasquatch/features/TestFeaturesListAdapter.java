package com.microsoft.appcenter.sasquatch.features;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.microsoft.appcenter.sasquatch.R;

import java.util.List;

public class TestFeaturesListAdapter extends BaseAdapter {

    private final List<TestFeatures.TestFeatureModel> mList;

    public TestFeaturesListAdapter(List<TestFeatures.TestFeatureModel> list) {
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        /* Set title and description to the view. */
        View rowView = null;
        Object item = getItem(position);
        if (item instanceof TestFeatures.TestFeatureTitle) {
            if (convertView != null && convertView.getTag() != null && ((ViewHolder) convertView.getTag()).mClass == TestFeatures.TestFeatureTitle.class) {
                ViewHolder holder = (ViewHolder) convertView.getTag();
                holder.mTextView1.setText(((TestFeatures.TestFeatureTitle) item).getTitle());
                rowView = convertView;
            } else {
                rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_title, parent, false);
                TextView titleView = rowView.findViewById(R.id.title);
                titleView.setText(((TestFeatures.TestFeatureTitle) item).getTitle());
                rowView.setTag(new ViewHolder(TestFeatures.TestFeatureTitle.class, titleView));
            }
        } else if (item instanceof TestFeatures.TestFeatureModel) {
            TestFeatures.TestFeature model = (TestFeatures.TestFeature) item;
            if (convertView != null && convertView.getTag() != null && ((ViewHolder) convertView.getTag()).mClass == TestFeatures.TestFeatureModel.class) {
                ViewHolder holder = (ViewHolder) convertView.getTag();
                holder.mTextView1.setText(model.getTitle());
                holder.mTextView2.setText(model.getDescription());
                rowView = convertView;
            } else {
                rowView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                TextView titleView = rowView.findViewById(android.R.id.text1);
                TextView descriptionView = rowView.findViewById(android.R.id.text2);
                titleView.setText(model.getTitle());
                descriptionView.setText(model.getDescription());
                rowView.setTag(new ViewHolder(TestFeatures.TestFeatureModel.class, titleView, descriptionView));
            }
        }
        return rowView;
    }

    private static class ViewHolder {

        private final Class mClass;

        private final TextView mTextView1;

        private final TextView mTextView2;

        private ViewHolder(Class clazz, TextView view) {
            this(clazz, view, null);
        }

        private ViewHolder(Class clazz, TextView view1, TextView view2) {
            mClass = clazz;
            mTextView1 = view1;
            mTextView2 = view2;
        }
    }
}