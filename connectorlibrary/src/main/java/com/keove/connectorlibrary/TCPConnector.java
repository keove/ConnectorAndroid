package com.keove.connectorlibrary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class TCPConnector {

    public static int SUCCESS = 1;
    public static int ERROR = 2;

    ArrayList<TCPCompleteListener> listeners = new ArrayList<TCPCompleteListener>();
    private String ip = "";
    private int port = -1;
    Activity act;
    private String msgtosend;
    private boolean gzip = false;


    public static String endflag = "#endconnect";
    public static int SOCKET_TIMEOUT_DURATION = 25000;
    public static int RESPONSE_TIMEOUT_DURATION = 25000;
    private static String ERROR_FLAG = "TCPConnector_Error";

    public String contract = "";

    private enum TCPConnectorKeys {
        PORT,
        IP
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }

    public TCPConnector(String IP, int PORT, Activity act) {
        this.act = act;
        ip = IP;
        port = PORT;
    }


    ArrayList<View> lockViews = new ArrayList<>();

    public TCPConnector(Context context) {
        try {
            this.ip = GetValueByContext(context.getApplicationContext(), TCPConnectorKeys.IP.name(), "");
            String portString = GetValueByContext(context.getApplicationContext(), TCPConnectorKeys.PORT.name(), "");
            int port = Integer.valueOf(portString);
            this.port = port;
        } catch (Exception ex) {
            this.ip = "192.168.1.1";
            this.port = 8888;
        }
    }

    public static void setConfig(Context context, String IP, int PORT) {
        SetValueByContext(context.getApplicationContext(), TCPConnectorKeys.IP.name(), IP);
        String portString = String.valueOf(PORT);
        SetValueByContext(context.getApplicationContext(), TCPConnectorKeys.PORT.name(), portString);
    }

    public void GetResultAsync(String msgtosend) {
        this.msgtosend = msgtosend;
        TCPThread thread = new TCPThread();
        thread.start();
    }

    public void GetResultAsync(String msgtosend, String contract) {
        this.contract = contract;
        this.msgtosend = msgtosend;
        TCPThread thread = new TCPThread();
        thread.start();
    }

    int status = -1;


    public static void GetResultAsync(final String IP, final int PORT, final Activity act, final TCPCompleteListener listener, final String msg, final String contract, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TCPConnector connector = new TCPConnector(IP, PORT, act);
                connector.AddListener(listener, true);
                connector.GetResultAsync(msg, contract);
            }
        }, delay);
    }

    public static void GetResultAsync(final boolean gzip, final String IP, final int PORT, final Activity act, final TCPCompleteListener listener, final String msg, final String contract, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TCPConnector connector = new TCPConnector(IP, PORT, act);
                connector.gzip = gzip;
                connector.AddListener(listener, true);
                connector.GetResultAsync(msg, contract);
            }
        }, delay);
    }

    public static void GetResultAsync(final Context context, final TCPCompleteListener listener, final String msg, final String contract, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TCPConnector connector = new TCPConnector(context);
                connector.AddListener(listener, true);
                connector.GetResultAsync(msg, contract);
            }
        }, delay);
    }

    public static void GetResultAsync(final boolean gzip, final Context context, final TCPCompleteListener listener, final String msg, final String contract, int delay) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                TCPConnector connector = new TCPConnector(context);
                connector.gzip = gzip;
                connector.AddListener(listener, true);
                connector.GetResultAsync(msg, contract);
            }
        }, delay);

    }

    public static void GetResultAsync(final boolean gzip, final Context context, final TCPCompleteListener listener, final String msg, final String contract, int delay, final View... lockViews) {

        for (View view : lockViews) {
            view.setEnabled(false);
        }

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {

                TCPConnector connector = new TCPConnector(context);
                for (View view : lockViews) {
                    connector.lockViews.add(view);
                }
                connector.gzip = gzip;
                connector.AddListener(listener, true);
                connector.GetResultAsync(msg, contract);
            }
        }, delay);

    }


    public static String GetResult(Context context, String msg) {
        TCPConnector connector = new TCPConnector(context);
        return connector.GetResult(msg);
    }

    public static String GetResult(boolean gzip, Context context, String msg) {
        TCPConnector connector = new TCPConnector(context);
        connector.gzip = gzip;
        return connector.GetResult(msg);
    }

    public String GetResult(String msg) {
        String response = "";
        boolean resultsent = false;
        try {
            Socket socket = new Socket();
            SocketAddress address = new InetSocketAddress(ip, port);
            socket.connect(address, SOCKET_TIMEOUT_DURATION);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            if (gzip) {
                OutputStream os = socket.getOutputStream();
                byte[] m = compress(msg);
                os.write(m);
            }
            else {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                out.flush();
                out.write(msg);
                out.flush();
            }
            try {
                char[] buffer = new char[20384];
                int charsread = 0;
                String part = "";

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                @SuppressWarnings("unused") long timediff = 0;
                Date starttime = new Date();


                while ((timediff = new Date().getTime() - starttime.getTime()) < RESPONSE_TIMEOUT_DURATION) {
                    if (socket.getInputStream().available() > 0 && (charsread = in.read(buffer)) != -1) {
                        part = new String(buffer).substring(0, charsread);

                        if (part.contains(endflag)) {
                            status = SUCCESS;
                            part = part.replace(endflag, "");
                            response += part;

                            return response;
                        }
                        else {
                            response += part;
                        }


                    }
                    else {

                    }
                }


                return response;

            } catch (Exception e) {
                return response;
            }
        } catch (Exception e) {
            return response;
        }


    }

    class TCPThread extends Thread {
        @Override
        public void run() {
            String response = "";
            boolean resultsent = false;
            try {
                Socket socket = new Socket();
                SocketAddress address = new InetSocketAddress(ip, port);
                socket.connect(address, SOCKET_TIMEOUT_DURATION);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                if (gzip) {
                    OutputStream os = socket.getOutputStream();
                    byte[] m = compress(msgtosend);
                    os.write(m);
                }
                else {
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    out.flush();
                    out.write(msgtosend);
                    out.flush();
                }
                try {
                    char[] buffer = new char[20384];
                    int charsread = 0;
                    String part = "";

                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    @SuppressWarnings("unused") long timediff = 0;
                    Date starttime = new Date();


                    while ((timediff = new Date().getTime() - starttime.getTime()) < RESPONSE_TIMEOUT_DURATION) {
                        if (socket.getInputStream().available() > 0 && (charsread = in.read(buffer)) != -1) {
                            part = new String(buffer).substring(0, charsread);

                            if (part.contains(endflag)) {
                                status = SUCCESS;
                                part = part.replace(endflag, "");
                                response += part;

								
									/*out.write("#endconnect");
                                    out.flush();*/
                                Message msg = new Message();
                                msg.obj = response;
                                resulthandler.handleMessage(msg);
                                resultsent = true;
                                if (resultsent) return;
                                break;
                            }
                            else {
                                response += part;
                            }


                        }
                        else {

                        }
                    }


                    Message msg = new Message();
                    msg.obj = response;
                    resulthandler.handleMessage(msg);

                } catch (Exception e) {
                    if (!resultsent) {
                        status = ERROR;
                        Message msg = new Message();
                        msg.obj = ERROR_FLAG + e.toString();
                        resulthandler.handleMessage(msg);
                        resultsent = true;
                    }
                }
            } catch (Exception e) {
                if (!resultsent) {
                    status = ERROR;
                    Message msg = new Message();
                    msg.obj = ERROR_FLAG + e.toString();
                    resulthandler.handleMessage(msg);
                    resultsent = true;
                }
            }
        }
    }


    @SuppressLint("HandlerLeak")
    Handler resulthandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String result = (String) msg.obj;
            InvokeListeners(result);
        }
    };


    private void InvokeListeners(final String result) {
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                public void run() {

                    for (TCPCompleteListener listener : listeners) {
                        listener.TCPComplete(result, status, TCPConnector.this);
                    }

                    if (lockViews != null && lockViews.size() > 0) {
                        for (View view : lockViews) {
                            view.setEnabled(true);
                        }
                    }
                }
            });
        }
        else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    for (TCPCompleteListener listener : listeners) {
                        listener.TCPComplete(result, status, TCPConnector.this);
                    }

                    if (lockViews != null && lockViews.size() > 0) {
                        for (View view : lockViews) {
                            view.setEnabled(true);
                        }
                    }
                }
            });

        }
    }

    public interface TCPCompleteListener {
        public abstract void TCPComplete(String response, int status, TCPConnector connector);
    }


    public void AddListener(TCPCompleteListener listener, boolean cleanothers) {
        if (cleanothers) listeners = new ArrayList<TCPCompleteListener>();
        listeners.add(listener);
    }


    public static byte[] compress(final String str) {
        try {
            if ((str == null) || (str.length() == 0)) {
                return null;
            }


            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(obj);
            gzip.write(str.getBytes("UTF-8"));
            gzip.close();
            return obj.toByteArray();
        } catch (Exception ex) { return null;}

    }

    public static String GetValueByContext(Context ctx, String tag, String defaultvalue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(tag, defaultvalue);
    }

    public static void SetValueByContext(Context ctx, String tag, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString(tag, value).commit();
    }

}
