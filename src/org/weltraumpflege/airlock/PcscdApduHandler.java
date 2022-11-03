package org.weltraumpflege.airlock;

import com.nxp.nfclib.CustomModules;
import com.nxp.nfclib.interfaces.IApduHandler;
import com.nxp.nfclib.interfaces.IReader;
import com.nxp.nfclib.interfaces.IUtility;

import javax.smartcardio.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

public class PcscdApduHandler implements IApduHandler {
    private CardTerminal reader;
    private CardChannel chan;

    private ByteBuffer cmd, res;

    public PcscdApduHandler() {
        cmd = ByteBuffer.allocate(512);
        res = ByteBuffer.allocate(512);
    }

    public void selectReader(String name) {
        CardTerminals terminals = TerminalFactory.getDefault().terminals();
        try {
            for(CardTerminal t: terminals.list()) {
                if (t.getName().equals(name)) {
                    this.reader = t;
                    return;
                }
            }
        } catch (CardException e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("No suitable Smartcard Reader found.");
    }

    public void waitForCard() {
        try {
            this.reader.waitForCardPresent((long) 30000.00);
        } catch (CardException e) {
        }

        try {
            Card c = this.reader.connect("*");
            System.out.printf("Channel Protocol: %s\n", c.getProtocol());
            this.chan = c.getBasicChannel();
        } catch (CardException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] apduExchange(byte[] data) {
        cmd.clear();
        res.clear();

        cmd.put(data);
        cmd.flip();

        IUtility utility = CustomModules.getUtility();
        System.out.printf("tx: %s\n", utility.dumpBytes(data));

        try
        {
            int rx = chan.transmit(cmd, res);
            byte[] resBytes = new byte[rx];
            res.flip();
            res.get(resBytes);


            System.out.printf("rx: %s\n", utility.dumpBytes(resBytes));
            ResponseAPDU resApdu = new ResponseAPDU(resBytes);
            return resApdu.getBytes();
        }
        catch( CardException e )
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IReader getReader() {
        return null;
    }
}
