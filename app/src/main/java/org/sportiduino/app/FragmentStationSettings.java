package org.sportiduino.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.sportiduino.app.databinding.FragmentStationSettingsBinding;
import org.sportiduino.app.sportiduino.Card;
import org.sportiduino.app.sportiduino.CardAdapter;
import org.sportiduino.app.sportiduino.CardMifareClassic;
import org.sportiduino.app.sportiduino.CardMifareUltralight;
import org.sportiduino.app.sportiduino.CardType;
import org.sportiduino.app.sportiduino.Config;
import org.sportiduino.app.sportiduino.MasterCard;
import org.sportiduino.app.sportiduino.ReadWriteCardException;
import org.sportiduino.app.sportiduino.Util;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class FragmentStationSettings extends NfcFragment {
    private FragmentStationSettingsBinding binding;
    private Password password = Password.defaultPassword();
    private ArrayList<RadioButton> listRadioButtons;
    private CardType cardType = CardType.UNKNOWN;
    Calendar wakeupTime = Calendar.getInstance();
    private int timerCount;
    private Timer timer;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentStationSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        updatePasswordFromSharedPreferences(sharedPref);

        preferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("password")) {
                updatePasswordFromSharedPreferences(sharedPreferences);
            }
        };
        sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        int count = binding.radioGroup.getChildCount();
        listRadioButtons = new ArrayList<>();

        for (int i = 0; i < count; ++i) {
            final View v = binding.radioGroup.getChildAt(i);

            if (v instanceof RadioButton) {
                RadioButton rb = (RadioButton) v;
                listRadioButtons.add(rb);
                rb.setOnClickListener(rbClickListener);
            }
        }

        binding.editTextStationNumber.setFilters(new InputFilter[]{new MinMaxFilter(1, 255)});

        binding.buttonStart.setOnClickListener(buttonClickListener);
        binding.buttonFinish.setOnClickListener(buttonClickListener);
        binding.buttonClear.setOnClickListener(buttonClickListener);
        binding.buttonCheck.setOnClickListener(buttonClickListener);

        binding.textViewWakeupDate.setPaintFlags(binding.textViewWakeupDate.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        binding.textViewWakeupDate.setOnClickListener(dateClickListener);

        binding.textViewWakeupTime.setPaintFlags(binding.textViewWakeupTime.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        binding.textViewWakeupTime.setOnClickListener(timeClickListener);

        updateWakeupTime();
        
        ArrayAdapter<Config.ActiveModeDuration> adapter = new ArrayAdapter<>(requireActivity(),
                android.R.layout.simple_spinner_item, Config.ActiveModeDuration.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerActiveTime.setAdapter(adapter);
        binding.spinnerActiveTime.setSelection(1);

        binding.spinnerAntennaGain.setAdapter(new ArrayAdapter<>(requireActivity(),
                android.R.layout.simple_spinner_item, Config.AntennaGain.realValues()));
        binding.spinnerAntennaGain.setSelection(2);

        binding.newPassword1.setFilters(new InputFilter[]{new MinMaxFilter(0, 255)});
        binding.newPassword2.setFilters(new InputFilter[]{new MinMaxFilter(0, 255)});
        binding.newPassword3.setFilters(new InputFilter[]{new MinMaxFilter(0, 255)});

        binding.mpNewPassword1.setFilters(new InputFilter[]{new MinMaxFilter(0, 255)});
        binding.mpNewPassword2.setFilters(new InputFilter[]{new MinMaxFilter(0, 255)});
        binding.mpNewPassword3.setFilters(new InputFilter[]{new MinMaxFilter(0, 255)});
    }

    View.OnClickListener dateClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int year = wakeupTime.get(Calendar.YEAR);
            int month = wakeupTime.get(Calendar.MONTH);
            int day = wakeupTime.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(getActivity(), dateSetListener, year, month, day).show();
        }
    };

    View.OnClickListener timeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int hour = wakeupTime.get(Calendar.HOUR_OF_DAY);
            int minute = wakeupTime.get(Calendar.MINUTE);

            new TimePickerDialog(getActivity(), timeSetListener, hour, minute,
                    android.text.format.DateFormat.is24HourFormat(getActivity())).show();
        }
    };

    TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
        wakeupTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        wakeupTime.set(Calendar.MINUTE, minute);
        wakeupTime.set(Calendar.SECOND, 0);
        updateWakeupTime();
    };

    DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
        wakeupTime.set(Calendar.YEAR, year);
        wakeupTime.set(Calendar.MONTH, month);
        wakeupTime.set(Calendar.DAY_OF_MONTH, day);
        updateWakeupTime();
    };

    private void updatePasswordFromSharedPreferences(SharedPreferences sharedPreferences) {
        String passwordStr = sharedPreferences.getString("password", Password.defaultPassword().toString());
        this.password = Password.fromString(passwordStr);

        binding.newPassword1.setText(String.valueOf(password.getValue(0)));
        binding.newPassword2.setText(String.valueOf(password.getValue(1)));
        binding.newPassword3.setText(String.valueOf(password.getValue(2)));

        binding.mpNewPassword1.setText(String.valueOf(password.getValue(0)));
        binding.mpNewPassword2.setText(String.valueOf(password.getValue(1)));
        binding.mpNewPassword3.setText(String.valueOf(password.getValue(2)));
    }

    private void updateWakeupTime() {
        binding.textViewWakeupDate.setText(DateFormat.getDateInstance().format(wakeupTime.getTime()));
        binding.textViewWakeupTime.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(wakeupTime.getTime()));
    }

    View.OnClickListener rbClickListener = (View v) -> {
        RadioButton rb = (RadioButton) v;
        rbChecked(rb);
    };

    @Override
    public void onResume() {
        super.onResume();

        for (RadioButton rb : listRadioButtons) {
            if (rb.isChecked()) {
                rbChecked(rb);
            }
        }
    }

    private void rbChecked(RadioButton rb) {
        if (listRadioButtons.contains(rb) && binding.textViewNfcInfo.getText().toString().isEmpty()) {
            binding.textViewNfcInfo.setText(R.string.bring_card);
        }
        binding.layoutStationNumber.setVisibility(View.GONE);
        binding.layoutWakeupTime.setVisibility(View.GONE);
        binding.layoutPassword.setVisibility(View.GONE);
        binding.layoutConfig.setVisibility(View.GONE);
        int rbId = rb.getId();
        if (rbId == R.id.radio_button_master_get_state) {
            cardType = CardType.MASTER_GET_STATE;
        } else if (rbId == R.id.radio_button_master_time) {
            cardType = CardType.MASTER_SET_TIME;
        } else if (rbId == R.id.radio_button_master_number) {
            cardType = CardType.MASTER_SET_NUMBER;
            binding.layoutStationNumber.setVisibility(View.VISIBLE);
        } else if (rbId == R.id.radio_button_master_sleep) {
            cardType = CardType.MASTER_SLEEP;
            binding.layoutWakeupTime.setVisibility(View.VISIBLE);
        } else if (rbId == R.id.radio_button_master_password) {
            cardType = CardType.MASTER_PASSWORD;
            binding.layoutPassword.setVisibility(View.VISIBLE);
        } else if (rbId == R.id.radio_button_master_config) {
            cardType = CardType.MASTER_CONFIG;
            binding.layoutConfig.setVisibility(View.VISIBLE);
        } else if (rbId == R.id.radio_button_master_backup) {
            cardType = CardType.MASTER_READ_BACKUP;
        } else {
            cardType = CardType.UNKNOWN;
        }
        binding.scrollView.post(() -> binding.scrollView.fullScroll(View.FOCUS_DOWN));
    }

    View.OnClickListener buttonClickListener = (View view) -> {
        Button b = (Button) view;
        int id = b.getId();
        if (id == R.id.button_start) {
            binding.editTextStationNumber.setText(String.valueOf(Config.START_STATION));
        } else if (id == R.id.button_finish) {
            binding.editTextStationNumber.setText(String.valueOf(Config.FINISH_STATION));
        } else if (id == R.id.button_check) {
            binding.editTextStationNumber.setText(String.valueOf(Config.CHECK_STATION));
        } else if (id == R.id.button_clear) {
            binding.editTextStationNumber.setText(String.valueOf(Config.CLEAR_STATION));
        }
    };

    @Override
    public void onNewTagDetected(Tag tag) {
        if (cardType == CardType.UNKNOWN) {
            return;
        }
        String[] techList = tag.getTechList();
        CardAdapter adapter = null;
        for (String s : techList) {
            if (s.equals(MifareClassic.class.getName())) {
                adapter = new CardMifareClassic(MifareClassic.get(tag));
            } else if (s.equals(MifareUltralight.class.getName())) {
                adapter = new CardMifareUltralight(MifareUltralight.get(tag));
            }
            if (adapter != null) {
                MasterCard masterCard = getMasterCard(adapter);
                if (masterCard == null) {
                    return;
                }
                new WriteCardTask(masterCard).execute();
                break;
            }
        }
    }

    @Nullable
    private MasterCard getMasterCard(CardAdapter adapter) {
        MasterCard masterCard = new MasterCard(adapter, cardType, password);
        if (binding.radioButtonMasterNumber.isChecked()) {
            String str = binding.editTextStationNumber.getText().toString();
            int stationNumber;
            try {
                stationNumber = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                stationNumber = 0;
            }
            if (stationNumber == 0) {
                binding.textViewNfcInfo.setText(R.string.insert_station_number);
                return null;
            }
            masterCard.dataForWriting = MasterCard.packStationNumber(stationNumber);
        } else if (binding.radioButtonMasterTime.isChecked()) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND, 3);
            masterCard.dataForWriting = MasterCard.packTime(c);
        } else if (binding.radioButtonMasterSleep.isChecked()) {
            masterCard.dataForWriting = MasterCard.packTime(wakeupTime);
        } else if (binding.radioButtonMasterPassword.isChecked()) {
            Password password = new Password(
                    Integer.parseInt(binding.mpNewPassword1.getText().toString()),
                    Integer.parseInt(binding.mpNewPassword2.getText().toString()),
                    Integer.parseInt(binding.mpNewPassword3.getText().toString()));
            masterCard.dataForWriting = MasterCard.packNewPassword(password);
        } else if (binding.radioButtonMasterConfig.isChecked()) {
            Config config = new Config();
            config.stationCode = 0;  // if 0 don't change code of station
            config.activeModeDuration = (Config.ActiveModeDuration) binding.spinnerActiveTime.getSelectedItem();
            config.startAsCheck = binding.checkBoxStartFinish.isChecked();
            config.checkCardInitTime = binding.checkBoxInitTime.isChecked();
            config.autoSleep = binding.checkBoxAutoSleep.isChecked();
            config.antennaGain = (Config.AntennaGain) binding.spinnerAntennaGain.getSelectedItem();
            config.password = new Password(
                    Integer.parseInt(binding.newPassword1.getText().toString()),
                    Integer.parseInt(binding.newPassword2.getText().toString()),
                    Integer.parseInt(binding.newPassword3.getText().toString()));
            masterCard.dataForWriting = config.pack();
        }
        return masterCard;
    }

    private void startCountdownTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        CountdownTimerTask countdownTimerTask = new CountdownTimerTask();
        timerCount = 3;
        timer.scheduleAtFixedRate(countdownTimerTask, 0, 1000);
    }

    class CountdownTimerTask extends TimerTask {

        @Override
        public void run() {
            requireActivity().runOnUiThread(() -> {
                String timerStr = String.valueOf(timerCount);
                if (timerCount == 0) {
                    timerStr = getString(R.string.beep);
                } else if (timerCount < 0) {
                    timerStr = "";
                    timer.cancel();
                    timer = null;
                }
                binding.textViewTimer.setText(timerStr);
                --timerCount;
            });
        }
	}

    class WriteCardTask extends AsyncTask<Void, Void, Boolean> {
        final private Card card;

        WriteCardTask(Card card) {
            this.card = card;
        }

        @Override
        protected void onPreExecute() {
            binding.textViewNfcInfo.setText(R.string.writing_card_dont_remove_it);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                card.write();
                return true;
            } catch (ReadWriteCardException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                binding.textViewNfcInfo.setText(Util.ok(getString(R.string.data_written_to_card_successfully)));
                if (binding.radioButtonMasterTime.isChecked()) {
                    startCountdownTimer();
                }
            } else {
                binding.textViewNfcInfo.setText(Util.error(getString(R.string.writing_card_failed)));
            }
        }
    }
}
