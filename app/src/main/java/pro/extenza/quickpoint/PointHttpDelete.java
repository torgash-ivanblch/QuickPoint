package pro.extenza.quickpoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class PointHttpDelete {
	SharedPreferences prefs;
	String URLHost = "http://point.im/api/";
	String login;
	String password;
	String URLapi;
	String result="";
	String postText;
	String postTags;
	String _private;
	String csrf_token;
	String token;
    String mesID;
	final String TAG = "QUICKPOINT";
	public PointHttpDelete(Context contex, String _URL, String _login, String _password) {
		// TODO Auto-generated constructor stub
		//Авторизация. Отправляем логин и пароль, получаем два токена, сохраняем в SharedPref.
		prefs = contex.getSharedPreferences("prefs", 0);
		login = _login;
		password = _password;
		URLapi = URLHost + _URL;

	}
	public PointHttpDelete(String URL) {
		// TODO Auto-generated constructor stub
		//токен брать из SharedPref
	}
	public PointHttpDelete(Context contex, String ID) {
		// TODO Auto-generated constructor stub
		//удаление поста. Токен посылать формата X-CSRF: <csrf_token>
		prefs = contex.getSharedPreferences("prefs", 0);
		token = prefs.getString("token", "");
		csrf_token = prefs.getString("csrf_token", "");
		mesID = ID;
	}
	int postResult;
	public int deletePost(){
	 	HttpClient httpclient = new DefaultHttpClient();
	 	
	    
	    HttpDelete httpDelete = new HttpDelete("http://point.im/api/post/"+mesID);
	    
	    try {
	        // Add your data

	        httpDelete.setHeader("Authorization", token);
	        httpDelete.setHeader("X-CSRF", csrf_token);
	        Log.d("DOWNLOAD", httpDelete.getFirstHeader("X-CSRF").toString());
	        Log.d("DOWNLOAD", httpDelete.getParams().toString());
	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httpDelete);
	        HttpEntity httpEntity=response.getEntity();
	        InputStream stream = AndroidHttpClient.getUngzippedContent(httpEntity);
		    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder responseBuilder= new StringBuilder();
            char[] buff = new char[1024*512];
            int read;
            while((read = bufferedReader.read(buff)) != -1) {
                responseBuilder.append(buff, 0, read) ;
                Log.d("DOWNLOAD", "Downloaded " + responseBuilder.length());
                Log.d("DOWNLOAD", responseBuilder.toString());
            }
            result = responseBuilder.toString();
           
            try {
                JSONObject obj = new JSONObject(new JSONTokener(result));
                	if(obj.has("error")) return 1;
                      String ok = obj.getString("id");
                      
                      Log.d("JSON","id = " + ok);
                    MainActivity.message_id = ok;
                      if(ok != null){
                    	  return 0;
                      }
                 
                
              } catch (JSONException jsonEx) {
                Log.e("JSON","Error parsing data "+jsonEx.toString());
                return 1;
                //Toast.makeText(getApplicationContext(), "fail", Toast.LENGTH_SHORT).show();
              }
            
            
	    
	    
	    } catch (ClientProtocolException e) {
	    	return 2;
	        // TODO Auto-generated catch block
	    } catch (IOException e) {
	    	return 3;
	        // TODO Auto-generated catch block
	    }
	    return 0;
		
		
		
	}

	public static SharedPreferences getSharedPreferences (Context ctxt) {
		   return ctxt.getSharedPreferences("FILE", 0);
		}
}
