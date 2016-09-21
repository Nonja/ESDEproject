package fi.oulu.tol.esde08.ohapclient08;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.opimobi.ohap.Container;
import com.opimobi.ohap.EventSource;
import com.opimobi.ohap.Item;

/**
 * Created by Jonna on 15.5.2015.
 */
public class ContainerListAdapter implements android.widget.ListAdapter, EventSource.Listener<Container, Item> {

    public DataSetObservable datasetobservable;

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
            datasetobservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
            datasetobservable.unregisterObserver(observer);
    }

    @Override
    public int getCount() {
        return container.getItemCount();
    }

    @Override
    public Object getItem(int position) {
        return container.getItemByIndex(position);
    }

    @Override
    public long getItemId(int position) {
        return container.getItemByIndex(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            Context context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.rowTextView = (TextView)convertView.findViewById(R.id.rowTextView);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder)convertView.getTag();

        viewHolder.rowTextView.setText(container.getItemByIndex(position).getName());

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    private Container container;

    public ContainerListAdapter(Container container) {
        this.container = container;
        container.itemAddedEventSource.addListener(this);
        container.itemRemovedEventSource.addListener(this);
        datasetobservable = new DataSetObservable();
    }

    @Override
    public void onEvent(Container container, Item item) {
        datasetobservable.notifyChanged();
    }

    private static class ViewHolder {
        public TextView rowTextView;
    }
}
