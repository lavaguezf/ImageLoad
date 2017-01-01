package com.example.imageloadetest.Util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by U310 on 2016/12/31.
 */

public class ImageLoader {
    private static ImageLoader mInstance;
    /**
     * 图片缓存
     */
    private LruCache<String, Bitmap> mLruCache;
    /**
     * 线程池和默认线程数
     */
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT = 1;
    /**
     * 队列的调度方式
     */
    public static Type mType = Type.LIFO;



    public enum Type {
        FIFO, LIFO
    }

    /**
     * 线程队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 后台轮询线程
     */
    private Thread mPoolThread ;
    private Handler mPoolThreadHandle;
    /**
     * ui handle
     */
    private Handler mUiHandle;
    //信号量，为零时可以阻塞线程
    private Semaphore mSemaphpreAddTask = new Semaphore(0);

    private Semaphore mSemaphorePoolThread;
    /**
     * 单例模式，两层判断提高效率
     *
     * @return
     */
    public static ImageLoader getInstance()
    {

        if (mInstance == null)
        {
            synchronized (ImageLoader.class)
            {
                if (mInstance == null)
                {
                    mInstance = new ImageLoader(1, Type.LIFO);
                }
            }
        }
        return mInstance;
    }
    public static ImageLoader getInstance(int threadCount, Type type)
    {

        if (mInstance == null)
        {
            synchronized (ImageLoader.class)
            {
                if (mInstance == null)
                {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    private ImageLoader(int threadCount, Type type)
    {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type) {
        //线程池线程
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandle = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池取出一个线程进行执行
                       mThreadPool.execute(getTaskFromEnque());
                        //没执行完了一个任务释放一个信号量
                        try {
                            mSemaphorePoolThread.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放信号量
                mSemaphpreAddTask.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory /4;
        mLruCache = new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getHeight()*value.getRowBytes();
            }
        };
        mThreadPool= Executors.newFixedThreadPool(threadCount);
        mTaskQueue=new LinkedList<Runnable>();
        mType=type ==null?Type.LIFO:type;
        mSemaphorePoolThread=new Semaphore(threadCount);
    }

    private Runnable getTaskFromEnque() {
        if (mType==Type.FIFO){
            return mTaskQueue.removeFirst();
        }else {
            return  mTaskQueue.removeLast();
        }

    }

    public void loadImage(final String path, final ImageView imageView){
        imageView.setTag(path);
        if (mUiHandle==null){
            mUiHandle=new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg) {
                    //获取到图片为imageview回调设置图片。
                    ImgBeanHolder holder = (ImgBeanHolder)msg.obj;
                    String path = holder.path;
                    ImageView iv = holder.imageView;
                    Bitmap bm = holder.bitmap;
                    if (iv.getTag().toString().equals(path)){
                        iv.setImageBitmap(bm);
                    }
                }
            };
        }
        //根据path在缓存中读取bm
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm!=null){
            //使用ui线程显示imageview
            Message ms = Message.obtain();
            ImgBeanHolder holder = new ImgBeanHolder();
            holder.bitmap=bm;
            holder.path=path;
            holder.imageView=imageView;
            ms.obj=holder;
            mUiHandle.sendMessage(ms);
        }
        else {
            addTask(new Runnable(){
                @Override
                public void run() {
                    //加载图片,压缩图片
                    int width=getImageViewSize(imageView).width;
                    int height=getImageViewSize(imageView).height;

                    //压缩图片
                    Bitmap bm = getBitmapFromSize(path,width,height);
                    //
                    addBitmaoToCache(path,bm);
                    //使用ui线程显示imageview
                    Message ms = Message.obtain();
                    ImgBeanHolder holder = new ImgBeanHolder();
                    holder.bitmap=bm;
                    holder.path=path;
                    holder.imageView=imageView;
                    ms.obj=holder;
                    mUiHandle.sendMessage(ms);
                    mSemaphorePoolThread.release();
                }
            });
        }
    }

    private void addBitmaoToCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path)==null){
            if (bm!=null){
                mLruCache.put(path,bm);
            }
        }
    }

    private Bitmap getBitmapFromSize(String path, int width, int height) {
        //获取到图片的宽和高并不把图片加载到内存中
        BitmapFactory.Options options= new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize=caculateSampleSize(options,width,height);
        //使用获取到的inSamplesize解析
        options.inJustDecodeBounds=false;
        Bitmap bm = BitmapFactory.decodeFile(path,options);
        return bm;
    }

    /**
     * 根据需求的宽和高和图片的宽高计算实际宽和高
     * @param options
     * @param rwidth
     * @param rheight
     * @return
     */
    private int caculateSampleSize(BitmapFactory.Options options, int rwidth, int rheight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (width>rwidth||height>rheight){
            //压缩
            int widthRadio = Math.round(width*1.0f/rwidth);
            int heightRadio = Math.round(height*1.0f/rwidth);
            inSampleSize=Math.max(widthRadio,heightRadio);
        }
        return inSampleSize;
    }

    private ImageSize getImageViewSize(ImageView iv) {
        DisplayMetrics metrics = iv.getResources().getDisplayMetrics();
        ImageSize imageSize = new ImageSize();
        ViewGroup.LayoutParams params = iv.getLayoutParams();

        int width = iv.getWidth();
        if (width<=0){
            width=params.width;
        }
        if (width<0){
            width=iv.getMaxWidth();
        }
        if (width<0){
            //屏幕宽度
            width=metrics.widthPixels;
        }
        int height = iv.getHeight();
        if (width<=0){
            width=params.height;
        }
        if (width<0){
            width=iv.getMaxHeight();
        }
        if (width<0){
            //屏幕宽度
            width=metrics.heightPixels;
        }
        imageSize.height=height;
        imageSize.width=width;
        return imageSize;
    }
//同步，避免多个线程同时缩信号
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        if (mPoolThreadHandle==null){
            try {
                mSemaphpreAddTask.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mPoolThreadHandle.sendEmptyMessage(1);
    }

    public Bitmap getBitmapFromLruCache(String path){

        return mLruCache.get(path);
    }

    public class ImgBeanHolder{
        String path;
        Bitmap bitmap;
        ImageView imageView;
    }
    public class ImageSize{
        int width;
        int height;
    }
}
