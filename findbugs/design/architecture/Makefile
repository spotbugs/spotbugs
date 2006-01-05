TEX_SRCS = architecture.tex
PDFS = $(TEX_SRCS:.tex=.pdf)

%.pdf : %.tex
	pdflatex $*
	pdflatex $*

all : $(PDFS)

depend :
	./mkdep.pl $(TEX_SRCS) > depend.mak

depend.mak :
	touch depend.mak

include depend.mak
