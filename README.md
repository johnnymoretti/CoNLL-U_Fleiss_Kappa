# CoNLL-U_Fleiss_Kappa

#### Requirements :
- Java 1.8
- Maven
- R and Rscript

#### Setup :

In your R console type `install.packages("irr")` to install 'irr' package.

To compile the script type the following commands:
```
git clone https://github.com/johnnymoretti/CoNLL-U_Fleiss_Kappa.git
cd CoNLL-U_Fleiss_Kappa
mvn package
```

#### Usage:
After the setup procedure you can use the script using this command:
``
java -jar target/CoNLL-U_Fleiss_irr.jar -m POS|EDGES|DEPREL <conllu file rate 1> <conllu file rate 2> ...
``
The 'mode' parameter specify the ConLL-U field used in irr evaluation. It's possible to choose among POS,EDGES and DEPREL. 
