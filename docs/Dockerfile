# This Dockerfile is to build Docker image to build sphinx documents.
# Run `docker build -t spotbugs-sphinx .` to build docker image,
# and run `docker run -it -v $(pwd):/documents spotbugs-sphinx make html` to generate documents.

FROM ubuntu:xenial

RUN mkdir documents
RUN apt-get update
RUN apt-get install -y python-sphinx texlive texlive-latex-extra pandoc build-essential curl
RUN curl -kL https://bootstrap.pypa.io/get-pip.py | python

RUN pip install sphinx-intl

WORKDIR /documents/docs
VOLUME /documents

CMD ["/bin/bash"]
