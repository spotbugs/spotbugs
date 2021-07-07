# Check projects with Spotbugs Github Action

[Github Actions](https://docs.github.com/en/actions)  can be used to automate CI/CD activities.
A series of such action is called a workflow.
This repository contains 2 workflows which can be used to automatically run a Spotbugs Java projects.
[check-projects](check-projects.yml) is responsible for running the analysis.
[check-projects-comment](check-projects-comment.yml) is responsible for publishing the results of the analysis.
The separation of the commenting phase is due to [security reasons](https://securitylab.github.com/research/github-actions-preventing-pwn-requests/).

## Analysis goals

The [check-projects](check-projects.yml) workflow builds a Spotbugs runnable from master (labelled baseline) and the current PR (labelled new).
The unit tests are *not* run in this job on purpose to keep it simple and allow for a fast response time.
After building the baseline and new versions of Spotbugs, an analysis is run on open source projects.
The list of projects currently analyzed:
  - spotbugs
  - matsim-libs
  - jenkins

The workflow analyses these projcts via the baseline and the new versions of Spotbugs and produces a diff of analysis results for each one.
The diff of the analysis results can be used to see if the PR produces new results and/or old results are gone.
Time of analysis is also measured in order to catch performance regressions.

## Installation

These workflows are intended to be installed on a Spotbugs fork.
On the default branch of the repository (main or master by default) the workflow description files should be placed into the `.github/workflows` directory.
Both [check-projects.yml](check-projects.yml) and [check-projects-comment.yml](check-projects-comment.yml) files should be there and must be committed.


## Usage

Once installed you can use the workflows by creating a feature branch in you fork, and creating a pull-request from that branch to your forks master branch.
The outcome of the analysis is seen on the job status of the PR, and the diff of results can be seen as PR comment.
