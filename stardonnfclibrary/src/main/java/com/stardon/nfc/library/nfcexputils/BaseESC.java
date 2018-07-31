package com.stardon.nfc.library.nfcexputils;


public class BaseESC {
	protected Port port;
	protected JQPrinter.PRINTER_TYPE printerType;
	protected int maxDots;//允许打印的大点数
	protected int canvasMaxHeight;//打印机画板最大高度，单位dots.

	/*
	 * 构造函数
	 */
	public BaseESC(Port port,JQPrinter.PRINTER_TYPE printer_type)
	{
		if (port== null)
			return;
		this.port = port;

		printerType = printer_type;
		switch(printerType)
		{
			case VMP02:
				maxDots = 384;
				canvasMaxHeight = 100;
				break;
			case VMP02_P:
				maxDots = 384;
				canvasMaxHeight = 200;
				break;
			case ULT113x:
				maxDots = 576;
				canvasMaxHeight = 120;
				break;
			case JLP351:
				maxDots = 576;
				canvasMaxHeight = 250;
				break;
			default:
				maxDots = 576;
				canvasMaxHeight = 100;
				break;
		}
	}


	/*
	 * 设置打印对象的x，y坐标
	 */
	public boolean setXY(int x,int y)
	{
		if (x < 0 || x >= maxDots || x > 0x1FF)
		{
			return false;
		}

		if (y < 0 || y >= canvasMaxHeight || y > 0x7F)
		{
			return false;
		}

		byte[] cmd ={0x1B, 0x24, 0x00, 0x00};
		int pos = ((x & 0x1FF) | ((y & 0x7F) << 9));
		cmd[2] = (byte)pos;
		cmd[3] = (byte)(pos>>8);
		port.write(cmd);
		return true;
	}
	/*
	 * 设置打印对象对齐方式
	 * 支持打印对象:文本(text),条码(barcode)
	 */
	public boolean setAlign(JQPrinter.ALIGN align)
	{
		byte []cmd = { 0x1B, 0x61, 0x00};
		cmd[2] = (byte)align.ordinal();
		return port.write(cmd);
	}
	public boolean setLineSpace(int dots)
	{
		byte[] cmd = { 0x1B, 0x33, 0x00 };
		cmd[2] = (byte)dots;
		return port.write(cmd);
	}

	public boolean init()
	{
		byte []cmd= { 0x1B, 0x40};
		return port.write(cmd);
	}

	/// <summary>
	/// 换行回车
	/// </summary>
	public boolean enter()
	{
		byte[] cmd = { 0x0D, 0x0A };
		return port.write(cmd);
	}


}
