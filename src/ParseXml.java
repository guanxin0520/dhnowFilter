import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ParseXml extends DefaultHandler 
{ 

  private Vector<String> tagName; 

  private Vector<String> tagValue; 

  private int step; 
  
  public ParseXml ()
  {
	super();
  }

  // start parsing xml 
  public void startDocument() throws SAXException 
  { 
    tagName = new Vector<String>(); 
    tagValue = new Vector<String>(); 
    step = 0; 
  } 

  // finish parsing xml
  public void endDocument() throws SAXException 
  { 
    for (int i = 0; i < tagName.size(); i++) 
    { 
      if (!tagName.get(i).equals("") || tagName.get(i) != null) 
      { 
        System.out.println("node name:" + tagName.get(i)); 
        System.out.println("node value:" + tagValue.get(i)); 
      } 
    } 
  } 

  /** 
    *
    */ 
  public void startElement(String uri, String localName, String qName, 
      Attributes attributes) throws SAXException 
  { 
    // node name
    tagName.add(qName); 
    // attributes 
    for (int i = 0; i < attributes.getLength(); i++) 
    { 
      // attribute name
      System.out.println("attribute name:" + attributes.getQName(i)); 
      // attribute value
      System.out.println("attribute value:" 
          + attributes.getValue(attributes.getQName(i))); 
    } 

  } 

  /** 
    * meet element end syntax 
    */ 
  public void endElement(String uri, String localName, String qName) 
      throws SAXException 
  { 

    step = step + 1; 
  } 

  /** 
    * value
    */ 
  public void characters(char ch[], int start, int length) 
      throws SAXException 
  { 
    // 
    if (tagName.size() - 1 == tagValue.size()) 
    { 
      tagValue.add(new String(ch, start, length)); 
    } 
    
    if(tagName.size() ==1){
    	
    	System.out.print(ch);
    }
  } 

  public static void main(String[] args) 
  { 
    String filename = args[0]; 
    System.out.println("input file name:"+filename);
    SAXParserFactory spf = SAXParserFactory.newInstance(); 
    try 
    { 
    	/*
		XMLReader xr = XMLReaderFactory.createXMLReader();
		ParseXml handler = new ParseXml();
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);
		
		FileReader r = new FileReader(filename);
		InputSource r_ =new InputSource(r);
		r_.setEncoding("UTF-8");
	    xr.parse(r_);
    */
      InputStream inputStream= new FileInputStream(filename);
      Reader reader = new InputStreamReader(inputStream,"UTF-8");

      InputSource is = new InputSource(reader);
      is.setEncoding("UTF-8");
      SAXParser saxParser = spf.newSAXParser(); 
      saxParser.parse(is, new ParseXml()); 
    } 
    catch (Exception e) 
    { 
      e.printStackTrace(); 
    } 
  } 

  public Vector<String> getTagName() 
  { 
    return tagName; 
  } 

  public void setTagName(Vector<String> tagName) 
  { 
    this.tagName = tagName; 
  } 

  public Vector<String> getTagValue() 
  { 
    return tagValue; 
  } 

  public void setTagValue(Vector<String> tagValue) 
  { 
    this.tagValue = tagValue; 
  } 

} 
