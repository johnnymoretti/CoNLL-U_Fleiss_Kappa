library(irr)

data = read.csv("Raters_CSV.tsv",header=TRUE,check.names=FALSE, sep="\t")

# k = kappam.fleiss(data)               # Fleiss' Kappa

k =kappam.fleiss(data, detail=TRUE)  # Fleiss' and category-wise Kappa
#kappam.fleiss(data, exact=TRUE)   # Exact Kappa
#kappam.fleiss(data[,1:4])         # Fleiss' Kappa of raters 1 to 4

print(k)


