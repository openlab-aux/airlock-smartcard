package org.weltraumpflege.airlock;

import com.nxp.nfclib.defaultimpl.KeyData;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.KeySpec;

public class CryptographicKeys {
    public byte defaultBytes[] = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    };

    public byte intermediaryBytes[] = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
    };

    private KeyStore keyStore;

    public KeyData cardMaster = new KeyData();
    public KeyData applicationMaster = new KeyData();
    public KeyData doorKey = new KeyData();

    public KeyData defaultCardMaster = new KeyData();
    public KeyData intermediaryCardMaster = new KeyData();

    public KeyData defaultApplicationMaster = new KeyData();

    public void loadKeys(String path, String password) {
        try {
            this.keyStore = KeyStore.getInstance(new File(path), password.toCharArray());
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            cardMaster.setKey(keyStore.getKey("card_master", password.toCharArray()));
            applicationMaster.setKey(keyStore.getKey("app_master", password.toCharArray()));
            doorKey.setKey(keyStore.getKey("door_key", password.toCharArray()));
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }

        // default keys
        defaultCardMaster.setKey(new SecretKeySpec(defaultBytes, "DESede"));
        intermediaryCardMaster.setKey(new SecretKeySpec(intermediaryBytes, "DESede"));
        defaultApplicationMaster.setKey(new SecretKeySpec(ByteBuffer.allocate(16).array(), "AES"));
    }
}
