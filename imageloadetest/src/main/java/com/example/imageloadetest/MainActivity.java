package com.example.imageloadetest;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imageloadetest.Bean.FolderBean;
import com.example.imageloadetest.Util.ImageAdapter;
import com.example.imageloadetest.Util.ImageLoader;
import com.example.imageloadetest.Util.ListImgDirPopuWindow;


import java.io.File;
import java.io.FilenameFilter;
import java.security.KeyRep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private GridView mGridLayoutl;
    private List<String> mImgs;
    private ImageAdapter mImageAdapter;

    private RelativeLayout bottomLayout;
    private TextView mDirName;
    private TextView mDirCount;

    private int mMaxCount ;

    private File mCurrentFile;
    private int mCurrentCount;

    private List<FolderBean> mFolderBeanList = new ArrayList<>();

    private ProgressDialog progressDialog;

    private ListImgDirPopuWindow mPopuWindow;

    private Handler mHandle = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            progressDialog.dismiss();
            bindDatasToGridView();
        }
    };

    private void bindDatasToGridView() {
        if (mCurrentFile==null) {
            Toast.makeText(this, "没有图片", Toast.LENGTH_LONG);
            return;
        }
        mImgs = Arrays.asList(mCurrentFile.list());

        mImageAdapter=new ImageAdapter(this,mImgs,mCurrentFile.getAbsolutePath());
        mGridLayoutl.setAdapter(mImageAdapter);
        mDirCount.setText(""+mMaxCount);
        mDirName.setText(mCurrentFile.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDatas();
        initEvents();
    }

    private void initView() {
        mGridLayoutl = (GridView) findViewById(R.id.id_gridview);
        bottomLayout = (RelativeLayout) findViewById(R.id.bootom_layout);
        mDirName = (TextView) findViewById(R.id.bottom_dir);
        mDirCount = (TextView) findViewById(R.id.bottom_counts);
        bottomLayout.getBackground().setAlpha(150);
        //初始化popuwindow
        mPopuWindow = new ListImgDirPopuWindow(this,mFolderBeanList);
        mPopuWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        mPopuWindow.setOnSelectedListener(new ListImgDirPopuWindow.OnSelectedListener() {
            @Override
            public void onSelected(FolderBean bean) {
                Log.i("aaca","popupWindow selected");
                mCurrentFile = new File(bean.getDir());
                mImgs = Arrays.asList(mCurrentFile.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.endsWith(".png")||name.endsWith(".jpg")){
                            return  true;
                        }
                        return false;
                    }
                }));
                mImageAdapter = new ImageAdapter(MainActivity.this
                        ,mImgs
                        ,mCurrentFile.getAbsolutePath());
                mGridLayoutl.setAdapter(mImageAdapter);
                mDirCount.setText(mImgs.size()+"");
                mDirName.setText(mCurrentFile.getName());
                mPopuWindow.dismiss();
            }
        });
    }

    /**
     * 内容区域变亮
     */
    private void lightOn() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha=1.0f;
        getWindow().setAttributes(params);
    }
    /**
     * 内容区域变an
     */
    private void lightDown() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha=.3f;
        getWindow().setAttributes(params);
    }

    /**
     * 利用contentprovider扫描所有图片
     */
    private Set<String> mDir = new HashSet<>();
    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可用", Toast.LENGTH_LONG);
            return;
        }
        progressDialog = ProgressDialog.show(this, null, "loading...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();

//                Cursor cursor = cr.query(mImageUri
//                        , null
//                        , MediaStore.Images.Media.MIME_TYPE + "=?or" + MediaStore.Images.Media.MIME_TYPE + "=?"
//                        , new String[]{"image/jpg", "image/png"}
//                        , MediaStore.Images.Media.DATE_MODIFIED);
                Cursor cursor = cr.query(mImageUri, null,
                        MediaStore.Images.Media.MIME_TYPE + "=? or "
                                + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[] { "image/jpeg", "image/png" },
                        MediaStore.Images.Media.DATE_MODIFIED);
                while (cursor.moveToNext()){
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile==null){
                        continue;
                       }
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;
                    if (mDir.contains(dirPath)){
                        continue;
                    }else {
                        mDir.add(dirPath);
                        folderBean=new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgDir(path);
                    }
                    if (parentFile.list()==null){
                        continue;
                    }
                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            if (name.endsWith(".png")||name.endsWith(".jpg")){
                                return  true;
                            }
                            return false;
                        }
                    }).length;
                    folderBean.setCount(picSize);
                    mFolderBeanList.add(folderBean);

                    if (picSize>mMaxCount){
                       mMaxCount = picSize;
                        mCurrentFile=parentFile;
                    }

                }
                cursor.close();
                mDir=null;
                //图片查询完成
                mHandle.sendEmptyMessage(2);
            }
        }).start();

    }
    private void initEvents() {
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopuWindow.setAnimationStyle(R.style.popuWindowSlideAnima);
                mPopuWindow.showAsDropDown(bottomLayout,0,0);
                lightDown();
            }
        });
    }
    private long mExitTime =0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK){
            if((System.currentTimeMillis()-mExitTime)>1500){
                Toast.makeText(this,"再按一次退出",Toast.LENGTH_SHORT).show();
                mExitTime=System.currentTimeMillis();
            }else {
                System.exit(0);
            }
            return  true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
