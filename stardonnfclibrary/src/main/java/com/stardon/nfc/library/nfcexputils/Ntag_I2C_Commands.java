package com.stardon.nfc.library.nfcexputils;

import android.annotation.SuppressLint;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import static com.stardon.nfc.library.nfcexputils.Ntag_Get_Version.NTAG_I2C_1k;
import static com.stardon.nfc.library.nfcexputils.Ntag_Get_Version.NTAG_I2C_2k;


public class Ntag_I2C_Commands {
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
	
	private int BlockSize;

	public int getBlockSize() {
		return BlockSize;
	}
	
	private int SRAMSize;

	public int getSRAMSize() {
		return SRAMSize;
	}

	
	Ntag_Commands reader;
	byte[] answer;
	byte[] session_registers;
	Ntag_Get_Version get_version_response;
	byte sram_sector;
	boolean TimeOut = false;
	Object lock = new Object();

	/**
	 * Special Registers of the NTAG I2C
	 * 
	 */
	public enum Register {
		Session((byte) 0xF8), Configuration((byte) 0xE8), SRAM_Begin(
				(byte) 0xF0), User_memory_Begin((byte) 0x04), UID((byte) 0x00);

		byte value;

		private Register(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}
	}

	// ---------------------------------------------------------------------------------
	// Begin Public Functions
	// ---------------------------------------------------------------------------------

	/**
	 * Constructor
	 * 
	 * @param tag
	 *            Tag to connect
	 * @throws IOException
	 */
	public Ntag_I2C_Commands(Tag tag) throws IOException {
		BlockSize = 4;
		SRAMSize = 64;
		this.reader = new Ntag_Commands(tag);
		connect();
		if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_2k)
			sram_sector = 1;
		else
			sram_sector = 0;
		close();

	}
	
	@SuppressLint("MissingPermission")
	public static Ntag_I2C_Commands get(Tag tag) throws IOException,
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
					return new Ntag_I2C_Commands(tag);
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
					return new Ntag_I2C_Commands(tag);
				} catch (Exception e2) {
					Log.e("JQ", "I2C_Enabled_Commands retry io err");
					nfca.close();
				}
			}
		}
		return null;
	}


	public void close() throws IOException {
		reader.close();
	}

	public void connect() throws IOException {
		reader.connect();
	}

	public boolean isConnected() {
		return reader.isConnected();
	}

	public byte[] getLastAnswer() {
		return answer;
	}

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
						get_version_response = NTAG_I2C_1k;

					} else if (temp[0] == (byte) 0x04
							&& temp[12] == (byte) 0xE1
							&& temp[13] == (byte) 0x10
							&& temp[14] == (byte) 0xEA
							&& temp[15] == (byte) 0x00) {
						get_version_response = NTAG_I2C_2k;
					}
				} catch (FormatException e2) {
					reader.close();
					reader.connect();
					e2.printStackTrace();
					get_version_response = NTAG_I2C_1k;
				}

			}
		}
		return get_version_response.Get_Product();
	}

	public byte[] getSessionRegisters() throws IOException, FormatException {
		reader.SectorSelect((byte) 3);
		return reader.read(Register.Session.getValue());
	}

	public byte[] getConfigRegisters() throws IOException, FormatException {

		if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_1k)
			reader.SectorSelect((byte) 0);
		else if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_2k)
			reader.SectorSelect((byte) 1);
		else
			throw new IOException();

		return reader.read(Register.Configuration.getValue());
	}

	public byte getConfigRegister(CR_Offset off) throws IOException,
            FormatException {
		byte[] register = getConfigRegisters();
		return register[off.getValue()];
	}

	public byte getSessionRegister(SR_Offset off) throws IOException,
            FormatException {
		byte[] register = getSessionRegisters();
		return register[off.getValue()];
	}

	public void writeConfigRegisters(byte NC_R, byte LD_R, byte SM_R,
			byte WD_LS_R, byte WD_MS_R, byte I2C_CLOCK_STR) throws IOException,
            FormatException {
		byte[] Data = new byte[4];

		if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_1k)
			reader.SectorSelect((byte) 0);
		else if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_2k)
			reader.SectorSelect((byte) 1);
		else
			throw new IOException();

		// Write the Config Regs
		Data[0] = NC_R;
		Data[1] = LD_R;
		Data[2] = SM_R;
		Data[3] = WD_LS_R;
		reader.write(Data, Register.Configuration.getValue());

		Data[0] = WD_MS_R;
		Data[1] = I2C_CLOCK_STR;
		Data[2] = (byte) 0x00;
		Data[3] = (byte) 0x00;
		reader.write(Data, (byte) (Register.Configuration.getValue() + 1));
	}

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

	public void writeEEPROM(byte[] data) throws IOException, FormatException {
		if (data.length > getProduct().getMemsize()) {
			throw new IOException("Data is to long");
		}

		reader.SectorSelect((byte) 0);
		byte[] temp;
		int Index = 0;
		byte BlockNr = Register.User_memory_Begin.getValue();

		// write till all Data is written or the Block 0xFF was written(BlockNr
		// should be
		// 0 then, because of the type byte)
		for (Index = 0; Index < data.length && BlockNr != 0; Index += 4) {
			temp = Arrays.copyOfRange(data, Index, Index + 4);
			reader.write(temp, BlockNr);
			BlockNr++;
		}

		// If Data is left write to the 1. Sector
		if (Index < data.length) {
			reader.SectorSelect((byte) 1);
			BlockNr = 0;

			for (; Index < data.length; Index += 4) {
				temp = Arrays.copyOfRange(data, Index, Index + 4);
				reader.write(temp, BlockNr);
				BlockNr++;
			}
		}
	}

	public void writeEEPROM(int startAddr, byte[] data) throws IOException,
            FormatException {

		if ((startAddr & 0x100) != 0x000 && (startAddr & 0x200) != 0x100) {
			throw new FormatException("Sector not supported");
		}

		reader.SectorSelect((byte) ((startAddr & 0x200) >> 16));
		byte[] temp;
		int Index = 0;
		byte BlockNr = (byte) (startAddr & 0xFF);

		// write till all Data is written or the Block 0xFF was written(BlockNr
		// should be
		// 0 then, because of the type byte)
		for (Index = 0; Index < data.length && BlockNr != 0; Index += 4) {
			temp = Arrays.copyOfRange(data, Index, Index + 4);
			reader.write(temp, BlockNr);
			BlockNr++;
		}

		// If Data is left write and the first Sector was not already written
		// switch to the first
		if (Index < data.length && (startAddr & 0x100) != 0x100) {
			reader.SectorSelect((byte) 1);
			BlockNr = 0;
			for (; Index < data.length; Index += 4) {
				temp = Arrays.copyOfRange(data, Index, Index + 4);
				reader.write(temp, BlockNr);
				BlockNr++;
			}
		} else if ((startAddr & 0x100) == 0x100) {
			throw new IOException("Data is to long");
		}
	}
	
	private byte[] concat(byte[] one, byte[] two) {
		if (one == null)
			one = new byte[0];
		if (two == null)
			two = new byte[0];

		byte[] combined = new byte[one.length + two.length];

		System.arraycopy(one, 0, combined, 0, one.length);
		System.arraycopy(two, 0, combined, one.length, two.length);

		return combined;
	}


	public byte[] readEEPROM(int absStart, int absEnd) throws IOException,
            FormatException {

		int maxfetchsize = reader.getMaxTransceiveLength();
		int max_fast_read = (maxfetchsize - 2) / 4;
		int fetch_start = absStart;
		int fetch_end = 0;
		byte data[] = null;
		byte temp[] = null;

		reader.SectorSelect((byte) 0);

		while (fetch_start <= absEnd) {
			fetch_end = fetch_start + max_fast_read - 1;

			// check for last read, fetch only rest
			if (fetch_end > absEnd)
				fetch_end = absEnd;

			// check for sector change in between and reduce fast_read to stay
			// within sector
			if ((fetch_start & 0xFF00) != (fetch_end & 0xFF00))
				fetch_end = (fetch_start & 0xFF00) + 0xFF;

			// fetch data
			temp = reader.fast_read((byte) (fetch_start & 0x00FF),
					(byte) (fetch_end & 0x00FF));
			data = concat(data, temp);

			// calculate next fetch_start
			fetch_start = fetch_end + 1;

			// check for sector select needed
			if ((fetch_start & 0xFF00) != (fetch_end & 0xFF00))
				reader.SectorSelect((byte) 1);
		}
		return data;
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

			if(!reader.write(TxBuffer, (byte) (Register.SRAM_Begin.getValue() + i))) return false;
		}
		return true;
	}

/*	public void writeSRAM(byte[] data, R_W_Methods method) throws IOException,
			FormatException, TimeoutException {
		int Blocks = (int) Math.ceil(data.length / 64.0);
		for (int i = 0; i < Blocks; i++) {

			writeSRAMBlock(data);
			if (method == R_W_Methods.Polling_Mode) {
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

	}*/

	public byte[] readSRAMBlock() throws IOException, FormatException, TimeoutException {
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

	public void writeEmptyNdef() throws IOException, FormatException {
		int index = 0;
		byte[] Data = new byte[4];
		index = 0;

		reader.SectorSelect((byte) 0);

		Data[index++] = (byte) 0x03;
		Data[index++] = (byte) 0x00;
		Data[index++] = (byte) 0xFE;
		Data[index++] = (byte) 0x00;

		reader.write(Data, (byte) 0x04);
	}

	public void writeDeliveryNdef() throws IOException, FormatException,
			CC_differException, StaticLockBitsException,
			DynamicLockBitsException {
		int index = 0;
		byte[] Data = new byte[4];
		byte[] Eq;
		index = 0;

		reader.SectorSelect((byte) 0);

		// checking Capability Container
		if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_1k) {
			// CC for NTAG 1k
			Data[index++] = (byte) 0xE1;
			Data[index++] = (byte) 0x10;
			Data[index++] = (byte) 0x6D;
			Data[index++] = (byte) 0x00;

		} else if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_2k) {
			// CC for NTAG 2k
			Data[index++] = (byte) 0xE1;
			Data[index++] = (byte) 0x10;
			Data[index++] = (byte) 0xEA;
			Data[index++] = (byte) 0x00;
		}

		// write CC
		try {
			reader.write(Data, (byte) 0x03);
		} catch (IOException e) {
			throw new CC_differException(
					"Capability Container cannot be written (use I2C instead to reset)");
		}

		// check if CC are set correctly
		Eq = reader.read((byte) 0x03);
		if (!(Eq[0] == Data[0] && Eq[1] == Data[1] && Eq[2] == Data[2] && Eq[3] == Data[3])) {
			throw new CC_differException(
					"Capability Container wrong (use I2C instead to reset)");
		}

		// checking static Lock bits
		Eq = reader.read((byte) 0x02);
		if (!(Eq[2] == 0 && Eq[3] == 0)) {
			throw new StaticLockBitsException(
					"Static Lockbits set, cannot reset (use I2C instead to reset)");
		}

		// checking dynamic Lock bits
		if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_1k) {
			Eq = reader.read((byte) 0xE2);

		} else if (getProduct() == Ntag_Get_Version.Prod.NTAG_I2C_2k) {
			reader.SectorSelect((byte) 1);
			Eq = reader.read((byte) 0xE0);
		}

		if (!(Eq[0] == 0 && Eq[1] == 0 && Eq[2] == 0)) {
			throw new DynamicLockBitsException(
					"Dynamic Lockbits set, cannot reset (use I2C instead to reset)");
		}

		// write all zeros
		reader.SectorSelect((byte) 0);

		byte[] d = new byte[getProduct().getMemsize()];
		writeEEPROM(d);

		// Write empty NDEF TLV in User Memory
		writeEmptyNdef();
		return;
	}

	public void writeNDEF(NdefMessage message) throws IOException,
            FormatException {
		byte[] Ndef_message_byte = createRawNdefTlv(message);
		writeEEPROM(Ndef_message_byte);
	}

	public NdefMessage readNDEF() throws IOException, FormatException {
		int NDEFsize;
		int TLVsize;
		int TLV_plus_NDEF;

		// get TLV
		byte[] TLV = readEEPROM(Register.User_memory_Begin.getValue(),
				Register.User_memory_Begin.getValue() + 3);

		// checking TLV - maybe there are other TLVs on the tag
		if (TLV[0] != 0x03) {
			throw new FormatException("Format on Tag not supported");
		}

		if (TLV[1] != (byte) 0xFF) {
			NDEFsize = (TLV[1] & 0xFF);
			TLVsize = 2;
			TLV_plus_NDEF = TLVsize + NDEFsize;
		} else {
			NDEFsize = (TLV[3] & 0xFF);
			NDEFsize |= ((TLV[2] << 8) & 0xFF00);
			TLVsize = 4;
			TLV_plus_NDEF = TLVsize + NDEFsize;
		}

		// Read NDEF Message
		byte[] data = readEEPROM(Register.User_memory_Begin.getValue(),
				Register.User_memory_Begin.getValue() + (TLV_plus_NDEF / 4));

		// delete TLV
		data = Arrays.copyOfRange(data, TLVsize, data.length);
		// delete end of String which is not part of the NDEF Message
		data = Arrays.copyOf(data, NDEFsize);

		// get the String out of the Message
		NdefMessage message = new NdefMessage(data);
		return message;
	}

	// -------------------------------------------------------------------
	// Helping function
	// -------------------------------------------------------------------

	/**
	 * create a Raw NDEF TLV from a NDEF Message
	 */
	private byte[] createRawNdefTlv(NdefMessage NDEFmessage)
			throws UnsupportedEncodingException {
		// creating NDEF
		byte[] Ndef_message_byte = NDEFmessage.toByteArray();
		int ndef_message_size = Ndef_message_byte.length;
		byte[] message;

		if (ndef_message_size < 0xFF) {
			message = new byte[ndef_message_size + 3];
			byte TLV_size = 0;
			TLV_size = (byte) ndef_message_size;
			message[0] = (byte) 0x03;
			message[1] = (byte) TLV_size;
			message[message.length - 1] = (byte) 0xFE;
			System.arraycopy(Ndef_message_byte, 0, message, 2,
					Ndef_message_byte.length);
		} else {
			message = new byte[ndef_message_size + 5];
			int TLV_size = ndef_message_size;
			TLV_size |= 0xFF0000;
			message[0] = (byte) 0x03;
			message[1] = (byte) ((TLV_size >> 16) & 0xFF);
			message[2] = (byte) ((TLV_size >> 8) & 0xFF);
			message[3] = (byte) (TLV_size & 0xFF);
			message[message.length - 1] = (byte) 0xFE;
			System.arraycopy(Ndef_message_byte, 0, message, 4,
					Ndef_message_byte.length);
		}

		return message;
	}

	public boolean checkPTwritePossible() throws IOException, FormatException {
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

}
