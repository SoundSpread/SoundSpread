package zsd.example.com.soundspread;

import android.app.AlertDialog;
import android.content.Context;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EditActivity extends AppCompatActivity implements MarkerView.MarkerListener,
        WaveformView.WaveformListener{
    private Button clipaudio;
    private Button checkclipfile;
    private Spinner spinner;
    private Spinner spinner1;
    private String musicname;
    private long firstbookmark;
    private long secondbookmark;
    private long curretntime;
    private int calculatecountsflag=0;
    private FTPUtils ftpUtils = null;


    private SamplePlayer mPlayer;


    private DataList dataList;
    public static EditActivity instance = null;
    private long mLoadingLastUpdateTime;
    private boolean mLoadingKeepGoing;
    private long mRecordingLastUpdateTime;
    private boolean mRecordingKeepGoing;
    private double mRecordingTime;
    private boolean mFinishActivity;
    private TextView mTimerTextView;
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    private SoundFile mSoundFile;
    private File mFile;
    private String mFilename;
    private String mArtist;
    private String mTitle;
    private int mNewFileKind;
    private boolean mWasGetContentIntent;
    private WaveformView mWaveformView;
    private MarkerView mStartMarker;
    private MarkerView mEndMarker;
    private TextView mStartText;
    private TextView mEndText;
    private TextView mInfo;
    private String mInfoContent;
    private ImageButton mPlayButton;
    private ImageButton mRewindButton;
    private ImageButton mFfwdButton;
    private boolean mKeyDown;
    private String mCaption = "";
    private int mWidth;
    private int mMaxPos;
    private int mStartPos;
    private int mEndPos;
    private boolean mStartVisible;
    private boolean mEndVisible;
    private int mLastDisplayedStartPos;
    private int mLastDisplayedEndPos;
    private int mOffset;
    private int mOffsetGoal;
    private int mFlingVelocity;
    private int mPlayStartMsec;
    private int mPlayEndMsec;
    private Handler mHandler;
    private boolean mIsPlaying;
    //private SamplePlayer mPlayer;
    private boolean mTouchDragging;
    private float mTouchStart;
    private int mTouchInitialOffset;
    private int mTouchInitialStartPos;
    private int mTouchInitialEndPos;
    private long mWaveformTouchStartMsec;
    private float mDensity;
    private long cutstartposition;
    private long cutfinalposition;
    private int mMarkerLeftInset;
    private int mMarkerRightInset;
    private int mMarkerTopOffset;
    private ArrayList<TextView> bookmarklist;
    private int duration;
    private int mMarkerBottomOffset;

    private Thread mLoadSoundFileThread;
    private Thread mRecordAudioThread;
    private Thread mSaveSoundFileThread;
    private String mtimeflag;
    private int minute;
    private int seconds;
    private double counts=1;
    private int mOffsetstart;
    private int mOffsetend;
    private ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        InitFTPServerSetting();
        Intent intent=getIntent();
        Bundle bundle = intent.getExtras();
        musicname = (String) bundle.getSerializable("uri");
        dataList=(DataList)bundle.getSerializable("datalist");
        Uri uri= Uri.parse(musicname);
        curretntime=System.currentTimeMillis();
        cutstartposition=0;
        cutfinalposition=0;
        bookmarklist=new ArrayList<TextView>();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;
        mHandler=new Handler();
        mMarkerLeftInset = (int)(46 * mDensity);
        mMarkerRightInset = (int)(48 * mDensity);
        mMarkerTopOffset = (int)(10 * mDensity);
        mMarkerBottomOffset = (int)(10 * mDensity);
        mMaxPos = 0;
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;
        mFilename=musicname;
        mFile = new File(mFilename);
        mPlayer = null;
        loadgui();
        mLoadingLastUpdateTime = getCurrentTime();
        mLoadingKeepGoing = true;
        mFinishActivity = false;
        mProgressDialog=null;
        mProgressDialog = new ProgressDialog(EditActivity.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle("Loading");
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mLoadingKeepGoing = false;
                        mFinishActivity = true;
                    }
                });
        mProgressDialog.show();
        final SoundFile.ProgressListener listener =
                new SoundFile.ProgressListener() {
                    public boolean reportProgress(double fractionComplete) {
                        long now = getCurrentTime();
                        if (now - mLoadingLastUpdateTime > 100) {
                            mProgressDialog.setProgress(
                                    (int) (mProgressDialog.getMax() * fractionComplete));
                            mLoadingLastUpdateTime = now;
                        }
                        return mLoadingKeepGoing;
                    }
                };
        mLoadSoundFileThread = new Thread() {
            public void run() {
                try {
                    mSoundFile = SoundFile.create(mFile.getAbsolutePath(), listener);

                    if (mSoundFile == null) {
                        mProgressDialog.dismiss();
                        String name = mFile.getName().toLowerCase();
                        String[] components = name.split("\\.");
                        String err;
                        if (components.length < 2) {
                        } else {
                        }
                        return;
                    }
                    mPlayer = new SamplePlayer(mSoundFile);
                } catch (final Exception e) {
                    mProgressDialog.dismiss();
                    e.printStackTrace();
                    mInfoContent = e.toString();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mInfo.setText(mInfoContent);
                        }
                    });
                    return;
                }
                mProgressDialog.dismiss();
                if (mLoadingKeepGoing) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            finishOpeningSoundFile();
                        }
                    };
                    mHandler.post(runnable);
                } else if (mFinishActivity){
                    EditActivity.this.finish();
                }
            }
        };
        mLoadSoundFileThread.start();
        instance = this;

    }

    private void loadgui()
    {
        setContentView(R.layout.activity_edit);
        mWaveformView = (WaveformView)findViewById(R.id.waveform);
        mWaveformView.setListener(this);
        if (mSoundFile != null && !mWaveformView.hasSoundFile()) {
            //Toast.makeText(EditActivity.this,"input",Toast.LENGTH_SHORT).show();
            mWaveformView.setSoundFile(mSoundFile);
            mWaveformView.recomputeHeights(mDensity);
            mMaxPos = mWaveformView.maxPos();
        }
        else
        {
           // Toast.makeText(EditActivity.this,"aout",Toast.LENGTH_SHORT).show();
        }
        loadplayer();
        mStartMarker = (MarkerView)findViewById(R.id.startmarker);
        mStartMarker.setListener(this);
        mStartMarker.setAlpha(1f);
        mStartMarker.setFocusable(true);
        mStartMarker.setFocusableInTouchMode(true);
        mStartVisible = true;

        mEndMarker = (MarkerView)findViewById(R.id.endmarker);
        mEndMarker.setListener(this);
        mEndMarker.setAlpha(1f);
        mEndMarker.setFocusable(true);
        mEndMarker.setFocusableInTouchMode(true);
        mEndVisible = true;
        //Toast.makeText(EditActivity.this, musicname, Toast.LENGTH_SHORT).show();
        clipaudio=(Button)findViewById(R.id.clipaudio);
        clipaudio.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText et=new EditText(EditActivity.this);
                new AlertDialog.Builder(EditActivity.this).setTitle("please input filename").setIcon(
                        android.R.drawable.ic_dialog_info).setView(et)
                        .setPositiveButton("OK",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {


                                String username = et.getText().toString();
                                if (username.length() != 0) {
                                    if (matchfile(username)) {

                                        new AlertDialog.Builder(EditActivity.this).setTitle("SAME FILENAME")//设置对话框标题

                                                .setMessage("Filename already exists.Please use another name.")//设置显示的内容

                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {//添加确定按钮


                                                    @Override

                                                    public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件


                                                    }

                                                }).show();//在按键响应事件中显示此对话框


                                    } else {


                                        try {
                                            MP3File f = new MP3File(musicname);
                                            f.cut(cutstartposition, cutfinalposition, username);
                                            createtxt(et.getText().toString());
                                            ftpUtils.uploadFile("/mnt/sdcard/soundspread/clip/" + et.getText().toString() + ".mp3", et.getText().toString() + ".mp3");

                                            Toast.makeText(EditActivity.this, "MP3 file is cut successfully", Toast.LENGTH_SHORT).show();

                                        } catch (Exception exx) {
                                            exx.printStackTrace();
                                        }
                                    }
                                }
                            }

                        }).setNegativeButton("Cancel", null).show();
            }
        });
        checkclipfile=(Button)findViewById(R.id.checkclipfile);
        checkclipfile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(EditActivity.this, ShowActivity.class);
                startActivity(intent);
            }
        });
        updateDisplay();
    }


    public boolean matchfile(String ofilename){
        boolean flag = false;
        String path="/mnt/sdcard/soundspread/clip/";
        File file=new File(path);
        if (file.exists()){
            File[] filelist =file.listFiles();
            ofilename=ofilename+".mp3";

            for(int i=0;i<filelist.length;i++){

                if(ofilename.equals(filelist[i].getName().toString())) {
                    flag = true;
                    break;
                }

            }
        }
        return flag;

    }

    private void createtxt(String clna){
        File filedir = new File("/mnt/sdcard/soundspread/clip/");
        if(!filedir.exists())
            filedir.mkdir();
        String fileName = "/mnt/sdcard/soundspread/clip/" + clna + ".txt";
        File mp3file= new File(musicname);
        String tempcontent=ID3V1.main(mp3file);
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(tempcontent.getBytes());
            fos.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    private void loadplayer() {

        mIsPlaying = false;
        mPlayButton = (ImageButton)findViewById(R.id.play);
        mPlayButton.setOnClickListener(mPlayListener);
        mRewindButton = (ImageButton)findViewById(R.id.rew);
        mRewindButton.setOnClickListener(mRewindListener);
        mFfwdButton = (ImageButton)findViewById(R.id.ffwd);
        mFfwdButton.setOnClickListener(mFfwdListener);



    }

    private synchronized void handlePause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        mWaveformView.setPlayback(-1);
        mIsPlaying = false;
        enableDisableButtons();
    }

    private synchronized void onPlay(int startPosition) {
        if (mIsPlaying) {
            handlePause();
            return;
        }

        if (mPlayer == null) {
            // Not initialized yet
            Toast.makeText(EditActivity.this,"no player",Toast.LENGTH_SHORT).show();
            return;
        }

        try {
           // Toast.makeText(EditActivity.this,Integer.toString(mStartPos),Toast.LENGTH_SHORT).show();
          //  Toast.makeText(EditActivity.this,Integer.toString(mEndPos),Toast.LENGTH_SHORT).show();

            mPlayStartMsec = mWaveformView.pixelsToMillisecs(startPosition);
            if (startPosition < mStartPos) {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mStartPos);
            } else if (startPosition > mEndPos) {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mMaxPos);
            } else {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mEndPos);
            }
            mPlayer.setOnCompletionListener(new SamplePlayer.OnCompletionListener() {
                @Override
                public void onCompletion() {
                    handlePause();
                }
            });
            mIsPlaying = true;

            mPlayer.seekTo(mPlayStartMsec);
            mPlayer.start();
            updateDisplay();
            enableDisableButtons();
        } catch (Exception e) {
           // showFinalAlert(e, R.string.play_error);
            return;
        }
    }

    private View.OnClickListener mPlayListener = new View.OnClickListener() {
        public void onClick(View sender) {
            onPlay(mStartPos);
        }
    };

    private View.OnClickListener mRewindListener = new View.OnClickListener() {
        public void onClick(View sender) {
            if (mIsPlaying) {
                int newPos = mPlayer.getCurrentPosition() - 5000;
                if (newPos < mPlayStartMsec)
                    newPos = mPlayStartMsec;
                mPlayer.seekTo(newPos);
            } else {
                mStartMarker.requestFocus();
                markerFocus(mStartMarker);
            }
        }
    };

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View sender) {
            if (mIsPlaying) {
                int newPos = 5000 + mPlayer.getCurrentPosition();
                if (newPos > mPlayEndMsec)
                    newPos = mPlayEndMsec;
                mPlayer.seekTo(newPos);
            } else {
                mEndMarker.requestFocus();
                markerFocus(mEndMarker);
            }
        }
    };



    private void enableDisableButtons() {
        if (mIsPlaying) {
            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
            mPlayButton.setContentDescription("Stop");
        } else {
            mPlayButton.setImageResource(android.R.drawable.ic_media_play);
            mPlayButton.setContentDescription("Pause");
        }
    }

    @Override
    public void markerTouchStart(MarkerView marker, float pos) {
        mTouchDragging = true;
        mTouchStart = pos;
        mTouchInitialStartPos = mStartPos;
        mTouchInitialEndPos = mEndPos;
    }

    @Override
    public void markerTouchMove(MarkerView marker, float pos) {
        float delta = pos - mTouchStart;

        if (marker == mStartMarker) {
            mStartPos = trap((int)(mTouchInitialStartPos + delta));
            mEndPos = trap((int)(mTouchInitialEndPos + delta));
        } else {
            mEndPos = trap((int)(mTouchInitialEndPos + delta));
            if (mEndPos < mStartPos)
                mEndPos = mStartPos;
        }

        updateDisplay();
    }

    @Override
    public void markerTouchEnd(MarkerView marker) {
        mTouchDragging = false;
        if (marker == mStartMarker) {
      //      setOffsetGoalStart();
        } else {
     //       setOffsetGoalEnd();
        }
    }

    @Override
    public void markerFocus(MarkerView marker) {
        mKeyDown = false;
        if (marker == mStartMarker) {
       //     setOffsetGoalStartNoUpdate();
        } else {
           // setOffsetGoalEndNoUpdate();
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler.postDelayed(new Runnable() {
            public void run() {
                updateDisplay();
            }
        }, 100);
    }

    @Override
    public void markerLeft(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos = trap(mStartPos - velocity);
            mEndPos = trap(mEndPos - (saveStart - mStartPos));
         //   setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity);
                mEndPos = mStartPos;
            } else {
                mEndPos = trap(mEndPos - velocity);
            }
         //   setOffsetGoalEnd();
        }

        updateDisplay();
    }

    @Override
    public void markerRight(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos += velocity;
            if (mStartPos > mMaxPos)
                mStartPos = mMaxPos;
            mEndPos += (mStartPos - saveStart);
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;
                mOffsetstart=mOffset;
         //   setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            mEndPos += velocity;
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;
                mOffsetend=mOffset;
         //   setOffsetGoalEnd();
        }

        updateDisplay();
    }

    @Override
    public void markerEnter(MarkerView marker) {

    }

    @Override
    public void markerKeyUp() {
        mKeyDown = false;
        updateDisplay();
    }

    @Override
    public void markerDraw() {

    }

    @Override
    public void waveformTouchStart(float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialOffset = mOffset;
        mFlingVelocity = 0;
        mWaveformTouchStartMsec = getCurrentTime();
    }

    @Override
    public void waveformTouchMove(float x) {
        mOffset = trap((int)(mTouchInitialOffset + (mTouchStart - x)));
        updateDisplay();
    }

    @Override
    public void waveformTouchEnd() {
        mTouchDragging = false;
        mOffsetGoal = mOffset;

        long elapsedMsec = getCurrentTime() - mWaveformTouchStartMsec;
        if (elapsedMsec < 300) {
            if (mIsPlaying) {
                int seekMsec = mWaveformView.pixelsToMillisecs(
                        (int)(mTouchStart + mOffset));
                if (seekMsec >= mPlayStartMsec &&
                        seekMsec < mPlayEndMsec) {
                } else {
                }
            } else {
            }
        }
    }

    @Override
    public void waveformFling(float x) {
        mTouchDragging = false;
        mOffsetGoal = mOffset;
        mFlingVelocity = (int)(-x);
        updateDisplay();
    }

    @Override
    public void waveformDraw() {
        mWidth = mWaveformView.getMeasuredWidth();
        if (mOffsetGoal != mOffset && !mKeyDown)
            updateDisplay();
        else if (mIsPlaying) {
            updateDisplay();
        } else if (mFlingVelocity != 0) {
            updateDisplay();
        }
    }

    @Override
    public void waveformZoomIn() {
        if (mWaveformView.canZoomIn())
            counts=counts/2;
        mWaveformView.zoomIn();
        mStartPos = mWaveformView.getStart();
        mEndPos = mWaveformView.getEnd();
        mMaxPos = mWaveformView.maxPos();
        mOffset = mWaveformView.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();
    }

    @Override
    public void waveformZoomOut() {
        if (mWaveformView.canZoomOut())
            counts=counts*2;
        mWaveformView.zoomOut();
        mStartPos = mWaveformView.getStart();
        mEndPos = mWaveformView.getEnd();
        mMaxPos = mWaveformView.maxPos();
        mOffset = mWaveformView.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();
    }
   /* public void onItemSelected(AdapterView<?> parent,
                               View v, int position, long id) {
        switch(v.getId())
        {
            case R.id.showbookmark:
                //firstbookmark=dataList.getitem(position).getBookmarktime();
                firstbookmark=71538;
                Toast.makeText(EditActivity.this,Long.toString(firstbookmark),Toast.LENGTH_SHORT).show();
                break;
            case R.id.shownextbookmark:
                //secondbookmark=dataList.getitem(position).getBookmarktime();
                secondbookmark=181230;
                Toast.makeText(EditActivity.this,Long.toString(secondbookmark),Toast.LENGTH_SHORT).show();
                break;
        }
    }*/
   private long getCurrentTime() {
       return System.nanoTime() / 1000000;
   }
    private void resetPositions() {
        mStartPos = mWaveformView.secondsToPixels(0.0);
        mEndPos = mWaveformView.secondsToPixels(15.0);

    }
    private synchronized void updateDisplay() {
       // int frames = mWaveformView.millisecsToPixels(0);
       // mWaveformView.setPlayback(frames);
        //setOffsetGoalNoUpdate(frames - mWidth / 2);
        if(mtimeflag==null) {
            mtimeflag = mWaveformView.timeflag;
        }
        if (mIsPlaying) {
            int now = mPlayer.getCurrentPosition();
            int frames = mWaveformView.millisecsToPixels(now);//set the default value, which can be replaced by the position of the player
            mWaveformView.setPlayback(frames);
           // setOffsetGoalNoUpdate(frames - mWidth / 2);
            if (now >= mPlayEndMsec) {
                handlePause();
            }
        }
        if (!mTouchDragging) {
            int offsetDelta;

            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30;
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80;
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80;
                } else {
                    mFlingVelocity = 0;
                }

                mOffset += offsetDelta;

                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2;
                    mFlingVelocity = 0;
                }
                if (mOffset < 0) {
                    mOffset = 0;
                    mFlingVelocity = 0;
                }
                mOffsetGoal = mOffset;
            } else {
                offsetDelta = mOffsetGoal - mOffset;

                if (offsetDelta > 10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta > 0)
                    offsetDelta = 1;
                else if (offsetDelta < -10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta < 0)
                    offsetDelta = -1;
                else
                    offsetDelta = 0;

                mOffset += offsetDelta;
            }
        }

        mWaveformView.setParameters(mStartPos, mEndPos, mOffset);
        mWaveformView.invalidate();

        mStartMarker.setContentDescription(
                getResources().getText(R.string.start_marker) + " " +
                        formatTime(mStartPos));
        mEndMarker.setContentDescription(
                getResources().getText(R.string.end_marker) + " " +
                        formatTime(mEndPos));
        int startX = mStartPos - mOffset - mMarkerLeftInset;
        if (startX + mStartMarker.getWidth() >= 0) {
            if (!mStartVisible) {
                // Delay this to avoid flicker
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mStartVisible = true;
                        mStartMarker.setAlpha(1f);
                    }
                }, 0);
            }
        } else {
            if (mStartVisible) {
                mStartMarker.setAlpha(0f);
                mStartVisible = false;
            }
            startX = 0;
        }
        int endX = mEndPos - mOffset - mEndMarker.getWidth() + mMarkerRightInset;
        if (endX + mEndMarker.getWidth() >= 0) {
            if (!mEndVisible) {
                // Delay this to avoid flicker
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mEndVisible = true;
                        mEndMarker.setAlpha(1f);
                    }
                }, 0);
            }
        } else {
            if (mEndVisible) {
                mEndMarker.setAlpha(0f);
                mEndVisible = false;
            }
            endX = 0;
        }
        //mStartMarker.setAlpha(1f);//change the transparency of startmark
        //mEndMarker.setAlpha(1f);//change the transparency of endmark
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                startX,
                mMarkerTopOffset,
                -mStartMarker.getWidth(),
                -mStartMarker.getHeight());
        mStartMarker.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                endX,
                (int) (getWindowManager().getDefaultDisplay().getHeight() * 0.333),
                -mStartMarker.getWidth(),
                -mStartMarker.getHeight());
        mEndMarker.setLayoutParams(params);
        if(mtimeflag!=null&&calculatecountsflag==0) {
            Pattern p = Pattern.compile("[0-9]");
            Matcher m = p.matcher(mtimeflag);
            if(m.find())
                minute=Integer.parseInt(m.group());
            p = Pattern.compile(":[0-9]+");
            m = p.matcher(mtimeflag);
            if(m.find()) {
                p = Pattern.compile("[0-9]+");
                m = p.matcher(m.group());
            }
            if(m.find())
                seconds=Integer.parseInt(m.group());
            if(minute!=0)
            {
                counts=minute*60000;
                calculatecountsflag=1;
            }
            else
            {
                counts=seconds*1000;
                if(counts>15000&&counts<30000)
                    counts=30000;
                else if(counts>7500&&counts<=15000)
                    counts=15000;
                else if(counts>0&&counts<=7500)
                    counts=7500;
                calculatecountsflag=1;
            }
        }

        //Toast.makeText(EditActivity.this,Double.toString(counts),Toast.LENGTH_SHORT).show();
        cutstartposition = (long) ((mStartPos*1.0 / ((RelativeLayout) findViewById(R.id.dynamiclayout)).getWidth()) * counts);
        cutfinalposition = (long) ((mEndPos*1.0 / ((RelativeLayout) findViewById(R.id.dynamiclayout)).getWidth()) * counts);
        //Toast.makeText(EditActivity.this,Double.toString(counts),Toast.LENGTH_SHORT).show();
        updatedotdisplay();
    }

    private String formatTime(int pixels) {
        if (mWaveformView != null && mWaveformView.isInitialized()) {
            return formatDecimal(mWaveformView.pixelsToSeconds(pixels));
        } else {
            return "";
        }
    }
    private String formatDecimal(double x) {
        int xWhole = (int)x;
        int xFrac = (int)(100 * (x - xWhole) + 0.5);

        if (xFrac >= 100) {
            xWhole++; //Round up
            xFrac -= 100; //Now we need the remainder after the round up
            if (xFrac < 10) {
                xFrac *= 10; //we need a fraction that is 2 digits long
            }
        }

        if (xFrac < 10)
            return xWhole + ".0" + xFrac;
        else
            return xWhole + "." + xFrac;
    }
    private void setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2);
    }

    private void setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2);
    }

    private void setOffsetGoal(int offset) {
        setOffsetGoalNoUpdate(offset);
        updateDisplay();
    }

    private void setOffsetGoalNoUpdate(int offset) {
        if (mTouchDragging) {
            return;
        }

        mOffsetGoal = offset;
        if (mOffsetGoal + mWidth / 2 > mMaxPos)
            mOffsetGoal = mMaxPos - mWidth / 2;
        if (mOffsetGoal < 0)
            mOffsetGoal = 0;
    }
    private int trap(int pos) {
        if (pos < 0)
            return 0;
        if (pos > mMaxPos)
            return mMaxPos;
        return pos;
    }

    private void updatedotdisplay() {
        int mOffset1 = mOffset;
        if (System.currentTimeMillis() - curretntime > 1000) {
                RelativeLayout dynamiclayout = (RelativeLayout) findViewById(R.id.dynamiclayout);
                if (bookmarklist.size() != 0) {
                    for (int k = 0; k < bookmarklist.size(); k++) {
                        dynamiclayout.removeView(bookmarklist.get(k));
                    }
                }
                //Toast.makeText(EditActivity.this,Integer.toString(mOffset),Toast.LENGTH_SHORT).show();
                for (int i = 0; i < dataList.size(); i++) {
                    if ((int) ((dataList.getitem(i).getBookmarktime() / counts) * dynamiclayout.getWidth()) - mOffset1 >= 0) {
                        //Toast.makeText(EditActivity.this,Integer.toString(dataList.getitem(i).getBookmarktime()/1000),Toast.LENGTH_SHORT).show();
                        TextView bookmark = new TextView(this);
                        //bookmark.setId(110);
                        bookmark.setText("●");
                        bookmark.setId(dataList.getitem(i).getBookmarktime() / 1000);
                        bookmark.setTextColor(android.graphics.Color.RED);
                        RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        lp1.setMargins((int) ((dataList.getitem(i).getBookmarktime() / counts) * dynamiclayout.getWidth()) - mOffset1
                                , (int) (getWindowManager().getDefaultDisplay().getHeight() * 0.2), bookmark.getWidth(), bookmark.getHeight());
//        lp1.addRule(RelativeLayout.ALIGN_TOP);
//        lp1.setMargins(30, 50, 100, 100);//(int left, int top, int right, int bottom)
                        dynamiclayout.addView(bookmark, lp1);
                        bookmarklist.add(bookmark);
                    } else {
                        //Toast.makeText(EditActivity.this,"delete",Toast.LENGTH_SHORT).show();
                        for (int j = 0; j < bookmarklist.size(); j++) {
                            // Toast.makeText(EditActivity.this,bookmarklist.get(j).getText().toString(),Toast.LENGTH_SHORT).show();
                            if (bookmarklist.get(j).getId() == dataList.getitem(i).getBookmarktime() / 1000)
                                dynamiclayout.removeView(bookmarklist.get(j));
                        }
                    }
                }

        }

    }
    private void finishOpeningSoundFile() {
        mWaveformView.setSoundFile(mSoundFile);
        mWaveformView.recomputeHeights(mDensity);

        mMaxPos = mWaveformView.maxPos();
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        mTouchDragging = false;

        mOffset = 0;
        mOffsetGoal = 0;
        mFlingVelocity = 0;
        resetPositions();
        if (mEndPos > mMaxPos)
            mEndPos = mMaxPos;
        updateDisplay();
        markerFocus(mEndMarker);
        markerFocus(mStartMarker);
    }
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v("Ringdroid", "EditActivity onConfigurationChanged");
        final int saveZoomLevel = mWaveformView.getZoomLevel();
        super.onConfigurationChanged(newConfig);

        loadgui();

        mHandler.postDelayed(new Runnable() {
            public void run() {
                mStartMarker.requestFocus();
                markerFocus(mStartMarker);

                mWaveformView.setZoomLevel(saveZoomLevel);
                mWaveformView.recomputeHeights(mDensity);

                updateDisplay();
            }
        }, 500);
    }
    public void InitFTPServerSetting() {
        // TODO Auto-generated method stub
        ftpUtils = FTPUtils.getInstance();
        boolean flag = ftpUtils.initFTPSetting("192.168.23.1", 21, "Anonymous", "123456");

    }
}
