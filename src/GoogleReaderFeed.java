import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


public class GoogleReaderFeed {
	/*************************************
	 * According to google reader api, get atoms from google reader
	 * 
	 * @author guanxin
	 */
	private String label = "DH";
	
	public GoogleReaderFeed(){
		
		
	}
	
	/**************************************************************
	 * getAuth() return the auth string for SID LSID adn AUTH
	 * 
	 * @return [SID, LSID, AUTH]
	 * @throws IOException
	 * @throws URISyntaxException
	 */	
	public String[] getAuth() throws IOException, URISyntaxException{
		
		HttpClient httpclient = new DefaultHttpClient();
		URIBuilder builder = new URIBuilder();
		builder.setScheme("https").setHost("www.google.com").setPath("/accounts/ClientLogin")
				.setParameter("accountType", "HOSTED_OR_GOOGLE")
				.setParameter("Email", "reader@pressforward.org")
				.setParameter("Passwd","CHNMadmin1994")
				.setParameter("service", "reader")
				.setParameter("source", "DHNow");
		
		URI uri = builder.build();
		HttpPost httppost = new HttpPost(uri);
		//System.out.println(httppost.getURI());
		
		HttpResponse response = httpclient.execute(httppost);
		String result = EntityUtils.toString(response.getEntity());
		//System.out.println(result);
		String[] paraTemp = result.split("\n");
		String[] para = new String[3];
		for (int i = 0; i<3;i++){
			para[i] = paraTemp[i].split("=")[1];
		}
		return para;

	}
	
	/*******************************************************************
	 * get token, a session identification, but that expire rather quickly 
	 * You usaully need a token for direct api calls that change informations. 
	 * @param auth
	 * @return
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getToken(String[] auth) throws URISyntaxException, ClientProtocolException, IOException{
		
		String SID = auth[0];
		String LSID = auth[1];
		String AUTH = auth[2];
		
		
		HttpClient httpclient = new DefaultHttpClient();
		URIBuilder builder = new URIBuilder();
		builder.setScheme("https").setHost("www.google.com").setPath("/reader/api/0/token")
				.setParameter("client", "scroll")
				.setParameter("ck", String.valueOf((new Date()).getTime()));
		
		URI uri = builder.build();
		HttpGet httpget = new HttpGet(uri);
		httpget.setHeader("Authorization", "GoogleLogin auth=" + AUTH);
		httpget.setHeader("Cookie", "SID=" + SID);
		System.out.println(httpget.getURI());
		HttpResponse response = httpclient.execute(httpget);
		String result = EntityUtils.toString(response.getEntity());
		System.out.println(result);
		return result;
	}
	
	/****************************************************************
	 * getAtom() download the num of the feeds from google reader, in descending time order. 
	 * return an xml file that contains all the contents. 
	 * @param auth
	 * @param token
	 * @param num
	 * @return
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getAtoms(String[] auth, String token,int num) throws URISyntaxException, ClientProtocolException, IOException{
		String SID = auth[0];
		String LSID = auth[1];
		String AUTH = auth[2];
		
		HttpClient httpclient = new DefaultHttpClient();
		URIBuilder builder = new URIBuilder();
		
		builder.setScheme("https").setHost("www.google.com").setPath("/reader/atom/user/-/label/"+label)
				.addParameter("n", String.valueOf(num));
		
		URI uri = builder.build();
		HttpGet httpget = new HttpGet(uri);
		httpget.setHeader("Authorization", "GoogleLogin auth=" + AUTH);
		httpget.setHeader("Cookie", "SID=" + SID);
		System.out.println(httpget.getURI());
		HttpResponse response = httpclient.execute(httpget);
		String result = EntityUtils.toString(response.getEntity());
		System.out.println(result.substring(0, 1000));
		System.out.println(result.substring(result.length()-1000));
		long unixTime = System.currentTimeMillis() / 1000L;
		BufferedWriter out = null; 
		String outputPath = "timestamp";
		FileOutputStream output=new FileOutputStream(outputPath,false);
    	out = new BufferedWriter(new OutputStreamWriter(  
                output));  
    	out.append(String.valueOf(unixTime));
    	out.close();
		return result;
	}
	
	public String getNewAtoms(String[] auth, String token,int num) throws URISyntaxException, ClientProtocolException, IOException{
		String SID = auth[0];
		String LSID = auth[1];
		String AUTH = auth[2];
		
		HttpClient httpclient = new DefaultHttpClient();
		URIBuilder builder = new URIBuilder();
		BufferedReader in = new BufferedReader(new FileReader("timestamp"));
		long lasttimestamp = Long.parseLong(in.readLine());
		builder.setScheme("https").setHost("www.google.com").setPath("/reader/atom/user/-/label/"+label)
				.addParameter("n", String.valueOf(num))
				.addParameter("r","o")
				.addParameter("ot",String.valueOf(lasttimestamp));
		
		
		URI uri = builder.build();
		HttpGet httpget = new HttpGet(uri);
		httpget.setHeader("Authorization", "GoogleLogin auth=" + AUTH);
		httpget.setHeader("Cookie", "SID=" + SID);
		System.out.println(httpget.getURI());
		HttpResponse response = httpclient.execute(httpget);
		String result = EntityUtils.toString(response.getEntity());
		BufferedWriter out = null; 
		long unixTime = System.currentTimeMillis() / 1000L;
		String outputPath = "timestamp";
		FileOutputStream output=new FileOutputStream(outputPath,false);
    	out = new BufferedWriter(new OutputStreamWriter(  
                output));  
    	out.append(String.valueOf(unixTime));
    	out.close();
		return result;
	}
	
	/****************************************************************
	 * getUnreadAtom() download the num of the unread feeds from google reader, in descending time order. 
	 * return an xml file that contains all the contents. 
	 * @param auth
	 * @param token
	 * @param num
	 * @return
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getUnreadAtoms(String[] auth, String token, int num) throws URISyntaxException, ClientProtocolException, IOException{
		String SID = auth[0];
		String LSID = auth[1];
		String AUTH = auth[2];
		
		
		HttpClient httpclient = new DefaultHttpClient();
		URIBuilder builder = new URIBuilder();
		builder.setScheme("https").setHost("www.google.com").setPath("/reader/atom/user/-/label/"+label)
				.addParameter("n", String.valueOf(num))
				.addParameter("xt","user/-/state/com.google/read");
		
		URI uri = builder.build();
		HttpGet httpget = new HttpGet(uri);
		httpget.setHeader("Authorization", "GoogleLogin auth=" + AUTH);
		httpget.setHeader("Cookie", "SID=" + SID);
		System.out.println(httpget.getURI());
		HttpResponse response = httpclient.execute(httpget);
		String result = EntityUtils.toString(response.getEntity());
		System.out.println(result);
		//System.out.println(result.substring(0, 1000));
		//System.out.println(result.substring(result.length()-1000));
		return result;
	}
	
	public void write2file(String content, String outputPath) throws IOException{
		
		BufferedWriter out = null; 
		if (outputPath ==""){
			outputPath = "test";
		}
		FileOutputStream output=new FileOutputStream(outputPath,false);
    	out = new BufferedWriter(new OutputStreamWriter(  
                output));  
    	out.append(content);
    	out.close();
	}
	
	
	public static void main(String[] args){
		GoogleReaderFeed feeder = new GoogleReaderFeed();
		try{
			String[] auth = feeder.getAuth();
			
			System.out.println("SID = " + auth[0] + "\nLSID = " + auth[1] + "\nAuth = "+auth[2]);
			String token = feeder.getToken(auth);
			int num = 50;
			String content = feeder.getAtoms(auth, token, num);
			//String content = feeder.getUnreadAtoms(auth, token,num);
			BufferedWriter out = null; 
			String outputPath = "test";
			FileOutputStream output=new FileOutputStream(outputPath,true);
        	out = new BufferedWriter(new OutputStreamWriter(  
                    output));  
        	out.append(content);
        	out.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
