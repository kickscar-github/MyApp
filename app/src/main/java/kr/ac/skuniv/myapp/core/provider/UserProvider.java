package kr.ac.skuniv.myapp.core.provider;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.GsonBuilder;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import kr.ac.skuniv.myapp.core.domain.User;
import kr.ac.skuniv.myapp.core.exception.HttpResponseException;
import kr.ac.skuniv.myapp.core.exception.JSONResultException;
import kr.ac.skuniv.myapp.core.network.JSONResult;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by cs618 on 2017-07-18.
 */

public class UserProvider {
    private Context context;

    public UserProvider( Context context ) {
        this.context = context;
    }

    public User login( String email, String password )
        throws HttpResponseException, JSONResultException {

        String url =
                "http://117.17.143.141:8080/mysite/api/user?a=login";
        String query = "email=" + email + "&password=" + password;

        HttpRequest request = HttpRequest.post( url );

        request.accept( HttpRequest.CONTENT_TYPE_JSON );
        request.connectTimeout( 1000 );
        request.readTimeout( 3000 );
        request.send( query );

        int responseCode = request.code();
        if ( responseCode != HttpURLConnection.HTTP_OK  ) {
            throw new HttpResponseException( "Response Code:" + responseCode  );
        }

        JSONResultLogin result = new GsonBuilder().
                create().
                fromJson(
                        request.bufferedReader(),
                        JSONResultLogin.class );
        if( "fail".equals( result.getResult() ) == true ) {
            throw new JSONResultException( result.getMessage() );
        }

        // 로긴 성공( JSESSIONID Cookie 저장)
        String jsessionCookie = null;
        Map<String, List<String>> map = request.getConnection().getHeaderFields();        if( map != null ) {
            List<String> cookies = map.get( "Set-Cookie" );
            if( cookies != null ) {
                for( String cookieString : cookies ) {
                    if( cookieString.startsWith( "JSESSIONID=" )){
                        jsessionCookie =
                                cookieString.substring( 0, cookieString.indexOf( ";" ) );
                    }
                }
            }
        }

        System.out.println( "----------->" + jsessionCookie );

        if( jsessionCookie != null ) {
            SharedPreferences sharedPreference = context.getSharedPreferences( "myapp-pref", MODE_PRIVATE );
            SharedPreferences.Editor sharedPreferenceEditor = sharedPreference.edit();
            sharedPreferenceEditor.putString( "session", jsessionCookie );
            sharedPreferenceEditor.commit();
        }

        return result.getData();
    }

    public List<User> fetchUserList() throws HttpResponseException, JSONResultException {
        String url =
                "http://117.17.143.141:8080/mysite/api/user?a=list";
        HttpRequest request = HttpRequest.get( url );

        // session 얻어오기
        SharedPreferences preferences = context.getSharedPreferences( "myapp-pref", Context.MODE_PRIVATE );
        String sessionCookies = preferences.getString( "session", null );
        String cookieString = "";
        if( sessionCookies != null ) {
            cookieString = cookieString + ";" + sessionCookies;
        }

        // cookie 세팅
        request.header( "Cookie", cookieString );

        request.contentType( HttpRequest.CONTENT_TYPE_JSON );
        request.accept( HttpRequest.CONTENT_TYPE_JSON );
        request.connectTimeout( 1000 );
        request.readTimeout( 3000 );

        int responseCode = request.code();
        if ( responseCode != HttpURLConnection.HTTP_OK  ) {
            throw new HttpResponseException( "Response Code:" + responseCode  );
        }

        JSONResultFetchUserList result = new GsonBuilder().
                create().
                fromJson(
                        request.bufferedReader(),
                        JSONResultFetchUserList.class );
        if( "fail".equals( result.getResult() ) == true ) {
            throw new JSONResultException( result.getMessage() );
        }

        return result.getData();
    }

    private class JSONResultLogin extends JSONResult<User> {}
    private class JSONResultFetchUserList extends kr.ac.skuniv.myapp.core.network.JSONResult<List<User>> {}
}
