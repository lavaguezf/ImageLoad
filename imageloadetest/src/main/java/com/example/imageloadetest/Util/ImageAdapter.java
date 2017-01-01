package com.example.imageloadetest.Util;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.imageloadetest.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by U310 on 2017/1/1.
 */

public class ImageAdapter extends BaseAdapter {
    //存储选中图片状态
    private static Set<String>mselectedImg = new HashSet<>();

    private List<String> mImagePaths;
    private String path;
    private LayoutInflater inflater;



    public ImageAdapter(Context context, List<String>mDatas, String path) {
        this.mImagePaths=mDatas;
        this.path=path;
        this.inflater=LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mImagePaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImagePaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder vh ;
        if (convertView==null){
            convertView=inflater.inflate(R.layout.item,parent,false);
            vh = new ViewHolder();
            vh.imageButton=(ImageButton)convertView.findViewById(R.id.id_item_select);
            vh.imageView=(ImageView)convertView.findViewById(R.id.id_item_image);
            convertView.setTag(vh);
        }else {
            vh=(ViewHolder)convertView.getTag();
        }
        vh.imageView.setImageResource(R.mipmap.pictures_no);
        vh.imageButton.setImageResource(R.mipmap.picture_unselected);
        vh.imageView.setColorFilter(null);
//            MyImageLoader.getInstance(3, MyImageLoader.Type.LIFO)
//                    .loadImage(path+"/"+mImagePaths.get(position),vh.imageView);
        //showImage出错。。
        ImageLoader.getInstance(3,ImageLoader.Type.LIFO)
                .loadImage(path+"/"+mImagePaths.get(position),vh.imageView);
        final String filePath = path+"/"+mImagePaths.get(position);
        vh.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filePath = path+"/"+mImagePaths.get(position);
                if (mselectedImg.contains(filePath)){
                    mselectedImg.remove(filePath);
                    vh.imageView.setColorFilter(null);
                    vh.imageButton.setImageResource(R.mipmap.picture_unselected);
                }
                else {
                    mselectedImg.add(filePath);
                    vh.imageView.setColorFilter(Color.parseColor("#77000000"));
                    vh.imageButton.setImageResource(R.mipmap.pictures_selected);
                }
                //刷新所有数据会导致闪屏
//                notifyDataSetChanged();
            }
        });
        if (mselectedImg.contains(filePath)){
            vh.imageView.setColorFilter(Color.parseColor("#77000000"));
            vh.imageButton.setImageResource(R.mipmap.pictures_selected);
        }
        return convertView;
    }
    private   class ViewHolder{
        ImageView imageView;
        ImageButton imageButton;
    }
}
