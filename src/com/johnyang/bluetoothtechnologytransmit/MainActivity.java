package com.johnyang.bluetoothtechnologytransmit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnItemClickListener {

	private TextView tvDevices;
	private BluetoothAdapter mBluetoothAdapter;
	private List<String> mBluetoothDevices = new ArrayList<String>();
	private ListView lvDevices;
	private ArrayAdapter<String> arrayAdapter;
	
	private final UUID MY_UUID = UUID.fromString("db764ac8-4b08-7f25-aafe-59d03c27bae3");
	private final String NAME="Bluetooth_Socket";
	
	private AcceptThread acceptThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		tvDevices = (TextView) findViewById(R.id.tvDevices);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		lvDevices = (ListView) findViewById(R.id.lvDevices);
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if(pairedDevices.size()>0) {
			for (BluetoothDevice device : pairedDevices) {
				 Log.i("info", device.getName()+"==="+device.getAddress()+"\n");
				 mBluetoothDevices.add(device.getName()+":"+device.getAddress()+"\n");
			}
		}  
		arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,android.R.id.text1,mBluetoothDevices);
		lvDevices.setAdapter(arrayAdapter);
		lvDevices.setOnItemClickListener(this);
		
		//搜索到蓝牙的意图过滤器
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(receiver, filter);
		//搜索蓝牙结束
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(receiver, filter);
		
		acceptThread = new AcceptThread();
		acceptThread.start();
		
	}
	
	
	public void onClick(View view) {
		setProgressBarIndeterminateVisibility(true);
		setTitle("正在扫描...");
		//先看看是否正在搜索，如果正在搜索则取消掉。然后再开始搜索
		if(mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
		//开始搜索
		mBluetoothAdapter.startDiscovery();
		
	}
	
	/**
	 * 蓝牙广播接收器
	 */
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				//判断设备是否被绑定，如果被没有被绑定则需要重新添加
				if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
					tvDevices.append(device.getName()+":"+device.getAddress()+"\n");
				}
			}else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarVisibility(false);
				setTitle("搜索完成");
				
			}
			
			
		}
	};
	private BluetoothDevice device;
	private BluetoothSocket clientSocket;
	private OutputStream os;


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String s = arrayAdapter.getItem(position);
		String address = s.substring(s.indexOf(":")+1).trim();
		try {
			if(mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
			
			try {
				if(device == null) {
					//获取远程连接设备
					device = mBluetoothAdapter.getRemoteDevice(address);
					
				}
				if(clientSocket == null) {
					clientSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
					//连接蓝牙
					clientSocket.connect();
					os = clientSocket.getOutputStream();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if(os != null) {
				os.write("发送信息到其他蓝牙设备".getBytes("utf-8"));
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			
			Toast.makeText(MainActivity.this, String.valueOf(msg.obj), Toast.LENGTH_LONG).show();
			super.handleMessage(msg);
			
		};
	};
	
	
	private class AcceptThread extends Thread {
		private BluetoothServerSocket serverSocket;
		private BluetoothSocket socket;
		private OutputStream os;
		private InputStream is;
		
		public AcceptThread() {
			
			try {
				serverSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			try {
				socket = serverSocket.accept();
				 is = socket.getInputStream();
				  os = socket.getOutputStream();
				  while(true) {
					  byte[] buffer = new byte[128];
					  int count = is.read(buffer);
					  Message msg = new Message();
					  msg.obj = new String(buffer, 0, count,"utf-8");
					  handler.sendMessage(msg);
				  }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
}
