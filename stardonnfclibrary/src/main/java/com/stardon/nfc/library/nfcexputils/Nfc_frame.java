package com.stardon.nfc.library.nfcexputils;

import android.nfc.FormatException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Nfc_frame {
	final int frameHeadLen = 22;
	public enum FRAME_CMD{
		SETUP,
		MASTER_SEND_WITHOUT_RSPONSE,//手机发送数据并且不需要应答
		MASTER_SEND_WITH_RSPONSE,//手机发送数据,需要应答
	}
	private Nfc_const.R_W_Methods method;
	public String message;
	private NFC_Enabled_Commands reader;
	private int blockSize;
	byte[] cmds;
	public FrameParam param;
	public Nfc_frame(NFC_Enabled_Commands reader, Nfc_const.R_W_Methods method){
		this.reader = reader;
		blockSize = reader.getSRAMSize();
		this.method = method;
		cmds = new byte[blockSize];
		param = new FrameParam();
	}
	/**
	 * 
	 * @param cmd
	 * @return
	 */
	private boolean write(byte []cmd){
		try {
			reader.writeSRAMBlock(cmd);			
		} catch (Exception e) {
			message = "requstSend writeSRAMBlock err";
			try {
				reader.waitforI2Cread(100);
			} catch (IOException e2) {
			} catch (FormatException e2) {
			} catch (TimeoutException e2) {
			}
			try {						
				reader.writeSRAMBlock(cmd);
			} catch (Exception e1) {
				message = "requstSend 重新 writeSRAMBlock err";
				return false;
			}
		}	
		return true;
	}
	/**
	 * 等待写结束
	 * @param timeout
	 * @return
	 */
	private boolean waitWriteFinished(final int timeout){
		if (method == Nfc_const.R_W_Methods.Polling_Mode) {
			try {
				reader.waitforI2Cread(timeout);
			} catch (IOException e) {
				message = "waitWriteFinished io err";
				return false;
			} catch (FormatException e) {
				message = "waitWriteFinished format err";
				return false;
			} catch (TimeoutException e) {
				message = "waitWriteFinished format timeout";
				return false;
			}
		} else {
			try {
				Thread.sleep(6);
			} catch (Exception e) {
			}
		}
		return true;
	}	
	/**
	 * 
	 * @param timeout
	 */
	private void sleep(final int timeout)
	{
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}		
	}
	/**
	 * 等待读结束
	 * @param timeout1
	 * @return
	 */
	private boolean waitReadFinished(final int timeout0, final int timeout1){
		if (method == Nfc_const.R_W_Methods.Polling_Mode) {
			try {
				reader.waitforI2Cwrite(timeout0);
			} catch (IOException e) {
				message = "waitReadFinished io err";
				return false;
			} catch (FormatException e) {
				message = "waitReadFinished format err";
				return false;
			} catch (TimeoutException e) {
				message = "waitReadFinished format timeout";
				return false;
			}
		} else {
			try {
				Thread.sleep(timeout1);
			} catch (Exception e) {
			}
		}
		return true;
	}	
	
	/**
	 * 
	 * @return
	 */
	private byte[] read(){
		byte[] temp = null;
		try {
			temp = reader.readSRAMBlock();
		} catch (IOException e) {
			message = "read readSRAMBlock IO err";
		} catch (FormatException e) {
			message = "read readSRAMBlock formata err";
		}
		/*catch (TimeoutException e) {
			message = "read timeout err";
		}*/
		if (temp == null){
			message = "read unknown err";
		}
		return temp;
	}
			
	/**
	 * 发送块数据
	 * @param param
	 * @param index, 数据开始位置
	 * @param length，数据长度
	 * @return
	 */
	private boolean sendBlock(FrameParam param , final int index, final int length){
		final int len;
		int start = 0;
		
		//发送数据
		if (param.sendData == null)
			len = 0;		
		else{ 
			len = length;
			start = param.sendDataStart + index;
		}
		
		//head
		cmds[0] = (byte) 0xFE;
		cmds[1] = (byte) 0xFE;
		//id
		cmds[2] = (byte) param.id;
		cmds[3] = (byte) (param.id ^ 0xFF);
		//cmd
		cmds[4] = (byte) param.cmd.ordinal();// 
		cmds[5] = (byte) (cmds[4] ^ 0xFF);
		//crc
		int crc16 = Crc.crc16(param.sendData, start, len);		
		cmds[6] = (byte) crc16;
		cmds[7] = (byte) (crc16 >> 8 );
		cmds[8] = (byte) (cmds[6] ^ 0xFF);
		cmds[9] = (byte) (cmds[7] ^ 0xFF);
		//p0
		cmds[10] = (byte) param.request_param0;
		cmds[11] = (byte) (param.request_param0 >> 8 );
		cmds[12] = (byte) (cmds[10] ^ 0xFF);
		cmds[13] = (byte) (cmds[11] ^ 0xFF);
		// p1
		cmds[14] = (byte) param.request_param1;
		cmds[15] = (byte) (param.request_param1 >> 8);
		cmds[16] = (byte) (cmds[14] ^ 0xFF);
		cmds[17] = (byte) (cmds[15] ^ 0xFF);
		
		//len
		cmds[18] = (byte) len;
		cmds[19] = (byte) (len >> 8 );
		cmds[20] = (byte) (cmds[18] ^ 0xFF);
		cmds[21] = (byte) (cmds[19] ^ 0xFF);
		
		int allDataLen = len + frameHeadLen;
		int blockCount = (allDataLen -1)/blockSize + 1; //需要分成多少个块来发送
		
		for (int i = 0, left = len, sended = 0; i< blockCount; i++){
			if (i == 0){//第一个block
				if (blockCount > 1)
					sended = blockSize - frameHeadLen;
				else
					sended = len;	
				
				for (int j = 0; j < sended; j++)
					cmds[frameHeadLen + j] = param.sendData[start + j];			
			}
			else{ //剩下的块
				if (left >= blockSize)
					sended = blockSize;
				else
					sended = left;
				
				for (int j = 0; j <  sended; j++){
					cmds[j] = param.sendData[start + j];
				}
			}			
			left -= sended;
			start += sended;
			//NfcFragment.displaynomove("block 发送中..." + i);
			//NfcFragment.setDatarateCallback("block 发送中..." + i);	
			
			if (!write(cmds)){ 
				//NfcFragment.setDatarateCallback("block 发送错误:" + message);
				return false;
			}
			if(!waitWriteFinished(100)){
				message = "block requstSend waitWriteFinished 超时";
				return false;
			}
			sleep(10);
		}
		return true;
	}
	
	/**
	 * 解码帧数据
	 * @param frame
	 * @return
	 */
	private boolean frameDecode(byte [] frame){
		int crc16;
		//head
		if (frame[0] != (byte)0xFE){
			//NfcFragment.setDatarateCallback("head err");
			return false;
		}
		if (frame[1] != (byte)0xFE){
			//NfcFragment.setDatarateCallback("head err");
			return false;
		}
		//id
		if (frame[2] != (byte)(frame[3] ^ 0xFF)){
			//NfcFragment.setDatarateCallback("id verfiy err :" + frame[2] + " " + frame[3]);
			return false;
		}
		if (frame[2] != param.id){
			//NfcFragment.setDatarateCallback("id err");
			return false;
		}
		// cmd
		if (frame[4] != (byte) (frame[5] ^ 0xFF)) {
			//NfcFragment.setDatarateCallback("cmd verfiy err");
			return false;
		}
		if (frame[4] != (byte)(param.cmd.ordinal())) {
			//NfcFragment.setDatarateCallback("cmd err");
			return false;
		}
		// crc16
		if ((frame[6] != (byte) (frame[8] ^ 0xFF)) || (frame[7] != (byte) (frame[9] ^ 0xFF))) {
			//NfcFragment.setDatarateCallback("crc verfiy err :"+ frame[6] + ":" + frame[7]+ ":" + frame[8]+ " " + frame[9]);
			return false;
		}
		crc16 = (frame[6] & 0xFF) | ( (frame[7] & 0xFF) << 8);
		// frame state
		if ((frame[10] != (byte) (frame[12] ^ 0xFF)) || (frame[11] != (byte) (frame[13] ^ 0xFF))) {
			//NfcFragment.setDatarateCallback("frame state verfiy err");
			return false;
		}
		param.frameState = (frame[10] & 0xFF) | ((frame[11] & 0xFF) << 8);
		// printer state
		if ((frame[14] != (byte) (frame[16] ^ 0xFF)) || (frame[15] != (byte) (frame[17] ^ 0xFF))) {
			//NfcFragment.setDatarateCallback("printer state verfiy err");
			return false;
		}
		if ((frame[15] & 0x80) != 0){
			param.bPrinterState = true;
			param.printerState[0] = frame[14];
			param.printerState[1] = (byte)(frame[15] & 0x7F);
		} else {
			param.bPrinterState = false;
		}
		// len
		if ((frame[18] != (byte) (frame[20] ^ 0xFF)) || (frame[19] != (byte) (frame[21] ^ 0xFF))) {
			//NfcFragment.setDatarateCallback("len verfiy err");
			return false;
		}
		param.recvDataLen = (frame[18] & 0xFF) | ( (frame[19] & 0xFF) << 8);
		
		if(param.recvDataLen > 0){
			for (int i = 0; i < param.recvDataLen; i++){
				param.recvData[i] = frame[frameHeadLen + i];			
			}
			int crcCalc = Crc.crc16(param.recvData, 0, param.recvDataLen);
			if (crcCalc != crc16){
				//NfcFragment.setDatarateCallback("data crc verfiy err ");
				return false;
			}
		}		
		return true;
	}	
	/**
	 * 接受返回数据
	 * @return
	 */
	private boolean recvBlock(final int timeout){
		sleep(timeout);
		if (!waitReadFinished(300, param.request_param1)){
			//NfcFragment.setDatarateCallback("block waitReadFinished err");
			return false;
		}
		
		byte [] temp = read();
		if (temp == null){
			//NfcFragment.setDatarateCallback("read temp null err:"+ message);
			return false;
		}
		if (!frameDecode(temp)) return false;
		if (param.frameState != 0){ 
			//NfcFragment.setDatarateCallback("返回状态 错误:"+ param.frameState );
			return  false;
		}
		if (param.recvDataLen >0 )
		{
			//NfcFragment.setDatarateCallback("接收数据:"+ Util.toHexString(param.recvData, 0, param.recvDataLen));
		}
		return true;
	}	
	/**
	 * 发送块数据
	 * 发送数据并结束返回状态。如果返回错误，重新发送
	 * @param
	 * @param index ,数据开始位置
	 * @param length，数据长度
	 * @return
	 */
	private boolean sendAndRecvBlock(final int index, final int length){
		int i = 0;
		for( ; i < param.sendLoop; i++){
			if (sendBlock(param, index, length)) {
				break;//发送成功
			}
			else{
				//NfcFragment.setDatarateCallback("sr 发送失败，重试 "+ i);
				continue;
			}
		}
		if (i >= param.sendLoop){
			//NfcFragment.setDatarateCallback("sr 发送最终失败");
			return false;
		}
		if (!recvBlock(param.request_param1)) {
			//NfcFragment.setDatarateCallback("sr 接收失败");
			return false;
		}
		return true;
	}
	
	/**
	 * 发送所有数据，
	 * 数据长度如果一次发送数据最大值，会分成几次发送
	 * @param data ：发送数据
	 * @param start :发送数据开始位置
	 * @param dataLen :需要发送数据长度
	 * @return
	 */
	public boolean sendDataWithAck(final byte[] data, final int start, final int dataLen, final int timeout){
		param.sendData = data;
		
		param.sendDataStart = start;
		param.sendDataLength = dataLen;
		if (param.sendData == null){
			param.sendDataLength = 0;
		}
		param.sendLoop = 5;
		param.cmd = FRAME_CMD.MASTER_SEND_WITH_RSPONSE;
		param.request_param0 = timeout;
		param.request_param1 = 1; 
		int sended;
	    int blockCount;
	    if (param.sendDataLength == 0){
	    	blockCount = 1;
	    }
	    else {
	    	blockCount = (param.sendDataLength -1) / param.sendDataBlockWithAckMax + 1;
	    }
		for (int left = param.sendDataLength, index = 0, id = 0; blockCount > 0; left -= sended, blockCount--){
			if (left >= param.sendDataBlockWithAckMax){
				sended = param.sendDataBlockWithAckMax;
			}else{
				sended = left;
			}
			param.id = (byte)id;
			if (!sendAndRecvBlock(index, sended)) return false;
			index += sended;
			
		}
		return true;
	}
	
	private boolean sendBlockOnly(final int index, final int length){
		int i = 0;
		for( ; i < param.sendLoop; i++){
			if (sendBlock(param, index, length)) {
				break;//发送成功
			}
			else{
				//NfcFragment.setDatarateCallback("sr 发送失败，重试 "+ i);
				continue;
			}
		}
		if (i >= param.sendLoop){
			//NfcFragment.setDatarateCallback("sr 发送最终失败");
			return false;
		}
		return true;
	}
	
	public boolean sendDataWithoutAck(final byte[] data, final int start, final int dataLen){
		param.sendData = data;
		
		param.sendDataStart = start;
		param.sendDataLength = dataLen;
		if (param.sendData == null){
			param.sendDataLength = 0;
		}
		param.sendLoop = 5;
		param.cmd = FRAME_CMD.MASTER_SEND_WITHOUT_RSPONSE;
		int sended;
	    int blockCount;
	    if (param.sendDataLength == 0){
	    	blockCount = 1;
	    }
	    else {
	    	blockCount = (param.sendDataLength -1) / param.sendDataBlockWithoutAckMax + 1;
	    }
		for (int left = param.sendDataLength, index = 0, id = 0; blockCount > 0; left -= sended, blockCount--){
			if (left >= param.sendDataBlockWithoutAckMax){
				sended = param.sendDataBlockWithoutAckMax;
			}else{
				sended = left;
			}
			param.id = (byte)id;
			if (!sendBlockOnly(index, sended)) return false;
			index += sended;
			
		}
		return true;
	}
}
