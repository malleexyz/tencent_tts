package xy.max.tencent.tts.tencent_tts;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.qcloudtts.LongTextTTS.LongTextTtsController;
import com.tencent.qcloudtts.VoiceLanguage;
import com.tencent.qcloudtts.VoiceSpeed;
import com.tencent.qcloudtts.VoiceType;
import com.tencent.qcloudtts.callback.QCloudPlayerCallback;
import com.tencent.qcloudtts.callback.TtsExceptionHandler;
import com.tencent.qcloudtts.exception.TtsException;
import com.tencent.qcloudtts.exception.TtsNotInitializedException;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;


/**
 * TencentTtsPlugin
 */
public class TencentTtsPlugin implements FlutterPlugin, MethodCallHandler {

    private static final String TAG = "TencentTtsPlugin";

    private Context context;


    private MethodChannel channel;

    private LongTextTtsController mTtsController;

    private int tts_speed = VoiceSpeed.VOICE_SPEED_NORMAL.getNum();
    private int tts_voice = VoiceType.VOICE_TYPE_AFFNITY_FEMALE.getNum();
    private int tts_language = VoiceLanguage.VOICE_LANGUAGE_CHINESE.getNum();

    private AudioManager.OnAudioFocusChangeListener listener;


    /**
     * Plugin registration.
     */
    public static void registerWith(PluginRegistry.Registrar registrar) {
        TencentTtsPlugin instance = new TencentTtsPlugin();
        instance.initInstance(registrar.messenger(), registrar.activeContext());
    }


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        initInstance(flutterPluginBinding.getBinaryMessenger(), flutterPluginBinding.getApplicationContext());

    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "init": {
              String appId = call.argument("appId");
              String secretId = call.argument("secretId");
              String secretKey = call.argument("secretKey");
              init(Long.parseLong(appId), secretId, secretKey);
              result.success(1);
                break;
            }
            case "speak": {
                String text = call.arguments.toString();
                speak(text);
                result.success(1);
                break;
            }
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    private void initInstance(BinaryMessenger messenger, Context context) {
        this.context = context;
        channel = new MethodChannel(messenger, "tencent_tts");
        channel.setMethodCallHandler(this);
//    handler = new Handler(Looper.getMainLooper());
//    bundle = new Bundle();
//    tts = new TextToSpeech(context, onInitListener, googleTtsEngine);

        //构造LongTextTtsController，支持长文本播放，可暂停/恢复播放。非流式api，故建议文本中第一句话不要设的太长
        mTtsController = new LongTextTtsController();

    }

    /**
     * Android Plugin APIs
     */


    private void init(Long appId, String secretId, String secretKey) {
        mTtsController.init(context, appId
                , secretId
                , secretKey);

    }

    //发起tts请求
    private void speak(final String ttsText) {

        try {

            //设置语速
            mTtsController.setVoiceSpeed(tts_speed);

            //设置音色
            mTtsController.setVoiceType(tts_voice);

            //设置语言
            mTtsController.setVoiceLanguage(tts_language);

            //设置ProjectId
            mTtsController.setProjectId(0);


            mTtsController.startTts(ttsText, mTtsExceptionHandler, new QCloudPlayerCallback() {

                //播放开始
                @Override
                public void onTTSPlayStart() {
                    Log.d("tts", "onPlayStart");
                }

                //音频缓冲中
                @Override
                public void onTTSPlayWait() {
                    Log.d("tts", "onPlayWait");
                }

                //缓冲完成，继续播放
                @Override
                public void onTTSPlayResume() {
                    Log.d("tts", "onPlayResume");
                }

                //连续播放下一句
                @Override
                public void onTTSPlayNext() {
                    Log.d("tts", "onPlayNext");
                }

                //播放中止
                @Override
                public void onTTSPlayStop() {
                    Log.d("tts", "onPlayStop");
                }

                //播放结束
                @Override
                public void onTTSPlayEnd() {
                    Log.d("tts", "onPlayEnd");
                }

                //当前播放的字符,当前播放的字符在所在的句子中的下标.
                @Override
                public void onTTSPlayProgress(String currentWord, int currentIndex) {
                    Log.d("tts", "onTTSPlayProgress" + currentWord + currentIndex);
                }
            });
        } catch (TtsNotInitializedException e) {
            Log.e("tts", "TtsNotInitializedException e:" + e.getMessage());
        }
    }


    private final TtsExceptionHandler mTtsExceptionHandler = new TtsExceptionHandler() {
        @Override
        public void onRequestException(TtsException e) {
            Log.e(TAG, "tts onRequestException :" + e.getMessage());
            //网络出错的时候
            mTtsController.pause();
//      Toast.makeText(LongTextTtsActivity.this, e.getErrMsg(), Toast.LENGTH_SHORT).show();
        }

    };


    private void requestAudioFocus(@NonNull final LongTextTtsController ttsController) {
        //初始化audio mananger
        listener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    //丢失焦点，直接
                    mTtsController.stop();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    //丢失焦点，但是马上又能恢复
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    //降低音量
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    //获得了音频焦点
                }
            }
        };

        //设置listener
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void abandonAudioFocus() {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.abandonAudioFocus(listener);
        }
    }
}
