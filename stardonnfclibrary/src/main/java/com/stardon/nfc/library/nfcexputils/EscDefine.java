package com.stardon.nfc.library.nfcexputils;

public class EscDefine {
	/*
	 * 模版ID
	 * 注意:目前只支持1个变量
	 */
	public enum MOULD_ID{
		m0,
		err
	}
	
	/*
	 * 变量ID
	 * 注意:目前只支持4个变量
	 */
	public enum VAR_ID{
		v0,
		v1,
		v2,
		v3,
		err
	}
	
	/**
	 * 枚举类型：文本放大方式
	 * @author Administrator
	 *
	 */
	public static enum 	TEXT_ENLARGE {
		NORMAL(0x00),                        //正常字符 
        HEIGHT_DOUBLE(0x01),                 //倍高字符
        WIDTH_DOUBLE(0x10),                  //倍宽字符
        HEIGHT_WIDTH_DOUBLE (0x11);           //倍高倍宽字符        
        private int _value;
		private TEXT_ENLARGE(int mode) {
			_value = mode;
		}		
		public int value() {
			return _value;
		}
	}
	
	/**
	 * 枚举类型：字体ID
	 * @author Administrator
	 *
	 */
	public static enum FONT_ID {
		ASCII_12x24(0x0000),                     
        ASCII_8x16(0x0001),
        ASCII_16x32(0x0003),
        ASCII_24x48(0x0004),
        ASCII_32x64(0x0005),
        GBK_24x24(0x0010),
        GBK_16x16(0x0011),
        GBK_32x32(0x0013),
        GB2312_48x48(0x0014);
        
        private int _value;
		private FONT_ID(int id)	{
			_value = id;
		}		
		public int value()	{
			return _value;
		}	
	}
	/**
	 * 枚举类型：字体高度
	 * @author Administrator
	 *
	 */
	public static enum FONT_HEIGHT {
		x24,                     
        x16,
        x32,
        x48,
        x64,        
	}
	
	/**
	 * 枚举类型：对齐方式，对所有打印对象有效
	 * @author Administrator
	 *
	 */
	public static enum ALIGN {
		LEFT,
		CENTER,
		RIGHT;
	}
	
	/**
	 * esc条码基本单元大小
	 * @author Administrator
	 *
	 */
	public static enum ESC_BAR_UNIT {
		x1(1),
		x2(2),
		x3(3),
		x4(4);
		private int _value;
		private ESC_BAR_UNIT(int dots)	{
			_value = dots;
		}		
		public int value() {
			return _value;
		}
	}
	
	public static enum BAR_TEXT_POS {
		NONE,
		TOP,
		BOTTOM,				
	}
	
	public static enum BAR_TEXT_SIZE {
		ASCII_12x24,
		ASCII_8x16,						
	}
}
