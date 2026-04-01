package com.llr.rideapp.di

import com.llr.rideapp.data.remote.api.*
import com.llr.rideapp.data.local.TokenManager
import com.llr.rideapp.data.repository.*
import com.llr.rideapp.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(userApiService: UserApiService): UserRepository =
        UserRepositoryImpl(userApiService)

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        userApiService: UserApiService,
        tokenManager: TokenManager
    ): AuthRepository = AuthRepositoryImpl(authApiService, userApiService, tokenManager)

    @Provides
    @Singleton
    fun provideRideRepository(rideApiService: RideApiService): RideRepository =
        RideRepositoryImpl(rideApiService)

    @Provides
    @Singleton
    fun provideVehicleRepository(vehicleApiService: VehicleApiService): VehicleRepository =
        VehicleRepositoryImpl(vehicleApiService)

    @Provides
    @Singleton
    fun provideCallRepository(callApiService: CallApiService): CallRepository =
        CallRepositoryImpl(callApiService)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationApiService: NotificationApiService
    ): NotificationRepository = NotificationRepositoryImpl(notificationApiService)
}
