
# Build Evosuite-sushi integration in Eclipse

- clone the repository from https://github.com/gdenaro73/evosuite.git

- init git submodules by executing

	git submodule init
	git submodule update

- compile submodules with gradle by executing

	cd sushi-runtime
	gradle build

- import evosuite as "existing maven project" in Eclipse	

- [if needed] import sushi-runtime as "existing Gradle project" in Eclipse
	
- build the shaded jar by executing Maven Build directly in Eclipse with goals: 

	package -DskipTests
	
# Notes to merge change from original evosuite repo

exec pull with option: 

	git pull -Xignore-all-space upstream master