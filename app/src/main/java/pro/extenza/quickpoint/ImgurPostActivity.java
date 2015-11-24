package pro.extenza.quickpoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class ImgurPostActivity extends Activity {

        final Activity activ = this;
        String TAG = "QUICKPOINT";
        String imgurToken;
        String imgurHost = "https://api.imgur.com/3/";
        SharedPreferences prefs;

        YaServiceProgressDialog yaSPDialog;
        File imageToUpload;
        final String UPLOADEDIMGINFO = "imginfo";
        String uploadedImageLink = "";

        final int DIALOG_PROBLEM = 1;
        final int NETWORK_PROBLEM = 2;
        final int SERVER_RESPONSE_PROBLEM = 3;

        Boolean quickpointAlbum;
        int resolution;
        Context contex;
        String[] imageUrls;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.imgur_post);
            contex = this.getApplicationContext();
            prefs = getApplicationContext().getSharedPreferences("prefs", 0);

            imgurToken = getString(R.string.imgur_client_id);
            // now let's call YaServiceProgressDialog and retrieve service document
            new GetImgurPossibilityTask().execute();

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

        DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {

                    case Dialog.BUTTON_POSITIVE:
                        finish();

                        break;

                }
            }
        };

        protected void onResume() {


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




        public class GetImgurPossibilityTask extends AsyncTask<Void, String, Integer> {
            // here we retriever service document and album list
            ProgressDialog progressD;
            @Override
            protected Integer doInBackground(Void... params) {
                // TODO Auto-generated method stub

                // first let's get user data from /api/me because the app is unaware
                // of username during OAuth authorization

                HttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(imgurHost + "credits");
                try {
                    // Add your data
                    // httpget.setHeader("Host", yaHost);
                    httpget.setHeader("Authorization",
                            "Client-ID " + getString(R.string.imgur_client_id));
                    Log.d(TAG, "GET " + httpget.getURI());
                    Log.d(TAG, "Request line: " + httpget.getRequestLine());
                    Header[] getHeaders = httpget.getAllHeaders();
                    for (int i = 0; i < getHeaders.length; i++) {
                        Log.d(TAG, "Header " + i + ": " + getHeaders[i].getName()
                                + ": " + getHeaders[i].getValue());
                    }

                    // Execute HTTP Post Request
                    HttpResponse response = httpclient.execute(httpget);
                    Log.d(TAG, "/credits result is: "
                            + +response.getStatusLine().getStatusCode()
                            + response.getStatusLine().getReasonPhrase());
                    Log.d(TAG, "Full /credits response is " + response.toString());
                    Header[] headers = response.getAllHeaders();
                    Log.d(TAG, "Excluded " + headers.length + " headers:");
                    for (int i = 0; i < headers.length; i++) {
                        Log.d(TAG, "Header " + i + ": " + headers[i].getName()
                                + ": " + headers[i].getValue());
                    }
                    if (response.getStatusLine().getStatusCode() == java.net.HttpURLConnection.HTTP_OK) {
                        if(response.containsHeader("X-RateLimit-ClientRemaining")) {
                            int result = Integer.valueOf(response.getFirstHeader("X-RateLimit-ClientRemaining").getValue().toString());
                            Log.d(TAG, "Found a rate limit of " + result + " uploads left for today");

                        return result;
                        }else return 150;
                    } else
                        return 150;
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    progressD.dismiss();
                    Log.d(TAG, "ClientProtocolException raised: " + e);

                    e.printStackTrace();
                    return -1;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    progressD.dismiss();
                    Log.d(TAG, "IOException raised: " + e);

                    e.printStackTrace();
                    return -1;
                }

            }

            @Override
            protected void onPreExecute() {
                // TODO Auto-generated method stub
                progressD = new ProgressDialog(ImgurPostActivity.this);
                progressD.setMessage("Checking rate limit...");
                progressD.setTitle("Checking rate limit...");
                progressD.setIndeterminate(true);
                progressD.setCancelable(false);
                progressD.show();
            }

            @Override
            protected void onPostExecute(Integer result) {
                // TODO Auto-generated method stub
                progressD.dismiss();
                if(result == -1){
                    showDialog(NETWORK_PROBLEM);
                    return;
                }
                if (result < 50) {
                    Toast.makeText(ImgurPostActivity.this, "Out of rate limit for today", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                Log.d(TAG, "Here we go to image uploading task");
                startImageUploading(imgurHost + "upload");

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



        int imageTotal;

        public void startImageUploading(String _link) {
            File[] image = getIntentExtra();

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
                    if (imageUri != null) {
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
                HttpResponse[] result = new HttpResponse[params.length - 1];
                String[] responseString = new String[params.length -1];
                for (int i = 0; i < params.length - 1; i++) {
                    publishProgress(String.valueOf(i+1));
                    imgCurrent = i + 1;
                    File image = new File(params[i + 1]);
                    HttpClient httpClient = new DefaultHttpClient();
                    httpClient.getParams().setParameter(
                            CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                    String albumLink = link;
                    HttpPost httpPost = new HttpPost(albumLink);
                    httpPost.setHeader("Authorization",
                            "Client-ID " + getString(R.string.imgur_client_id));
                    Log.d(TAG, "Creating HTTP POST for link: " + albumLink);
                    Log.d(TAG, "Testing file existance: " + image.exists() + ", "
                            + image.getAbsolutePath());
                    if(image.exists()) {
                        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                        Bitmap bitmap = BitmapFactory.decodeFile(params[i + 1]);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                        byte[] byte_arr = stream.toByteArray();
                        ByteArrayBody fileBody = new ByteArrayBody(byte_arr,
                                image.getName());
                        builder.addPart("image", fileBody);

                        httpPost.setEntity(builder.build());
                        Log.d(TAG, "Processing the request: " + httpPost.getRequestLine());
                    }
                    else {

                            return null;
                        }
                    try {

                        result[i] = httpClient.execute(httpPost);


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
                    HttpEntity resEntity = result[i].getEntity();
                    Log.d(TAG, "Response: " + result[i].getStatusLine());

                    InputStream userDataStream;
                    try {
                        userDataStream = AndroidHttpClient
                                .getUngzippedContent(resEntity);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(ImgurPostActivity.this, "Wrong server response", Toast.LENGTH_LONG).show();
                        return null;
                    }
                    Log.d(TAG,
                            "Full userdatastream is: " + userDataStream.toString());
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(userDataStream));
                    StringBuilder responseBuilder = new StringBuilder();
                    char[] buff = new char[1024 * 512];
                    int read;
                    try {
                        while ((read = bufferedReader.read(buff)) != -1) {
                            responseBuilder.append(buff, 0, read);
                            Log.d(TAG, "Downloaded " + responseBuilder.length());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(ImgurPostActivity.this, "Wrong server response", Toast.LENGTH_LONG).show();
                    }
                    responseString[i] = responseBuilder.toString();
                    Log.d(TAG, "POST response is: " + result);

                }
                return responseString;

            }

            @Override
            protected void onPreExecute() {
                // TODO Auto-generated method stub
                progressD = new ProgressDialog(ImgurPostActivity.this);
                progressD.setIndeterminate(false);
                progressD.setProgress(0);
                progressD.setMax(100);
                progressD.setCancelable(false);
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
            protected void onPostExecute(String[] responseString) {
                // TODO Auto-generated method stub
                progressD.dismiss();


                if (responseString != null) {
                    imageUrls = new String[responseString.length];
                    for (int j = 0; j < responseString.length; j++) {

                        try {
                            JSONObject obj = new JSONObject(new JSONTokener(responseString[j]));
                            JSONObject objData = obj.getJSONObject("data");
                            if(objData.has("link")){
                                imageUrls[j] = objData.getString("link");
                            }


                        } catch (JSONException jsonEx) {
                            Log.e("JSON","Error parsing data "+jsonEx.toString());
                            return;
                            //Toast.makeText(getApplicationContext(), "fail", Toast.LENGTH_SHORT).show();
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
                progressD.setProgress((Integer.valueOf(processName[0])-1)*100/imageTotal);
                progressD.setMessage("Uploading " + processName[0] + " of " + imageTotal);
                progressD.show();
            }

            @Override
            protected void onCancelled() {
                Log.d(TAG, "Cancelled.");
                Toast.makeText(activ, "Загрузка отменена.", Toast.LENGTH_LONG).show();
                progressD.dismiss();
                // TODO Auto-generated method stub

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

        protected void onStop() {
            ProgressDialog progressD = new ProgressDialog(ImgurPostActivity.this);
            progressD.show();
            progressD.dismiss();
            super.onStop();

        }
    }
