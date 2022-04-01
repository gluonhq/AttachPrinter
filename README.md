# Attach Extended Printer service #

This is an extension to Gluon [Attach](http://gluonhq.com/products/mobile/attach/), the component that addresses the integration with low-level platform APIs in an end-to-end Java Mobile solution, providing Bluetooth printer services on Android.

## Building Attach Extended Printer ##

### Requisites ###

These are the requisites:

* A recent version of [JDK 11](http://jdk.java.net/11/) or superior
* Gradle 6.0 or superior. 
* Attach Util 4.0.12 or superior

To build the Android services:

* Android SDK and Android NDK (The GluonFX plugin installs both when building for Android)

### Attach Documentation ###

Read about Attach: [Device Interface](https://docs.gluonhq.com/#_device_interface)

The section [Building Attach](https://docs.gluonhq.com/#_building_attach) contains more advanced information.

### How to build and install Attach Extended Printer ###

To build the Attach Extended Printer service on the project's root, run:

`./gradlew clean build`

If you want to install them, run:

`./gradlew clean publishToMavenLocal`

### How to use Attach Extended Printer ###

The printer service can be added to a Gluon Mobile project like:

```
<repositories>
    <repository>
        <id>Gluon</id>
        <url>https://nexus.gluonhq.com/nexus/content/repositories/releases</url>
    </repository>
</repositories>

<!-- dependencies -->
<dependency>
    <groupId>com.gluonhq.attachextendedprinter</groupId>
    <artifactId>printer</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.gluonhq.attachextendedprinter</groupId>
    <artifactId>printer</artifactId>
    <version>1.0.0</version>
    <classifier>android</classifier>
    <scope>runtime</scope>
</dependency>
```

and used from the project like:

```
PrinterService.create().ifPresent(service -> {
    if (!printer.deviceList().isEmpty()) {
        printer.print("something", printer.deviceList().get(0).getAddress(), 3000);
    }
});
```
