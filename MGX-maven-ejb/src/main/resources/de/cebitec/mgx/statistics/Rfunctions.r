
rarefaction<-function(x, subsample=5, plot=TRUE, color=TRUE, error=FALSE, legend=TRUE, symbol=c(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18)) {

  library(vegan)
  library(parallel)

  x <- as.matrix(x)
  y1<-apply(x, 1, sum)
  rare.data<-x                                   

  select<-unique(sort(c((apply(x, 1, sum)), (seq(0,(max(y1)), by=subsample)), recursive=TRUE)))

  storesummary.e<-matrix(data=NA, ncol=length(rare.data[,1]),nrow=length(select))
  rownames(storesummary.e)<-c(select)
  colnames(storesummary.e)<-rownames(x)
  storesummary.se<-matrix(data=NA, ncol=length(rare.data[,1]),nrow=length(select))
  rownames(storesummary.se)<-c(select)
  colnames(storesummary.se)<-rownames(x)

  tmp <- mclapply(1:length(select), function(X) rareStep(x, select, X), mc.cores=48)
  for (i in 1:length(select)) {
    storesummary.e[i,] <- tmp[[i]][1]
    storesummary.se[i,] <- tmp[[i]][2]
  }
                
  storesummary.e<-as.data.frame(storesummary.e)               
  richness.error<<-storesummary.se
                
  for (i in 1:(length(storesummary.e))) {
    storesummary.e[,i]<-ifelse(select>sum(x[i,]), NA, storesummary.e[,i])
  }

  list("richness"= storesummary.e, "SE"=richness.error, "subsample"=select)        
}

rareStep<-function(x, select, i) {
  select.c<-select[i]
  foo<-rarefy(x, select.c, se=T)
  return(c(foo[1,], foo[2,]))
}
