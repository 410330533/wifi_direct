package com.wzj.wifi_direct;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import com.wzj.bean.Network;
import com.wzj.chat.ChatActivity;
import com.wzj.communication.ClientThread;
import com.wzj.communication.MemberServerThread;
import com.wzj.communication.MulticastThread;
import com.wzj.communication.ServerThread;
import com.wzj.communication.UDPBroadcast;
import com.wzj.handover.GroupOwnerParametersCollection;
import com.wzj.service.SocketService;
import com.wzj.util.ComputeBandwidth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wzj.communication.MulticastThread.GO_MULTICAST_MEMMAP;
import static com.wzj.communication.MulticastThread.MULTICAST_WRITE;
import static com.wzj.communication.MulticastThread.P2P_INTERFACE_REGREX;


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
    public static final String TAG = "DeviceDetailFragment";
    ProgressDialog progressDialog = null;
    public static final int PORT = 8990;
    public static final int MS_PORT = 8999;
    public static final String GO_ADDRESS = "192.168.49.1";
    private Handler mHandler;
    private static ServerThread serverReadThread;
    private static MemberServerThread memberServerThread;
    private Map<String, Member> memberMap = new LinkedHashMap<>();//记录当前组所有的member包括自身
    private Map<String, Member> currentGroupMemberMap = new LinkedHashMap<>();
    private Map<String, Map<String, Member>> otherGroupMemberMap = new HashMap<>();//记录其他组的成员，第一个参数String为发送该信息的设备的MAC地址；用于区分发送方
    private Map<String, Socket> tcpConnections = new HashMap<>();//记录所有连接
    private List<Member> memberList = new ArrayList<>();//页面显示（不包括自身）
    private String choiceIp;
    private String choiceMac;
    private Boolean isCurrentGroup;
    private String macAddressofRelayClient;
    private char sentInterface;
    private String secondGOMac = "";
    private WiFiDirectActivity activity;
    private WifiP2pDevice myDevice;
    private int preGroupSize = 0;
    private Collection<WifiP2pDevice> preGroupList;
    private SocketService socketService = null;
    private boolean isStop = false;
    private Map<String ,List<ChatModel>> chatMap = new LinkedHashMap<>();
    private ServiceConnection mConnection;
    private Timer timer = new Timer();
    private ComputeBandwidth computeBandwidth = new ComputeBandwidth();
    private float bandwidth = 0;
    private static MulticastThread multicastP2pRead;
    private static MulticastThread multicastWlanRead;
    private static UDPBroadcast udpBroadcastRead;
    private boolean isGO = false;
    private Timer multicastTimer;


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


    private void mapToList(Map<String, Member> memberMap){
        Log.d("mapToList", "初始-"+memberMap.size());
        List<Member> temp = new ArrayList<>();
        for(Map.Entry<String, Member> map : memberMap.entrySet()){
            if(!map.getKey().equals(myDevice.deviceAddress)) {
                temp.add(map.getValue());
                Log.d("mapToList", "转换List-"+temp.size());
            }
        }
        memberList.clear();
        memberList.addAll(temp);
        ((MemberListAdapter)getListAdapter()).notifyDataSetChanged();
        Log.d("mapToList", "成员列表更新了~~");
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
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        choiceIp = member.getIpAddress();
                        choiceMac =member.getMacAddress();
                        isCurrentGroup = member.getisCurrentGroup();
                        macAddressofRelayClient = member.getMacAddressofRelay();
                        sentInterface = member.getSentInterface();
                        System.out.println(choiceIp+"/"+choiceMac+"/"+isCurrentGroup+"/"+macAddressofRelayClient);
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
                        System.out.println(member.getIpAddress());
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

                        //查找p2p接口的mac地址
                        Enumeration<NetworkInterface> networkInterfaces = null;
                        String myMacAddress = "";
                        try {
                            networkInterfaces = NetworkInterface.getNetworkInterfaces();
                            String regrex = "^p2p";
                            Pattern pattern = Pattern.compile(regrex);
                            while(networkInterfaces.hasMoreElements()){
                                NetworkInterface networkInterface = networkInterfaces.nextElement();
                                Matcher matcher = pattern.matcher(networkInterface.getName());
                                if(matcher.find()){
                                    myMacAddress = getMacFromBytes(networkInterface.getHardwareAddress());
                                    Log.d("NetworkInterface", "匹配/"+myMacAddress);
                                    break;
                                }
                            }
                        } catch (SocketException e) {
                            e.printStackTrace();
                        }
                        intent.putExtra("my_name", myDevice.deviceName);
                        intent.putExtra("my_macAddress", myMacAddress);
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
    public String getMacFromBytes(byte[] macBytes) {
        String mac = "";
        for(int i = 0;i < macBytes.length; i++){
            String sTemp = Integer.toHexString(0xFF &  macBytes[i]);
            if(sTemp.length() == 1){
                mac += "0" + sTemp + ":";
            }else {
                mac += sTemp + ":";
            }

        }

        mac = mac.substring(0,mac.lastIndexOf(":"));
        return mac;
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
        /*SharedPreferences sharedPreferences = activity.getSharedPreferences("DDFragment", Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        editor.putString("memberMap", gson.toJson(this.getMemberMap()));
        editor.putString("myDevice", gson.toJson(this.getMyDevice()));
        editor.commit();*/
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*Gson gson =new Gson();
        SharedPreferences sharedPreferences = activity.getSharedPreferences("DDFragment", Context.MODE_PRIVATE);
        String mStr = sharedPreferences.getString("memberMap", "");
        String dStr = sharedPreferences.getString("myDevice", "");
        myDevice = gson.fromJson(dStr.trim(), new TypeToken<WifiP2pDevice>(){}.getType());
        Log.d("sharedPreferences", tcpConnections.size()+"/"+mStr);
        if(!mStr.equals("")){
            Map<String, Member> memberMapTemp = gson.fromJson(mStr.trim(), new TypeToken<Map<String, Member>>(){}.getType());
            memberMap = memberMapTemp;
            mapToList(memberMap);
        }*/

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
                        if(!isGO){
                            String str = msg.getData().getString("memberMap");
                            Gson gson =new Gson();
                            Map<String, Member> memberMapTemp = gson.fromJson(str.trim(), new TypeToken<Map<String, Member>>(){}.getType());
                            memberMap.clear();
                            memberMap.putAll(memberMapTemp);
                            Iterator<Map.Entry<String, Socket>> iterator = tcpConnections.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<String, Socket> entry = iterator.next();
                                if (entry.getValue() == null || entry.getValue().isClosed()) {
                                    iterator.remove();
                                    Log.d(TAG, "迭代器删除tcpConnections中的元素-"+entry.getKey());
                                }
                            }
                        /*for(Entry<String, Member> entry : memberMapTemp.entrySet()){
                            memberMap.put(entry.getKey(), entry.getValue());
                        }*/
                            mapToList(memberMap);
                            for(Member member : memberList){
                                if(member.getIpAddress().equals(GO_ADDRESS)){
                                    activity.setGroupOwnerMac(member.getMacAddress());
                                }
                            }
                            if((getActivity()) != null){
                                if((((WiFiDirectActivity)getActivity()).getReceiver()).isConnected()
                                        && memberList.size() == 0){
                                    clearMembers();
                                }else if(memberList.size() == 0){
                                    //((WiFiDirectActivity)getActivity()).resetData();
                                    Log.d(WiFiDirectActivity.TAG, "No member");
                                }
                            }
                       /* //memberMap有成员离开应该更新TCPConnections
                        String removeIp = "";
                        if(memberMapTemp.size() < memberMap.size()){
                            for(Map.Entry<String, Member> map : memberMap.entrySet()){
                                if(!memberMapTemp.containsKey(map.getKey())){
                                    removeIp = map.getValue().getIpAddress();
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
                        }*/
                       /* memberMap = memberMapTemp;
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
                        }*/
                        }
                        break;
                    case 6:
                        Log.d("6 handler MemberList: ", ""+memberMap.size()+"/"+memberList.size());
                        mapToList(memberMap);
                        Log.d("6 handler MemberList: ", ""+memberList.size());
                        break;
                    case 7:
                        Gson gson = new Gson();
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
                        String sourceIp = msg.getData().getString("sourceIp");
                        Gson memberGson =new Gson();
                        Member member = memberGson.fromJson(memberStr.trim(), new TypeToken<Member>(){}.getType());
                        memberMapPut(member.getMacAddress(), member);
                        //检测是否已经存在组主
                        /*if (sourceIp.equals(GO_ADDRESS) && tcpConnections.containsKey(GO_ADDRESS) && !memberMap.containsKey(member.getMacAddress())){
                            String deviceName = member.getDeviceName();
                            String[] spllited = deviceName.split("\\)");
                            member.setDeviceName("(GO')"+spllited[1]);
                            member.setIpAddress("192.168.49.0");
                            memberMapPut(member.getMacAddress(), member);
                            secondGOMac = member.getMacAddress();
                            Log.d("case 8:", "增加第二个组主："+secondGOMac);
                        } else if(member.getMacAddress().equals(secondGOMac)){
                            String deviceName = member.getDeviceName();
                            String[] spllited = deviceName.split("\\)");
                            member.setDeviceName("(GO')"+spllited[1]);
                            member.setIpAddress("192.168.49.0");
                            memberMapPut(member.getMacAddress(), member);
                            Log.d("case 8:", "更新第二个组主："+secondGOMac);
                        }else if(!sourceIp.equals(member.getIpAddress())){ //检测是否为legacy用户
                            if(!sourceIp.equals(GO_ADDRESS)){
                                for(Entry<String, Member> entry : memberMap.entrySet()){
                                    if(entry.getValue().getIpAddress().equals(sourceIp)){
                                        Log.d("case 8: ", "找到legcay用户！"+ sourceIp);
                                        entry.getValue().setIpAddress(sourceIp);
                                    }
                                }
                            }else {

                            }
                        }else {
                            memberMapPut(member.getMacAddress(), member);
                        }*/
                        mapToList(memberMap);
                        Log.d("case8",""+memberMap.size()+"/"+memberList.size());
                        break;
                    case 9:
                        String delMemberStr = msg.getData().getString("member");
                        Gson delMemberGson = new Gson();
                        Member delMember = delMemberGson.fromJson(delMemberStr.trim(), new TypeToken<Member>(){}.getType());
                        if(null != msg.getData().getString("mac") && otherGroupMemberMap.containsKey(msg.getData().getString("mac"))){
                            Log.d(TAG, "组播删除成员！！");
                            otherGroupMemberMap.get(msg.getData().getString("mac")).remove(delMember.getMacAddress());
                        }

                        Log.d("case9","删除成员"+delMember.getIpAddress()+"/"+delMember.getMacAddress());
                        if(memberMap.containsKey(delMember.getMacAddress())){
                            memberMap.remove(delMember.getMacAddress());
                            if(tcpConnections.containsKey(delMember.getIpAddress())){
                                tcpConnections.remove(delMember.getIpAddress());
                                Log.d("case9", "tcpConnections已删除！");
                            }
                            tcpConnections.remove(delMember.getIpAddress());
                            Log.d("case9", "已删除！");
                            mapToList(memberMap);
                        }
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
                    case 10:
                        Log.d(TAG, "收到组播memberMap！！！");
                        /*if(currentGroupMemberMap.size() == 0){
                            Log.d(TAG, "收到其他组的memberMap，将本组的memberMap添加到currentGroupMemberMap！！！");
                            currentGroupMemberMap.putAll(memberMap);
                        }*/
                        String mac = msg.getData().getString("mac");
                        String multicastMemberMapStr = msg.getData().getString("memberMap");
                        char sentInterface = msg.getData().getChar("interface");
                        Gson multicastMemberMapGson = new Gson();
                        Map<String, Member> multicastMemberMap = multicastMemberMapGson.fromJson(multicastMemberMapStr.trim(), new TypeToken<Map<String, Member>>(){}.getType());
                        if(otherGroupMemberMap.size() != 0){
                            if(multicastMemberMap.size() < otherGroupMemberMap.get(mac).size()){
                                for(Entry<String, Member> entry : otherGroupMemberMap.get(mac).entrySet()){
                                    if(!multicastMemberMap.containsKey(entry.getKey()) && !entry.getKey().equals(mac)){
                                        Log.d(TAG, "找到之前未删除的其他组成员-"+entry.getKey());
                                        memberMap.remove(entry.getKey());
                                    }
                                }
                            }
                            otherGroupMemberMap.get(mac).clear();
                        }
                        otherGroupMemberMap.put(mac, multicastMemberMap);
                        String wiFiMacAddress = getWiFiMacAddress();
                        for(Entry<String, Member> entry : multicastMemberMap.entrySet()){
                            if(!entry.getKey().equals(wiFiMacAddress) && !entry.getKey().equals(myDevice.deviceAddress)){
                                entry.getValue().setMacAddressofRelay(mac);
                                entry.getValue().setisCurrentGroup(false);
                                entry.getValue().setSentInterface(sentInterface);
                                memberMap.put(entry.getKey(), entry.getValue());
                            }else {
                                Log.d(TAG, "mac相同不添加！！！"+ wiFiMacAddress + "/" + myDevice.deviceAddress);
                            }
                        }
                        mapToList(memberMap);
                        Log.d(TAG, "收到组播memberMap-广播给组员！！！");
                        UDPBroadcast udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_MEMMAP, memberMap);
                        new Thread(udpBroadcast).start();
                        break;
                    case 11:
                        Log.d(TAG, "case11:更新currentGroupMemberMap");
                        String str = msg.getData().getString("currentMemberMap");
                        Gson currentMemberMapGson = new Gson();
                        Map<String, Member> memberMapTemp = currentMemberMapGson.fromJson(str.trim(), new TypeToken<Map<String, Member>>(){}.getType());
                        currentGroupMemberMap.clear();
                        currentGroupMemberMap.putAll(memberMapTemp);
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
                    if(isCurrentGroup){
                        Log.d(TAG, "组内通信");
                        if (client != null){
                            ClientThread clientThread = new ClientThread(activity, mHandler, myDevice);
                            clientThread.setUri(uri);
                            clientThread.setType("write");
                            clientThread.setSocket(client);
                            System.out.println("客户端写！！！！");
                            new Thread(clientThread).start();
                        }else {
                            //开启读写线程，用于组员间通信
                            System.out.println("开启客户端读写线程！！！");
                            ClientThread clientThread = new ClientThread(activity, mHandler, choiceIp, MS_PORT, myDevice, tcpConnections);
                            clientThread.setUri(uri);
                            clientThread.setType("rw");
                            new Thread(clientThread).start();
                        }
                    }else if(memberMap.containsKey(choiceMac)){
                        Log.d(TAG, "开启组间通信");
                        if(sentInterface == 'p'){
                            //自右向左，发给GO中转
                            Socket go = tcpConnections.get(GO_ADDRESS);
                            ClientThread clientThread = new ClientThread(activity, mHandler, myDevice);
                            clientThread.setUri(uri);
                            clientThread.setType("relay-p");
                            clientThread.setSocket(go);
                            clientThread.setMacAddress(choiceMac);
                            System.out.println("组间通信-GO中转！！！！");
                            new Thread(clientThread).start();
                        }else {
                            if (client != null){
                                ClientThread clientThread = new ClientThread(activity, mHandler, myDevice);
                                clientThread.setUri(uri);
                                clientThread.setType("relay");
                                clientThread.setSocket(client);
                                clientThread.setMacAddress(choiceMac);
                                System.out.println("组间通信-客户端写！！！！");
                                new Thread(clientThread).start();
                            }else {
                                //开启读写线程，用于组员间通信
                                System.out.println("组间通信-开启客户端读写线程！！！");
                                Member relayClient = memberMap.get(macAddressofRelayClient);
                                ClientThread clientThread = new ClientThread(activity, mHandler, relayClient.getIpAddress(), MS_PORT, myDevice, tcpConnections);
                                clientThread.setUri(uri);
                                clientThread.setType("relay-rw");
                                clientThread.setMacAddress(choiceMac);
                                new Thread(clientThread).start();
                            }
                        }
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
                    if(isCurrentGroup){
                        Log.d(TAG, "组内通信");
                        if (client != null){
                            Uri uri = data.getData();
                            ClientThread clientThread = new ClientThread(activity, mHandler, myDevice);
                            clientThread.setUri(uri);
                            clientThread.setType("write");
                            clientThread.setSocket(client);
                            System.out.println("服务端写！！！！");
                            new Thread(clientThread).start();
                        }else {
                            Uri uri = data.getData();
                            Socket relayClient = null;
                            for(Entry<String, Socket> entry : tcpConnections.entrySet()){
                                if(entry.getValue() != null && !entry.getValue().isClosed()){
                                    relayClient = entry.getValue();
                                    Log.d(TAG, "选择RelyClient - "+relayClient.getInetAddress().getHostAddress());
                                    break;
                                }
                            }
                            ClientThread clientThread = new ClientThread(activity, mHandler, myDevice);
                            clientThread.setUri(uri);
                            clientThread.setType("relay");
                            clientThread.setSocket(relayClient);
                            clientThread.setMacAddress(choiceMac);
                            System.out.println("组间通信-RelayClient服务端写！！！！");
                            new Thread(clientThread).start();
                        }
                    }else if(memberMap.containsKey(choiceMac)){
                        Log.d(TAG, "开启组间通信");
                        if (client != null){
                            Uri uri = data.getData();
                            ClientThread clientThread = new ClientThread(activity, mHandler, myDevice);
                            clientThread.setUri(uri);
                            clientThread.setType("write");
                            clientThread.setSocket(client);
                            System.out.println("组间通信-服务端写！！！！");
                            new Thread(clientThread).start();
                        }else {
                            Uri uri = data.getData();
                            Socket relayClient = null;
                            for(Entry<String, Socket> entry : tcpConnections.entrySet()){
                                if(entry.getValue() != null && !entry.getValue().isClosed()){
                                    relayClient = entry.getValue();
                                    Log.d(TAG, "选择RelyClient - "+relayClient.getInetAddress().getHostAddress());
                                    break;
                                }
                            }
                            ClientThread clientThread = new ClientThread(activity, mHandler, myDevice);
                            clientThread.setUri(uri);
                            clientThread.setType("relay");
                            clientThread.setSocket(relayClient);
                            clientThread.setMacAddress(choiceMac);
                            System.out.println("组间通信-RelayClient服务端写！！！！");
                            new Thread(clientThread).start();
                        }
                    }


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
        this.isGO = info.isGroupOwner;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView)mContentView.findViewById(R.id.group_ip);
        //view.setText("Group Owner IP -" + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if(info.groupFormed && info.isGroupOwner){
            if(serverReadThread == null){
                serverReadThread =new ServerThread(activity, memberMap, mHandler, "read", tcpConnections, myDevice);
                new Thread(serverReadThread).start();
            }
            if(memberServerThread == null){
                memberServerThread = new MemberServerThread(activity, memberMap, mHandler, "read", tcpConnections);
                new Thread(memberServerThread).start();
            }
            if(multicastP2pRead == null){
                multicastP2pRead = new MulticastThread(activity, MulticastThread.MULTICAST_READ, mHandler, P2P_INTERFACE_REGREX);
                new Thread(multicastP2pRead).start();
                Log.d(TAG, "创建muiticastP2pRead!");
            }
            //若以legacy连接到WFD网络则开启multicastWlanRead，否则由WiFiBroadcastReceiver负责开启
            if(multicastWlanRead == null){
                WifiManager wifiManager = activity.getWifiManager();
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ssid = wifiInfo.getSSID();
                int intIPAddress = wifiInfo.getIpAddress();
                String ipAddress = String.format("%d.%d.%d.%d", (intIPAddress & 0xff), (intIPAddress >> 8 & 0xff), (intIPAddress >> 16 & 0xff),
                        (intIPAddress >> 24 & 0xff));
                Log.d(TAG, ssid +"/"+ ipAddress);
                String ssidRegrex = "^\"DIRECT-[a-zA-Z 0-9]+-[a-zA-Z _0-9]+\"";
                String ipRegrex = "^192.168.49.[1-9]+";
                Pattern ssidPattern = Pattern.compile(ssidRegrex);
                Matcher ssidMatcher = ssidPattern.matcher(ssid);
                Pattern ipPattern = Pattern.compile(ipRegrex);
                Matcher ipMatcher = ipPattern.matcher(ipAddress);
                if(ssidMatcher.matches() && ipMatcher.matches()){
                    Log.d(TAG, "以legacy已连接到WiFi Direct网络！");
                    multicastWlanRead = new MulticastThread(activity, MulticastThread.MULTICAST_READ, mHandler, MulticastThread.WLAN_INTERFACE_REGREX);
                    new Thread(multicastWlanRead).start();
                    Log.d(TAG, "创建muiticastWlanRead!");
                    setPeriodMulticastWlanWrite();
                    Log.d(TAG, "创建muiticastPeroidWlanWrite!");
                }

            }
            //清理candidateNetworks,设置所有Network的isGroupOwner为false
            Map<String, Network> candidateNetworks = activity.getCandidateNetworks();
            if(candidateNetworks != null && candidateNetworks.size() != 0){
                Log.d("清理candidateNetworks", "设置所有Network的isGroupOwner为false");
                for(Entry<String ,Network> entry : candidateNetworks.entrySet()){
                    entry.getValue().setGroupOwner(false);
                }
            }
        }else if(info.groupFormed){
            if(!tcpConnections.containsKey(GO_ADDRESS)){
                //传入membermap
                ClientThread clientThread = new ClientThread(activity, mHandler, GO_ADDRESS, PORT, myDevice, tcpConnections, this.memberMap);
                new Thread(clientThread).start();
                /*multicastP2pRead = new MulticastThread(MulticastThread.MULTICAST_READ, mHandler, MulticastThread.P2P_INTERFACE_REGREX);
                new Thread(multicastP2pRead).start();*/
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
            System.out.println("组信息变化！！！！"+ ssid +"/"+group.getPassphrase());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ((WiFiDirectActivity)getActivity()).setSsid(ssid);
        ((WiFiDirectActivity)getActivity()).setIsGroupOwner(group.isGroupOwner());
        ((WiFiDirectActivity)getActivity()).setMemberServiceDiscovery(group.isGroupOwner());
        ((WiFiDirectActivity)getActivity()).setGroupSize(group.getClientList().size());
        if(group.isGroupOwner()){
            //定时器检测平均带宽
            if(timer == null){
                WiFiDirectActivity.dataSize = Long.valueOf(0);
                timer = new Timer();
                computeBandwidth = new ComputeBandwidth();
                timer.scheduleAtFixedRate(computeBandwidth, 1000, 5000);
            }
            //关闭组员监听线程
            if(activity.getMemberParametersCollection() != null){
                activity.getMemberParametersCollection().setFlag(false);
                activity.setMemberParametersCollection(null);
                Log.d(TAG, "关闭组员监听线程！！");
            }
            //开启组主监听线程
            if(activity.getGroupOwnerParametersCollection() == null && activity.getPower() < 0.25){
                final GroupOwnerParametersCollection groupOwnerParametersCollection = new GroupOwnerParametersCollection(activity, activity.getBatteryReceiver());
                activity.setGroupOwnerParametersCollection(groupOwnerParametersCollection);
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        WiFiDirectActivity.threadPoolExecutor.execute(groupOwnerParametersCollection);
                        Log.d(TAG, "开启组主监听线程！！");
                    }
                }, 3000);
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
                String removeMac = "";
                for (WifiP2pDevice device : preGroupList){
                    if(!macAddress.contains(device.deviceAddress)){
                        removeMac = device.deviceAddress;
                        Log.d(TAG, "找到离开的组员-"+removeMac);
                    }
                }
                removeIp = memberMap.get(removeMac).getIpAddress();
                if(otherGroupMemberMap.containsKey(removeMac)){
                    Log.d(TAG, "删除的成员为legacy（GO2）- 删除GO2组的成员");
                    String[] removeMACs = new String[otherGroupMemberMap.get(removeMac).size()];
                    int index = 0;
                    for(Entry<String, Member> entry : otherGroupMemberMap.get(removeMac).entrySet()){
                        removeMACs[index++] = entry.getValue().getMacAddress();
                    }
                    otherGroupMemberMap.get(removeMac).clear();
                    otherGroupMemberMap.remove(removeMac);
                    for(int i = 0; i < removeMACs.length; i++){
                        memberMap.remove(removeMACs[i]);
                    }
                    memberMapRemove(removeMac);
                    mapToList(memberMap);
                    //所需要删除的其他组成员
                    UDPBroadcast udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_MEMMAP, memberMap);
                    new Thread(udpBroadcast).start();
                }else {
                    memberMapRemove(removeMac);
                    mapToList(memberMap);
                    //将删除的member广播给其他成员
                    UDPBroadcast udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.DELETE_MEMBER, new Member(removeIp, "", removeMac));
                    new Thread(udpBroadcast).start();

                    //再次广播
                    udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.DELETE_MEMBER, new Member(removeIp, "", removeMac));
                    new Thread(udpBroadcast).start();

                }
                //将删除的member组播给其他组主;若删除的为gateway node则需要在接收组播时，将该gateway node下的成员全部删除
                MulticastThread multicastWlanWrite = new MulticastThread(activity, MulticastThread.MULTICAST_WRITE, MulticastThread.GO_MULTICAST_DELETE, MulticastThread.WLAN_INTERFACE_REGREX, new Member(removeIp, "", removeMac));
                new Thread(multicastWlanWrite).start();
                MulticastThread multicastP2pWrite = new MulticastThread(activity, MulticastThread.MULTICAST_WRITE, MulticastThread.GO_MULTICAST_DELETE, P2P_INTERFACE_REGREX, new Member(removeIp, "", removeMac));
                new Thread(multicastP2pWrite).start();
                /*for(Map.Entry<String, Member> map : memberMap.entrySet()){
                    if(!group.getOwner().deviceAddress.equals(map.getKey()) && !macAddress.contains(map.getKey())){
                        removeIp = map.getValue().getIpAddress();
                        removeMac = map.getValue().getMacAddress();
                        memberMap.remove(map.getKey());
                        mapToList(memberMap);
                        break;
                    }
                }*/
                try {
                    //关闭该设备对应的socket
                    if(tcpConnections.get(removeIp) != null){
                        tcpConnections.get(removeIp).close();
                    }
                    tcpConnections.remove(removeIp);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            preGroupSize = deviceList.size();
            preGroupList = deviceList;
            //将currentGroupMemberMap广播给组员
            /*UDPBroadcast udpBroadcast = new UDPBroadcast(UDPBroadcast.BROADCAST_WRITE, UDPBroadcast.ADD_CURRENT_MEMMAP, currentGroupMemberMap);
            new Thread(udpBroadcast).start();*/
        }

        String clients ="";
        for(final WifiP2pDevice device :deviceList){
            if(device.deviceName.equals("") ){
                if(!memberMap.containsKey(device.deviceAddress)){
                    //周期性遍历ARP表，直到找到指定mac地址的IP为止
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getIPFromMac(device, 0);
                        }
                    }, 1000);
                    Member member = new Member("", "Legacy("+device.deviceAddress+")", device.deviceAddress);
                    memberMap.put(device.deviceAddress, member);
                    currentGroupMemberMap.put(device.deviceAddress, member);
                    //更新成员列表
                    mapToList(memberMap);
                    //this.memberMapPut(device.deviceAddress, new Member("", "Legacy("+device.deviceAddress+")", device.deviceAddress));
                }
                clients = clients + "Legacy client - " + device.deviceAddress + "\n";
            }else{
                if(!memberMap.containsKey(device.deviceAddress)){
                    //周期性遍历ARP表，直到找到指定mac地址的IP为止
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getIPFromMac(device, 1);
                        }
                    }, 1000);
                    Member member = new Member("", device.deviceName, device.deviceAddress);
                    memberMap.put(device.deviceAddress, member);
                    currentGroupMemberMap.put(device.deviceAddress, member);
                    //更新成员列表
                    mapToList(memberMap);
                    //this.memberMapPut(device.deviceAddress, new Member("", device.deviceName, device.deviceAddress));
                }
                clients = clients + device.deviceName + " - "+ device.deviceAddress +"\n";
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
        Log.d("组内成员", clients);
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
        currentGroupMemberMap.clear();
        otherGroupMemberMap.clear();
        memberMap.clear();
        clearMembers();
        tcpConnections.clear();
        preGroupSize = 0;
        preGroupList = null;
        /*SharedPreferences sharedPreferences = activity.getSharedPreferences("DDFragment", Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.clear();*/
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
        if(udpBroadcastRead != null){
            udpBroadcastRead.close();
            udpBroadcastRead = null;
        }
    }

    public void disconnect(){
        Log.d("DeviceDetailFrag", "断开连接");
        if(info != null){
            if(info.isGroupOwner){
                //GO断开连接时应关闭ServerSocket
                ServerThread.close();
                serverReadThread = null;
                if(memberServerThread != null){
                    MemberServerThread.close();
                    memberServerThread = null;
                }

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
            if(udpBroadcastRead != null){
                udpBroadcastRead.close();
                udpBroadcastRead = null;
            }

            //关闭MulticastSocket
            if(multicastP2pRead != null){
                multicastP2pRead.close();
                multicastP2pRead = null;
            }
            if(multicastWlanRead != null){
                multicastWlanRead.close();
                multicastWlanRead = null;
            }

            if(multicastTimer != null){
                multicastTimer.cancel();
                multicastTimer = null;
            }
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
    public String getWiFiMacAddress(){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/sys/class/net/wlan0/address"));
            String mac = bufferedReader.readLine();
            Log.d("读取本机WiFi Mac地址：", mac);
            return mac.trim();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void getIPFromMac(final WifiP2pDevice device, final int type){
        try {
            int count = 0;
            while (true){
                BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"));
                String line = bufferedReader.readLine();
                while ((line = bufferedReader.readLine()) != null){
                    String[] splitted = line.split(" +");
                    if(splitted != null && splitted.length > 4){
                        if(device.deviceAddress.matches(splitted[3])){
                            if(type == 0){
                                Log.d("getIPFromMac: ", "找到Legacy IP/"+splitted[0]);
                                Member member = new Member(splitted[0], "Legacy("+device.deviceAddress+")", device.deviceAddress);
                                memberMap.put(device.deviceAddress, member);
                                currentGroupMemberMap.put(device.deviceAddress, member);
                                //组播成员列表
                                MulticastThread multicastWlanWrite = new MulticastThread(activity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, MulticastThread.WLAN_INTERFACE_REGREX, memberMap, myDevice.deviceAddress);
                                new Thread(multicastWlanWrite).start();
                                MulticastThread multicastP2pWrite = new MulticastThread(activity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, P2P_INTERFACE_REGREX, memberMap, myDevice.deviceAddress);
                                new Thread(multicastP2pWrite).start();
                            }else if (type == 1){
                                Log.d("getIPFromMac: ", "找到非Legacy IP/"+splitted[0]);
                                Member member = new Member(splitted[0], device.deviceName, device.deviceAddress);
                                memberMap.put(device.deviceAddress, member);
                                currentGroupMemberMap.put(device.deviceAddress, member);
                            }
                            Message msg = new Message();
                            msg.what = 6;
                            mHandler.sendMessage(msg);
                            return;
                        }
                    }
                }
                if(type == 0){
                    Log.d("getIPFromMac", "ARP表中没有找到Legcay IP");
                }else if (type == 1){
                    Log.d("getIPFromMac", "ARP表中没有找到非Legcay IP");
                }
                if(count > 15){
                    return;
                }
                count++;
                Thread.sleep(1000);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

    public Map<String, Member> getMemberMap() {
        return memberMap;
    }

    public void setMemberMap(Map<String, Member> memberMap) {
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

    public Handler getmHandler() {
        return mHandler;
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }
    public void setMyDevice(WifiP2pDevice myDevice) {
        this.myDevice = myDevice;
    }

    public boolean isGO() {
        return isGO;
    }

    public void setMulticastWlanRead() {
        if(multicastWlanRead == null && isGO){
            multicastWlanRead = new MulticastThread(activity, MulticastThread.MULTICAST_READ, mHandler, MulticastThread.WLAN_INTERFACE_REGREX);
            new Thread(multicastWlanRead).start();
            Log.d(TAG, "创建muiticastWlanRead!");
        }
    }

    public MulticastThread getMulticastWlanRead() {
        return multicastWlanRead;
    }

    public void setMulticastWlanRead(MulticastThread multicastWlanRead) {
        DeviceDetailFragment.multicastWlanRead = multicastWlanRead;
    }

    public void setPeriodMulticastWlanWrite(){
        if(isGO && multicastTimer == null){
            multicastTimer = new Timer();
            multicastTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(otherGroupMemberMap.size() == 0 && memberMap.size() != 0){
                        Log.d(TAG, "wlan组播memberMap");
                        MulticastThread multicastThread = new MulticastThread(activity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, MulticastThread.WLAN_INTERFACE_REGREX, memberMap, myDevice.deviceAddress);
                        new Thread(multicastThread).start();
                    }else if(currentGroupMemberMap.size() != 0){
                        Log.d(TAG, "wlan组播currentGroupMemberMap");
                        MulticastThread multicastThread = new MulticastThread(activity, MULTICAST_WRITE, GO_MULTICAST_MEMMAP, MulticastThread.WLAN_INTERFACE_REGREX, currentGroupMemberMap, myDevice.deviceAddress);
                        new Thread(multicastThread).start();
                    }
                }
            },0, 1000*60);
        }
    }

    public void setIsGO(boolean GO) {
        isGO = GO;
    }

    public Map<String, Map<String, Member>> getOtherGroupMemberMap() {
        return otherGroupMemberMap;
    }

    public Map<String, Member> getCurrentGroupMemberMap() {
        return currentGroupMemberMap;
    }

    public void memberMapPut(String key, Member value){
        if(currentGroupMemberMap.containsKey(key)){
            memberMap.put(key, value);
            currentGroupMemberMap.put(key, value);
        }else {
            memberMap.put(key, value);
        }
    }

    public void memberMapRemove(String key){
        if(currentGroupMemberMap.containsKey(key)){
            memberMap.remove(key);
            currentGroupMemberMap.remove(key);
        }else {
            memberMap.remove(key);
        }
    }
    public Map<String, Member> getMapofCurrentGroup(){
        if(otherGroupMemberMap.size() != 0){
            return currentGroupMemberMap;
        }else {
            return memberMap;
        }
    }

    public static UDPBroadcast getUdpBroadcastRead() {
        return udpBroadcastRead;
    }

    public static void setUdpBroadcastRead(UDPBroadcast udpBroadcastRead) {
        if(DeviceDetailFragment.udpBroadcastRead == null){
            DeviceDetailFragment.udpBroadcastRead = udpBroadcastRead;
        }
    }
}
