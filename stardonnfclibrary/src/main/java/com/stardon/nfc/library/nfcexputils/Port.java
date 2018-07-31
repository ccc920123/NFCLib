package com.stardon.nfc.library.nfcexputils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class Port {
	public static enum PORT_STATE
	{
		PORT_OPEND,
		PORT_CLOSED,
		BT_ADAPTER_NULL,//btAdapter对象为null
		BT_REMOTE_DEVIECE_NULL,
		BT_ADAPTER_ERROR,
		BT_CREAT_RFCOMM_SERVICE_ERROR,
		BT_CONNECT_ERROR,
		BT_SOCKET_CLOSE_ERROR,
		BT_GET_OUT_STREAM_ERROR,
		BT_GET_IN_STREAM_ERROR,
	}
	private byte []cmd ={0};
	private PORT_STATE portState;
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket mmBtSocket;
	private String btDeviceString;
	private OutputStream mmOutStream = null;
	private InputStream mmInStream = null;

	public boolean isOpen = false;

	//构造函数
	public Port(BluetoothAdapter bt_adapter, String bt_device_string)
	{
		if (bt_adapter == null)
		{
			portState = PORT_STATE.BT_ADAPTER_NULL;
			return;
		}
		if (bt_device_string == null)
		{
			portState = PORT_STATE.BT_REMOTE_DEVIECE_NULL;
			return;
		}
		btAdapter = bt_adapter;
		btDeviceString = bt_device_string;
		if ( btAdapter.getState() != BluetoothAdapter.STATE_ON)
		{
			portState = PORT_STATE.BT_ADAPTER_ERROR;
			return ;
		}
		portState = PORT_STATE.PORT_CLOSED;
	}

	//when the object is destroyed , free resources.
	@Override
	protected void finalize() throws Throwable
	{
		super.finalize();
		close();
	}

	public PORT_STATE getState()
	{
		return portState;
	}

	public boolean open()
	{
		if (btAdapter == null || btDeviceString == null)
		{
			isOpen =  false;
			return false;
		}
		BluetoothSocket TmpSock = null;
		try
		{
			BluetoothDevice btDevice = btAdapter.getRemoteDevice(btDeviceString);
			TmpSock = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
		}
		catch (Exception ex)
		{
			TmpSock = null;
			Log.e("JQ", "createRfcommSocketToServiceRecord exception");
			isOpen = false;
			portState = PORT_STATE.BT_REMOTE_DEVIECE_NULL;
			return false;
		}
		finally
		{
			mmBtSocket = TmpSock;
		}
		try {
			mmBtSocket.connect();
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.e("JQ", "connect exception");

			try {
				mmBtSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
				portState = PORT_STATE.BT_SOCKET_CLOSE_ERROR;
			}
			isOpen = false;
			portState = PORT_STATE.BT_CONNECT_ERROR;
			return false;
		}

		try {
			mmOutStream = mmBtSocket.getOutputStream();
		} catch (IOException e) {
			portState = PORT_STATE.BT_GET_OUT_STREAM_ERROR;
			e.printStackTrace();
		}
		try {
			mmInStream = mmBtSocket.getInputStream();
		} catch (IOException e) {
			portState = PORT_STATE.BT_GET_IN_STREAM_ERROR;
			e.printStackTrace();
		}
		isOpen = true;
		portState = PORT_STATE.PORT_OPEND;
		return true;
	}

	public boolean open(int timeout)
	{
		if (btAdapter == null || btDeviceString == null)
		{
			isOpen =  false;
			return false;
		}
		if (timeout < 1000)
			timeout = 1000;
		if (timeout > 6000)
			timeout = 6000;

		long start_time = SystemClock.elapsedRealtime();
		for(;;)
		{
			if (BluetoothAdapter.STATE_ON == btAdapter.getState())
			{
				break;
			}
			if (SystemClock.elapsedRealtime() -start_time > timeout)
			{
				Log.e("JQ", "adapter state on timeout");
				return false;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		BluetoothSocket TmpSock = null;
		try
		{
			BluetoothDevice btDevice = btAdapter.getRemoteDevice(btDeviceString);
			TmpSock = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
		}
		catch (Exception ex)
		{
			TmpSock = null;
			Log.e("JQ", "createRfcommSocketToServiceRecord exception");
			isOpen = false;
			portState = PORT_STATE.BT_REMOTE_DEVIECE_NULL;
			return false;
		}
		mmBtSocket = TmpSock;

		start_time = SystemClock.elapsedRealtime();
		for(;;)
		{
			try
			{
				mmBtSocket.connect();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				Log.e("JQ", "connect exception");

				if (SystemClock.elapsedRealtime() -start_time > timeout)
				{
					try
					{
						mmBtSocket.close();
					}
					catch (IOException e)
					{
						portState = PORT_STATE.BT_SOCKET_CLOSE_ERROR;
						e.printStackTrace();
					}
					isOpen = false;
					Log.e("JQ", "connet timeout");

					portState = PORT_STATE.BT_CONNECT_ERROR;
					return false;
				}

				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			break;
		}

		try {
			mmOutStream = mmBtSocket.getOutputStream();
		} catch (IOException e) {
			portState = PORT_STATE.BT_GET_OUT_STREAM_ERROR;
			e.printStackTrace();
		}
		try {
			mmInStream = mmBtSocket.getInputStream();
		} catch (IOException e) {
			portState = PORT_STATE.BT_GET_IN_STREAM_ERROR;
			e.printStackTrace();
		}
		isOpen = true;
		portState = PORT_STATE.PORT_OPEND;
		Log.e("JQ","connect ok");
		return true;
	}

	public boolean close()
	{
		if(mmBtSocket == null)
		{
			isOpen =  false;
			Log.e("JQ", "mmBtSocket null");
			return false;
		}
		if(isOpen)
		{
			try
			{
				if (mmOutStream != null)
				{
					mmOutStream.close();
					mmOutStream = null;
				}
				if (mmInStream != null)
				{
					mmInStream.close();
					mmOutStream = null;
				}
				mmBtSocket.close(); //SB close会使Socket无效，必须下次使用必须再次createRfcommSocketToServiceRecord来创建
			}
			catch(Exception ex)
			{
				isOpen = false;
				Log.e("JQ", "close exception");
				return false;
			}
		}
		isOpen = false;
		mmBtSocket = null;
		portState = PORT_STATE.PORT_CLOSED;
		return true;
	}

	public boolean flushReadBuffer()
	{
		byte []buffer =  new byte[64];
		if (!isOpen)
			return false;
		while(true)
		{
			int r = 0;
			try {
				r= mmInStream.available();
				if (r==0) break;
				if (r>0)
				{
					if (r>64) r= 64;
					mmInStream.read(buffer, 0, r);
				}
			} catch (IOException e) {}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}

		}
		return true;
	}
	public boolean write(byte[] buffer, int offset, int length)
	{
		if (!isOpen)
			return false;
		if (mmBtSocket == null)
		{
			Log.e("JQ", "mmBtSocket null");
			return false;
		}
		if (mmOutStream==null)
		{
			return false;
		}
		try{mmOutStream.write(buffer, offset, length);}
		catch(Exception ex){return false;}

		return true;
	}

	public boolean write(byte cmd)
	{
		byte [] buffer = {0x00};
		buffer[0] = cmd;
		return write(buffer, 0, 1);
	}

	public boolean write(byte[] buffer, int length)
	{
		if (length > buffer.length)
			return false;
		return write(buffer, 0, length);
	}

	public boolean write(byte[] buffer)
	{
		return write(buffer,0, buffer.length);
	}

	public boolean write(short s)
	{
		byte []buffer = {0,0};
		buffer[0] = (byte)s;
		buffer[1] = (byte)(s>>8);
		return write(buffer,0, buffer.length);
	}

	public boolean writeNULL()
	{
		cmd[0] = 0;
		return write(cmd,0,1);
	}

	public boolean write(String text)
	{
		byte[] data = null;
		try
		{
			data = text.getBytes("GBK");
		}
		catch (UnsupportedEncodingException e)
		{
			Log.e("JQ", "Sting getBytes('GBK') failed");
			return false;
		}
		if (!write(data, 0, data.length))
			return false;
		return writeNULL();
	}

	public boolean read(byte[] buffer, int offset, int length,int timeout_read)
	{
		if (!isOpen)
			return false;
		if (timeout_read < 200) timeout_read = 200;
		if (timeout_read > 5000) timeout_read = 5000;

		try
		{
			long start_time = SystemClock.elapsedRealtime();
			long cur_time = 0;
			int need_read = length;
			int cur_readed = 0;
			for(;;)
			{
				if (mmInStream.available()>0)
				{
					cur_readed = mmInStream.read(buffer, offset, need_read);
					offset += cur_readed;
					need_read -= cur_readed;
				}
				if(need_read == 0)
				{
					break;
				}
				cur_time = SystemClock.elapsedRealtime();
				if (cur_time -start_time > timeout_read)
				{
					Log.e("JQ", "read timeout");
					return false;
				}
				Thread.sleep(20);
			}
		}
		catch(Exception ex)
		{
			Log.e("JQ","read exception");
			close();
			return false;
		}
		return true;
	}

	public boolean read(byte[] buffer, int length,int timeout_read)
	{
		if (length > buffer.length)
			return false;
		return read(buffer,0,length ,timeout_read);
	}
}
