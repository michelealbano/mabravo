VER=1.0.5

.PHONY: all clean

all: clean
	javac -classpath code `find code -name "*.java"`
	mkdir mabravo-$(VER)
	cd code && jar cf mabravo-$(VER).jar `find -name "*.class"`
	mv code/mabravo-$(VER).jar mabravo-$(VER)

clean:
	rm -f `find -name "*.class"`
	rm -rf mabravo-$(VER)
