package com.stardon.nfc.library.nfcexputils;

import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.util.Log;

import java.io.IOException;

public class Ntag_Commands {

	/**
	 * current Sector in which the Tag is
	 */
	private byte current_sec;
	private int sector_select_timout;
	private final int timeout = 20;

	private byte[] answer;
	private byte[] command;
	private NfcA nfca;

	/**
	 * Constructor connects the Tag also
	 * 
	 * @return tag tag which should be connected
	 * @throws IOException
	 */
	public Ntag_Commands(Tag tag) throws IOException {
		nfca = NfcA.get(tag);
		sector_select_timout = timeout;
		nfca.setTimeout(timeout);
		current_sec = 0;
	}


	/**
	 * Close the Connection to the Tag
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		nfca.close();
		current_sec = 0;
	}

	/**
	 * Reopens the connection to the Tag
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		nfca.connect();
		current_sec = 0;
	}

	/**
	 * Checks if the tag is still connected
	 */
	public boolean isConnected() {
		return nfca.isConnected();
	}

	/**
	 * Returns Byte Code of last Command
	 * 
	 * @return Byte Code of last Command
	 */
	public byte[] getLastCommand() {
		return command;
	}

	/**
	 * Returns Byte Code of last Answer
	 * 
	 * @return Byte Code of last Answer
	 */
	public byte[] getLastAnswer() {
		return answer;
	}

	/**
	 * Performs a Sector Select if necessary
	 * 
	 * @param sector
	 *            Sector which should be selected
	 * @throws IOException
	 * @throws FormatException
	 */
	public void SectorSelect(byte sector) throws IOException, FormatException {

		// When card is already in this sector do nothing
		if (current_sec == sector)
			return;

		command = new byte[2];
		command[0] = (byte) 0xc2;
		command[1] = (byte) 0xff;

		try{
			nfca.transceive(command);
		}catch(IOException e){
			Log.e("JQ", "SectorSelect io error");
		}

		command = new byte[4];
		command[0] = (byte) sector;
		command[1] = (byte) 0x00;
		command[2] = (byte) 0x00;
		command[3] = (byte) 0x00;

		nfca.setTimeout(sector_select_timout);

		// catch exception, passive ack
		try {
			nfca.transceive(command);
		} catch (IOException e) {
			Log.e("JQ", "SectorSelect passive ack io error");
		}

		nfca.setTimeout(timeout);
		current_sec = sector;
	}

	/**
	 * Writes Data on the Tag
	 * 
	 * @param data
	 *            Data to write
	 * @param blockNr
	 *            Block Number to write
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean write(byte[] data, byte blockNr) throws IOException,
            FormatException {
		// no answer
		answer = new byte[0];

		command = new byte[6];
		command[0] = (byte) 0xA2;
		command[1] = blockNr;
		command[2] = data[0];
		command[3] = data[1];
		command[4] = data[2];
		command[5] = data[3];

		//nfca.transceive(command);
		try{
			nfca.transceive(command);
		}catch(IOException e){
			//Log.e("JQ", "write io error");
			return false;
		}
		return true;
	}

	/**
	 * Performs a Fast Read Command
	 * 
	 * @param startAddr
	 *            Start Address
	 * @param endAddr
	 *            End Address
	 * @return Answer of the Fast Read Command
	 * @throws IOException
	 * @throws FormatException
	 */
	public byte[] fast_read(byte startAddr, byte endAddr) throws IOException,
            FormatException {

		command = new byte[3];

		command[0] = (byte) 0x3A;
		command[1] = (byte) startAddr;
		command[2] = (byte) endAddr;
		try{
			nfca.setTimeout(500);			
			answer = nfca.transceive(command);
			nfca.setTimeout(timeout);
			return answer;
		}catch(IOException e){
			Log.e("JQ", "fast_read io error");
			return null;
		}		
	}

	/**
	 * Performs a Read Command
	 * 
	 * @param blockNr
	 *            Block Number to begin Read
	 * @return Answer of the Read (always 16Byte)
	 * @throws IOException
	 * @throws FormatException
	 */
	public byte[] read(byte blockNr) throws IOException, FormatException {
		command = new byte[2];
		command[0] = (byte) 0x30;
		command[1] = blockNr;

		try{
			answer = nfca.transceive(command);
			return answer;
		}catch(IOException e){
			Log.e("JQ", "read io error");
			return null;
		}		
	}

	/**
	 * Performs a Get Version Command
	 * 
	 * @return Get Version Response
	 * @throws IOException
	 */
	public byte[] getVersion() throws IOException {
		command = new byte[1];
		command[0] = (byte) 0x60;
		try{
			answer = nfca.transceive(command);
			return answer;
		}catch(IOException e){
			Log.e("JQ", "getVersion io error");
			return null;
		}	
	}

	/**
	 * returns the maximum Transceive length
	 * 
	 * @return Maximum Transceive length
	 */
	public int getMaxTransceiveLength() {
		return nfca.getMaxTransceiveLength();
	}
}
