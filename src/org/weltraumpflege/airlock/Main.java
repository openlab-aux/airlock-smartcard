package org.weltraumpflege.airlock;

import com.nxp.nfclib.CustomModules;
import com.nxp.nfclib.KeyType;
import com.nxp.nfclib.LibraryManager;
import com.nxp.nfclib.desfire.*;
import com.nxp.nfclib.interfaces.ILogger;
import com.nxp.nfclib.interfaces.IUtility;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;

public class Main {
    private static CryptographicKeys keys;
    private static IDESFireEV1 desfire;

    private static LibraryManager nxpLibraryManager;

    private static final int appId = 420;

    @Command(name = "list_readers", description = "List available Card Readers")
    private static class ListReaders implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            List<CardTerminal> terminals = TerminalFactory.getDefault().terminals().list();
            System.out.printf("Found %d Terminals\n", terminals.size());
            for(CardTerminal t: terminals) {
                System.out.printf("- %s\n", t.getName());
            }

            return 0;
        }
    }

    @Command(name = "read_id", description = "Read secret Card ID")
    private static class ReadId implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            desfire.selectApplication(appId);
            desfire.authenticate(1, IDESFireEV1.AuthType.AES, KeyType.AES128, keys.doorKey);
            byte[] id = desfire.readData(0, 0, 32, IDESFireEV1.CommunicationType.Enciphered, 32);
            System.out.println(new String(id, StandardCharsets.UTF_8));

            return null;
        }
    }

    @Command(name = "create_application", description = "create airlock application")
    private static class CreateApplication implements Callable<Integer> {
        public Integer call() throws Exception {
            IUtility utility = CustomModules.getUtility();
            // Step 1: Authenticate
            desfire.selectApplication(0);

            MultiAuth ma = new MultiAuth();
            ma.addAuthParam(KeyType.TWO_KEY_THREEDES, IDESFireEV1.AuthType.Native, keys.defaultCardMaster);
            ma.addAuthParam(KeyType.AES128, IDESFireEV1.AuthType.AES, keys.cardMaster);
            boolean authenticated = ma.authenticate(desfire, 0);
            if(!authenticated) {
                System.out.println("Couldn't authenticate with any known keys.");
                return 1;
            }

            // Step 2: Create Application
            EV1ApplicationKeySettings.Builder aksBuilder = new EV1ApplicationKeySettings.Builder();
            aksBuilder
                .setKeyTypeOfApplicationKeys(KeyType.AES128)
                .setMaxNumberOfApplicationKeys(10)
                .setAppMasterKeyChangeable(true)
                .setAuthenticationRequiredForFileManagement(false)
                .setAuthenticationRequiredForDirectoryConfigurationData(false)
                .setAppKeyChangeAccessRight((byte)0x00);

            desfire.createApplication(
                utility.intToBytes(appId, 3),
                aksBuilder.build()
            );

            // Step 3: Change Application Master Key
            desfire.selectApplication(appId);
            desfire.authenticate(0, IDESFireEV1.AuthType.AES, KeyType.AES128, keys.defaultApplicationMaster);
            desfire.changeKey(0, KeyType.AES128, keys.defaultApplicationMaster.getKey().getEncoded(), keys.applicationMaster.getKey().getEncoded(), (byte)0x00);
            desfire.authenticate(0, IDESFireEV1.AuthType.AES, KeyType.AES128, keys.applicationMaster);

            desfire.changeKey(1, KeyType.AES128, keys.defaultApplicationMaster.getKey().getEncoded(), keys.doorKey.getKey().getEncoded(), (byte)0x00);

            // Step 4: Create File with Card ID
            DESFireFile.FileSettings fs = new DESFireFile.StdDataFileSettings(
                IDESFireEV1.CommunicationType.Enciphered,    // Communication type
                (byte)0x01,                                  // Readable with key 1 (door)
                (byte)0x00,                                  // other permissions with Application master key (id: 0)
                (byte)0x00,
                (byte)0x00,
                32                                           // Card ID is 32 characters
            );
            desfire.createFile(0, fs);
            desfire.writeData(
                0,                  // File Number
                0,                 // offset
                "01234567890123456789012345678901".getBytes()
            );

            return 0;
        }
    }

    @Command(name = "format", description = "format a card")
    private static class Format implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            desfire.selectApplication(0);

            MultiAuth ma = new MultiAuth();
            ma.addAuthParam(KeyType.TWO_KEY_THREEDES, IDESFireEV1.AuthType.Native, keys.defaultCardMaster);
            ma.addAuthParam(KeyType.AES128, IDESFireEV1.AuthType.AES, keys.cardMaster);
            boolean authenticated = ma.authenticate(desfire, 0);
            if(!authenticated) {
                System.out.println("Couldn't authenticate with any known keys.");
                return 1;
            }
            desfire.format();
            return 0;
        }
    }

    @Command(name  = "card_info", description = "Gets information on card")
    public static class CardInfo implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            desfire.selectApplication(0);
            int applicationIds[] = desfire.getApplicationIDs();
            System.out.printf("Applications: %d\n", applicationIds.length);
            for (int id : applicationIds) {
                System.out.printf("... found application with ID %d\n", id);
            }
            return 0;
        }
    }

    @Command(name = "airlock-sc", subcommands = {
        CreateApplication.class,
        Format.class,
        CardInfo.class,
        ReadId.class,
        ListReaders.class
    })
    private static class HelloWorld implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Hello!");
            return 0;
        }
    }

    public static void main(String args[]) {
        ListReaders lr = new ListReaders();
        try {
            lr.call();
        } catch(Exception e) {}

        // Register Java Application
        nxpLibraryManager = new LibraryManager();
        nxpLibraryManager.registerJavaApp("InspireJavaLicense.txt");

        // Load Keys
        keys = new CryptographicKeys();
        keys.loadKeys("./keystore.jks", System.getenv("AIRLOCK_KEYSTORE_PASSWORD"));

        // Setup DESFire
        String readerName = System.getenv("AIRLOCK_SMARTCARD_READER_NAME");
        PcscdApduHandler apdu = new PcscdApduHandler();
        try {
            apdu.selectReader(readerName);
        } catch (RuntimeException e) {
            System.out.printf("Could not select Reader with name \"%s\".\n", readerName);
            System.exit(1);
        }

        CustomModules cm = new CustomModules();
        cm.getLogger().disableLogging(ILogger.LogLevel.INFO);

        cm.setTransceive(apdu);
        System.out.println("Waiting for Card...");
        apdu.waitForCard();
        desfire = DESFireFactory.getInstance().getDESFire(cm);
        desfire.setCommandSet(IDESFireEV1.CommandSet.ISO);

        // Call subcommand
        CommandLine cli = new CommandLine(new HelloWorld());
        int ret = cli.execute(args);
        System.exit(ret);
    }
}