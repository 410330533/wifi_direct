package com.wzj.hellocontentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by wzj on 2016/12/20.
 */

public class HelloContentProvider extends ContentProvider {

    private static final UriMatcher um = new UriMatcher(UriMatcher.NO_MATCH);
    private SQLiteDatabase sqLite;
    private MyHelper helper;
    @Override
    public boolean onCreate() {
        um.addURI("com.wzj.helloContentProvider.provider.books", "book", 1);
        System.out.println(getContext().getFilesDir().getAbsolutePath().replace("files", "databases")+"/wzjdb");
        helper = new MyHelper(this.getContext(), "wzjdb", null ,1);
        sqLite = helper.getReadableDatabase();
        /*sqLite = SQLiteDatabase.openDatabase(getContext().getFilesDir().getAbsolutePath().replace("files", "databases")+ "/wzjdb", null,
                SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.CREATE_IF_NECESSARY);*/

       /* sqLite.execSQL("create table books ([id] integer primary key autoincrement not null," +
                "[bookname] varchar(30) not null)");*/

        ContentValues cv = new ContentValues();
        cv.put("bookname", "Android开发入门与实践");
        sqLite.insert("books", null, cv);
        cv.put("bookname", "Java编程思想");
        sqLite.insert("books", null, cv);
        return false;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if(um.match(uri) == 1){
            Cursor cursor = sqLite.query("books", null, null, null, null, null, null ,null);
            return cursor;
        }else {
            return null;
        }

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if(um.match(uri) == 1){
            int result = sqLite.update("books", values, "id = '6'", null);
            return result;
        }
        return 0;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }
}
