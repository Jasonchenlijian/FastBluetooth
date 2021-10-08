package com.clj.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clj.fastbluetooth.FastBluetooth;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {

    private List<BluetoothDevice> mList = new ArrayList<>();
    private Context mContext;

    public DeviceAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void addData(BluetoothDevice data) {
        this.mList.add(data);
    }

    public void removeData(int position) {
        mList.remove(position);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.adapter_device, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final BluetoothDevice device = mList.get(position);
        if (device != null) {
            boolean isConnected = FastBluetooth.getInstance().isConnected(bleDevice);
            String name = device.getName();
            String mac = device.getAddress();
            holder.txt_name.setText(name);
            holder.txt_mac.setText(mac);
            if (isConnected) {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_connected);
                holder.txt_name.setTextColor(0xFF1DE9B6);
                holder.txt_mac.setTextColor(0xFF1DE9B6);
                holder.layout_idle.setVisibility(View.GONE);
                holder.layout_connected.setVisibility(View.VISIBLE);
            } else {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_remote);
                holder.txt_name.setTextColor(0xFF000000);
                holder.txt_mac.setTextColor(0xFF000000);
                holder.layout_idle.setVisibility(View.VISIBLE);
                holder.layout_connected.setVisibility(View.GONE);
            }
        }

        holder.btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onConnect(device);
                }
            }
        });

        holder.btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDisConnect(device);
                }
            }
        });

        holder.btn_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDetail(device);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView img_blue;
        TextView txt_name;
        TextView txt_mac;
        LinearLayout layout_idle;
        LinearLayout layout_connected;
        Button btn_disconnect;
        Button btn_connect;
        Button btn_detail;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            img_blue = (ImageView) itemView.findViewById(R.id.img_blue);
            txt_name = (TextView) itemView.findViewById(R.id.txt_name);
            txt_mac = (TextView) itemView.findViewById(R.id.txt_mac);
            layout_idle = (LinearLayout) itemView.findViewById(R.id.layout_idle);
            layout_connected = (LinearLayout) itemView.findViewById(R.id.layout_connected);
            btn_disconnect = (Button) itemView.findViewById(R.id.btn_disconnect);
            btn_connect = (Button) itemView.findViewById(R.id.btn_connect);
            btn_detail = (Button) itemView.findViewById(R.id.btn_detail);
        }
    }

    public interface OnDeviceClickListener {
        void onConnect(BluetoothDevice bleDevice);

        void onDisConnect(BluetoothDevice bleDevice);

        void onDetail(BluetoothDevice bleDevice);
    }

    private OnDeviceClickListener mListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }
}
