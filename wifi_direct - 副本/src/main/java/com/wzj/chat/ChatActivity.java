package com.wzj.chat;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wzj.adapter.ChatAdapter;
import com.wzj.bean.ChatModel;
import com.wzj.service.SocketService;
import com.wzj.communication.ClientThread;
import com.wzj.wifi_direct.R;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wzj on 2017/5/12.
 */

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText et;
    private TextView tvSend;
    private String content = "";
    private SocketService socketService;
    private Socket socket;    //传入到该Activity中的socket都已实例化，并建立了连接
    private String name;
    private String macAddress;
    private String my_name;
    private String my_macAddress;
    private List<ChatModel> chat = new ArrayList<>();
    private Handler mhandler;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            socketService = ((SocketService.MBinder)service).getService();
            socket = socketService.getSocket();
            socketService.setHandler(mhandler);
            System.out.println(socket.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            socketService = null;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mhandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case 0:
                        System.out.println(msg.getData().getString("chat"));
                        Gson gson = new Gson();
                        ChatModel chatModel = gson.fromJson(msg.getData().getString("chat"), ChatModel.class);
                        chat.add(chatModel);
                        adapter.add(chatModel);
                        if(adapter.getItemCount() != 0){
                            recyclerView.smoothScrollToPosition(adapter.getItemCount()-1);
                        }
                        break;
                }
            }
        };
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_main);

        recyclerView = (RecyclerView) findViewById(R.id.recylerView);
        et = (EditText) findViewById(R.id.et);
        tvSend = (TextView) findViewById(R.id.tvSend);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter = new ChatAdapter());
        initData();
        Intent serviceIntent = new Intent(this, SocketService.class);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        macAddress = intent.getStringExtra("macAddress");
        my_name = intent.getStringExtra("my_name");
        this.getSupportActionBar().setHomeButtonEnabled(true);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setTitle(name);
        my_macAddress = intent.getStringExtra("my_macAddress");
        String str = intent.getStringExtra("chat");
        System.out.println(str);
        Gson gson = new Gson();
        chat =  gson.fromJson(str.trim(), new TypeToken<List<ChatModel>>(){}.getType());
        adapter.replaceAll((ArrayList<ChatModel>) chat);
        if(adapter.getItemCount() != 0){
            recyclerView.smoothScrollToPosition(adapter.getItemCount()-1);
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent();
        Gson gson = new Gson();
        intent.putExtra("chat_return", gson.toJson(chat));
        setResult(100, intent);
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    @Override
    public void onBackPressed() {
        System.out.println("后退！！");
        Intent intent = new Intent();
        Gson gson = new Gson();
        intent.putExtra("chat_return", gson.toJson(chat));
        setResult(100, intent);
        this.finish();
    }

    private void initData() {
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                content = s.toString().trim();
            }
        });

        tvSend.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if(!content.equals("")){
                    ChatModel model = new ChatModel("http://img.my.csdn.net/uploads/201508/05/1438760758_6667.jpg",
                            my_name, my_macAddress, content, ChatModel.CHAT_B);
                    chat.add(model);
                    adapter.add(model);
                    ChatModel sendModel = new ChatModel("http://img.my.csdn.net/uploads/201508/05/1438760758_3497.jpg",
                            my_name, my_macAddress, content, ChatModel.CHAT_A);
                    Gson gson = new Gson();
                    String str = gson.toJson(sendModel);
                    ClientThread writeMessage = new ClientThread(socket, "message", str);
                    System.out.println("str---- "+str);
                    new Thread(writeMessage).start();
                    et.setText("");
                    hideKeyBorad(et);
                    if(adapter.getItemCount() != 0){
                        recyclerView.smoothScrollToPosition(adapter.getItemCount()-1);
                    }
                }

            }
        });

    }

    private void hideKeyBorad(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        }
    }
}
