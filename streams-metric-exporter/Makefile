default: package
setup:
	mvn install:install-file -Dfile=lib/com.ibm.streams.management.jmxmp.jar -DgroupId=com.ibm.streams.management -DartifactId=jmxmp -Dversion=4 -Dpackaging=jar
	mvn install:install-file -Dfile=lib/com.ibm.streams.management.mx.jar -DgroupId=com.ibm.streams.management -DartifactId=mx -Dversion=4 -Dpackaging=jar
package: setup
	mvn package
tar: package
	mvn assembly:single

clean:
	mvn clean
