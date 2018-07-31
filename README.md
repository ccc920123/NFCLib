# NFCLib
### 打印机属性
在进行NFC打印时我们先要了解打印机的属性，本文中打印机以EXP342为标准进行编写集成技术文档。EXP342的部分设置请参考该产品的使用说明书。我们在进行Android集成开发时需要注意打印机的黑标感应点的位置，确定打印纸张黑标是否完全覆盖打印机黑标感应点，如果未完全覆盖将会导致打印纸不能走到你预期想要的位置。标准纸张左黑标长度2.2cm,右边黑标长1.6cm,如果黑标未能达到该标准，系统默然将走纸长度为31.25cm处。
### 程序接入
在项目Project   build.gradle添加<br>
`allprojects {`<br>
		`repositories {`<br>
			`...`<br>
			`maven { url 'https://jitpack.io' }`<br>
		`}`<br>
	`}`<br>
在项目app  build.gradle添加<br>
 `dependencies {`<br>
	       `implementation 'com.github.ccc920123:NfcLibrary:v1.0'`<br>
	`}`<br>


步骤一：过滤标签，我们都知道每个NFC都有标签，当设备靠近标签时设备会前去解析标签，这里我们将过滤标签，再程序中只解析打印机标签，如果想要系统再靠近标签时不弹出系统空标签，在实际开发种我们将以下代码写在BaseActivity中，让其他Activity继承该该BaseActivity。

`nfcPrinter = new NfcPrinter(this);`<br>
`boolean result = nfcPrinter.check_loacl_NFC_device();//判断是否支持Nfc`<br>
`if (!result)`<br>
    `return;`<br>
`nfcPrinter.setNfcForeground();`<br>

步骤二：再Activity 的onResume与onPause，启动与暂停读取打印机标签。

`@Override`<br>
`public void onResume() {`<br>
    `super.onResume();`<br>
    `if (nfcPrinter != null) {`<br>
        `nfcPrinter.regist();`<br>
    `}`<br>
`}`<br>

`@Override`<br>
`public void onPause() {`<br>
    `super.onPause();`<br>
    `if (nfcPrinter != null) {`<br>
        `nfcPrinter.disableForegroundDispatch();`<br>
    `}`<br>
`}`<br>
步骤三：组装数据，所有的数据都是通过Esc工具类封装，在Esc
中主要有以下重要的方法：
`Esc esc = new Esc(2048);`<br>
`esc.reset();//复位ESC指令`<br>
`esc.feedRightMark();//走纸到右黑标`<br>
`esc.feedLeftMark()//走纸到左黑标`<br>
`esc.feedDots(int dots)// 走纸n点，每个点0.125mm,有点相当于空格`<br>
`esc. feedLines(final int nLines)// 走纸n行,相当于换行符`<br>
`esc.setAlign(EscDefine.ALIGN.CENTER);// 居中对齐，EscDefine.ALIGN支持LEFT,CENTER,RIFHT,三种对齐方式。`<br>
`esc.setTextSize(EscDefine.BAR_TEXT_SIZE.ASCII_12x24);//设置字体大小，在该工具中封装了2种字体大小分别是ASCII_12x24，ASCII_8x16`<br>
`esc.text(String text);// 添加文本`<br>
`esc.mouldDefine(EscDefine.MOULD_ID id, final byte []data, final int dataLen`<br>
`)//设置模板`<br>
`esc.mouldRun(EscDefine.MOULD_ID.m0, 1);//运行模板`<br>

`esc.getESCData()//得到装的esc数据`<br>

步骤四：将封装的数据发送给打印机  
`byte[] data = esc.getESCData();//获取需要发送的数据`<br>
`this.nfcPrinter.startPrintTask(data, 1000 * 30);//开启线程发送数据`<br>

步骤五：<br>
通过Handler来接受打印机反馈的结果。<br>
`public static Handler UImHandler;`<br>


`@Override`<br>
`protected void onStart() {`<br>
    `super.onStart();`<br>
    `//接受返回的数据`<br>
    `UImHandler = new Handler() {`<br>
        `@Override`<br>
        `public void handleMessage(Message msg) { //接收工作线程返回的消息`<br>
            `Bundle bundle = msg.getData();`<br>
            `byte[] data = bundle.getByteArray(KEY_DATA);`<br>
            `String str = new String(data);`<br>
                      `Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();`<br>
            `super.handleMessage(msg);`<br>
        `}`<br>
    `};`<br>
`}`<br>


