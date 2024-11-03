package xyz.xingfeng.Shiro.network;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 */
public class NetRequest {
    //主机域名端口
    public static final String IP = "127.0.0.1:5700";
    //获取登录号信息
    public static final String GET_LOGIN_INFO = "get_login_info";
    //获取在线机型
    public static final String GET_MODEL_SHOW ="_get_model_show";
    //

    /**
     * Get请求
     * @param url url链接
     * @throws IOException io错误
     */
    public static String get(String url) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = okHttpClient.newCall(request).execute();
        String string = response.body().string();
        System.out.println(string);
        return string;
    }/**
     * Get请求
     * @param url url链接
     * @throws IOException io错误
     */
    public static InputStream getImage(String url) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = okHttpClient.newCall(request).execute();
        InputStream inputStream = response.body().byteStream();
        return inputStream;
    }
    /**
     * Get请求，带参
     * @param url url链接
     * @param paramMap 请求参数
     * @throws IOException io错误
     */
    public void get(String url, Map<String, Object> paramMap) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder requestbuilder = new Request.Builder()
                .get();

        StringBuilder urlbuilder = new StringBuilder(url);
        if (Objects.nonNull(paramMap)) {
            urlbuilder.append("?");
            paramMap.forEach((key, value) -> {
                try {
                    urlbuilder.append(URLEncoder.encode(key, "utf-8"))
                            .append("=")
                            .append(URLEncoder.encode(String.valueOf(value), "utf-8"))
                            .append("&");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            });
            urlbuilder.deleteCharAt(urlbuilder.length() - 1);
        }

        Request request = requestbuilder.url(urlbuilder.toString()).build();
        Response response = okHttpClient.newCall(request).execute();
        String string = response.body().string();
        System.out.println(string);
    }

    /**
     * Get请求，带参，带头
     * @param url url链接
     * @param paramMap 请求参数
     * @param heardMap 请求头内容
     * @throws IOException io错误
     */
    public String get(String url, Map<String, Object> paramMap,Map<String, String> heardMap) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS) // 设置连接超时时间
                .readTimeout(30, TimeUnit.SECONDS) // 设置读取超时时间
                .build();
        Request.Builder requestbuilder = new Request.Builder()
                .get();

        //增加参数
        StringBuilder urlbuilder = new StringBuilder(url);
        if (Objects.nonNull(paramMap)) {
            urlbuilder.append("?");
            paramMap.forEach((key, value) -> {
                try {
                    urlbuilder.append(URLEncoder.encode(key, "utf-8"))
                            .append("=")
                            .append(URLEncoder.encode(String.valueOf(value), "utf-8"))
                            .append("&");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            });
            urlbuilder.deleteCharAt(urlbuilder.length() - 1);
        }
        //增加请求头
        Request.Builder heardBuilder = requestbuilder.url(urlbuilder.toString());
        for (Map.Entry<String, String> stringObjectEntry : heardMap.entrySet()) {
            heardBuilder.addHeader(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        }

        Request request = heardBuilder.build();
        Response response = okHttpClient.newCall(request).execute();
        String string = response.body().string();

        System.out.println(response.message());
        System.out.println(response.code());
        return string;
    }

    /**
     * post请求
     * @param url 请求的url
     * @param json json格式字符串
     * @param heardMap 请求头内容
     * @throws IOException io错误
     */
    public static String post(String url, String json, Map<String, String> heardMap) throws IOException {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String requestBody = json;
        Request.Builder requestbuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(mediaType, requestBody));
        //增加请求头
        for (Map.Entry<String, String> stringObjectEntry : heardMap.entrySet()) {
            requestbuilder.addHeader(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        }

        Request request = requestbuilder.build();
        OkHttpClient okHttpClient = new OkHttpClient();
        Response response = okHttpClient.newCall(request).execute();
        String string = response.body().string();
        System.out.println(string);
        System.out.println(response.message());
        System.out.println(response.code());
        return string;
    }

    public static String postFormData(String url,String file) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image",file,
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                new File(file)))
                .build();
        Request request = new Request.Builder()
                .url(url+"/detect")
                .method("POST", body)
                .addHeader("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                .build();
        Response response = client.newCall(request).execute();
        String s1 = response.body().string();
        return s1;
    }

    public static String postFormUrl(String url,String file) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image",file)
                .build();
        Request request = new Request.Builder()
                .url(url+"/detect")
                .method("POST", body)
                .addHeader("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                .build();
        Response response = client.newCall(request).execute();
        String s1 = response.body().string();
        return s1;
    }
}
