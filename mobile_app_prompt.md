# Prompt IA : Développement de l'Application Mobile RideApp (Kotlin)

**Contexte de la demande :**
Tu es un Agent IA expert en développement d'applications mobiles Android natives avec **Kotlin**. Ton objectif est de développer "RideApp", une application de VTC/Réservation de trajets. Le backend en Spring Boot est déjà opérationnel.

Comme tu dois avoir toutes les clés en main, ce document détaille **exhaustivement** les contrats d'interface (Endpoints REST), les objets de requêtes attendus, et l'architecture technique exacte que tu dois mettre en œuvre pour éviter toute hallucination ou erreur d'implémentation.

🚨 **INFORMATION CRUCIALE : GESTION DES RÔLES (3 ACTEURS)** 🚨
L'application doit gérer 3 types d'utilisateurs avec des interfaces et des droits différents. Lors de la connexion, tu recevras les rôles de l'utilisateur. Tu dois implémenter un **système de navigation / Dashboard conditionnel** selon le rôle de la personne connectée :
1.  **Client :** Peut commander un trajet, voir son historique personnel, gérer ses infos, recevoir des notifications et participer à des appels WebRTC avec le support ou le chauffeur.
2.  **Admin :** Peut voir tous les trajets, les véhicules, assigner les trajets, effectuer du support via appel WebRTC, et gérer partiellement le système.
3.  **Super Admin :** A accès à toutes les fonctionnalités de l'Admin, avec en plus la capacité de gérer les utilisateurs (activer/désactiver des comptes, gérer les rôles).

---

## 1. Architecture et Stack Technique (Android)

*   **Langage :** Kotlin
*   **Architecture :** MVVM (Model-View-ViewModel) + Clean Architecture
*   **Asynchronisme :** Coroutines & Kotlin Flows
*   **Client HTTP :** Retrofit 2 + OkHttp3
*   **Parsing JSON :** Gson ou Kotlinx Serialization
*   **Interface Graphique :** Jetpack Compose (fortement recommandé) ou XML.
*   **Stockage du Token JWT :** `EncryptedSharedPreferences` (AndroidX Security) pour stocker de manière sécurisée l'Access Token et le Refresh Token.
*   **Injection de dépendances :** Dagger Hilt

**Configuration de Retrofit :**
Tu dois absolument configurer un `Interceptor` OkHttp qui vient lire le token JWT depuis les `SharedPreferences` et l'ajoute dans le header HTTP :
`Authorization: Bearer <access_token>`
*Note : Si la réponse HTTP est 401 (Unauthorized), tu dois appeler l'endpoint `/api/v1/auth/refresh` pour obtenir un nouveau token et relancer la requête (via un `Authenticator` OkHttp).*

*URL de Base (Émulateur Android) :* `http://10.0.2.2:8080` (Les endpoints mentionnent le prefixe `/api/v1/`)

---

## 2. Définition des Endpoints Backend et Implémentation Client

Le backend est structuré en plusieurs modules. Voici les contrats précis. **Important :** Presque tous les retours du backend englobent les données dans un objet standardisé (`ApiResponse`) sauf pour certains endpoints spécifiques. Prends-en compte dans tes modèles de données (Retrofit Responses).

### A. Authentification et Gestion de compte (`/api/v1/auth` et `/api/v1/users`)

**Inscription (Création de compte Client) :**
*   **Endpoint :** `POST /api/v1/users` (Pas de JWT requis)
*   **Body :**
    ```json
    {
      "firstname": "John",
      "lastname": "Doe",
      "username": "johndoe",
      "email": "john@example.com",
      "password": "Password123!"
    }
    ```

**Connexion (Login) :**
*   **Endpoint :** `POST /api/v1/auth/login`
*   **Body :**
    ```json
    {
      "usernameOrEmail": "john@example.com",
      "password": "Password123!"
    }
    ```
*   **Action :** Sauvegarder les tokens retournés de manière sécurisée.
*   **Attention :** Inspecte bien l'objet retourné par le login ou utilise l'appel `GET /api/v1/users/{id}` (avec ton token) pour lire le tableau `roles` et déterminer si l'utilisateur est Client, Admin, ou Super Admin afin de rediriger vers le bon écran (Dashboard).

**Déconnexion (Logout) :**
*   **Endpoint :** `POST /api/v1/auth/logout`
*   **Header :** JWT Actuel
*   **Action :** Vider les `SharedPreferences` de l'application et rediriger vers l'écran de Login.

**Gestion des Utilisateurs *(Réservé Super Admin)* :**
*   **Lister les utilisateurs :** `GET /api/v1/users?search={texte}`
*   **Activer/Désactiver :** `PATCH /api/v1/users/{id}/status?enabled=true|false`
*   **Assigner Rôles :** `POST /api/v1/users/{id}/roles` (Body: `{ "roleIds": ["UUID1", "UUID2"] }`)

### B. Gestion des Trajets / Rides (`/api/v1/rides`)

**Demander un nouveau trajet *(Client)* :**
*   **Endpoint :** `POST /api/v1/rides`
*   **Header :** JWT
*   **Body :**
    ```json
    {
      "userId": "UUID-de-l-utilisateur",
      "vehiculeId": null,
      "pickupLocation": "Adresse de départ",
      "dropoffLocation": "Adresse d'arrivée"
    }
    ```

**Récupérer les trajets :**
*   **Pour un client (Historique personnel) :** `GET /api/v1/rides/user/{userId}`
*   **Pour l'Admin / Super Admin (Tous les trajets) :** `GET /api/v1/rides`

**Mettre à jour le statut d'un trajet *(Admin / Chauffeur)* :**
*   **Endpoint :** `PATCH /api/v1/rides/{id}/status?status=ACCEPTED` (Statuts possibles: ACCEPTED, IN_PROGRESS, COMPLETED, etc.)

### C. Gestion des Véhicules (`/api/v1/vehicules`) *(Admin/Super Admin)*

**Lister les véhicules disponibles :**
*   **Endpoint :** `GET /api/v1/vehicules/available`

**Enregistrer un véhicule :**
*   **Endpoint :** `POST /api/v1/vehicules`
*   **Body :**
    ```json
    {
      "brand": "Toyota",
      "model": "Corolla",
      "year": 2022,
      "licensePlate": "AB-123-CD",
      "vehiculeClass": "ECONOMY" // (Enum: ECONOMY, PREMIUM, etc.)
    }
    ```

### D. Module d'Appels WebRTC - Audio/Vidéo (`/api/v1/calls`)

Le backend gère la création et la signalisation des appels intraspécifiques (entre utilisateurs, par exemple Client <-> Support Admin). 

**Exigences techniques Android pour les appels :**
Tu auras besoin de la librairie standard Android pour le WebRTC (`org.webrtc:google-webrtc`).

**Initiation de l'appel :**
*   **Endpoint :** `POST /api/v1/calls`
*   **Body :**
    ```json
    {
      "calleeId": "UUID-du-destinataire",
      "callType": "AUDIO" // ou "VIDEO"
    }
    ```
*   **Retourne :** L'`uuid` de l'appel (callId).

**Actions en cours d'appel :**
*   Accepter : `PATCH /api/v1/calls/{callId}/accept`
*   Décliner : `PATCH /api/v1/calls/{callId}/decline`
*   Raccrocher : `PATCH /api/v1/calls/{callId}/end` (Body optionnel: `{ "reason": "NORMAL" }`)

**Signalisation WebRTC (SDP / ICE Candidates) :**
*   **Endpoint :** `POST /api/v1/calls/signaling`
*   **Body :**
    ```json
    {
      "callId": "UUID-de-l-appel",
      "signal": "chaîne de caractères contenant le JSON SDP ou ICE candidate converti en string"
    }
    ```

**Historique des appels (tous rôles) :**
*   **Endpoint :** `GET /api/v1/calls/history?page=0&size=20`

### E. Notifications In-App (`/api/v1/notifications/in-app`)

Gérer le centre de notifications de l'utilisateur (valable pour les 3 acteurs).

*   **Récupérer toutes ses notifications :** `GET /api/v1/notifications/in-app`
*   **Compter le nombre de badges non lus :** `GET /api/v1/notifications/in-app/unread/count` (Retourne un objet avec une clé contenant le nombre).
*   **Marquer une notification comme lue :** `PATCH /api/v1/notifications/in-app/{notificationId}/read`
*   **Marquer tout comme lu :** `PATCH /api/v1/notifications/in-app/read-all`

---

## 3. Plan d'Action pour le Développement (Les étapes que TU dois suivre)

En tant qu'Agent IA, tu dois implémenter cette application en suivant cet ordre restrictif :

1.  **Mise en place du Projet & Gradle :** Configurer `build.gradle` (Retrofit, Hilt, WebRTC, Coroutines, ViewModel, Compose).
2.  **Configuration Réseau (Retrofit) :** 
    *   Créer le module Dagger Hilt pour unifier l'`ApiClient`.
    *   Créer l'`AuthInterceptor` pour inclure le JWT Token et gérer les `401 Unauthorized` de façon transparente.
3.  **Authentication Flow :**
    *   Créer l'interface de Login et Register (UI).
    *   Implémenter le Repository qui gère `EncryptedSharedPreferences` (Token).
    *   **Redirection par Rôle :** Le login doit rediriger vers `ClientDashboard`, `AdminDashboard` ou `SuperAdminDashboard` en fonction du rôle retourné par l'API.
4.  **Dashboards par Acteur :**
    *   **Client :** Écran pour commander un trajet (`POST /rides`), voir l'historique de ses trajets (`GET /rides/user/{id}`).
    *   **Admin :** Voir la liste de tous les trajets (`GET /rides`), véhicules (`GET /vehicules`), modifier les statuts.
    *   **Super Admin :** Panneau de gestion des utilisateurs (liste, activation/désactivation).
5.  **Système de Notifications :**
    *   Implémenter une icône de cloche en haut de l'écran avec un badge dynamique basé sur `/unread/count`.
    *   Ouvrir une liste de notifications.
6.  **Module d'Appel WebRTC :**
    *   Implémenter le PeerConnection WebRTC pour tout le monde (pour que n'importe qui puisse appeler et être appelé via son `userId`).
    *   Utiliser l'endpoint `/signaling` pour échanger l'offre (Offer), la réponse (Answer) et les ICE candidates.

---
**Rappel final à l'IA :** Les endpoints comme `POST /api/v1/rides` ou `GET /api/v1/users` renvoient ou attendent la casse exacte indiquée dans ce prompt (ex: `firstname`, `vehiculeId`, camelCase). Écris tes `data class` Kotlin en adéquation complète avec ceux-ci via les annotations `@SerializedName` !
