#!/bin/bash -xe

docker build -t spotbugs-sphinx .
rm -rf .build

# extract messages from base document (en) to .pot files
docker run -it -v $(pwd)/..:/docs spotbugs-sphinx make gettext

# build .po files by new .pot files
docker run -it -v $(pwd)/..:/docs spotbugs-sphinx sphinx-intl update -p .build/locale -l ja
docker run -it -v $(pwd)/..:/docs spotbugs-sphinx sphinx-intl update -p .build/locale -l pt_BR

# build html files
docker run -it -v $(pwd)/..:/docs spotbugs-sphinx make html
