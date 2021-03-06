package HeavenTao.Audio;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import HeavenTao.Data.*;

//音频处理线程类。
public abstract class AudioProcThread extends Thread
{
    public String m_CurClsNameStrPt = this.getClass().getSimpleName(); //当前类名称字符串

    public int m_ExitFlag = 0; //本线程退出标记，为0表示保持运行，为1表示请求退出。
    public int m_ExitCode = 0; //本线程退出代码，为0表示正常退出，为-1表示初始化失败，为-2表示处理失败。

    public static Context m_AppContextPt; //存放应用程序上下文类对象的内存指针。
    public int m_SamplingRate = 16000; //采样频率，取值只能为8000、16000、32000。
    public int m_FrameLen = 320; //帧的数据长度，单位采样数据，取值只能为10毫秒的倍数。例如：8000Hz的10毫秒为80、20毫秒为160、30毫秒为240，16000Hz的10毫秒为160、20毫秒为320、30毫秒为480，32000Hz的10毫秒为320、20毫秒为640、30毫秒为960。

    public int m_IsPrintLogcat = 0; //存放是否打印Logcat日志。

    PowerManager.WakeLock m_ProximityScreenOffWakeLockPt; //存放接近息屏唤醒锁类对象的内存指针。
    PowerManager.WakeLock m_FullWakeLockPt; //存放屏幕键盘全亮唤醒锁类对象的内存指针。
    int m_IsUseWakeLock; //存放是否使用唤醒锁，非0表示要使用，0表示不使用。

    AudioRecord m_AudioRecordPt; //存放音频输入类对象的内存指针。
    int m_AudioRecordBufSz; //存放音频输入类对象的缓冲区大小，单位字节。

    AudioTrack m_AudioTrackPt; //存放音频输出类对象的内存指针。
    int m_AudioTrackBufSz; //存放音频输出类对象的缓冲区大小，单位字节。

    public int m_UseWhatAec = 0; //存放使用什么声学回音消除器，为0表示不使用，为1表示Speex声学回音消除器，为2表示WebRtc定点版声学回音消除器，为2表示WebRtc浮点版声学回音消除器，为4表示SpeexWebRtc三重声学回音消除器。

    SpeexAec m_SpeexAecPt; //存放Speex声学回音消除器类对象的内存指针。
    int m_SpeexAecFilterLen; //存放Speex声学回音消除器的滤波器数据长度，单位毫秒。
    int m_SpeexAecIsSaveMemFile; //存放Speex声学回音消除器是否保存内存块到文件，为非0表示要保存，为0表示不保存。
    String m_SpeexAecMemFileFullPathStrPt; //存放Speex声学回音消除器的内存块文件完整路径字符串。

    WebRtcAecm m_WebRtcAecmPt; //存放WebRtc定点版声学回音消除器类对象的内存指针。
    int m_WebRtcAecmIsUseCNGMode; //存放WebRtc定点版声学回音消除器是否使用舒适噪音生成模式，为非0表示要使用，为0表示不使用。
    int m_WebRtcAecmEchoMode; //存放WebRtc定点版声学回音消除器的消除模式，消除模式越高消除越强，取值区间为[0,4]。
    int m_WebRtcAecmDelay; //存放WebRtc定点版声学回音消除器的回音延迟，单位毫秒，取值区间为[-2147483648,2147483647]，为0表示自适应设置。

    WebRtcAec m_WebRtcAecPt; //存放WebRtc浮点版声学回音消除器类对象的内存指针。
    int m_WebRtcAecEchoMode; //存放WebRtc浮点版声学回音消除器的消除模式，消除模式越高消除越强，取值区间为[0,2]。
    int m_WebRtcAecDelay; //存放WebRtc浮点版声学回音消除器的回音延迟，单位毫秒，取值区间为[-2147483648,2147483647]，为0表示自适应设置。
    int m_WebRtcAecIsUseDelayAgnosticMode; //存放WebRtc浮点版声学回音消除器是否使用回音延迟不可知模式，为非0表示要使用，为0表示不使用。
    int m_WebRtcAecIsUseExtdFilterMode; //存放WebRtc浮点版声学回音消除器是否使用扩展滤波器模式，为非0表示要使用，为0表示不使用。
    int m_WebRtcAecIsUseRefinedFilterAdaptAecMode; //存放WebRtc浮点版声学回音消除器是否使用精制滤波器自适应Aec模式，为非0表示要使用，为0表示不使用。
    int m_WebRtcAecIsUseAdaptAdjDelay; //存放WebRtc浮点版声学回音消除器是否使用自适应调节回音的延迟，为非0表示要使用，为0表示不使用。
    int m_WebRtcAecIsSaveMemFile; //存放WebRtc浮点版声学回音消除器是否保存内存块到文件，为非0表示要保存，为0表示不保存。
    String m_WebRtcAecMemFileFullPathStrPt; //存放WebRtc浮点版声学回音消除器的内存块文件完整路径字符串。

    SpeexWebRtcAec m_SpeexWebRtcAecPt; //存放SpeexWebRtc三重声学回音消除器类对象的内存指针。
    int m_SpeexWebRtcAecWorkMode; //存放SpeexWebRtc三重声学回音消除器的工作模式，为1表示Speex声学回音消除器+WebRtc定点版声学回音消除器，为2表示WebRtc定点版声学回音消除器+WebRtc浮点版声学回音消除器，为3表示Speex声学回音消除器+WebRtc定点版声学回音消除器+WebRtc浮点版声学回音消除器。
    int m_SpeexWebRtcAecSpeexAecFilterLen; //存放SpeexWebRtc三重声学回音消除器的Speex声学回音消除器的滤波器数据长度，单位毫秒。
    float m_SpeexWebRtcAecSpeexAecEchoMultiple; //存放SpeexWebRtc三重声学回音消除器的Speex声学回音消除器在残余回音消除时，残余回音的倍数，倍数越大消除越强，取值区间为[0.0,100.0]。
    float m_SpeexWebRtcAecSpeexAecEchoCont; //存放SpeexWebRtc三重声学回音消除器的Speex声学回音消除器在残余回音消除时，残余回音的持续系数，系数越大消除越强，取值区间为[0.0,0.9]。
    int m_SpeexWebRtcAecSpeexAecEchoSupes; //存放SpeexWebRtc三重声学回音消除器的Speex声学回音消除器在残余回音消除时，残余回音最大衰减的分贝值，分贝值越小衰减越大，取值区间为[-2147483648,0]。
    int m_SpeexWebRtcAecSpeexAecEchoSupesAct; //存放SpeexWebRtc三重声学回音消除器的Speex声学回音消除器在残余回音消除时，有近端语音活动时残余回音最大衰减的分贝值，分贝值越小衰减越大，取值区间为[-2147483648,0]。
    int m_SpeexWebRtcAecWebRtcAecmIsUseCNGMode; //存放SpeexWebRtc三重声学回音消除器的WebRtc定点版声学回音消除器是否使用舒适噪音生成模式，为非0表示要使用，为0表示不使用。
    int m_SpeexWebRtcAecWebRtcAecmEchoMode; //存放SpeexWebRtc三重声学回音消除器的WebRtc定点版声学回音消除器的消除模式，消除模式越高消除越强，取值区间为[0,4]。
    int m_SpeexWebRtcAecWebRtcAecmDelay; //存放SpeexWebRtc三重声学回音消除器的WebRtc定点版声学回音消除器的回音延迟，单位毫秒，取值区间为[-2147483648,2147483647]，为0表示自适应设置。
    int m_SpeexWebRtcAecWebRtcAecEchoMode; //存放SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器的消除模式，消除模式越高消除越强，取值区间为[0,2]。
    int m_SpeexWebRtcAecWebRtcAecDelay; //存放SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器的回音延迟，单位毫秒，取值区间为[-2147483648,2147483647]，为0表示自适应设置。
    int m_SpeexWebRtcAecWebRtcAecIsUseDelayAgnosticMode; //存放SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器是否使用回音延迟不可知模式，为非0表示要使用，为0表示不使用。
    int m_SpeexWebRtcAecWebRtcAecIsUseExtdFilterMode; //存放SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器是否使用扩展滤波器模式，为非0表示要使用，为0表示不使用。
    int m_SpeexWebRtcAecWebRtcAecIsUseRefinedFilterAdaptAecMode; //存放SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器是否使用精制滤波器自适应Aec模式，为非0表示要使用，为0表示不使用。
    int m_SpeexWebRtcAecWebRtcAecIsUseAdaptAdjDelay; //存放SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器是否使用自适应调节回音的延迟，为非0表示要使用，为0表示不使用。

    public int m_UseWhatNs = 0; //存放使用什么噪音抑制器，为0表示不使用，为1表示Speex预处理器的噪音抑制，为2表示WebRtc定点版噪音抑制器，为3表示WebRtc浮点版噪音抑制器，为4表示RNNoise噪音抑制器。

    SpeexPproc m_SpeexPprocPt; //存放Speex预处理器类对象的内存指针。
    int m_SpeexPprocIsUseNs; //存放Speex预处理器是否使用噪音抑制，为非0表示要使用，为0表示不使用。
    int m_SpeexPprocNoiseSupes; //存放Speex预处理器在噪音抑制时，噪音最大衰减的分贝值，分贝值越小衰减越大，取值区间为[-2147483648,0]。
    int m_SpeexPprocIsUseDereverb; //存放Speex预处理器是否使用混响音消除，为非0表示要使用，为0表示不使用。
    int m_SpeexPprocIsUseRec; //存放Speex预处理器是否使用残余回音消除，为非0表示要使用，为0表示不使用。
    float m_SpeexPprocEchoMultiple; //存放Speex预处理器在残余回音消除时，残余回音的倍数，倍数越大消除越强，取值区间为[0.0,100.0]。
    float m_SpeexPprocEchoCont; //存放Speex预处理器在残余回音消除时，残余回音的持续系数，系数越大消除越强，取值区间为[0.0,0.9]。
    int m_SpeexPprocEchoSupes; //存放Speex预处理器在残余回音消除时，残余回音最大衰减的分贝值，分贝值越小衰减越大，取值区间为[-2147483648,0]。
    int m_SpeexPprocEchoSupesAct; //存放Speex预处理器在残余回音消除时，有近端语音活动时残余回音最大衰减的分贝值，分贝值越小衰减越大，取值区间为[-2147483648,0]。

    WebRtcNsx m_WebRtcNsxPt; //存放WebRtc定点版噪音抑制器类对象的内存指针。
    int m_WebRtcNsxPolicyMode; //存放WebRtc定点版噪音抑制器的策略模式，策略模式越高抑制越强，取值区间为[0,3]。

    WebRtcNs m_WebRtcNsPt; //存放WebRtc浮点版噪音抑制器类对象的内存指针。
    int m_WebRtcNsPolicyMode; //存放WebRtc浮点版噪音抑制器的策略模式，策略模式越高抑制越强，取值区间为[0,3]。

    RNNoise m_RNNoisePt; //RNNoise噪音抑制器类对象的内存指针。

    int m_IsUseSpeexPprocOther; //存放Speex预处理器是否使用其他功能，为非0表示要使用，为0表示不使用。
    int m_SpeexPprocIsUseVad; //存放Speex预处理器是否使用语音活动检测，为非0表示要使用，为0表示不使用。
    int m_SpeexPprocVadProbStart; //存放Speex预处理器在语音活动检测时，从无语音活动到有语音活动的判断百分比概率，概率越大越难判断为有语音活，取值区间为[0,100]。
    int m_SpeexPprocVadProbCont; //存放Speex预处理器在语音活动检测时，从有语音活动到无语音活动的判断百分比概率，概率越大越容易判断为无语音活动，取值区间为[0,100]。
    int m_SpeexPprocIsUseAgc; //存放Speex预处理器是否使用自动增益控制，为非0表示要使用，为0表示不使用。
    int m_SpeexPprocAgcLevel; //存放Speex预处理器在自动增益控制时，增益的目标等级，目标等级越大增益越大，取值区间为[1,2147483647]。
    int m_SpeexPprocAgcIncrement; //存放Speex预处理器在自动增益控制时，每秒最大增益的分贝值，分贝值越大增益越大，取值区间为[0,2147483647]。
    int m_SpeexPprocAgcDecrement; //存放Speex预处理器在自动增益控制时，每秒最大减益的分贝值，分贝值越小减益越大，取值区间为[-2147483648,0]。
    int m_SpeexPprocAgcMaxGain; //存放Speex预处理器在自动增益控制时，最大增益的分贝值，分贝值越大增益越大，取值区间为[0,2147483647]。

    public int m_UseWhatCodec = 0; //存放使用什么编解码器，为0表示PCM原始数据，为1表示Speex编解码器，为2表示Opus编解码器。

    SpeexEncoder m_SpeexEncoderPt; //存放Speex编码器类对象的内存指针。
    SpeexDecoder m_SpeexDecoderPt; //存放Speex解码器类对象的内存指针。
    int m_SpeexCodecEncoderUseCbrOrVbr; //存放Speex编码器使用固定比特率还是动态比特率进行编码，为0表示要使用固定比特率，为非0表示要使用动态比特率。
    int m_SpeexCodecEncoderQuality; //存放Speex编码器的编码质量等级，质量等级越高音质越好、压缩率越低，取值区间为[0,10]。
    int m_SpeexCodecEncoderComplexity; //存放Speex编码器的编码复杂度，复杂度越高压缩率不变、CPU使用率越高、音质越好，取值区间为[0,10]。
    int m_SpeexCodecEncoderPlcExpectedLossRate; //存放Speex编码器在数据包丢失隐藏时，数据包的预计丢失概率，预计丢失概率越高抗网络抖动越强、压缩率越低，取值区间为[0,100]。
    int m_SpeexCodecDecoderIsUsePerceptualEnhancement; //存放Speex解码器是否使用知觉增强，为非0表示要使用，为0表示不使用。

    int m_IsSaveAudioToFile = 0; //存放是否保存音频到文件，非0表示要使用，0表示不使用。
    WaveFileWriter m_AudioInputWaveFileWriterPt; //存放音频输入Wave文件写入器对象的内存指针。
    WaveFileWriter m_AudioOutputWaveFileWriterPt; //存放音频输出Wave文件写入器对象的内存指针。
    WaveFileWriter m_AudioResultWaveFileWriterPt; //存放音频结果Wave文件写入器对象的内存指针。
    String m_AudioInputFileFullPathStrPt; //存放音频输入文件的完整路径字符串。
    String m_AudioOutputFileFullPathStrPt; //存放音频输出文件的完整路径字符串。
    String m_AudioResultFileFullPathStrPt; //存放音频结果文件的完整路径字符串。

    LinkedList< short[] > m_InputFrameLnkLstPt; //存放输入帧链表类对象的内存指针。
    LinkedList< short[] > m_OutputFrameLnkLstPt; //存放已出帧链表类对象的内存指针。

    AudioInputThread m_AudioInputThreadPt; //存放音频输入线程类对象的内存指针。
    AudioOutputThread m_AudioOutputThreadPt; //存放音频输出线程类对象的内存指针。

    long m_HasVoiceActFrameTotal; //有语音活动帧总数。

    //音频输入线程类。
    private class AudioInputThread extends Thread
    {
        public int m_ExitFlag = 0; //本线程退出标记，0表示保持运行，1表示请求退出。

        //请求本线程退出。
        public void RequireExit()
        {
            m_ExitFlag = 1;
        }

        public void run()
        {
            this.setPriority( MAX_PRIORITY ); //设置本线程优先级。
            Process.setThreadPriority( Process.THREAD_PRIORITY_URGENT_AUDIO ); //设置本线程优先级。

            short p_TmpInputFramePt[];
            Date p_LastDatePt;
            Date p_NowDatePt;

            if( m_IsPrintLogcat != 0 ) Log.i( m_CurClsNameStrPt, "音频输入线程：开始准备音频输入。" );

            //计算WebRtc定点版和浮点版声学回音消除器的回音延迟。
            {
                HTInt pclDelay = new HTInt( 0 );
                int p_TmpInt32;

                p_LastDatePt = new Date();
                p_TmpInputFramePt = new short[m_FrameLen];

                //跳过刚开始读取到的空输入帧。
                skip:
                while( true )
                {
                    m_AudioRecordPt.read( p_TmpInputFramePt, 0, p_TmpInputFramePt.length );

                    for( p_TmpInt32 = 0; p_TmpInt32 < p_TmpInputFramePt.length; p_TmpInt32++ )
                    {
                        if( p_TmpInputFramePt[p_TmpInt32] != 0 )
                            break skip;
                    }
                }

                p_NowDatePt = new Date();

                m_AudioOutputThreadPt.start(); //启动音频输出线程。

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频输入线程：" + "准备耗时：" + ( p_NowDatePt.getTime() - p_LastDatePt.getTime() ) + " 毫秒，丢弃掉刚开始读取到的空输入帧，现在启动音频输出线程，并开始音频输入循环，为了保证音频输入线程走在输出数据线程的前面。" );

                p_TmpInt32 = ( int ) ( ( m_AudioTrackBufSz / 2 - m_FrameLen ) * 1000 / m_SamplingRate + ( p_NowDatePt.getTime() - p_LastDatePt.getTime() ) );
                if( ( m_WebRtcAecmPt != null ) && ( m_WebRtcAecmPt.GetDelay( pclDelay ) == 0 ) && ( pclDelay.m_Val == 0 ) ) //如果使用了WebRtc定点版声学回音消除器，且需要自适应设置回音的延迟。
                {
                    m_WebRtcAecmPt.SetDelay( p_TmpInt32 / 2 );
                    m_WebRtcAecmPt.GetDelay( pclDelay );
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频输入线程：自适应设置WebRtc定点版声学回音消除器的回音延迟为 " + pclDelay.m_Val + " 毫秒。" );
                }
                if( ( m_WebRtcAecPt != null ) && ( m_WebRtcAecPt.GetDelay( pclDelay ) == 0 ) && ( pclDelay.m_Val == 0 ) ) //如果使用了WebRtc浮点版声学回音消除器，且需要自适应设置回音的延迟。
                {
                    if( m_WebRtcAecIsUseDelayAgnosticMode == 0 ) //如果WebRtc浮点版声学回音消除器不使用回音延迟不可知模式。
                    {
                        m_WebRtcAecPt.SetDelay( p_TmpInt32 );
                        m_WebRtcAecPt.GetDelay( pclDelay );
                    }
                    else //如果WebRtc浮点版声学回音消除器要使用回音延迟不可知模式。
                    {
                        m_WebRtcAecPt.SetDelay( 20 );
                        m_WebRtcAecPt.GetDelay( pclDelay );
                    }
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频输入线程：自适应设置WebRtc浮点版声学回音消除器的回音延迟为 " + pclDelay.m_Val + " 毫秒。" );
                }
                if( ( m_SpeexWebRtcAecPt != null ) && ( m_SpeexWebRtcAecPt.GetWebRtcAecmDelay( pclDelay ) == 0 ) && ( pclDelay.m_Val == 0 ) ) //如果使用了SpeexWebRtc三重声学回音消除器，且WebRtc定点版声学回音消除器需要自适应设置回音的延迟。
                {
                    m_SpeexWebRtcAecPt.SetWebRtcAecmDelay( p_TmpInt32 / 2 );
                    m_SpeexWebRtcAecPt.GetWebRtcAecmDelay( pclDelay );
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频输入线程：SpeexWebRtc三重声学回音消除器的WebRtc定点版声学回音消除器自适应设置回音延迟为 " + pclDelay.m_Val + " 毫秒。" );
                }
                if( ( m_SpeexWebRtcAecPt != null ) && ( m_SpeexWebRtcAecPt.GetWebRtcAecDelay( pclDelay ) == 0 ) && ( pclDelay.m_Val == 0 ) ) //如果使用了SpeexWebRtc三重声学回音消除器，且WebRtc浮点版声学回音消除器需要自适应设置回音的延迟。
                {
                    if( m_SpeexWebRtcAecWebRtcAecIsUseDelayAgnosticMode == 0 ) //如果SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器不使用回音延迟不可知模式。
                    {
                        m_SpeexWebRtcAecPt.SetWebRtcAecDelay( p_TmpInt32 );
                        m_SpeexWebRtcAecPt.GetWebRtcAecDelay( pclDelay );
                    }
                    else //如果SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器要使用回音延迟不可知模式。
                    {
                        m_SpeexWebRtcAecPt.SetWebRtcAecDelay( 20 );
                        m_SpeexWebRtcAecPt.GetWebRtcAecDelay( pclDelay );
                    }
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频输入线程：SpeexWebRtc三重声学回音消除器的WebRtc浮点版声学回音消除器自适应设置的回音延迟为 " + pclDelay.m_Val + " 毫秒。" );
                }

                p_LastDatePt = p_NowDatePt;
            }

            //开始音频输入循环。
            out:
            while( true )
            {
                p_TmpInputFramePt = new short[m_FrameLen];

                //读取本次输入帧。
                m_AudioRecordPt.read( p_TmpInputFramePt, 0, p_TmpInputFramePt.length );

                if( m_IsPrintLogcat != 0 )
                {
                    p_NowDatePt = new Date();
                    Log.i( m_CurClsNameStrPt, "音频输入线程：读取耗时：" + ( p_NowDatePt.getTime() - p_LastDatePt.getTime() ) + " 毫秒，" + "输入帧链表元素个数：" + m_InputFrameLnkLstPt.size() + "。" );
                    p_LastDatePt = p_NowDatePt;
                }

                //追加本次输入帧到输入帧链表。
                synchronized( m_InputFrameLnkLstPt )
                {
                    m_InputFrameLnkLstPt.addLast( p_TmpInputFramePt );
                }

                if( m_ExitFlag == 1 ) //如果退出标记为请求退出。
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频输入线程：本线程接收到退出请求，开始准备退出。" );
                    break out;
                }
            }

            if( m_IsPrintLogcat != 0 ) Log.i( m_CurClsNameStrPt, "音频输入线程：本线程已退出。" );
        }
    }

    //音频输出线程类。
    private class AudioOutputThread extends Thread
    {
        public int m_ExitFlag = 0; //本线程退出标记，0表示保持运行，1表示请求退出。

        //请求本线程退出。
        public void RequireExit()
        {
            m_ExitFlag = 1;
        }

        public void run()
        {
            this.setPriority( MAX_PRIORITY ); //设置本线程优先级。
            Process.setThreadPriority( Process.THREAD_PRIORITY_URGENT_AUDIO ); //设置本线程优先级。

            short p_TmpOutputDataPt[];
            Date p_LastDatePt = null;
            Date p_NowDatePt;
            int p_TmpInt32;

            if( m_IsPrintLogcat != 0 )
            {
                p_LastDatePt = new Date();
                Log.i( m_CurClsNameStrPt, "音频输出线程：开始准备音频输出。" );
            }

            //开始音频输出循环。
            out:
            while( true )
            {
                p_TmpOutputDataPt = new short[m_FrameLen];

                //调用用户定义的写入输出帧函数，并解码成PCM原始数据。
                switch( m_UseWhatCodec ) //使用什么编解码器。
                {
                    case 0: //如果使用PCM原始数据。
                    {
                        //调用用户定义的写入输出帧函数。
                        UserWriteOutputFrame( p_TmpOutputDataPt, null, null );

                        break;
                    }
                    case 1: //如果使用Speex编解码器。
                    {
                        byte p_SpeexOutputFramePt[] = new byte[m_FrameLen]; //Speex格式输出帧。
                        HTLong p_SpeexOutputFrameLenPt = new HTLong(); //Speex格式输出帧的数据长度，单位字节。

                        //调用用户定义的写入输出帧函数。
                        UserWriteOutputFrame( null, p_SpeexOutputFramePt, p_SpeexOutputFrameLenPt );

                        //使用Speex解码器。
                        if( p_SpeexOutputFrameLenPt.m_Val != 0 ) //如果本次Speex格式输出帧接收到了。
                        {
                            p_TmpInt32 = m_SpeexDecoderPt.Proc( p_SpeexOutputFramePt, p_SpeexOutputFrameLenPt.m_Val, p_TmpOutputDataPt );
                        }
                        else //如果本次Speex格式输出帧丢失了。
                        {
                            p_TmpInt32 = m_SpeexDecoderPt.Proc( null, 0, p_TmpOutputDataPt );
                        }
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "使用Speex解码器成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "使用Speex解码器失败。返回值：" + p_TmpInt32 );
                        }
                        break;
                    }
                    case 2: //如果使用Opus编解码器。
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "暂不支持使用Opus解码器。" );
                    }
                }

                //写入本次输出帧。
                m_AudioTrackPt.write( p_TmpOutputDataPt, 0, p_TmpOutputDataPt.length );

                //调用用户定义的获取PCM格式输出帧函数。
                UserGetPcmOutputFrame( p_TmpOutputDataPt );

                if( m_IsPrintLogcat != 0 )
                {
                    p_NowDatePt = new Date();
                    Log.i( m_CurClsNameStrPt, "音频输出线程：写入耗时：" + ( p_NowDatePt.getTime() - p_LastDatePt.getTime() ) + " 毫秒，" + "输出帧链表元素个数：" + m_OutputFrameLnkLstPt.size() + "。" );
                    p_LastDatePt = p_NowDatePt;
                }

                //追加本次输出帧到输出帧链表。
                synchronized( m_OutputFrameLnkLstPt )
                {
                    m_OutputFrameLnkLstPt.addLast( p_TmpOutputDataPt );
                }

                if( m_ExitFlag == 1 ) //如果退出标记为请求退出。
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频输出线程：本线程接收到退出请求，开始准备退出。" );
                    break out;
                }
            }

            if( m_IsPrintLogcat != 0 ) Log.i( m_CurClsNameStrPt, "音频输出线程：本线程已退出。" );
        }
    }

    //用户定义的相关函数。
    public abstract int UserInit(); //用户定义的初始化函数，在本线程刚启动时调用一次，返回值表示是否成功，为0表示成功，为非0表示失败。

    public abstract int UserProcess(); //用户定义的处理函数，在本线程运行时每隔1毫秒就调用一次，返回值表示是否成功，为0表示成功，为非0表示失败。

    public abstract int UserDestroy(); //用户定义的销毁函数，在本线程退出时调用一次，返回值表示是否重新初始化，为0表示直接退出，为非0表示重新初始化。

    public abstract int UserReadInputFrame( short PcmInputFramePt[], short PcmResultFramePt[], int VoiceActSts, byte SpeexInputFramePt[], long SpeexInputFrameLen, int SpeexInputFrameIsNeedTrans ); //用户定义的读取输入帧函数，在读取到一个输入帧并处理完后回调一次，为0表示成功，为非0表示失败。

    public abstract void UserWriteOutputFrame( short PcmOutputFramePt[], byte SpeexOutputFramePt[], HTLong SpeexOutputFrameLenPt ); //用户定义的写入输出帧函数，在需要写入一个输出帧时回调一次。注意：本函数不是在音频处理线程中执行的，而是在音频输出线程中执行的，所以本函数应尽量在一瞬间完成执行，否则会导致音频输入输出帧不同步，从而导致回音消除失败。

    public abstract void UserGetPcmOutputFrame( short PcmOutputFramePt[] ); //用户定义的获取PCM格式输出帧函数，在解码完一个输出帧时回调一次。注意：本函数不是在音频处理线程中执行的，而是在音频输出线程中执行的，所以本函数应尽量在一瞬间完成执行，否则会导致音频输入输出帧不同步，从而导致回音消除失败。

    //请求本线程退出。
    public void RequireExit()
    {
        m_ExitFlag = 1;
    }

    //初始化音频处理线程类对象。
    public int Init( Context AppContextPt, int SamplingRate, int FrameMsLen )
    {
        int p_Result = -1; //存放本函数执行结果的值，0表示成功，非0表示失败。

        out:
        {
            //判断各个变量是否正确。
            if( ( AppContextPt == null ) || //如果上下文类对象不正确。
                    ( SamplingRate != 8000 ) && ( SamplingRate != 16000 ) && ( SamplingRate != 32000 ) || //如果采样频率不正确。
                    ( ( FrameMsLen == 0 ) || ( FrameMsLen % 10 != 0 ) ) ) //如果帧的毫秒长度不正确。
            {
                break out;
            }

            m_AppContextPt = AppContextPt; //设置应用程序上下文类对象的内存指针。
            m_SamplingRate = SamplingRate; //设置采样频率。
            m_FrameLen = FrameMsLen * SamplingRate / 1000; //设置帧的数据长度。

            p_Result = 0;
        }

        return p_Result;
    }

    //设置打印日志。
    public void SetPrintLogcat( int IsPrintLogcat )
    {
        m_IsPrintLogcat = IsPrintLogcat;
    }

    //设置使用唤醒锁。
    public void SetUseWakeLock( int IsUseWakeLock )
    {
        m_IsUseWakeLock = IsUseWakeLock;
    }

    //设置不使用声学回音消除器。
    public void SetUseNoAec()
    {
        m_UseWhatAec = 0;
    }

    //设置使用Speex声学回音消除器。
    public void SetUseSpeexAec( int FilterLength, int IsSaveMemoryFile, String MemFileFullPathStrPt )
    {
        m_UseWhatAec = 1;
        m_SpeexAecFilterLen = FilterLength;
        m_SpeexAecIsSaveMemFile = IsSaveMemoryFile;
        m_SpeexAecMemFileFullPathStrPt = MemFileFullPathStrPt;
    }

    //设置使用WebRtc定点版声学回音消除器。
    public void SetUseWebRtcAecm( int IsUseCNGMode, int EchoMode, int Delay )
    {
        m_UseWhatAec = 2;
        m_WebRtcAecmIsUseCNGMode = IsUseCNGMode;
        m_WebRtcAecmEchoMode = EchoMode;
        m_WebRtcAecmDelay = Delay;
    }

    //设置使用WebRtc浮点版声学回音消除器。
    public void SetUseWebRtcAec( int EchoMode, int Delay, int IsUseDelayAgnosticMode, int IsUseExtdFilterMode, int IsUseRefinedFilterAdaptAecMode, int IsUseAdaptiveAdjustDelay, int IsSaveMemoryFile, String MemFileFullPathStrPt )
    {
        m_UseWhatAec = 3;
        m_WebRtcAecEchoMode = EchoMode;
        m_WebRtcAecDelay = Delay;
        m_WebRtcAecIsUseDelayAgnosticMode = IsUseDelayAgnosticMode;
        m_WebRtcAecIsUseExtdFilterMode = IsUseExtdFilterMode;
        m_WebRtcAecIsUseRefinedFilterAdaptAecMode = IsUseRefinedFilterAdaptAecMode;
        m_WebRtcAecIsUseAdaptAdjDelay = IsUseAdaptiveAdjustDelay;
        m_WebRtcAecIsSaveMemFile = IsSaveMemoryFile;
        m_WebRtcAecMemFileFullPathStrPt = MemFileFullPathStrPt;
    }

    //设置使用SpeexWebRtc三重声学回音消除器。
    public void SetUseSpeexWebRtcAec( int WorkMode, int SpeexAecFilterLength, float SpeexAecEchoMultiple, float SpeexAecEchoCont, int SpeexAecEchoSuppress, int SpeexAecEchoSuppressActive, int WebRtcAecmIsUseCNGMode, int WebRtcAecmEchoMode, int WebRtcAecmDelay, int WebRtcAecEchoMode, int WebRtcAecDelay, int WebRtcAecIsUseDelayAgnosticMode, int WebRtcAecIsUseExtdFilterMode, int WebRtcAecIsUseRefinedFilterAdaptAecMode, int WebRtcAecIsUseAdaptAdjDelay )
    {
        m_UseWhatAec = 4;
        m_SpeexWebRtcAecWorkMode = WorkMode;
        m_SpeexWebRtcAecSpeexAecFilterLen = SpeexAecFilterLength;
        m_SpeexWebRtcAecSpeexAecEchoMultiple = SpeexAecEchoMultiple;
        m_SpeexWebRtcAecSpeexAecEchoCont = SpeexAecEchoCont;
        m_SpeexWebRtcAecSpeexAecEchoSupes = SpeexAecEchoSuppress;
        m_SpeexWebRtcAecSpeexAecEchoSupesAct = SpeexAecEchoSuppressActive;
        m_SpeexWebRtcAecWebRtcAecmIsUseCNGMode = WebRtcAecmIsUseCNGMode;
        m_SpeexWebRtcAecWebRtcAecmEchoMode = WebRtcAecmEchoMode;
        m_SpeexWebRtcAecWebRtcAecmDelay = WebRtcAecmDelay;
        m_SpeexWebRtcAecWebRtcAecEchoMode = WebRtcAecEchoMode;
        m_SpeexWebRtcAecWebRtcAecDelay = WebRtcAecDelay;
        m_SpeexWebRtcAecWebRtcAecIsUseDelayAgnosticMode = WebRtcAecIsUseDelayAgnosticMode;
        m_SpeexWebRtcAecWebRtcAecIsUseExtdFilterMode = WebRtcAecIsUseExtdFilterMode;
        m_SpeexWebRtcAecWebRtcAecIsUseRefinedFilterAdaptAecMode = WebRtcAecIsUseRefinedFilterAdaptAecMode;
        m_SpeexWebRtcAecWebRtcAecIsUseAdaptAdjDelay = WebRtcAecIsUseAdaptAdjDelay;
    }

    //设置不使用噪音抑制器。
    public void SetUseNoNs()
    {
        m_UseWhatNs = 0;
    }

    //设置使用Speex预处理器的噪音抑制。
    public void SetUseSpeexPprocNs( int IsUseNs, int NoiseSuppress, int IsUseDereverberation, int IsUseRec, float EchoMultiple, float EchoCont, int EchoSuppress, int EchoSuppressActive )
    {
        m_UseWhatNs = 1;
        m_SpeexPprocIsUseNs = IsUseNs;
        m_SpeexPprocNoiseSupes = NoiseSuppress;
        m_SpeexPprocIsUseDereverb = IsUseDereverberation;
        m_SpeexPprocIsUseRec = IsUseRec;
        m_SpeexPprocEchoMultiple = EchoMultiple;
        m_SpeexPprocEchoCont = EchoCont;
        m_SpeexPprocEchoSupes = EchoSuppress;
        m_SpeexPprocEchoSupesAct = EchoSuppressActive;
    }

    //设置使用WebRtc定点版噪音抑制器。
    public void SetUseWebRtcNsx( int PolicyMode )
    {
        m_UseWhatNs = 2;
        m_WebRtcNsxPolicyMode = PolicyMode;
    }

    //设置使用WebRtc定点版噪音抑制器。
    public void SetUseWebRtcNs( int PolicyMode )
    {
        m_UseWhatNs = 3;
        m_WebRtcNsPolicyMode = PolicyMode;
    }

    //设置使用RNNoise噪音抑制器。
    public void SetUseRNNoise()
    {
        m_UseWhatNs = 4;
    }

    //设置Speex预处理器的其他功能。
    public void SetSpeexPprocOther( int IsUseOther, int IsUseVad, int VadProbStart, int VadProbCont, int IsUseAgc, int AgcLevel, int AgcIncrement, int AgcDecrement, int AgcMaxGain )
    {
        m_IsUseSpeexPprocOther = IsUseOther;
        m_SpeexPprocIsUseVad = IsUseVad;
        m_SpeexPprocVadProbStart = VadProbStart;
        m_SpeexPprocVadProbCont = VadProbCont;
        m_SpeexPprocIsUseAgc = IsUseAgc;
        m_SpeexPprocAgcIncrement = AgcIncrement;
        m_SpeexPprocAgcDecrement = AgcDecrement;
        m_SpeexPprocAgcLevel = AgcLevel;
        m_SpeexPprocAgcMaxGain = AgcMaxGain;
    }

    //设置使用PCM原始数据。
    public void SetUsePcm()
    {
        m_UseWhatCodec = 0;
    }

    //设置使用Speex编解码器。
    public void SetUseSpeexCodec( int EncoderUseCbrOrVbr, int EncoderQuality, int EncoderComplexity, int EncoderPlcExpectedLossRate, int DecoderIsUsePerceptualEnhancement )
    {
        m_UseWhatCodec = 1;
        m_SpeexCodecEncoderUseCbrOrVbr = EncoderUseCbrOrVbr;
        m_SpeexCodecEncoderQuality = EncoderQuality;
        m_SpeexCodecEncoderComplexity = EncoderComplexity;
        m_SpeexCodecEncoderPlcExpectedLossRate = EncoderPlcExpectedLossRate;
        m_SpeexCodecDecoderIsUsePerceptualEnhancement = DecoderIsUsePerceptualEnhancement;
    }

    //设置使用Opus编解码器。
    public void SetUseOpusCodec()
    {
        m_UseWhatCodec = 2;
    }

    //设置保存音频到文件。
    public void SetSaveAudioToFile( int IsSaveAudioToFile, String AudioInputFileFullPathStrPt, String AudioOutputFileFullPathStrPt, String AudioResultFileFullPathStrPt )
    {
        m_IsSaveAudioToFile = IsSaveAudioToFile;
        m_AudioInputFileFullPathStrPt = AudioInputFileFullPathStrPt;
        m_AudioOutputFileFullPathStrPt = AudioOutputFileFullPathStrPt;
        m_AudioResultFileFullPathStrPt = AudioResultFileFullPathStrPt;
    }

    //本线程执行函数。
    public void run()
    {
        this.setPriority( this.MAX_PRIORITY ); //设置本线程优先级。
        Process.setThreadPriority( Process.THREAD_PRIORITY_URGENT_AUDIO ); //设置本线程优先级。

        int p_TmpInt32;
        Date p_LastDatePt;
        Date p_NowDatePt;

        ReInit:
        while( true )
        {
            out:
            {
                p_LastDatePt = new Date(); //记录初始化开始的时间。

                m_ExitCode = -1; //先将本线程退出代码预设为初始化失败，如果初始化失败，这个退出代码就不用再设置了，如果初始化成功，再设置为成功的退出代码。

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：本地代码的指令集的名称（CPU类型+ ABI约定）为" + android.os.Build.CPU_ABI + "。" );

                //初始化唤醒锁类对象。
                if( m_IsUseWakeLock != 0 )
                {
                    //初始化接近息屏唤醒锁类对象。
                    m_ProximityScreenOffWakeLockPt = ( ( PowerManager ) m_AppContextPt.getSystemService( Activity.POWER_SERVICE ) ).newWakeLock( PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, m_CurClsNameStrPt );
                    if( m_ProximityScreenOffWakeLockPt != null )
                    {
                        m_ProximityScreenOffWakeLockPt.acquire();

                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：初始化接近息屏唤醒锁类对象成功。" );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "音频处理线程：初始化接近息屏唤醒锁类对象失败。" );
                        break out;
                    }

                    //初始化屏幕键盘全亮唤醒锁类对象。
                    m_FullWakeLockPt = ( ( PowerManager ) m_AppContextPt.getSystemService( Activity.POWER_SERVICE ) ).newWakeLock( PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, m_CurClsNameStrPt );
                    if( m_FullWakeLockPt != null )
                    {
                        m_FullWakeLockPt.acquire();

                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：初始化屏幕键盘全亮唤醒锁类对象成功。" );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "音频处理线程：初始化屏幕键盘全亮唤醒锁类对象成功。" );
                        break out;
                    }
                }

                //调用用户定义的初始化函数。
                p_TmpInt32 = UserInit();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：调用用户定义的初始化函数成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：调用用户定义的初始化函数失败。返回值：" + p_TmpInt32 );
                    break out;
                }

                //初始化音频输入类对象。
                try
                {
                    m_AudioRecordBufSz = AudioRecord.getMinBufferSize( m_SamplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT );
                    m_AudioRecordBufSz = ( int ) ( m_AudioRecordBufSz > m_FrameLen * 2 ? m_AudioRecordBufSz : m_FrameLen * 2 );
                    m_AudioRecordPt = new AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            m_SamplingRate,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            m_AudioRecordBufSz
                    );
                    if( m_AudioRecordPt.getState() == AudioRecord.STATE_INITIALIZED )
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：初始化音频输入类对象成功。音频输入缓冲区大小：" + m_AudioRecordBufSz );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "音频处理线程：初始化音频输入类对象失败。" );
                        break out;
                    }
                }
                catch( IllegalArgumentException e )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：初始化音频输入类对象失败。原因：" + e.getMessage() );
                    break out;
                }

                //用第一种方法初始化音频输出类对象。
                try
                {
                    m_AudioTrackBufSz = m_FrameLen * 2;
                    m_AudioTrackPt = new AudioTrack( AudioManager.STREAM_MUSIC,
                            m_SamplingRate,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            m_AudioTrackBufSz,
                            AudioTrack.MODE_STREAM );
                    if( m_AudioTrackPt.getState() == AudioTrack.STATE_INITIALIZED )
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：用第一种方法初始化音频输出类对象成功。音频输出缓冲区大小：" + m_AudioTrackBufSz );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "音频处理线程：用第一种方法初始化音频输出类对象失败。" );
                        m_AudioTrackPt.release();
                        m_AudioTrackPt = null;
                    }
                }
                catch( IllegalArgumentException e )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：用第一种方法初始化音频输出类对象失败。原因：" + e.getMessage() );
                }

                //用第二种方法初始化音频输出类对象。
                if( m_AudioTrackPt == null )
                {
                    try
                    {
                        m_AudioTrackBufSz = AudioTrack.getMinBufferSize( m_SamplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT );
                        m_AudioTrackPt = new AudioTrack( AudioManager.STREAM_MUSIC,
                                m_SamplingRate,
                                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                m_AudioTrackBufSz,
                                AudioTrack.MODE_STREAM );
                        if( m_AudioTrackPt.getState() == AudioTrack.STATE_INITIALIZED )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：用第二种方法初始化音频输出类对象成功。音频输出缓冲区大小：" + m_AudioTrackBufSz );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：用第二种方法初始化音频输出类对象失败。" );
                            break out;
                        }
                    }
                    catch( IllegalArgumentException e )
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "音频处理线程：用第二种方法初始化音频输出类对象失败。原因：" + e.getMessage() );
                        break out;
                    }
                }

                //初始化声学回音消除器对象。
                switch( m_UseWhatAec )
                {
                    case 0: //如果不使用声学回音消除器。
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：不使用声学回音消除器。" );
                        break;
                    }
                    case 1: //如果使用Speex声学回音消除器。
                    {
                        //读取Speex声学回音消除器的内存块到文件。
                        if( ( m_SpeexAecIsSaveMemFile != 0 ) && ( new File( m_SpeexAecMemFileFullPathStrPt ).exists() ) )
                        {
                            byte p_SpeexAecMemPt[];
                            long p_SpeexAecMemLen;
                            FileInputStream p_SpeexAecMemFileInputStreamPt = null;

                            ReadSpeexAecMemoryFile:
                            {
                                try
                                {
                                    p_SpeexAecMemFileInputStreamPt = new FileInputStream( m_SpeexAecMemFileFullPathStrPt );
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：创建Speex声学回音消除器内存块文件 " + m_SpeexAecMemFileFullPathStrPt + " 的文件输入流对象成功。" );
                                }
                                catch( FileNotFoundException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：创建Speex声学回音消除器内存块文件 " + m_SpeexAecMemFileFullPathStrPt + " 的文件输入流对象失败。原因：" + e.toString() );
                                    break ReadSpeexAecMemoryFile;
                                }

                                p_SpeexAecMemPt = new byte[8];

                                //读取Speex声学回音消除器内存块文件的采样频率。
                                try
                                {
                                    if( p_SpeexAecMemFileInputStreamPt.read( p_SpeexAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有采样频率。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器内存块文件的采样频率成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器内存块文件的采样频率失败。原因：" + e.toString() );
                                    break ReadSpeexAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_SpeexAecMemPt[0] & 0xFF ) + ( ( ( int ) p_SpeexAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_SpeexAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_SpeexAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_SamplingRate )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：Speex声学回音消除器内存块文件中的采样频率已被修改，需要重新初始化。" );
                                    break ReadSpeexAecMemoryFile;
                                }

                                //读取Speex声学回音消除器内存块文件的帧数据长度。
                                try
                                {
                                    if( p_SpeexAecMemFileInputStreamPt.read( p_SpeexAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有帧的数据长度。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器内存块文件的帧数据长度成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器内存块文件的帧数据长度失败。原因：" + e.toString() );
                                    break ReadSpeexAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_SpeexAecMemPt[0] & 0xFF ) + ( ( ( int ) p_SpeexAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_SpeexAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_SpeexAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_FrameLen )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：Speex声学回音消除器内存块文件中的帧数据长度已被修改，需要重新初始化。" );
                                    break ReadSpeexAecMemoryFile;
                                }

                                //读取Speex声学回音消除器内存块文件的滤波器数据长度。
                                try
                                {
                                    if( p_SpeexAecMemFileInputStreamPt.read( p_SpeexAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有滤波器的数据长度。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器内存块文件的滤波器数据长度成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器内存块文件的滤波器数据长度失败。原因：" + e.toString() );
                                    break ReadSpeexAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_SpeexAecMemPt[0] & 0xFF ) + ( ( ( int ) p_SpeexAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_SpeexAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_SpeexAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_SpeexAecFilterLen )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：Speex声学回音消除器内存块文件中的滤波器数据长度已被修改，需要重新初始化。" );
                                    break ReadSpeexAecMemoryFile;
                                }

                                //跳过Speex声学回音消除器内存块文件的有语音活动帧总数。
                                try
                                {
                                    if( p_SpeexAecMemFileInputStreamPt.skip( 8 ) != 8 )
                                    {
                                        throw new IOException( "文件中没有有语音活动帧总数。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：跳过Speex声学回音消除器内存块文件的有语音活动帧总数成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：跳过Speex声学回音消除器内存块文件的有语音活动帧总数失败。原因：" + e.toString() );
                                    break ReadSpeexAecMemoryFile;
                                }

                                //读取Speex声学回音消除器内存块文件的内存块。
                                try
                                {
                                    p_SpeexAecMemLen = p_SpeexAecMemFileInputStreamPt.available();
                                    if( p_SpeexAecMemLen <= 0 )
                                    {
                                        throw new IOException( "文件中没有内存块。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：获取Speex声学回音消除器内存块文件的内存块数据长度成功。内存块数据长度：" + p_SpeexAecMemLen );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：获取Speex声学回音消除器内存块文件的内存块数据长度失败。原因：" + e.toString() );
                                    break ReadSpeexAecMemoryFile;
                                }
                                p_SpeexAecMemPt = new byte[( int ) p_SpeexAecMemLen];
                                try
                                {
                                    if( p_SpeexAecMemFileInputStreamPt.read( p_SpeexAecMemPt, 0, ( int ) p_SpeexAecMemLen ) != p_SpeexAecMemLen )
                                    {
                                        throw new IOException( "文件中没有内存块。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器内存块文件中的内存块成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器内存块文件中的内存块失败。原因：" + e.toString() );
                                    break ReadSpeexAecMemoryFile;
                                }

                                m_SpeexAecPt = new SpeexAec();
                                p_TmpInt32 = m_SpeexAecPt.InitFromMem( p_SpeexAecMemPt, p_SpeexAecMemLen );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：根据Speex声学回音消除器内存块来初始化Speex声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                                }
                                else
                                {
                                    m_SpeexAecPt = null;

                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：根据Speex声学回音消除器内存块来初始化Speex声学回音消除器类对象失败，重新初始化。返回值：" + p_TmpInt32 );
                                    break ReadSpeexAecMemoryFile;
                                }
                            }

                            //销毁Speex声学回音消除器内存块文件的文件输入流对象。
                            if( p_SpeexAecMemFileInputStreamPt != null )
                            {
                                try
                                {
                                    p_SpeexAecMemFileInputStreamPt.close();
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁Speex声学回音消除器内存块文件的文件输入流对象成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁Speex声学回音消除器内存块文件的文件输入流对象失败。原因：" + e.toString() );
                                }
                            }
                        }

                        if( m_SpeexAecPt == null )
                        {
                            m_SpeexAecPt = new SpeexAec();
                            p_TmpInt32 = m_SpeexAecPt.Init( m_SamplingRate, m_FrameLen, m_SpeexAecFilterLen );
                            if( p_TmpInt32 == 0 )
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：初始化Speex声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                            }
                            else
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：初始化Speex声学回音消除器类对象失败。返回值：" + p_TmpInt32 );
                                break out;
                            }
                        }
                        break;
                    }
                    case 2: //如果使用WebRtc定点版声学回音消除器。
                    {
                        m_WebRtcAecmPt = new WebRtcAecm();
                        p_TmpInt32 = m_WebRtcAecmPt.Init( m_SamplingRate, m_FrameLen, m_WebRtcAecmIsUseCNGMode, m_WebRtcAecmEchoMode, m_WebRtcAecmDelay );
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：初始化WebRtc定点版声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：初始化WebRtc定点版声学回音消除器类对象失败。返回值：" + p_TmpInt32 );
                            break out;
                        }
                        break;
                    }
                    case 3: //如果使用WebRtc浮点版声学回音消除器。
                    {
                        //读取WebRtc浮点版声学回音消除器的内存块到文件。
                        if( ( m_WebRtcAecIsSaveMemFile != 0 ) && ( new File( m_WebRtcAecMemFileFullPathStrPt ).exists() ) )
                        {
                            byte p_WebRtcAecMemPt[];
                            long p_WebRtcAecMemLen;
                            FileInputStream p_WebRtcAecMemFileInputStreamPt = null;

                            ReadWebRtcAecMemoryFile:
                            {
                                try
                                {
                                    p_WebRtcAecMemFileInputStreamPt = new FileInputStream( m_WebRtcAecMemFileFullPathStrPt );
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：创建WebRtc浮点版声学回音消除器内存块文件 " + m_WebRtcAecMemFileFullPathStrPt + " 的文件输入流对象成功。" );
                                }
                                catch( FileNotFoundException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：创建WebRtc浮点版声学回音消除器内存块文件 " + m_WebRtcAecMemFileFullPathStrPt + " 的文件输入流对象失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                p_WebRtcAecMemPt = new byte[8];

                                //读取WebRtc浮点版声学回音消除器内存块文件的采样频率。
                                try
                                {
                                    if( p_WebRtcAecMemFileInputStreamPt.read( p_WebRtcAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有采样频率。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的采样频率成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的采样频率失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_WebRtcAecMemPt[0] & 0xFF ) + ( ( ( int ) p_WebRtcAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_WebRtcAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_WebRtcAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_SamplingRate )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：WebRtc浮点版声学回音消除器内存块文件中的采样频率已被修改，需要重新初始化。" );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                //读取WebRtc浮点版声学回音消除器内存块文件的帧数据长度。
                                try
                                {
                                    if( p_WebRtcAecMemFileInputStreamPt.read( p_WebRtcAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有帧的数据长度。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的帧数据长度成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的帧数据长度失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_WebRtcAecMemPt[0] & 0xFF ) + ( ( ( int ) p_WebRtcAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_WebRtcAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_WebRtcAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_FrameLen )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：WebRtc浮点版声学回音消除器内存块文件中的帧数据长度已被修改，需要重新初始化。" );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                //读取WebRtc浮点版声学回音消除器内存块文件的消除模式。
                                try
                                {
                                    if( p_WebRtcAecMemFileInputStreamPt.read( p_WebRtcAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有消除模式。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的消除模式成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的消除模式失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_WebRtcAecMemPt[0] & 0xFF ) + ( ( ( int ) p_WebRtcAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_WebRtcAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_WebRtcAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_WebRtcAecEchoMode )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：WebRtc浮点版声学回音消除器内存块文件中的消除模式已被修改，需要重新初始化。" );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                //读取WebRtc浮点版声学回音消除器内存块文件的回音延迟。
                                try
                                {
                                    if( p_WebRtcAecMemFileInputStreamPt.read( p_WebRtcAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有消除模式。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的回音延迟成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的回音延迟失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_WebRtcAecMemPt[0] & 0xFF ) + ( ( ( int ) p_WebRtcAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_WebRtcAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_WebRtcAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_WebRtcAecDelay )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：WebRtc浮点版声学回音消除器内存块文件中的回音延迟已被修改，需要重新初始化。" );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                //读取WebRtc浮点版声学回音消除器内存块文件的是否使用回音延迟不可知模式。
                                try
                                {
                                    if( p_WebRtcAecMemFileInputStreamPt.read( p_WebRtcAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有消除模式。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的是否使用回音延迟不可知模式成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的是否使用回音延迟不可知模式失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_WebRtcAecMemPt[0] & 0xFF ) + ( ( ( int ) p_WebRtcAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_WebRtcAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_WebRtcAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_WebRtcAecIsUseDelayAgnosticMode )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：WebRtc浮点版声学回音消除器内存块文件中的是否使用回音延迟不可知模式已被修改，需要重新初始化。" );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                //读取WebRtc浮点版声学回音消除器内存块文件的是否使用自适应调节回音的延迟。
                                try
                                {
                                    if( p_WebRtcAecMemFileInputStreamPt.read( p_WebRtcAecMemPt, 0, 4 ) != 4 )
                                    {
                                        throw new IOException( "文件中没有消除模式。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的是否使用自适应调节回音的延迟成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件的是否使用自适应调节回音的延迟失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }
                                p_TmpInt32 = ( ( int ) p_WebRtcAecMemPt[0] & 0xFF ) + ( ( ( int ) p_WebRtcAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( int ) p_WebRtcAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( int ) p_WebRtcAecMemPt[3] & 0xFF ) << 24 );
                                if( p_TmpInt32 != m_WebRtcAecIsUseAdaptAdjDelay )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：WebRtc浮点版声学回音消除器内存块文件中的是否使用自适应调节回音的延迟已被修改，需要重新初始化。" );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                //跳过WebRtc浮点版声学回音消除器内存块文件的有语音活动帧总数。
                                try
                                {
                                    if( p_WebRtcAecMemFileInputStreamPt.skip( 8 ) != 8 )
                                    {
                                        throw new IOException( "文件中没有有语音活动帧总数。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：跳过WebRtc浮点版声学回音消除器内存块文件的有语音活动帧总数成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：跳过WebRtc浮点版声学回音消除器内存块文件的有语音活动帧总数失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                //读取WebRtc浮点版声学回音消除器内存块文件的内存块。
                                try
                                {
                                    p_WebRtcAecMemLen = p_WebRtcAecMemFileInputStreamPt.available();
                                    if( p_WebRtcAecMemLen <= 0 )
                                    {
                                        throw new IOException( "文件中没有内存块。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：获取WebRtc浮点版声学回音消除器内存块文件的内存块数据长度成功。内存块数据长度：" + p_WebRtcAecMemLen );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：获取WebRtc浮点版声学回音消除器内存块文件的内存块数据长度失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }
                                p_WebRtcAecMemPt = new byte[( int ) p_WebRtcAecMemLen];
                                try
                                {
                                    if( p_WebRtcAecMemFileInputStreamPt.read( p_WebRtcAecMemPt, 0, ( int ) p_WebRtcAecMemLen ) != p_WebRtcAecMemLen )
                                    {
                                        throw new IOException( "文件中没有内存块。" );
                                    }
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件中的内存块成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器内存块文件中的内存块失败。原因：" + e.toString() );
                                    break ReadWebRtcAecMemoryFile;
                                }

                                m_WebRtcAecPt = new WebRtcAec();
                                p_TmpInt32 = m_WebRtcAecPt.InitFromMem( p_WebRtcAecMemPt, p_WebRtcAecMemLen );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：根据WebRtc浮点版声学回音消除器内存块来初始化WebRtc浮点版声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                                }
                                else
                                {
                                    m_WebRtcAecPt = null;

                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：根据WebRtc浮点版声学回音消除器内存块来初始化WebRtc浮点版声学回音消除器类对象失败，重新初始化。返回值：" + p_TmpInt32 );
                                    break ReadWebRtcAecMemoryFile;
                                }
                            }

                            //销毁WebRtc浮点版声学回音消除器内存块文件的文件输入流对象。
                            if( p_WebRtcAecMemFileInputStreamPt != null )
                            {
                                try
                                {
                                    p_WebRtcAecMemFileInputStreamPt.close();
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版声学回音消除器内存块文件的文件输入流对象成功。" );
                                }
                                catch( IOException e )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版声学回音消除器内存块文件的文件输入流对象失败。原因：" + e.toString() );
                                }
                            }
                        }

                        if( m_WebRtcAecPt == null )
                        {
                            m_WebRtcAecPt = new WebRtcAec();
                            p_TmpInt32 = m_WebRtcAecPt.Init( m_SamplingRate, m_FrameLen, m_WebRtcAecEchoMode, m_WebRtcAecDelay, m_WebRtcAecIsUseDelayAgnosticMode, m_WebRtcAecIsUseExtdFilterMode, m_WebRtcAecIsUseRefinedFilterAdaptAecMode, m_WebRtcAecIsUseAdaptAdjDelay );
                            if( p_TmpInt32 == 0 )
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：初始化WebRtc浮点版声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                            }
                            else
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：初始化WebRtc浮点版声学回音消除器类对象失败。返回值：" + p_TmpInt32 );
                                break out;
                            }
                        }
                        break;
                    }
                    case 4: //如果使用SpeexWebRtc三重声学回音消除器。
                    {
                        m_SpeexWebRtcAecPt = new SpeexWebRtcAec();
                        p_TmpInt32 = m_SpeexWebRtcAecPt.Init( m_SamplingRate, m_FrameLen, m_SpeexWebRtcAecWorkMode, m_SpeexWebRtcAecSpeexAecFilterLen, m_SpeexWebRtcAecSpeexAecEchoMultiple, m_SpeexWebRtcAecSpeexAecEchoCont, m_SpeexWebRtcAecSpeexAecEchoSupes, m_SpeexWebRtcAecSpeexAecEchoSupesAct, m_SpeexWebRtcAecWebRtcAecmIsUseCNGMode, m_SpeexWebRtcAecWebRtcAecmEchoMode, m_SpeexWebRtcAecWebRtcAecmDelay, m_SpeexWebRtcAecWebRtcAecEchoMode, m_SpeexWebRtcAecWebRtcAecDelay, m_SpeexWebRtcAecWebRtcAecIsUseDelayAgnosticMode, m_SpeexWebRtcAecWebRtcAecIsUseExtdFilterMode, m_SpeexWebRtcAecWebRtcAecIsUseRefinedFilterAdaptAecMode, m_SpeexWebRtcAecWebRtcAecIsUseAdaptAdjDelay );
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：初始化SpeexWebRtc三重声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：初始化SpeexWebRtc三重声学回音消除器类对象失败。返回值：" + p_TmpInt32 );
                            break out;
                        }
                        break;
                    }
                }

                //初始化噪音抑制器对象。
                switch( m_UseWhatNs )
                {
                    case 0: //如果不使用噪音抑制器。
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：不使用噪音抑制器。" );
                        break;
                    }
                    case 1: //如果使用Speex预处理器的噪音抑制。
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：稍后在初始化Speex预处理器时一起初始化Speex预处理器的噪音抑制。" );
                        break;
                    }
                    case 2: //如果使用WebRtc定点版噪音抑制器。
                    {
                        m_WebRtcNsxPt = new WebRtcNsx();
                        p_TmpInt32 = m_WebRtcNsxPt.Init( m_SamplingRate, m_FrameLen, m_WebRtcNsxPolicyMode );
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：初始化WebRtc定点版噪音抑制器类对象成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：初始化WebRtc定点版噪音抑制器类对象失败。返回值：" + p_TmpInt32 );
                            break out;
                        }
                        break;
                    }
                    case 3: //如果使用WebRtc浮点版噪音抑制器。
                    {
                        m_WebRtcNsPt = new WebRtcNs();
                        p_TmpInt32 = m_WebRtcNsPt.Init( m_SamplingRate, m_FrameLen, m_WebRtcNsxPolicyMode );
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：初始化WebRtc浮点版噪音抑制器类对象成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：初始化WebRtc浮点版噪音抑制器类对象失败。返回值：" + p_TmpInt32 );
                            break out;
                        }
                        break;
                    }
                    case 4: //如果使用RNNoise噪音抑制器。
                    {
                        m_RNNoisePt = new RNNoise();
                        p_TmpInt32 = m_RNNoisePt.Init( m_SamplingRate, m_FrameLen );
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：初始化RNNoise噪音抑制器类对象成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：初始化RNNoise噪音抑制器类对象失败。返回值：" + p_TmpInt32 );
                            break out;
                        }
                        break;
                    }
                }

                //初始化Speex预处理器类对象。
                if( ( m_UseWhatNs == 1 ) || ( m_IsUseSpeexPprocOther != 0 ) )
                {
                    if( m_UseWhatNs != 1 )
                    {
                        m_SpeexPprocIsUseNs = 0;
                        m_SpeexPprocIsUseDereverb = 0;
                        m_SpeexPprocIsUseRec = 0;
                    }

                    if( m_IsUseSpeexPprocOther == 0 )
                    {
                        m_SpeexPprocIsUseVad = 0;
                        m_SpeexPprocIsUseAgc = 0;
                    }

                    m_SpeexPprocPt = new SpeexPproc();
                    if( m_SpeexAecPt != null )
                        p_TmpInt32 = m_SpeexPprocPt.Init( m_SamplingRate, m_FrameLen, m_SpeexPprocIsUseNs, m_SpeexPprocNoiseSupes, m_SpeexPprocIsUseDereverb, m_SpeexPprocIsUseVad, m_SpeexPprocVadProbStart, m_SpeexPprocVadProbCont, m_SpeexPprocIsUseAgc, m_SpeexPprocAgcLevel, m_SpeexPprocAgcIncrement, m_SpeexPprocAgcDecrement, m_SpeexPprocAgcMaxGain, m_SpeexPprocIsUseRec, m_SpeexAecPt.GetSpeexAecPt(), m_SpeexPprocEchoMultiple, m_SpeexPprocEchoCont, m_SpeexPprocEchoSupes, m_SpeexPprocEchoSupesAct );
                    else
                        p_TmpInt32 = m_SpeexPprocPt.Init( m_SamplingRate, m_FrameLen, m_SpeexPprocIsUseNs, m_SpeexPprocNoiseSupes, m_SpeexPprocIsUseDereverb, m_SpeexPprocIsUseVad, m_SpeexPprocVadProbStart, m_SpeexPprocVadProbCont, m_SpeexPprocIsUseAgc, m_SpeexPprocAgcLevel, m_SpeexPprocAgcIncrement, m_SpeexPprocAgcDecrement, m_SpeexPprocAgcMaxGain, 0, 0, 0, 0, 0, 0 );
                    if( p_TmpInt32 == 0 )
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：初始化Speex预处理器类对象成功。返回值：" + p_TmpInt32 );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "音频处理线程：初始化Speex预处理器类对象失败。返回值：" + p_TmpInt32 );
                        break out;
                    }
                }

                //初始化编解码器对象。
                switch( m_UseWhatCodec )
                {
                    case 0: //如果使用PCM原始数据。
                    {
                        //什么都不要做。
                        break;
                    }
                    case 1: //如果使用Speex编解码器。
                    {
                        if( m_FrameLen != m_SamplingRate / 50 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：帧的数据长度不为20毫秒不能使用Speex编解码器。" );
                            break out;
                        }

                        m_SpeexEncoderPt = new SpeexEncoder();
                        p_TmpInt32 = m_SpeexEncoderPt.Init( m_SamplingRate, m_SpeexCodecEncoderUseCbrOrVbr, m_SpeexCodecEncoderQuality, m_SpeexCodecEncoderComplexity, m_SpeexCodecEncoderPlcExpectedLossRate );
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：初始化Speex编码器类对象成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：初始化Speex编码器类对象失败。返回值：" + p_TmpInt32 );
                            break out;
                        }

                        m_SpeexDecoderPt = new SpeexDecoder();
                        p_TmpInt32 = m_SpeexDecoderPt.Init( m_SamplingRate, m_SpeexCodecDecoderIsUsePerceptualEnhancement );
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：初始化Speex解码器类对象成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：初始化Speex解码器类对象失败。返回值：" + p_TmpInt32 );
                            break out;
                        }
                        break;
                    }
                    case 2: //如果使用Opus编解码器。
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "音频处理线程：暂不支持使用Opus编解码器。" );
                        break out;
                    }
                }

                //初始化各个音频文件的文件输出流对象。
                if( m_IsSaveAudioToFile != 0 )
                {
                    //创建并初始化音频输入Wave文件写入器对象。
                    m_AudioInputWaveFileWriterPt = new WaveFileWriter();
                    if( m_AudioInputWaveFileWriterPt.Init( ( m_AudioInputFileFullPathStrPt + "\0" ).getBytes(), ( short ) 1, m_SamplingRate, 16 ) == 0 )
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "创建音频输入文件 " + m_AudioInputFileFullPathStrPt + " 的Wave文件写入器对象成功。" );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "创建音频输入文件 " + m_AudioInputFileFullPathStrPt + " 的Wave文件写入器对象失败。" );
                        break out;
                    }

                    //创建并初始化音频输出Wave文件写入器对象。
                    m_AudioOutputWaveFileWriterPt = new WaveFileWriter();
                    if( m_AudioOutputWaveFileWriterPt.Init( ( m_AudioOutputFileFullPathStrPt + "\0" ).getBytes(), ( short ) 1, m_SamplingRate, 16 ) == 0 )
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "创建音频输出文件 " + m_AudioOutputFileFullPathStrPt + " 的Wave文件写入器对象成功。" );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "创建音频输出文件 " + m_AudioOutputFileFullPathStrPt + " 的Wave文件写入器对象失败。" );
                        break out;
                    }

                    //创建并初始化音频结果Wave文件写入器对象。
                    m_AudioResultWaveFileWriterPt = new WaveFileWriter();
                    if( m_AudioResultWaveFileWriterPt.Init( ( m_AudioResultFileFullPathStrPt + "\0" ).getBytes(), ( short ) 1, m_SamplingRate, 16 ) == 0 )
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "创建音频结果文件 " + m_AudioResultFileFullPathStrPt + " 的Wave文件写入器对象成功。" );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "创建音频结果文件 " + m_AudioResultFileFullPathStrPt + " 的Wave文件写入器对象失败。" );
                        break out;
                    }
                }

                //初始化各个链表类对象。
                m_InputFrameLnkLstPt = new LinkedList< short[] >(); //初始化输入帧链表类对象。
                m_OutputFrameLnkLstPt = new LinkedList< short[] >(); //初始化输出帧链表类对象。

                //初始化各个线程类对象。
                m_AudioInputThreadPt = new AudioInputThread(); //初始化音频输入线程类对象。
                m_AudioOutputThreadPt = new AudioOutputThread(); //初始化音频输出线程类对象。

                m_AudioRecordPt.startRecording(); //让音频输入类对象开始录音。
                m_AudioTrackPt.play(); //让音频输出类对象开始播放。

                //启动音频输入线程，让音频输入线程去启动音频输出线程。
                m_AudioInputThreadPt.start();

                m_ExitCode = -2; //初始化已经成功了，再将本线程退出代码预设为处理失败，如果处理失败，这个退出代码就不用再设置了，如果处理成功，再设置为成功的退出代码。

                if( m_IsPrintLogcat != 0 )
                {
                    p_NowDatePt = new Date();
                    Log.i( m_CurClsNameStrPt, "音频处理线程：音频处理线程初始化完毕，耗时：" + ( p_NowDatePt.getTime() - p_LastDatePt.getTime() ) + " 毫秒，正式开始处理音频。" );
                }

                //以下变量要在初始化以后再声明才行。
                short p_PcmInputFramePt[]; //PCM格式输入帧。
                short p_PcmOutputFramePt[]; //PCM格式音频输出帧。
                short p_PcmResultFramePt[] = new short[m_FrameLen]; //PCM格式结果帧。
                short p_PcmTmpFramePt[] = new short[m_FrameLen]; //PCM格式临时帧。
                short p_PcmSwapFramePt[]; //PCM格式交换帧。
                HTInt p_VoiceActStsPt = new HTInt( 1 ); //语音活动状态，1表示有语音活动，0表示无语音活动，预设为1，为了让在没有使用语音活动检测的情况下永远都是有语音活动。
                m_HasVoiceActFrameTotal = 0; //有语音活动帧总数清0。
                byte p_SpeexInputFramePt[] = ( m_UseWhatCodec == 1 ) ? new byte[m_FrameLen] : null; //Speex格式输入帧。
                HTLong p_SpeexInputFrameLenPt = new HTLong( 0 ); //Speex格式输入帧数组的数据长度，单位字节。
                HTInt p_SpeexInputFrameIsNeedTransPt = new HTInt( 1 ); //Speex格式输入帧是否需要传输，1表示需要传输，0表示不需要传输，预设为1为了让在没有使用非连续传输的情况下永远都是需要传输。

                while( true )
                {
                    if( m_IsPrintLogcat != 0 ) p_LastDatePt = new Date();

                    //调用用户定义的处理函数。
                    p_TmpInt32 = UserProcess();
                    if( p_TmpInt32 == 0 )
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：调用用户定义的处理函数成功。返回值：" + p_TmpInt32 );
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.e( m_CurClsNameStrPt, "音频处理线程：调用用户定义的处理函数失败。返回值：" + p_TmpInt32 );
                        break out;
                    }

                    //开始处理输入帧。
                    if( ( m_InputFrameLnkLstPt.size() > 0 ) && ( m_OutputFrameLnkLstPt.size() > 0 ) || //如果输入帧链表和输出帧链表中都有帧了，才开始处理。
                            ( m_InputFrameLnkLstPt.size() > 15 ) ) //如果输入帧链表里已经累积很多输入帧了，说明输出帧链表里迟迟没有音频输出帧，也开始处理。
                    {
                        //从输入帧链表中取出第一个输入帧。
                        synchronized( m_InputFrameLnkLstPt )
                        {
                            p_PcmInputFramePt = m_InputFrameLnkLstPt.getFirst();
                            m_InputFrameLnkLstPt.removeFirst();
                        }
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：从输入帧链表中取出第一个输入帧。" );

                        //从输出帧链表中取出第一个输出帧。
                        if( m_OutputFrameLnkLstPt.size() > 0 ) //如果输出帧链表里有输出帧。
                        {
                            synchronized( m_OutputFrameLnkLstPt )
                            {
                                p_PcmOutputFramePt = m_OutputFrameLnkLstPt.getFirst();
                                m_OutputFrameLnkLstPt.removeFirst();
                            }
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：从输出帧链表中取出第一个输出帧。" );
                        }
                        else //如果输出帧链表里没有输出帧。
                        {
                            p_PcmOutputFramePt = new short[m_FrameLen];
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：输出帧链表中没有输出帧，用一个空帧代替。" );
                        }

                        //将输入帧复制到结果帧，方便处理。
                        System.arraycopy( p_PcmInputFramePt, 0, p_PcmResultFramePt, 0, m_FrameLen );

                        //使用什么声学回音消除器。
                        switch( m_UseWhatAec )
                        {
                            case 0: //如果不使用声学回音消除器。
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：不使用声学回音消除器。" );
                                break;
                            case 1: //如果使用Speex声学回音消除器。
                                p_TmpInt32 = m_SpeexAecPt.Proc( p_PcmResultFramePt, p_PcmOutputFramePt, p_PcmTmpFramePt );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：使用Speex声学回音消除器成功。返回值：" + p_TmpInt32 );

                                    p_PcmSwapFramePt = p_PcmResultFramePt;
                                    p_PcmResultFramePt = p_PcmTmpFramePt;
                                    p_PcmTmpFramePt = p_PcmSwapFramePt;
                                }
                                else
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：使用Speex声学回音消除器失败。返回值：" + p_TmpInt32 );
                                }
                                break;
                            case 2: //如果使用WebRtc定点版声学回音消除器。
                                p_TmpInt32 = m_WebRtcAecmPt.Proc( p_PcmResultFramePt, p_PcmOutputFramePt, p_PcmTmpFramePt );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：使用WebRtc定点版声学回音消除器成功。返回值：" + p_TmpInt32 );

                                    p_PcmSwapFramePt = p_PcmResultFramePt;
                                    p_PcmResultFramePt = p_PcmTmpFramePt;
                                    p_PcmTmpFramePt = p_PcmSwapFramePt;
                                }
                                else
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：使用WebRtc定点版声学回音消除器失败。返回值：" + p_TmpInt32 );
                                }
                                break;
                            case 3: //如果使用WebRtc浮点版声学回音消除器。
                                p_TmpInt32 = m_WebRtcAecPt.Proc( p_PcmResultFramePt, p_PcmOutputFramePt, p_PcmTmpFramePt );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：使用WebRtc浮点版声学回音消除器成功。返回值：" + p_TmpInt32 );

                                    p_PcmSwapFramePt = p_PcmResultFramePt;
                                    p_PcmResultFramePt = p_PcmTmpFramePt;
                                    p_PcmTmpFramePt = p_PcmSwapFramePt;
                                }
                                else
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：使用WebRtc浮点版声学回音消除器失败。返回值：" + p_TmpInt32 );
                                }
                                break;
                            case 4: //如果使用SpeexWebRtc三重浮点版声学回音消除器。
                                p_TmpInt32 = m_SpeexWebRtcAecPt.Proc( p_PcmResultFramePt, p_PcmOutputFramePt, p_PcmTmpFramePt );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：使用SpeexWebRtc三重浮点版声学回音消除器成功。返回值：" + p_TmpInt32 );

                                    p_PcmSwapFramePt = p_PcmResultFramePt;
                                    p_PcmResultFramePt = p_PcmTmpFramePt;
                                    p_PcmTmpFramePt = p_PcmSwapFramePt;
                                }
                                else
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：使用SpeexWebRtc三重浮点版声学回音消除器失败。返回值：" + p_TmpInt32 );
                                }
                                break;
                        }

                        //使用什么噪音抑制器。
                        switch( m_UseWhatNs )
                        {
                            case 0: //如果不使用噪音抑制器。
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：不使用噪音抑制器。" );
                                break;
                            }
                            case 1: //如果使用Speex预处理器的噪音抑制。
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：稍后在使用Speex预处理器时一起使用噪音抑制。" );
                                break;
                            }
                            case 2: //如果使用WebRtc定点版噪音抑制器。
                            {
                                p_TmpInt32 = m_WebRtcNsxPt.Proc( p_PcmResultFramePt, p_PcmTmpFramePt );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：使用WebRtc定点版噪音抑制器成功。返回值：" + p_TmpInt32 );

                                    p_PcmSwapFramePt = p_PcmResultFramePt;
                                    p_PcmResultFramePt = p_PcmTmpFramePt;
                                    p_PcmTmpFramePt = p_PcmSwapFramePt;
                                }
                                else
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：使用WebRtc定点版噪音抑制器失败。返回值：" + p_TmpInt32 );
                                }
                                break;
                            }
                            case 3: //如果使用WebRtc浮点版噪音抑制器。
                            {
                                p_TmpInt32 = m_WebRtcNsPt.Proc( p_PcmResultFramePt, p_PcmTmpFramePt );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：使用WebRtc浮点版噪音抑制器成功。返回值：" + p_TmpInt32 );

                                    p_PcmSwapFramePt = p_PcmResultFramePt;
                                    p_PcmResultFramePt = p_PcmTmpFramePt;
                                    p_PcmTmpFramePt = p_PcmSwapFramePt;
                                }
                                else
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：使用WebRtc浮点版噪音抑制器失败。返回值：" + p_TmpInt32 );
                                }
                                break;
                            }
                            case 4: //如果使用RNNoise噪音抑制器。
                            {
                                p_TmpInt32 = m_RNNoisePt.Proc( p_PcmResultFramePt, p_PcmTmpFramePt );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：使用RNNoise噪音抑制器成功。返回值：" + p_TmpInt32 );

                                    p_PcmSwapFramePt = p_PcmResultFramePt;
                                    p_PcmResultFramePt = p_PcmTmpFramePt;
                                    p_PcmTmpFramePt = p_PcmSwapFramePt;
                                }
                                else
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：使用RNNoise噪音抑制器失败。返回值：" + p_TmpInt32 );
                                }
                                break;
                            }
                        }

                        //使用Speex预处理器。
                        if( ( m_UseWhatNs == 1 ) || ( m_IsUseSpeexPprocOther != 0 ) )
                        {
                            p_TmpInt32 = m_SpeexPprocPt.Proc( p_PcmResultFramePt, p_PcmTmpFramePt, p_VoiceActStsPt );
                            if( p_TmpInt32 == 0 )
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：使用Speex预处理器成功。语音活动状态：" + p_VoiceActStsPt.m_Val + "，返回值：" + p_TmpInt32 );

                                p_PcmSwapFramePt = p_PcmResultFramePt;
                                p_PcmResultFramePt = p_PcmTmpFramePt;
                                p_PcmTmpFramePt = p_PcmSwapFramePt;
                            }
                            else
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：使用Speex预处理器失败。返回值：" + p_TmpInt32 );
                            }
                        }

                        //递增有语音活动帧总数。
                        m_HasVoiceActFrameTotal += p_VoiceActStsPt.m_Val;

                        //使用什么编码器。
                        switch( m_UseWhatCodec )
                        {
                            case 0: //如果使用PCM原始数据。
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：使用PCM原始数据。" );
                                break;
                            }
                            case 1: //如果使用Speex编码器。
                            {
                                p_TmpInt32 = m_SpeexEncoderPt.Proc( p_PcmResultFramePt, p_SpeexInputFramePt, p_SpeexInputFramePt.length, p_SpeexInputFrameLenPt, p_SpeexInputFrameIsNeedTransPt );
                                if( p_TmpInt32 == 0 )
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.i( m_CurClsNameStrPt, "音频处理线程：使用Speex编码器成功。Speex格式输入帧的内存长度：" + p_SpeexInputFrameLenPt.m_Val + "，Speex格式输入帧是否需要传输：" + p_SpeexInputFrameIsNeedTransPt.m_Val + "，返回值：" + p_TmpInt32 );
                                }
                                else
                                {
                                    if( m_IsPrintLogcat != 0 )
                                        Log.e( m_CurClsNameStrPt, "音频处理线程：使用Speex编码器失败。返回值：" + p_TmpInt32 );
                                }
                                break;
                            }
                            case 2: //如果使用Opus编码器。
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：暂不支持使用Opus编码器。" );
                                break out;
                            }
                        }

                        //用音频输入Wave文件写入器写入输入帧数据。
                        if( m_AudioInputWaveFileWriterPt != null )
                        {
                            if( m_AudioInputWaveFileWriterPt.WriteData( p_PcmInputFramePt, m_FrameLen ) == 0 )
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：用音频输入Wave文件写入器写入输入帧数据成功。" );
                            }
                            else
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：用音频输入Wave文件写入器写入输入帧数据失败。" );
                            }
                        }

                        //用音频输出Wave文件写入器写入输出帧数据。
                        if( m_AudioOutputWaveFileWriterPt != null )
                        {
                            if( m_AudioOutputWaveFileWriterPt.WriteData( p_PcmOutputFramePt, m_FrameLen ) == 0 )
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：用音频输出Wave文件写入器写入输出帧数据成功。" );
                            }
                            else
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：用音频输出Wave文件写入器写入输出帧数据失败。" );
                            }
                        }

                        //用音频结果Wave文件写入器写入结果帧数据。
                        if( m_AudioResultWaveFileWriterPt != null )
                        {
                            if( m_AudioResultWaveFileWriterPt.WriteData( p_PcmResultFramePt, m_FrameLen ) == 0 )
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.i( m_CurClsNameStrPt, "音频处理线程：用音频结果Wave文件写入器写入结果帧数据成功。" );
                            }
                            else
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：用音频结果Wave文件写入器写入结果帧数据失败。" );
                            }
                        }

                        //调用用户定义的读取输入帧函数。
                        p_TmpInt32 = UserReadInputFrame( p_PcmInputFramePt, p_PcmResultFramePt, p_VoiceActStsPt.m_Val, p_SpeexInputFramePt, p_SpeexInputFrameLenPt.m_Val, p_SpeexInputFrameIsNeedTransPt.m_Val );
                        if( p_TmpInt32 == 0 )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：音频处理线程：调用用户定义的读取输入帧函数成功。返回值：" + p_TmpInt32 );
                        }
                        else
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：调用用户定义的读取输入帧函数失败。返回值：" + p_TmpInt32 );
                            break out;
                        }

                        if( m_IsPrintLogcat != 0 )
                        {
                            p_NowDatePt = new Date();
                            Log.i( m_CurClsNameStrPt, "音频处理线程：本音频帧处理完毕，耗时：" + ( p_NowDatePt.getTime() - p_LastDatePt.getTime() ) + " 毫秒。" );
                            p_LastDatePt = p_NowDatePt;
                        }

                        if( m_ExitFlag != 0 ) //如果本线程退出标记为请求退出。
                        {
                            m_ExitCode = 0; //处理已经成功了，再将本线程退出代码设置为正常退出。
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：接收到退出请求，开始准备退出。" );
                            break out;
                        }
                    }

                    SystemClock.sleep( 1 ); //暂停一下，避免CPU使用率过高。
                }
            }

            if( m_IsPrintLogcat != 0 ) Log.i( m_CurClsNameStrPt, "音频处理线程：本线程开始退出。" );

            //请求音频输入线程退出。
            if( m_AudioInputThreadPt != null )
            {
                m_AudioInputThreadPt.RequireExit();
            }

            //请求音频输出线程退出。
            if( m_AudioOutputThreadPt != null )
            {
                m_AudioOutputThreadPt.RequireExit();
            }

            //等待音频输入线程退出。
            if( m_AudioInputThreadPt != null )
            {
                try
                {
                    m_AudioInputThreadPt.join();
                    m_AudioInputThreadPt = null;
                }
                catch( InterruptedException e )
                {

                }
            }

            //等待音频输出线程退出。
            if( m_AudioOutputThreadPt != null )
            {
                try
                {
                    m_AudioOutputThreadPt.join();
                    m_AudioOutputThreadPt = null;
                }
                catch( InterruptedException e )
                {

                }
            }

            //销毁音频输入Wave文件写入器对象。
            if( m_AudioInputWaveFileWriterPt != null )
            {
                p_TmpInt32 = m_AudioInputWaveFileWriterPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁音频输入Wave文件写入器对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁音频输入Wave文件写入器对象失败。返回值：" + p_TmpInt32 );
                }
            }

            //销毁音频输出Wave文件写入器对象。
            if( m_AudioOutputWaveFileWriterPt != null )
            {
                p_TmpInt32 = m_AudioOutputWaveFileWriterPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁音频输出Wave文件写入器对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁音频输出Wave文件写入器对象失败。返回值：" + p_TmpInt32 );
                }
            }

            //销毁音频结果Wave文件写入器对象。
            if( m_AudioResultWaveFileWriterPt != null )
            {
                p_TmpInt32 = m_AudioResultWaveFileWriterPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁音频结果Wave文件写入器对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁音频结果Wave文件写入器对象失败。返回值：" + p_TmpInt32 );
                }
            }

            //销毁输入帧链表类对象。
            if( m_InputFrameLnkLstPt != null )
            {
                m_InputFrameLnkLstPt.clear();
                m_InputFrameLnkLstPt = null;

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：销毁输入帧链表类对象成功。" );
            }

            //销毁输出帧链表类对象。
            if( m_OutputFrameLnkLstPt != null )
            {
                m_OutputFrameLnkLstPt.clear();
                m_OutputFrameLnkLstPt = null;

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：销毁输出帧链表类对象成功。" );
            }

            //销毁Speex声学回音消除器类对象。
            if( m_SpeexAecPt != null )
            {
                //保存Speex声学回音消除器的内存块到文件。
                if( m_SpeexAecIsSaveMemFile != 0 )
                {
                    File file = new File( m_SpeexAecMemFileFullPathStrPt );
                    FileInputStream p_SpeexAecMemFileInputStreamPt = null;
                    FileOutputStream p_SpeexAecMemFileOutputStreamPt = null;
                    long p_i64SpeexAecMemoryFileVoiceActivityStatusTotal = 0;
                    byte p_SpeexAecMemPt[];
                    HTLong p_SpeexAecMemLen = new HTLong();

                    ReadSpeexAecMemoryFile:
                    if( file.exists() )
                    {
                        try
                        {
                            p_SpeexAecMemFileInputStreamPt = new FileInputStream( m_SpeexAecMemFileFullPathStrPt );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：创建Speex声学回音消除器的内存块文件 " + m_AudioInputFileFullPathStrPt + " 的文件输入流对象成功。" );
                        }
                        catch( FileNotFoundException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：创建Speex声学回音消除器的内存块文件 " + m_AudioInputFileFullPathStrPt + " 的文件输入流对象失败。原因：" + e.toString() );
                            break ReadSpeexAecMemoryFile;
                        }

                        p_SpeexAecMemPt = new byte[8];

                        //跳过Speex声学回音消除器内存块文件的采样频率、帧的数据长度、过滤器的数据长度。
                        try
                        {
                            if( p_SpeexAecMemFileInputStreamPt.skip( 16 ) != 16 )
                            {
                                throw new IOException( "文件中没有有语音活动帧总数。" );
                            }
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：跳过Speex声学回音消除器内存块文件的采样频率、帧的数据长度、过滤器的数据长度成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：跳过Speex声学回音消除器内存块文件的采样频率、帧的数据长度、过滤器的数据长度失败。原因：" + e.toString() );
                            break ReadSpeexAecMemoryFile;
                        }

                        try
                        {
                            if( p_SpeexAecMemFileInputStreamPt.read( p_SpeexAecMemPt, 0, 8 ) != 8 )
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：Speex声学回音消除器的内存块文件中没有有语音活动帧总数。" );
                                break ReadSpeexAecMemoryFile;
                            }
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：读取Speex声学回音消除器的内存块文件中的有语音活动帧总数失败。原因：" + e.toString() );
                            break ReadSpeexAecMemoryFile;
                        }

                        p_i64SpeexAecMemoryFileVoiceActivityStatusTotal = ( ( long ) p_SpeexAecMemPt[0] & 0xFF ) + ( ( ( long ) p_SpeexAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( long ) p_SpeexAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( long ) p_SpeexAecMemPt[3] & 0xFF ) << 24 ) + ( ( ( long ) p_SpeexAecMemPt[4] & 0xFF ) << 32 ) + ( ( ( long ) p_SpeexAecMemPt[5] & 0xFF ) << 40 ) + ( ( ( long ) p_SpeexAecMemPt[6] & 0xFF ) << 48 ) + ( ( ( long ) p_SpeexAecMemPt[7] & 0xFF ) << 56 );
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：Speex声学回音消除器的内存块文件中的有语音活动帧总数为：" + p_i64SpeexAecMemoryFileVoiceActivityStatusTotal + "，本次的：" + m_HasVoiceActFrameTotal + "。" );
                    }

                    if( p_SpeexAecMemFileInputStreamPt != null )
                    {
                        try
                        {
                            p_SpeexAecMemFileInputStreamPt.close();
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：销毁Speex声学回音消除器的内存块文件的文件输入流对象成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：销毁Speex声学回音消除器的内存块文件的文件输入流对象失败。原因：" + e.toString() );
                        }
                    }

                    WriteSpeexAecMemoryFile:
                    if( ( m_HasVoiceActFrameTotal >= 1500 ) || ( m_HasVoiceActFrameTotal > p_i64SpeexAecMemoryFileVoiceActivityStatusTotal ) ) //如果本次有语音活动帧总数超过30秒，或本次的有语音活动帧总数比Speex声学回音消除器的内存块文件中的大。
                    {
                        if( m_SpeexAecPt.GetMemLen( p_SpeexAecMemLen ) != 0 )
                        {
                            break WriteSpeexAecMemoryFile;
                        }

                        try
                        {
                            p_SpeexAecMemFileOutputStreamPt = new FileOutputStream( m_SpeexAecMemFileFullPathStrPt );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：创建Speex声学回音消除器的内存块文件 " + m_SpeexAecMemFileFullPathStrPt + " 的文件输出流对象成功。" );
                        }
                        catch( FileNotFoundException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：创建Speex声学回音消除器的内存块文件 " + m_SpeexAecMemFileFullPathStrPt + " 的文件输出流对象失败。原因：" + e.toString() );
                            break WriteSpeexAecMemoryFile;
                        }

                        p_SpeexAecMemPt = new byte[( int ) p_SpeexAecMemLen.m_Val];

                        //写入采样频率到Speex声学回音消除器的内存块文件。
                        p_SpeexAecMemPt[0] = ( byte ) ( m_SamplingRate & 0xFF );
                        p_SpeexAecMemPt[1] = ( byte ) ( m_SamplingRate >> 8 & 0xFF );
                        p_SpeexAecMemPt[2] = ( byte ) ( m_SamplingRate >> 16 & 0xFF );
                        p_SpeexAecMemPt[3] = ( byte ) ( m_SamplingRate >> 24 & 0xFF );

                        try
                        {
                            p_SpeexAecMemFileOutputStreamPt.write( p_SpeexAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入采样频率到Speex声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入采样频率到Speex声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteSpeexAecMemoryFile;
                        }

                        //写入帧的数据长度到Speex声学回音消除器的内存块文件。
                        p_SpeexAecMemPt[0] = ( byte ) ( m_FrameLen & 0xFF );
                        p_SpeexAecMemPt[1] = ( byte ) ( m_FrameLen >> 8 & 0xFF );
                        p_SpeexAecMemPt[2] = ( byte ) ( m_FrameLen >> 16 & 0xFF );
                        p_SpeexAecMemPt[3] = ( byte ) ( m_FrameLen >> 24 & 0xFF );

                        try
                        {
                            p_SpeexAecMemFileOutputStreamPt.write( p_SpeexAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入帧的数据长度到Speex声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入帧的数据长度到Speex声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteSpeexAecMemoryFile;
                        }

                        //写入滤波器的数据长度到Speex声学回音消除器的内存块文件。
                        p_SpeexAecMemPt[0] = ( byte ) ( m_SpeexAecFilterLen & 0xFF );
                        p_SpeexAecMemPt[1] = ( byte ) ( m_SpeexAecFilterLen >> 8 & 0xFF );
                        p_SpeexAecMemPt[2] = ( byte ) ( m_SpeexAecFilterLen >> 16 & 0xFF );
                        p_SpeexAecMemPt[3] = ( byte ) ( m_SpeexAecFilterLen >> 24 & 0xFF );

                        try
                        {
                            p_SpeexAecMemFileOutputStreamPt.write( p_SpeexAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入滤波器的数据长度到Speex声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入滤波器的数据长度到Speex声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteSpeexAecMemoryFile;
                        }

                        //写入有语音活动帧总数到Speex声学回音消除器的内存块文件。
                        p_SpeexAecMemPt[0] = ( byte ) ( m_HasVoiceActFrameTotal & 0xFF );
                        p_SpeexAecMemPt[1] = ( byte ) ( m_HasVoiceActFrameTotal >> 8 & 0xFF );
                        p_SpeexAecMemPt[2] = ( byte ) ( m_HasVoiceActFrameTotal >> 16 & 0xFF );
                        p_SpeexAecMemPt[3] = ( byte ) ( m_HasVoiceActFrameTotal >> 24 & 0xFF );
                        p_SpeexAecMemPt[4] = ( byte ) ( m_HasVoiceActFrameTotal >> 32 & 0xFF );
                        p_SpeexAecMemPt[5] = ( byte ) ( m_HasVoiceActFrameTotal >> 40 & 0xFF );
                        p_SpeexAecMemPt[6] = ( byte ) ( m_HasVoiceActFrameTotal >> 48 & 0xFF );
                        p_SpeexAecMemPt[7] = ( byte ) ( m_HasVoiceActFrameTotal >> 56 & 0xFF );

                        try
                        {
                            p_SpeexAecMemFileOutputStreamPt.write( p_SpeexAecMemPt, 0, 8 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入有语音活动帧总数到Speex声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入有语音活动帧总数到Speex声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteSpeexAecMemoryFile;
                        }

                        //写入内存块到Speex声学回音消除器内存块文件。
                        if( m_SpeexAecPt.GetMem( p_SpeexAecMemPt, p_SpeexAecMemLen.m_Val ) != 0 )
                        {
                            break WriteSpeexAecMemoryFile;
                        }

                        try
                        {
                            p_SpeexAecMemFileOutputStreamPt.write( p_SpeexAecMemPt, 0, ( int ) p_SpeexAecMemLen.m_Val );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入内存块到Speex声学回音消除器内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入内存块到Speex声学回音消除器内存块文件失败。原因：" + e.toString() );
                            break WriteSpeexAecMemoryFile;
                        }
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：因为本次有语音活动帧总数没有超过30秒，或本次的有语音活动帧总数比Speex声学回音消除器内存块文件中的小，所以本次不保存Speex声学回音消除器内存块到文件。" );
                    }

                    if( p_SpeexAecMemFileOutputStreamPt != null )
                    {
                        try
                        {
                            p_SpeexAecMemFileOutputStreamPt.close();
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：销毁Speex声学回音消除器的内存块文件的文件输出流对象成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：销毁Speex声学回音消除器的内存块文件的文件输出流对象失败。原因：" + e.toString() );
                        }
                    }
                }

                p_TmpInt32 = m_SpeexAecPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁Speex声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁Speex声学回音消除器类对象失败。返回值：" + p_TmpInt32 );
                }
                m_SpeexAecPt = null;
            }

            //销毁WebRtc定点版声学回音消除器类对象。
            if( m_WebRtcAecmPt != null )
            {
                p_TmpInt32 = m_WebRtcAecmPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc定点版声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc定点版声学回音消除器类对象失败。返回值：" + p_TmpInt32 );
                }
                m_WebRtcAecmPt = null;
            }

            //销毁WebRtc浮点版声学回音消除器类对象。
            if( m_WebRtcAecPt != null )
            {
                //保存WebRtc浮点版声学回音消除器的内存块到文件。
                if( m_WebRtcAecIsSaveMemFile != 0 )
                {
                    File file = new File( m_WebRtcAecMemFileFullPathStrPt );
                    FileInputStream p_WebRtcAecMemFileInputStreamPt = null;
                    FileOutputStream p_WebRtcAecMemFileOutputStreamPt = null;
                    long p_i64WebRtcAecMemoryFileVoiceActivityStatusTotal = 0;
                    byte p_WebRtcAecMemPt[];
                    HTLong p_WebRtcAecMemLen = new HTLong( 0 );

                    ReadWebRtcAecMemoryFile:
                    if( file.exists() )
                    {
                        try
                        {
                            p_WebRtcAecMemFileInputStreamPt = new FileInputStream( m_WebRtcAecMemFileFullPathStrPt );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：创建WebRtc浮点版声学回音消除器的内存块文件 " + m_AudioInputFileFullPathStrPt + " 的文件输入流对象成功。" );
                        }
                        catch( FileNotFoundException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：创建WebRtc浮点版声学回音消除器的内存块文件 " + m_AudioInputFileFullPathStrPt + " 的文件输入流对象失败。原因：" + e.toString() );
                            break ReadWebRtcAecMemoryFile;
                        }

                        p_WebRtcAecMemPt = new byte[8];

                        //跳过WebRtc浮点版声学回音消除器内存块文件的前8个参数。
                        try
                        {
                            if( p_WebRtcAecMemFileInputStreamPt.skip( 36 ) != 36 )
                            {
                                throw new IOException( "文件中没有有语音活动帧总数。" );
                            }
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：跳过WebRtc浮点版声学回音消除器内存块文件的采样频率、帧的数据长度、过滤器的数据长度成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：跳过WebRtc浮点版声学回音消除器内存块文件的采样频率、帧的数据长度、过滤器的数据长度失败。原因：" + e.toString() );
                            break ReadWebRtcAecMemoryFile;
                        }

                        try
                        {
                            if( p_WebRtcAecMemFileInputStreamPt.read( p_WebRtcAecMemPt, 0, 8 ) != 8 )
                            {
                                if( m_IsPrintLogcat != 0 )
                                    Log.e( m_CurClsNameStrPt, "音频处理线程：WebRtc浮点版声学回音消除器的内存块文件中没有有语音活动帧总数。" );
                                break ReadWebRtcAecMemoryFile;
                            }
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：读取WebRtc浮点版声学回音消除器的内存块文件中的有语音活动帧总数失败。原因：" + e.toString() );
                            break ReadWebRtcAecMemoryFile;
                        }

                        p_i64WebRtcAecMemoryFileVoiceActivityStatusTotal = ( ( long ) p_WebRtcAecMemPt[0] & 0xFF ) + ( ( ( long ) p_WebRtcAecMemPt[1] & 0xFF ) << 8 ) + ( ( ( long ) p_WebRtcAecMemPt[2] & 0xFF ) << 16 ) + ( ( ( long ) p_WebRtcAecMemPt[3] & 0xFF ) << 24 ) + ( ( ( long ) p_WebRtcAecMemPt[4] & 0xFF ) << 32 ) + ( ( ( long ) p_WebRtcAecMemPt[5] & 0xFF ) << 40 ) + ( ( ( long ) p_WebRtcAecMemPt[6] & 0xFF ) << 48 ) + ( ( ( long ) p_WebRtcAecMemPt[7] & 0xFF ) << 56 );
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：WebRtc浮点版声学回音消除器的内存块文件中的有语音活动帧总数为：" + p_i64WebRtcAecMemoryFileVoiceActivityStatusTotal + "，本次的：" + m_HasVoiceActFrameTotal + "。" );
                    }

                    if( p_WebRtcAecMemFileInputStreamPt != null )
                    {
                        try
                        {
                            p_WebRtcAecMemFileInputStreamPt.close();
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版声学回音消除器的内存块文件的文件输入流对象成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版声学回音消除器的内存块文件的文件输入流对象失败。原因：" + e.toString() );
                        }
                    }

                    WriteWebRtcAecMemoryFile:
                    if( ( m_HasVoiceActFrameTotal >= 1500 ) || ( m_HasVoiceActFrameTotal > p_i64WebRtcAecMemoryFileVoiceActivityStatusTotal ) ) //如果本次有语音活动帧总数超过30秒，或本次的有语音活动帧总数比WebRtc浮点版声学回音消除器的内存块文件中的大。
                    {
                        if( m_WebRtcAecPt.GetMemLen( p_WebRtcAecMemLen ) != 0 )
                        {
                            break WriteWebRtcAecMemoryFile;
                        }

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt = new FileOutputStream( m_WebRtcAecMemFileFullPathStrPt );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：创建WebRtc浮点版声学回音消除器的内存块文件 " + m_WebRtcAecMemFileFullPathStrPt + " 的文件输出流对象成功。" );
                        }
                        catch( FileNotFoundException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：创建WebRtc浮点版声学回音消除器的内存块文件 " + m_WebRtcAecMemFileFullPathStrPt + " 的文件输出流对象失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }

                        p_WebRtcAecMemPt = new byte[( int ) p_WebRtcAecMemLen.m_Val];

                        //写入采样频率到WebRtc浮点版声学回音消除器的内存块文件。
                        p_WebRtcAecMemPt[0] = ( byte ) ( m_SamplingRate & 0xFF );
                        p_WebRtcAecMemPt[1] = ( byte ) ( m_SamplingRate >> 8 & 0xFF );
                        p_WebRtcAecMemPt[2] = ( byte ) ( m_SamplingRate >> 16 & 0xFF );
                        p_WebRtcAecMemPt[3] = ( byte ) ( m_SamplingRate >> 24 & 0xFF );

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.write( p_WebRtcAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入采样频率到WebRtc浮点版声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入采样频率到WebRtc浮点版声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }

                        //写入帧的数据长度到WebRtc浮点版声学回音消除器的内存块文件。
                        p_WebRtcAecMemPt[0] = ( byte ) ( m_FrameLen & 0xFF );
                        p_WebRtcAecMemPt[1] = ( byte ) ( m_FrameLen >> 8 & 0xFF );
                        p_WebRtcAecMemPt[2] = ( byte ) ( m_FrameLen >> 16 & 0xFF );
                        p_WebRtcAecMemPt[3] = ( byte ) ( m_FrameLen >> 24 & 0xFF );

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.write( p_WebRtcAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入帧的数据长度到WebRtc浮点版声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入帧的数据长度到WebRtc浮点版声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }

                        //写入消除模式到WebRtc浮点版声学回音消除器的内存块文件。
                        p_WebRtcAecMemPt[0] = ( byte ) ( m_WebRtcAecEchoMode & 0xFF );
                        p_WebRtcAecMemPt[1] = ( byte ) ( m_WebRtcAecEchoMode >> 8 & 0xFF );
                        p_WebRtcAecMemPt[2] = ( byte ) ( m_WebRtcAecEchoMode >> 16 & 0xFF );
                        p_WebRtcAecMemPt[3] = ( byte ) ( m_WebRtcAecEchoMode >> 24 & 0xFF );

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.write( p_WebRtcAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入消除模式到WebRtc浮点版声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入消除模式到WebRtc浮点版声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }

                        //写入回音的延迟到WebRtc浮点版声学回音消除器的内存块文件。
                        p_WebRtcAecMemPt[0] = ( byte ) ( m_WebRtcAecDelay & 0xFF );
                        p_WebRtcAecMemPt[1] = ( byte ) ( m_WebRtcAecDelay >> 8 & 0xFF );
                        p_WebRtcAecMemPt[2] = ( byte ) ( m_WebRtcAecDelay >> 16 & 0xFF );
                        p_WebRtcAecMemPt[3] = ( byte ) ( m_WebRtcAecDelay >> 24 & 0xFF );

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.write( p_WebRtcAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入回音的延迟到WebRtc浮点版声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入回音的延迟到WebRtc浮点版声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }

                        //写入是否使用回音延迟不可知模式到WebRtc浮点版声学回音消除器的内存块文件。
                        p_WebRtcAecMemPt[0] = ( byte ) ( m_WebRtcAecIsUseDelayAgnosticMode & 0xFF );
                        p_WebRtcAecMemPt[1] = ( byte ) ( m_WebRtcAecIsUseDelayAgnosticMode >> 8 & 0xFF );
                        p_WebRtcAecMemPt[2] = ( byte ) ( m_WebRtcAecIsUseDelayAgnosticMode >> 16 & 0xFF );
                        p_WebRtcAecMemPt[3] = ( byte ) ( m_WebRtcAecIsUseDelayAgnosticMode >> 24 & 0xFF );

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.write( p_WebRtcAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入是否使用回音延迟不可知模式到WebRtc浮点版声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入是否使用回音延迟不可知模式到WebRtc浮点版声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }

                        //写入是否使用自适应调节回音的延迟到WebRtc浮点版声学回音消除器的内存块文件。
                        p_WebRtcAecMemPt[0] = ( byte ) ( m_WebRtcAecIsUseAdaptAdjDelay & 0xFF );
                        p_WebRtcAecMemPt[1] = ( byte ) ( m_WebRtcAecIsUseAdaptAdjDelay >> 8 & 0xFF );
                        p_WebRtcAecMemPt[2] = ( byte ) ( m_WebRtcAecIsUseAdaptAdjDelay >> 16 & 0xFF );
                        p_WebRtcAecMemPt[3] = ( byte ) ( m_WebRtcAecIsUseAdaptAdjDelay >> 24 & 0xFF );

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.write( p_WebRtcAecMemPt, 0, 4 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入是否使用自适应调节回音的延迟到WebRtc浮点版声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入是否使用自适应调节回音的延迟到WebRtc浮点版声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }

                        //写入有语音活动帧总数到WebRtc浮点版声学回音消除器的内存块文件。
                        p_WebRtcAecMemPt[0] = ( byte ) ( m_HasVoiceActFrameTotal & 0xFF );
                        p_WebRtcAecMemPt[1] = ( byte ) ( m_HasVoiceActFrameTotal >> 8 & 0xFF );
                        p_WebRtcAecMemPt[2] = ( byte ) ( m_HasVoiceActFrameTotal >> 16 & 0xFF );
                        p_WebRtcAecMemPt[3] = ( byte ) ( m_HasVoiceActFrameTotal >> 24 & 0xFF );
                        p_WebRtcAecMemPt[4] = ( byte ) ( m_HasVoiceActFrameTotal >> 32 & 0xFF );
                        p_WebRtcAecMemPt[5] = ( byte ) ( m_HasVoiceActFrameTotal >> 40 & 0xFF );
                        p_WebRtcAecMemPt[6] = ( byte ) ( m_HasVoiceActFrameTotal >> 48 & 0xFF );
                        p_WebRtcAecMemPt[7] = ( byte ) ( m_HasVoiceActFrameTotal >> 56 & 0xFF );

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.write( p_WebRtcAecMemPt, 0, 8 );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入有语音活动帧总数到WebRtc浮点版声学回音消除器的内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入有语音活动帧总数到WebRtc浮点版声学回音消除器的内存块文件失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }

                        //写入内存块到WebRtc浮点版声学回音消除器内存块文件。
                        if( m_WebRtcAecPt.GetMem( p_WebRtcAecMemPt, p_WebRtcAecMemLen.m_Val ) != 0 )
                        {
                            break WriteWebRtcAecMemoryFile;
                        }

                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.write( p_WebRtcAecMemPt, 0, ( int ) p_WebRtcAecMemLen.m_Val );
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：写入内存块到WebRtc浮点版声学回音消除器内存块文件成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：写入内存块到WebRtc浮点版声学回音消除器内存块文件失败。原因：" + e.toString() );
                            break WriteWebRtcAecMemoryFile;
                        }
                    }
                    else
                    {
                        if( m_IsPrintLogcat != 0 )
                            Log.i( m_CurClsNameStrPt, "音频处理线程：因为本次有语音活动帧总数没有超过30秒，或本次的有语音活动帧总数比WebRtc浮点版声学回音消除器内存块文件中的小，所以本次不保存WebRtc浮点版声学回音消除器内存块到文件。" );
                    }

                    if( p_WebRtcAecMemFileOutputStreamPt != null )
                    {
                        try
                        {
                            p_WebRtcAecMemFileOutputStreamPt.close();
                            if( m_IsPrintLogcat != 0 )
                                Log.i( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版声学回音消除器的内存块文件的文件输出流对象成功。" );
                        }
                        catch( IOException e )
                        {
                            if( m_IsPrintLogcat != 0 )
                                Log.e( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版声学回音消除器的内存块文件的文件输出流对象失败。原因：" + e.toString() );
                        }
                    }
                }

                p_TmpInt32 = m_WebRtcAecPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版声学回音消除器类对象失败。返回值：" + p_TmpInt32 );
                }
                m_WebRtcAecPt = null;
            }

            //销毁SpeexWebRtc三重声学回音消除器类对象。
            if( m_SpeexWebRtcAecPt != null )
            {
                p_TmpInt32 = m_SpeexWebRtcAecPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁SpeexWebRtc三重声学回音消除器类对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁SpeexWebRtc三重声学回音消除器类对象失败。返回值：" + p_TmpInt32 );
                }
                m_SpeexWebRtcAecPt = null;
            }

            //销毁WebRtc定点版噪音抑制器类对象。
            if( m_WebRtcNsxPt != null )
            {
                p_TmpInt32 = m_WebRtcNsxPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc定点版噪音抑制器类对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc定点版噪音抑制器类对象失败。返回值：" + p_TmpInt32 );
                }
                m_WebRtcNsxPt = null;
            }

            //销毁WebRtc浮点版噪音抑制器类对象。
            if( m_WebRtcNsPt != null )
            {
                p_TmpInt32 = m_WebRtcNsPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版噪音抑制器类对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁WebRtc浮点版噪音抑制器类对象失败。返回值：" + p_TmpInt32 );
                }
                m_WebRtcNsPt = null;
            }

            //销毁RNNoise噪音抑制器类对象。
            if( m_RNNoisePt != null )
            {
                p_TmpInt32 = m_RNNoisePt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁RNNoise噪音抑制器类对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁RNNoise噪音抑制器类对象失败。返回值：" + p_TmpInt32 );
                }
                m_RNNoisePt = null;
            }

            //销毁Speex预处理器类对象。
            if( m_SpeexPprocPt != null )
            {
                p_TmpInt32 = m_SpeexPprocPt.Destroy();
                if( p_TmpInt32 == 0 )
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.i( m_CurClsNameStrPt, "音频处理线程：销毁Speex预处理器类对象成功。返回值：" + p_TmpInt32 );
                }
                else
                {
                    if( m_IsPrintLogcat != 0 )
                        Log.e( m_CurClsNameStrPt, "音频处理线程：销毁Speex预处理器类对象失败。返回值：" + p_TmpInt32 );
                }
                m_SpeexPprocPt = null;
            }

            //销毁Speex编码器类对象。
            if( m_SpeexEncoderPt != null )
            {
                m_SpeexEncoderPt.Destroy();
                m_SpeexEncoderPt = null;

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：销毁Speex编码器类对象成功。" );
            }

            //销毁Speex解码器类对象。
            if( m_SpeexDecoderPt != null )
            {
                m_SpeexDecoderPt.Destroy();
                m_SpeexDecoderPt = null;

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：销毁Speex解码器类对象成功。" );
            }

            //销毁音频输入类对象。
            if( m_AudioRecordPt != null )
            {
                if( m_AudioRecordPt.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING )
                {
                    m_AudioRecordPt.stop();
                }
                m_AudioRecordPt.release();
                m_AudioRecordPt = null;

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：销毁音频输入类对象成功。" );
            }

            //销毁音频输出类对象。
            if( m_AudioTrackPt != null )
            {
                if( m_AudioTrackPt.getPlayState() != AudioTrack.PLAYSTATE_STOPPED )
                {
                    m_AudioTrackPt.stop();
                }
                m_AudioTrackPt.release();
                m_AudioTrackPt = null;

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：销毁音频输出类对象成功。" );
            }

            //调用用户定义的销毁函数。
            p_TmpInt32 = UserDestroy();

            //销毁接近息屏唤醒锁类对象。
            if( m_ProximityScreenOffWakeLockPt != null )
            {
                m_ProximityScreenOffWakeLockPt.release();
                m_ProximityScreenOffWakeLockPt = null;

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：销毁接近息屏唤醒锁类对象成功。" );
            }

            //销毁屏幕键盘全亮唤醒锁类对象。
            if( m_FullWakeLockPt != null )
            {
                m_FullWakeLockPt.release();
                m_FullWakeLockPt = null;

                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：销毁屏幕键盘全亮唤醒锁类对象成功。" );
            }

            if( p_TmpInt32 == 0 ) //如果用户需要直接退出。
            {
                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：本线程已退出。" );
                break ReInit;
            }
            else //如果用户需用重新初始化。
            {
                if( m_IsPrintLogcat != 0 )
                    Log.i( m_CurClsNameStrPt, "音频处理线程：本线程重新初始化。" );
            }
        }
    }
}