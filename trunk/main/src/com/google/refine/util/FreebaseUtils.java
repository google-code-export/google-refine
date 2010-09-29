package com.google.refine.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.ProjectManager;
import com.google.refine.RefineServlet;
import com.google.refine.oauth.Credentials;
import com.google.refine.oauth.OAuthUtilities;
import com.google.refine.oauth.Provider;

public class FreebaseUtils {

    static final public String FREEBASE_HOST = "www.freebase.com";
    
    static final private String FREEQ_URL = "http://data.labs.freebase.com/freeq/gridworks";
    //static final private String FREEQ_URL = "http://data.labs.freebase.com/freeq/refine";
    
    static final private String AGENT_ID = "/en/google_refine";
    
    private static String getUserInfoURL(String host) {
        return "http://" + host + "/api/service/user_info";
    }

    private static String getMQLWriteURL(String host) {
        return "http://" + host + "/api/service/mqlwrite";
    }

    private static String getMQLReadURL(String host) {
        return "http://" + host + "/api/service/mqlread";
    }
    
    private static String getUserAgent() {
        return RefineServlet.FULLNAME;        
    }
    
    public static String getUserInfo(Credentials credentials, Provider provider) 
        throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, IOException {
        
        OAuthConsumer consumer = OAuthUtilities.getConsumer(credentials, provider);
        
        HttpGet httpRequest = new HttpGet(getUserInfoURL(provider.getHost()));
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
        
        // this is required by the Metaweb API to avoid XSS
        httpRequest.setHeader("X-Requested-With", "1");
        
        // sign the request with the oauth library
        consumer.sign(httpRequest);

        // execute the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        
        // return the results
        return EntityUtils.toString(httpResponse.getEntity());
    }

    public static String getUserBadges(Provider provider, String user_id)
    throws ClientProtocolException, IOException, JSONException {

        String query = "{" +
          "'id' : '" + user_id + "'," +
          "'!/type/usergroup/member' : [{" +
            "'id' : null," +
            "'key' : [{" +
              "'namespace' : null" +
            "}]" +
          "}]" +
        "}".replace("'", "\"");

        return mqlread(provider, query);
    }

    public static String mqlread(Provider provider, String query) 
    throws ClientProtocolException, IOException, JSONException {
        
        JSONObject envelope = new JSONObject();
        envelope.put("query", new JSONObject(query));
        
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("query", envelope.toString()));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

        HttpPost httpRequest = new HttpPost(getMQLReadURL(provider.getHost()));
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
        httpRequest.setEntity(entity);

        // this is required by the Metaweb API to avoid XSS
        httpRequest.setHeader("X-Requested-With", "1");

        // execute the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
    
        // return the results
        return EntityUtils.toString(httpResponse.getEntity());
    }
    
    public static String mqlwrite(Credentials credentials, Provider provider, String query) 
    throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, IOException, JSONException {
        OAuthConsumer consumer = OAuthUtilities.getConsumer(credentials, provider);

        JSONObject envelope = new JSONObject();
        envelope.put("query", new JSONObject(query));
        
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("query", envelope.toString()));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

        HttpPost httpRequest = new HttpPost(getMQLWriteURL(provider.getHost()));
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
        httpRequest.setEntity(entity);

        // this is required by the Metaweb API to avoid XSS
        httpRequest.setHeader("X-Requested-With", "1");

        // sign the request with the oauth library
        consumer.sign(httpRequest);

        // execute the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
    
        // return the results
        return EntityUtils.toString(httpResponse.getEntity());
    }

    public static String uploadTriples(
        HttpServletRequest request,
        String qa,
        String source_name,
        String source_id,
        String mdo_id,
        String triples
    ) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, JSONException, IOException {
        
        Provider provider = OAuthUtilities.getProvider(FREEBASE_HOST);
        
        Credentials credentials = Credentials.getCredentials(request, provider, Credentials.Type.ACCESS);
        
        JSONObject mdo_info = new JSONObject();
        mdo_info.put("name", source_name);
        if (source_id != null) {
            mdo_info.put("info_source",source_id);
        }
        
        JSONObject user_info = new JSONObject(getUserInfo(credentials, provider));
        if (user_info.has("username")) {

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("user", user_info.getString("id")));
            formparams.add(new BasicNameValuePair("action_type", "LOAD_TRIPLE"));
            formparams.add(new BasicNameValuePair("operator", user_info.getString("id")));
            formparams.add(new BasicNameValuePair("software_tool_used", AGENT_ID));
            formparams.add(new BasicNameValuePair("mdo_info", mdo_info.toString()));
            formparams.add(new BasicNameValuePair("graphport", "sandbox"));
            formparams.add(new BasicNameValuePair("payload", triples));
            formparams.add(new BasicNameValuePair("check_params", "false"));
            if (mdo_id != null) {
                formparams.add(new BasicNameValuePair("mdo_guid", mdo_id));
            }
            if (Boolean.parseBoolean(qa)) {
                formparams.add(new BasicNameValuePair("rabj", "true"));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

            HttpPost httpRequest = new HttpPost(getFreeQUrl());
            httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent());
            httpRequest.setEntity(entity);
            
            HttpPost surrogateRequest = new HttpPost(getUserInfoURL(FREEBASE_HOST));
            surrogateRequest.setEntity(entity);
            
            OAuthConsumer consumer = OAuthUtilities.getConsumer(credentials, provider);

            consumer.sign(surrogateRequest);

            Header[] h = surrogateRequest.getHeaders("Authorization");
            if (h.length > 0) {
                httpRequest.setHeader("X-Freebase-Credentials", h[0].getValue());
            } else {
                throw new RuntimeException("Couldn't find the oauth signature header in the surrogate request");
            }
            
            // execute the request
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            
            // return the results
            return EntityUtils.toString(httpResponse.getEntity());
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
        
    static public String getFreeQUrl() {
    	String url = (String) ProjectManager.singleton.getPreferenceStore().get("freebase.freeq");
    	return url != null ? url : FREEQ_URL;
    }
}
