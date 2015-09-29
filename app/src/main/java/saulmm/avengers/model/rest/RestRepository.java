/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package saulmm.avengers.model.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.net.SocketTimeoutException;
import java.util.List;

import javax.inject.Inject;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import rx.Observable;
import saulmm.avengers.model.Comic;
import saulmm.avengers.model.Repository;

public class RestRepository implements Repository {

    private final MarvelApi mMarvelApi;
    public final static int MAX_ATTEMPS = 3;

    String publicKey    = "74129ef99c9fd5f7692608f17abb88f9";
    String privateKey   = "281eb4f077e191f7863a11620fa1865f2940ebeb";

    @Inject
    public RestRepository() {

        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new CharacterItemAdapterFactory())
                .create();

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(chain -> {
            Request request = chain.request();
            String marvelHash = MarvelApiUtils.generateMarvelHash(publicKey, privateKey);
            request = request.newBuilder().url(
                    request.httpUrl().newBuilder()
                            .addQueryParameter(MarvelApi.PARAM_API_KEY, publicKey)
                            .addQueryParameter(MarvelApi.PARAM_TIMESTAMP, MarvelApiUtils.getUnixTimeStamp())
                            .addQueryParameter(MarvelApi.PARAM_HASH, marvelHash)
                            .build())
                    .build();
            return chain.proceed(request);
        });

        Retrofit marvelApiAdapter = new Retrofit.Builder()
                .baseUrl(MarvelApi.END_POINT)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        mMarvelApi = marvelApiAdapter.create(MarvelApi.class);
    }



    @Override
    public Observable<saulmm.avengers.model.Character> getCharacter(int characterId) {
        return mMarvelApi.getCharacter(characterId);
    }

    @Override
    public Observable<List<Comic>> getCharacterComics(int characterId) {

        final String comicsFormat   = "comic";
        final String comicsType     = "comic";

        return mMarvelApi.getCharacterComics(characterId, comicsFormat, comicsType)
                .retry((attemps, error) -> error instanceof SocketTimeoutException && attemps < MAX_ATTEMPS);
    }


}
