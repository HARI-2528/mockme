package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.DECIMAL_FORMAT;
import static cl.coders.faketraveler.MainActivity.sharedPrefKey;
import static cl.coders.faketraveler.SharedPrefsUtil.getDouble;
import static cl.coders.faketraveler.SharedPrefsUtil.putDouble;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.more_layout), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);

        TextView tvLeafletLicense = findViewById(R.id.tv_LeafletLicense);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvLeafletLicense.setText(Html.fromHtml(getString(R.string.ActivityMore_LeafletLicense),
                    Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvLeafletLicense.setText(Html.fromHtml(getString(R.string.ActivityMore_LeafletLicense)));
        }
        tvLeafletLicense.setMovementMethod(LinkMovementMethod.getInstance());

        EditText etDMockLat = findViewById(R.id.et_DMockLat);
        etDMockLat.setText(DECIMAL_FORMAT.format(getDouble(sharedPref, "dLat", 0)));
        etDMockLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (etDMockLat.getText().toString().isBlank()) {
                    putDouble(editor, "dLat", 0);
                } else {
                    try {
                        putDouble(editor, "dLat", Double.parseDouble(etDMockLat.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse dLat!", t);
                    }
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText etDMockLon = findViewById(R.id.et_DMockLon);
        etDMockLon.setText(DECIMAL_FORMAT.format(getDouble(sharedPref, "dLng", 0)));
        etDMockLon.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (etDMockLon.getText().toString().isBlank()) {
                    putDouble(editor, "dLng", 0);
                } else {
                    try {
                        putDouble(editor, "dLng", Double.parseDouble(etDMockLon.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse dLng!", t);
                    }
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText etMockCount = findViewById(R.id.et_MockCount);
        etMockCount.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("mockCount", 0)));
        etMockCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (etMockCount.getText().toString().isBlank()) {
                    editor.putInt("mockCount", 0);
                } else {
                    try {
                        editor.putInt("mockCount", Integer.parseInt(etMockCount.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse mockCount!", t);
                    }
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText etMockFrequency = findViewById(R.id.et_MockFrequency);
        etMockFrequency.setText(String.format(Locale.ROOT, "%d", sharedPref.getInt("mockFrequency", 10)));
        etMockFrequency.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (etMockFrequency.getText().toString().isBlank()) {
                    editor.putInt("mockFrequency", 10);
                } else {
                    try {
                        editor.putInt("mockFrequency", Integer.parseInt(etMockFrequency.getText().toString()));
                    } catch (Throwable t) {
                        Log.e(MoreActivity.class.toString(), "Could not parse mockFrequency!", t);
                    }
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        CheckBox mockSpeed = findViewById(R.id.cb_MockSpeed);
        mockSpeed.setChecked(sharedPref.getBoolean("mockSpeed", true));
        mockSpeed.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("mockSpeed", mockSpeed.isChecked());
            editor.apply();
        });

        EditText etMapProvider = findViewById(R.id.et_MapProvider);
        etMapProvider.setText(sharedPref.getString("mapProvider",
                MapProviderUtil.getDefaultMapProvider(Locale.getDefault())));
        etMapProvider.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (etMapProvider.getText().toString().isBlank()) {
                    editor.putString("mapProvider", MapProviderUtil.getDefaultMapProvider(Locale.getDefault()));
                } else {
                    editor.putString("mapProvider", etMapProvider.getText().toString());
                }

                editor.apply();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        SeekBar seekbarSpeed = findViewById(R.id.seekbar_speed);
        TextView tvSpeedValue = findViewById(R.id.tv_speed_value);
        int savedSpeed = sharedPref.getInt("routeSpeed", 50);
        seekbarSpeed.setProgress(savedSpeed);
        tvSpeedValue.setText(savedSpeed + " km/h");
        seekbarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 5) {
                    progress = 5;
                    seekBar.setProgress(5);
                }
                tvSpeedValue.setText(progress + " km/h");
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("routeSpeed", progress);
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        RadioGroup rgMode = findViewById(R.id.rg_mode);
        String savedMode = sharedPref.getString("routeMode", "driving");
        if ("walking".equals(savedMode)) {
            rgMode.check(R.id.rb_walking);
        } else if ("cycling".equals(savedMode)) {
            rgMode.check(R.id.rb_cycling);
        } else {
            rgMode.check(R.id.rb_driving);
        }
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            String mode = "driving";
            if (checkedId == R.id.rb_walking) {
                mode = "walking";
            } else if (checkedId == R.id.rb_cycling) {
                mode = "cycling";
            }
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("routeMode", mode);
            editor.apply();
        });

        CheckBox switchLoop = findViewById(R.id.switch_loop);
        switchLoop.setChecked(sharedPref.getBoolean("routeLoop", false));
        switchLoop.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("routeLoop", isChecked);
            editor.apply();
        });
    }

}