package com.wzj.wifi_direct;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.wzj.wifi_direct.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";


    public FileTransferService() {
        super("FileTransferService");
    }
    public FileTransferService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if(intent.getAction().equals(ACTION_SEND_FILE)){
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket -");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host,port)),SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "Client socket -"+socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream in =null;
                try{
                    in = cr.openInputStream(Uri.parse(fileUri));
                    //Demo代码不完整
                    byte buf[] = new byte[1024];
                    int length;
                    while ((length = in.read(buf)) != -1){
                        //将buf中从0到length个字节写到输出流
                        stream.write(buf, 0, length);
                    }
                    in.close();
                    stream.flush();
                    stream.close();
                }catch(FileNotFoundException e){
                    Log.d(WiFiDirectActivity.TAG,e.toString());
                }
            }catch (IOException e){
                Log.d(WiFiDirectActivity.TAG, e.toString());
            }finally {
                /*if(socket != null){
                    if(socket.isConnected()){
                        try{
                            socket.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }

                    }
                }*/
            }
        }
    }


}
