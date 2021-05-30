# Compiler of the Java-- language to Java Bytecodes

**Group 1F**

| Name             | Number    | E-Mail             |
| ---------------- | --------- | ------------------ |
| Beatriz Mendes    | 201806551 |up201806551@fe.up.pt|
| Henrique Pereira  | 201806538 |up201806538@fe.up.pt|
| Mariana Truta    | 201806543 |up201806543@fe.up.pt|
| Rita Peixoto    | 201806257 |up201806257@fe.up.pt|


### Project
This project requires the [installation of Gradle](https://gradle.org/install/)

## Compile

To compile the program, run ``gradle build``.

### Run

To run you have two options: Run the ``.class`` files or run the JAR.

### Run ``.class``

To run the ``.class`` files, do the following:

```cmd
java -cp "./build/classes/java/main/" <class_name> <arguments>
```

Where ``<class_name>`` is the name of the class you want to run and ``<arguments>`` are the arguments to be passed to ``main()``.

### Run ``.jar``

To run the JAR, do the following command:

```cmd
java -jar <jar filename> <arguments>
```

Where ``<jar filename>`` is the name of the JAR file that has been copied to the root folder, and ``<arguments>`` are the arguments to be passed to ``main()``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``build/reports/tests/test/index.html``.


## Syntatic Analysis

## Semantic Analysis

## OLLIR

## Jasmin


## Optimizations

## Extra implementations




## Future work

