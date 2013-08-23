*************************************************************
******DH Now filter version 1.5

java program for dhnow editors to filter useful content. 
Based on Active SVM algorithm.
This program is only working with the pressforward project.
A general using version written in Python is under development.
**************************************************************

How to?
First install java 1.6 or later.

Then in teriminal or command line, run 
java -jar recommendSystem.jar
it will make prediction about the new blogs based on the classifier. And the output file would be result_xxxxxxxx.xml.   xxxxxxxx is the current date MMDDYYYY.

The format of result_xxxxxxx.xml :
<?xml version="1.0"?>
<channel>
<item>
<id>4152</id>
<title><![CDATA[Bisson, Casey: Camera frustrations and other first world problems]]></title>
<link>http://maisonbisson.com/?p=16790</link>
<label>recommend</label>
<score>4.3278</score>
<gtruth></gtruth>
</item>
</channel>

title will be quoted. The items are already ranked by scores, a high score means a more confidential item(not the quality of the article, but the topic is more related to DH). 
"recommend" has three values, "recommend","related","not recommend". It is suggested that just review the "recommend" and "related"items.
The amount of "not recommend" articles are huge. And because they are ranked. You can just review these by order.

***************************************************************
update classifier to have better performance
First please set the review result(y or n) to the tag <gtruth> of every item in result_xxxxxxxx.xml. Not everyone, just the ones you've reviewed.

before you update the classifier, you can backup the current classifier by run
java -jar recommendSystem.jar -b

then run 
java -jar recommendSystem.jar -g result_xxxxxxxx.xml
java -jar recommendSystem.jar -u

it will save the evaluate result of the new classifier to
report_old_classifier_xxxxxxxx.xml and
report_update_classifier_xxxxxxxx.xml, xxxxxxxx is date MMddyyyy
formate of that file is:
<?xml version="1.0"?>
<classifier>
<date>MMddyyyy</date>
<description>old_classifier</description>
<Accuracy>0.88</Accuracy>
<F1>0.88</F1>
<Precision>0.88</Precision>
<Recall>0.88</Recall>
<ConfusionMatrix>
Confusion Matrix, row=true, column=predicted  accuracy=0.956
  label   0   1  |total
  0  No 881  28  |909
  1 Yes  16  75  |91
</ConfusionMatrix>
</classifier> 
If you are satisfied with it, run java -jar recommendSystem.jar -a

done!
***************************************************************
if not satisfied with the classifier, you can retrain a new one.

before you update the classifier, you can backup the current classifier by run
java -jar recommendSystem.jar -b

and then run
java -jar recommendSystem.jar -t
The evaluate result of the new classifier will be saved in file
report_retrain_classifier_xxxxxxxx.xml, xxxxxxxx is date MMddyyyy
formate of that file is:
<?xml version="1.0"?>
<classifier>
<date>MMddyyyy</date>
<description>old_classifier</description>
<Accuracy>0.88</Accuracy>
<F1>0.88</F1>
<Precision>0.88</Precision>
<Recall>0.88</Recall>
<ConfusionMatrix>
Confusion Matrix, row=true, column=predicted  accuracy=0.956
  label   0   1  |total
  0  No 881  28  |909
  1 Yes  16  75  |91
</ConfusionMatrix>
</classifier> 

If you are satisfied with it, run java -jar recommendSystem.jar -a

done!
***************************************************************
backup/reset classifier
you can backup your classifier by run
java -jar recommendSystem.jar -b

you can backup your classifier by run
java -jar recommendSystem.jar -r
***************************************************************
Copyright and Acknowlagements:

In this project we use Mallet and libSVM libary. Thanks for their execllent 
job. Mallet is under CPL licence, and libSVM is under "the modified BSD license", 
which compatible with many free software licenses such as GPL.
  McCallum, Andrew Kachites.  "MALLET: A Machine Learning for Language Toolkit."
    http://mallet.cs.umass.edu. 2002.
  Chih-Chung Chang and Chih-Jen Lin, LIBSVM : a library for support vector machines. ACM Transactions on Intelligent Systems and Technology, 2:27:1--27:27, 2011. Software available at http://www.csie.ntu.edu.tw/~cjlin/libsvm

