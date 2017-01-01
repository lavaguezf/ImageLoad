package com.example.imageloadetest.Util;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imageloadetest.Bean.FolderBean;
import com.example.imageloadetest.R;

import java.util.List;

/**
 * Created by U310 on 2017/1/1.
 */

public class ListImgDirPopuWindow extends PopupWindow {

    private int width;
    private int height;
    private List<FolderBean>mDatas;
    private View convertView;
    private ListView mListView;

    private Context context;

    private OnSelectedListener mListener;
    public interface OnSelectedListener{
        void onSelected(FolderBean bean);
    }
    public void setOnSelectedListener (OnSelectedListener mListener){
       this.mListener=mListener;
    }

    public ListImgDirPopuWindow(Context context, List<FolderBean>mDatas) {
        this.context=context;
        caculateWidthHeight(context);
        convertView= LayoutInflater.from(context).inflate(R.layout.dir_list_item,null);
        this.mDatas=mDatas;
        setContentView(convertView);
        setWidth(width);
        setHeight(height);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
               if (event.getAction()==MotionEvent.ACTION_OUTSIDE){
                   dismiss();
                   return true;
               }
                return false;
            }
        });
        initView(context);

        initEvents();
    }
    private void initView(Context context) {
        mListView=(ListView)convertView.findViewById(R.id.id_list_dir);
        ListDirAdapter apapter = new ListDirAdapter(context,mDatas);
        mListView.setAdapter(apapter);


    }
    private void initEvents() {
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.i("aaca","itemlick-->"+position);
                    if (mListener!=null){
                        Log.i("aaca","mListener!=null-->");
                        mListener.onSelected(mDatas.get(position));
                    }
                }
            });
    }



    private void caculateWidthHeight(Context context) {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        width=metrics.widthPixels;
        height=(int)(metrics.heightPixels*0.9);

    }

    private class ListDirAdapter extends BaseAdapter {
        private List<FolderBean>mDatas;
        private LayoutInflater inflater;

        public ListDirAdapter(Context context,List<FolderBean>mDatas) {

           this.mDatas=mDatas;
            inflater=LayoutInflater.from(context);

        }

        @Override
        public int getCount() {
            return mDatas.size();
        }

        @Override
        public Object getItem(int position) {
            return mDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if (convertView==null){
                vh = new ViewHolder();
                convertView=inflater.inflate(R.layout.dir_list,parent,false);
                vh.mImg=(ImageView)convertView.findViewById(R.id.id_dir_item_image);
                vh.mCount=(TextView)convertView.findViewById(R.id.id_dir_item_count);
                vh.mDir=(TextView)convertView.findViewById(R.id.id_dir_item_name);
                convertView.setTag(vh);
            }
            else {
                vh=(ViewHolder)convertView.getTag();
            }
            FolderBean bean = mDatas.get(position);
            vh.mImg.setImageResource(R.mipmap.pictures_no);
            ImageLoader.getInstance().loadImage(bean.getFirstImgDir(),vh.mImg);
            vh.mCount.setText(bean.getCount()+"");
            vh.mDir.setText(bean.getDir());

            return convertView;
        }
        private class ViewHolder{
            private ImageView mImg;
            private TextView mDir;
            private TextView mCount;
        }
    }


}
