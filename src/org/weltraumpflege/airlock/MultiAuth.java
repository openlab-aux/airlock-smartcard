package org.weltraumpflege.airlock;

import com.nxp.nfclib.KeyType;
import com.nxp.nfclib.defaultimpl.KeyData;
import com.nxp.nfclib.desfire.IDESFireEV1;
import com.nxp.nfclib.exceptions.InvalidResponseLengthException;

import java.util.List;
import java.util.Vector;

public class MultiAuth {
    public class AuthParams {
        KeyType keyType;
        IDESFireEV1.AuthType authType;
        KeyData keyData;

        public AuthParams(KeyType keyType, IDESFireEV1.AuthType authType, KeyData keyData) {
            this.authType = authType;
            this.keyType = keyType;
            this.keyData = keyData;
        }
    }

    private List<AuthParams> authParams = new Vector<AuthParams>();

    public void addAuthParam(KeyType keyType, IDESFireEV1.AuthType authType, KeyData keyData) {
        authParams.add(new AuthParams(keyType, authType, keyData));
    }

    public boolean authenticate(IDESFireEV1 desfire, int applicationId) throws Exception {
        for (AuthParams a : authParams) {
            try {
                desfire.authenticate(applicationId, a.authType, a.keyType, a.keyData);
                return true;
            } catch (InvalidResponseLengthException e) {
                if(!e.getMessage().equals("Authentication Error")) {
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }

}
