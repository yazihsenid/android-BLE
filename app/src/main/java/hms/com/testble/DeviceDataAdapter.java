package hms.com.testble;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by yasitha on 5/26/17.
 */

public class DeviceDataAdapter extends RecyclerView.Adapter<DeviceDataAdapter.DeviceItem> {

    private static final long EXPIRE_TIME = TimeUnit.SECONDS.toMillis(5);

    private Map<String, BluetoothDeviceExpireble> devicesMap;
    private List<BluetoothDeviceExpireble> deviceList;

    public DeviceDataAdapter() {
        devicesMap = new HashMap<>();
        deviceList = new ArrayList<>();
    }

    @Override
    public DeviceItem onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.lv_item_device, parent, false);
        return new DeviceItem(v);
    }

    @Override
    public void onBindViewHolder(DeviceItem holder, int position) {
        BluetoothDevice device = deviceList.get(position).bluetoothDevice;
        holder.tvDeviceName.setText(device.getName());
        holder.tvDeviceAddress.setText(device.getAddress());

        StringBuilder UUIDs = new StringBuilder();
        boolean sanityCheckPassed = device.fetchUuidsWithSdp();
        device.describeContents();
        if (sanityCheckPassed) {
            Log.i("sanity Check", "passed");
            ParcelUuid[] parcelUuids = device.getUuids();
            if (parcelUuids != null) {
                for (ParcelUuid uuid : parcelUuids) {
                    UUIDs.append(uuid.getUuid()).append(',');
                }
            } else {
                UUIDs.append("uuid array is null");
            }
        } else {
            UUIDs.append("sanityCheckFailed");
        }
        holder.tvDeviceUUID.setText(UUIDs);

        holder.tvDeviceBondState.setText(String.valueOf(device.getBondState()));
        holder.tvDeviceType.setText(String.valueOf(device.getType()));
        holder.tvBluetoothClass.setText(device.getBluetoothClass().toString());
        holder.tvRSSI.setText(String.valueOf(deviceList.get(position).rssi));
    }

    @Override
    public int getItemCount() {
        return devicesMap.size();
    }

    public void addDevice(BluetoothDevice device, int rssi) {
        if (!devicesMap.containsKey(device.getAddress())) {
            synchronized (this) {
                int pos = deviceList.size();
                BluetoothDeviceExpireble deviceExpireble = new BluetoothDeviceExpireble(device, rssi, pos);
                devicesMap.put(device.getAddress(), deviceExpireble);
                deviceList.add(deviceExpireble);
            }
        } else {
            synchronized (this) {
                BluetoothDeviceExpireble deviceExpireble = devicesMap.get(device.getAddress());
                deviceExpireble.updateTimeStamp();
                deviceExpireble.bluetoothDevice = device;
                deviceExpireble.rssi = rssi;
            }
        }
        notifyDataSetChanged();
    }

    public void clear() {
        devicesMap.clear();
        deviceList.clear();
    }

    public void expireItems() {
        for (BluetoothDeviceExpireble deviceExpireble : devicesMap.values()) {
            if (deviceExpireble.isExpired()) {
                deviceList.remove(deviceExpireble);
                devicesMap.remove(deviceExpireble.bluetoothDevice.getAddress());
            }
        }
        notifyDataSetChanged();
    }

    public class DeviceItem extends RecyclerView.ViewHolder {

        TextView tvDeviceName, tvDeviceAddress, tvDeviceUUID, tvBluetoothClass, tvDeviceType, tvDeviceBondState, tvRSSI;

        public DeviceItem(View itemView) {
            super(itemView);
            tvDeviceName = (TextView) itemView.findViewById(R.id.txt_device_name);
            tvDeviceAddress = (TextView) itemView.findViewById(R.id.txt_device_address);
            tvDeviceUUID = (TextView) itemView.findViewById(R.id.txt_device_uuid);
            tvBluetoothClass = (TextView) itemView.findViewById(R.id.txt_device_bt_class);
            tvDeviceType = (TextView) itemView.findViewById(R.id.txt_device_type);
            tvDeviceBondState = (TextView) itemView.findViewById(R.id.txt_device_bond_state);
            tvRSSI = (TextView) itemView.findViewById(R.id.txt_rssi);
        }
    }

    public class BluetoothDeviceExpireble {

        BluetoothDevice bluetoothDevice;
        long created;
        int rssi;
        int position;

        public BluetoothDeviceExpireble(BluetoothDevice bluetoothDevice, int rssi, int position) {
            this.bluetoothDevice = bluetoothDevice;
            this.rssi = rssi;
            this.position = position;
            this.created = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return this.created + EXPIRE_TIME < System.currentTimeMillis();
        }

        public void updateTimeStamp() {
            this.created = System.currentTimeMillis();
        }
    }
}
