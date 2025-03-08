package xyz.xingfeng.Shiro.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用来存储Pixiv排行榜的类
 * @author xingfeng
 */
public class PixivTop {
    public static final String PIXIV_RANKING_URL = "https://www.pixiv.net/ranking.php?mode=male_r18&format=json";
    InetSocketAddress proxyAddress = new InetSocketAddress("127.0.0.1", 7897);
    public PixivTop() throws Exception {
        List<String> urls = new ArrayList<>();
        JSONObject json = new JSONObject();
        // 创建代理服务器
        Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);

        // 创建 OkHttpClient 并设置代理
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();

        // 创建请求
        Request request = new Request.Builder()
                .url(PIXIV_RANKING_URL)
                .addHeader("cookie","")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                json = new JSONObject(response.body().string());
                JSONArray contents = json.getJSONArray("contents");
                for (int i = 0; i < contents.length(); i++) {
                    //用正则表达式提取图片url
                    String string = contents.getJSONObject(i).getString("url");
                    String pattern = "img/(.*)p0";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(string);
                    int count = 0;
                    //提取前10张图片的url
                    if (m.find()) {
                        //构建下载链接
                        urls.add("https://i.pximg.net/img-original/img/"+m.group(1));
                        count++;
                        if (count == 10) {
                            break;
                        }
                    }

                }
            } else {
                System.out.println("Request failed: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String url : urls) {
            DwonloadImage(url);
        }
    }

    public void DwonloadImage(String url) throws Exception {

        Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .connectionSpecs(Collections.singletonList(
                        new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                                .allEnabledCipherSuites() // 使用所有默认支持的 CipherSuites
                                .build()
                ))
                .build();
        int count = 0;
        String endName = ".jpg";
        while (true) {
            String newUrl = url + "p" + count + endName;
            Request request = new Request.Builder()
                    .url(newUrl)
                    .addHeader("referer", "https://www.pixiv.net/")
                    .addHeader("cookie", "p_ab_id=0; p_ab_id_2=5; p_ab_d_id=985308277; yuid_b=FJUiJmI; privacy_policy_notification=0; a_type=0; login_ever=yes; privacy_policy_agreement=7; c_type=22; b_type=1; PHPSESSID=55434770_WCVAjIAOG56yDFqeqzwbV4bl5pCOMUiq; device_token=6f62c30588fac868ce8d8d1ccc927469; __cf_bm=5xqw2uCbUxMB3mB6PNwcYmsNfpOHiPhj6gHGgdmUwjs-1732615273-1.0.1.1-PSHOkd2CCn_JZhDlB2dcbtZtnjs8sklYcs1og3Zaal0nYxc2ysT4Xlqtu2UAK.yPXvFLDMKqxQ6mMwqflAmWAZij9Q2B6v2HFreZZe9SaPM; cf_clearance=fhrYJBRlLVG.ZRPv9nnJpQL3txP8riIIE_NwnDyNJXw-1732615276-1.2.1.1-CDWI9HG5gd8pqsDY5J0NPadKmoUoopFctFCJJVilIxpXNCfA0Or3.JOh63vT1.m1GdeoDVi43T8EFkxOArZ4wuPM6KGsQnU71.EB5F24aNfA4b_H23Zau7T.u4IZiFInn1auYsduWM9HvwH1BLo_dgGap7Bi46YpGswOUi6PYzC2vTUxYi_2M.Pg5iT22P_k4BoL5Uw3LRHPHBsSxocdcmxV9J70uyCLuj0iUw5yRv_ZHUVVUJ4lDqM6vHP0sW63rgTGSf_wlWsMDv_nC8bmVSTSZaC9L66D2chgPKRjSbhFmCmbBo5fas2KKabe1c_oR772DAQtsRdVA1nii2iHsubVyIPUV8h0Uc4CxxpzV1hnRsN_jUqjkPXWf7Ivv8QB67XNRPUYXza9JnqbSz6GGQ")
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.code() == 200) {
                    LocalDate currentDate = LocalDate.now();
                    int year = currentDate.getYear();
                    int month = currentDate.getMonthValue();
                    int day = currentDate.getDayOfMonth();
                    String path = Paths.get(year + "-" + month + "-" + day).toString();
                    if (!Paths.get(path).toFile().exists()) {
                        Paths.get(path).toFile().mkdirs();
                    }
                    String fileName = url.substring(url.lastIndexOf("/"))+"p"+count+endName;
                    try (InputStream inputStream = response.body().byteStream();
                         FileOutputStream fileOutputStream = new FileOutputStream(path + fileName)) {
                        fileOutputStream.write(inputStream.readAllBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if (response.code() == 404) {
                    if (count == 0 && ".jpg".equals(endName)){
                        //如果是第一张图片，尝试下载png格式图片
                        endName = ".png";
                        continue;
                    }else if (count == 0 && ".png".equals(endName)){
                        //如果是第一张图片，尝试下载gif格式图片
                        endName = ".gif";
                        continue;
                    }else if (count == 0 && ".gif".equals(endName)){
                        //尝试完所有格式图片，仍然下载失败，退出循环，抛出异常
                        throw new Exception("下载失败，已尝试所有格式图片");
                    }
                    if (count >= 0){
                        //如果是大于0，却返回404，说明已经下载完最后一张图片，退出循环
                        break;
                    }
                }
                count++;
                response.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
