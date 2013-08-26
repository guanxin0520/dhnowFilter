import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ca.uwo.csd.ai.nlp.common.SparseVector;
import ca.uwo.csd.ai.nlp.kernel.KernelManager;
import ca.uwo.csd.ai.nlp.kernel.LinearKernel;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import cc.mallet.util.Randoms;

/**************************************
 * train and evaluate classifier
 * @author guanxin
 *
 */

public class EvaluateClassifier {
	Pipe pipe;
	
	public EvaluateClassifier(){
		pipe = buildPipe();
	}
	
	private static String RECOMMEND = "recommend";
	private static String RELATED = "RELATED";
	private static String NOTRECOMMEND = "not recommend";
	private static String UNKNOWN = "unknown";
	
	public Pipe buildPipe() {
        ArrayList pipeList = new ArrayList();

        // Read data from File objects
        pipeList.add(new Input2CharSequence("UTF-8"));

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers, 
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        Pattern tokenPattern =
            Pattern.compile("[\\p{L}\\p{N}_]+");

        // Tokenize raw strings
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        // Normalize all tokens to all lowercase
        pipeList.add(new TokenSequenceLowercase());

        // Remove stopwords from a standard English stoplist.
        //  options: [case sensitive] [mark deletions]
        pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        // Rather than storing tokens as strings, convert 
        //  them to integers by looking them up in an alphabet.
        pipeList.add(new TokenSequence2FeatureSequence());

        // Do the same thing for the "target" field: 
        //  convert a class label string to a Label object,
        //  which has an index in a Label alphabet.
        pipeList.add(new Target2Label());

        // Now convert the sequence of features to a sparse vector,
        //  mapping feature IDs to counts.
        pipeList.add(new FeatureSequence2FeatureVector());

        // Print out the features and the label
        //pipeList.add(new PrintInputAndTarget());

        return new SerialPipes(pipeList);
    }
	
	public InstanceList readSingleFile(File file){
		return readSingleFile(file, pipe);
    }
	
	public InstanceList readSingleFile(File file, Pipe temp_pipe){
    	InstanceList instances = new InstanceList(temp_pipe);
    	try{
    		CsvIterator reader = new CsvIterator(new FileReader(file), "(\\w+)\\s+(\\w+)\\s+(.*)",3,2,1);
    		
    		instances.addThruPipe(reader);
	    }
		catch(IOException e) {
	        e.printStackTrace();
			
		}	
		return instances;
    }
	
	public InstanceList readKFolderFiles(String path, int k , int test_num){
    	InstanceList instances = new InstanceList(pipe);
    	try{
    		for(int i = 0; i < k; i++)
    		{
    			if(i != test_num){
    				CsvIterator reader = new CsvIterator(new FileReader(new File(path+i)), "(\\w+)\\s+(\\w+)\\s+(.*)",3,2,1);
        		
    				instances.addThruPipe(reader);
    			}
    		}
    		
	    }
		catch(IOException e) {
	        e.printStackTrace();
			
		}	
		return instances;
    }
	
	public Classifier loadClassifier(File serializedFile)
	    throws FileNotFoundException, IOException, ClassNotFoundException {
	
	    // The standard way to save classifiers and Mallet data                                            
	    //  for repeated use is through Java serialization.                                                
	    // Here we load a serialized classifier from a file.                                               
	
	    Classifier classifier;
	
	    ObjectInputStream ois =
	        new ObjectInputStream (new FileInputStream (serializedFile));
	    classifier = (Classifier) ois.readObject();
	    ois.close();
	
	    return classifier;
	}
	
	public void evaluateInstanceList(Classifier classifier, InstanceList testInstances){
	
		evaluateInstanceList(classifier, testInstances,"");
	}
	
	public void evaluateInstanceList(Classifier classifier, InstanceList testInstances,String classifierIdentity){
		
		Trial trial = new Trial(classifier, testInstances);
        
        // The Trial class implements many standard evaluation                                             
        //  metrics. See the JavaDoc API for more details.                                                 
        ConfusionMatrix matrix = new ConfusionMatrix(trial);
        System.out.println(matrix.toString());
        System.out.println("Accuracy: " + trial.getAccuracy());

	// precision, recall, and F1 are calcuated for a specific                                          
        //  class, which can be identified by an object (usually                                           
	//  a String) or the integer ID of the class                                                       

        System.out.println("F1 for class 'Yes': " + trial.getF1("Yes"));

        System.out.println("Precision for class '" +
                           classifier.getLabelAlphabet().lookupLabel(0) + "': " +
                           trial.getPrecision("Yes"));
        System.out.println("Recall for class '" +
                classifier.getLabelAlphabet().lookupLabel(0) + "': " +
                trial.getRecall("Yes"));
        System.out.println("F1 for class 'No': " + trial.getF1("No"));

        System.out.println("Precision for class '" +
                           classifier.getLabelAlphabet().lookupLabel(1) + "': " +
                           trial.getPrecision("No"));
        System.out.println("Recall for class '" +
                classifier.getLabelAlphabet().lookupLabel(1) + "': " +
                trial.getRecall("No"));
        System.out.println("evaluate end...");
        
        //write to file
        try{
        	  String date = new SimpleDateFormat("MMddyyyy").format(new Date());
        	  StringBuffer output = new StringBuffer();
        	  output.append("<classifier>\n");
        	  output.append("\t<date>"+date+"</date>\n");
        	  output.append("\t<description>"+classifierIdentity+"</description>\n");
        	  output.append("\t<Accuracy>"+String.format("%.2f",trial.getAccuracy())+"</Accuracy>\n");
        	  output.append("\t<F1>"+String.format("%.2f", trial.getF1("Yes"))+"</F1>\n");
        	  output.append("\t<Precision>"+String.format("%.2f",trial.getPrecision("Yes"))+"</Precision>\n");
        	  output.append("\t<Recall>"+String.format("%.2f",trial.getRecall("Yes"))+"</Recall>\n");
        	  output.append("\t<ConfusionMatrix>\n\t"+matrix.toString()+"\t</ConfusionMatrix>\n");
        	  output.append("</classifier>\n");
        	  // Create file 
        	  FileWriter fstream = new FileWriter("./output/report.xml",true);
        	  BufferedWriter out = new BufferedWriter(fstream);
        	  out.write(output.toString());

        	  //Close the output stream
        	  out.close();
        	  fstream.close();
        	  FileWriter fstream_ = new FileWriter("./output/report_"+classifierIdentity+"_"+date+".xml",false);
        	  BufferedWriter out_ = new BufferedWriter(fstream_);
        	  out_.write("<?xml version=\"1.0\"?>\n");
        	  out_.write(output.toString());
        	  out_.close();
        	  fstream_.close();
        }catch (Exception e){//Catch exception if any
        	  System.err.println("Error: " + e.getMessage());
        }
		
	}
	
	/* unused functions begin*******************************************************************
	 public void evaluate(Classifier classifier, File file) throws IOException {
		 System.out.println("evaluate start...");
        // Create an InstanceList that will contain the test data.                                         
        // In order to ensure compatibility, process instances                                             
        //  with the pipe used to process the original training                                            
        //  instances.                                                                                     

        InstanceList testInstances = new InstanceList(classifier.getInstancePipe());

        // Create a new iterator that will read raw instance data from                                     
        //  the lines of a file.                                                                           
        // Lines should be formatted as:                                                                   
        //                                                                                                 
        //   [name] [label] [data ... ]                                                                    

        CsvIterator reader =
            new CsvIterator(new FileReader(file),
                            "(\\w+)\\s+(\\w+)\\s+(.*)",
                            3, 2, 1);  // (data, label, name) field indices               

        // Add all instances loaded by the iterator to                                                     
        //  our instance list, passing the raw input data                                                  
        //  through the classifier's original input pipe.                                                  

        testInstances.addThruPipe(reader);

        Trial trial = new Trial(classifier, testInstances);
        
        // The Trial class implements many standard evaluation                                             
        //  metrics. See the JavaDoc API for more details.                                                 
        ConfusionMatrix matrix = new ConfusionMatrix(trial);
        System.out.println(matrix.toString());
        System.out.println("Accuracy: " + trial.getAccuracy());

	// precision, recall, and F1 are calcuated for a specific                                          
        //  class, which can be identified by an object (usually                                           
	//  a String) or the integer ID of the class                                                       

        System.out.println("F1 for class 'Yes': " + trial.getF1("Yes"));

        System.out.println("Precision for class '" +
                           classifier.getLabelAlphabet().lookupLabel(0) + "': " +
                           trial.getPrecision("Yes"));
        System.out.println("Recall for class '" +
                classifier.getLabelAlphabet().lookupLabel(0) + "': " +
                trial.getRecall("Yes"));
        System.out.println("F1 for class 'No': " + trial.getF1("No"));

        System.out.println("Precision for class '" +
                           classifier.getLabelAlphabet().lookupLabel(1) + "': " +
                           trial.getPrecision("No"));
        System.out.println("Recall for class '" +
                classifier.getLabelAlphabet().lookupLabel(1) + "': " +
                trial.getRecall("No"));
        System.out.println("evaluate end...");
        
        //write to file
        try{
        	  // Create file 
        	  FileWriter fstream = new FileWriter(".\\output\\report.txt",true);
        	  BufferedWriter out = new BufferedWriter(fstream);
        	  out.write(matrix.toString());
        	  out.write("\n");
        	  out.write("Accuracy: " + trial.getAccuracy());
        	  out.write("\n");
      	// precision, recall, and F1 are calcuated for a specific                                          
              //  class, which can be identified by an object (usually                                           
      	//  a String) or the integer ID of the class                                                       

        	  out.write("F1 for class 'Yes': " + trial.getF1("Yes"));
        	  out.write("\n");
        	  out.write("Precision for class '" +
                                 classifier.getLabelAlphabet().lookupLabel(0) + "': " +
                                 trial.getPrecision("Yes"));
        	  out.write("\n");
        	  out.write("Recall for class '" +
                      classifier.getLabelAlphabet().lookupLabel(0) + "': " +
                      trial.getRecall("Yes"));
        	  out.write("\n");
        	  out.write("F1 for class 'No': " + trial.getF1("No"));
        	  out.write("\n");

        	  out.write("Precision for class '" +
                                 classifier.getLabelAlphabet().lookupLabel(1) + "': " +
                                 trial.getPrecision("No"));
        	  out.write("\n");
        	  out.write("Recall for class '" +
                      classifier.getLabelAlphabet().lookupLabel(1) + "': " +
                      trial.getRecall("No"));
        	  out.write("\n");
        	  
        	  //Close the output stream
        	  out.close();
        }catch (Exception e){//Catch exception if any
        	  System.err.println("Error: " + e.getMessage());
        }
	}
	 
	 public Trial testTrainSplit(InstanceList instances) {

	        int TRAINING = 0;
	        int TESTING = 1;
	        int VALIDATION = 2;

	        // Split the input list into training (90%) and testing (10%) lists.                               
		// The division takes place by creating a copy of the list,                                        
		//  randomly shuffling the copy, and then allocating                                               
		//  instances to each sub-list based on the provided proportions.                                  

	        InstanceList[] instanceLists =
	            instances.split(new Randoms(),
		                    new double[] {0.7, 0.3, 0.0});

		// The third position is for the "validation" set,                                                 
	        //  which is a set of instances not used directly                                                  
	        //  for training, but available for determining                                                    
	        //  when to stop training and for estimating optimal                                               
		//  settings of nuisance parameters.                                                               
		// Most Mallet ClassifierTrainers can not currently take advantage                                 
	        //  of validation sets.                                                                            

		Classifier classifier = trainClassifier( instanceLists[TRAINING] );
		saveClassifier(classifier, new File(".\\resources\\classifier"));
	        return new Trial(classifier, instanceLists[TESTING]);
	}
	 
	public void evaluatePatial(InstanceList instanceList){
		
		InstanceList[] instanceLists =
	            instanceList.split(new Randoms(),
		                    new double[] {0.5, 0.5, 0.0});
		InstanceList instance_train =instanceLists[0];
		InstanceList instance_test = instanceLists[1];
		for(int i = 1; i < 11; i++){
			InstanceList[] partialList =
		            instance_train.split(new Randoms(),
			                    new double[] {0.1*i, 1-0.1*i, 0.0});
			Classifier classifier = trainClassifier(partialList[0]);
			evaluateInstanceList(classifier,instance_test);
			
		}
		
		
	}
	
	**************unused functions end**********************/
	 
    public Classifier trainClassifier(InstanceList trainingInstances){
    	
    	System.out.println("train classifier start...");
    	//ClassifierTrainer trainer = new MaxEntTrainer();
    	//ClassifierTrainer trainer = new NaiveBayesTrainer();
    	//ClassifierTrainer trainer = new C45Trainer();
    	ClassifierTrainer trainer = new SVMClassifierTrainer(new LinearKernel(),false);
    	
    	
    	Classifier temp = trainer.train(trainingInstances);
    	Trial trial = new Trial(temp, trainingInstances);
    	ConfusionMatrix matrix = new ConfusionMatrix(trial);
    	double rho = ((SVMClassifier)temp).getRho()[0];
    	System.out.println("rho:"+rho);
    	System.out.println(matrix.toString());
        
        System.out.println("F1 for class 'Yes': " + trial.getF1("Yes"));

        System.out.println("Precision for class '" +
                           temp.getLabelAlphabet().lookupLabel(0) + "': " +
                           trial.getPrecision("Yes"));
        System.out.println("F1 for class 'No': " + trial.getF1("No"));

        System.out.println("Precision for class '" +
                           temp.getLabelAlphabet().lookupLabel(1) + "': " +
                           trial.getPrecision("No"));
        System.out.println("train classifer end...");
    	return temp;
    }
    
    public void saveClassifier(Classifier classifier, File serializedFile){
    	try{
    		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream (serializedFile));
    		oos.writeObject(classifier);
    		oos.close();
    	}
    	catch(IOException e){
    		e.printStackTrace();
    	}
    	
    }
    
    
    public void saveVector(InstanceList instances, String outputPath){
    	
    	BufferedWriter out_vector = null; 
    	BufferedWriter out_label = null; 
    	SVMClassifierTrainer trainer = new SVMClassifierTrainer(new LinearKernel(),true);
    	try {
        	FileOutputStream output_vector=new FileOutputStream(outputPath,false);
        	FileOutputStream output_label=new FileOutputStream(outputPath+"_label",false);
        	out_vector = new BufferedWriter(new OutputStreamWriter(  
        			output_vector)); 
        	out_label = new BufferedWriter(new OutputStreamWriter(  
        			output_label));  
        	List<ca.uwo.csd.ai.nlp.libsvm.ex.Instance> list= trainer.getSVMSparesInstances(instances);
			Iterator<ca.uwo.csd.ai.nlp.libsvm.ex.Instance> iterator = list.iterator();
			int i = 0;
			while(iterator.hasNext()){
				
				ca.uwo.csd.ai.nlp.libsvm.ex.Instance  temp = iterator.next();
				SparseVector vector = (SparseVector)temp.getData();
				double label = temp.getLabel();
				int size = vector.size();
				for (int x = 0; x <size; x++ ){
					
					//System.out.print(vector.get(x).toString());
					
					out_vector.append(String.valueOf(i+1)+"\t"+String.valueOf(vector.get(x).index+1)+"\t"+String.valueOf(vector.get(x).value)+"\n");
				}
				out_label.append(String.valueOf(label)+"\n");
				i++;
			}
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            try {
            	out_label.close();
            	out_vector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void printLabelings(Classifier classifier, File file) throws IOException {

        // Create a new iterator that will read raw instance data from                                     
        //  the lines of a file.                                                                           
        // Lines should be formatted as:                                                                   
        //                                                                                                 
        //   [name] [label] [data ... ]                                                                    
        //                                                                                                 
        //  in this case, "label" is ignored.                                                              

        CsvIterator reader =
            new CsvIterator(new FileReader(file),
                            "(\\w+)\\s+(\\w+)\\s+(.*)",
                            3, 2, 1);  // (data, label, name) field indices               

        // Create an iterator that will pass each instance through                                         
        //  the same pipe that was used to create the training data                                        
        //  for the classifier.                                                                            
        Iterator instances =
            classifier.getInstancePipe().newIteratorFrom(reader);

        // Classifier.classify() returns a Classification object                                           
        //  that includes the instance, the classifier, and the                                            
        //  classification results (the labeling). Here we only                                            
        //  care about the Labeling.                         
        int i = 0;
        while (instances.hasNext()) {
        	Instance next = (Instance) instances.next();
            Labeling labeling = classifier.classify(next).getLabeling();

            // print the labels with their weights in descending order (ie best first)                     
            if (labeling.getLabelAtRank(0).toString().equals("Yes")&labeling.getValueAtRank(0)>0.995)
            {
            	i++;
            }
            for (int rank = 0; rank < labeling.numLocations(); rank++){
                System.out.print(next.getName().toString()+" "+labeling.getLabelAtRank(rank) + ":" +
                                 labeling.getValueAtRank(rank) + " ");
               
            }
            System.out.println("i = "+i);

        }
    }
    
    public void saveInstancesList(InstanceList instanceList, File outputPath ){
    	
    	ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(outputPath));
			oos.writeObject(instanceList);
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public Classifier activeLearningTrainer_static(InstanceList instances, int initial, int iteration, int query){
		//initial sample 10:10 pos:neg to be the training set
		java.util.Random r = new java.util.Random(System.currentTimeMillis());
		InstanceList trainSet = instances.cloneEmpty();
		instances.shuffle(r);
		int temp_i = 0;
		int pos = 0;
		int neg = 0;
		ArrayList<Integer> temp= new ArrayList<Integer>();
		while(trainSet.size()<2*initial && temp_i<instances.size()){
			if (instances.get(temp_i).getLabeling().toString().equals("Yes") && pos<initial){
				trainSet.add(instances.get(temp_i));
				temp.add(temp_i);
				pos++;
			}
			else if (instances.get(temp_i).getLabeling().toString().equals("No") && neg<initial){
				
				if(r.nextDouble()< 0.3){
					
					trainSet.add(instances.get(temp_i));
					temp.add(temp_i);
					neg++;
				}
			}
			temp_i++;
		}
		
		//remove from instances list
		for(int i = temp.size()-1;i>=0;i--){
			
			int temp_j = temp.get(i);
			
			instances.remove(temp_j);
			
		}
		//train classifier using training set for n times
		Classifier classifier_r = null;
		for(int i = 0; i < iteration; i++){
			
			//train classifier using training set
			classifier_r = this.trainClassifier(trainSet);
			//get next query instances
			ArrayList<Classification> c = classifier_r.classify(instances);
			ArrayList<Double> decisionSort = new ArrayList<Double>();
			for (int j = 0 ; j < c.size();j++){
				
				Classification temp_c = c.get(j);
				double a =temp_c.getLabelVector().value(temp_c.getLabelVector().getLabelAlphabet().lookupLabel(0));
				double b =temp_c.getLabelVector().value(temp_c.getLabelVector().getLabelAlphabet().lookupLabel(1));
				if( Math.abs(a)>Math.abs(b)){
					decisionSort.add(Double.valueOf(Math.abs(a)));
				}else{
					decisionSort.add(Double.valueOf(Math.abs(b)));
				}
			}
			
			Collections.sort(decisionSort);
			// add to training set & remove from instances list
			double lastValue  = Double.POSITIVE_INFINITY;
			if (query < decisionSort.size()){
				lastValue = decisionSort.get(query);
			}
			
			int ii = 0;
			for (int j = 0 ; j < c.size();j++){
				
				Classification temp_c = c.get(j);
				double a =temp_c.getLabelVector().value(temp_c.getLabelVector().getLabelAlphabet().lookupLabel(0));
				double b =temp_c.getLabelVector().value(temp_c.getLabelVector().getLabelAlphabet().lookupLabel(1));
				if( Math.abs(a)>Math.abs(b)){
					if(Math.abs(a)<= lastValue && ii <20){
						trainSet.add(temp_c.getInstance());
						instances.remove(temp_c.getInstance());
						ii++;
					}
				}else{
					if(Math.abs(b)<= lastValue && ii <20){
						trainSet.add(temp_c.getInstance());
						instances.remove(temp_c.getInstance());
						ii++;
					}
				}
			}
				
		}
		//save classifiers
		if (classifier_r!=null)
		{
			this.saveClassifier(classifier_r,new File("./resources/newClassifier"));
			saveInstancesList(trainSet,new File("./resources/newTrainSet"));
		}
		
		return classifier_r;
    }
    
    public void saveCsv(ArrayList<Classification> classArray, File result) throws ClassNotFoundException{
		//File result = new File("result.csv");
        BufferedWriter out = null; 
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
	    // create a database connection
	    try {
	    	FileOutputStream output=new FileOutputStream(result,false);
	    	out = new BufferedWriter(new OutputStreamWriter(  
	                output)); 
	    	out.append("\"id\",\"title\",\"url\",\"recommend\",\"score\",\"gtruth\"\n");
			connection = DriverManager.getConnection("jdbc:sqlite:records.db");

		    Statement statement = connection.createStatement();
		    statement.setQueryTimeout(30);  // set timeout to 30 sec.
			for(int i = 0;i < classArray.size();i++){
				Classification temp = classArray.get(i);
				String label = null;
	        	double a =temp.getLabelVector().value(temp.getLabelVector().getLabelAlphabet().lookupLabel(0));
				double b =temp.getLabelVector().value(temp.getLabelVector().getLabelAlphabet().lookupLabel(1));
				String id = (String)temp.getInstance().getName();
				String sql = String.format("select * from blogs where \"id\"=%s",id);
        		
        		ResultSet query = statement.executeQuery(sql);
        		String recommend = RELATED;
        		double score = a;
				if( Math.abs(a)>Math.abs(b)){
					label = temp.getLabelVector().getLabelAlphabet().lookupLabel(0).toString();
					System.out.println(temp.getInstance().getName()+":"+a+":"+label);
					score = a;
					if(label.equals("Yes") &&  score> 0.5){
						recommend = RECOMMEND;
					}
					else if (label.equals("No") && score<-0.1){
						
						recommend = NOTRECOMMEND;
					}
			
				}else{
					label = temp.getLabelVector().getLabelAlphabet().lookupLabel(1).toString();
					System.out.println(temp.getInstance().getName()+":"+b+":"+label);
					score = b;
					if(label.equals("Yes") &&  score> 0.5){
						recommend = RECOMMEND;
					}
					else if (label.equals("No") && score<-0.1){
						
						recommend = NOTRECOMMEND;
					}
				}
				out.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%.4f\",\"\"",temp.getInstance().getName(),query.getString(2),query.getString(5),recommend,score));
				out.newLine();
				sql = String.format("update blogs set \"label\" = \"%s\" , \"score\" = \"%.4f\" where \"id\"=%s",recommend,score,id);
				statement.executeUpdate(sql);
				
			}
			connection.close();
			out.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
    public void saveXML(ArrayList<Classification> classArray, File result) throws ClassNotFoundException{
		//File result = new File("result.csv");
        BufferedWriter out = null; 
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
	    // create a database connection
	    try {
	    	FileOutputStream output=new FileOutputStream(result,false);
	    	out = new BufferedWriter(new OutputStreamWriter(  
	                output)); 
	    	out.append("<?xml version=\"1.0\"?>\n");
	    	out.append("<channel>\n");
	    	
			connection = DriverManager.getConnection("jdbc:sqlite:records.db");

		    Statement statement = connection.createStatement();
		    statement.setQueryTimeout(30);  // set timeout to 30 sec.
			for(int i = 0;i < classArray.size();i++){
				out.append("<item>\n");
		    	
				Classification temp = classArray.get(i);
				String label = null;
	        	double a =temp.getLabelVector().value(temp.getLabelVector().getLabelAlphabet().lookupLabel(0));
				double b =temp.getLabelVector().value(temp.getLabelVector().getLabelAlphabet().lookupLabel(1));
				String id = (String)temp.getInstance().getName();
				String sql = String.format("select * from blogs where \"id\"=%s",id);
        		
        		ResultSet query = statement.executeQuery(sql);
        		String recommend = RELATED;
        		double score = a;
				if( Math.abs(a)>Math.abs(b)){
					label = temp.getLabelVector().getLabelAlphabet().lookupLabel(0).toString();
					System.out.println(temp.getInstance().getName()+":"+a+":"+label);
					score = a;
					if(label.equals("Yes") &&  score> 0.5){
						recommend = RECOMMEND;
					}
					else if (label.equals("No") && score<-0.1){
						
						recommend = NOTRECOMMEND;
					}
			
				}else{
					label = temp.getLabelVector().getLabelAlphabet().lookupLabel(1).toString();
					System.out.println(temp.getInstance().getName()+":"+b+":"+label);
					score = b;
					if(label.equals("Yes") &&  score> 0.5){
						recommend = RECOMMEND;
					}
					else if (label.equals("No") && score<-0.1){
						
						recommend = NOTRECOMMEND;
					}
				}
				//out.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%.4f\",\"\"",temp.getInstance().getName(),query.getString(2),query.getString(5),recommend,score));
				out.append(String.format("\t<id>%s</id>\n", temp.getInstance().getName()));
				out.append(String.format("\t<title><![CDATA[%s]]></title>\n", query.getString(2)));
				out.append(String.format("\t<link><![CDATA[%s]]></link>\n", query.getString(5)));
				out.append(String.format("\t<label>%s</label>\n", recommend));
				out.append(String.format("\t<score>%.4f</score>\n", score));
				out.append("\t<gtruth></gtruth>\n");
				out.append("</item>\n");
				sql = String.format("update blogs set \"label\" = \"%s\" , \"score\" = \"%.4f\" where \"id\"=%s",recommend,score,id);
				statement.executeUpdate(sql);
				
			}
			connection.close();
			out.append("</channel>\n");
			out.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }

    public void updateGroundTruth(File result) throws ClassNotFoundException{
		//File result = new File("result.csv");
        BufferedReader in = null; 
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
	    // create a database connection
	    try {
	    	in = new BufferedReader(new FileReader(result));
	    	
			connection = DriverManager.getConnection("jdbc:sqlite:records.db");

		    Statement statement = connection.createStatement();
		    statement.setQueryTimeout(30);  // set timeout to 30 sec.
		    
            
            // repeat until all lines is read
            /*******deal with csv file.
            String text = null;
            in.readLine();
            while ((text = in.readLine()) != null){
            	String[] item = text.split("\",\"");
            	String id = item[0].replace("\"", "");
        		String groundTruth = item[5].replace("\"", "");
        		if(groundTruth.toLowerCase().equals("yes")||groundTruth.equals("y")||groundTruth.equals("Y")){
        			groundTruth = RECOMMEND;
        		}
        		else if(groundTruth.toLowerCase().equals("no")||groundTruth.equals("n")||groundTruth.equals("N")){
        			groundTruth = NOTRECOMMEND;
        		}
        		if(groundTruth.equals("")==false){
        			String sql = String.format("update blogs set \"gtruth\" = \"%s\" where \"id\"=%s",groundTruth,id);
        			statement.executeUpdate(sql);
        		}
				
			}
			**********************/
            try {
            	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            	DocumentBuilder dBuilder;
			
				dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(result);
				NodeList nList = doc.getElementsByTagName("item");
				 
				String id = "";
				String groundTruth = "";
			 
				for (int i = 0; i < nList.getLength(); i++) {
			 
					Node nNode = nList.item(i);
			 
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
			 
						Element eElement = (Element) nNode;
						id = eElement.getElementsByTagName("id").item(0).getTextContent();
						groundTruth = eElement.getElementsByTagName("gtruth").item(0).getTextContent();
						
						if(groundTruth.toLowerCase().equals("yes")||groundTruth.equals("y")||groundTruth.equals("Y")){
		        			groundTruth = RECOMMEND;
		        		}
		        		else if(groundTruth.toLowerCase().equals("no")||groundTruth.equals("n")||groundTruth.equals("N")){
		        			groundTruth = NOTRECOMMEND;
		        		}
		        		if(groundTruth.equals("")==false){
		        			String sql = String.format("update blogs set \"gtruth\" = \"%s\" where \"id\"=%s",groundTruth,id);
		        			statement.executeUpdate(sql);
		        		}
		        		id = "";
						groundTruth = "";
					}
				}
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }

    //update classifier by new data with ground truth label
    public Classifier updateClassifier(InstanceList trainSet, InstanceList newDataSet){
    	
    	//if trainset contains too many instance (more than 400), remove the first 20 or random remove 20
    	if(trainSet.size()>=400){
    		Random r = new Random();
    		trainSet.shuffle(r);
    		trainSet = trainSet.subList(0, 400-newDataSet.size());
    	}
    	//add new dataset to trainset and save it to new train set
    	
    	trainSet.addAll(newDataSet);
    	//get new classifier from new trainset
    	Classifier classifier_r = this.trainClassifier(trainSet);
    	
    	//save new train set & classifier
    	deleteFile("./resources/newTrainSet");
    	saveInstancesList(trainSet,new File("./resources/newTrainSet"));
    	saveClassifier(classifier_r,new File("./resources/newClassifier"));
    	//evaluate new classifier
    	InstanceList testSet = InstanceList.load(new File("./resources/testSet"));
    	Classifier classifier;
		try {
			classifier = loadClassifier(new File("./resources/classifier"));
			evaluateInstanceList(classifier, testSet,"old_classifier");
			evaluateInstanceList(classifier_r,testSet,"update_classifier");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return classifier_r;
    }
    
    //generate instance list with ground truth label
    public InstanceList generateUpdateDataSet(){
    	
    	InstanceList newTrainSet = new InstanceList(pipe);
    	InstanceList newTestSet = InstanceList.load(new File("./resources/testSet"));
    	
    	try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Connection connection = null;
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:records.db");
		
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);  // set timeout to 30 sec.
	    	
			//get instance list from future data
			InstanceList temp = readSingleFile(new File("./resources/futureData"),newTestSet.getPipe());
			for(Instance instance: temp){
				//get ground truth from database
				String id = instance.getName().toString();
				
				String sql = String.format("select * from blogs where \"id\"=%s",id);
				ResultSet query = statement.executeQuery(sql);
				String groundTruth = query.getString(8);
				String label = query.getString(6);
				//if no ground truth, ignore
				if(groundTruth!=null){
					if(groundTruth.equals(RECOMMEND)||groundTruth.equals(NOTRECOMMEND)){
						
						instance.unLock();
						if(groundTruth.equals(RECOMMEND)){
							
							instance.setTarget(((LabelAlphabet) newTestSet.getTargetAlphabet()).lookupLabel("Yes"));
							
						}
						else{
							instance.setTarget(((LabelAlphabet) newTestSet.getTargetAlphabet()).lookupLabel("No"));
						}
						instance.lock();
			
						if(label.equals(RELATED)){
							newTrainSet.add(instance);
						}
						
						newTestSet.add(instance);
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
    	//if too many new data, random choose 20
    	if(newTrainSet.size()>20){
    		Random rand = new Random();
    		newTrainSet.shuffle(rand);
    		newTrainSet = newTrainSet.subList(0, 20);	
    	}
    	
    	if(newTestSet.size()>1000){
    		Random r = new Random();
    		newTestSet.shuffle(r);
    		newTestSet = newTestSet.subList(0, 1000);
    		deleteFile("./resources/testSet");
    		saveInstancesList(newTestSet,new File("./resources/testSet"));
    	}
    	
    	return newTrainSet;
    }
    
    //accept the new classifier
    public void replaceClassifier(){
    	try {
			replaceFile("./resources/classifier","./resources/newClassifier");
			replaceFile("./resources/trainSet","./resources/newTrainSet");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    //delete file
    public void deleteFile(String input){
    	//delete file
    	File file = new File(input);
    	
    	if(file.delete()){
    		
    		System.out.println(file.getName() + " is deleted!");
		}else{
			System.out.println("Delete operation is failed.");
		}
    	
    }
    
    //backup classifier
    public void backupClassifier(){
    	try {
			replaceFile("./resources/backupClassifier","./resources/classifier");
			replaceFile("./resources\\backupTrainSet","./resources/trainSet");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    //reset classifier
    public void resetClassifier(){
    	try {
			replaceFile("./resources/classifier","./resources/backupClassifier");
			replaceFile("./resources/trainSet","./resources/backupTrainSet");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    //replace file
    private void replaceFile(String origin, String replace) throws IOException{
    	//delete if exist
    	File file = new File(origin);
    	file.delete();
    	
    	//copy new one
    	InputStream is = null;
    	OutputStream os = null;
    	try {
    	    is = new FileInputStream(replace);
    	    os = new FileOutputStream(origin);
    	    byte[] buffer = new byte[1024];
    	    int length;
    	    while ((length = is.read(buffer)) > 0) {
    	    	os.write(buffer, 0, length);
    	    }
    	} finally {
    		is.close();
    		os.close();
    	}
    }
    
    
    
/*    public static void main(String[] args){
    	
    	//////random sample training
		EvaluateClassifier importer_r = new EvaluateClassifier();
		InstanceList instances_r = importer_r.readSingleFile(new File(args[1]));
		//InstanceList instances_r = InstanceList.load(new File("instanceList_r"));
		/////////////////////////Classifier classifier_r = importer_r.trainClassifier(instances_r);
		
		// output spare matrix for matlab
		//importer_r.saveVector(instances_r, "vector");
		importer_r.saveInstancesList(instances_r, new File("instanceList_r"));
		InstanceList instances_t = importer_r.readSingleFile(new File(args[3]),instances_r.getPipe());
		importer_r.saveInstancesList(instances_r, new File("instanceList_r"));
		importer_r.saveVector(instances_t, "future_vector");
		//Classifier classifier = importer_r.trainClassifier(instances_r);

		//importer_r.evaluatePatial(instances_t);
		//importer_r.evaluate(classifier, new File(args[3]));
		//importer_r.printLabelings(classifier, new File(args[2]));
        
        System.out.println("finish!");
    	
    }*/
    
    public static void main(String[] args){
    	
    	boolean loadInstance = true;
    	boolean loadClassifier = true;
    	boolean runPredict = true;
    	boolean updateGroundTruth = false;
    	boolean timestampFlag = false;
    	boolean download = true;
    	boolean onlydownload = false;
    	String file_classifier = "./resources/classifier";
    	String file_instanceList = "./resources/instanceList_r";
    	String file_futureData = "./resources/futureData";
    	String file_trainData = "./resources/trainingData";
    	//String file_groundTruth = "result.csv";
    	String file_groundTruth = "./output/result.xml";
    	
    	long timestamp = 0;
    	
    	EvaluateClassifier eval = new EvaluateClassifier();
    	
    	if (args.length!=0){
    		
    		for(int i = 0; i < args.length; i++){
    			
    			if(args[i].equals("-t")){
    				
    				loadClassifier = false; //training with instance list
    				download = false;
    				runPredict = false;
    			}
    			else if(args[i].equals("-nt")){
    				
    				loadClassifier = true; //training with instance list
    				if (i+1<args.length && args[i+1]!=null){
    					
    					if(((String)args[i+1]).charAt(0)!='-'){
    						file_classifier = args[i+1];
    						i++;
    					}
    				}
    			}
    			else if(args[i].equals("-np")){
    				
    				runPredict = false;// do not run predict
    			}
    			else if(args[i].equals("-p")){
    				//run predict on specific file
    				runPredict = true; //predict instance list
    				if (i+1<args.length && args[i+1]!=null){
    					
    					if(((String)args[i+1]).charAt(0)!='-'){
    						file_futureData = args[i+1];
    						i++;
    					}
    				}
    			}
    			else if(args[i].equals("-g")){
    				//update ground truth
    				loadInstance = false;
    		    	loadClassifier = false;
    		    	runPredict = false;
    		    	updateGroundTruth = true;
    		    	if (i+1<args.length && args[i+1]!=null){
    					
    					if(((String)args[i+1]).charAt(0)!='-'){
    						file_groundTruth = args[i+1];
    						i++;
    					}
    				}
    		    	try {
						eval.updateGroundTruth(new File(file_groundTruth));
						System.out.println("update groundtruth finish");
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    		    	return;
    				
    			}
    			else if(args[i].equals("-li")){
    				//load instancelist
    				loadInstance = true; //training with instance list
    				if (i+1<args.length && args[i+1]!=null){
    					
    					if(((String)args[i+1]).charAt(0)!='-'){
    						file_instanceList = args[i+1];
    						i++;
    					}
    				}
    				
    			}
    			else if(args[i].equals("-lm")){
    				//not load instancelist + mallet format file name
    				loadInstance = false; //training with instance list
    				if (i+1<args.length && args[i+1]!=null){
    					
    					if(((String)args[i+1]).charAt(0)!='-'){
    						file_trainData = args[i+1];
    						i++;
    					}
    				}
    				
    			}
    			else if(args[i].equals("-f")){
    				//not load instancelist + mallet format file name
    				loadInstance = false; //training with instance list
    				if (i+1<args.length && args[i+1]!=null){
    					
    					if(((String)args[i+1]).charAt(0)!='-'){
    						String file_input = args[i+1];
    						readTxt.createDataFile(file_input, file_futureData);
    						
    						i++;
    					}
    				}
    				return;
    				
    			}
    			else if(args[i].equals("-u")){
    				//update classifier		
    				InstanceList newDataSet = eval.generateUpdateDataSet();
    				InstanceList trainSet = InstanceList.load(new File("./resources/trainSet"));
    				eval.updateClassifier(trainSet, newDataSet);
    				return;
    				
    			}
    			else if(args[i].equals("-b")){
    				//backup classifier
    				eval.backupClassifier();
    				return;
    				
    			}
    			else if(args[i].equals("-r")){
    				//reset classifier
    				eval.resetClassifier();
    				return;
    				
    			}
    			else if(args[i].equals("-a")){
    				//accept new classifier
    				eval.replaceClassifier();
    				return;
    				
    			}
    			else if(args[i].equals("-d")){
    				//just download from dhnow feed
    				onlydownload = true;
    				
    			}
    			else if(args[i].equals("-nd")){
    				//run classifier without download new item
    				download = false;
    				
    			}
    			else if (args[i].equals("-ts")){
    				
    				if (i+1<args.length && args[i+1]!=null){
    					
    					if(((String)args[i+1]).charAt(0)!='-'){
    						timestamp = Long.parseLong(args[i+1]);
    						timestampFlag = true;
    						i++;
    					}
    				}
    			}
    			else if(args[i].equals("-h")){
    				//print out help
    				System.out.println("training classifier and evaluate it, also output the prediction for future data \n" +
    						"-t: training classifier \n" +
    						"-nt fileName: not train, but load a classifier \n" +
    						"-p fileName: predict future data label \n" +
    						"-np: not predict \n" +
    						"-g fileName: update database with ground truth \n" +
    						"-li fileName: load training instancelist file \n" +
    						"-lm fileName: load training mallet format file name \n" +
    						"-tx fileName: load feed xml input, save it to future data in required format"+
    						"-h: help \n" +
    						"-f fileName: create future data file from preprocessed text file"+
    						"-u: update classifier with incoming data"+
    						"-b: backup classifier"+
    						"-r: reset classifier"+
    						"-a: accept new classifier"+
    						"-ts xxxxxxx: unix time stamp, download all new blogs published since timestamp"+
    						"-d: download from dhnow feed, and save it to future data"+
    						"-nd: run classifier without download"+
    						"if no args, download new data, load instanceList_r, classifier and future data by default,\n output the prediction of future data");
    				return;
    			}
    			else if (args[i].equals("-tx")){
    				
    				if (i+1<args.length && args[i+1]!=null){
    					
    					if(((String)args[i+1]).charAt(0)!='-'){
    						String file_input = args[i+1];
    						readTxt.runReadTxt_(file_input);
    						
    						i++;
    					}
    				}
    				return;
    				
    			}
    			
    		}
    		
    	}
    	
    	try{
    		if(!timestampFlag){
    			BufferedReader in = new BufferedReader(new FileReader("./resources/timestamp"));
    			//BufferedReader in_ = new BufferedReader(new FileReader(in.getPath()));
    			timestamp = Long.parseLong(in.readLine());
    			System.out.println(timestamp);
    			in.close();
    		}
    		
    		if(onlydownload){
    			readTxt.runReadTxt(timestamp);
    			return;
    		}
    		
    		if(download){
    			
    			readTxt.runReadTxt(timestamp);
    		}
    		
    		
        	InstanceList instances_r = null;
        	Classifier classifier_r = null;
        	InstanceList instances_t = null;
	    	//load instanceList
	    	if (loadInstance){
	    		instances_r = InstanceList.load(new File(file_instanceList));
	    	}
	    	else if(!updateGroundTruth){
	    		
	    		instances_r = eval.readSingleFile(new File(file_trainData));
	    		eval.saveInstancesList(instances_r, new File(file_instanceList));
	    	}
	    	
	    	//train classifier
	    	if (loadClassifier){
	    		
	    		classifier_r = eval.loadClassifier(new File(file_classifier));
	    		KernelManager.setCustomKernel(new LinearKernel());
	    	}
	    	else if(!updateGroundTruth){
	    		InstanceList instances_r_ = instances_r.cloneEmpty();
	    		instances_r_.addAll(instances_r);
	    		int initial = 10;
	    		int iteration = 8;
	    		int query = 20;
	    		classifier_r= eval.activeLearningTrainer_static(instances_r_, initial, iteration, query);
	    		eval.evaluateInstanceList(classifier_r, instances_r,"retrain_classifier");
	    	}
	    	
	    	//predict
	    	if(runPredict){
	    		instances_t = eval.readSingleFile(new File(file_futureData),instances_r.getPipe());	
	    		//instances_t = instances_r;
	    		eval.saveInstancesList(instances_r, new File(file_instanceList));
				
				//get the predict labels
				ArrayList<Classification> classArray = classifier_r.classify(instances_t);
				//print out the predict labels with probability
				Collections.sort(classArray,eval.new CustomComparator());
				String date = new SimpleDateFormat("MMddyyyy").format(new Date());
				//save it to csv format and update database
				//File result = new File("result_"+date+".csv");
				//eval.saveCsv(classArray,result);
	    		//save it to XML format and update database
				File result = new File("./output/result_"+date+".xml");
				eval.saveXML(classArray,result);
				
	    	}
	    	
	    	//update groundTruth
	    	if(updateGroundTruth){
	    		return;
	    	}
	        
	        System.out.println("finish!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 		
    	
    }
    
    private class CustomComparator implements Comparator<Classification> {
        @Override
        public int compare(Classification o1, Classification o2) {
        	double o1value = 0;
        	double a =o1.getLabelVector().value(o1.getLabelVector().getLabelAlphabet().lookupLabel(0));
			double b =o1.getLabelVector().value(o1.getLabelVector().getLabelAlphabet().lookupLabel(1));
			if( Math.abs(a)>Math.abs(b)){
				o1value = a;
			}else{
				o1value = b;
			}
        	double o2value = 0;
        	double c =o2.getLabelVector().value(o2.getLabelVector().getLabelAlphabet().lookupLabel(0));
			double d =o2.getLabelVector().value(o2.getLabelVector().getLabelAlphabet().lookupLabel(1));
			if( Math.abs(c)>Math.abs(d)){
				o2value = c;
			}else{
				o2value = d;
			}
        	
        	if (o1value < o2value){
        		return 1;
        	}
        	else if (o1value == o2value){
        		return 0;
        	}
        	else if (o1value >o2value){
        		return -1;
        	}
			return 0;
        }
    }

}


