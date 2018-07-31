package com.stardon.nfc.library.nfcexputils;

/**
 * Class for Get version Command response
 */
public class Ntag_Get_Version {

	/**
	 * Enum for different Products
	 */
	public enum Prod {
		NTAG_I2C_1k(888), NTAG_I2C_2k(1904), Unknown(0);

		private int mem_size;

		private Prod(int mem_size) {
			this.mem_size = mem_size;
		}

		/**
		 * gets the Memsize of a Tag
		 * 
		 * @return Memsize of a Tag
		 */
		public int getMemsize() {
			return mem_size;
		}
	}

	private byte vendor_ID;
	private byte product_type;
	private byte product_subtype;
	private byte major_product_version;
	private byte minor_product_version;
	private byte storage_size;
	private byte protocol_type;

	/**
	 * Get version Response of a NTAG_I2C_1K
	 */
	public static final Ntag_Get_Version NTAG_I2C_1k;

	/**
	 * Get version Response of a NTAG_I2C_2K
	 */
	public static final Ntag_Get_Version NTAG_I2C_2k;

	static {
		NTAG_I2C_1k = new Ntag_Get_Version(new byte[] { 0x00, 0x04, 0x04, 0x05, 0x01, 0x01, 0x13, 0x03 });
		NTAG_I2C_2k = new Ntag_Get_Version(new byte[] { 0x00, 0x04, 0x04, 0x05, 0x01, 0x01, 0x15, 0x03 });
	}

	/**
	 * Returns the Product to which this get Version Response belongs
	 * 
	 * @return Product
	 */
	public Prod Get_Product() {
		if (this.equals(NTAG_I2C_1k))
			return Prod.NTAG_I2C_1k;
		if (this.equals(NTAG_I2C_2k))
			return Prod.NTAG_I2C_2k;
		else
			return Prod.Unknown;
	}

	/**
	 * Constructor
	 * 
	 * @param Data
	 *            Data from the Get Version Command
	 */
	public Ntag_Get_Version(byte[] Data) {
		vendor_ID = Data[1];
		product_type = Data[2];
		product_subtype = Data[3];
		major_product_version = Data[4];
		minor_product_version = Data[5];
		storage_size = Data[6];
		protocol_type = Data[7];
	}

	@Override
	/**
	 * Compares the Response by means of VendorID, Product Type, Product Subtype and Storage Size
	 */
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other.getClass() != this.getClass())
			return false;

		Ntag_Get_Version temp = (Ntag_Get_Version) other;

		if (temp.vendor_ID == this.vendor_ID
				&& temp.product_type == this.product_type
				&& temp.product_subtype == this.product_subtype
				&& temp.storage_size == this.storage_size)
			return true;
		else
			return false;

	}

	/**
	 * Returns the Vendor ID
	 * 
	 * @return Vendor ID
	 */
	public byte getVendor_ID() {
		return vendor_ID;
	}

	/**
	 * Returns the Product Type
	 * 
	 * @return Product Type
	 */
	public byte getProduct_type() {
		return product_type;
	}

	/**
	 * Returns the Product Subtype
	 * 
	 * @return Product Subtype
	 */
	public byte getProduct_subtype() {
		return product_subtype;
	}

	/**
	 * Returns the Major Product Version
	 * 
	 * @return Major Product Version
	 */
	public byte getMajor_product_version() {
		return major_product_version;
	}

	/**
	 * Returns the Minor Product Version
	 * 
	 * @return Minor Product Version
	 */
	public byte getMinor_product_version() {
		return minor_product_version;
	}

	/**
	 * Returns the Storage Size
	 * 
	 * @return Storage Size
	 */
	public byte getStorage_size() {
		return storage_size;
	}

	/**
	 * Returns the Protocol Type
	 * 
	 * @return Protocol Type
	 */
	public byte getProtocol_type() {
		return protocol_type;
	}

}
