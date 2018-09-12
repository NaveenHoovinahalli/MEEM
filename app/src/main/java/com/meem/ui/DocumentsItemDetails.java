package com.meem.ui;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.meem.androidapp.CablePresenter;
import com.meem.androidapp.MainActivity;
import com.meem.androidapp.R;
import com.meem.events.ResponseCallback;
import com.meem.mmp.messages.MMPConstants;
import com.meem.ui.utils.CustomDialogProgress;
import com.meem.ui.utils.MeemTextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Naveen on 8/4/2016.
 */
public class DocumentsItemDetails extends Fragment implements MainActivity.ToolBarItemClickListener, View.OnClickListener {

    View mRootView;
    MainActivity mMainActivity;
    String path;
    WebView mWebView;
    MeemTextView mViewButton;


    HashMap haspMap;


    Cursor cursor;
    String mVaultId;
    Byte catCode = MMPConstants.MMP_CATCODE_DOCUMENTS;
    boolean isMirror;
    boolean isSdCard;
    GenDataInfo genDataInfo;
    private RestoreOrShareGenDataInterface mListener;
    private RelativeLayout mParentView;
    private ProgressDialog mInitProgress;


    public DocumentsItemDetails() {

    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mVaultId = getArguments().getString(DetailsFragment.vaultId);
        isMirror = getArguments().getBoolean("isMirror");
        genDataInfo = getArguments().getParcelable("GENOBJECT");
        path = genDataInfo.destFPath;
        mRootView = inflater.inflate(R.layout.documentdetails, null);
        init();
        return mRootView;
    }

    private void init() {
        mInitProgress = CustomDialogProgress.ctor(getContext());
        findViewById();
        setOnClickListener();
        viewDocuments();
//        viewDoc();
//        viewExcel();

    }

    private void setOnClickListener() {
        mViewButton.setOnClickListener(this);
    }

    private void setWebview() {
        String newPath = "file://" + path;

        mWebView.loadUrl(newPath);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setEventListner(this);
        setEventListner(mMainActivity.getCablePresenter());
        mMainActivity.showShareAndRestoreIcon(true);
        mMainActivity.setOptionMenuContent(true, true, true, false);
        mMainActivity.setAppTitle(getResources().getString(R.string.documents));

    }

    private void setEventListner(CablePresenter cablePresenter) {
        mListener = cablePresenter;
    }

    private void findViewById() {

        mWebView = (WebView) mRootView.findViewById(R.id.webview);
        mViewButton = (MeemTextView) mRootView.findViewById(R.id.viewButton);
        mParentView = (RelativeLayout) mRootView.findViewById(R.id.parentView);

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
        startActivity(Intent.createChooser(share, getString(R.string.share_documents)));
    }

    public void viewDocuments() {
        File file = new File(path);
        Uri uri = Uri.fromFile(file);

        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        String mimeType = getMimeType(path);

        // BugFix: 20Feb2017: Arun: This can very well be null for unsupported document formats
        if (mimeType == null) {
            Toast.makeText(getActivity(), "No installed app to view this type of file.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getActivity(), "-" + mimeType, Toast.LENGTH_LONG);
        if (mimeType.equals("text/plain") || mimeType.equals("text/html")) {
            mViewButton.setVisibility(View.GONE);
            mWebView.setVisibility(View.VISIBLE);
            setWebview();
            return;
        }

        newIntent.setDataAndType(Uri.fromFile(file), mimeType);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), "No installed app to view this type of file.", Toast.LENGTH_LONG).show();
        }
    }

    private void viewDoc() {

        String path = "/storage/emulated/0/Download/HelloWorld.docx";
        File file = new File(path);
        Uri uri = Uri.fromFile(file);

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        String type = "application/msword";
        intent.setDataAndType(Uri.fromFile(file), type);
        startActivity(intent);

    }

    private void viewExcel() {
        String path = "/storage/emulated/0/ShareImage/naveendoctest.xlsx";
        File file = new File(path);
        Uri uri = Uri.fromFile(file);


        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.ms-excel");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), "No Application Available to View Excel", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        viewDocuments();
    }
}
