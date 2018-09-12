package com.meem.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.ExifUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by naveen on 8/4/2016.
 * to display the individual image, and we can share or download the image 
 */
public class PhotosItemDetails extends Fragment implements MainActivity.ToolBarItemClickListener {

    final String TAG = "PhotosItemDetails";
    ImageView imageView;


    View mRootView;
    MainActivity mMainActivity;
    String path;

    HashMap<Integer, String> hashMap = new HashMap<>();


    Cursor cursor;
    String mVaultId;
    Byte catCode = MMPConstants.MMP_CATCODE_PHOTO;
    boolean isMirror;
    boolean isSdCard;
    GenDataInfo genDataInfo;
    private RestoreOrShareGenDataInterface mListener;
    private RelativeLayout mParentView;
    private ProgressDialog mInitProgress;

    public PhotosItemDetails() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isMirror = getArguments().getBoolean("isMirror");
        mVaultId = getArguments().getString(DetailsFragment.vaultId);
        genDataInfo = getArguments().getParcelable("GENOBJECT");
        mRootView = inflater.inflate(R.layout.photodetails, null);
        path = genDataInfo.destFPath;
        init();

        return mRootView;

    }

    private void init() {
        mInitProgress = CustomDialogProgress.ctor(getContext());
        findViewById();
        setImage();
    }

    private void setImage() {
        mMainActivity.showSimpleProgressBar(0, mInitProgress, "");

        try {

            File image = new File(path);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
            bitmap = ExifUtil.fixOrientation(path, bitmap);
            imageView.setImageBitmap(bitmap);
        }catch (Exception e){
            e.printStackTrace();
        }

//
//
//        imageView.setImageURI(Uri.fromFile(new File(path)));

        mInitProgress.dismiss();
    }

    private void findViewById() {
        imageView = (ImageView) mRootView.findViewById(R.id.imageview);

        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);

    }


    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        setEventListner(mMainActivity.getCablePresenter());
        mMainActivity.showShareAndRestoreIcon(true);
        mMainActivity.setOptionMenuContent(true, true, true, false);
        mMainActivity.setAppTitle(getString(R.string.photos));

    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }

    @Override
    public void onPause() {
        mMainActivity.showShareAndRestoreIcon(false);
        mMainActivity.setOptionMenuContent(false, false, false, false);
        super.onPause();
    }


    public void Share() {


        // Share Intent
        Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);

        // Type of file to share
        share.setType("image/jpeg");

        ArrayList<Uri> uris = new ArrayList<Uri>();
        String[] filePaths = new String[]{path};
        for (String file : filePaths) {
            File fileIn = new File(file);
            Uri u = Uri.fromFile(fileIn);
            uris.add(u);
        }
        share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        // Show the social share chooser list
        startActivity(Intent.createChooser(share, getString(R.string.share_photos)));

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


}
