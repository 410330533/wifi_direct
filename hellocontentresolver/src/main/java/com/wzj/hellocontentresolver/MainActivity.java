package com.wzj.hellocontentresolver;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final String WZJ_URI =
            "content://com.wzj.helloContentProvider.provider.books/book";
    private ContentResolver cr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cr = this.getContentResolver();
        Button b1 = (Button)findViewById(R.id.button1);
        Button b2 = (Button)findViewById(R.id.button2);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor cursor = cr.query(Uri.parse(WZJ_URI), null, null, null, null);
                String text="";
                while (false != cursor.moveToNext()){
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    text+=id + " " + name + "\n";
                }
                TextView tv = (TextView)findViewById(R.id.showText);
                tv.setText(text);
                cursor.close();
            }
        });

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues cv = new ContentValues();
                cv.put("bookname", "C++编程");
                int result = cr.update(Uri.parse(WZJ_URI), cv, null, null);
                if(result > 0){
                    Toast.makeText(MainActivity.this, "修改成功", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
