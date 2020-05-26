# Subgroup Discovery

## Motivation
My goal was to find profitable strategy for selecting stocks to buy (add to portfolio) or sell. I was certain that the most obvious algorithmic methods of picking stocks must have been already tested many times (linear regression, deep learning models, k-nearest neighbours and clustering algorithms - usually used on time series of stock prices). So instead of trying to develop something meaningful using methods tried by many people before me already, I decided to try some older, partly-forgotten approaches - and apply them to fundamental financial data today. Subgroup discovery was suggested to me as a project idea by my project supervisor.

## Implementation
I implemented two subgroup discovery algorithms, both most representative of their general category. CN2-SD (Nada Lavrač et al., 2004 ) - a modification of CN2 classification algorithm (Clark and Niblett, 1989.) and SDIGA ( del Jesus et al., 2007) which utilised evolutionary heuristic.

The problem was that both papers introducing these algorithms were flawed - I was not able to reproduce results of CN2-SD, the formula described in the paper to verify significance of rule didn't yield the expected results described in the paper. The identified subgroups were clearly meaningful but the quality measures I implemented didn't reflect what the authors supposedly measured. This also meant that the algorithm wouldn't really terminate, the rule significance was always pretty high (high enough for 99% likelikhood ratio significance) so it would find hundreds of rules. I had to introduce my own constraint, the algorithm would keep looking for rules only as long as they didn't have less than 90% modified weighted relative accuracy of the first rule found (mWRAcc would go down as the same records were covered by more rules in subsequent iterations).

SDIGA paper used CN2-SD for comparison of their performance. Sadly it seems that they didn't produce the CN2-SD results themselves - they simply copied them from the CN2-SD paper. They were identical and nowhere did they bother explain what went on with the significance testing. The paper also contained an error their local improvement search which could in some cases result in infinite loops.

Of course before I began my work I tried to find existing implementations. The CN2-SD paper said that it was implemented in the WEKA data mining environment. I couldn't find it there. I could find it, however, in the KEEL project but it was 4 years since the project was updated (and a decade since the algorithm was updated) and I couldn't get it working - it always resulted in Java errors (I tried it with numerous Java versions, no difference). SDIGA paper didn't suggest where their algorithm design was implemented but I found it in KEEL too. Of course I couldn't use it either. It was also implemented in SDEFSR package for R language. I tried this implementation, set all parameters according to the authors of the SDIGA paper, used the same dataset and couldn't reproduce the results. After analyzing the R code I found that the R implementation used different genetic operators from those found in KEEL. In KEEL I wasn't even able to comprehend what the authors of the code tried to achieve with their obscure mutation operator. Nevertheless, after taking some semantic inspiration from their approach I managed to produce results quite similar to those described in the SDIGA paper. The genetic operator, however, was VERY VERY VERY different from that described in the SDIGA paper.

## Input format
Dataset for my implementation is a csv document. First line of the document is a comma separated list of indicators whether the column is a numerical variable (n), categorical (c), target variable (t) or if the column should be ignored (i). Second line is a comma separated list of column names and from third line on it contains individual records - one per line.

## Limitations
CN2-SD algorithm works only with categorical variables. Therefore discretization must be done to the data before it's fed to my implementation.

And while my SDIGA algorithm implements an on-the-fly discretization (which is necessary for definiton of fuzzy partitions), it can't cope with variables that aren't numerical and ordered.

## Results
Both algorithms produce results very similar to those of described in the papers of their authors (except for the significance testing). 

Applied to fundamental financial data of US companies over the past 20 years I managed to find 3 interesting subgroups. Before I could apply subgroup discovery I needed to discretize the data. It wasn't simple as the dataset was sparse. I used equally sized bins, equally sized intervals and in the end also k-means clustering. All of these discretization methods resulted in sparse datasets. The least sparse was k-means clustering with 10 clusters.

SDIGA didn't yield any results. It didn't manage to find any subgroups. This could be best explaiend by the fact that there are more than 100 variables (columns) in the dataset which are already sparse. Strategy of SDIGA is to randomize subgroup selectors. But out of all combinations of the selectors, only extremely small number of them define non-empty subgroups. Together with the fact that the dataset was already nearly 70% sparse this unfortunate result could be expected. Curse of dimensionality.

CN2-SD performed much better since it builds subgroup definitions iteratively, starting with only 1 selector - in contrast with SDIGA which has probability to use all selectors at any iteration.

I classified records of fundamental data in the dataset into 3 classes: stocks where returns one year in the future were within 1 standard deviation of the S&P index (AVERAGE), those whose returns were above 1 standard deviation (LARGE) and those where they were below 1 standard deviation (SMALL). Distribution of these classes were 30% SMALL, 45% AVERAGE and 25% LARGE.

CN2-SD managed to find subgroups that predicted **SMALL with 35% confidence** (5% increase over the default), **LARGE with 29% confidence** (4% increase) and **AVERAGE with 63% confidence** (18% increase). These results were confirmed using 5-stratified-fold cross validation and found subgroups had 99% statistical significance (likelihood ratio test).

## Acknowledgements
* these three datasets (balance, breast-w and diabetes) were downloaded from the UCI machine learning repository
* due to licence issues I am unable to provide the fundamental financial dataset I used to produce my results
