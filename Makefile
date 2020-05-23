VER=1.0.5

.PHONY: all clean doc release

all:
	javac -g -classpath src `find src -name "*.java"`

clean:
	rm -f `find -name "*.class"`
	rm -rf mabravo-$(VER)

release: clean all
	mkdir mabravo-$(VER)
	cd src && jar cf mabravo-$(VER).jar `find -name "*.class"`
	mv src/mabravo-$(VER).jar mabravo-$(VER)
