# Processor project
In this project, which produced by Strogonov Andrej and Gopenko Kirill we research and develop e2e process of
generating topology of SoC processors by FPGA Architecture
<!--Writerside adds this topic when you create a new documentation project.
You can use it as a sandbox to play with Writerside features, and remove it from the TOC when you don't need it anymore.-->
## Overview description
Project has tree structure from src. It has 2 modules for code (src/main/scala) and for testing (modeling):
* src/main/scala
* src/test/scala 
For correctly configuration we use sbt system for build, test and deploy application.

## Installation guide
In now stage project has developer environment. How to install in your personal computer on Windows:
* Install IntellijIdea Community Edition
* Install Scala plugin (if you don't install in previosly step)
* For correctly configuration see build.sbt and project/plugins.sbt file