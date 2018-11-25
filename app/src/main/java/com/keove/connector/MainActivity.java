package com.keove.connector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.keove.connectorlibrary.HttpConnector;

import org.json.JSONObject;

import java.net.URLDecoder;
import java.util.ArrayList;

import cz.msebera.android.httpclient.NameValuePair;

public class MainActivity extends AppCompatActivity {



    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
}
