package com.wzj.wifi_direct;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wzj.bean.ChatModel;
import com.wzj.bean.Member;
import com.wzj.chat.ChatActivity;
import com.wzj.communication.ClientThread;
import com.wzj.communication.MemberServerThread;
import com.wzj.communication.ServerThread;
import com.wzj.communication.UDPBroadcast;
import com.wzj.service.SocketService;
import com.wzj.util.ComputeBandwidth;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;


/**
 * Created by wzj on 2017/1/17.
 */
/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends ListFragment implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener{

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    protected static final int SERVER_CHOOSE_FILE_RESULT_CODE = 40;
    protected static final int CHAT_RESULT_CODE = 60;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    public static final int PORT = 8990;
    public static final int MS_PORT = 8999;
    public static final String GO_ADDRESS = "192.168.49.1";
    private Handler mHandler;
    private static ServerThread serverReadThread;
    private static MemberServerThread memberServerThread;
    private Map<String, Map<String, Member>> memberMap = new LinkedHashMap<>();//记录所有的member包括自身
    private Map<String, Socket> tcpConnections = new HashMap<>();//记录所有连接
    private List<Member> memberList = new ArrayList<>();//页面显示（不包括自身）
    private String choiceIp;
    private String choiceMac;
    private WiFiDirectActivity activity;
    private WifiP2pDevice myDevice;
    public static int preGroupSize = 0;
    private SocketService socketService = null;
    private boolean isStop = false;
    private Map<String ,List<ChatModel>> chatMap = new LinkedHashMap<>();
    private ServiceConnection mConnection;
    private Timer timer = new Timer();
    private ComputeBandwidth computeBandwidth = new ComputeBandwidth();
    private float bandwidth = 0;
    public void setMyDevice(WifiP2pDevice myDevice) {
        this.myDevice = myDevice;
    }

    //onCreateView()：每次创建、绘制该Fragment的View组件时回调该方法，Fragment将会显示该方法返回的View组件。
    //onCreate()：当Fragment所在的Activity被启动完成后回调该方法。
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setListAdapter(new MemberListAdapter(getActivity(), R.layout.row_member, memberList));
        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();
        /*chatFlag = false;
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                socketService = ((SocketService.MBinder) service).getService();
                socketService.setTcpConnections(tcpConnections);
                Log.d("SocketService","设置tcpConnections");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent serviceIntent = new Intent(activity, SocketService.class);
        activity.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);*/

    }


    private void mapToList(Map<String, Map<String, Member>> memberMap){
        List<Member> temp =new ArrayList<>();
        for(Map.Entry<String, Map<String, Member>> map : memberMap.entrySet()){
            if(!map.getKey().equals(myDevice.deviceAddress)) {
                for(Map.Entry<String, Member> mapE : map.getValue().entrySet()){
                    temp.add(mapE.getValue());
                }
            }
        }
        memberList.clear();
        memberList.addAll(temp);
    }


    //适配器
    private class MemberListAdapter extends ArrayAdapter<Member> {
        private List<Member> items;
        public MemberListAdapter(Context context, int textViewResourceId,
                                 List<Member> items){
            super(context, textViewResourceId, items);
            this.items = items;
        }
        @Nullable
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                //LayoutInflater是用来找res/layout/下的xml布局文件，并且实例化；
                //而findViewById()是找xml布局文件下的具体widget控件(如Button、TextView等)。
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_member, null);
            }

            final Member member = items.get(position);
            if(member != null){
                TextView name = (TextView) v.findViewById(R.id.member_name);
                if(name != null){
                    name.setText(member.getDeviceName());
                }
                ImageView imageView = (ImageView)v.findViewById(R.id.send_picture);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println(member.getIpAddress());
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        choiceIp = member.getIpAddress();
                        if(info.isGroupOwner){
                            startActivityForResult(intent, SERVER_CHOOSE_FILE_RESULT_CODE);
                        }else{
                            startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                        }
                    }
                });
                imageView = (ImageView)v.findViewById(R.id.send_message);
                imageView.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        if(socketService == null){
                            mConnection = new ServiceConnection() {
                                @Override
                                public void onServiceConnected(ComponentName name, IBinder service) {
                                    socketService = ((SocketService.MBinder)service).getService();
                                    choiceIp = member.getIpAddress();
                                    Socket socket = tcpConnections.get(choiceIp);
                                    if(socket != null){
                                        socketService.setSocket(socket);
                                    }else if(socket == null){
                                        ClientThread clientThread = new ClientThread(activity, mHandler, choiceIp, MS_PORT, myDevice, tcpConnections);
                                        new Thread(clientThread).start();
                                        socket = tcpConnections.get(choiceIp);
                                        while (socket == null){
                                            //等待TCPConnections的更新
                                            socket = tcpConnections.get(choiceIp);
                                        }
                                        socketService.setSocket(socket);
                                        System.out.println("通过了！！！！！！！！");
                                    }

                                    System.out.println("service连接上了！！！！！！！！！");
                                }

                                @Override
                                public void onServiceDisconnected(ComponentName name) {
                                    socketService = null;
                                }
                            };
                            Intent serviceIntent = new Intent(activity, SocketService.class);
                            activity.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
                        }else{
                            System.out.println("service已经绑定！！！！！！！！！");
                            choiceIp = member.getIpAddress();
                            Socket socket = tcpConnections.get(choiceIp);
                            if(socket != null){
                                socketService.setSocket(socket);
                            }else if(socket == null){
                                ClientThread clientThread = new ClientThread(activity, mHandler, choiceIp, MS_PORT, myDevice, tcpConnections);
                                new Thread(clientThread).start();
                                socket = tcpConnections.get(choiceIp);
                                while (socket == null){
                                    //等待TCPConnections的更新
                                    socket = tcpConnections.get(choiceIp);
                                }
                                socketService.setSocket(socket);
                            }
                        }
                        Intent intent = new Intent();
                        intent.putExtra("my_name", myDevice.deviceName);
                        intent.putExtra("my_macAddress", myDevice.deviceAddress);
                        intent.putExtra("name",member.getDeviceName());
                        intent.putExtra("macAddress", member.getMacAddress());
                        choiceMac = member.getMacAddress();
                        Gson gson = new Gson();
                        List<ChatModel> models = new ArrayList<>();
                        if(chatMap.containsKey(member.getMacAddress())){
                            models = chatMap.get(member.getMacAddress());
                        }else{
                            chatMap.put(member.getMacAddress(), models);
                        }
                        String str = gson.toJson(models);
                        intent.putExtra("chat", str);
                        intent.setClass(activity, ChatActivity.class);
                        startActivityForResult(intent, CHAT_RESULT_CODE);
                    }
                });
            }
            return v;
        }
    }
    public void clearMembers(){
        memberList.clear();
        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (WiFiDirectActivity)context;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.setListAdapter(new MemberListAdapter(getActivity(), R.layout.row_member, memberList));
        mapToList(memberMap);
        System.out.println("MemberMap的大小："+memberMap.size());
        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        System.out.println("停止了！！！！！！！");
        isStop = true;
    }
    @Override
    public void onDestroy() {
        if(socketService != null){
            getActivity().unbindService(mConnection);
        }
        SharedPreferences sharedPreferences = activity.getSharedPreferences("DDFragment", Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        editor.putString("memberMap", gson.toJson(this.getMemberMap()));
        editor.putString("myDevice", gson.toJson(this.getMyDevice()));
        editor.commit();
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Gson gson =new Gson();
        SharedPreferences sharedPreferences = activity.getSharedPreferences("DDFragment", Context.MODE_PRIVATE);
        String mStr = sharedPreferences.getString("memberMap", "");
        String dStr = sharedPreferences.getString("myDevice", "");
        myDevice = gson.fromJson(dStr.trim(), new TypeToken<WifiP2pDevice>(){}.getType());
        Log.d("sharedPreferences", tcpConnections.size()+"/"+mStr);
        if(!mStr.equals("")){
            Map<String, Map<String, Member>> memberMapTemp = gson.fromJson(mStr.trim(), new TypeToken<Map<String, Map<String, Member>>>(){}.getType());
            memberMap = memberMapTemp;
            mapToList(memberMap);
            ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();
        }

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case 1:
                        Bundle bundle = msg.getData();
                        String file = bundle.getString("file");
                        if(file != null){
                            Intent intent = new Intent();
                            File image = new File(file);
                            intent.setAction(Intent.ACTION_VIEW);
                            //Android 7.0给其他应用传递 file:// URI 类型的Uri，可能会导致接受者无法访问该路径。
                            //因此，在Android7.0中尝试传递 file:// URI 会触发 FileUriExposedException
                            //使用FileProvider解决该问题
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            //intent.setDataAndType(Uri.parse("file://"+file), "image/*");
                            intent.setDataAndType(FileProvider.getUriForFile(activity,"com.wzj.wifi_direct.FileProvider", image),"image/*");
                            System.out.println(intent.getData());
                            activity.startActivity(intent);

                        }
                        break;
                    case 2:
                        Toast.makeText(activity, "File transfer finished!", Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        Toast.makeText(activity, "Broken pipe, retry please!", Toast.LENGTH_SHORT).show();
                        break;
                    case 4:
                        Bundle bundle1 = msg.getData();
                        Toast.makeText(activity, bundle1.getString("data")+" from "+bundle1.getString("address"), Toast.LENGTH_SHORT).show();
                        break;
                    case 5:
                        String str = msg.getData().getString("memberMap");
                        Gson gson =new Gson();
                        Map<String, Map<String, Member>> memberMapTemp = gson.fromJson(str.trim(), new TypeToken<Map<String, Map<String, Member>>>(){}.getType());
                        //memberMap有成员离开应该更新TCPConnections
                        String removeIp = "";
                        if(memberMapTemp.size() < memberMap.size()){
                            for(Map.Entry<String, Map<String, Member>> map : memberMap.entrySet()){
                                if(!memberMapTemp.containsKey(map.getKey())){
                                    for(Map.Entry<String, Member> member : map.getValue().entrySet()){
                                        removeIp = member.getKey();
                                    }
                                }
                            }
                            try {
                                if(tcpConnections.containsKey(removeIp)){
                                    tcpConnections.get(removeIp).close();
                                    tcpConnections.remove(removeIp);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        memberMap = memberMapTemp;
                        mapToList(memberMap);
                        for(Member member : memberList){
                            if(member.getIpAddress().equals(GO_ADDRESS)){
                                activity.setGroupOwnerMac(member.getMacAddress());
                            }
                        }
                        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();
                        if((getActivity()) != null){
                            if((((WiFiDirectActivity)getActivity()).getReceiver()).isConnected()
                                    && memberList.size() == 0){
                                clearMembers();
                            }else if(memberList.size() == 0){
                                //((WiFiDirectActivity)getActivity()).resetData();
                                Log.d(WiFiDirectActivity.TAG, "No member");
                            }
                        }
                        break;
                    case 6:
                        mapToList(memberMap);
                        Log.d("handler MemberList: ", ""+memberList.size());
                        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();
                        break;
                    case 7:
                        gson = new Gson();
                        String message = msg.getData().getString("message");
                        ChatModel chatModel = gson.fromJson(message.trim(), ChatModel.class);
                        if(chatMap.containsKey(chatModel.getMacAddress())){
                            chatMap.get(chatModel.getMacAddress()).add(chatModel);
                        }else{
                            List<ChatModel> model = new ArrayList<>();
                            model.add(chatModel);
                            chatMap.put(chatModel.getMacAddress(), model);
                        }
                        System.out.println("看这里！！！！！！！"+ chatMap.size());

                        if(isStop && chatModel.getMacAddress().equals(choiceMac)){
                            Handler cHandler = socketService.getHandler();
                            if(cHandler != null){
                                Message msge = new Message();
                                msge.what = 0;
                                Bundle bundle2 = new Bundle();
                                bundle2.putString("chat", message);
                                msge.setData(bundle2);
                                cHandler.sendMessage(msge);
                            }
                        }
                        break;
                    case 8:
                        String memberStr = msg.getData().getString("member");
                        Gson memberGson =new Gson();
                        Member member = memberGson.fromJson(memberStr.trim(), new TypeToken<Member>(){}.getType());
                        Map<String ,Member> tMap = new HashMap<>();
                        tMap.put(member.getIpAddress(), member);
                        memberMap.put(member.getMacAddress(), tMap);
                        mapToList(memberMap);
                        Log.d("case8",""+memberMap.size()+"/"+memberList.size());
                        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();

                        break;
                }
            }
        };
        mContentView = inflater.inflate(R.layout.device_detail, null);
        return mContentView;
    }

    @Override
    // 请求码在调用startActivityForResult(Intent intent, int requestCode)输入用于区别不同的业务
    // 返回码由新的Activity通过setResult(int resultCode)方法返回，以区分不同的Activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(null != data ){

            if(null != data.getData()){
                if(requestCode == CHOOSE_FILE_RESULT_CODE ){ //客户端发送图片
                    Uri uri = data.getData();
                    //statusText.setText("Sending: "+uri);
                    Log.d(WiFiDirectActivity.TAG, "Intent--------------"+uri);
                    Socket client = tcpConnections.get(choiceIp);
                    if(client != null){
                        ClientThread clientThread = new ClientThread(activity, mHandler, myDevice, tcpConnections);
                        clientThread.setUri(uri);
                        clientThread.setType("write");
                        clientThread.setSocket(client);
                        System.out.println("客户端写！！！！");
                        new Thread(clientThread).start();
                    }else if(client == null){
                        //开启读写线程，用于组员间通信
                        System.out.println("开启客户端读写线程！！！");
                        ClientThread clientThread = new ClientThread(activity, mHandler, choiceIp, MS_PORT, myDevice, tcpConnections);
                        clientThread.setUri(uri);
                        clientThread.setType("rw");
                        new Thread(clientThread).start();
                    }

                }else if(requestCode == SERVER_CHOOSE_FILE_RESULT_CODE){ //服务器端发送图片
                    /*Uri uri = data.getData();
                    Socket client = null;
                    client = tcpConnections.get(choiceIp);
                    Log.d(WiFiDirectActivity.TAG, "ServerSend:Intent--------------"+uri);
                    serverWriteThread =new ServerThread(activity, memberMap, mHandler, "write", tcpConnections, myDevice);
                    serverWriteThread.setSocket(client);
                    serverWriteThread.setFile(uri);
                    new Thread(serverWriteThread).start();*/
                    Socket client = tcpConnections.get(choiceIp);
                    Uri uri = data.getData();
                    ClientThread clientThread = new ClientThread(activity, mHandler, myDevice, tcpConnections);
                    clientThread.setUri(uri);
                    clientThread.setType("write");
                    clientThread.setSocket(client);
                    System.out.println("服务端写！！！！");
                    new Thread(clientThread).start();

                }
            }
            if(requestCode == CHAT_RESULT_CODE){
                String str = data.getStringExtra("chat_return");
                Gson gson = new Gson();
                System.out.println(str);
                List<ChatModel> chatModels = gson.fromJson(str, new TypeToken<List<ChatModel>>(){}.getType());
                if(chatModels != null && chatModels.size() > 0){
                    if(chatMap.containsKey(choiceMac)){
                        chatMap.get(choiceMac).clear();
                        chatMap.get(choiceMac).addAll(chatModels);
                    }else{
                        chatMap.put(choiceMac, chatModels);
                    }
                }
            }

        }else{
            Log.d(WiFiDirectActivity.TAG, "上一页面返回为空");
        }
    }

    @Override
    //P2p连接已经建立
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        System.out.println("连接信息变化！！！！");
        if(progressDialog !=null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView)mContentView.findViewById(R.id.group_ip);
        view.setText("Group Owner IP -" + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if(info.groupFormed && info.isGroupOwner){
            if(serverReadThread == null){
                    serverReadThread =new ServerThread(activity, memberMap, mHandler, "read", tcpConnections, myDevice);
                    new Thread(serverReadThread).start();
            }

        }else if(info.groupFormed){
            if(!tcpConnections.containsKey(GO_ADDRESS)){
                ClientThread clientThread = new ClientThread(activity, mHandler, GO_ADDRESS, PORT, myDevice, tcpConnections);
                new Thread(clientThread).start();
            }
            if(memberServerThread == null){
                memberServerThread = new MemberServerThread(activity, memberMap, mHandler, "read", tcpConnections);
                new Thread(memberServerThread).start();
            }
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        String ssid = group.getNetworkName();
        try {
            if(ssid.indexOf("\\") != -1){
                String[] subString = ssid.split("-");
                String sourceArr[] = subString[subString.length-1].split("\\\\");
                byte[] byteArr = new byte[sourceArr.length - 1];
                for (int i = 1; i < sourceArr.length; i++) {
                    Integer hexInt = Integer.decode("0" + sourceArr[i]);
                    byteArr[i - 1] = hexInt.byteValue();
                }
                ssid = "";
                for(int i = 0;i < subString.length-1;i++){
                    ssid+=subString[i]+"-";
                }
                ssid += new String(byteArr, "utf-8");
            }
            System.out.println("组信息变化！！！！"+ ssid);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ((WiFiDirectActivity)getActivity()).setSsid(ssid);
        ((WiFiDirectActivity)getActivity()).setIsGroupOwner(group.isGroupOwner());
        ((WiFiDirectActivity)getActivity()).setGroupSize(group.getClientList().size());
        if(group.isGroupOwner()){

            //定时器检测平均带宽
            if(timer == null){
                WiFiDirectActivity.dataSize = Long.valueOf(0);
                timer = new Timer();
                computeBandwidth = new ComputeBandwidth();
                timer.scheduleAtFixedRate(computeBandwidth, 1000, 5000);
            }
            if(activity.getMemberParametersCollection() != null){
                activity.getMemberParametersCollection().setFlag(false);
                activity.setMemberParametersCollection(null);
            }
        }
        /*mapToList(memberMap);
        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();*/
        Collection<WifiP2pDevice> deviceList = group.getClientList();
        //检测成员列表是否变化，删除离开的成员更新表
        if(group.isGroupOwner()){
            List<String> macAddress = new ArrayList<>();
            for(WifiP2pDevice device : deviceList){
                macAddress.add(device.deviceAddress);
            }
            if(deviceList.size() < preGroupSize){
                System.out.println("Member离开！！！！"+deviceList.size()+" "+preGroupSize);
                String removeIp = "";
                for(Map.Entry<String, Map<String, Member>> map : memberMap.entrySet()){
                    if(!group.getOwner().deviceAddress.equals(map.getKey()) && !macAddress.contains(map.getKey())){
                        for(Map.Entry<String, Member> memberEntry : map.getValue().entrySet()){
                            removeIp = memberEntry.getValue().getIpAddress();
                        }
                        memberMap.remove(map.getKey());
                        mapToList(memberMap);
                        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();
                        break;
                    }
                }
                try {
                    //关闭该设备对应的socket
                    if(tcpConnections.get(removeIp) != null){
                        tcpConnections.get(removeIp).close();
                    }
                    tcpConnections.remove(removeIp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //将memberList广播给其他成员
                UDPBroadcast udpBroadcast = new UDPBroadcast(memberMap);
                new Thread(udpBroadcast).start();
            }
            preGroupSize = deviceList.size();
        }

        String clients ="";
        for(WifiP2pDevice device :deviceList){
            if(device.deviceName.equals("")){
                clients = clients + "Legacy client - " + device.deviceAddress + "\n";
            }else{
                clients = clients + device.deviceName + "\n";
            }
        }
        TextView view = (TextView) mContentView.findViewById(R.id.group_info);
        String info;
        info = "Group Owner: " + group.getOwner().deviceName + "\n"
                + "Interface: " + group.getInterface()  + "\n"
                + "isGo: " + group.isGroupOwner() + "\n"
                + (group.isGroupOwner() ? "SSID: " + group.getNetworkName() + "\n"
                + "Passphrase: " + group.getPassphrase() + "\n"
                + "The number of clients: " + deviceList.size() + "\n"
                + "Clients: " + clients: "");
        view.setText(info);
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(final WifiP2pDevice device, View view){
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        /*if(device.status == WifiP2pDevice.AVAILABLE || device.status == WifiP2pDevice.INVITED){
            mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
            //mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        }else if(device.status == WifiP2pDevice.CONNECTED && info.isGroupOwner){
            mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
            //mContentView.findViewById(R.id.btn_start_server).setVisibility(View.VISIBLE);
        } else if(device.status == WifiP2pDevice.CONNECTED){
            mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
            //mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
        }else {
            mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
        }*/
       /* TextView view = (TextView)mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView)mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());*/

    }


    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews(){
        //mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view =(TextView)mContentView.findViewById(R.id.group_ip);
        view.setText(R.string.empty);
        view =(TextView)mContentView.findViewById(R.id.group_info);
        view.setText(R.string.empty);
        //view = (TextView)mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        //mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        //mContentView.findViewById(R.id.btn_start_server).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
        memberMap.clear();
        clearMembers();
        tcpConnections.clear();
        SharedPreferences sharedPreferences = activity.getSharedPreferences("DDFragment", Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.clear();
    }
    //Member被动断开
    public void closeConnections(){
        //Member断开时关闭MemberServerSocket
        if(memberServerThread != null){
            MemberServerThread.close();
            memberServerThread = null;
        }
        //关闭所有socket
        if(!tcpConnections.isEmpty()){
            for(Map.Entry<String, Socket> map : tcpConnections.entrySet()){
                try {
                    map.getValue().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //关闭DatagramSocket
        UDPBroadcast.close();
    }

    public void disconnect(){
        if(info != null){
            if(info.isGroupOwner){
                //GO断开连接时应关闭ServerSocket
                ServerThread.close();
                serverReadThread = null;

            }else {
                //Member断开时关闭MemberServerSocket
                MemberServerThread.close();
                memberServerThread = null;
            }
            //关闭所有socket
            for(Map.Entry<String, Socket> map : tcpConnections.entrySet()){
                try {
                    map.getValue().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭DatagramSocket
            UDPBroadcast.close();
        }
    }

    public WifiP2pConfig connect(){
        WifiP2pConfig config = null;
        if(device.status == WifiP2pDevice.CONNECTED){
            Toast.makeText(getActivity(), "This device is connected", Toast.LENGTH_SHORT).show();
        }else {
            config = new WifiP2pConfig();
            //所需连接设备的MAC地址
            //通过showDetail()方法初始化device,为列表中所选中的device
            config.deviceAddress = device.deviceAddress;
            Log.d(WiFiDirectActivity.TAG, "[] "+device.deviceAddress);
            //WpsInfo(WiFi Protected Setup)
            //PBC(Push Button Configuration)
            config.wps.setup = WpsInfo.PBC;
            config.groupOwnerIntent = 15;
            if(progressDialog != null && progressDialog.isShowing()){
                progressDialog.dismiss();
            }
            progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "Connecting to :" + device.deviceAddress, true, true, new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    ((DeviceListFragment.DeviceActionListener)getActivity()).cancel();
                }
            });
        }
        return config;
    }
    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public ComputeBandwidth getComputeBandwidth() {
        return computeBandwidth;
    }

    public void setComputeBandwidth(ComputeBandwidth computeBandwidth) {
        this.computeBandwidth = computeBandwidth;
    }

    public float getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(float bandwidth) {
        this.bandwidth = bandwidth;
    }

    public Map<String, Map<String, Member>> getMemberMap() {
        return memberMap;
    }

    public void setMemberMap(Map<String, Map<String, Member>> memberMap) {
        this.memberMap = memberMap;
    }

    public Map<String, Socket> getTcpConnections() {
        return tcpConnections;
    }

    public List<Member> getMemberList() {
        return memberList;
    }

    public WifiP2pDevice getMyDevice() {
        return myDevice;
    }
}
