package com.llr.rideapp.di

import com.llr.rideapp.data.remote.api.*
import com.llr.rideapp.data.remote.interceptor.AuthInterceptor
import com.llr.rideapp.data.remote.interceptor.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    //private const val BASE_URL = "http://10.0.2.2:8080/"
    private const val BASE_URL = "http://10.254.107.116:8080/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService =
        retrofit.create(UserApiService::class.java)

    @Provides
    @Singleton
    fun provideRideApiService(retrofit: Retrofit): RideApiService =
        retrofit.create(RideApiService::class.java)

    @Provides
    @Singleton
    fun provideVehicleApiService(retrofit: Retrofit): VehicleApiService =
        retrofit.create(VehicleApiService::class.java)

    @Provides
    @Singleton
    fun provideCallApiService(retrofit: Retrofit): CallApiService =
        retrofit.create(CallApiService::class.java)

    @Provides
    @Singleton
    fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService =
        retrofit.create(NotificationApiService::class.java)

    @Provides
    @Singleton
    fun provideVehiculeApiService(retrofit: Retrofit): VehiculeApiService =
        retrofit.create(VehiculeApiService::class.java)
}
