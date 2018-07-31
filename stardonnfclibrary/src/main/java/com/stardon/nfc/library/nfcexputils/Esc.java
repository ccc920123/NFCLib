package com.stardon.nfc.library.nfcexputils;

import android.util.Log;

import java.io.UnsupportedEncodingException;

public class Esc {
    final int ESC_BAR_2D_PDF417 = 0;
    final int ESC_BAR_2D_DATAMATIX = 1;
    final int ESC_BAR_2D_QRCODE = 2;

    public Image image;
    /*
     * 变量类型
     */
    public enum VAR_TYPE
    {
        VAR_STRING//字符串类型
    }

    private byte []dataBuffer;
    /**
     * 获取数据缓冲区
     * @return
     */
    public byte[] getDataBuffer(){
        return dataBuffer;
    }
    /**
     * 数据长度
     */
    private int dataIndex;

    /**
     * 获取数据长度
     * @return
     */
    public int getDataLen() {
        return dataIndex;
    }

    /**
     * 缓冲区最大值
     */
    private int dataMaxSize;

    /**
     * 页面宽度
     * 打印机只有576个点
     */
    final private int escPageWidth = 576;
    /**
     * 页面高度
     */
    final private int escPageHeight = 384;
    public static enum IMAGE_MODE
    {
        SINGLE_WIDTH_8_HEIGHT(0x01),        //单倍宽8点高
        DOUBLE_WIDTH_8_HEIGHT(0x00),        //倍宽8点高
        SINGLE_WIDTH_24_HEIGHT(0x21),       //单倍宽24点高
        DOUBLE_WIDTH_24_HEIGHT(0x20);       //倍宽24点高
        private int _value;
        private IMAGE_MODE(final int mode)
        {
            _value = mode;
        }
        public int value()
        {
            return _value;
        }
    }
    public static enum IMAGE_ENLARGE
    {
        NORMAL,//正常
        HEIGHT_DOUBLE,//倍高
        WIDTH_DOUBLE,//倍宽
        HEIGHT_WIDTH_DOUBLE	//倍高倍宽
    }

    public Esc(final int bufferSize){
        dataMaxSize = bufferSize;
        dataBuffer = new byte[bufferSize];
        esc_data_reset();
    }

    public void esc_data_reset(){
        dataIndex = 0;
    }

    /**
     * 添加数组
     * @param data
     * @param dataLen
     * @return
     */
    public boolean add(final byte data[], final int dataLen){
        if (dataIndex + dataLen > dataMaxSize)
            return false;
        for (int i = 0 ; i < dataLen; i++)
            dataBuffer[dataIndex++] = data[i];
        return true;
    }

    /**
     * 添加数组
     * @param data
     * @param
     * @return
     */
    private boolean add(final byte data[]){
        if (dataIndex + data.length > dataMaxSize)
            return false;
        for (int i = 0 ; i < data.length; i++)
            dataBuffer[dataIndex++] = data[i];
        return true;
    }

    /**
     * 添加1个byte
     * @param data
     * @param
     * @return
     */
    private boolean add(final byte data){
        if (dataIndex + 1 > dataMaxSize)
            return false;
        dataBuffer[dataIndex++] = data;
        return true;
    }
    /**
     * 复位ESC指令
     * @return
     */
    public boolean reset()
    {
        final byte[] cmd = {0x1B, 0x40};
        return add(cmd);
    }
    /**
     * 走纸n行
     * @param
     * @return
     */
    public boolean feedLines(final int nLines) {
        byte[] cmd = {0x1B, 0x64, 0x00};
        cmd[2] = (byte) nLines;
        return add(cmd);
    }
    /**
     * 走纸n点，每个点0.125mm
     * @param dots
     * @return
     */
    public boolean feedDots(int dots) {
        byte[] cmd = {0x1B, 0x4A, 0x00};
        cmd[2] = (byte) dots;
        return add(cmd);
    }
    /**
     * 走纸到右黑标
     * @return
     */
    public boolean feedRightMark(){
        return add((byte)0x0C);
    }
    /**
     * 走纸到左黑标
     * @return
     */
    public boolean feedLeftMark(){
        return add((byte)0x0E);
    }
    /**
     * 添加文本
     * @param text
     * @return
     */
    public boolean text(String text){
        if (text == null)
            return false;

        byte data[];
        try {
            data = text.getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            Log.e("JQ", "Sting getBytes('GBK') failed");
            return false;
        }
        if (dataIndex + data.length + 1 > dataMaxSize)
            return false;
        for (int i = 0; i < data.length; i++ ){
            dataBuffer[dataIndex++] = data[i];
        }
        dataBuffer[dataIndex++] = 0; //文本以0x00结尾

        return true;
    }
    /**
     * 设置文字的放大方式
     * @param enlarge
     * @return
     */
    public boolean textSetFontEnlarge(EscDefine.TEXT_ENLARGE enlarge) {
        byte[] cmd = {0x1D, 0x21, 0x00};
        cmd[2] = (byte) enlarge.value();
        return add(cmd);
    }
    /**
     * 设置文本字体ID
     * @param id
     * @return
     */
    public boolean textSetFontID(EscDefine.FONT_ID id) {
        switch (id) {
            case ASCII_16x32:
            case ASCII_24x48:
            case ASCII_32x64:
            case GBK_32x32:
            case GB2312_48x48:
                break;
            default:
                return false;
        }
        byte[] cmd = {0x1B, 0x4D, 0x00};
        cmd[2] = (byte) id.value();
        return add(cmd);
    }
    /**
     * 通过字体ID来设置文本字体高度
     * @param height
     * @return
     */
    public boolean textSetFontHeight(EscDefine.FONT_HEIGHT height)
    {
        switch(height){
            case x24:
                textSetFontID(EscDefine.FONT_ID.ASCII_12x24);
                textSetFontID(EscDefine.FONT_ID.GBK_24x24);
                break;
            case x16:
                textSetFontID(EscDefine.FONT_ID.ASCII_8x16);
                textSetFontID(EscDefine.FONT_ID.GBK_16x16);
                break;
            case x32:
                textSetFontID(EscDefine.FONT_ID.ASCII_16x32);
                textSetFontID(EscDefine.FONT_ID.GBK_32x32);
                break;
            case x48:
                textSetFontID(EscDefine.FONT_ID.ASCII_24x48);
                textSetFontID(EscDefine.FONT_ID.GB2312_48x48);
                break;
            case x64:
                textSetFontID(EscDefine.FONT_ID.ASCII_32x64);
                break;
            default:
                return false;
        }
        return true;
    }
    /**
     * 设置文本加粗方式
     * @param bold
     * @return
     */
    public boolean textSetBold(boolean bold) {
        byte[] cmd = {0x1B, 0x45, 0x00};
        cmd[2] = (byte) (bold ? 1 : 0);
        return add(cmd);
    }
    /**
     * 设置文本下划线方式
     * @param underline
     * @return
     */
    public boolean textSetUnderline(boolean underline) {
        byte[] cmd = {0x1B, 0x2D, 0x00};
        cmd[2] = (byte) (underline ? 1 : 0);
        return add(cmd);
    }
    /**
     * 设置打印对象的x，y坐标 ,打印对象包括text, barcode
     * 注意：打印对象不要超过一行，超出一行的打印内容会出现在page的行首。可能出现排版问题。
     * @param x
     * @param y
     * @return
     */
    public boolean setXY(int x, int y) {
        if (x < 0 || x >= escPageWidth || x > 0x1FF) {
            return false;
        }
        if (y < 0 || y >= escPageHeight || y > 0x7F) {
            return false;
        }
        int pos = ((x & 0x1FF) | ((y & 0x7F) << 9));
        byte[] cmd = {0x1B, 0x24, 0x00, 0x00};
        cmd[2] = (byte) pos;
        cmd[3] = (byte) (pos >> 8);
        return add(cmd);
    }

    /*
     * 打印输出文本
     * x: x坐标
     * y：y坐标
     * 注意：x，y坐标是有限制的，请参考setXY函数源代码
     */
    public boolean text(int x,int y,String text) {
        if (!setXY(x,y))
            return false;
        return text(text);
    }
    /**
     * 根据字体高度输出字符串
     * 注意：如果不加换行回车(\r\n)，文本不会输出。
     * @param height
     * @param text
     * @return
     */
    public boolean text(EscDefine.FONT_HEIGHT height, String text) {
        if (!textSetFontHeight(height))
            return false;
        return text(text);
    }
    /**
     * 设置打印对象对齐方式
     * 支持打印对象:文本(text),条码(barcode)
     * @param align
     * @return
     */
    public boolean setAlign(EscDefine.ALIGN align) {
        byte[] cmd = {0x1B, 0x61, 0x00};
        cmd[2] = (byte) align.ordinal();
        return add(cmd);
    }
    /**
     * 根据参数输出文本
     * @param align
     * @param height
     * @param bold
     * @param enlarge
     * @param text
     * @return
     */
    public boolean text(EscDefine.ALIGN align, EscDefine.FONT_HEIGHT height, boolean bold, EscDefine.TEXT_ENLARGE enlarge, String text) {
        if (!setAlign(align))
            return false;
        if (!textSetFontHeight(height))
            return false;
        if (!textSetBold(bold))
            return false;
        if (!textSetFontEnlarge(enlarge))
            return false;
        return text(text);
    }
    /**
     * 输出文本
     * @param align
     * @param bold
     * @param text
     * @return
     */
    public boolean text(EscDefine.ALIGN align, boolean bold, String text) {
        if (!setAlign(align))
            return false;
        if (!textSetBold(bold))
            return false;
        return text(text);
    }
    /**
     * 绘制文本并带下划线
     * @param
     * @param text
     * @return
     */
    public boolean textWithUnderline(String text){
        if (!textSetUnderline(true))
            return false;
        if (!text(text))
            return false;
        return textSetUnderline(false);
    }
    /**
     * 绘制文本并带加粗和放大效果
     * @param enlarge
     * @param text
     * @return
     */
    public boolean textWidthBoldAndEnlarge(EscDefine.TEXT_ENLARGE enlarge, String text){
        if (!textSetBold(true)) return false;
        if (!textSetFontEnlarge(enlarge)) return false;
        if (!text(text)) return false;
        return reset();
    }
    /**
     * 设置1维条码高度
     * @param height:单位为dot,每个点为0.125mm
     * @return
     */
    public boolean barcodeSet1DHeight(int height) {
        byte cmd[] = {0x1D, 0x68, 0x00};
        cmd[2] = (byte) height;
        return add(cmd);
    }
    /**
     * 设置1维，2维条码基本单元大小
     * @param unit
     * @return
     */
    public boolean barcode_set_unit(EscDefine.ESC_BAR_UNIT unit) {
        byte cmd[] = {0x1D, 0x77, 0x00};
        cmd[2] = (byte) unit.value();
        return add(cmd);
    }
    /**
     * 设置条码文字位置
     * @param pos
     * @return
     */
    public boolean barcodeSetTextPosition(EscDefine.BAR_TEXT_POS pos) {
        byte cmd[] = {0x1D, 0x48, 0x00};
        cmd[2] = (byte) pos.ordinal();
        return add(cmd);
    }
    /**
     * 设置条码文字大小
     * @param size
     * @return
     */
    public boolean setTextSize(EscDefine.BAR_TEXT_SIZE size) {
        byte cmd[] = {0x1D, 0x66, 0x00};
        cmd[2] = (byte) size.ordinal();
        return add(cmd);
    }
    /**
     * 输出code128条码
     * @param text
     * @return
     */
    public boolean barcodeCode128Auto(String text){
        final byte[] cmd = {0x1D, 0x6B, 0x18};
        if (!add(cmd)) return false;
        return text(text);
    }


    /**
     * 输出code39条码
     * @param text
     * @return
     */
    public boolean barcodeCode39Auto(String text) {
        final byte[] cmd = {0x1D, 0x6B, 0x14};
        if (!add(cmd)) return false;
        return text(text);
    }

    private boolean barcode2D_out(byte m, byte n, byte k, String bartext)
    {
        int size = bartext.length() + 1;
        if (size == 1)
            return false;

        final byte[] cmd = {0x1B, 0x5A};

        if (!add(cmd))
            return false;
        if (!add(m))
            return false;
        if (!add(n))
            return false;
        if (!add(k))
            return false;

        cmd[0] = (byte)(size);
        cmd[1] = (byte)(size >> 8);
        if (!add(cmd))
            return false;
        return text(bartext);
    }


    /**
     * 设置2D条码类型
     * @param type
     * @return
     */
    private boolean barcode2D_set_type(int type)
    {
        final byte[] cmd = {0x1D, 0x5A, 0x00};
        cmd[2] = (byte)type;
        return add(cmd);
    }

    /**
     * 绘制QRcode 在画板，
     * note：内容不打印机，除非内容超过一行或者遇到换行命令才打印
     * @param bartext：条码内容
     * @param version：版本，版本和容纳数据大小相关。如果设置为0，将自动计算版本。
     * @param ecc ：纠错级别 有效值0，1，2，3。 值越大纠错能力越高。
     * @param unitSize:基本单位像素大小。
     */
    public boolean barcode2D_QRCode(String bartext, int version, int ecc, EscDefine.ESC_BAR_UNIT unitSize)
    {
        if (!barcode_set_unit(unitSize))
            return false;
        if (!barcode2D_set_type(ESC_BAR_2D_QRCODE))
            return  false;
        return  barcode2D_out((byte)version, (byte)ecc,(byte)0, bartext);
    }

    /**
     * 变量输出
     * @param id
     * @return
     */
    public boolean variablePrintOut(EscDefine.VAR_ID id){
        final byte[] cmd = {0x10, 0x57, 0x00, 0x00};
        cmd[2] = (byte)id.ordinal();
        return add(cmd);
    }
    /**
     * 定义字符串变量
     * @param id ，变量的id,目前只支持4个变量
     * @param text，变量字符串，不允许超过127个字节，一个中文占2个字节
     * @return
     */
    public boolean variableDefineString(EscDefine.VAR_ID id, String text){
        final byte[] cmd = {0x10, 0x56, 0x00, 0x00, 0x00, 0x00};
        byte[] data = null;
        int len;
        try {
            data = text.getBytes("GBK");
            len =data.length;
        } catch (UnsupportedEncodingException e) {
            Log.e("JQ", "Sting getBytes('GBK') failed");
            return false;
        }
        cmd[2] = (byte)id.ordinal();
        cmd[3] = (byte) VAR_TYPE.VAR_STRING.ordinal();
        cmd[4] = (byte)len;
        cmd[5] = (byte)((0xff00 & len) >> 8);
        if (!add(cmd)) return false;
        return add(data);
    }

    /**
     * 定义模版
     */
    public boolean mouldDefine(EscDefine.MOULD_ID id, final byte []data, final int dataLen){
        byte[] cmd = {0x10, 0x4D, 0x00, 0x00, 0x00};
        cmd[2] = (byte)id.ordinal();
        cmd[3] = (byte)dataLen;
        cmd[4] = (byte)((0xff00 & dataLen) >> 8);

        if (!add(cmd))
            return false;
        return add(data, dataLen);
    }

    /**
     * 获取为封装的esc数据
     * @return
     */
    public byte[] getESCData(){
        int len = getDataLen();

        byte []src = getDataBuffer();
        byte []outData = new byte[len];

        for (int i = 0 ; i< len; i++){
            outData[i] = src[i];
        }
        esc_data_reset();
        return outData;
    }

    /**
     * 运行模版
     * @param id， 运行哪个模版的ID
     * @param count， 运行几次
     * @return
     */
    public boolean mouldRun(EscDefine.MOULD_ID id, final int count){
        byte[] cmd = {0x10, 0x4E, 0x00, 0x00, 0x00};
        cmd[2] = (byte)id.ordinal();
        cmd[3] = (byte)count;
        cmd[4] = (byte)((0xff00 & count) >> 8);
        return add(cmd);
    }


}
