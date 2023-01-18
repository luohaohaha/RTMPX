package com.rtmpx.app.ui;

import static com.rtmpx.app.config.Const.BITRATE;
import static com.rtmpx.app.config.Const.FRAME_RATE;
import static com.rtmpx.app.config.Const.HEIGHT;
import static com.rtmpx.app.config.Const.PREFERENCE_PUBLISH_CONFIG;
import static com.rtmpx.app.config.Const.PREFERENCE_SAVE_CONFIG;
import static com.rtmpx.app.config.Const.PUBLISH_URL;
import static com.rtmpx.app.config.Const.RECORD;
import static com.rtmpx.app.config.Const.RECORD_PATH;
import static com.rtmpx.app.config.Const.RESULT;
import static com.rtmpx.app.config.Const.WIDTH;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_BITRATE;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_FRAME_RATE;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_VIDEO_HEIGHT;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_VIDEO_WIDTH;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.rtmpx.app.R;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Project: RTMPX<br/>
 * Package: com.rtmpx.app<br/>
 * ClassName: PublishPreferenceActivity<br/>
 * Description: TODO<br/>
 * Date: 2023-01-17 18:19 <br/>
 * <p>
 * Author luohao<br/>
 * Version 1.0<br/>
 * since JDK 1.6<br/>
 * <p>
 */
public class PublishPreferenceActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "PublishPreference";

    private LinearLayout mPublishBitrateContainer;
    private TextInputEditText mPublishBitrateInput;
    private LinearLayout mPublishUrlContainer;
    private TextInputEditText mPublishUrlInput;
    private LinearLayout mPublishFrameRateContainer;
    private TextInputEditText mPublishFrameRateInput;
    private LinearLayout mPublishResolutionContainer;
    private TextInputEditText mPublishResolutionInput;
    private LinearLayout mPublishRecordContainer;
    private Switch mPublishRecordSwitch;
    private LinearLayout mPublishRecordPathContainer;

    private TextInputEditText mPublishRecordPathInput;
    private Button mSavePublishConfig;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_setting);
        initView();
    }

    private void initView() {
        mPublishBitrateContainer = (LinearLayout) findViewById(R.id.publish_bitrate_container);
        mPublishBitrateInput = (TextInputEditText) findViewById(R.id.publish_bitrate_input);
        mPublishUrlContainer = (LinearLayout) findViewById(R.id.publish_url_container);
        mPublishUrlInput = (TextInputEditText) findViewById(R.id.publish_url_input);
        mPublishFrameRateContainer = (LinearLayout) findViewById(R.id.publish_frame_rate_container);
        mPublishFrameRateInput = (TextInputEditText) findViewById(R.id.publish_frame_rate_input);
        mPublishResolutionContainer = (LinearLayout) findViewById(R.id.publish_resolution_container);
        mPublishResolutionInput = (TextInputEditText) findViewById(R.id.publish_resolution_input);
        mPublishRecordContainer = (LinearLayout) findViewById(R.id.publish_record_container);
        mPublishRecordSwitch = (Switch) findViewById(R.id.publish_record_switch);
        mPublishRecordPathContainer = (LinearLayout) findViewById(R.id.publish_record_path_container);
        mPublishRecordPathInput = (TextInputEditText) findViewById(R.id.publish_record_path_input);
        mSavePublishConfig = (Button) findViewById(R.id.save_publish_config);

        mSavePublishConfig.setOnClickListener(this);
        mPublishRecordSwitch.setOnCheckedChangeListener(this);

        String savedCong = getSharedPreferences(PREFERENCE_PUBLISH_CONFIG, MODE_PRIVATE).getString(PREFERENCE_SAVE_CONFIG, "");
        if (!TextUtils.isEmpty(savedCong)) {
            try {
                JSONObject config = new JSONObject(savedCong);
                mPublishBitrateInput.setText(String.valueOf(config.optInt(BITRATE) / 1000));
                mPublishUrlInput.setText(config.optString(PUBLISH_URL));
                mPublishFrameRateInput.setText(config.optString(FRAME_RATE));
                mPublishResolutionInput.setText(String.format("%sx%s", config.optString(WIDTH), config.optString(HEIGHT)));
                mPublishRecordSwitch.setChecked(config.optBoolean(RECORD));
                mPublishRecordPathInput.setText(config.optString(RECORD_PATH));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_publish_config:
                String publishUrl = mPublishUrlInput.getText().toString();
                if (TextUtils.isEmpty(publishUrl)) {
                    Toast.makeText(this, mPublishUrlInput.getHint(), Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject result = new JSONObject();
                int bitrate = 0;
                int frameRate = 0;
                int width = 0;
                int height = 0;

                try {
                    bitrate = Integer.parseInt(mPublishBitrateInput.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (bitrate <= 0) {
                    Log.d(TAG, String.format(" bitrate is %s , use default ", bitrate));
                    bitrate = RTMP_DEFAULT_BITRATE;
                }

                try {
                    frameRate = Integer.parseInt(mPublishFrameRateInput.getText().toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (frameRate <= 0) {
                    Log.d(TAG, String.format(" frameRate is %s , use default ", frameRate));
                    frameRate = RTMP_DEFAULT_FRAME_RATE;
                }

                try {
                    String[] resolution = mPublishResolutionInput.getText().toString().split("x");
                    width = Integer.parseInt(resolution[0]);
                    height = Integer.parseInt(resolution[1]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (width <= 0 || height <= 0) {
                    Log.d(TAG, String.format(" width is %s , height is %s , use default ", width, height));
                    width = RTMP_DEFAULT_VIDEO_WIDTH;
                    height = RTMP_DEFAULT_VIDEO_HEIGHT;
                }

                try {
                    result.putOpt(BITRATE, bitrate * 1000);
                    result.putOpt(PUBLISH_URL, publishUrl);
                    result.putOpt(FRAME_RATE, frameRate);
                    result.putOpt(WIDTH, width);
                    result.putOpt(HEIGHT, height);
                    boolean checked = mPublishRecordSwitch.isChecked();
                    result.putOpt(RECORD, checked);
                    if (checked) {
                        result.putOpt(RECORD_PATH, mPublishRecordPathInput.getText().toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String value = result.toString();
                Log.d(TAG, " =====CONFIG==== " + value);
                getSharedPreferences(PREFERENCE_PUBLISH_CONFIG, MODE_PRIVATE).edit().putString(PREFERENCE_SAVE_CONFIG, value).commit();
                Intent intent = new Intent();
                intent.putExtra(RESULT, value);
                setResult(RESULT_OK, intent);
                finish();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mPublishRecordPathContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
    }
}
