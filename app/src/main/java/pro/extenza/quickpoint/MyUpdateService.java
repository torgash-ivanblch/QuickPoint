package pro.extenza.quickpoint;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by torgash on 04.03.15.
 */
public class MyUpdateService extends Service {
    private static final String TAG = "QUICKPOINT";
    public static final String DBTABLE = "qp";
    private Timer timer;
    //    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
    Context ctx;
    SharedPreferences prefs;
    boolean isTimerScheduled = false;
    private TimerTask updateTask = new TimerTask() {


        @Override
        public void run() {

            Log.d(TAG, "Task scheduled");
            isTimerScheduled = true;
            showQueueNotification();
            if (prefs.getLong("serviceUpdateFrequency", 15000L) == 0L) {
                Log.d(TAG, "Stopping service as zero update frequency detected in settings");
                isTimerScheduled = false;
                this.cancel();
                MyUpdateService.this.stopSelf();
            }
            if (getConnectivityStatus()) {
                isTimerScheduled = true;
                Cursor cursor = MySQLiteSingleton.getDBCursor(getApplicationContext());
                if (cursor.moveToFirst()) {


                    for (int i = 0; i < cursor.getCount(); i++) {



                        String[] post = new String[]{"", "", "", ""};

                        post[0] = cursor.getString(1);
                        post[1] = cursor.getString(2);
                        post[2] = cursor.getString(0);
                        post[3] = String.valueOf(cursor.getInt(3));
                        Log.d(TAG, "Trying to send post #" + post[0]);
                        new SendPostTask().execute(post);
                        try {
                            TimeUnit.MILLISECONDS.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (cursor.moveToNext()) continue;
                        else {
                            isTimerScheduled = false;
                            break;
                        }
                    }
                } else {
                    Log.d(TAG, "Service stopped due to cursor.moveToFirst() error");
                    isTimerScheduled = false;
                    Intent ishintent = new Intent(MyUpdateService.this, MyUpdateService.class);
                    PendingIntent pintent = PendingIntent.getService(MyUpdateService.this, 0, ishintent, 0);
                    AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                    alarm.cancel(pintent);
                    this.cancel();
                    MyUpdateService.this.stopSelf();

                }
            } else {
                isTimerScheduled = false;

                MyUpdateService.this.stopSelf();

            }


        }
    };

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

    }

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * {@link android.content.Context#startService}, providing the arguments it supplied and a
     * unique integer token representing the start request.  Do not call this method directly.
     * <p/>
     * <p>For backwards compatibility, the default implementation calls
     * {@link #onStart} and returns either {@link #START_STICKY}
     * or {@link #START_STICKY_COMPATIBILITY}.
     * <p/>
     * <p>If you need your application to run on platform versions prior to API
     * level 5, you can use the following model to handle the older {@link #onStart}
     * callback in that case.  The <code>handleCommand</code> method is implemented by
     * you as appropriate:
     * <p/>
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.java
     * start_compatibility}
     * <p/>
     * <p class="caution">Note that the system calls this on your
     * service's main thread.  A service's main thread is the same
     * thread where UI operations take place for Activities running in the
     * same process.  You should always avoid stalling the main
     * thread's event loop.  When doing long-running operations,
     * network calls, or heavy disk I/O, you should kick off a new
     * thread, or use {@link android.os.AsyncTask}.</p>
     *
     * @param intent  The Intent supplied to {@link android.content.Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.  Currently either
     *                0, {@link #START_FLAG_REDELIVERY}, or {@link #START_FLAG_RETRY}.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     * @return The return value indicates what semantics the system should
     * use for the service's current started state.  It may be one of the
     * constants associated with the {@link #START_CONTINUATION_MASK} bits.
     * @see #stopSelfResult(int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        timer = new Timer("UpdateTimer");


        if (prefs.getLong("serviceUpdateFrequency", 15000L) == 0l) {
            Log.d(TAG, "Service stopped due to zero update frequency set");
            MyUpdateService.this.stopSelf();
        } else {
            try {
                showQueueNotification();
                        if (!isTimerScheduled) {
                            timer.scheduleAtFixedRate(updateTask, 15000L, prefs.getLong("serviceUpdateFrequency", 15000L));
                        }
                        Log.d(TAG, "Service started");


                        return Service.START_STICKY;


                        //Do some work here




            } catch (IllegalStateException e) {
                e.printStackTrace();
                Log.d(TAG, "Service stopped due to exception" + e.toString());

                MyUpdateService.this.stopSelf();
            }
        }


        return Service.START_STICKY;


    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link android.os.IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     * <p/>
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link android.content.Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    int postResult;

    public class SendPostTask extends AsyncTask<String, Void, Integer> {

        public String ID;
        public String text;

        public void onPreExecute() {


        }

        @Override
        protected Integer doInBackground(String... params) {
            // TODO Auto-generated method stub
            PointHttpPost newPost;

            newPost = new PointHttpPost(getApplicationContext(), "/post",
                    params[0], params[1], !params[3].equals("0") );
            ID = params[2];
            text = params[0];
            postResult = 0;
            postResult = newPost.makePost();
            Log.d(TAG, "Got a postResult from posting");

            return postResult;
        }

        public void onCancelled() {

            super.onCancelled();
        }

        public void onPostExecute(Integer result) {


            Log.d(TAG, "We've come to PostExecute");
            switch (postResult) {
                case 0:
//                    Toast.makeText(MainActivity.this,
//                            "Message #" + message_id + " posted", Toast.LENGTH_LONG)
//                            .show();
                    Log.d(TAG, "Posted successfully");
                    try {
                        MySQLiteSingleton.deletePostFromQueue(getApplicationContext(), ID);
                    } catch (Exception e) {
                        //do nothing
                        Log.d(TAG, "Post wasn't removed from queue due to exception");
                    }

                    Notification.Builder builder =
                            new Notification.Builder(MyUpdateService.this);
                    builder.setSmallIcon(R.mipmap.ic_launcher)
                            .setTicker("Пост отправлен")
                            .setWhen(System.currentTimeMillis())
                            .setContentTitle("Пост отправлен")
                            .setContentText(text)
                            .setDefaults(Notification.DEFAULT_SOUND)
//                            .setSound(
//                                    RingtoneManager.getDefaultUri(
//                                            RingtoneManager.TYPE_NOTIFICATION))
//                            .setVibrate(new long[]{1000, 1000, 1000})
                            .setAutoCancel(true);
//                            .setLights(Color.WHITE, 0, 1);

                    Notification notification = null;
                    if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        notification = builder.getNotification();
                    } else {
                        notification = builder.build();
                    }
                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    notificationManager.notify(25, notification);
                    showQueueNotification();
                    break;
                case 1:
                    Log.d(TAG, "couldn't post: Error parsing JSON");

                    break;
                case 2:
                    Log.d(TAG, "couldn't post: ClientProtocolException");
                    break;
                case 3:
                    Log.d(TAG, "couldn't post: General IO exception");
                    break;
            }
            return;
        }

    }

    public boolean getConnectivityStatus() {
        ConnectivityManager connectivity = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (NetworkInfo networkInfo : info) {
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void showQueueNotification() {
        Cursor cursor = MySQLiteSingleton.getDBCursor(MyUpdateService.this);
        if (cursor.moveToFirst()) {
            Intent intent2 = new Intent(MyUpdateService.this, OfflineListActivity.class);

            intent2.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pIntent = PendingIntent.getActivity(MyUpdateService.this, 24, intent2, 0);
            Notification.Builder builder =
                    new Notification.Builder(MyUpdateService.this);
            builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker("Постов к отправке: " + cursor.getCount())
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(cursor.getCount() + " постов ожидает отправки")
                    .setContentText("Коснитесь для просмотра очереди")
                    .setContentIntent(pIntent)
                    .setSound(null);


            Notification notification = null;
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                notification = builder.getNotification();
            } else {
                notification = builder.build();
            }
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(24, notification);
        }else{
            NotificationManager notificationManager = (NotificationManager) MyUpdateService.this
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(24);
        }
    }
}
