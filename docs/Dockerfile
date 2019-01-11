# This Dockerfile is to build Docker image to build sphinx documents.
# Run `docker build -t spotbugs-sphinx .` to build docker image,
# and run `docker run -it -v $(pwd):/documents spotbugs-sphinx make html` to generate documents.

FROM ubuntu:xenial

# Necessary to build documents with Python3
# https://click.palletsprojects.com/en/7.x/python3/
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

RUN mkdir documents
RUN apt-get update && \
    apt-get install -y texlive texlive-latex-extra pandoc build-essential curl make python3 && \
    apt-get autoremove && \
    apt-get clean
RUN curl -kL https://bootstrap.pypa.io/get-pip.py | python3

RUN pip3 install sphinx sphinx-intl sphinx_rtd_theme

WORKDIR /documents/docs
VOLUME /documents

CMD ["/bin/bash"]
