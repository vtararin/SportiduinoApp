package org.sportiduino.app;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.sportiduino.app.databinding.FragmentReadCardBinding;
import org.sportiduino.app.sportiduino.Card;
import org.sportiduino.app.sportiduino.CardMifareClassic;
import org.sportiduino.app.sportiduino.CardMifareUltralight;
import org.sportiduino.app.sportiduino.Util;

public class FragmentReadCard extends NfcFragment implements IntentReceiver {
    private FragmentReadCardBinding binding;
    private String tagIdStr;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentReadCardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.textViewInfo.setText(R.string.bring_card);
    }

    @Override
    public void onNewTagDetected(Tag tag) {
        StringBuilder tagInfo = new StringBuilder();
        byte[] tagId = tag.getId();
        for (byte b : tagId) {
            tagInfo.append(Integer.toHexString(b & 0xFF)).append(" ");
        }
        tagIdStr = tagInfo.toString();
        binding.textViewInfo.setText(String.format(getString(R.string.tag_id_s), tagIdStr));

        String[] techList = tag.getTechList();
        Card card = null;
        for (String s : techList) {
            if (s.equals(MifareClassic.class.getName())) {
                card = new CardMifareClassic(MifareClassic.get(tag));
            } else if (s.equals(MifareUltralight.class.getName())) {
                card = new CardMifareUltralight(MifareUltralight.get(tag));
            }
            if (card != null) {
                new ReadCardTask(card, setText, setTagType).execute();
                break;
            }
        }
    }

    public Util.Callback setText = (str) -> binding.textViewTagInfo.setText(str);
    public Util.Callback setTagType = (str) -> {
        binding.textViewInfo.setText(String.format(getString(R.string.tag_id_s_type_s), tagIdStr, str));
    };
}
