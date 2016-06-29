package avalanche.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.utils.DeviceInfoHelper;
import avalanche.sasquatch.R;

public class DeviceInfoActivity extends AppCompatActivity {
    private ListView deviceInfoListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        deviceInfoListView = (ListView) findViewById(R.id.device_info_list_view);

        DeviceLog log = DeviceInfoHelper.getDeviceLog(getApplicationContext());
        final List<DeviceInfoDisplayModel> list = getDeviceInfoDisplayModelList(log);

        ArrayAdapter<DeviceInfoDisplayModel> adapter = new ArrayAdapter<DeviceInfoDisplayModel>(this, android.R.layout.simple_list_item_2, android.R.id.text1, list) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(list.get(position).title);
                text2.setText(list.get(position).value);
                return view;
            }
        };

        deviceInfoListView.setAdapter(adapter);
    }

    private List<DeviceInfoDisplayModel> getDeviceInfoDisplayModelList(DeviceLog log) {
        List<DeviceInfoDisplayModel> list = new ArrayList<>();

        Method[] methods = DeviceLog.class.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("get") && !name.equals("getClass") && !name.equals("getToffset")) {
                DeviceInfoDisplayModel model = new DeviceInfoDisplayModel();
                model.title = name.replace("get", "");
                try {
                    model.value = method.invoke(log).toString();
                } catch (Exception e) {
                    model.value = "N/A";
                }
                list.add(model);
            }
        }

        return list;
    }

    private class DeviceInfoDisplayModel {
        private String title;
        private String value;
    }
}