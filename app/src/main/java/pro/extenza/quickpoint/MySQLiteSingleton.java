package pro.extenza.quickpoint;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by torgash on 04.03.15.
 */
public class MySQLiteSingleton {

    private static final String TAG = "QUICKPOINT";
    static Cursor cursor;
    // The index (key) column name for use in where clauses.
    static final String KEY_ID = "_id";
    // The name and column index of each column in your database.
// These should be descriptive.
    public static final String DBNAME =
            "DBNAME";
    public static final String KEY_POST_TEXT =
            "POST_TEXT";
    static final String KEY_POST_TAGS =
            "POST_TAGS";

    static final String KEY_POST_PRIVATE =
            "PRIVATE";
    static String[] result_columns = new String[] {
            KEY_ID, KEY_POST_TEXT, KEY_POST_TAGS, KEY_POST_PRIVATE };
    // Specify the where clause that will limit our results.
    static String where = KEY_POST_TEXT + "= *" ;
    // Replace these with valid SQL statements as necessary.
    static String[] whereArgs = null;
    static String groupBy = null;
    static String having = null;
    static String order = null;
    static MyDBHelper myDBOpenHelper;
    static SQLiteDatabase db;
    private static Context contex;
    public static MySQLiteSingleton singleton;
    public MySQLiteSingleton() {
        //Let's first create a database
        if(null != contex){
            myDBOpenHelper = new MyDBHelper(contex,
            MyDBHelper.DATABASE_NAME, null,
            MyDBHelper.DATABASE_VERSION);
            db = myDBOpenHelper.getWritableDatabase();

        }

    }
    public static MySQLiteSingleton getInstance(){
        if(null == singleton) {
            singleton = new MySQLiteSingleton();
            return singleton;

        }else return singleton;

    }
    public static SQLiteDatabase getDataBase(Context _contex){
        if(contex != null && !contex.equals(_contex)){
            singleton = null;
            contex = _contex;
        }else contex = _contex;
        return MySQLiteSingleton.getInstance().db;
    }
    public static Cursor getDefaultCursor(Context _contex, String feedLinkHash){
        if(contex != null && !contex.equals(_contex)){
            singleton = null;
            contex = _contex;
        }else contex = _contex;
        cursor = MySQLiteSingleton.getInstance().db.query(feedLinkHash,
                result_columns, where,
                whereArgs, groupBy, having, order);
        return cursor;
    }

    public static boolean makeNewDBRecord(Context _contex, MyPost post, long time){

        if(contex != null && !contex.equals(_contex)){
            singleton = null;
            contex = _contex;
        }else contex = _contex;
        ContentValues values = new ContentValues();
        values.put(MyDBHelper.KEY_POST_TEXT, post.getText());
        values.put(MyDBHelper.KEY_POST_TAGS, post.getTags());

        values.put(MyDBHelper.KEY_POST_PRIVATE, post.getPrivate() ? "1" : "0");
        Log.d(TAG, "Making db-insert query of " + values.valueSet().toString());
        MySQLiteSingleton.getInstance().db.beginTransaction();
        long result = -1;
        try {
            result = MySQLiteSingleton.getInstance().db.insert(myDBOpenHelper.DATABASE_TABLE, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            //let's check what's left in the database
            cursor = MySQLiteSingleton.getInstance().db.query(myDBOpenHelper.DATABASE_TABLE,
                    result_columns, null,
                    whereArgs, groupBy, having, order);
            order = null;
            Log.d(TAG, "Values left in database: \n");
            if (cursor.moveToLast()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    Log.d(TAG, cursor.getString(0) + " - " + cursor.getString(2) + " - " + cursor.getString(1) + " - " + cursor.getInt(3) + "\n");
                    if (cursor.moveToPrevious()) continue;
                    else break;
                }
            }
        }
        if(result == -1) {
            Log.d(TAG, "Couldn't insert into table");
            return false;
        }
        else return true;
    }



    public static ArrayList<MyPost> getPostsInQueue(Context _contex){
        if(contex != null && !contex.equals(_contex)){
            singleton = null;
            contex = _contex;
        }else contex = _contex;
        ArrayList<MyPost> postList = new ArrayList<>();
//        order = KEY_NEWS_TIME + " DESC LIMIT " + String.valueOf(feedRequiredNumber);
        order = KEY_ID;
        cursor = MySQLiteSingleton.getInstance().db.query(myDBOpenHelper.DATABASE_TABLE,
                result_columns, null,
                whereArgs, groupBy, having, order);
        order = null;
        if (cursor.moveToLast()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                postList.add(new MyPost(cursor.getString(1), cursor.getString(2)));
                if(cursor.moveToPrevious()) continue; else break;
            }
        }
        return postList;
    }
    public static Cursor getDBCursor(Context _contex){
        if(contex != null && !contex.equals(_contex)){
            singleton = null;
            contex = _contex;
        }else contex = _contex;
        ArrayList<MyPost> postList = new ArrayList<>();
//        order = KEY_NEWS_TIME + " DESC LIMIT " + String.valueOf(feedRequiredNumber);
        order = KEY_ID;
        cursor = MySQLiteSingleton.getInstance().db.query(myDBOpenHelper.DATABASE_TABLE,
                result_columns, null,
                whereArgs, groupBy, having, order);

        return cursor;
    }
    public static void deletePostFromQueue(Context _contex, String id) {
        if(contex != null && !contex.equals(_contex)){
            singleton = null;
            contex = _contex;
        }else contex = _contex;
        MySQLiteSingleton.getInstance().db.beginTransaction();
        long result = 0;
        try {
            result = MySQLiteSingleton.getInstance().db.delete(myDBOpenHelper.DATABASE_TABLE, KEY_ID + " = " + id, null);
            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully deleted post " + id + " from database");

            //let's check what's left in the database
            cursor = MySQLiteSingleton.getInstance().db.query(myDBOpenHelper.DATABASE_TABLE,
                    result_columns, null,
                    whereArgs, groupBy, having, order);
            order = null;
            if (cursor.moveToLast()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    Log.d(TAG, "Values left in database: \n" + cursor.getString(2) + " - " + cursor.getString(0) + " - " + cursor.getString(1));
                    if (cursor.moveToPrevious()) continue;
                    else break;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            Log.d(TAG, "Couldn't delete from table");
        }
        finally {
            db.endTransaction();
        }

    }
    public static void clearManually() {
        myDBOpenHelper.reCreateManually(db);
    }
}
