package okhttp;

import okhttp3.*;

import java.io.IOException;

public class OkHttpDemo {
    private static final String URL = "http://localhost:8808/test";

    public static void main(String[] args) throws IOException {
        syncGet();
//        asyncGet();
    }

    /**
     * 同步
     * @throws IOException
     */
    public static void syncGet() throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient(); // 创建OkHttpClient对象
        Request request = new Request.Builder().url(URL).build(); // 创建一个请求
        Response response = okHttpClient.newCall(request).execute(); // 返回实体
        if (response.isSuccessful()) { // 判断是否成功
            /**获取返回的数据，可通过response.body().string()获取，默认返回的是utf-8格式；
             * string()适用于获取小数据信息，如果返回的数据超过1M，建议使用stream()获取返回的数据，
             * 因为string() 方法会将整个文档加载到内存中。*/
            System.out.println(response.body().string()); // 打印数据
        } else {
            System.out.println("失败"); // 链接失败
        }
    }

    /**
     * 异步调用
     */
    public static void asyncGet() {
        OkHttpClient okHttpClient = new OkHttpClient(); // 创建OkHttpClient对象
        Request request = new Request.Builder().url(URL).build(); // 创建一个请求
        okHttpClient.newCall(request).enqueue(new Callback() { // 回调
            public void onResponse(Call call, Response response) throws IOException {
                // 请求成功调用，该回调在子线程
                System.out.println(response.body().string());
            }

            public void onFailure(Call call, IOException e) {
                // 请求失败调用
                System.out.println(e.getMessage());
            }
        });
    }
}
