*************************************************************
##DH Now filter version 1.5

Java program for dhnow editors to filter useful content.
Based on Active SVM algorithm.
This program is only working with the pressforward project.
A general using version written in Python is under development.
**************************************************************

###How to?
First install java 1.6 or later.

 Then in teriminal or command line, run<br/>
 java -jar dhnowFilter.jar<br/>
 It will make prediction about the new blogs based on the classifier. And the output file would be result_xxxxxxxx.xml.   xxxxxxxx is the current date MMDDYYYY. The output file will be stored in output folder

The format of result_xxxxxxx.xml :
<pre>
  &lt;?xml version="1.0"?&gt;
  &lt;channel&gt;
  &lt;item&gt;
      &lt;id&gt;4152&lt;/id&gt;
      &lt;title&gt;&lt;![CDATA[Bisson, Casey: Camera frustrations and other first world problems]]&gt;&lt;/title&gt;
      &lt;link&gt;&lt;![CDATA[http://maisonbisson.com/?p=16790]]&gt;&lt;/link&gt;
      &lt;label&gt;recommend&lt;/label&gt;
      &lt;score&gt;4.3278&lt;/score&gt;
      &lt;gtruth&gt;/gtruth&gt;
    &lt;/item&gt;
  &lt;/channel&gt;
</pre>
"title" will be quoted. The items are already ranked by scores, a high score means a more confidential item(not the quality of the article, but the topic is more related to DH). 
"recommend" has three values, "recommend","related","not recommend". It is suggested that just review the "recommend" and "related"items.
The amount of "not recommend" articles are huge. And because they are ranked. You can just review these by order.

***************************************************************
###Update classifier to have better performance
First please set the review result(y or n) to the tag <gtruth> of every item in result_xxxxxxxx.xml. Not everyone, just the ones you've reviewed.

Before you update the classifier, you can backup the current classifier by run
<pre>
java -jar dhnowFilter.jar -b
</pre>
Then run
<pre>
java -jar dhnowFilter.jar -g ./output/result_xxxxxxxx.xml <br/>
java -jar dhnowFilter.jar -u
</pre>
it will save the evaluate result of the new classifier to
report_old_classifier_xxxxxxxx.xml and
report_update_classifier_xxxxxxxx.xml, xxxxxxxx is date MMddyyyy.
These files are stored in output folder.
Formate of that file is:

<pre>
&lt;?xml version="1.0"?&gt;
&lt;classifier&gt;
  &lt;date&gt;MMddyyyy&lt;/date&gt;
  &lt;description&gt;old_classifier&lt;/description&gt;
  &lt;Accuracy&gt;0.88&lt;/Accuracy&gt;
  &lt;F1&gt;0.88&lt;/F1&gt;
  &lt;Precision&gt;0.88&lt;/Precision&gt;
  &lt;Recall&gt;0.88&lt;/Recall&gt;
  &lt;ConfusionMatrix&gt;
    Confusion Matrix, row=true, column=predicted  accuracy=0.956
      label   0   1  |total
      0  No 881  28  |909
      1 Yes  16  75  |91
  &lt;/ConfusionMatrix&gt;
&lt;/classifier&gt;
</pre>
If you are satisfied with it, run 
<pre>
 java -jar dhnowFilter.jar -a
</pre>
done!
***************************************************************
###Retrain classifier
if not satisfied with the classifier, you can retrain a new one.

before you update the classifier, you can backup the current classifier by run
<pre>
java -jar dhnowFilter.jar -b
java -jar dhnowFilter.jar -t
</pre>
The evaluate result of the new classifier will be saved in file
report_retrain_classifier_xxxxxxxx.xml, xxxxxxxx is date MMddyyyy
formate of that file is:

<pre>
&lt;?xml version="1.0"?&gt;
&lt;classifier&gt;
  &lt;date&gt;MMddyyyy&lt;/date&gt;
  &lt;description&gt;retrain_classifier&lt;/description&gt;
  &lt;Accuracy&gt;0.88&lt;/Accuracy&gt;
  &lt;F1&gt;0.88&lt;/F1&gt;
  &lt;Precision&gt;0.88&lt;/Precision&gt;
  &lt;Recall&gt;0.88&lt;/Recall&gt;
  &lt;ConfusionMatrix&gt;
    Confusion Matrix, row=true, column=predicted  accuracy=0.956
      label   0   1  |total
      0  No 881  28  |909
      1 Yes  16  75  |91
  &lt;/ConfusionMatrix&gt;
&lt;/classifier&gt;
</pre>

If you are satisfied with it, run 
<pre>
java -jar dhnowFilter.jar -a
</pre>

done!
***************************************************************
###Backup/Reset classifier
you can backup your classifier by run
<pre>
java -jar dhnowFilter.jar -b
</pre>

you can backup your classifier by run
<pre>
java -jar dhnowFilter.jar -r
</pre>
***************************************************************
###Copyright and Acknowlagements:

In this project we use Mallet and libSVM libary. Thanks for their execllent 
job. Mallet is under CPL licence, and libSVM is under "the modified BSD license", 
which compatible with many free software licenses such as GPL. We also use 
Mallet-LibSVM, a library for directly using LibSVM from Mallet.

  McCallum, Andrew Kachites.  "MALLET: A Machine Learning for Language Toolkit."
    http://mallet.cs.umass.edu. 2002.
    
  Chih-Chung Chang and Chih-Jen Lin, LIBSVM : a library for support vector machines. ACM Transactions on Intelligent Systems and Technology, 2:27:1--27:27, 2011. Software available at http://www.csie.ntu.edu.tw/~cjlin/libsvm

  Mellet-LibSVM: https://github.com/syeedibnfaiz/Mallet-LibSVM
