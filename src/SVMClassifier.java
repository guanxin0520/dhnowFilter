
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.uwo.csd.ai.nlp.common.SparseVector;
import ca.uwo.csd.ai.nlp.libsvm.svm;
import ca.uwo.csd.ai.nlp.libsvm.svm_model;
import ca.uwo.csd.ai.nlp.libsvm.svm_node;
import ca.uwo.csd.ai.nlp.libsvm.ex.SVMPredictor;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelVector;

/**
 * A wrapper for LibSVM classifier.
 * @author Syeed Ibn Faiz
 */
public class SVMClassifier extends Classifier {

    private svm_model model;
    //private Map<Label, Double> mltLabel2svmLabel;       //mapping from Mallet to SVM label
    private Map<String, Double> mltLabel2svmLabel;       //mapping from Mallet to SVM label
    private Map<Double, String> svmLabel2mltLabel;       //mapping from SVM label to Mallet Label
    private int[] svmIndex2mltIndex;                    //mapping from SVM Label indices (svm.label) to Mallet Label indices (targetLabelAlphabet)
    private boolean predictProbability;                 //whether probability is predicted by SVM
    private boolean reverseOrder;
    
    public SVMClassifier(svm_model model, Map<String, Double> mLabel2sLabel, Pipe instancePipe, boolean predictProbability) {
        super(instancePipe);
        this.model = model;
        this.mltLabel2svmLabel = mLabel2sLabel;
        this.predictProbability = predictProbability;
        double temp = this.mltLabel2svmLabel.get("Yes");
        this.reverseOrder = false;
        if(temp == 2.0){
        	this.reverseOrder = true;
        }
        init();        
    }

    private void init() {
        svmLabel2mltLabel = new HashMap<Double, String>();
        for (Entry<String, Double> entry : mltLabel2svmLabel.entrySet()) {
            svmLabel2mltLabel.put(entry.getValue(), entry.getKey());
        }

        svmIndex2mltIndex = new int[model.nr_class + 1];
        int[] sLabels = model.label;
        LabelAlphabet labelAlphabet = getLabelAlphabet();
        for (int sIndex = 0; sIndex < sLabels.length; sIndex++) {
            double sLabel = sLabels[sIndex];
            String mLabel = svmLabel2mltLabel.get(sLabel * 1.0);
            int mIndex = labelAlphabet.lookupIndex(mLabel.toString(), false);
            svmIndex2mltIndex[sIndex] = mIndex;
        }
    }

    @Override
    public Classification classify(Instance instance) {
        SparseVector vector = SVMClassifierTrainer.getVector(instance);
        double[] scores = new double[model.nr_class];
        double sLabel = mltLabel2svmLabel.get(instance.getTarget().toString());//(getLabelAlphabet().lookupLabel(instance.getTarget().toString()));
        double p = SVMPredictor.predictProbability(new ca.uwo.csd.ai.nlp.libsvm.ex.Instance(sLabel, vector), model, scores);
        
        //if SVM is not predicting probability then assign a score of 1.0 to the best class(p)
        //and 0.0 to the other classes
        if (!predictProbability) {
            String label = svmLabel2mltLabel.get(p);
            int index = getLabelAlphabet().lookupIndex(label.toString(), false);
            scores[index] = 1.0;
        } else {
            rearrangeScores(scores);
        }        
        Classification classification = new Classification(instance, this,
                new LabelVector(getLabelAlphabet(), scores));

        return classification;
    }
    
    @Override
    public ArrayList<Classification> classify(InstanceList instanceList) {
    	ArrayList<Classification> classification = new ArrayList<Classification>();
    	for(int i = 0; i < instanceList.size();i++){
    		Instance instance = instanceList.get(i);
	        SparseVector vector = SVMClassifierTrainer.getVector(instance);
	        double[] scores = new double[model.nr_class];
	        double sLabel = mltLabel2svmLabel.get(instance.getTarget().toString());//get(getLabelAlphabet().lookupLabel(instance.getTarget().toString()));
	        double decisionValue[] = new double[1];
	        
	        //if SVM is not predicting probability then assign a score of 1.0 to the best class(p)
	        //and 0.0 to the other classes
	        if (!predictProbability) {
	        	
	        	double predictClass = svm.svm_predict_values(model, new svm_node(new ca.uwo.csd.ai.nlp.libsvm.ex.Instance(sLabel, vector).getData()), decisionValue);
	            String label = svmLabel2mltLabel.get(predictClass);
	            
	            int index = getLabelAlphabet().lookupIndex(label.toString(), false);
	            if(this.reverseOrder){
	            	scores[index] = -decisionValue[0];
	            }
	            else{
	            	scores[index] = decisionValue[0];
	            }
	        } else {
	        	double p = SVMPredictor.predictProbability(new ca.uwo.csd.ai.nlp.libsvm.ex.Instance(sLabel, vector), model, scores);
	            rearrangeScores(scores);
	        }        
	        classification.add(new Classification(instance, this,
	                new LabelVector(getLabelAlphabet(), scores)));
    	}
        return classification;
    }
    
    public double[] getRho(){
    	
    	return model.rho;
    }
    

    /**
     * SVM model's label indices differ from labelAlphabet's label indices, which is why we
     * need to rearrange the score vector returned by the SVM model.
     * @param scores 
     */
    private void rearrangeScores(double[] scores) {
        for (int i = 0; i < scores.length; i++) {
            int mIndex = svmIndex2mltIndex[i];
            double tmp = scores[i];
            scores[i] = scores[mIndex];
            scores[mIndex] = tmp;
        }
    }
}
