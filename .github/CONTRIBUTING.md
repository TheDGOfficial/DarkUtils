# Contributing to this repository

Step by step:

* First, fork and clone this project.
    * Second, create a branch for your changes. (e.g: "fix-null-pointer-exception")
      This can be done easily using "git switch -c fix-null-pointer-exception". It will create a new branch based on the current branch
      and
      switch to it. If you are not on the main branch, you can switch to it using "git checkout main".

    * Then, make changes to the created branch via commits. This can be done either using some IDE, Git GUI or via
      commandline using "git commit -m \<commit message>".

    * Finally, open a pull request for your branch to be merged. You can do this from GitHub page of this project. Just
      be sure you've pushed the changes to your fork and refresh the page. There should be an open a pull request button
      if you did all the steps correctly.

You should fill the pull request template before publishing a pull request. We will review your pull request and merge
into main or another target branch if we've found your changes great.

Don't be fear, we are all kind & formal here. Just open an issue if you don't know how to fix it. If you know how to fix
it, Then just follow the above guidelines and open a pull request. If your pull request is merged, you are now a
Contributor. Congratulations!

**Also read
the <a href="https://github.com/TheDGOfficial/DarkUtils/blob/main/.github/PROJECT_PREFERENCES.md">
project
preferences</a> before making any contribution!**

# Cloning, building and running the project

Step by step:

* Clone this (or your fork's) repository via your IDE or command-line. (git clone https://github.com/<organization or
  user\>/\<repository/project name\>.git)
* Run ./gradlew build from your terminal to build the JAR for the project. Copy the generated JAR file at build/libs/DarkUtils-version.jar to your mods folder (make sure to get the jar that **does not** end with -sources.jar - that's the source code, not the binary). You should be able to run your game normally afterwards to test your changes in the mod.

