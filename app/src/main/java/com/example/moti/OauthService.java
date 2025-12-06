package com.example.moti;

import android.util.Log;
import com.google.gson.Gson;
import okhttp3.*;

public class OauthService {

    private static final String BASE_URL = "https://b9ec58cdb006.ngrok-free.app/moti/api/user_auth/";
    private final OkHttpClient client = ApiClient.getClient();
    private final Gson gson = new Gson();


    public void verifyEmail(String email, String otp, Callback callback) {
        String url = BASE_URL + "verify_email/";

        VerifyRequest req = new VerifyRequest(email, otp);
        String json = gson.toJson(req);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }


    public void registerUser(String email, Callback callback) {
        String url = BASE_URL + "register/";
        RegisterRequest req = new RegisterRequest(email);
        String json = gson.toJson(req);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void loginUser(String email, String password, Callback callback) {
        String url = BASE_URL + "login/";
        LoginRequest req = new LoginRequest(email, password);
        String json = gson.toJson(req);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getUser(String token, Callback callback) {
        String url = BASE_URL + "get_user/";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getUsers(String token, Callback callback) {
        String url = BASE_URL + "get_users/";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteUser(String token, String otp, Callback callback) {
        String url = BASE_URL + "delete_user/";
        DeleteRequest req = new DeleteRequest(otp);
        String json = gson.toJson(req);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);
    }



    private static class RegisterRequest {
        String email;
        RegisterRequest(String email) {
            this.email = email;
        }
    }

    private static class VerifyRequest {
        String email;
        String otp_code;
        VerifyRequest(String email, String otp) {
            this.email = email;
            this.otp_code = otp;
        }
    }

    private static class LoginRequest {
        String email;
        String password;
        LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    private static class DeleteRequest {
        String otp;
        DeleteRequest(String otp) {
            this.otp = otp;
        }
    }
}