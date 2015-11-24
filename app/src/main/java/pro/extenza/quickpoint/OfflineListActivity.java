package pro.extenza.quickpoint;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OfflineListActivity extends ListActivity {
    final String TAG = "QUICKPOINT";
    Toolbar toolbar;
    ArrayList<Map<String, String>> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_list);
        toolbar = (Toolbar) findViewById(R.id.bar);
        toolbar.setTitle(R.string.title_activity_offline_list);
        toolbar.inflateMenu(R.menu.menu_offline_list);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        Cursor cursor = MySQLiteSingleton.getDBCursor(this);
        if (cursor.moveToFirst()) {
            postList = new ArrayList<>();
            for (int i = 0; i < cursor.getCount(); i++) {
                Map<String, String> postMap = new HashMap<>();


                postMap.put("postText", cursor.getString(1));
                postMap.put("postTags", cursor.getString(2));
                postMap.put("postID", cursor.getString(0));
                postList.add(postMap);
                if (cursor.moveToNext()) continue;
                else break;
            }
            String[] from = {"postText", "postTags", "postID"};
            // массив ID View-компонентов, в которые будут вставлять данные
            int[] to = {R.id.tvPostText, R.id.tags, R.id.postNumber};

            // создаем адаптер
            final SimpleAdapter sAdapter = new SimpleAdapter(this, postList, R.layout.listitem,
                    from, to);

            // определяем список и присваиваем ему адаптер
            ListView listView = getListView();
            // Create a ListView-specific touch listener. ListViews are given special treatment because
            // by default they handle touches for their list items... i.e. they're in charge of drawing
            // the pressed state (the list selector), handling list item clicks, etc.
            SwipeDismissListViewTouchListener touchListener =
                    new SwipeDismissListViewTouchListener(
                            listView,
                            new SwipeDismissListViewTouchListener.DismissCallbacks() {
                                @Override
                                public boolean canDismiss(int position) {
                                    return true;
                                }

                                @Override
                                public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                    for (int position : reverseSortedPositions) {
                                        String id_to_delete = postList.get(position).get("postID");
                                        postList.remove(position);

                                        MySQLiteSingleton.deletePostFromQueue(OfflineListActivity.this, id_to_delete);
                                        Toast.makeText(OfflineListActivity.this, "Post deleted from queue", Toast.LENGTH_LONG).show();
                                    }
                                    sAdapter.notifyDataSetChanged();
                                }
                            });
            listView.setAdapter(sAdapter);
            listView.setOnTouchListener(touchListener);
            // Setting this scroll listener is required to ensure that during ListView scrolling,
            // we don't look for swipes.
            listView.setOnScrollListener(touchListener.makeScrollListener());


            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Log.d(TAG, "Item id = " + item.getItemId());
                    if (item.getItemId() == R.id.action_clear) {
                        MySQLiteSingleton.clearManually();
                        Toast.makeText(OfflineListActivity.this, "Queue cleared",
                                Toast.LENGTH_LONG).show();

                        postList.clear();
                        sAdapter.notifyDataSetChanged();
                    }
                    return false;
                }
            });
            toolbar.invalidate();

        }


    }

    /**
     * Called as part of the activity lifecycle when an activity is going into
     * the background, but has not (yet) been killed.  The counterpart to
     * {@link #onResume}.
     * <p/>
     * <p>When activity B is launched in front of activity A, this callback will
     * be invoked on A.  B will not be created until A's {@link #onPause} returns,
     * so be sure to not do anything lengthy here.
     * <p/>
     * <p>This callback is mostly used for saving any persistent state the
     * activity is editing, to present a "edit in place" model to the user and
     * making sure nothing is lost if there are not enough resources to start
     * the new activity without first killing this one.  This is also a good
     * place to do things like stop animations and other things that consume a
     * noticeable amount of CPU in order to make the switch to the next activity
     * as fast as possible, or to close resources that are exclusive access
     * such as the camera.
     * <p/>
     * <p>In situations where the system needs more memory it may kill paused
     * processes to reclaim resources.  Because of this, you should be sure
     * that all of your state is saved by the time you return from
     * this function.  In general {@link #onSaveInstanceState} is used to save
     * per-instance state in the activity and this method is used to store
     * global persistent data (in content providers, files, etc.)
     * <p/>
     * <p>After receiving this call you will usually receive a following call
     * to {@link #onStop} (after the next activity has been resumed and
     * displayed), however in some cases there will be a direct call back to
     * {@link #onResume} without going through the stopped state.
     * <p/>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onResume
     * @see #onSaveInstanceState
     * @see #onStop
     */
    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_offline_list, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_clear) {
//
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public class MyAdapter extends SimpleAdapter {

        /**
         * Constructor
         *
         * @param context  The context where the View associated with this SimpleAdapter is running
         * @param data     A List of Maps. Each entry in the List corresponds to one row in the list. The
         *                 Maps contain the data for each row, and should include all the entries specified in
         *                 "from"
         * @param resource Resource identifier of a view layout that defines the views for this list
         *                 item. The layout file should include at least those named views defined in "to"
         * @param from     A list of column names that will be added to the Map associated with each
         *                 item.
         * @param to       The views that should display column in the "from" parameter. These should all be
         *                 TextViews. The first N views in this list are given the values of the first N columns
         */
        public MyAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        /**
         * @param position
         * @param convertView
         * @param parent
         * @see Adapter#getView(int, View, ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }

    }

}
