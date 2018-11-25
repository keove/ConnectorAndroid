package com.keove.connectorlibrary;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpResponseException;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.params.HttpProtocolParams;
import cz.msebera.android.httpclient.util.EntityUtils;

public class WSHttpConnector
	{

		public static String WSHttpConnector_ERROR = "wshcerror";
		
		public static int SUCCESS;
		public static int FAIL;
		
		ArrayList<WSCompleteListener> listeners = new ArrayList<WSCompleteListener>();
		
		@SuppressWarnings("all")
		private String url;
		@SuppressWarnings("all")
		private String host;
		@SuppressWarnings("all")
		private int contimeout;
		@SuppressWarnings("all")
		private int sotimeout;
		
		
		private String asyncsoap;
		public String contract;
		private String asyncexception;
		private int STATUS;
		
		private Activity act;
		
		
		
		public WSHttpConnector(String url, int contimeout, int sotimeout, String host)
		{
			this.url = url;
			
			this.contimeout = contimeout;
			this.sotimeout = sotimeout;
			this.host = host;
			act = null;
		}
		
		
		public WSHttpConnector(String url, int contimeout, int sotimeout, String host, Activity activity)
		{
			this.url = url;
			
			this.contimeout = contimeout;
			this.sotimeout = sotimeout;
			this.host = host;
			act = activity;
		}
		
		
		
		
		
		public interface WSCompleteListener
		{
			public abstract void WSComplete(int status, String response, WSHttpConnector connector);
		}
		
		
		
		@SuppressWarnings("all")
		private void InvokeListeners(final int status, final String response)
		{
			if(act!=null)
			{
				try 
				{
					act.runOnUiThread(new Runnable()
					{
						
						public void run() 
						{
							for (WSCompleteListener listener : listeners)
				            {
					            listener.WSComplete(status, response,WSHttpConnector.this);
				            }
						}
					});
				}
				catch (Exception e)
				{

				}
				
				
			}
			else
			{
				for (WSCompleteListener listener : listeners)
	            {
		            listener.WSComplete(status, response, WSHttpConnector.this);
	            }
			}
		}
		
		public void AddListener(WSCompleteListener listener, Boolean CleanOtherListeners)
		{
			if(CleanOtherListeners) listeners = new ArrayList<WSCompleteListener>();
			listeners.add(listener);
		}
		
		
		
		private class RequestAsyncTask extends AsyncTask<String, Void, Void>
		{
			String response = "";
			
			@Override
			protected Void doInBackground(String... params)
			{
				try 
				{
					DefaultHttpClient client = new DefaultHttpClient();
					HttpParams hparams = client.getParams();
					HttpConnectionParams.setConnectionTimeout(hparams, contimeout);
					HttpConnectionParams.setSoTimeout(hparams, sotimeout);
					HttpProtocolParams.setUseExpectContinue(hparams, true);
					HttpPost post = new HttpPost(url);
					post.setHeader("soapaction",contract);
					post.setHeader("Content-Type", "text/xml; charset=utf-8");
					post.setHeader("Host", host);
					
					try
		            {
			            HttpEntity entity = new StringEntity(asyncsoap, "utf-8");
			            post.setEntity(entity);
			            ResponseHandlerTR rh = new ResponseHandlerTR();
			            response = client.execute(post,rh);
			            STATUS = SUCCESS;
		            }
		            catch (Exception e)
		            {
		            	STATUS = FAIL;
		            	response = e.toString();
		            }
					//Message msg = new Message();
					//msg.obj = response;
					//RequestHandler.handleMessage(msg);
				} 
				catch (Exception e)
				{
					STATUS = FAIL;
	            	response = e.toString();
	            	//Message msg = new Message();
					//msg.obj = response;
					//RequestHandler.handleMessage(msg);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result)
			{
				InvokeListeners(STATUS, response);
				super.onPostExecute(result);
				//super.onPostExecute(result);
			}
			
		}
		
		
		
		class RequestThread extends Thread
		{
			@Override
            public void run()
			{
				String response = "";
				try 
				{
					DefaultHttpClient client = new DefaultHttpClient();
					HttpParams params = client.getParams();
					HttpConnectionParams.setConnectionTimeout(params, contimeout);
					HttpConnectionParams.setSoTimeout(params, sotimeout);
					HttpProtocolParams.setUseExpectContinue(params, true);
					HttpPost post = new HttpPost(url);
					post.setHeader("soapaction",contract);
					post.setHeader("Content-Type", "text/xml; charset=utf-8");
					post.setHeader("Host", host);
					
					try
		            {
			            HttpEntity entity = new StringEntity(asyncsoap, "utf-8");
			            post.setEntity(entity);
			            ResponseHandlerTR rh = new ResponseHandlerTR();
			            response = client.execute(post,rh);
			            STATUS = SUCCESS;
		            }
		            catch (Exception e)
		            {
		            	STATUS = FAIL;
		            	response = e.toString();
		            }
					Message msg = new Message();
					msg.obj = response;
					RequestHandler.handleMessage(msg);
				} 
				catch (Exception e)
				{
					STATUS = FAIL;
	            	response = e.toString();
	            	Message msg = new Message();
					msg.obj = response;
					RequestHandler.handleMessage(msg);
				}
				
			}
		}
		
		Handler RequestHandler = new Handler()
		{
			@Override
            public void handleMessage(Message msg)
			{
				String result = (String)msg.obj;
				//result = result.replace("&amp;", "&");
				InvokeListeners(STATUS, result);
			}
		};

		
		
	
		public void GetSoapResultAsync(String envelope, String soapaction)
		{
			contract = soapaction;
			asyncsoap = envelope;
			RequestThread thread = new RequestThread();
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		
		public void GetSoapResultAsync(String envelope, String soapaction, boolean useasynctask)
		{
			if(!useasynctask)
			{
				contract = soapaction;
				asyncsoap = envelope;
				RequestThread thread = new RequestThread();
				thread.setPriority(Thread.MIN_PRIORITY);
				thread.start();
			}
			else
			{
				new RequestAsyncTask().execute("");
			}
		}
		
		
		public String GetSoapResult(String envelope, String soapaction)
		{
			DefaultHttpClient client = new DefaultHttpClient();
			HttpParams params = client.getParams();
			HttpConnectionParams.setConnectionTimeout(params, contimeout);
			HttpConnectionParams.setSoTimeout(params, sotimeout);
			HttpProtocolParams.setUseExpectContinue(params, true);
			HttpPost post = new HttpPost(url);
			post.setHeader("soapaction",soapaction);
			post.setHeader("Content-Type", "text/xml; charset=utf-8");
			post.setHeader("Host", host);
			String response;
			try
            {
	            HttpEntity entity = new StringEntity(envelope, "utf-8");
	            post.setEntity(entity);
	            ResponseHandlerTR rh = new ResponseHandlerTR();
	            response = client.execute(post,rh);
	            return response;
            }
            catch (Exception e)
            {
            	asyncexception = e.toString();
	            return WSHttpConnector_ERROR;
            }
			
		}
		
		
		
		
		/*
		 * Object for encoding http response in Turkish
		 * @author DeathknighT
		 *	
		 */
		class ResponseHandlerTR extends BasicResponseHandler
		{
			 @Override
			 public String handleResponse(HttpResponse response) throws HttpResponseException, IOException {
			    StatusLine statusLine = response.getStatusLine();
			         if (statusLine.getStatusCode() >= 300) {
			             throw new HttpResponseException(statusLine.getStatusCode(),
			                     statusLine.getReasonPhrase());
			         }

			         HttpEntity entity = response.getEntity();
			         return entity == null ? null : EntityUtils.toString(entity, "iso-8859-9");
			 }
		}
		
		
		
	}
