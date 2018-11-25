package com.keove.connectorlibrary;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.keove.connectorlibrary.eventhub.GlobalEventApplication;

import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.mime.Header;
import cz.msebera.android.httpclient.entity.mime.HttpMultipartMode;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.entity.mime.content.FileBody;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.protocol.HTTP;
import okhttp3.CertificatePinner;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpConnector {

    public static boolean DEBUG_MODE = false;

    public enum CLIENT {

        APACHE_HTTP, HTTPURLCONNECTION, OK_HTTP
    }


    private CLIENT client = CLIENT.OK_HTTP;

    public String contract;
    public String url;
    public String extras = "";
    private boolean isPost = false;
    private ArrayList<NameValuePair> postparams = null;
    private ArrayList<NameValuePair> getheaders = null;
    private ArrayList<NameValuePair> postheaders = null;
    private File postFile = null;
    private String fileParamName = "";
    ArrayList<OnCompleteListener> listeners = new ArrayList<OnCompleteListener>();
    ArrayList<HttpCompleteListener> httplisteners = new ArrayList<HttpCompleteListener>();
    private RequestAsyncTask currenttask = null;
    private RequestThread currentthread = null;

    public int httpStatusCode = -1;


    public String rawBody = "";
    public boolean postRawBody = false;

    private boolean encryption = false;

    Activity act = null;
    private boolean stopthread = false;

    ArrayList<View> lockViews = new ArrayList<>();

    public ArrayList<NameValuePair> getPostparams() {
        return postparams;
    }

    public String getPostParamValue(String key) {
        for (NameValuePair param:postparams) {
            if(param.getName().contentEquals(key)) {
                return param.getValue();
            }
        }
        return "";
    }

    public HttpConnector(String url) {
        this.url = url;
    }

    public HttpConnector(Activity activity) {
        act = activity;
    }

    public HttpConnector(String url, Activity activity) {
        this.url = url;
        act = activity;
    }

    public HttpConnector() { }

    @SuppressWarnings("all") private void InvokeListeners(final int status, final String response) {
        currenttask = null;
        currentthread = null;


        if (DEBUG_MODE) {
            Log.d("HttpConnector " + contract, response);
        }

        stopthread = false;
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                public void run() {

                    if (lockViews != null && lockViews.size() > 0) {
                        for (View view : lockViews) {

                            if (view != null) {
                                try {
                                    view.setEnabled(true);
                                }
                                catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }

                    for (OnCompleteListener listener : listeners) {
                        listener.OnComplete(status, response);
                    }
                    for (HttpCompleteListener httpCompleteListener : httplisteners) {
                        if (httpCompleteListener != null)
                            httpCompleteListener.HttpComplete(response, status, HttpConnector.this);
                    }
                }
            });

        }
        else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {

                    if (lockViews != null && lockViews.size() > 0) {
                        for (View view : lockViews) {
                            if (view != null) {
                                try {
                                    view.setEnabled(true);
                                }
                                catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }

                    for (OnCompleteListener listener : listeners) {
                        listener.OnComplete(status, response);
                    }
                    for (HttpCompleteListener httpCompleteListener : httplisteners) {
                        if (httpCompleteListener != null)
                            httpCompleteListener.HttpComplete(response, status, HttpConnector.this);
                    }
                }
            });

        }
    }

    public void AddListener(OnCompleteListener listener, Boolean ClearOtherListeners) {
        if (ClearOtherListeners) listeners = new ArrayList<OnCompleteListener>();
        listeners.add(listener);
    }

    public void AddListener(HttpCompleteListener listener, Boolean ClearOtherListeners) {
        if (ClearOtherListeners) httplisteners = new ArrayList<HttpCompleteListener>();
        httplisteners.add(listener);
    }


    // region GET DEPRECATED

    @Deprecated
    public void GetResultWithGETAsync(String url) {
        this.url = url;
        currenttask = null;
        RequestThread thread = new RequestThread();
        currentthread = thread;
        currentthread.start();
    }

    @Deprecated
    public static void GetResultWithGetAsync(Activity activity, String contract, String url, HttpCompleteListener listener) {
        HttpConnector connector = new HttpConnector(activity);
        connector.contract = contract;
        connector.AddListener(listener, true);
        connector.GetResultWithGETAsync(url);
    }

    @Deprecated
    public static void GetResultWithGetAsync(final Activity activity, final String contract, final String url, final HttpCompleteListener listener, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.GetResultWithGETAsync(url);
            }
        }, delay);
    }

    @Deprecated
    public void GetResultWithGETAsync(String url, ArrayList<NameValuePair> headerparams) {
        getheaders = headerparams;
        this.url = url;
        currenttask = null;
        RequestThread thread = new RequestThread();
        // thread.setPriority(Thread.MAX_PRIORITY);
        currentthread = thread;
        currentthread.start();
    }

    @Deprecated
    public void GetResultWithGETAsync(String url, boolean useasynctask) {
        if (!useasynctask) {
            this.url = url;
            currenttask = null;
            RequestThread thread = new RequestThread();
            thread.start();
        }
        else {
            this.url = url;
            currentthread = null;
            currenttask = new RequestAsyncTask();
            currenttask.execute("");
        }
    }


    @Deprecated
    public void GetResultWithGETAsync(String url, ArrayList<NameValuePair> headerparams, String contract) {
        if (contract != null) this.contract = contract;
        getheaders = headerparams;
        this.url = url;
        currenttask = null;
        RequestThread thread = new RequestThread();
        currentthread = thread;
        currentthread.start();
    }

    // endregion


    //region POST DEPRECATED

    @Deprecated
    public void GetResultWithPOSTAsync(String url, ArrayList<NameValuePair> pairs) {
        isPost = true;
        postparams = pairs;
        this.url = url;
        RequestThread thread = new RequestThread();
        thread.start();
    }

    @Deprecated
    public void GetResultWithPOSTAsync(String url, ArrayList<NameValuePair> pairs, ArrayList<NameValuePair> headerparams) {
        postheaders = headerparams;
        GetResultWithPOSTAsync(url, pairs);
        /*
         * isPost = true; postparams = params; this.url = url; RequestThread thread = new RequestThread(); thread.start();
		 */
    }

    @Deprecated
    public void GetResultWithPOSTAsync(String url, ArrayList<NameValuePair> pairs, ArrayList<NameValuePair> headerparams, File postFile, String fileParamName) {
        this.postFile = postFile;
        this.fileParamName = fileParamName;
        GetResultWithPOSTAsync(url, pairs, headerparams);
    }


    @Deprecated
    public static void GetResultWithPOSTAsync(final Activity activity, final String contract, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams, View... lockViews) {


        for (View view : lockViews) {
            view.setEnabled(false);
        }

        GetResultWithPOSTAsync(activity, contract, listener, delay, url, pairs, headerparams);
    }

    @Deprecated
    public static void GetResultWithPOSTAsync(final Activity activity, final String contract, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.GetResultWithPOSTAsync(url, pairs, headerparams);
            }
        }, delay);
    }

    @Deprecated
    public static void GetResultWithPOSTAsync(final Activity activity, final String contract, final String extras, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.extras = extras;
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.GetResultWithPOSTAsync(url, pairs, headerparams);
            }
        }, delay);
    }

    @Deprecated
    public static void GetResultWithPOSTAsync(final Activity activity, final String contract, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams, final File file, final String fileParamName) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.GetResultWithPOSTAsync(url, pairs, headerparams, file, fileParamName);
            }
        }, delay);
    }

    // endregion


    // region GET

    public void getASYNC(String url) {
        this.url = url;
        currenttask = null;
        RequestThread thread = new RequestThread();
        currentthread = thread;
        currentthread.start();
    }

    public static void getASYNC(Activity activity, String contract, String url, HttpCompleteListener listener) {
        HttpConnector connector = new HttpConnector(activity);
        connector.contract = contract;
        connector.AddListener(listener, true);
        connector.getASYNC(url);
    }

    public static void getASYNC(final Activity activity, final String contract, final String url, final HttpCompleteListener listener, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.getASYNC(url);
            }
        }, delay);
    }

    public void getASYNC(String url, ArrayList<NameValuePair> headerparams) {
        getheaders = headerparams;
        this.url = url;
        currenttask = null;
        RequestThread thread = new RequestThread();
        // thread.setPriority(Thread.MAX_PRIORITY);
        currentthread = thread;
        currentthread.start();
    }

    public void getASYNC(String url, boolean useasynctask) {
        if (!useasynctask) {
            this.url = url;
            currenttask = null;
            RequestThread thread = new RequestThread();
            thread.start();
        }
        else {
            this.url = url;
            currentthread = null;
            currenttask = new RequestAsyncTask();
            currenttask.execute("");
        }
    }

    public void getASYNC(String url, ArrayList<NameValuePair> headerparams, String contract) {
        if (contract != null) this.contract = contract;
        getheaders = headerparams;
        this.url = url;
        currenttask = null;
        RequestThread thread = new RequestThread();
        currentthread = thread;
        currentthread.start();
    }

    // endregion


    // region POST
    public void postASYNC(String url, ArrayList<NameValuePair> pairs) {
        isPost = true;
        postparams = pairs;
        this.url = url;
        RequestThread thread = new RequestThread();
        thread.start();
    }

    public void postASYNC(String url, ArrayList<NameValuePair> pairs, ArrayList<NameValuePair> headerparams) {
        postheaders = headerparams;
        postASYNC(url, pairs);
        /*
         * isPost = true; postparams = params; this.url = url; RequestThread thread = new RequestThread(); thread.start();
		 */
    }

    public void postASYNC(String url, ArrayList<NameValuePair> pairs, ArrayList<NameValuePair> headerparams, File postFile, String fileParamName) {
        this.postFile = postFile;
        this.fileParamName = fileParamName;
        postASYNC(url, pairs, headerparams);
    }

    public static void postASYNC(final Activity activity, final String contract, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams, final View... lockViews) {
        if(lockViews!=null) {
            for (View view : lockViews) {
                if (view != null) {
                    try {
                        view.setEnabled(false);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

            }
        }


        //postASYNC(activity, contract, listener, delay, url, pairs, headerparams);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.lockViews = new ArrayList<View>();

                for (View view : lockViews) {
                    connector.lockViews.add(view);
                }

                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.postASYNC(url, pairs, headerparams);
            }
        }, delay);
    }


    public static void postASYNC(final Activity activity, final String contract, final HttpCompleteListener listener, int delay, final String url, final String rawBody, final ArrayList<NameValuePair> headerparams) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.postRawBody = true;
                connector.rawBody = rawBody;
                connector.postASYNC(url, null, headerparams);
            }
        }, delay);
    }

    public static void postASYNC(final Activity activity, final String contract, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.postASYNC(url, pairs, headerparams);
            }
        }, delay);
    }

    public static void postASYNC(final Activity activity, final String contract, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams, final boolean encryption) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.encryption = encryption;
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.postASYNC(url, pairs, headerparams);
            }
        }, delay);
    }

    public static void postASYNC(final Activity activity, final String contract, final String extras, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.extras = extras;
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.postASYNC(url, pairs, headerparams);
            }
        }, delay);
    }

    public static void postASYNC(final Activity activity, final String contract, final HttpCompleteListener listener, int delay, final String url, final ArrayList<NameValuePair> pairs, final ArrayList<NameValuePair> headerparams, final File file, final String fileParamName) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpConnector connector = new HttpConnector(activity);
                connector.contract = contract;
                connector.AddListener(listener, true);
                connector.postASYNC(url, pairs, headerparams, file, fileParamName);
            }
        }, delay);
    }

    // endregion

    public void Cancel() {
        if (currenttask != null) {
            currenttask.cancel(true);
        }
        if (currentthread != null) {
            stopthread = true;
        }
    }


    private class RequestThread extends Thread implements KeoveMultipartEntity.MultipartProgressListener {


        private String getWithOkHttpClient() {


            String result = "";

            try {
                Request.Builder builder = new Request.Builder();
                builder.header("User-Agent", "OkHttp Headers.java");

                if (getheaders != null) {

                    for (NameValuePair np : getheaders) {
                        builder.addHeader(np.getName(), np.getValue());
                    }
                }

                CertificatePinner certificatePinner = new CertificatePinner.Builder().add(
                        "edergilik.turktelekom.com.tr",
                        "sha256/7lMGjwHBE/ZskDiIHE20C7+QRSPsmSpw2u3OAub3Lgg=")
                        .add("edergilik.turktelekom.com.tr",
                                "sha256/klO23nT2ehFDXCfx3eHTDRESMz3asj1muO+4aIdjiuY=")
                        .add("edergilik.turktelekom.com.tr",
                                "sha256/grX4Ta9HpZx6tSHkmCrvpApTQGo67CYDnvprLg5yRME=")
                        .build();


                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(15,
                        TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();

                //.certificatePinner(certificatePinner).build();

                builder.url(url);
                Request request = builder.build();

                Response response = client.newCall(request).execute();
                httpStatusCode = response.code();
                result = response.body().string();
            }
            catch (Exception ex) {

            }

            /*String result = "";

            try {
                Request.Builder builder = new Request.Builder();
                builder.url(url);
                builder.header("User-Agent", "Keove");

                if (getheaders != null) {
                    for (NameValuePair np : getheaders) {
                        builder.addHeader(np.getName(), np.getValue());
                    }
                }

                Response response = new OkHttpClient().newCall(builder.build()).execute();
                if (response.isSuccessful()) {
                    result = response.body().string();
                }
            } catch (Exception ex) {

            }

            return result;*/

            return result;
        }


        private String getWithApacheClient() {

            String result = "";

            try {

                InputStream content = null;
                HttpClient client = HttpClientBuilder.create().build();


                //cz.msebera.android.httpclient.params.HttpParams params = client.getParams();

                //HttpParams params = client.getParams();
                //HttpConnectionParams.setConnectionTimeout(params, 30000);
                //HttpConnectionParams.setSoTimeout(params, 30000);
                //HttpConnectionParams.setTcpNoDelay(params, true);

                RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
                requestConfigBuilder.setConnectTimeout(30000);
                requestConfigBuilder.setConnectionRequestTimeout(30000);

                HttpGet get = new HttpGet(url);
                if (getheaders != null) {
                    for (NameValuePair np : getheaders) {
                        get.setHeader(np.getName(), np.getValue());
                    }
                }

                cz.msebera.android.httpclient.Header[] allHeaders = get.getAllHeaders();

                HttpResponse response = client.execute(get);
                content = response.getEntity().getContent();

                BufferedReader br = new BufferedReader(new InputStreamReader(content));

                String line = "";
                while ((line = br.readLine()) != null) {
                    result += line;
                    if (stopthread) {
                        return result;
                    }
                }

            }
            catch (Exception ex) {

                return ex.toString();
            }

            return result;
        }

        // region fuck that
        /*private String getWithApacheClient2() {

            String result = "";

            try {

                InputStream content = null;
                HttpClient client = new DefaultHttpClient();


                cz.msebera.android.httpclient.params.HttpParams params = client.getParams();

                //HttpParams params = client.getParams();
                HttpConnectionParams.setConnectionTimeout(params, 30000);
                HttpConnectionParams.setSoTimeout(params, 30000);
                HttpConnectionParams.setTcpNoDelay(params, true);
                HttpGet get = new HttpGet(url);
                if (getheaders != null) {
                    for (NameValuePair np : getheaders) {
                        get.setHeader(np.getName(), np.getValue());
                    }
                }

                cz.msebera.android.httpclient.Header[] allHeaders  = get.getAllHeaders();

                HttpResponse response = client.execute(get);
                content = response.getEntity().getContent();

                BufferedReader br = new BufferedReader(new InputStreamReader(content));

                String line = "";
                while ((line = br.readLine()) != null) {
                    result += line;
                    if (stopthread) {
                        return result;
                    }
                }

            } catch (Exception ex) {

                return ex.toString();
            }

            return result;
        }*/
        // endregion

        private String getWithAndroidClient() {

            String result = "";

            try {
                URL u = new URL(HttpConnector.this.url);
                HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(60000);
                urlConnection.setConnectTimeout(60000);
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);

                if (getheaders != null) {
                    for (NameValuePair np : getheaders) {
                        urlConnection.setRequestProperty(np.getName(), np.getValue());
                    }
                }

                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();


                BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }

                result = total.toString();
            }
            catch (Exception ex) {

                result = ex.toString();
            }

            return result;

        }

        private void doGET() {
            try {

                String result = "";

                if (client == CLIENT.APACHE_HTTP) {
                    result = getWithApacheClient();
                }
                else if (client == CLIENT.HTTPURLCONNECTION) {
                    result = getWithAndroidClient();
                }
                else if (client == CLIENT.OK_HTTP) {
                    result = getWithOkHttpClient();
                    //result = getWithApacheClient();
                }


                Message msg = new Message();
                msg.obj = result;
                RequestHandler.handleMessage(msg);

            }
            catch (Exception ex) {

                Message msg = new Message();
                msg.obj = ex.toString();
                RequestHandler.handleMessage(msg);
            }
        }


        private String postWithApacheClient() {

            String result = "";

            try {

                InputStream content = null;
                HttpClient client = HttpClientBuilder.create().build();
                //new DefaultHttpClient();
                HttpPost post = new HttpPost(url);


                //HttpParams params = client.getParams();
                //HttpConnectionParams.setConnectionTimeout(params, 30000);
                //HttpConnectionParams.setSoTimeout(params, 30000);
                //HttpConnectionParams.setTcpNoDelay(params, true);

                if (postheaders != null) {
                    for (NameValuePair np : postheaders) {
                        if (encryption) {
                            post.setHeader(np.getName(), encrypt(np.getValue()));
                        }
                        else {
                            post.setHeader(np.getName(), np.getValue());
                        }

                    }
                }

                if (postparams != null) {


                    ContentType ct = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    for (NameValuePair pair : postparams) {

                        if (encryption) {
                            builder.addTextBody(pair.getName(), encrypt(pair.getValue()),
                                    ct);
                        }
                        else {
                            builder.addTextBody(pair.getName(), pair.getValue(), ct);
                        }

                    }
                    if (postFile != null) {
                        builder.addPart(fileParamName, new FileBody(postFile));
                    }

                    HttpEntity entity = builder.build();

                    post.setEntity(entity);
                }

                HttpResponse response = client.execute(post);
                content = response.getEntity().getContent();
                BufferedReader br = new BufferedReader(new InputStreamReader(content));
                String line = "";
                while ((line = br.readLine()) != null) {
                    result += line;
                    if (stopthread) {
                        return result;
                    }
                }
            }
            catch (Exception ex) {

                result = ex.toString();
            }

            return result;
        }

        private String postWithOkHttpClient() {

            String result = "";


            try {

                Request.Builder builder = new Request.Builder();
                builder.header("User-Agent", "OkHttp Headers.java");

                if (postheaders != null) {

                    for (NameValuePair np : postheaders) {
                        if (encryption) {
                            builder.addHeader(np.getName(), encrypt(np.getValue()));
                        }
                        else {
                            builder.addHeader(np.getName(), np.getValue());
                        }

                    }
                }

                if(postRawBody) {
                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),rawBody);
                    builder.post(body);
                }

                else {

                    if(postFile != null) {

                        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder();
                        multipartBodyBuilder.setType(MultipartBody.FORM);

                        if(postparams != null) {
                            for(NameValuePair pair : postparams) {
                                if(encryption) {
                                    multipartBodyBuilder.addFormDataPart(pair.getName(),encrypt(pair.getValue()));
                                }
                                else {
                                    multipartBodyBuilder.addFormDataPart(pair.getName(),pair.getValue());
                                }
                            }
                        }


                        multipartBodyBuilder.addFormDataPart(fileParamName,postFile.getName(),
                                RequestBody.create(getMediaType(GlobalEventApplication.applicationContext,postFile.toURI()),postFile)
                        );

                        builder.post(multipartBodyBuilder.build());

                    }
                    else {

                        if (postparams != null) {

                            FormBody.Builder bodyBuilder = new FormBody.Builder();
                            for (NameValuePair pair : postparams) {

                                if (encryption) {
                                    bodyBuilder.addEncoded(pair.getName(),
                                            encrypt(pair.getValue()));
                                }
                                else {
                                    bodyBuilder.addEncoded(pair.getName(), pair.getValue());
                                }
                            }
                            builder.post(bodyBuilder.build());
                        }
                    }
                }






                CertificatePinner certificatePinner = new CertificatePinner.Builder().add(
                        "edergilik.turktelekom.com.tr",
                        "sha256/7lMGjwHBE/ZskDiIHE20C7+QRSPsmSpw2u3OAub3Lgg=")
                        .add("edergilik.turktelekom.com.tr",
                                "sha256/klO23nT2ehFDXCfx3eHTDRESMz3asj1muO+4aIdjiuY=")
                        .add("edergilik.turktelekom.com.tr",
                                "sha256/grX4Ta9HpZx6tSHkmCrvpApTQGo67CYDnvprLg5yRME=")
                        .build();


                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(25,
                        TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build();
                //.certificatePinner(certificatePinner).build();

                builder.url(url);
                Request request = builder.build();

                Response response = client.newCall(request).execute();
                httpStatusCode = response.code();

                result = response.body().string();

            }
            catch (Exception ex) {
                ex.printStackTrace();
                result = ex.toString();
            }

            return result;
        }


        private String postWithAndroidClient() {

            String result = "";

            try {
                URL u = new URL(HttpConnector.this.url);
                HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
                urlConnection.setReadTimeout(60000);
                urlConnection.setConnectTimeout(60000);
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                if (postheaders != null) {
                    for (NameValuePair np : postheaders) {
                        urlConnection.setRequestProperty(np.getName(), np.getValue());
                    }
                }


            }
            catch (Exception ex) {
                ex.printStackTrace();
            }


            return result;
        }

        private void doPOST() {

            String result = "";

            if (client == CLIENT.APACHE_HTTP) {
                result = postWithApacheClient();
            }
            else if (client == CLIENT.HTTPURLCONNECTION) {
                result = postWithAndroidClient();
            }
            else if (client == CLIENT.OK_HTTP) {
                result = postWithOkHttpClient();
            }


            Message msg = new Message();
            msg.obj = result;
            RequestHandler.handleMessage(msg);
        }

        public String encrypt(String val) {

            try {

                JNCryptor cryptor = new AES256JNCryptor();
                byte[] text = val.getBytes();
                String password = "1234ab";
                byte[] cipher = cryptor.encryptData(text,password.toCharArray());
                String base64 = Base64.encodeToString(cipher, Base64.DEFAULT);
                return base64;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return "";
            }

        }

        @Override
        public void run() {
            if (!isPost) {
                doGET();
            }
            else {
                doPOST();
            }
        }

        @Override
        public void transferred(long num) {
            // TODO Auto-generated method stub
        }
    }

    private okhttp3.MediaType getMediaType(Context context, URI uri1) {
        Uri uri = Uri.parse(uri1.toString());
        String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return okhttp3.MediaType.parse(mimeType);
    }

    private class RequestAsyncTask extends AsyncTask<String, Void, Void> {
        String resultstr = "";

        @Override
        protected Void doInBackground(String... params) {
            try {
                if (url != null) {
                    InputStream content = null;
                    HttpClient client = new DefaultHttpClient();
                    HttpResponse response = client.execute(new HttpGet(url));
                    content = response.getEntity().getContent();
                    BufferedReader br = new BufferedReader(new InputStreamReader(content));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        resultstr += line;
                    }
                }
                else {
                    Log.e("HttpConnector", "_url is null!");
                }
            }
            catch (Exception ex) {
                Log.e("HttpConnector", ex.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {


            if (url != null) {
                InvokeListeners(1, resultstr);
            }
            super.onPostExecute(result);
        }
    }

    public String decrypt(String base64) {

        try {
            String password = "1234ab";
            JNCryptor cryptor = new AES256JNCryptor();
            byte[] cipher = Base64.decode(base64, Base64.DEFAULT);
            byte[] text = cryptor.decryptData(cipher,password.toCharArray());
            return  new String(text);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }

    }

    Handler RequestHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String result = (String) msg.obj;

            /*
            if (result != null && result.contains("\"session\":\"invalid\"")) {
                GlobalEventApplication.fireEvent(edergiApplication.APP.getApplicationContext(),
                        MainActivity.SESSION_DROPPED, null, false, 0);
                return;
            }
            */


            if (encryption) {
                Log.i("HttpConnector", "result before decryption : \n" + result);
                result = decrypt(result);
            }
            InvokeListeners(1, result);
        }
    };

    public String GetResultWithGET(String url) {
        try {
            InputStream content = null;
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(new HttpGet(url));
            content = response.getEntity().getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(content));
            String result = "";
            String line = "";
            while ((line = br.readLine()) != null) {
                result += line;
            }
            return result;
        }
        catch (Exception ex) {
            return "error";
        }
    }

    public interface OnCompleteListener {
        public abstract void OnComplete(int status, String response);
    }

    public interface HttpCompleteListener {
        public abstract void HttpComplete(String response, int status, HttpConnector connector);
    }
}
