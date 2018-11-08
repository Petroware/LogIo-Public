# Log I/O - Library for accessing well log files

Log I/O is a library for reading and writing well log files.

As of Q4/2018 Log I/O supports DLIS, LIS, LAS 2.0, LAS 3.0, BIT, XTF, ASC,
SPWLA, CSV, and JSON Well Log Format. Log I/O wraps the complexity of these
formats in a clean, complete, well documented, efficient and extremely simple
to use programming API.

<img hspace="100" src="https://petroware.no/images/LogIoBox.250.png">

The open source version of Log I/O available here is the Java accessor for the
JSON Well Log Format.

Log I/O web page: https://petroware.no/logio.html


## Setup

Capture the UoM code to local disk by:

```
$ git clone https://github.com/Petroware/LogIo.git
```


## Dependencies

The JSON Well Log Format accessor depends on the JSON API specification
and an implementation of this. We are using the official one from Oracle,
but these can probably be replaced if required:

```
lib/javax.json-api-1.1.3.jar
lib/javax.json-1.1.3.jar
```


## API Documentation

Java: https://petroware.no/logio/javadoc/index.html

.Net: https://petroware.no/logio/doxygen/index.html


## Programming examples

### Write

An example for creating a JSON Well Log file from scratch is shown below:

```java
import no.petroware.logio.json.JsonCurve;
import no.petroware.logio.json.JsonFile;
import no.petroware.logio.json.JsonFileWriter;

:

// Create an empty JSON file instance
JsonFile jsonFile = new JsonFile();

// Populate with metadata
jsonFile.setName("EcoScope Data");
jsonFile.setWell("35/12-6S");
jsonFile.setField("Ekofisk");
:

// Create curves
JsonCurve c1 = new JsonCurve("MD", "Measured depth", "length", "m", Class.double, 1);
jsonFile.addCurve(c1);

JsonCurve c2 = new JsonCurve("RES", "Resistivity", "electrical resistivity", "ohm metre", Class.double, 1);
jsonFile.addCurve(c2);

// Add curve data
c1.addValue(1000.0);
c1.addValue(1001.0);
c1.addValue(1002.0);
:

c2.addValue(127.3);
c2.addValue(92.16);
c2.addValue(null);
:

// Write to file
JsonFileWriter fileWriter = new JsonFileWriter(new File("path/to/file.JSON", true, 2);
fileWriter.write(jsonFile);
```

This will create the following file:

```txt
{
     "log": {
       "metadata": {
         "name": "EcoScope Data" ,
         "well": "35/12-6S",
         "field": "Ekofisk",
         "startIndex": 1000.0,
         "endIndex": 1349.0,
         "step": 1.0
       },
       "curves": [
         {
           "name": "MD",
           "description": "Measured depth",
           "quantity": "length",
           "unit": "m",
           "valueType": "float",
           "dimensions": 1
         },
         {
           "name": "A40H",
           "description": "Attenuation resistivity 40 inch",
           "quantity": "electrical resistivity",
           "unit": "ohm metre",
           "valueType": "float",
           "dimensions": 1
         }
       ]
       "data": [
         [1000.0, 127.300],
         [1001.0,  92.160],
         [1002.0,    null],
         :
         :
         [1349.0, 112.871]
       ]
     }
   }
```

### Read

Reading a JSON Well Log file is shown below:

```java
import no.petroware.logio.json.JsonFile;
import no.petroware.logio.json.JsonFileReader;

:

// Create an empty JSON file instance
JsonFileReader fileReader = new JsonFileReader(new File("path/to/file.JSON"));
List<JsonFile> jsonFiles = fileReader.read(true, false, null);
```

From this point navigate the JsonFile instances to get curve and metadata.


# About Petroware

Petroware AS is a software company within the data management, data analytics,
petrophysics, geology and reservoir engineering domains.

Petroware creates highly advanced software components and end-user products that
acts as a research platform within software architecture and scalability, system design,
parallelism and multi-threading, user experience (UX) and usability analysis as well
as development methodologies and techniques.

**Petroware AS**<br>
Stavanger - Norway<br>
[https://petroware.no](https://petroware.no)<br>
info@petroware.no
