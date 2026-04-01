package com.llr.rideapp.presentation.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CLIENT_DASHBOARD = "client_dashboard"
    const val CLIENT_NEW_RIDE = "client_new_ride"
    const val CLIENT_RIDE_HISTORY = "client_ride_history"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_RIDES = "admin_rides"
    const val ADMIN_VEHICLES = "admin_vehicles"
    const val ADMIN_ADD_VEHICLE = "admin_add_vehicle"
    const val SUPER_ADMIN_DASHBOARD = "super_admin_dashboard"
    const val SUPER_ADMIN_USERS = "super_admin_users"
    const val NOTIFICATIONS = "notifications"
    const val CALL = "call/{callId}/{callType}/{isIncoming}/{remoteUserId}"

    fun callRoute(callId: String, callType: String, isIncoming: Boolean, remoteUserId: String) =
        "call/$callId/$callType/$isIncoming/$remoteUserId"
}
