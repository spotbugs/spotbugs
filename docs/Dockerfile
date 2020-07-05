# This Dockerfile is to build Docker image to build sphinx documents.
# Run `docker build -t spotbugs-sphinx .` to build docker image,
# and run `docker run -it -v $(pwd):/documents spotbugs-sphinx make html` to generate documents.

FROM sphinxdoc/sphinx:3.1.1

WORKDIR /docs/docs
ADD requirements.txt /tmp
RUN pip3 install -r /tmp/requirements.txt
