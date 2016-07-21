A few times I've discussed with engineers how to make reproducible jars.
Given the same checkout of source, one would expect the resulting JAR 
to be the same assuming there is nothing funny going on.

Maven puts a date in the pom.properties file it ships in META-INF.
Gradle doesn't do this.

However, even with gradle the SHA1 of the jar changes build over build.
Ignoring signing  (Which this code will likely break).  The root of the
problem lies within the zip format.  Zip stores last modified and access
times inside the archive.

This tool will set the last modified and access times to 
12/31/1969 00:00:00, allowing one to compare the sha1 of 2 jars and 
determine they are the same.

