# This Dockerfile is to build Docker image to build sphinx documents.
# Run `docker build -t spotbugs-sphinx .` to build docker image,
# and run `docker run -it -v $(pwd):/documents spotbugs-sphinx make html` to generate documents.

FROM sphinxdoc/sphinx:7.1.2

WORKDIR /docs/docs
RUN pip3 install --upgrade pip

ARG USER=sphinx
RUN mkdir /home/$USER && \
    useradd --no-log-init $USER && \
    chown $USER:$USER -R /home/$USER

USER $USER
ENV PATH "/home/$USER/.local/bin:$PATH"

ADD requirements.txt /tmp
RUN pip3 install -r /tmp/requirements.txt
