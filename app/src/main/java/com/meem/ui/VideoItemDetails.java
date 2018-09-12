package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by SCS on 8/4/2016.
 */
public class VideoItemDetails extends Fragment implements MainActivity.ToolBarItemClickListener, IVLCVout.Callback, SeekBar.OnSeekBarChangeListener {

    private final static int VideoSizeChanged = -1;
    public Button mPlayPause, mStop;
    View mRootView;
    MainActivity mMainActivity;
    String path;
    HashMap haspMap;
    Cursor cursor;
    ArrayList<String> vCard;
    String vfile;
    String mVaultId;
    Byte catCode = MMPConstants.MMP_CATCODE_VIDEO;
    boolean isMirror;
    boolean isSdCard;
    GenDataInfo genDataInfo;
    SeekBar mSeekBar;
    private RestoreOrShareGenDataInterface mListener;
    private RelativeLayout mParentView;
    // display surface
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    // media player
    private LibVLC libvlc;
    private org.videolan.libvlc.MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private ProgressDialog mInitProgress;
    private org.videolan.libvlc.MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    public VideoItemDetails() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mVaultId = getArguments().getString(DetailsFragment.vaultId);
        isMirror = getArguments().getBoolean("isMirror");
        genDataInfo = getArguments().getParcelable("GENOBJECT");
        if (genDataInfo != null) {
            path = genDataInfo.destFPath;
        }

        mRootView = inflater.inflate(R.layout.videodetails, null);
        init();
        return mRootView;

    }

    private void init() {
        findViewById();
        mInitProgress = CustomDialogProgress.ctor(getContext());
        mSurface = (SurfaceView) mRootView.findViewById(R.id.surface);
        holder = mSurface.getHolder();
        mSeekBar.setOnSeekBarChangeListener(this);


        mStop = (Button) mRootView.findViewById(R.id.stopBtn);
        mStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                mMediaPlayer.stop();
            }
        });

        mPlayPause = (Button) mRootView.findViewById(R.id.pauseBtn);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mPlayPause.setText(getResources().getString(R.string.play));
                } else {
                    mMediaPlayer.play();
                    mPlayPause.setText(getResources().getString(R.string.pause));
                }
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        mMainActivity.showShareAndRestoreIcon(true);
        setEventListner(mMainActivity.getCablePresenter());
        mMainActivity.setOptionMenuContent(true, true, true, false);
        mMainActivity.setAppTitle(getResources().getString(R.string.videos));
        if (holder != null)
            createPlayer(path);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }

    @Override
    public void onPause() {
        super.onPause();
        mMainActivity.showShareAndRestoreIcon(false);
        mMainActivity.setOptionMenuContent(false, false, false, false);
        if (mMediaPlayer != null)
            mMediaPlayer.pause();
    }

    private void findViewById() {


        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);
        mSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar);

    }

    @Override
    public void onEditOrCancelClickListner(String text) {

    }

    @Override
    public void onSaveClickListner() {

    }

    @Override
    public void onShareIconClickListener() {
        Share();


    }

    @Override
    public void onRestoreIconListener() {
        ArrayList<GenDataInfo> genDataInfoArrayList = new ArrayList<>();
        genDataInfoArrayList.add(genDataInfo);
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");
        mListener.onRestoreGenData(mVaultId, catCode, genDataInfoArrayList, isMirror, new ResponseCallback() {
            @Override
            public boolean execute(boolean result, Object info, Object extraInfo) {
                mInitProgress.dismiss();
                if (result) {
                    mMainActivity.showToast(getString(R.string.success));
                } else {
                    mMainActivity.showToast(getString(R.string.failed));
                }
                return false;
            }
        });
    }

    @Override
    public void onSelectIconClickListener() {

    }

    @Override
    public void onBackButtonPressed() {

    }

    @Override
    public void onDeleteIconClickListener() {

    }

    public void Share() {

        Intent share = new Intent(Intent.ACTION_SEND);

        share.setType("image/jpeg");

        File file = new File(path);

        Uri uri = Uri.fromFile(file);

        share.putExtra(Intent.EXTRA_STREAM, uri);

        startActivity(Intent.createChooser(share, getString(R.string.share_videos)));

    }

    private void createPlayer(String media) {
        releasePlayer();
        try {
         /*   if (media.length() > 0) {
                Toast toast = Toast.makeText(getActivity(), media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();
            }*/

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(options);


            //libvlc.setOnHardwareAccelerationError(this);
            if (holder != null)
                holder.setKeepScreenOn(true);

            // Create media player
            mMediaPlayer = new org.videolan.libvlc.MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);


            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();


            MediaController mediaController = new MediaController(getActivity());
            mediaController.setAnchorView(mSurface);

            Media m = new Media(libvlc, media);
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();

        } catch (Exception e) {
            Log.e("Exception", "--", e);
            Toast.makeText(getActivity(), "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    // TODO: handle this cleaner
    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;


        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {

    }

    /*************
     * Surface
     *************/
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if (holder == null || mSurface == null)
            return;

        // get screen size
        int w = getActivity().getWindow().getDecorView().getWidth();
        int h = getActivity().getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        holder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mMediaPlayer.setPosition((float) progress / 100);
            mMediaPlayer.play();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayPause.setText("pause");
        }

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    private static class MyPlayerListener implements org.videolan.libvlc.MediaPlayer.EventListener {
        private WeakReference<VideoItemDetails> mOwner;

        public MyPlayerListener(VideoItemDetails owner) {
            mOwner = new WeakReference<VideoItemDetails>(owner);
        }

        @Override
        public void onEvent(org.videolan.libvlc.MediaPlayer.Event event) {
            VideoItemDetails player = mOwner.get();

            switch (event.type) {
                case org.videolan.libvlc.MediaPlayer.Event.EndReached:
                    player.releasePlayer();
                    player.mSeekBar.setProgress(100);
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Playing:
//                    player.mPlayPause.setText("pause");
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Paused:
//                    player.mPlayPause.setText("play");
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Stopped:
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.PositionChanged:
                    player.mSeekBar.setProgress((int) (player.mMediaPlayer.getPosition() * 100));
                    break;
                default:
                    break;
            }
        }

    }

}
