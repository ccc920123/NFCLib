package com.stardon.nfc.library.nfcexputils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import java.io.IOException;


public class NfcPrinter {
    private Activity activity;
    private Tag tag;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private NFC_Enabled_Commands reader;
    private Nfc_frame frame;

    private PrintTask printTask;

    public Boolean newIntent = false;
    private boolean exitTask = false;


    /**
     * 构造函数
     *
     * @param activity
     */
    public NfcPrinter(final Activity activity) {
        this.activity = activity;
        this.tag = null;
    }

    /**
     * 检查NFC是否存在
     * 检查NFC是否开启
     *
     * @return
     */
    public boolean check_loacl_NFC_device() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);

        if (mNfcAdapter== null) {//无nfc功能
            new AlertDialog.Builder(activity)
                    .setTitle("当前设备无NFC功能.")
                    .setNeutralButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    System.exit(0);
                                }
                            }).show();
            return false;
        }
        if (!mNfcAdapter.isEnabled()) {
            new AlertDialog.Builder(activity)
                    .setTitle("NFC未开启")
                    .setMessage("是否设置?")
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    activity.startActivity(new Intent(
                                            Settings.ACTION_NFC_SETTINGS));
                                }
                            })
                    .setNegativeButton("No",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    System.exit(0);
                                }
                            }).show();
        }
        return true;
    }

    /**
     * 设置NFC过滤标签     打印机tag为ACTION_TECH_DISCOVERED ，
     *
     * @return
     */
    public boolean setNfcForeground() {
        if (activity == null)
            return false;
        // Create a generic PendingIntent that will be delivered to this
        // activity. The NFC stack will fill
        // in the intent with the details of the discovered tag before
        // delivering it to this activity.
        mPendingIntent = PendingIntent.getActivity(activity, 0, new Intent(
                activity, activity.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup an intent filter for all NDEF based dispatches
        mFilters = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        };

        // Setup a tech list for all NFC tags
        mTechLists = new String[][]{new String[]{NfcA.class.getName()}};

        return true;
    }

    @SuppressLint("MissingPermission")
    public void disableForegroundDispatch() {
        if (mNfcAdapter != null && newIntent == false) {
            if (Build.VERSION.SDK_INT >= 19)
                mNfcAdapter.disableReaderMode(activity);
            else
                mNfcAdapter.disableForegroundDispatch(activity);
        }
    }

    public boolean tag_connect(Tag tag) {
        if (tag == null) {
            this.tag = null;
            return false;
        }
        try {
            this.tag = tag;
            reader = NFC_Enabled_Commands.get(tag);
            frame = new Nfc_frame(reader, Nfc_const.R_W_Methods.Fast_Mode);


            if (reader == null) {
                new AlertDialog.Builder(activity)
                        .setMessage("请重新连接")
                        .setTitle("通讯失败")
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                    }
                                }).show();
            } else {
                reader.connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private NfcAdapter.ReaderCallback createReaderCallback() {
        return new NfcAdapter.ReaderCallback() {

            @Override
            public void onTagDiscovered(final Tag tag) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tag_connect(tag);
                    }
                });
            }

        };
    }

    @SuppressLint("MissingPermission")
    public boolean regist() {
        if (mNfcAdapter == null)
            return false;

        Bundle options = new Bundle();
        if (Build.VERSION.SDK_INT >= 19) {
            mNfcAdapter.enableReaderMode(activity,
                    createReaderCallback(),
                    NfcAdapter.FLAG_READER_NFC_A,
                    Bundle.EMPTY);
        } else {
            mNfcAdapter.enableForegroundDispatch(activity,
                    mPendingIntent,
                    mFilters,
                    mTechLists);
        }
        return true;
    }

    public boolean isReady() {
        if (tag != null && reader != null)
            return true;
        else
            return false;
    }

    public boolean isConnected() {
        return reader.isConnected();
    }

    /**
     * 停止线程
     */
    public void printTaskFinish() {
        if (printTask != null && !printTask.isCancelled()) {
            exitTask = true;
            try {
                printTask.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            printTask = null;
        }
    }

    /**
     * 启动打印线程
     *
     * @throws IOException
     * @throws FormatException
     */
    public void printTaskStart(byte[] data, int timeout) throws IOException, FormatException {
        printTask = new PrintTask(data);
        printTask.execute(timeout);
    }

    /**
     * 开始将数据传输给EXP342打印机
     *
     * @param data    数据
     * @param timeout 相应时间
     */
    public void startPrintTask(byte[] data, int timeout) {
        printTaskFinish();
        try {
            printTaskStart(data, timeout);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }

    /**
     * nfc建立连接
     *
     * @param timeout
     * @return
     */
    private Nfc_const.Task_Result nfcConnet(final int timeout) {
        long start = System.currentTimeMillis();

        try {
            while (true) {
                if (exitTask) {
                    return Nfc_const.Task_Result.CANCEL;
                }
                if (isReady() && isConnected()) {
                    Thread.sleep(200); //连接成功后。必须等待一段时间待打印机nfc模块初始化
                    break;
                }
                if (((System.currentTimeMillis() - start) > timeout)) {
                    return Nfc_const.Task_Result.CONNET_TIMEOUT;
                }
                Thread.sleep(5);
            }
        } catch (Exception e) {
            return Nfc_const.Task_Result.EXCEPTION;
        }
        int checkTimeout = 5000;
        start = System.currentTimeMillis();
        try {
            while (true) {
                if (exitTask) {
                    return Nfc_const.Task_Result.CANCEL;
                }
                if (reader.checkPTwritePossible()) {
                    break;
                }
                if (((System.currentTimeMillis() - start) > checkTimeout)) {
                    return Nfc_const.Task_Result.CONNET_TIMEOUT;
                }
            }
        } catch (Exception e) {
            return Nfc_const.Task_Result.EXCEPTION;
        }
        return Nfc_const.Task_Result.SUCCESS;
    }

    /**
     * 发送数据
     * @param data
     * @param start
     * @param length
     * @return
     */
    private boolean printerSendData(byte[] data, final int start, final int length) {
        if (!frame.sendDataWithoutAck(data, start, length)) return false;
        return true;
    }

    /**
     *将数据回传到activity界面
     * @param str
     */

    private void sendMessageToUI(String str) {
        ((MessageCallback)activity).setMessageCallback(str);
    }

    /**
     * 启动异步线程发送数据
     */
    private class PrintTask extends AsyncTask<Integer, Integer, Nfc_const.Task_Result> {
        private byte[] m_dataBuffer; //需要发送的数据

        public PrintTask(byte[] data) {
            m_dataBuffer = data;
        }

        /**
         * 线程执行前操作
         */
        @Override
        protected void onPreExecute() {
        }

        /**
         * 后台执行线程
         */
        @Override
        protected Nfc_const.Task_Result doInBackground(Integer... param) {
            byte id = 0;
            if (exitTask) {
                return Nfc_const.Task_Result.CANCEL;
            }
            try {
                int timeout = param[0];
                Nfc_const.Task_Result result = nfcConnet(timeout);
                if (result != Nfc_const.Task_Result.SUCCESS) {
                    return result;
                }
                if (m_dataBuffer == null)
                    return Nfc_const.Task_Result.WAIT_DATA_TIMEOUT;
                 //开始向Exp342设备发送数据
                if (!printerSendData(m_dataBuffer, 0, m_dataBuffer.length)) {
                    return Nfc_const.Task_Result.SendFail;
                }
                return Nfc_const.Task_Result.SUCCESS;
            } catch (Exception e) {
                return Nfc_const.Task_Result.EXCEPTION;
            }
        }

        /**
         * 线程结束后操作
         */
        @Override
        protected void onPostExecute(Nfc_const.Task_Result result) {
            if (result == Nfc_const.Task_Result.CANCEL) {
                sendMessageToUI("取消任务");
            } else if (result == Nfc_const.Task_Result.SUCCESS) {
                sendMessageToUI("发送成功");
            } else if (result == Nfc_const.Task_Result.EXCEPTION) {
                sendMessageToUI("发送异常");
            } else if (result == Nfc_const.Task_Result.WAIT_DATA_TIMEOUT) {
                sendMessageToUI("等待数据超时");
            } else if (result == Nfc_const.Task_Result.CONNET_TIMEOUT) {
                sendMessageToUI("连接超时");
            } else if (result == Nfc_const.Task_Result.SendFail) {
                sendMessageToUI("发送失败");
            } else {
                sendMessageToUI("未知错误");
            }
            m_dataBuffer = null;
            cancel(true);
        }
    }
}
