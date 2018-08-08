# SpotBugs manual

[![Documentation Status](https://readthedocs.org/projects/spotbugs/badge/?version=latest)](http://spotbugs.readthedocs.io/en/latest/?badge=latest)

This repository hosts official SpotBugs manual built by [Sphinx](http://www.sphinx-doc.org/en/stable/).

## How to text

We use textlint to lint `.rst` files. To run lint, execute following commands:

```sh
$ nvm use
$ virtualenv -p python3 env
$ source env/bin/activate
(env) $ pip install -r requirements.txt
(env) $ npm install
(env) $ npm run lint
```

## How to build

We provide a `Dockerfile` and a script file to build documents.
After installation of `docker 1.13.1` or later, simply kick `./build.sh` then it will build Docker image and run commands in it. You can find generated HTML pages under `.build/html` directory.

## How to deploy changes to manual site

To deploy changes to manual site, just merge your changes to `master` branch then it will be deployed to [latest page](http://spotbugs.readthedocs.io/en/latest/).

## How to configure Read the Docs

When you need maintainer access to Read the Docs, please contact with @KengoTODA.

### How to add supported languages

For each supported language, We need independent ReadTheDocs project like below:

* [spotbugs](https://readthedocs.org/projects/spotbugs/)
* [spotbugs-ja](https://readthedocs.org/projects/spotbugs-ja/)

Please create similar project for new language, and configure [translations setting for spotbugs project](https://readthedocs.org/dashboard/spotbugs/translations/). For detail, visit [official document](http://docs.readthedocs.io/en/latest/localization.html#project-with-multiple-translations).

### How to add versions

When we want to add active versions for documents, visit [version page](https://readthedocs.org/projects/spotbugs/versions/). For detail, visit [official document](http://docs.readthedocs.io/en/latest/versions.html).
