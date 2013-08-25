import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/****************************************************
 * Read xml file, and create the input file for mallet.
 * 
 * INPUT:  XML file, the content is between <content> </content>
 * OUTPUT: The output will be a sigle file with one instance one line.
 * 		   Format: id label text
 * 
 * 
 * @author guanxin
 *
 */
public class readTxt {
	
	private class Entry{
		
		public String author;
		public String time;
		public String url;
		public String title;
		public String content;
		
		public Entry(){
			
			author = "";
			time = "";
			url = "";
			title = "";
			content = "";
			
		}
	}
	
	//xml google reader target tag
	/*
	private String str_content_start = "<content";
    private String str_content_end = "</content>";
    private String str_summary_start = "<summary";
    private String str_summary_end = "</summary>";
    private String str_title_start = "<title";
    private String str_title_end = "</title>";
    private String str_title_start_ = "<displayName";
    private String str_title_end_ = "</displayName>";
    private String str_entry_start = "<entry";
    private String str_entry_end = "</entry>";
    private String str_url_start = "<link ";
    private String str_url_end = "/>";
    private String str_author_start = "<author";
    private String str_author_end = "</author>";
    private String str_time_start = "<published";
    private String str_time_end = "</published>";
    */
    //rss wordpress format tag
	private String str_content_start = "<content:encoded><![CDATA[";
    private String str_content_end = "]]></content:encoded>";
    private String str_summary_start = "<summary";
    private String str_summary_end = "</summary>";
    private String str_title_start = "<title><![CDATA[";
    private String str_title_end = "]]></title>";
    private String str_entry_start = "<item>";
    private String str_entry_end = "</item>";
    private String str_url_start = "<link>";
    private String str_url_end = "</link>";
    private String str_author_start = "<dc:creator>";
    private String str_author_end = "</dc:creator>";
    private String str_time_start = "<pubDate>";
    private String str_time_end = "</pubDate>";
    
    //html cleaner target char
    private String htmlLT = "&lt;";
    private String htmlGT = "&gt;";
    private String htmlLT_symbol = "<";
    private String htmlGT_symbol = ">";
    
    public readTxt(){
    	
    	
    }
    
    private String getTitle(String entry){
    	
    	int start = entry.indexOf(str_title_start);
    	int end = entry.indexOf(str_title_end);
    	String text = entry.substring(start, end+str_title_end.length());
    	text = text.replaceAll("<!\\[CDATA\\[","");
    	text = text.replaceAll("\\]\\]>","");
    	text = htmlCleaner(text,htmlLT_symbol,htmlGT_symbol);
    	//replace \n \t and ' ' make them become single line.
    	text = text.replaceAll("\'", "\'\'");
    	text= text.replaceAll("\n","");
    	text = text.replaceAll("\t", "");
    	text = text.replaceAll(" +"," ");
    	text = text.replaceAll("\'","");
    	return text;
    }
    
    private String getContent(String entry){
    	
    	int start = entry.indexOf(str_content_start);
    	int end = 0;
    	if (start != -1){
    		end = entry.indexOf(str_content_end);
    	}
    	else{
    		start = entry.indexOf(str_summary_start);
    		if (start ==-1){
    			return "";
    		}
    		end = entry.indexOf(str_summary_end);
    	}
    	String text = entry.substring(start, end);
    	text = text.replaceAll("<!\\[CDATA\\[","");
    	text = text.replaceAll("\\]\\]>","");
    	text = htmlCleaner(text,htmlLT_symbol,htmlGT_symbol);
    	text = htmlCleaner(text,htmlLT,htmlGT);
    	//replace \n \t and ' ' make them become single line.
    	text= text.replaceAll("\n","");
    	text = text.replaceAll("\t", "");
    	text = text.replaceAll(" +"," ");
    	text = text.replaceAll("&amp;","");
    	return text;
    }
    
    private String getAuthor(String entry){
    	
    	int start = entry.indexOf(str_author_start);
    	int end = entry.indexOf(str_author_end);
    	String text = entry.substring(start, end+str_author_end.length());
    	text=text.replaceAll("<name>", "");
    	text=text.replaceAll("</name>", "");
    	text = htmlCleaner(text,htmlLT_symbol,htmlGT_symbol);
    	//replace \n \t and ' ' make them become single line.
    	text= text.replaceAll("\n","");
    	text = text.replaceAll("\t", "");
    	text = text.replaceAll(" +"," ");
    	text = text.replaceAll("\'","");
    	return text;
    }
    
    private String getTime(String entry){
    	
    	int start = entry.indexOf(str_time_start);
    	int end = entry.indexOf(str_time_end);
    	String text = entry.substring(start, end+str_time_end.length());
    	text = htmlCleaner(text,htmlLT_symbol,htmlGT_symbol);
    	//replace \n \t and ' ' make them become single line.
    	text= text.replaceAll("\n","");
    	text = text.replaceAll("\t", "");
    	text = text.replaceAll(" +"," ");
    	text = text.replaceAll("\'","");
    	return text;
    }
    
    private String getUrl(String entry){
    	//google reader
    	/*
    	int start = entry.indexOf(str_url_start);
    	int end = entry.indexOf(str_url_end,start);
    	String text = entry.substring(start, end);
    	start = text.indexOf("href=\"");
    	end = text.indexOf("\"",start+"href=\"".length()+1);
    	String result = text.substring(start+"href=\"".length(),end);
    	return result;
    	*/
    	//word press rss feed
    	int start = entry.indexOf(str_url_start);
    	int end = entry.indexOf(str_url_end,start);
    	String result = entry.substring(start+str_url_start.length(),end);
    	return result;
    }

    public Entry getEntry(String entryString){
    	Entry entry = new Entry();
    	
    	entry.author = getAuthor(entryString);
    	entry.title = getTitle(entryString);
    	entry.content = getContent(entryString);
    	entry.url= getUrl(entryString);
    	entry.time = getTime(entryString);
    	
    	return entry; 
    }
    
    private String[] getEntryString(String text, boolean contentFlag){
    	/**************************************
    	 * entry, rest, errorCode
    	 * errorCode: 
    	 * 0: nothing find, text, empty
    	 * 1: find end, no start, entry, rest
    	 * 2: find start, no end, entry, empty
    	 * 3: find start and end, entry, rest		
    	 */
    	String errorCode = String.valueOf(0);
    	int x = 0;
    	int y = 0;
    	if(contentFlag==false){
    		x= text.indexOf(str_entry_start,0);
	    	if(x!=-1){
	    		y = text.indexOf(str_entry_end,x);
	    		if(y!=-1){
	    			String entry = text.substring(x,y+str_entry_end.length());
	    			String rest = text.substring(y+str_entry_end.length());
	    			errorCode = String.valueOf(3);

	    			String[] result = {entry,rest,errorCode};
		        	return result;
	        	}
	    		errorCode = String.valueOf(2);
	    		String[] result = {text.substring(x),"",errorCode};
	    		return result;
	    	}
	    	else{
	    		errorCode = String.valueOf(0);
		    	String[] result = {text,"",errorCode};
		    	return result;
	    	}
    	}
    	else{
    	
	    	y = text.indexOf(str_entry_end,0);
			if(y!=-1){
	    		String entry = text.substring(0,y+str_entry_end.length());
	    		String rest = text.substring(y+str_entry_end.length());
	    		errorCode = String.valueOf(1);
	    		String[] result = {entry,rest,errorCode};
	        	return result;
	    	}
			
	    	errorCode = String.valueOf(0);
	    	String[] result = {text,"",errorCode};
	    	return result;
    	}
    }
    
    private void getEntries(String inputPath, String outputPath, Statement statement) throws SQLException{
    	
    	Entry entry = new Entry();
    	File file = new File(inputPath);
        StringBuffer contents = new StringBuffer();
        StringBuffer contents_ = new StringBuffer();
        BufferedReader reader = null;
        BufferedWriter out = null;
        BufferedWriter out_new = null;
        boolean contentFlag = false;
        try {
        	FileOutputStream output=new FileOutputStream(outputPath,true);
        	out = new BufferedWriter(new OutputStreamWriter(  
                    output));  
        	FileOutputStream output_new=new FileOutputStream(outputPath+"_new",true);
        	out_new = new BufferedWriter(new OutputStreamWriter(  
                    output_new));  
            
            reader = new BufferedReader(new FileReader(file));
            String text = null;
            
            // repeat until all lines is read
            while ((text = reader.readLine()) != null) {
            	System.out.println(text);
            	
            	while(true){
	            	String[] temp = getEntryString(text, contentFlag);
	            	String entry_ = temp[0];
	            	String rest_ = temp[1];
	            	int errorCode = Integer.parseInt(temp[2]);
	            	 /* errorCode: 
	                	 * 0: nothing find, text, empty
	                	 * 1: find end, no start, entry, rest
	                	 * 2: find start, no end, entry, empty
	                	 * 3: find start and end, entry, rest			
	                	 */
	            	if (errorCode == 0){
	            		//nothing find
	            		if (contentFlag){
	            			contents_.append(entry_);
	            		}
	            		break;
	            	}
	            	else if(errorCode==1){
	            		//find end
	            		contents_.append(entry_);
	            		contentFlag = false;
	            		text = rest_;
	            		//get an entry
	            		Entry newEntry = getEntry(contents_.toString());
	            		
	            		String sql = String.format("insert into blogs values(null, \'%s\',\'%s\',\'%s\', \'%s\', null, null, null)",
	            				newEntry.title, newEntry.author,newEntry.time,newEntry.url);
	            				
	            		statement.executeUpdate(sql);
	            		ResultSet newid_ =statement.executeQuery("SELECT last_insert_rowid()");
	            		
	            		int newid = newid_.getInt(1);
	            		
	            		out.append(String.valueOf(newid));
	            		out.append("\n");
	            		out.append(newEntry.content);
	            		out.append("\n");
	            		
	            		out_new.append(String.valueOf(newid));
	            		out_new.append("\n");
	            		out_new.append(newEntry.content);
	            		out_new.append("\n");
	            		
	            		
	            	}
	            	else if(errorCode ==2){
	            		//find start, no end
	            		contentFlag =true;
	            		contents_ = new StringBuffer();
	            		contents_.append(entry_);
	            		text = rest_;
	            		
	            	}
	            	else if(errorCode ==3){
	            		//find start and end
	            		contents_= new StringBuffer();
	            		contents_.append(entry_);
	            		text= rest_;
	            		contentFlag = false;
	            		//get an entry
	            		Entry newEntry = getEntry(contents_.toString());
	            		String sql = String.format("insert into blogs values(null, \'%s\',\'%s\',\'%s\', \'%s\', null, null, null)",
	            				newEntry.title, newEntry.author,newEntry.time,newEntry.url);
	            		
	            		statement.executeUpdate(sql);
	            		ResultSet newid_ =statement.executeQuery("SELECT last_insert_rowid()");
	            		
	            		int newid = newid_.getInt(1);
	            		
	            		out.append(String.valueOf(newid));
	            		out.append("\n");
	            		out.append(newEntry.content);
	            		out.append("\n");
	            		
	            		out_new.append(String.valueOf(newid));
	            		out_new.append("\n");
	            		out_new.append(newEntry.content);
	            		out_new.append("\n");
	            	}
	            	
            	}
            	
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            	out.close();
            	out_new.close();
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    	
    }
    
	
	public String htmlCleaner(String htmlContent, String lt, String gt){
		
		//clean text between &lt; and &gt;
		StringBuffer temp = new StringBuffer();
		int previousEnd = 0;
		int x = htmlContent.indexOf(lt,0);
        while(x!=-1){
        	temp.append(htmlContent.substring(previousEnd, x));
        	if(x!=0){
        		temp.append(" ");
        	}
        	int y = htmlContent.indexOf(gt,x+lt.length());
        	if(y!=-1){
        		
        		previousEnd = y+gt.length();
        		x = htmlContent.indexOf(lt,y+gt.length());
        	}
        	else{
        		
        		return "wrong parser";
        	}
        }
        
        temp.append(htmlContent.substring(previousEnd, htmlContent.length()));
		
		return temp.toString();
	}
	
	public static void createDataFile(String inputPath, String outputPath){
		
		File input = new File(inputPath);
		BufferedReader input_reader = null;
		BufferedWriter output = null;

		try{
			FileOutputStream output_writer = new FileOutputStream(outputPath,false);
			output = new BufferedWriter(new OutputStreamWriter(output_writer));

			input_reader = new BufferedReader(new FileReader(input));
			String text = null;
			int i = 0;
			while ((text = input_reader.readLine())!=null){
				
				if (i%2 == 0){
					output.append(text);
					output.append("\t");
					output.append("Yes");
					output.append("\t");

					text = input_reader.readLine();
					output.append(text);
					output.append("\n");

					i+=2;
				}
				
			}
			System.out.print("finish save future data\n");
		} catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	        	
	        	output.close();
	            if (input_reader != null) {
	                input_reader.close();
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }	
	}
	
	public static String getNewItems() throws ClientProtocolException, URISyntaxException, IOException{
		BufferedReader in = new BufferedReader(new FileReader(".\\resources\\timestamp"));
		long lasttimestamp = Long.parseLong(in.readLine());
		in.close();
		return getNewItems(lasttimestamp);
	}
	
	public static String getNewItems(long timestamp) throws URISyntaxException, ClientProtocolException, IOException{
		
		HttpClient httpclient = new DefaultHttpClient();
		URIBuilder builder = new URIBuilder();
		builder.setScheme("http").setHost("dhnow.chnm.org").setPath("/")
				.addParameter("feed", "feedforward")
				.addParameter("from",String.valueOf(timestamp));
		
		URI uri = builder.build();
		HttpGet httpget = new HttpGet(uri);
		System.out.println(httpget.getURI());
		HttpResponse response = httpclient.execute(httpget);
		String result = EntityUtils.toString(response.getEntity());
		BufferedWriter out = null; 
		long unixTime = System.currentTimeMillis() / 1000L;
		String outputPath = ".\\resources\\timestamp";
		FileOutputStream output=new FileOutputStream(outputPath,false);
    	out = new BufferedWriter(new OutputStreamWriter(  
                output));  
    	out.append(String.valueOf(unixTime));
    	out.close();
		return result;
	}
	
	public void write2file(String content, String outputPath) throws IOException{
		
		BufferedWriter out = null; 
		if (outputPath ==""){
			outputPath = ".\\resources\\test";
		}
		FileOutputStream output=new FileOutputStream(outputPath,false);
    	out = new BufferedWriter(new OutputStreamWriter(  
                output));  
    	out.append(content);
    	out.close();
	}
	
	
	public static void runReadTxt(long timestamp){
		readTxt read = new readTxt();
		String data;
		String inputPath = ".\\resources\\input";
		String outputPath = ".\\resources\\output";
		String outputPath_new = outputPath+"_new";
		Connection connection = null;
		try {
			Class.forName("org.sqlite.JDBC");
		    
		    // create a database connection
		    connection = DriverManager.getConnection("jdbc:sqlite:records.db");
		    Statement statement = connection.createStatement();
		    statement.setQueryTimeout(30);  // set timeout to 30 sec.
			statement.executeUpdate("create table if not exists blogs (id INTEGER PRIMARY KEY, title string, author string, time string, url string, label string, score string, gtruth string )");
			data = readTxt.getNewItems(timestamp);
			read.write2file(data, inputPath);
			System.out.println("\n start getting entries \n");
			read.getEntries(inputPath, outputPath,statement);
			readTxt.createDataFile(outputPath_new, ".\\resources\\futureData");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    finally
	    {
	      try
	      {
	        if(connection != null)
	          connection.close();
	      }
	      catch(SQLException e)
	      {
	        // connection close failed.
	        System.err.println(e);
	      }
	    }		
		
	}
	
	
	public static void runReadTxt_(String inputPath){
		readTxt read = new readTxt();
		
		String outputPath = ".\\resources\\output";
		String outputPath_new = outputPath+"_new";
		Connection connection = null;
		try {
			Class.forName("org.sqlite.JDBC");
		    
		    // create a database connection
		    connection = DriverManager.getConnection("jdbc:sqlite:records.db");
		    Statement statement = connection.createStatement();
		    statement.setQueryTimeout(30);  // set timeout to 30 sec.
			statement.executeUpdate("create table if not exists blogs (id INTEGER PRIMARY KEY, title string, author string, time string, url string, label string, score string, gtruth string )");
			System.out.println("\n start getting entries \n");
			read.getEntries(inputPath, outputPath,statement);
			readTxt.createDataFile(outputPath_new, ".\\resources\\futureData");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    finally
	    {
	      try
	      {
	        if(connection != null)
	          connection.close();
	      }
	      catch(SQLException e)
	      {
	        // connection close failed.
	        System.err.println(e);
	      }
	    }		
		
	}
/****************************************************************************************/
	
    /*************************
     * main 
     * @param args
     * @throws ClassNotFoundException 
     */
	public static void main(String[] args) throws ClassNotFoundException {
	
		if(args.length!=0){
			
			System.out.println(args[0]);
			runReadTxt_(args[0]);
		}
		else{
			try {
				String data = readTxt.getNewItems();
				//System.out.println("finish!");
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//runReadTxt();
		}
    }
}




/****************************************************************************************/
/*****************************Previous version, not used***********************************************************
	public void generatePosNegFile(String content, String title, String outputPath){
		
		File contentFile = new File(content);
		File titleFile = new File(title);
        BufferedReader content_reader = null;
        BufferedReader title_reader = null;
        BufferedWriter out_positive = null; 
        BufferedWriter out_negative = null;
        try {
        	FileOutputStream output_positive=new FileOutputStream(outputPath+"_positive",true);
        	out_positive = new BufferedWriter(new OutputStreamWriter(  
        			output_positive));  
        	
        	FileOutputStream output_negative=new FileOutputStream(outputPath+"_negative",true);
        	out_negative = new BufferedWriter(new OutputStreamWriter(  
        			output_negative));  
            
            content_reader = new BufferedReader(new FileReader(contentFile));
            title_reader = new BufferedReader(new FileReader(titleFile));
            String text = null;
            String choice = "Editors Choice:";
            int i = 0;
            // repeat until all lines is read
            while ((text = content_reader.readLine()) != null) {
            	i++;
            	boolean success = false;
            	String text_ = title_reader.readLine();
            	if(text.length()<2){
            		continue;
            	}
            		
        		if(text_.indexOf(choice)!=-1){
        			success = true;
        		}

            	if(success){
            		
            		out_positive.append(i+"\tYes\t"+text);
            		out_positive.append("\n");
            		
            	}else{
            		
            		out_negative.append(i+"\tNo\t"+text);
            		out_negative.append("\n");
            	}
            	
            	
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            	out_positive.close();
            	out_negative.close();
                if (content_reader != null) {
                    content_reader.close();
                }
                
                if(title_reader != null){
                	title_reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		
	}
	
	********************************************************************
	 * get a sample from the pool.  train and test doesn't overlap. assume neg have large amount instance
	 * @param positive: file for positive input
	 * @param negative: file for negative input
	 * @param output: file for output
	 * @param train_total: size of train
	 * @param test_total: size of test
	 * @param ratio: positive : total
	 */
	/****************************************************************************************************
	public void randomSample(String positive, String negative, String output, int train_total, int test_total, float ratio){
		
		//check positive and negative num and adjust the train set and test set size 
		int train_pos = Math.round(train_total*ratio);
		int test_pos = Math.round(test_total*ratio);
		File posFile = new File(positive);
		File negFile = new File(negative);
        BufferedReader pos_reader = null;
        BufferedReader neg_reader = null;
        BufferedWriter out_train = null; 
        BufferedWriter out_test = null;
        System.out.println("random sample start...");
        try{
        
	        LineNumberReader  lnr_pos = new LineNumberReader(new FileReader(posFile));
	        lnr_pos.skip(Long.MAX_VALUE);
	        
	        LineNumberReader  lnr_neg = new LineNumberReader(new FileReader(negFile));
	        lnr_neg.skip(Long.MAX_VALUE);
        
        
	        //adjust the num
	        while(train_pos+test_pos > lnr_pos.getLineNumber())
	        {
	        	train_pos--;
	        	test_pos--;
	        	train_total-=2;
	        	test_total-=2;
	        }
	        
	      //get random array for pos and neg
	        int[] pos_shuffle = randomShuffle(lnr_pos.getLineNumber());
	        int[] neg_shuffle = randomShuffle(lnr_neg.getLineNumber());
	        
	        int[] pos_train_shuffle = Arrays.copyOfRange(pos_shuffle, 0, train_pos);
	        int[] pos_test_shuffle = Arrays.copyOfRange(pos_shuffle, train_pos, train_pos+test_pos);
	        int[] neg_train_shuffle = Arrays.copyOfRange(neg_shuffle, 0, train_total-train_pos);
	        int[] neg_test_shuffle = Arrays.copyOfRange(neg_shuffle, train_total-train_pos, train_total-train_pos+test_total-test_pos);
	        
	        //sort all the array, prepare for generate files
	        Arrays.sort(pos_train_shuffle);
	        Arrays.sort(pos_test_shuffle);
	        Arrays.sort(neg_train_shuffle);
	        Arrays.sort(neg_test_shuffle);
	        
	        //generate files train and test
        
        	FileOutputStream output_train=new FileOutputStream(output+"_train",true);
        	out_train = new BufferedWriter(new OutputStreamWriter(  
        			output_train));  
        	
        	FileOutputStream output_test=new FileOutputStream(output+"_test",true);
        	out_test = new BufferedWriter(new OutputStreamWriter(  
        			output_test));  
            
            pos_reader = new BufferedReader(new FileReader(posFile));
            neg_reader = new BufferedReader(new FileReader(negFile));
            String text = null;
            
            int i = 0;
            int train_i=0;
            int test_i = 0;
            // repeat until all lines is read
            while ((text = pos_reader.readLine()) != null) {
            	
            	
            	if(train_i <pos_train_shuffle.length &&i == pos_train_shuffle[train_i])
            	{
            		out_train.append(text);
            		out_train.append("\n");
            		train_i++;
            		
            	}
            	else if (test_i<pos_test_shuffle.length && i == pos_test_shuffle[test_i])
            	{
            		out_test.append(text);
            		out_test.append("\n");
            		test_i ++;
            		
            	}
            	i++;
            	
            	if(train_i+test_i > train_pos+test_pos){
            		break;
            	}
            }
            
            //initial
            //read from neg file, append to train if the line is in train array, or appendto test if the line is in test array
            i = 0;
            train_i= 0; 
            test_i = 0;
            while ((text = neg_reader.readLine()) != null) {
            	
            	
            	if(train_i <neg_train_shuffle.length &&i == neg_train_shuffle[train_i])
            	{
            		out_train.append(text);
            		out_train.append("\n");
            		train_i++;
            		
            	}
            	else if (test_i<neg_test_shuffle.length && i == neg_test_shuffle[test_i])
            	{
            		out_test.append(text);
            		out_test.append("\n");
            		test_i ++;
            		
            	}
            	i++;
            	if(train_i+test_i > train_total-train_pos+test_total-test_pos){
            		break;
            	}
            }
            System.out.println("random sample end...");
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            	out_train.close();
            	out_test.close();
                if (pos_reader != null) {
                    pos_reader.close();
                }
                
                if(neg_reader != null){
                	neg_reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }		
	}
	
	public void kValidSample(String positive, String negative, String output, int k, float ratio){
		
		//check positive and negative num and adjust the folder size
		
		

		File posFile = new File(positive);
		File negFile = new File(negative);
        BufferedReader pos_reader = null;
        BufferedReader neg_reader = null;
        BufferedWriter out = null; 
        try{
        
	        LineNumberReader  lnr_pos = new LineNumberReader(new FileReader(posFile));
	        lnr_pos.skip(Long.MAX_VALUE);
	        
	        LineNumberReader  lnr_neg = new LineNumberReader(new FileReader(negFile));
	        lnr_neg.skip(Long.MAX_VALUE);
        
	        int folder_pos = Math.round(lnr_pos.getLineNumber()/k);
			int folderSize= Math.round(folder_pos/ratio);
			int folder_neg = folderSize - folder_pos;
	       
	        
			//get random array for pos and neg
	        int[] pos_shuffle = randomShuffle(lnr_pos.getLineNumber());
	        int[] neg_shuffle = randomShuffle(lnr_neg.getLineNumber());
	        
	        
	        
	        for(int i = 0; i <k; i++){

	        	int[] pos = Arrays.copyOfRange(pos_shuffle, folder_pos*i, folder_pos*(i+1));
	        	int[] neg = Arrays.copyOfRange(neg_shuffle, folder_neg*i, folder_neg*(i+1));
	        
		        //sort all the array, prepare for generate files
		        Arrays.sort(pos);
		        Arrays.sort(neg);	        
		        //generate files train and test
	        
	        	FileOutputStream output_=new FileOutputStream(output+"_"+i,true);
	        	out = new BufferedWriter(new OutputStreamWriter(  
	        			output_));  
	            
	            pos_reader = new BufferedReader(new FileReader(posFile));
	            neg_reader = new BufferedReader(new FileReader(negFile));
	            String text = null;
	            
	            int j = 0;
	            int shuffle_i=0;
	            // repeat until all lines is read
	            while ((text = pos_reader.readLine()) != null) {
	            	
	            	
	            	if(shuffle_i <pos.length &&j == pos[shuffle_i])
	            	{
	            		out.append(text);
	            		out.append("\n");
	            		shuffle_i++;
	            		
	            	}
	            	
	            	j++;
	            	
	            	if(shuffle_i >= pos.length){
	            		break;
	            	}
	            }
	            
	            //initial
	            //read from neg file, append to train if the line is in train array, or appendto test if the line is in test array
	            j = 0;
	            shuffle_i= 0; 
	            
	            while ((text = neg_reader.readLine()) != null) {
	            	
	            	
	            	if(shuffle_i <neg.length &&j == neg[shuffle_i])
	            	{
	            		out.append(text);
	            		out.append("\n");
	            		shuffle_i++;
	            		
	            	}
	            	j++;
	            	if(shuffle_i > neg.length){
	            		break;
	            	}
	            }
	            out.close();
	        }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            	out.close();
                if (pos_reader != null) {
                    pos_reader.close();
                }
                
                if(neg_reader != null){
                	neg_reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }		
	}
	
	
	public void generateMalletFile(String content, String title, String outputPath){
		
		File contentFile = new File(content);
		File titleFile = new File(title);
        BufferedReader content_reader = null;
        BufferedReader title_reader = null;
        BufferedWriter out = null; 
        try {
        	FileOutputStream output=new FileOutputStream(outputPath,true);
        	out = new BufferedWriter(new OutputStreamWriter(  
                    output));  
            
            content_reader = new BufferedReader(new FileReader(contentFile));
            title_reader = new BufferedReader(new FileReader(titleFile));
            String text = null;
            String choice = "Editors Choice:";
            int i = 2598;
            // repeat until all lines is read
            while ((text = content_reader.readLine()) != null) {
            	i++;
            	boolean success = false;
            	String text_ = title_reader.readLine();
            	if(text.length()<2){
            		continue;
            	}
            		
        		if(text_.indexOf(choice)!=-1){
        			success = true;
        		}

            	if(success){
            		
            		out.append(i+"\tYes\t"+text);
            		
            	}else{
            		
            		out.append(i+"\tNo\t"+text);
            	}
            	out.append("\n");
            	
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            	out.close();
                if (content_reader != null) {
                    content_reader.close();
                }
                
                if(title_reader != null){
                	title_reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		
	}
	
	public int[] randomShuffle(int num){
		Random rgen = new Random();  // Random number generator
		int[] cards = new int[num];  
	
		//--- Initialize the array to the ints 0-51
		for (int i=0; i<cards.length; i++) {
		    cards[i] = i;
		}
	
		//--- Shuffle by exchanging each element randomly
		for (int i=0; i<cards.length; i++) {
		    int randomPosition = rgen.nextInt(cards.length);
		    int temp = cards[i];
		    cards[i] = cards[randomPosition];
		    cards[randomPosition] = temp;
		}
		return cards;
	}
	***********************************************************/
	 /***************
     * xml to txt
	public static void main(String[] args) {
	
		String positive = null;
		String negative =  null;
		String output = null;
		int train_total = 0;
		int test_total= 0;
		float ratio =0;
		
		readTxt read = new readTxt();
		if(args.length == 0){
        
	        read.getContents(inputFile_pool,output_content);
	        //read.getTitles(inputFile_pool,output_title_pool,str_title_start_,str_title_end_);
	        read.getTitles(inputFile_pool,output_title_pool,str_title_start,str_title_end);
	        read.getTitles(inputFile_choice,output_title_choice,str_title_start_,str_title_end_);
	        read.getLabel(output_title_pool,output_title_choice,output_title);
	        read.generatePosNegFile(output_content, output_title, output_mallet);
	        //read.generateMalletFile(output_content,output_title,output_mallet);
		}
				//if(args.length == 0){
        
			positive = ".\\record\\instance\\output_mallet_positive";
			negative = ".\\record\\instance\\output_mallet_negative";
			output = ".\\randomSample33";
			train_total = 600;
			test_total = 105;
			ratio = (float) 0.33;
	        
		//}
		//read.kValidSample(positive, negative, output, 10, ratio);
		read.randomSample(positive,negative,output,train_total,test_total,ratio);
    }
    **************/
	

/********************download from google reader******************	
	public static void runReadTxt(){
		readTxt read = new readTxt();
		
		GoogleReaderFeed reader = new GoogleReaderFeed();
		
		String[] auth;
		String token;
		String data;
		String inputPath = "input";
		String outputPath = "output";
		String outputPath_new = outputPath+"_new";
		Connection connection = null;
		try {
			Class.forName("org.sqlite.JDBC");
		    
		    // create a database connection
		    connection = DriverManager.getConnection("jdbc:sqlite:records.db");
		    Statement statement = connection.createStatement();
		    statement.setQueryTimeout(30);  // set timeout to 30 sec.
			statement.executeUpdate("create table if not exists blogs (id INTEGER PRIMARY KEY, title string, author string, time string, url string, label string, score string, gtruth string )");
			auth = reader.getAuth();
			token = reader.getToken(auth);
			data = reader.getNewAtoms(auth, token, 3000);
			reader.write2file(data, inputPath);
			System.out.println("\n start getting entries \n");
			read.getEntries(inputPath, outputPath,statement);
			readTxt.createDataFile(outputPath_new, "futureData");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    finally
	    {
	      try
	      {
	        if(connection != null)
	          connection.close();
	      }
	      catch(SQLException e)
	      {
	        // connection close failed.
	        System.err.println(e);
	      }
	    }		
		
	}
	***********************************************************************/
/****************************Previous version end************************************************************/
