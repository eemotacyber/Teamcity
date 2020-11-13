package com.cyberark.common;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.cyberark.common.exceptions.*;

import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.io.UnsupportedEncodingException;

public class HttpClient {

    public static Boolean DEBUG=false;

    // ==========================================
    // void disableSSL()
    //   from: https://nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
    //
    public static void disableSSL() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch(KeyManagementException e) {
            e.printStackTrace();
        }

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

    } // disableSSL



    public static String getThrowException(String urlString, String authHeader) throws InvalidHttpStatusCodeException {
        return responseThrowException(get(urlString, authHeader));
    }

    public static String postThrowException(String urlString, String authHeader, String body) throws InvalidHttpStatusCodeException{
        return responseThrowException(post(urlString, authHeader, body));
    }

    public static String patchThrowException(String urlString, String authHeader, String body) throws InvalidHttpStatusCodeException {
        return responseThrowException(patch(urlString, authHeader, body));
    }

    public static String putThrowException(String urlString, String authHeader, String body) throws InvalidHttpStatusCodeException {
        return responseThrowException(put(urlString, authHeader, body));
    }

    public static String responseThrowException(HttpResponse response) throws InvalidHttpStatusCodeException {
        if(response.statusCode > 299) {
            throw new InvalidHttpStatusCodeException("");
        }
        return response.body;
    }


    public static HttpResponse get(String urlString, String authHeader) {
        return request(urlString, "GET", authHeader, null);
    }

    public static HttpResponse post(String urlString, String authHeader, String body) {
        return request(urlString, "POST", authHeader, body);
    }

    public static HttpResponse patch(String urlString, String authHeader, String body) {
        return request(urlString, "PATCH", authHeader, body);
    }

    public static HttpResponse put(String urlString, String authHeader, String body) {
        return request(urlString, "PUT", authHeader, body);
    }

    public static HttpResponse request(String urlString, String method, String authHeader, String body) {
        String output = "";
        int statusCode = 0;
        try {
            // Create http connection with Authorization header and correct Content-Type
            URL url = new URL(urlString);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", authHeader);

            // java doesn't allow some http verbs so you have to make the method POST and then create an override property
            if(method != "GET" && method != "POST" && method != "DELETE"  && method != "PUT"  ) {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", method);
            } else {
                conn.setRequestMethod(method);
            }

            // Do not write body to request if body is empty or null or method is GET
            if(body == "" || body == null || method == "GET") {
                if(HttpClient.DEBUG) {
                    System.out.println("Body will not be added to http request");
                }
            } else {
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes());
                os.flush();
            }

            statusCode = conn.getResponseCode();

            if (statusCode > 299) {
                // TODO, maybe throw an exception here with an status code larger than 299
                System.out.println("Failed : HTTP error code : " + statusCode);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String tmp;
            while ((tmp = br.readLine()) != null) {
                output = output + tmp;
            }

            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(HttpClient.DEBUG) {
            System.out.println("JavaREST.request() ========");
            System.out.println("Response:");
            System.out.println(output);
            System.out.println(statusCode);
            System.out.println("============================");
        }

        return new HttpResponse(output, statusCode);
    }



    // ===============================================================
    // String base64Encode() - base64 encodes argument and returns encoded string
    //
    public static String base64Encode(String input) {
        String encodedString = "";
        try {
            encodedString = Base64.getEncoder().encodeToString(input.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodedString;
    } // base64Encode

}