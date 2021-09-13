# This Dockerfile is to build Docker image to build sphinx documents.
# Run `docker build -t spotbugs-sphinx .` to build docker image,
# and run `docker run -it -v $(pwd):/documents spotbugs-sphinx make html` to generate documents.

FROM sphinxdoc/sphinx:4.1.2

WORKDIR /docs/docs
ADD requirements.txt /tmp
RUN pip3 install --upgrade pip && \
    pip3 install -r /tmp/requirements.txt
