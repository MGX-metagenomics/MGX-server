library('vegan')
library('parallel')
library('BiocGenerics', warn.conflicts=F)
suppressPackageStartupMessages(library('Biobase', warn.conflicts=F))
library('amap', warn.conflicts=F)
library('MASS', warn.conflicts=F)
suppressPackageStartupMessages(library('compositions'))
suppressPackageStartupMessages(library('coda.base'))
suppressPackageStartupMessages(library("tidyverse", warn.conflict=F))

# ggtree
suppressWarnings(suppressMessages(library("ggtree", quietly = T)))
# suppressPackageStartupMessages(library("ggtree", warn.conflict=F))
# suppressMessages(library("ggtree", warn.conflict=F))


library("ape", warn.conflict=F)
library("svglite", warn.conflict=F)

#
# additive zero replacement strategy according to aitchison, 1986
#
aitchisonAdditiveZeroReplacement <- function(vec) {
  D <- length(vec)
  Z <- sum(vec == 0)
  d <- .Machine$double.eps

  if (Z == 0) {
    return(vec)
  }

  for (i in 1:(length(vec))) {
    vec[i] <- ifelse(vec[i]==0, (d*(Z+1)*(D-Z))/D*D, vec[i]-((d*(Z+1)*Z)/D*D))
  }
  return(vec)
}

#aitchisonMultiplicativeZeroReplacement <- function(vec) {
#  D <- length(vec)
#  Z <- sum(vec == 0)
#  c <- sum(vec)
#  d <- .Machine$double.eps

#  if (Z == 0) {
#    return(vec)
#  }

#  ret <- vector(mode="numeric", length=D)
#  for (i in 1:(length(vec))) {
#    ret[i] <- ifelse(vec[i]==0, d, vec[i]*(1-(/c)))
#  }
#  return(ret)
#}

#
# equal to coda.base::dist, but keeps rownames intact
#
aitchisonDist <- function (x, method = "euclidean", ...)
{
    METHODS <- c("aitchison", "euclidean", "maximum", "manhattan",
        "canberra", "binary", "minkowski")
    imethod <- pmatch(method, METHODS)
    .coda = FALSE
    if (!is.na(imethod) & imethod == 1) {
        .coda = TRUE
        y = coordinates(x)
        rownames(y) <- rownames(x)
        x <- y
        method = "euclidean"
    }
    adist = stats::dist(x, method = method, ...)
    if (.coda) {
        attr(adist, "method") = "aitchison"
    }
    adist
}


create_nwk_tree <- function(nwk_string){
  nwk_tree <- read.tree(text = nwk_string)
  pl <- ggtree(nwk_tree,
       branch.length = "none",
       size = 0.35) + geom_rootedge(rootedge = 1, size = 0.25) + geom_tiplab(
         aes(label = nwk_tree),
         as_ylab = TRUE,
         size = 10,
         color   = "black",
         offset = 2,
         align = T,
         linetype = NA,
         hjust = 0.5
       );
  s <- svgstring()
  plot(pl)
  t <- s()
  dev.off()
  pdf(NULL)
  return(t)
}
