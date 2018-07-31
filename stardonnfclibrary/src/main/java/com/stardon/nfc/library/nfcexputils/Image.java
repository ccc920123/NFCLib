package com.stardon.nfc.library.nfcexputils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;


public class Image extends BaseESC {

	public Image(Port port, JQPrinter.PRINTER_TYPE printer_type) {
		super(port, printer_type);
	}

	// / <summary>
	// / 图像数据下载到打印机内存
	// / 1)图像数据扫描方式是从左到右，从上到下
	// / 2)数据总大小:X_BYTES * Y_BYTES *8
	// / 3)X方向点数 X_BYTES * 8
	// / 4)Y方向点数 Y_BYTES * 8
	// / </SUMMARY>
	private boolean userImageDownloadIntoRAM(final int x_bytes,
											 final int y_bytes, final byte[] data) {
		final byte[] cmd = { 0x1D, 0x2A, 0, 0 };
		if (x_bytes <= 0)
			return false;
		if (y_bytes <= 0 || y_bytes > 127)
			return false;
		final int all_data_size = x_bytes * y_bytes * 8;

		if (all_data_size > 1024)
			return false;
		if (all_data_size != data.length)
			return false;

		cmd[2] = (byte) x_bytes;
		cmd[3] = (byte) y_bytes;
		if (!port.write(cmd))
			return false;
		return port.write(data);
	}

	// / <summary>
	// / 绘制RAM中预存储图像到打印画板
	// / 1)打印机不打印输出图像
	// / </summary>
	private boolean userImageDrawout(final Esc.IMAGE_ENLARGE mode) {
		final byte[] cmd = { 0x1D, 0x2F, 0x00 };
		cmd[2] = (byte) mode.ordinal();
		return port.write(cmd);
	}

	// / <summary>
	// / 绘制图像到打印机绘图区域
	// / 1)并不立刻打印输出,一些情况会导致打印内容输出
	// / A. 换行回车(\r \n 或者\r\n)
	// / B 打印对象导致x坐标超过画板宽度
	// / 2)图像高度超过打印画板高度高度会导致上部分图像丢失
	// / </summary>
	private boolean _drawOut(int image_width_dots, int image_height_dots,
							 Esc.IMAGE_ENLARGE mode, byte[] image_data)
	{
		int Y_Byte = (image_height_dots - 1) / 8 + 1; // 位图Y轴方向象素点的字节素；
		int X_Byte = (image_width_dots - 1) / 8 + 1; // 位图X轴方向象素点的字节素，表示需要X_Byte幅8
		// x BmpHeight的位图拼成目标位图；
		byte[] DotsBuf = new byte[Y_Byte * 8]; // 存放8 x BmpHeight位图的点阵数据；
		for (int i = 0; i < DotsBuf.length; i++)
			DotsBuf[i] = 0;
		int DotsBufIndex = 0; // 8xBmpHeight数据索引
		int DotsByteIndex = 0; // 原始位图数据索引
		for (int i = 0; i < X_Byte; i++) {
			for (int j = 0; j < 8; j++) {
				for (int k = 0; k < Y_Byte; k++) {
					DotsByteIndex = k * image_width_dots + i * 8 + j;
					if ((i << 3) + j < image_width_dots) // 当宽度大于位图实际宽度是，点阵数据为0，因为定义位图宽度为8的整数倍，而实际宽度可能不是整数倍
						DotsBuf[DotsBufIndex++] = (byte) image_data[DotsByteIndex];
					else
						DotsBuf[DotsBufIndex++] = 0x00;
				}
			}
			DotsBufIndex = 0;
			userImageDownloadIntoRAM(1, Y_Byte, DotsBuf); // 定义位图
			userImageDrawout(mode); // 打印定义位图
		}
		return true;
	}

	private boolean _drawOut(int image_width_dots, int image_height_dots,
							 Esc.IMAGE_ENLARGE mode, char[] image_data)
	{
		int Y_Byte = (image_height_dots - 1) / 8 + 1; // 位图Y轴方向象素点的字节素；
		int X_Byte = (image_width_dots - 1) / 8 + 1; // 位图X轴方向象素点的字节素，表示需要X_Byte幅8
		// x BmpHeight的位图拼成目标位图；
		byte[] DotsBuf = new byte[Y_Byte * 8]; // 存放8 x BmpHeight位图的点阵数据；
		for (int i = 0; i < DotsBuf.length; i++)
			DotsBuf[i] = 0;
		int DotsBufIndex = 0; // 8xBmpHeight数据索引
		int DotsByteIndex = 0; // 原始位图数据索引
		for (int i = 0; i < X_Byte; i++) {
			for (int j = 0; j < 8; j++) {
				for (int k = 0; k < Y_Byte; k++) {
					DotsByteIndex = k * image_width_dots + i * 8 + j;
					if ((i << 3) + j < image_width_dots) // 当宽度大于位图实际宽度是，点阵数据为0，因为定义位图宽度为8的整数倍，而实际宽度可能不是整数倍
						DotsBuf[DotsBufIndex++] = (byte) image_data[DotsByteIndex];
					else
						DotsBuf[DotsBufIndex++] = 0x00;
				}
			}
			DotsBufIndex = 0;
			userImageDownloadIntoRAM(1, Y_Byte, DotsBuf); // 定义位图
			userImageDrawout(mode); // 打印定义位图
		}
		return true;
	}

	// / <summary>
	// / 根据数组绘制图像到打印机画板
	// / 1)图像高度不能大于对打印机画板高度
	// / 2)由于图像并没有立即输出，还可以继续在相应的x,y坐标绘制打印对象
	// / </summary>
	public boolean drawOut(int x, int y, int image_width_dots,
						   int image_height_dots, Esc.IMAGE_ENLARGE mode, byte[] image_data)
	{
		if (!setXY(x, y))
			return false;
		return _drawOut(image_width_dots, image_height_dots, mode, image_data);
	}

	// / <summary>
	// / 根据数组绘制图像到打印机画板
	// / 1)图像高度不能大于对打印机画板高度
	// / 2)由于图像并没有立即输出，还可以继续在相应的x,y坐标绘制打印对象
	// / </summary>
	public boolean drawOut(int x, int y, int image_width_dots,
						   int image_height_dots, Esc.IMAGE_ENLARGE mode, char[] image_data) {
		if (!setXY(x, y))
			return false;
		return _drawOut(image_width_dots, image_height_dots, mode, image_data);
	}

	// / <summary>
	// / 根据bitmap对象，使用自定义位图方式绘制图像到打印画板
	// / 1)图像高度不能大于对打印机画板高度
	// / 2)由于图像并没有立即输出，还可以继续在相应的x,y坐标绘制打印对象
	// / </summary>
	public boolean drawOut(int x, int y, Bitmap bitmap)
	{
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if (width > this.maxDots)// || height > this.canvasMaxHeight)
		{
			Log.e("JQ", "w:" + width + " > " + maxDots);
			return false;
		}
		if (height > this.canvasMaxHeight) {
			Log.e("JQ", "h:" + height + " > " + canvasMaxHeight);
			return false;
		}

		ImageConvert conver = new ImageConvert();
		byte[] data = conver.CovertImageVertical(bitmap, 128, 8);

		if (data == null)
			return false;
		if (!setXY(x, y))
			return false;
		return _drawOut(width, height, Esc.IMAGE_ENLARGE.NORMAL, data);
	}

	// / <summary>
	// / 根据图片路径，使用自定义位图方式绘制图像到打印画板
	// / 1)图像高度不能大于对打印机画板高度
	// / 2)由于图像并没有立即输出，还可以继续在相应的x,y坐标绘制打印对象
	// / </summary>
	public boolean drawOut(int x, int y, String image_path) {
		if (new File(image_path).exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(image_path);
			return drawOut(x, y, bitmap);
		} else {
			Log.e("JQ", "文件路径错误:" + image_path);
			return false;
		}
	}
	public static byte[]  printOutimage(Bitmap bitmap) {
//		int width = bitmap.getWidth();
//		int height = bitmap.getHeight();

		ImageConvert conver = new ImageConvert();
		byte[] data = conver.CovertImageVertical(bitmap, 128, 8);

		return data;
	}
	// / <summary>
	// / 根据bitmap对象打印图片
	// / 1)适用于所有厂家及所有型号的POS打印机
	// / 2)图像立即输出,不能在图像上绘制文字
	// / </summary>
	public  boolean printOut(int x, Bitmap bitmap, int sleep_time) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if (width > this.maxDots)
			return false;
		ImageConvert conver = new ImageConvert();
		byte[] data = conver.CovertImageVertical(bitmap, 128, 8);

		if (data == null)
			return false;
		return _printOut(x, width, height,
				Esc.IMAGE_MODE.SINGLE_WIDTH_8_HEIGHT, data, sleep_time);
	}

	// / <summary>
	// / 根据文件路径打印图片
	// / 1)适用于所有厂家及所有型号的POS打印机
	// / 2)图像立即输出,不能在图像上绘制文字
	// / </summary>
	public boolean printOut(int x, String image_path, int sleep_time)
	{
		if (new File(image_path).exists())
		{
			Bitmap bitmap = BitmapFactory.decodeFile(image_path);
			return printOut(x, bitmap, sleep_time);
		} else {
			Log.e("JQ", "文件路径错误:" + image_path);
			return false;
		}
	}

	// / <summary>
	// / 从上到下把一副大图片分割成n=((height-1)/8+1)个小图片,每个小图像宽width，高8个点。
	// / 1)适用于所有厂家及型号的POS打印机
	// / </summary>
	private boolean _printOut(int x, int width, int height,
							  Esc.IMAGE_MODE mode, byte[] data, int sleep_time) {
		if (width > this.maxDots) {
			return false;
		}

		if (mode == Esc.IMAGE_MODE.SINGLE_WIDTH_8_HEIGHT
				|| mode == Esc.IMAGE_MODE.DOUBLE_WIDTH_8_HEIGHT)
		{
		}
		else
		{
			Log.e("JQ","图像模式错误");
			return false;
		}

		int count; // 分割成多少副图片
		count = (height - 1) / 8 + 1;
		if (data.length != width * count) {
			Log.e("JQ","数据长度和 IMAGE_MODE参数不匹配");
			return false;
		}
		byte[] cmd = { 0x1B, 0x2A };// 发送命令头
		this.setLineSpace(0);// 设置行间距为0
		for (int i = 0; i < count; i++)
		{
			this.setXY(x, 0);
			port.write(cmd);
			port.write((byte) mode.value());
			port.write((short) width);
			port.write(data, i * width, width);
			port.write("\r\n");
			try {
				Thread.sleep(sleep_time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		setLineSpace(8);// 恢复原始值 行间距为8

		port.write("\r\n");
		return true;
	}

	// / <summary>
	// / 根据文件路径打印快速打印图片
	// / 1)此为济强电子独有指令，不兼容别的品牌的打印机
	// / 2)此方法是把一副大图片，分割成n多副小图片(高度为base_image_height)来打印。
	// / 3)根据上位机的数据传输速度，可以调整base_image_height的大小来获得不同的图像打印速度。
	// / 4)使用此方法，不可以在其余区域绘制别的东西。如果需要在图片上绘制别的打印对象请使用drawOut相关函数
	// / 5)需要配合最新的打印机固件使用
	// / </summary>

	public boolean printOutFast(int x, String image_path, int sleep_time,
                                int base_image_height)
	{
		if (new File(image_path).exists())
		{
			Bitmap bitmap = BitmapFactory.decodeFile(image_path);
			return printOutFast(x, bitmap, sleep_time, base_image_height);
		} else
		{
			Log.e("JQ", "文件路径错误:" + image_path);
			return false;
		}
	}

	// / <summary>
	// / 通过Bitmap对象快速打印图像
	// / 1)需要配合最新的打印机固件使用
	// / </summary>
	public boolean  printOutFast(int x, Bitmap bitmap, int sleep_time,
                                 int base_image_height)
	{
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if (width > this.maxDots)
			return false;
		ImageConvert conver = new ImageConvert();
		byte[] data = conver.CovertImageHorizontal(bitmap, 128);

		if (data == null)
			return false;
		return _printOutFast(x, width, height, 1, 1, data, sleep_time,
				base_image_height);
	}

	// / <summary>
	// / 快速打印图片基本函数
	// / 1)需要配合最新的打印机固件使用
	// / </summary>
	private boolean _printOutFast(int x, int width, int height, int enlargeX,
								  int enlargeY, byte[] data, int sleep_time, int base_image_height)
	{
		if (width > this.maxDots) {
			return false;
		}

		if (enlargeX > 2 || enlargeY > 2 || enlargeX < 0 || enlargeY < 0) {
			Log.e("JQ","图像放大错误");
			return false;
		}

		int width_byte = (width - 1) / 8 + 1;
		if (width_byte * height != data.length) {
			Log.e("JQ", "data size error");
			return false;
		}

		byte[] cmd = { 0x1B, 0x2B };
		if (base_image_height < 4) {
			base_image_height = 4;
		}
		int HeightWriteUnit = base_image_height; // 每张图片的高度 4 =< u < 56

		if (width_byte * HeightWriteUnit > 2000) {
			Log.e("JQ", "单张图片数据太多，请减小图像高度");
			return false;
		}

		int HeightWrited = 0;

		int HeightLeft = height; // 剩下的高度
		this.setLineSpace(0);// 设置行间距为0
		for (; HeightLeft > 0;) {
			if (HeightLeft <= HeightWriteUnit) {
				HeightWriteUnit = HeightLeft;
			}
			this.setXY(x, 0);
			port.write(cmd);
			port.write((short) width); // 图片宽度
			port.write((short) HeightWriteUnit); // 图片高度
			port.write((byte) enlargeX);
			port.write((byte) enlargeY);
			port.write(data, HeightWrited * width_byte, HeightWriteUnit
					* width_byte);
			HeightWrited += HeightWriteUnit;
			HeightLeft -= HeightWriteUnit;
			if (sleep_time > 0)
				try {
					Thread.sleep(sleep_time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		this.setLineSpace(8);// 设置行间距为0
		return true;
	}
}
