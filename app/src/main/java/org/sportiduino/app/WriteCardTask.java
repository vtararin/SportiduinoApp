package org.sportiduino.app;

import static org.sportiduino.app.sportiduino.Constants.CARD_PAGE_INIT;
import static org.sportiduino.app.sportiduino.Constants.FW_PROTO_VERSION;
import static org.sportiduino.app.sportiduino.Constants.MASTER_CARD_SIGN;

import android.os.AsyncTask;

import org.sportiduino.app.sportiduino.Card;
import org.sportiduino.app.sportiduino.Util;
import org.sportiduino.app.sportiduino.WriteCardException;

public class WriteCardTask extends AsyncTask<Void, Void, Void> {
    Card card;
    Util.Callback showText;
    byte[] password;

    //final int numOfBlockInSector = 4;
    //byte[][] buffer = new byte[numOfSector * numOfBlockInSector][MifareClassic.BLOCK_SIZE];

    WriteCardTask(Card card, Util.Callback showText, Password password) {
        this.card = card;
        this.showText = showText;
        this.password = password.toByteArray();
    }

    WriteCardTask(Card card, Util.Callback showText) {
        this(card, showText, Password.defaultPassword());
    }

    @Override
    protected void onPreExecute() {
        showText.call("Writing card, don't remove it!");
    }

    @Override
    protected Void doInBackground(Void... params) {

        card.connect();

        try {
            switch (card.type) {
                case ORDINARY:
                    break;
                case MASTER_SET_NUMBER: {
                    final byte[][] data = {
                            {0, (byte) card.type.value, MASTER_CARD_SIGN, FW_PROTO_VERSION},
                            {password[0], password[1], password[2], 0},
                            {1, 0, 0, 0} // FIXME
                    };
                    card.writePages(CARD_PAGE_INIT, data, data.length);
                    break;
                }
                case MASTER_SLEEP: {
                    final byte[][] data = {
                            {0, (byte) card.type.value, MASTER_CARD_SIGN, FW_PROTO_VERSION},
                            {password[0], password[1], password[2], 0}
                            // TODO: add wakeup time
                    };
                    card.writePages(CARD_PAGE_INIT, data, data.length);
                    break;
                }
                case MASTER_SET_TIME: {
                    final byte[][] data = {
                            {0, (byte) card.type.value, MASTER_CARD_SIGN, FW_PROTO_VERSION},
                            {password[0], password[1], password[2], 0}
                            // FIXME: add time
                    };
                    card.writePages(CARD_PAGE_INIT, data, data.length);
                    break;
                }
                case MASTER_GET_STATE: {
                    final byte[][] data = {
                            {0, (byte) card.type.value, MASTER_CARD_SIGN, FW_PROTO_VERSION},
                            {password[0], password[1], password[2], 0}
                    };
                    card.writePages(CARD_PAGE_INIT, data, data.length);
                    break;
                }
            }
            showText.call("Data written to card successfully");
        } catch (WriteCardException e) {
            showText.call("Writing card failed!");
        }

        card.close();

        return null;
    }
}
