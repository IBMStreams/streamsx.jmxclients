default: package
setup:
	mvn install:install-file -Dfile=lib/v5/com.ibm.streams.management.jmxmp.jar -DgroupId=com.ibm.streams.management -DartifactId=jmxmp -Dversion=5 -Dpackaging=jar
	mvn install:install-file -Dfile=lib/v5/com.ibm.streams.management.mx.jar -DgroupId=com.ibm.streams.management -DartifactId=mx -Dversion=5 -Dpackaging=jar
package: setup
	mvn package
tar: package
	mvn assembly:single

clean:
	mvn clean
