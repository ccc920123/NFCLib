package com.stardon.nfc.library.nfcexputils;

public class FrameParam {
	public FrameParam() {
		printerState = new byte[2];
	}
	public byte id;
	public Nfc_frame.FRAME_CMD cmd;
	public int request_param0;
	public int request_param1;
	/**
	 * 发送数据
	 */
	public byte []sendData;
	/**
	 * 发送数据的长度
	 */
	public int sendDataLength;
	/**
	 * 发送数据开始位置
	 */
	public int sendDataStart;
	public int sendLoop;//发送重试次数
	/*
	 * 一次发送数据最大值
	 */
	final public int sendDataBlockWithAckMax = 64;
	final public int sendDataBlockWithoutAckMax = 2048;
	
	public byte []recvData = new byte[512];
	public int recvDataLen;
	public int recvBlockTimeout;
	
	public int frameState;
	public boolean bPrinterState;
	public byte []printerState;
}