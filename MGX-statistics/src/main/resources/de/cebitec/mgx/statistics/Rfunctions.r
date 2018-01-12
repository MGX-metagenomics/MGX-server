library('parallel')
library('BiocGenerics', warn.conflicts=F)
suppressPackageStartupMessages(library('Biobase', warn.conflicts=F))
library('amap', warn.conflicts=F)
library('MASS', warn.conflicts=F)
library('rtk', warn.conflicts=F)

getAngle <-function(x1, y1, x2, y2) {
        x <- x2 - x1;
        y <- y2 - y1;
        if (x == 0 && y == 0) {
            return(0);
        }
        cos <- x / sqrt(x * x + y * y);
        return((180/pi) * acos(cos));
}

rareCompute <- function(x, rare.data, select) {
  storesummary.e<-matrix(data=NA, ncol=length(rare.data[,1]),nrow=length(select))
  rownames(storesummary.e)<-c(select)
  colnames(storesummary.e)<-rownames(x)
  storesummary.se<-matrix(data=NA, ncol=length(rare.data[,1]),nrow=length(select))
  rownames(storesummary.se)<-c(select)
  colnames(storesummary.se)<-rownames(x)

  rareStep<-function(x, select, i) {
    select.c<-select[i]
    foo<-rarefy(x, select.c, se=T)
    return(c(foo[1,], foo[2,]))
  }

  tmp <- mclapply(1:length(select), function(X) rareStep(x, select, X), mc.cores=5, mc.cleanup=TRUE, mc.silent=TRUE)
  for (i in 1:length(select)) {
    storesummary.e[i,] <- tmp[[i]][1]
    storesummary.se[i,] <- tmp[[i]][2]
  }
  rm(tmp)
                
  storesummary.e<-as.data.frame(storesummary.e)               
                
  for (i in 1:(length(storesummary.e))) {
    storesummary.e[,i]<-ifelse(select>sum(x[i,]), NA, storesummary.e[,i])
  }

  list("richness"= storesummary.e$V1, "subsample"=select)
}

rarefaction<-function(x, subsample=2, symbol=c(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18)) {
  library(lattice)
  library(permute)
  suppressPackageStartupMessages(library(vegan))

  x <- as.matrix(x)
  y1<-apply(x, 1, sum)
  rare.data<-x                                   

  tmpmax <- max(y1)
  select<-unique(sort(c((apply(x, 1, sum)), (seq(0, tmpmax, by=subsample)), recursive=TRUE)))

  ret <- rareCompute(x, rare.data, select)

  additional <- c()
  prev_x <- 1
  prev_y <- 1
  prevAngle <- 45
  for (i in 1:(length(ret$subsample))) {
      cur_x <- ret$subsample[i]
      cur_y <- ret$richness[i]
      angle <- getAngle(prev_x, prev_y, cur_x, cur_y)
      #
      # if angle changes by more than 10 degrees at this point,
      # determine two additional subsample sizes before and
      # one additional subsample size after this datapoint
      #
      if (prevAngle - angle > 10) {
          d <- (cur_x - prev_x) / 3;
          additional[[length(additional)+1]] <- prev_x + d
          additional[[length(additional)+1]] <- prev_x + d + d
          additional[[length(additional)+1]] <- cur_x + (ret$subsample[i] - cur_x)/2
      }
      prev_x <- cur_x
      prev_y <- cur_y
      prevAngle <- angle
  }
  ###print(paste(additional, sep=","))

  retAdd <- rareCompute(x, rare.data, additional)

  # merge ret and retAdd
  for (i in 1:(length(retAdd$subsample))) {
      ss <- retAdd$subsample[i]
      rich <- retAdd$richness[i]
      ret$subsample[[length(ret$subsample)+1]] <- ss
      ret$richness[[length(ret$richness)+1]] <- rich
  }

  ret
}

