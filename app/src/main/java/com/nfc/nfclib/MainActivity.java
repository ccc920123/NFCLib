package com.nfc.nfclib;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.stardon.nfc.library.nfcexputils.Esc;
import com.stardon.nfc.library.nfcexputils.EscDefine;
import com.stardon.nfc.library.nfcexputils.MessageCallback;
import com.stardon.nfc.library.nfcexputils.NfcPrinter;

import static com.stardon.nfc.library.nfcexputils.Nfc_const.KEY_DATA;

public class MainActivity extends AppCompatActivity implements MessageCallback {

    public NfcPrinter nfcPrinter;
    private Button message;
    //    NfcPrinter nfcPrinter;
    public static Handler UImHandler;

    private CountDownTimer timer = new CountDownTimer(30000, 1000) {

        @Override
        public void onTick(long millisUntilFinished) {
            String string = "请贴近打印机实现打印" + (millisUntilFinished / 1000) + "秒后将失效";

            message.setText(Html.fromHtml(string));
        }

        @Override
        public void onFinish() {
            message.setVisibility(View.GONE);


        }
    };

    /**
     * 取消倒计时
     */
    public void oncancel() {
        timer.cancel();
    }

    /**
     * 开始倒计时
     */
    public void restart() {
        timer.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        message = (Button) findViewById(R.id.buttonprint);
        message.setOnClickListener(click);


        //初始化打印机
        nfcPrinter = new NfcPrinter(this);
        boolean result = nfcPrinter.check_loacl_NFC_device();//判断是否支持Nfc
        if (!result)
            return;
        nfcPrinter.setNfcForeground();

    }

    private View.OnClickListener click = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            restart();  //开始倒计时
            byte[] data = makeModel();//获取需要发送的数据
            nfcPrinter.startPrintTask(data, 1000 * 30);//开启线程发送数据


        }
    };


    //****处理NFC标签
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onResume() {
        super.onResume();
        if (nfcPrinter != null) {
            nfcPrinter.regist();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onPause() {
        super.onPause();
        if (nfcPrinter != null) {
            nfcPrinter.disableForegroundDispatch();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        //接受返回的数据
        UImHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) { //接收工作线程返回的消息
                Bundle bundle = msg.getData();
                byte[] data = bundle.getByteArray(KEY_DATA);
                String str = new String(data);
                message.setVisibility(View.VISIBLE);
                message.setText("点击打印");
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
                oncancel();

                super.handleMessage(msg);
            }
        };
    }

/****************NFC打印处理*******************/
    /**
     * 组装打印参数
     *
     * @return
     */
    private byte[] makeModel() {
        Esc esc = new Esc(2048);
        esc.reset();//复位nfc打印机
        esc.feedRightMark();//右走黑。
        esc.feedLines(4);//换行4行
        esc.setAlign(EscDefine.ALIGN.CENTER);//设置字体居中，这里可以有3个选择LEFT,CENTER,RIGHT
        esc.setTextSize(EscDefine.BAR_TEXT_SIZE.ASCII_12x24);//设置字体大写，这里支持2种大小字体ASCII_12x24，ASCII_8x16
        esc.text("四川星盾科技股份有限公司\n");
        esc.textSetBold(true);//加粗
        esc.text("打印测试\n");
        esc.textSetBold(false);
        esc.setAlign(EscDefine.ALIGN.LEFT);
        esc.text("智现在·共未来\n" +
                "\n" +
                "“智”，星盾科技以对技术的专业、行业的专注、领域的专一，为客户提供个性服务及信息化的解决方案。智慧、智能、智助、智汇成就客户乐享现在。\n" +
                "\n" +
                "“共”，星盾科技团队同心、同德，实现自身价值的同时，与客户携手共创行业智能化，共赢美好未来。");

        esc.variablePrintOut(EscDefine.VAR_ID.v0);
        //条码
        esc.barcodeSet1DHeight(80);
        esc.barcodeSetTextPosition(EscDefine.BAR_TEXT_POS.BOTTOM);
        esc.barcodeCode39Auto("12456789");
        esc.text("\r\r");
//
        esc.setAlign(EscDefine.ALIGN.LEFT);

        esc.barcode2D_QRCode("www.scxdtech.com", 0, 2, EscDefine.ESC_BAR_UNIT.x2);
        esc.text("\r\n");

        esc.text(100, 25, "说明");
        esc.text(100, 50, "请下载APP");
        esc.setXY(210, 0);
        esc.barcode2D_QRCode("www.scxdtech.com", 0, 2, EscDefine.ESC_BAR_UNIT.x3);

        esc.text("\r\n");

        byte[] mouldData = esc.getESCData();

        esc.mouldDefine(EscDefine.MOULD_ID.m0, mouldData, mouldData.length);

//        esc.variableDefineString(EscDefine.VAR_ID.v0, "第一联(处罚人留档)\r\n");
//        esc.mouldRun(EscDefine.MOULD_ID.m0, 1);
//
//        esc.variableDefineString(EscDefine.VAR_ID.v0, "第二联(处罚人留档)\r\n");
        esc.mouldRun(EscDefine.MOULD_ID.m0, 1);

        return esc.getESCData();
    }

    @Override
    public void setMessageCallback(String datarate) {
        Bundle bundle = new Bundle();
        bundle.putByteArray(KEY_DATA, datarate.getBytes());
        Message msg = new Message();
        msg.setData(bundle);
        UImHandler.sendMessage(msg);
    }
}
