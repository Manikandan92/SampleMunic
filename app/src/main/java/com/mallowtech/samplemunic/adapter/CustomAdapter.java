package com.mallowtech.samplemunic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mallowtech.samplemunic.R;
import com.mallowtech.samplemunic.datamodel.DataModel;

import java.util.ArrayList;

/**
 * Created by manikandan on 16/06/17.
 */
public class CustomAdapter extends ArrayAdapter<DataModel> {

    private ArrayList<DataModel> dataSet;
    Context mContext;

    // View lookup cache
    private static class ViewHolder {
        TextView txtUUID;
        TextView values;
    }

    public CustomAdapter(ArrayList<DataModel> data, Context context) {
        super(context, R.layout.row_item, data);
        this.dataSet = data;
        this.mContext = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        DataModel dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_item, parent, false);
            viewHolder.txtUUID = (TextView) convertView.findViewById(R.id.name);
            viewHolder.values = (TextView) convertView.findViewById(R.id.values);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.txtUUID.setText("UUID : " + dataModel.getUUID());
        viewHolder.values.setText(dataModel.getValue());
        // Return the completed view to render on screen
        return convertView;
    }
}