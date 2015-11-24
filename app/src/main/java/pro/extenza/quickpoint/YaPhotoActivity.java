package pro.extenza.quickpoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class YaPhotoActivity extends Activity {
    final Activity activ = this;
    String TAG = "QUICKPOINT";
    String yaToken;
    String yaHost = "api-fotki.yandex.ru";
    SharedPreferences prefs;
    final String SVCDOCFILENAME = "svcdoc";
    final String ALBUMLISTFILENAME = "albumlist";
    String albumListLink = "";
    String albumListTitle = "";
    ArrayList<String[]> albumArray;
    YaServiceProgressDialog yaSPDialog;
    final String UPLOADEDIMGINFO = "imginfo";
    final int DIALOG_PROBLEM = 1;
    final int NETWORK_PROBLEM = 2;
    final int SERVER_RESPONSE_PROBLEM = 3;
    int defaultSize;
    Boolean quipointAlbum;
    int resolution;
    Context contex;
    String[] imageUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ya_photo);
        contex = this.getApplicationContext();
        prefs = getApplicationContext().getSharedPreferences("prefs", 0);
        resolution = prefs.getInt("resolution", 0);
        if (!prefs.contains("yaToken")) {
            yaAuthorize();
        }
        yaToken = prefs.getString("yaToken", "");
        // now let's call YaServiceProgressDialog and retrieve service document
        if (!yaToken.equals(""))
            new GetServiceDocumentTask().execute();

    }

    public void albumListUpdate(View v) {

        new GetServiceDocumentTask().execute();
    }

    protected Dialog onCreateDialog(int id) {

        AlertDialog.Builder adb = new AlertDialog.Builder(this);

        adb.setTitle("ERROR");

        switch (id) {
            case 1:
                adb.setMessage("Unexpected error. Please report to @torgash.");
                break;
            case 2:
                adb.setMessage("Network problem");
                break;
            case 3:
                adb.setMessage("Server response problem");
                break;
        }


        adb.setIcon(android.R.drawable.ic_delete);

        adb.setPositiveButton("Close", myClickListener);

        return adb.create();

    }

    OnClickListener myClickListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {

                case Dialog.BUTTON_POSITIVE:
                    finish();

                    break;

            }
        }
    };

    protected void onResume() {
        defaultSize = prefs.getInt("defaultSize", 0);
        quipointAlbum = prefs.getBoolean("defQPAlbum", false);

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem mi = menu.add(0, 1, 0, getString(R.string.preferences));
        mi.setIntent(new Intent(this, SettingsActivity.class));
        return super.onCreateOptionsMenu(menu);
    }

    // yaAuthorize() is void because login dialog returns value through
    // getTokenUrl()
    public void yaAuthorize() {

        DialogFragment yaLoginDig;
        yaLoginDig = new YaLoginDialog();
        yaLoginDig.setCancelable(false);
        yaLoginDig.show(getFragmentManager(), "yalogindig");

    }

    public void getTokenUrl(String returnedUrl) {
        String tokenString = "";
        if (!returnedUrl.contains("error")) {
            tokenString = returnedUrl.substring(
                    returnedUrl.indexOf("access_token=") + 13,
                    returnedUrl.indexOf("&token_type"));
        }// insert "else" and handle unauthorized errors;
        Log.d("QUICKPOINT", "The URL was " + returnedUrl);
        Log.d("QUICKPOINT", "Got a token " + tokenString + " from it.");
        Editor editor = prefs.edit();
        Log.d(TAG, "token put to preferences");
        editor.putString("yaToken", tokenString);
        editor.apply();
        new GetServiceDocumentTask().execute();
    }

    public class GetServiceDocumentTask extends AsyncTask<Void, String, String> {
        // here we retriever service document and album list

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub

            // first let's get user data from /api/me because the app is unaware
            // of username during OAuth authorization
            publishProgress("Getting Service document...");
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("http://" + yaHost + "/api/me/");
            try {
                // Add your data
                // httpget.setHeader("Host", yaHost);
                httpget.setHeader("Authorization",
                        "OAuth " + prefs.getString("yaToken", ""));
                Log.d(TAG, "GET " + httpget.getURI());
                Log.d(TAG, "Request line: " + httpget.getRequestLine());
                Header[] getHeaders = httpget.getAllHeaders();
                for (int i = 0; i < getHeaders.length; i++) {
                    Log.d(TAG, "Header " + i + ": " + getHeaders[i].getName()
                            + ": " + getHeaders[i].getValue());
                }

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httpget);
                Log.d(TAG, "api/me result is: "
                        + +response.getStatusLine().getStatusCode()
                        + response.getStatusLine().getReasonPhrase());
                Log.d(TAG, "Full api/me response is " + response.toString());
                Header[] headers = response.getAllHeaders();
                Log.d(TAG, "Excluded " + headers.length + " headers:");
                for (int i = 0; i < headers.length; i++) {
                    Log.d(TAG, "Header " + i + ": " + headers[i].getName()
                            + ": " + headers[i].getValue());
                }
                if (response.getStatusLine().getStatusCode() == java.net.HttpURLConnection.HTTP_OK) {
                    HttpEntity httpEntity = response.getEntity();
                    Log.d(TAG, "Full entity is: " + httpEntity.toString());
                    InputStream userDataStream = AndroidHttpClient
                            .getUngzippedContent(httpEntity);
                    Log.d(TAG,
                            "Full userdatastream is: "
                                    + userDataStream.toString());
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(userDataStream));
                    StringBuilder responseBuilder = new StringBuilder();
                    char[] buff = new char[1024 * 512];
                    int read;
                    while ((read = bufferedReader.read(buff)) != -1) {
                        responseBuilder.append(buff, 0, read);
                        Log.d("DOWNLOAD", "downloaded " + responseBuilder.length());
                    }
                    String result = responseBuilder.toString();
                    Log.d(TAG, "Response from /api/me is: " + result);
                    publishProgress("done");
                    return result;
                } else
                    return null;
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "ClientProtocolException raised: " + e);

                e.printStackTrace();
                return null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "IOException raised: " + e);

                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            yaSPDialog = new YaServiceProgressDialog();
            yaSPDialog.show(getFragmentManager(), "yaSPdig");


        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {

                yaSPDialog.dismiss();
                Toast.makeText(YaPhotoActivity.this, "CHECK YOUR INTERNET CONNECTION", Toast.LENGTH_LONG).show();
                return;
            }
            // TODO Auto-generated method stub
            File svcdocfile = new File(getFilesDir(), SVCDOCFILENAME);
            Log.d(TAG, "File created: " + svcdocfile.toString());
            try {


                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                        openFileOutput(SVCDOCFILENAME, MODE_PRIVATE)));

                if (result != null) {
                    bw.write(result);
                }

                bw.close();

                Log.d(TAG, "File written: " + svcdocfile.getAbsolutePath()
                        + " ");
                InputStream in = null;
                try {
                    in = new BufferedInputStream(
                            new FileInputStream(svcdocfile));
                    Log.d(TAG, "File reading: " + in.toString());
                } finally {
                    if (in != null)
                        in.close();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // yaSPDialog.dismiss();

            // here and therefore we try to parse XMl;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document dom = null;
            try {

                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();

                // parse using builder to get DOM representation of the XML file
                dom = db.parse(svcdocfile);
                Log.d(TAG, dom.toString());

            } catch (ParserConfigurationException pce) {
                pce.printStackTrace();
                showDialog(SERVER_RESPONSE_PROBLEM);
                Log.d(TAG, "ParserConfigurationException: " + pce.toString());
            } catch (SAXException se) {
                se.printStackTrace();
                showDialog(SERVER_RESPONSE_PROBLEM);
                Log.d(TAG, "SAX Exception: " + se.toString());
            } catch (IOException ioe) {
                ioe.printStackTrace();
                showDialog(SERVER_RESPONSE_PROBLEM);
                Log.d(TAG, "IOException: " + ioe.toString());
            }

            // let's assume we've read the file with servicedoc. Let's look
            // what's inside.
            Element docEle = null;
            try {
                docEle = dom.getDocumentElement();
            } catch (Exception e) {
                Log.d(TAG, "Unknown exception: " + e.toString());
                e.printStackTrace();
                AlertDialog alert = new AlertDialog(YaPhotoActivity.this) {

                };
            }
            // get a nodelist of elements
            NodeList collection = null;
            if (docEle != null) {
                collection = docEle.getElementsByTagName("app:collection");
            }
            if (collection != null && collection.getLength() > 0) {
                Log.d(TAG, "Found collections: " + collection.getLength());
                for (int i = 0; i < collection.getLength(); i++) {
                    Element collectionEl = (Element) collection.item(i);
                    Log.d(TAG,
                            "Trying Attribute: "
                                    + collectionEl.getAttribute("href"));

                    if (collectionEl.getAttribute("id").equals("album-list")) {
                        albumListLink = collectionEl.getAttribute("href");
                        NodeList collectionTitles = collectionEl
                                .getElementsByTagName("atom:title");
                        Log.d(TAG, "Found atom:title nodes: "
                                + collectionTitles.getLength());
                        Log.d(TAG, "Retrieving atom:title node: "
                                + collectionTitles.item(0).getTextContent());
                        retrieveAlbumList(yaSPDialog, albumListLink,
                                albumListTitle);
                    }

                }
            }
        }

        @Override
        protected void onProgressUpdate(String... processName) {
            // TODO Auto-generated method stub
            yaSPDialog.onProcessChanged(processName[0]);
        }

        @Override
        protected void onCancelled() {
            // TODO Auto-generated method stub
            super.onCancelled();
        }

    }

    public void retrieveAlbumList(YaServiceProgressDialog dialog,
                                  String listLink, String listTitle) {
        dialog.onProcessChanged("Getting album list...");
        // now that we have the link to album list, let's retrieve it.
        new GetAlbumListTask().execute();
    }

    public class GetAlbumListTask extends AsyncTask<Void, String, String> {
        // here we retriever service document and album list

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub

            // first let's get user data from /api/me because the app is unaware
            // of username during OAuth authorization

            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(albumListLink + "published/");
            try {
                // Add your data
                // httpget.setHeader("Host", yaHost);
                httpget.setHeader("Authorization",
                        "OAuth " + prefs.getString("yaToken", ""));
                Log.d(TAG, "GET " + httpget.getURI());
                Log.d(TAG, "Request line: " + httpget.getRequestLine());
                Header[] getHeaders = httpget.getAllHeaders();
                for (int i = 0; i < getHeaders.length; i++) {
                    Log.d(TAG, "Header " + i + ": " + getHeaders[i].getName()
                            + ": " + getHeaders[i].getValue());
                }

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httpget);
                Log.d(TAG, "/albums/ result is: "
                        + +response.getStatusLine().getStatusCode()
                        + response.getStatusLine().getReasonPhrase());
                Log.d(TAG, "Full /albums/ response is " + response.toString());
                Header[] headers = response.getAllHeaders();
                Log.d(TAG, "Excluded " + headers.length + " headers:");
                for (int i = 0; i < headers.length; i++) {
                    Log.d(TAG, "Header " + i + ": " + headers[i].getName()
                            + ": " + headers[i].getValue());
                }
                if (response.getStatusLine().getStatusCode() == java.net.HttpURLConnection.HTTP_OK) {
                    HttpEntity httpEntity = response.getEntity();
                    Log.d(TAG, "Full entity is: " + httpEntity.toString());
                    InputStream userDataStream = AndroidHttpClient
                            .getUngzippedContent(httpEntity);
                    Log.d(TAG,
                            "Full userdatastream is: "
                                    + userDataStream.toString());
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(userDataStream));
                    StringBuilder responseBuilder = new StringBuilder();
                    char[] buff = new char[1024 * 512];
                    int read;
                    while ((read = bufferedReader.read(buff)) != -1) {
                        responseBuilder.append(buff, 0, read);
                        Log.d(TAG, "Downloaded " + responseBuilder.length());
                    }
                    String result = responseBuilder.toString();
                    Log.d(TAG, "Response from /api/me is: " + result);

                    return result;
                } else
                    return null;
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "ClientProtocolException raised: " + e);
                showDialog(NETWORK_PROBLEM);
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "IOException raised: " + e);
                showDialog(NETWORK_PROBLEM);
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub

        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            if (result == null) {
                Toast.makeText(YaPhotoActivity.this, "CHECK YOUR INTERNET CONNECTION", Toast.LENGTH_LONG).show();
                return;
            }

            File albumlistfile = new File(getFilesDir(), ALBUMLISTFILENAME);
            Log.d(TAG, "File created: " + albumlistfile.toString());
            try {


                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                        openFileOutput(ALBUMLISTFILENAME, MODE_PRIVATE)));

                if (result != null) {
                    bw.write(result);
                }

                bw.close();

                Log.d(TAG, "Album list file written: " + albumlistfile.getAbsolutePath()
                        + " ");
                InputStream in = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(
                            albumlistfile));
                    Log.d(TAG, "File reading: " + in.toString());
                } finally {
                    if (in != null)
                        in.close();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.d(TAG, "FileNotFound Exception: " + e.toString());
                showDialog(DIALOG_PROBLEM);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "FileNotFound Exception: " + e.toString());
                showDialog(DIALOG_PROBLEM);
            }

            // yaSPDialog.dismiss();

            // here and therefore we try to parse XMl;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document dom = null;
            try {

                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();

                // parse using builder to get DOM representation of the XML file
                dom = db.parse(albumlistfile);
                Log.d(TAG, dom.toString());

            } catch (ParserConfigurationException pce) {
                pce.printStackTrace();
                showDialog(SERVER_RESPONSE_PROBLEM);
                return;
            } catch (SAXException se) {
                se.printStackTrace();
                showDialog(SERVER_RESPONSE_PROBLEM);
                return;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                showDialog(SERVER_RESPONSE_PROBLEM);
                return;
            }

            // let's assume we've read the file with servicedoc. Let's look
            // what's inside.

            try {
                Element docEle = dom.getDocumentElement();

                // get a nodelist of elements
                NodeList collection = docEle.getElementsByTagName("entry");
                albumArray = new ArrayList<String[]>();

                if (collection != null && collection.getLength() > 0) {
                    Log.d(TAG, "Found album entries: " + collection.getLength());
                    for (int i = 0; i < collection.getLength(); i++) {
                        Element collectionEl = (Element) collection.item(i);
                        NodeList links = collectionEl
                                .getElementsByTagName("link");
                        Log.d(TAG, "Found link entries in album " + i + ": "
                                + links.getLength());
                        for (int j = 0; j < links.getLength(); j++) {
                            Element link = (Element) links.item(j);
                            if (link.getAttribute("rel").equals("photos")) {
                                String albumName = collectionEl
                                        .getElementsByTagName("title").item(0)
                                        .getTextContent();
                                String albumURN = collectionEl
                                        .getElementsByTagName("id").item(0)
                                        .getTextContent();
                                NodeList imageCount = collectionEl
                                        .getElementsByTagName("f:image-count");
                                String albumImageCount = ((Element) imageCount
                                        .item(0)).getAttribute("value");
                                String albumLink = link.getAttribute("href");
                                Log.d(TAG, "putting album " + albumName
                                        + "to ArrayLists");
                                albumArray.add(new String[]{albumName,
                                        albumLink, albumImageCount, albumURN});
                                populateAlbumList(albumArray);
                            }

                        }

                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.toString());
                e.printStackTrace();
                showDialog(DIALOG_PROBLEM);
            }
        }

        @Override
        protected void onProgressUpdate(String... processName) {
            // TODO Auto-generated method stub

        }

        @Override
        protected void onCancelled() {
            // TODO Auto-generated method stub
            super.onCancelled();
        }

    }

    public void populateAlbumList(ArrayList<String[]> albumList) {
        yaSPDialog.dismiss();
        ListView lvAlbumList = (ListView) findViewById(R.id.lvAlbumList);
        ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>(
                albumList.size());
        Map<String, Object> m;
        for (String[] x : albumList) {
            m = new HashMap<String, Object>();
            m.put("albumName", x[0]);
            m.put("imageCount", "photos: " + x[2]);
            data.add(m);
        }
        String[] from = {"albumName", "imageCount"};

        int[] to = {R.id.tvAlbumName, R.id.tvImgCount};

        SimpleAdapter sAdapter = new SimpleAdapter(this, data,
                R.layout.mymessageslistelement, from, to);

        lvAlbumList.setAdapter(sAdapter);
        lvAlbumList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String[] albname = albumArray.get(position);
                String link = albname[1];
                Log.d(TAG, "itemClick: position = " + position + ", id = "
                        + albname[0]);
                startImageUploading(link);
            }
        });

    }

    int imageTotal;

    public void startImageUploading(String _link) {
        File[] image = getIntentExtra();
        if (image != null) {
            imageTotal = image.length;
            String[] imagePaths = new String[imageTotal + 1];
            imagePaths[0] = _link;
            for (int i = 0; i < imageTotal; i++) {
                Log.d(TAG, "File to upload is acquired" + image.toString());
                imagePaths[i + 1] = image[i].getAbsolutePath();

            }
            PostImageUploadTask uploadTask = new PostImageUploadTask(imageTotal);
            uploadTask.execute(imagePaths);
        }

    }

    public File[] getIntentExtra() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String type_required = "image";
        Log.d(TAG, "we come here to pick the image");
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith(type_required)) {
                Uri imageUri = (Uri) intent
                        .getParcelableExtra(Intent.EXTRA_STREAM);
                Log.d(TAG, "Image URI acquired: " + imageUri.toString());
                if(imageUri.toString().startsWith("file")) {

                    String tempURI = imageUri.toString().substring(7);
                    imageUri = Uri.parse(tempURI);
                    File[] image = new File[1];
                    image[0] = new File(tempURI);
                    Log.d(TAG, "File to upload: " + image[0].getAbsolutePath());
                    return image;
                }else if (imageUri != null) {
                    // Update UI to reflect image being shared
                    Cursor cursor = getContentResolver()
                            .query(imageUri,
                                    new String[]{android.provider.MediaStore.Images.ImageColumns.DATA},
                                    null, null, null);
                    Log.d(TAG, "Cursor retrieved, " + cursor.getColumnCount()
                            + " columns, " + cursor.toString());
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        String columnName = cursor.getColumnName(i);
                        Log.d(TAG, "Cursor column #" + i + ": " + columnName
                                + cursor.getString(i));
                    }
                    String imageFilePath = cursor.getString(0);
                    Log.d(TAG, "retrieving imageFilePath from URI, got: "
                            + imageFilePath);
                    File[] image = new File[1];
                    image[0] = new File(imageFilePath.toString());
                    Log.d(TAG, "File to upload: " + image[0].getAbsolutePath());
                    return image;
                }
            }
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith(type_required)) {
                Log.d(TAG, "ACTION_SEND_MULTIPLE working");
                ArrayList<Uri> imageUri = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                Log.d(TAG, "Found an extra stream array of elements: " + imageUri.size());
                File[] image = new File[imageUri.size()];

                imageUrls = new String[imageUri.size()];
                if (imageUri != null) {
                    Log.d(TAG, "Image URI acquired: " + imageUri.toString());
                    // Update UI to reflect image being shared
                    if (((Uri) imageUri.get(0)).getScheme().equals("content")) {
                        for (int j = 0; j < imageUri.size(); j++) {
                            Cursor cursor = getContentResolver()
                                    .query(imageUri.get(j),
                                            new String[]{android.provider.MediaStore.Images.ImageColumns.DATA},
                                            null, null, null);
                            Log.d(TAG, "Cursor retrieved, " + cursor.getColumnCount()
                                    + " columns, " + cursor.toString());
                            cursor.moveToFirst();
                            for (int i = 0; i < cursor.getColumnCount(); i++) {
                                String columnName = cursor.getColumnName(i);
                                Log.d(TAG, "Cursor column #" + i + ": " + columnName
                                        + cursor.getString(i));
                            }
                            String imageFilePath = cursor.getString(0);
                            Log.d(TAG, "retrieving imageFilePath from URI, got: "
                                    + imageFilePath);

                            image[j] = new File(imageFilePath.toString());
                            Log.d(TAG, "File to upload: " + image[j].getAbsolutePath());
                        }
                    } else if (((Uri) imageUri.get(0)).getScheme().equals("file")) {
                        for (int j = 0; j < imageUri.size(); j++) {
                            image[j] = new File(((Uri) imageUri.get(j)).toString());
                        }
                    } else {
                        Toast.makeText(YaPhotoActivity.this, "Wrong URI passed", Toast.LENGTH_LONG).show();
                        return null;
                    }

                    return image;
                }
            }
        }
        return null;
    }

    public class PostImageUploadTask extends AsyncTask<String, String, String[]> {
        // here we retriever service document and album list
        int imgCurrent, imgTotal;
        ProgressDialog progressD;

        public PostImageUploadTask(int all) {
            super();

            imgTotal = all;
            // do stuff
        }


        @SuppressWarnings("deprecation")
        @Override
        protected String[] doInBackground(String... params) {
            // TODO Auto-generated method stub
            String link = params[0];
            String[] result = new String[params.length - 1];
            for (int i = 0; i < params.length - 1; i++) {
                publishProgress(String.valueOf(i + 1));
                imgCurrent = i + 1;
                File image = new File(params[i + 1]);
                HttpClient httpClient = new DefaultHttpClient();
                httpClient.getParams().setParameter(
                        CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                String albumLink = link;
                HttpPost httpPost = new HttpPost(albumLink);
                httpPost.setHeader("Authorization",
                        "OAuth " + prefs.getString("yaToken", ""));
                Log.d(TAG, "Creating HTTP POST for link: " + albumLink);
                Log.d(TAG, "Testing file existance: " + image.exists() + ", "
                        + image.getAbsolutePath());
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                builder.addTextBody("access", "public");
                Bitmap bitmap = BitmapFactory.decodeFile(params[i + 1]);
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                int bytes = byteSizeOf(bitmap);
                Log.d(TAG, "Got buffer of " + bytes + " bytes.");
//or we can calculate bytes this way. Use a different value than 4 if you don't use 32bit images.
//int bytes = b.getWidth()*b.getHeight()*4;

                ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
                bitmap.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
                byte[] byte_arr = new byte[0];
                try {
                    byte_arr = org.apache.commons.io.FileUtils.readFileToByteArray(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                byte[] byte_arr = buffer.array(); //Get the underlying array containing the data.

//                byte[] byte_arr = stream.toByteArray();
                ByteArrayBody fileBody = new ByteArrayBody(byte_arr,
                        image.getName());
                builder.addPart("image", fileBody);

                httpPost.setEntity(builder.build());
                Log.d(TAG, "Processing the request: " + httpPost.getRequestLine());

                try {
                    HttpResponse response;
                    response = httpClient.execute(httpPost);
                    HttpEntity resEntity = response.getEntity();
                    Log.d(TAG, "Response: " + response.getStatusLine());

                    InputStream userDataStream = AndroidHttpClient
                            .getUngzippedContent(resEntity);
                    Log.d(TAG,
                            "Full userdatastream is: " + userDataStream.toString());
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(userDataStream));
                    StringBuilder responseBuilder = new StringBuilder();
                    char[] buff = new char[1024 * 512];
                    int read;
                    while ((read = bufferedReader.read(buff)) != -1) {
                        responseBuilder.append(buff, 0, read);
                        Log.d(TAG, "Downloaded " + responseBuilder.length());
                    }
                    result[i] = responseBuilder.toString();
                    Log.d(TAG, "POST response is: " + result[i]);

                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Log.d(TAG, "ClientProtocolException: " + e.toString());


                    return null;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Log.d(TAG, "IOException: " + e.toString());
                    e.printStackTrace();

                    return null;
                }
            }
            return result;

        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            progressD = new ProgressDialog(YaPhotoActivity.this);
            progressD.setIndeterminate(false);
            progressD.setProgress(0);
            progressD.setMax(100);
            progressD.setCancelable(true);
            progressD.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancel(true);
                }
            });
            progressD.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressD.setMessage("Uploading...");
            progressD.show();


        }

        @Override
        protected void onPostExecute(String[] result) {
            // TODO Auto-generated method stub
            progressD.dismiss();
            if (result != null) {
                imageUrls = new String[result.length];
                for (int j = 0; j < result.length; j++) {

                    File postResult = new File(getFilesDir(), UPLOADEDIMGINFO);
                    Log.d(TAG, "File created: " + postResult.toString());
                    try {


                        BufferedWriter bw = new BufferedWriter(
                                new OutputStreamWriter(openFileOutput(
                                        UPLOADEDIMGINFO, MODE_PRIVATE)));

                        if (result != null) {
                            bw.write(result[j]);
                        }

                        bw.close();

                        Log.d(TAG,
                                "postResult file written: "
                                        + postResult.getAbsolutePath() + " ");
                        InputStream in = null;
                        try {
                            in = new BufferedInputStream(new FileInputStream(
                                    postResult));
                            Log.d(TAG, "File reading: " + in.toString());
                        } finally {
                            if (in != null)
                                in.close();
                        }

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // yaSPDialog.dismiss();

                    // here and therefore we try to parse XMl;
                    DocumentBuilderFactory dbf = DocumentBuilderFactory
                            .newInstance();
                    Document dom = null;
                    try {

                        // Using factory get an instance of document builder
                        DocumentBuilder db = dbf.newDocumentBuilder();

                        // parse using builder to get DOM representation of the XML
                        // file
                        dom = db.parse(postResult);
                        Log.d(TAG, dom.toString());

                    } catch (ParserConfigurationException pce) {
                        pce.printStackTrace();
                    } catch (SAXException se) {
                        se.printStackTrace();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }

                    // let's assume we've read the file with servicedoc. Let's look
                    // what's inside.

                    Element docEle = null;
                    if (dom != null) {
                        docEle = dom.getDocumentElement();


                        // get a nodelist of elements
                        NodeList collection = docEle.getElementsByTagName("f:img");
                        if (collection != null && collection.getLength() > 0) {

                            Log.d(TAG, "Found content links: " + collection.getLength());
                            for (int i = 0; i < collection.getLength(); i++) {
                                Element collectionEl = (Element) collection.item(i);
                                switch (resolution) {
                                    case 0:
                                        Log.d(TAG,
                                                "Trying Attribute: "
                                                        + collectionEl.getAttribute("size"));
                                        if (collectionEl.getAttribute("size")
                                                .equals("orig")) {
                                            String uploadedImageLink = collectionEl
                                                    .getAttribute("href");
                                            imageUrls[j] = uploadedImageLink + ".jpg";
                                        }
                                        break;
                                    case 1:
                                        Log.d(TAG,
                                                "Trying Attribute: "
                                                        + collectionEl.getAttribute("size"));
                                        if (collectionEl.getAttribute("size").equals("S")) {
                                            String uploadedImageLink = collectionEl
                                                    .getAttribute("href");
                                            imageUrls[j] = uploadedImageLink + ".jpg";
                                        }
                                        break;
                                    case 2:

                                        Log.d(TAG,
                                                "Trying Attribute: "
                                                        + collectionEl.getAttribute("size"));
                                        if (collectionEl.getAttribute("size").equals("L")) {
                                            String uploadedImageLink = collectionEl
                                                    .getAttribute("href");
                                            imageUrls[j] = uploadedImageLink + ".jpg";
                                        }
                                        break;
                                    case 3:

                                        Log.d(TAG,
                                                "Trying Attribute: "
                                                        + collectionEl.getAttribute("size"));
                                        if (collectionEl.getAttribute("size").equals("XL")) {
                                            String uploadedImageLink = collectionEl
                                                    .getAttribute("href");
                                            imageUrls[j] = uploadedImageLink + ".jpg";
                                        }
                                        break;
                                    case 4:

                                        Log.d(TAG,
                                                "Trying Attribute: "
                                                        + collectionEl.getAttribute("size"));
                                        if (collectionEl.getAttribute("size").equals("XXL")) {
                                            String uploadedImageLink = collectionEl
                                                    .getAttribute("href");
                                            imageUrls[j] = uploadedImageLink + ".jpg";
                                        }
                                        break;

                                }
                            }


                        }
                    }
                }
                goToPointActivity(imageUrls);
                return;
            } else {
                showDialog(NETWORK_PROBLEM);
                return;
            }

        }

        @Override
        protected void onProgressUpdate(String... processName) {
            // TODO Auto-generated method stub
            progressD.setProgress((Integer.valueOf(processName[0]) - 1) * 100 / imageTotal);
            progressD.setMessage("Uploading " + processName[0] + " of " + imageTotal);
            progressD.show();
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "Cancelled.");
            Toast.makeText(activ, "Загрузка отменена.", Toast.LENGTH_LONG).show();
            progressD.dismiss();
            // TODO Auto-generated method stub
            super.onCancelled();
        }

    }

    public void goToPointActivity(String[] links) {
        Intent intent = new Intent(this, MainActivity.class);
        String toSend = "";
        for (String link : links) {
            toSend = toSend.concat("\n\n" + link);
            Log.d(TAG, "link to insert: " + link);
        }
        intent.putExtra("link", toSend);
        startActivity(intent);
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();

    }
    protected int byteSizeOf(Bitmap data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return data.getRowBytes() * data.getHeight();
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return data.getByteCount();
        }
        else {
            return data.getAllocationByteCount();
        }
    }
}
