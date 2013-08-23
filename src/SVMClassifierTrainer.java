
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.uwo.csd.ai.nlp.common.SparseVector;
import ca.uwo.csd.ai.nlp.kernel.CustomKernel;
import ca.uwo.csd.ai.nlp.kernel.KernelManager;
import ca.uwo.csd.ai.nlp.libsvm.svm_model;
import ca.uwo.csd.ai.nlp.libsvm.svm_parameter;
import ca.uwo.csd.ai.nlp.libsvm.ex.SVMTrainer;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;

/**
 * Class used to generate an SVMClassifier.
 * @author Syeed Ibn Faiz
 */
public class SVMClassifierTrainer extends ClassifierTrainer<SVMClassifier> {
    private SVMClassifier classifier;
    //private Map<Label, Double> mLabel2sLabel;
    private Map<String, Double> mLabel2sLabel;
    private CustomKernel kernel;
    private int numClasses;
    private boolean predictProbability;
    private svm_parameter param;
    
    public SVMClassifierTrainer(CustomKernel kernel) {
        this(kernel, false);        
    }    
    
    public SVMClassifierTrainer(CustomKernel kernel, boolean predictProbability) {
        super();
        //mLabel2sLabel = new HashMap<Label, Double>();
        mLabel2sLabel = new HashMap<String, Double>();
        this.kernel = kernel;
        this.predictProbability = predictProbability;
        init();
    }    
    
    private void init() {        
        param = new svm_parameter();
        if (predictProbability) {
            param.probability = 1;
        }
    }        

    public svm_parameter getParam() {
        return param;
    }

    public void setParam(svm_parameter param) {
        this.param = param;
    }

    public CustomKernel getKernel() {
        return kernel;
    }

    public void setKernel(CustomKernel kernel) {
        this.kernel = kernel;
    }
    
    
    @Override
    public SVMClassifier getClassifier() {
        return classifier;
    }

    @Override
    public SVMClassifier train(InstanceList trainingSet) {
        cleanUp();        
        KernelManager.setCustomKernel(kernel);
        svm_model model = SVMTrainer.train(getSVMInstances(trainingSet), param);
        classifier = new SVMClassifier(model, mLabel2sLabel, trainingSet.getPipe(), predictProbability);
        return classifier;
    }
    
    private void cleanUp() {
        mLabel2sLabel.clear();
        numClasses = 0;
    }
    
    private List<ca.uwo.csd.ai.nlp.libsvm.ex.Instance> getSVMInstances(InstanceList instanceList) {
        List<ca.uwo.csd.ai.nlp.libsvm.ex.Instance> list = new ArrayList<ca.uwo.csd.ai.nlp.libsvm.ex.Instance>();
        for (Instance instance : instanceList) {
            SparseVector vector = getVector(instance);
            //list.add(new ca.uwo.csd.ai.nlp.libsvm.ex.Instance(getLabel((Label) instance.getTarget()), vector));    
            list.add(new ca.uwo.csd.ai.nlp.libsvm.ex.Instance(getLabel(instance.getTarget().toString()), vector)); 
        }
        return list;
    }
    
/*    private double getLabel(Label target) {
        Double label = mLabel2sLabel.get(target);
        if (label == null) {         
            numClasses++;
            label = 1.0 * numClasses;            
            mLabel2sLabel.put(target, label);
        }
        return label;
    }*/
    
    private double getLabel(String target) {
        Double label = mLabel2sLabel.get(target);
        if (label == null) {         
            numClasses++;
            label = 1.0 * numClasses;            
            mLabel2sLabel.put(target, label);
        }
        return label;
    }
    
    static SparseVector getVector(Instance instance) {
        FeatureVector fv = (FeatureVector) instance.getData();
        int[] indices = fv.getIndices();
        double[] values = fv.getValues();
        SparseVector vector = new SparseVector();
        for (int i = 0; i < indices.length; i++) {
            vector.add(indices[i], values[i]);
        }
        vector.sortByIndices();
        return vector;
    }
    
    public List<ca.uwo.csd.ai.nlp.libsvm.ex.Instance> getSVMSparesInstances(InstanceList instanceList){
    	
    	List<ca.uwo.csd.ai.nlp.libsvm.ex.Instance> list = getSVMInstances(instanceList);
    	return list;
    }
}
