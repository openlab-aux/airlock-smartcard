# Airlock-Smartcard

This repository holds the application used to provision Smartcards  with Airlock.

## Build

1. If not already present, install maven
1. Clone Repository
1. Download and unpack TaplinxSDK
    ```
    cd lib
    wget https://www.mifare.net/wp-content/uploads/2022/08/taplinx-java-release-2.0-RELEASE.zip
    unzip taplinx-java-release-2.0-RELEASE.zip
    cd ..
    ```
1. Run maven
    ```
    mvn initialize
    mvn package
    ```
   
There should now be a `target` folder containing a jar file

## Further Setup

### Taplinx License

In order to run the command, you have to acquire a TapLinx license. You can
sign up for one free of charge at [TapLinx Developer Center][1]

The resulting license needs to be in the working directory when the application
is run.

### KeyStore

For convenience, secret key material is stored in a JKS File.

```bash
keytool -genseckey -alias card_master -keystore keystore.jks -keyalg AES -keysize 128
keytool -genseckey -alias app_master -keystore keystore.jks -keyalg AES -keysize 128
keytool -genseckey -alias door_key -keystore keystore.jks -keyalg AES -keysize 128
```

All of these commands will ask for a passphrase with which the Keystore will be encrypted.

The encryption passphrase needs to be exported as an environment variable:

```
export AIRLOCK_KEYSTORE_PASSWORD=supersecret
```

The resulting JKS file needs to be located in the working directory when the
application is run.

### Select a Smartcard Reader

Now, we can query available Smartcard readers:

```bash
java -classpath target/airlock-1.0-SNAPSHOT.jar:lib/librarymanager-2.0-RELEASE.jar:lib/desfire-2.0-RELEASE.jar org.weltraumpflege.airlock.Main
```

The output should look something like:

```
Found 1 Terminals
- Linux Foundation 2.0 root hub (2019710) 00 00
Could not select Reader with name "null".
```

Now we can export our reader's name (everything after `- `) as an environment variable

```bash
export AIRLIOCK_SMARTCARD_READER_NAME="Linux Foundation 2.0 root hub (2019710) 00 00"
```

## Usage

```
java -jar target/airlock-1.0-SNAPSHOT.jar --help
```

### `create_application`

This will create an application with 

[1]: https://inspire.nxp.com/mifare/index.html