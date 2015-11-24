package pro.extenza.quickpoint;

/**
 * Created by torgash on 6/26/15.
 */
public class MyPost {
    private String postText;
    private String postTags;
    private boolean postPrivate = false;
    public MyPost(String text, String tags){
        postText = text;
        postTags = tags;
    }
    public MyPost(String text, String tags, boolean _privat){
        postText = text;
        postTags = tags;
        postPrivate = _privat;
    }
    public void setText(String text) {
        postText = text;
    }

    public void setTags(String tags) {
        postTags = tags;
    }
    public void setPrivate(boolean priv) { postPrivate = priv; }
    public String getText() {
        return postText;
    }
    public String getTags() {
        return postTags;
    }
    public boolean getPrivate() {
        return postPrivate;
    }
}
