package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTextView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Naveen on 8/4/2016.
 */
public class MusicItemDetails extends Fragment implements MainActivity.ToolBarItemClickListener {

    View mRootView;
    MediaPlayer mediaPlayer;
    VideoView mVideoView;
    MainActivity mMainActivity;


    String path;

    HashMap haspMap;


    Cursor cursor;
    ArrayList<String> vCard;
    String vfile;
    String mVaultId;
    Byte catCode = MMPConstants.MMP_CATCODE_MUSIC;
    boolean isMirror;
    boolean isSdCard;
    GenDataInfo genDataInfo;
    MeemTextView mTitle, mPath, mSize;
    private RestoreOrShareGenDataInterface mListener;
    private RelativeLayout mParentView;
    private ProgressDialog mInitProgress;


    public MusicItemDetails() {

    }

    public static String convertBtoKB(long size) {
        if (size <= 0) return "--";
        double temp = ((double) size / 1024) / 1024;
        if (temp < .01) return "0.01";
        return new DecimalFormat("#0.00").format(temp);
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
        path = genDataInfo.destFPath;
        mRootView = inflater.inflate(R.layout.musicdetails, null);
        init();
        return mRootView;

    }

    private void init() {
        mInitProgress = CustomDialogProgress.ctor(getContext());
        findViewById();
        setMediaPlayer();
        setValues();
    }

    private void setValues() {

        File file = new File(path);
        mTitle.setText(file.getName());
        mPath.setText(path);
        mSize.setText(convertBtoKB(Long.parseLong(genDataInfo.fSize)) + "MB");


    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        setEventListner(mMainActivity.getCablePresenter());
        mMainActivity.showShareAndRestoreIcon(true);
        mMainActivity.setOptionMenuContent(true, true, true, false);
        mMainActivity.setAppTitle(getString(R.string.music));

    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }

    private void findViewById() {

        mVideoView = (VideoView) mRootView.findViewById(R.id.videoView);
        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);
        mTitle = (MeemTextView) mRootView.findViewById(R.id.title);
        mPath = (MeemTextView) mRootView.findViewById(R.id.path);
        mSize = (MeemTextView) mRootView.findViewById(R.id.size);
    }

    private void setMediaPlayer() {


        MediaController mediaController = new MediaController(getActivity()) {
            @Override
            public void show() {
                super.show();
            }
        };
        mVideoView.setVideoPath(path);
        mVideoView.setMediaController(mediaController);
        mVideoView.requestFocus();
        mVideoView.start();
        mediaController.show(5000);


    }

    @Override
    public void onPause() {
        super.onPause();
        mMainActivity.showShareAndRestoreIcon(false);
        mMainActivity.setOptionMenuContent(false, false, false, false);
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
        startActivity(Intent.createChooser(share, getResources().getString(R.string.share_music)));

    }

}
