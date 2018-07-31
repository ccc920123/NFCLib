package com.stardon.nfc.library.nfcexputils;

import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

public class NFC_Enabled_Commands {
	Ntag_Commands reader;
	byte[] answer;
	Ntag_Get_Version get_version_response;
	boolean TimeOut = false;
	Object lock = new Object();
	byte sram_sector;
	
	public NFC_Enabled_Commands(Tag tag) throws IOException {
		BlockSize = 4;
		SRAMSize = 64;
		this.reader = new Ntag_Commands(tag);
		connect();
		if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_2k) {
			sram_sector = 1;
		} else
			sram_sector = 0;
		close();
	}

	public static NFC_Enabled_Commands get(Tag tag) throws IOException,
            InterruptedException {
		byte[] answer;
		byte[] command = new byte[2];

		NfcA nfca = NfcA.get(tag);
		Ntag_Get_Version.Prod prod;

		// Check for support of setTimout to be able to send an efficient
		// sector_select - select minimal implementation if not supported
		nfca.setTimeout(20);
		// check for timeout
		if (nfca.getTimeout() < 50) {
			// check if GetVersion is supported
			try {
				nfca.connect();
				command = new byte[1];
				command[0] = (byte) 0x60;
				answer = nfca.transceive(command);
				prod = (new Ntag_Get_Version(answer)).Get_Product();
				nfca.close();
				if (prod == Ntag_Get_Version.Prod.NTAG_I2C_1k || prod == Ntag_Get_Version.Prod.NTAG_I2C_2k)
					return new NFC_Enabled_Commands(tag);
			} catch (Exception e) {
				Log.e("JQ", "I2C_Enabled_Commands io err");
				nfca.close();
				// check if sector_select is supported
				try {
					nfca.connect();
					command = new byte[2];
					command[0] = (byte) 0xC2;
					command[1] = (byte) 0xFF;
					answer = nfca.transceive(command);

					nfca.close();
					return new NFC_Enabled_Commands(tag);
				} catch (Exception e2) {
					Log.e("JQ", "I2C_Enabled_Commands retry io err");
					nfca.close();
				}
			}
		}
		return null;
	}

	protected int SRAMSize;

	public int getSRAMSize() {
		return SRAMSize;
	}

	protected int BlockSize;

	public int getBlockSize() {
		return BlockSize;
	}



	/**
	 * Bits of the NS_REG Register
	 * 
	 */
	public enum NS_Reg_Func {
		RF_FIELD_PRESENT((byte) (0x01 << 0)), EEPROM_WR_BUSY((byte) (0x01 << 1)), EEPROM_WR_ERR(
				(byte) (0x01 << 2)), SRAM_RF_READY((byte) (0x01 << 3)), SRAM_I2C_READY(
				(byte) (0x01 << 4)), RF_LOCKED((byte) (0x01 << 5)), I2C_LOCKED(
				(byte) (0x01 << 6)), NDEF_DATA_READ((byte) (0x01 << 7)), ;

		byte value;

		private NS_Reg_Func(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}
	}

	/**
	 * Bits of the NC_REG Register
	 * 
	 */
	public enum NC_Reg_Func {
		PTHRU_DIR((byte) (0x01 << 0)), SRAM_MIRROR_ON_OFF((byte) (0x01 << 1)), FD_ON(
				(byte) (0x03 << 2)), FD_OFF((byte) (0x03 << 4)), PTHRU_ON_OFF(
				(byte) (0x01 << 6)), I2C_RST_ON_OFF((byte) (0x01 << 7)), ;

		byte value;

		private NC_Reg_Func(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}
	}

	/**
	 * Offset of the Config Registers
	 * 
	 */
	public enum CR_Offset {
		NC_REG((byte) 0x00), LAST_NDEF_PAGE((byte) 0x01), SM_REG((byte) 0x02), WDT_LS(
				(byte) 0x03), WDT_MS((byte) 0x04), I2C_CLOCK_STR((byte) 0x05), REG_LOCK(
				(byte) 0x06), FIXED((byte) 0x07);

		byte value;

		private CR_Offset(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}
	}

	/**
	 * Offset of the Session Registers
	 * 
	 */
	public enum SR_Offset {
		NC_REG((byte) 0x00), LAST_NDEF_PAGE((byte) 0x01), SM_REG((byte) 0x02), WDT_LS(
				(byte) 0x03), WDT_MS((byte) 0x04), I2C_CLOCK_STR((byte) 0x05), NS_REG(
				(byte) 0x06), FIXED((byte) 0x07);

		byte value;

		private SR_Offset(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}
	}

	/**
	 * Closes the connection
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		reader.close();
	}

	/**
	 * reopens the connection
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		reader.connect();
	}

	/**
	 * reopens the connection
	 * 
	 * @throws IOException
	 */
	public boolean isConnected() {
		return reader.isConnected();
	}

	/**
	 * returns the last answer as Byte Array
	 * 
	 * @return Byte Array of the last Answer
	 */
	public byte[] getLastAnswer() {
		return answer;
	}

	/**
	 * Gets the Product of the current Tag
	 * 
	 * @return Product of the Tag
	 * @throws IOException
	 */
	public Ntag_Get_Version.Prod getProduct() throws IOException {
		if (get_version_response == null) {
			try {
				get_version_response = new Ntag_Get_Version(reader.getVersion());
			} catch (Exception e) {
				try {
					reader.close();
					reader.connect();
					byte[] temp = reader.read((byte) 0x00);
					if (temp[0] == (byte) 0x04 && temp[12] == (byte) 0xE1
							&& temp[13] == (byte) 0x10
							&& temp[14] == (byte) 0x6D
							&& temp[15] == (byte) 0x00) {

						temp = reader.read((byte) 0xE8);
						get_version_response = Ntag_Get_Version.NTAG_I2C_1k;

					} else if (temp[0] == (byte) 0x04
							&& temp[12] == (byte) 0xE1
							&& temp[13] == (byte) 0x10
							&& temp[14] == (byte) 0xEA
							&& temp[15] == (byte) 0x00) {
						get_version_response = Ntag_Get_Version.NTAG_I2C_2k;
					}
				} catch (FormatException e2) {
					reader.close();
					reader.connect();
					e2.printStackTrace();
					get_version_response = Ntag_Get_Version.NTAG_I2C_1k;
				}

			}
		}
		return get_version_response.Get_Product();
	}

	/**
	 * Gets all Session Registers as Byte Array
	 * 
	 * @return all Session Registers
	 * @throws IOException
	 * @throws FormatException
	 * @throws
	 */
	public byte[] getSessionRegisters() throws IOException, FormatException {
		reader.SectorSelect((byte) 3);
		byte[] temp = reader.read(Ntag_I2C_Commands.Register.Session.getValue());
		
		return temp;
	}

	public byte getSessionRegister(SR_Offset off) throws IOException,
            FormatException {
		byte[] register = getSessionRegisters();
		return register[off.getValue()];
	}

	
	/**
	 * Checks if the Phone can write in the SRAM when PT is enabled
	 */
	public Boolean checkPTwritePossible() throws IOException, FormatException {
		reader.SectorSelect((byte) 3);

		byte nc_reg = getSessionRegister(SR_Offset.NC_REG);
		if ((nc_reg & NC_Reg_Func.PTHRU_ON_OFF.getValue()) == 0
				|| (nc_reg & NC_Reg_Func.PTHRU_DIR.getValue()) == 0)
			return false;

		byte ns_reg = getSessionRegister(SR_Offset.NS_REG);
		if ((ns_reg & NS_Reg_Func.RF_LOCKED.getValue()) == 0)
			return false;

		return true;
	}

	/**
	 * Waits till the I2C has written in the SRAM
	 */
	public void waitforI2Cwrite(int timeoutMS) throws IOException,
            FormatException, TimeoutException {
		reader.SectorSelect((byte) 3);

		TimeOut = false;

		// interrupts the wait after timoutMS milliseconds
		Timer mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				synchronized (lock) {
					TimeOut = true;
				}
			}
		}, timeoutMS);

		// if SRAM_RF_RDY is set the Reader can Read
		while ((getSessionRegister(SR_Offset.NS_REG) & NS_Reg_Func.SRAM_RF_READY
				.getValue()) == 0) {
			synchronized (lock) {
				if (TimeOut)
					throw new TimeoutException("waitforI2Cwrite had a Timout");
			}
		}

		mTimer.cancel();
		synchronized (lock) {
			TimeOut = true;
		}
		return;
	}

	/**
	 * Waits till the I2C has read the SRAM
	 */
	public void waitforI2Cread(int timeoutMS) throws IOException, FormatException, TimeoutException {
		reader.SectorSelect((byte) 3);
		
		TimeOut = false;
		
		// interrupts the wait after timoutMS milliseconds
		Timer mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				synchronized (lock) {
					TimeOut = true;
				}
			}
		}, timeoutMS);

		// if SRAM_I2C_READY is set the Reader can write
		while (((getSessionRegister(SR_Offset.NS_REG) & NS_Reg_Func.SRAM_I2C_READY
				.getValue()) == NS_Reg_Func.SRAM_I2C_READY.getValue())) {
			if (TimeOut)
				throw new TimeoutException("waitforI2Cread had a Timout");
		}
		
		mTimer.cancel();
		synchronized (lock) {
			TimeOut = true;
		}

		return;
	}

	public boolean writeSRAMBlock(byte[] data) throws IOException, FormatException {
		byte[] TxBuffer = new byte[4];
		int index = 0;

		reader.SectorSelect(sram_sector);

		for (int i = 0; i < 16; i++) {
			for (int d_i = 0; d_i < 4; d_i++) {
				if (index < data.length)
					TxBuffer[d_i] = data[index++];
				else
					TxBuffer[d_i] = (byte) 0x00;
			}

			reader.write(TxBuffer, (byte) (Ntag_I2C_Commands.Register.SRAM_Begin.getValue() + i));
		}
		return true;
	}
	
	public void writeSRAM(byte[] data, Nfc_const.R_W_Methods method) throws IOException,
            FormatException, TimeoutException {

		int Blocks = (int) Math.ceil(data.length / 64.0);
		for (int i = 0; i < Blocks; i++) {

			writeSRAMBlock(data);
			if (method == Nfc_const.R_W_Methods.Polling_Mode) {
				waitforI2Cread(100);
			} else {
				try {
					// else wait
					Thread.sleep(6);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (data.length > 64)
				data = Arrays.copyOfRange(data, 64, data.length);
		}
	}
	
	public byte[] readSRAMBlock() throws IOException, FormatException {

		answer = new byte[0];
		reader.SectorSelect(sram_sector);
		answer = reader.fast_read((byte) 0xF0, (byte) 0xFF);

		return answer;
	}
	
	public byte[] readSRAM(int blocks, Nfc_const.R_W_Methods method) throws IOException,
            FormatException, TimeoutException {
		byte[] response = new byte[0];
		byte[] temp;

		for (int i = 0; i < blocks; i++) {
			if (method == Nfc_const.R_W_Methods.Polling_Mode) {
				waitforI2Cwrite(100);
			} else {
				try {
					// else wait
					Thread.sleep(6);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			temp = readSRAMBlock();

			// concat read block to the full response
			response = concat(response, temp);
		}
		answer = response;
		return response;
	}

	protected byte[] concat(byte[] one, byte[] two) {
		if (one == null)
			one = new byte[0];
		if (two == null)
			two = new byte[0];

		byte[] combined = new byte[one.length + two.length];

		System.arraycopy(one, 0, combined, 0, one.length);
		System.arraycopy(two, 0, combined, one.length, two.length);

		return combined;
	}
}