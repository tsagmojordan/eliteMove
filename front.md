# 🛠️ Plan de correctifs — FRONTEND (eliteMove, Android Kotlin/Compose)

> ## ✅ STATUT : IMPLÉMENTÉ — build OK (`./gradlew assembleDebug` SUCCÈS)
>
> Implémenté et compilé le 2026-09-19 :
> - **F1.1** ✅ Refresh token via header (`TokenAuthenticator`)
> - **F1.2** ✅ Inscription → `POST /api/v1/auth/register` (nécessite backend **B1**)
> - **F1.3** ✅ Appels : `InitiateCallResponse` brut + `CallDto` aligné sur `CallResponse`
> - **F1.4** ✅ Notifications : `PaginatedResponse.content`, `subject`, compteur `Long`, `markAsRead` sans body
> - **F1.5** ✅ Utilisateurs : pagination `data.content`
> - **F1.6** ✅ Véhicules : enums backend (ECO/CONFORT/PREMIUM/VAN, AVAILABLE/IN_RIDE/MAINTENANCE/OUT_OF_SERVICE), champs photo1..3 + thumbnail, endpoints `/with-thumbnails`
> - **F1.7** ✅ Flotte admin : liste brute tous véhicules, badge basé sur `status`
> - **F1.8** ✅ Création multipart (`request` + `photos`), sélecteur de classe, prix, picker photos (max 3)
> - **F1.9** ✅ `PENDING`→`REQUESTED` + affichage du prix
> - **F2.1** ✅ Thumbnails Base64 dans les cartes
> - **F2.2** ✅ Photos pleine résolution dans un dialogue détail (Coil + header Authorization)
> - **F3** ✅ Client STOMP maison (`StompClient.kt`, OkHttp, aucune dépendance) + `CallRealtimeManager` ; abonnement `/user/queue/calls` + `/user/queue/notifications` ; bouton **Refuser** ; answer/ICE reçus branchés sur WebRtcManager ; fermeture auto sur DECLINED/ENDED (nécessite backend **B2** pour être actif)
> - **F4.1** ✅ Flotte admin : suppression + bascule de statut AVAILABLE↔MAINTENANCE
> - **F4.2** ✅ SuperAdmin : écran Rôles (lister/créer/supprimer), assignation de rôles par utilisateur, suppression d'utilisateur
> - **F4.3** ⚪ Non implémenté (backlog) : écran « mot de passe oublié », badge appels manqués, suppression d'un rôle individuel d'un utilisateur
> - **F5** ✅ Appels support fonctionnels (2026-10-04, backend **B7**) : bouton « Support » client → résout l'admin via `GET /api/v1/calls/support/admin-id` (fini l'UUID codé en dur) + dialogue d'erreur si 503 ; bouton « Appeler » dans la liste des trajets admin (appel sortant vers le `userId` du trajet). Écran d'appel : nom générique du correspondant (« Support » pour un client, « Client » pour un admin) — l'UUID brut n'est plus affiché. Appels audio uniquement — le code VIDEO restant est mort et assumé comme tel.
> - **F6** ✅ Téléphone à l'inscription (2026-10-04, backend **B8**) : champ « Téléphone » obligatoire dans le formulaire (clavier téléphonique, icône Phone, validation locale `^\+?[0-9]{8,15}$` identique au backend) ; `RegisterRequest` gagne `phone` → `POST /api/v1/auth/register` (contrat **C11**).
>
> Fichiers créés : `utils/ApiConfig.kt`, `data/remote/websocket/StompClient.kt`, `data/remote/websocket/CallRealtimeManager.kt`, `presentation/superadmin/SuperAdminRolesScreen.kt`.

> **Contexte** : audit croisé frontend ↔ backend. Les contrats backend sont **gelés** (voir table C1–C9 ci-dessous, identique à `../eliteMoveBackOffice/back.md`). Le backend ne subira que 3 modifications (B1 inscription publique, B2 push WebSocket appels, C8 prix ride) — tout le reste se corrige ici.
>
> **Adresse serveur confirmée** : `http://147.79.118.51:7820` (`NetworkModule.kt` et `network_security_config.xml` sont corrects — ne pas modifier).

---

## 📋 Table des contrats partagés (référence commune avec back.md)

| # | Sujet | Contrat retenu | Qui corrige |
|---|---|---|---|
| C1 | Refresh token | `POST /api/v1/auth/refresh` — refresh token dans le header `Authorization: Bearer <refresh>`, **pas de body** | Frontend |
| C2 | Inscription | Nouvel endpoint `POST /api/v1/auth/register` (public), body `{firstname,lastname,username,email,password}`, resp `ApiResponse<UserResponse>` 201 | Backend B1 + Frontend F1.2 |
| C3 | Initier un appel | `POST /api/v1/calls` → `InitiateCallResponse {callId, message}` **brut** (sans enveloppe) | Frontend |
| C4 | Notifications | `ApiResponse<PaginatedResponse<InAppNotificationResponse>>` (données dans `data.content`, champ **`subject`**) ; `unread/count` → `ApiResponse<Long>` ; `read`/`read-all` → `ApiResponse<Void>` | Frontend |
| C5 | Liste utilisateurs | `ApiResponse<PaginatedResponse<UserResponse>>` (`data.content`) | Frontend |
| C6 | Véhicules | Listes **brutes** ; création **multipart** parts `request` (JSON) + `photos` (fichiers) ; enums backend `VehiculeClass {ECO, CONFORT, PREMIUM, VAN}`, `VehiculeStatus {AVAILABLE, IN_RIDE, MAINTENANCE, OUT_OF_SERVICE}` | Frontend |
| C7 | Appels temps réel | WS `/ws-notifications` (STOMP, JWT au CONNECT) → destination `/user/queue/calls` avec `{type: "INCOMING_CALL"\|"SIGNAL"\|"CALL_STATUS", ...}` | Backend B2 + Frontend F3 |
| C8 | Prix ride | `RideDto` gagne `price` (Double, non-nul après ACCEPTED) | Backend B3 + Frontend F1.9 |
| C9 | Adresse | `http://147.79.118.51:7820/` confirmée — ne pas modifier | — |
| C10 | Admin de support à appeler | `GET /api/v1/calls/support/admin-id` → `ApiResponse<String>` (`data` = UUID de l'admin, tirage aléatoire côté backend, admins en appel exclus) ; aucun admin → 503 `ApiResponse<Void>` avec message | Backend B7 + Frontend F5 |
| C11 | Téléphone à l'inscription | `RegisterRequest` gagne `phone` (optionnel à l'API — `^\+?[0-9]{8,15}$`, max 20 ; **requis** par le formulaire mobile) ; `UserResponse` gagne `phone` (nullable pour les anciens comptes) | Backend B8 + Frontend F6 |

---

# Phase 1 — Contrats HTTP (bloquant, sans dépendance backend sauf mention)

### F1.1 — Refresh token via header (C1)
**Fichiers** : `data/remote/interceptor/AuthInterceptor.kt` (`TokenAuthenticator.authenticate`, l.~55-122), `data/remote/api/ApiServices.kt` (`refreshToken`)
- Retirer le body `{"refreshToken": ...}` de la requête de refresh.
- Envoyer le refresh token dans le header : `Authorization: Bearer <refreshToken>`.
- Le parsing de la réponse (`ApiResponse<AuthResponse>`) reste identique.
- **AC** : token expiré → requête initiale rejouée avec le nouvel access token, sans déconnexion.

### F1.2 — Inscription sur `/api/v1/auth/register` (C2, dépend de B1)
**Fichiers** : `ApiServices.kt` (déplacer `register` de `UserApiService` vers `AuthApiService`), `AuthRepositoryImpl.register`, `RegisterScreen` (via `AuthRepository` inchangé)
- Nouveau path `POST api/v1/auth/register`, body inchangé (`RegisterRequest`), réponse `ApiResponse<UserResponse>`.
- **AC** : inscription sans token → 201 → retour à l'écran login.

### F1.3 — Appels : réponses brutes (C3)
**Fichiers** : `Dtos.kt`, `ApiServices.kt` (`CallApiService`), `data/repository/CallRepositoryImpl.kt`
- `initiateCall` → `Response<InitiateCallResponse>` avec `@SerializedName("callId") val callId: String`, `message: String?`. Le ViewModel utilise `call.id` → remplacer par `call.callId`.
- `getCallHistory` → `Response<List<CallDto>>` **brut** ; aligner `CallDto` sur `CallResponse` backend : `id, callerId, calleeId, callType, status, createdAt, answeredAt, endedAt, durationSeconds, endReason, isActive, isTerminated` (remplacer `startedAt`).
- **AC** : `POST /api/v1/calls` retourne un `callId` exploitable par le ViewModel.

### F1.4 — Notifications : pagination + `subject` (C4)
**Fichiers** : `Dtos.kt`, `ApiServices.kt` (`NotificationApiService`), `NotificationRepositoryImpl.kt`
- Ajouter `data class PaginatedResponse<T>(@SerializedName("content") val content: List<T>, ...)`.
- `getAllNotifications` → `Response<ApiResponse<PaginatedResponse<NotificationDto>>>`, lecture de `body()?.data?.content`.
- `NotificationDto` : remplacer `title` par `subject` ; garder `id, message, read, createdAt` (ajouter `priority`, `templateCode` en optionnels).
- `getUnreadCount` → `Response<ApiResponse<Long>>` (supprimer `UnreadCountDto`, lire `data` comme `Long`).
- `markAsRead` → `Response<ApiResponse<Void>>` : succès = `response.isSuccessful`, **ne pas** faire `data.toModel()` (backend renvoie `Void`). Mettre à jour la liste localement (déjà fait dans le ViewModel).
- `markAllAsRead` : inchangé.
- **AC** : l'écran notifications liste les vraies notifications, badge unread correct, « marquer comme lu » sans erreur.

### F1.5 — Utilisateurs : pagination (C5)
**Fichiers** : `Dtos.kt`, `ApiServices.kt` (`UserApiService.getAllUsers`), `UserRepositoryImpl.getAllUsers`
- `getAllUsers` → `Response<ApiResponse<PaginatedResponse<UserDto>>>`, lecture de `data.content`. (Passer `page=0&size=50` ou pagination déroulante.)
- **AC** : `SuperAdminUsersScreen` affiche les utilisateurs.

### F1.6 — Véhicules : DTOs, enums et photos (C6)
**Fichiers** : `domain/model/VehiculeModels.kt`, `data/remote/api/VehiculeApiService.kt`, `ClientDashboardScreen.kt` (ViewModel + `VehiculeCard`)
- Aligner les enums sur le backend : `VehiculeClass { ECO, CONFORT, PREMIUM, VAN }` (supprimer STANDARD/LUXURY) ; `VehiculeStatus { AVAILABLE, IN_RIDE, MAINTENANCE, OUT_OF_SERVICE }` (supprimer BUSY/OFFLINE).
- `VehiculeDto` : remplacer `imagePath` par `photo1/photo2/photo3: String?` + `photo1MimeType: String?` ; conserver `longitude/latitude` (le backend les renvoie) ; `price` en `Double?`.
- `VehiculeApiService` : ajouter
  - `GET api/v1/vehicules/with-thumbnails` → `List<VehiculeWithThumbnailDto>` (`thumbnail: String?` = Base64 JPEG)
  - `GET api/v1/vehicules/available/with-thumbnails` → `List<VehiculeWithThumbnailDto>`
- Le ViewModel du dashboard client consomme `with-thumbnails` ; les cartes affichent le thumbnail Base64 (voir F2.1).
- Badge « DISPO » : ne plus le coder en dur — affiché seulement si `vehicule.status == AVAILABLE` (sinon badge gris « INDISPO »).
- **AC** : les véhicules s'affichent avec leurs classes correctes, badge conforme au statut, aucune image manquante.

### F1.7 — Admin véhicules : liste brute + `status` (C6)
**Fichiers** : `ApiServices.kt` (`VehicleApiService`), `VehicleRepositoryImpl.kt`, `AdminVehiclesScreen.kt`
- `getAvailableVehicles` → `Response<List<VehicleDto>>` **brut** (`response.body() ?: emptyList()`).
- `VehicleDto` : remplacer `available: Boolean?` par `status: String?` ; badge « DISPONIBLE » ⇔ `status == "AVAILABLE"`.
- **AC** : la flotte s'affiche (plus de liste vide silencieuse).

### F1.8 — Création véhicule : multipart + sélecteur de classe (C6)
**Fichiers** : `ApiServices.kt`, `VehicleRepositoryImpl` (+interface `VehicleRepository`), `AdminAddVehicleScreen.kt`, `build.gradle.kts`/`libs.versions.toml`
- `createVehicle` → `@Multipart` : part `request` (`@Part("request") request: RequestBody`, JSON de `CreateVehicleRequest` **avec `price`**) + part `photos` (`@Part photos: List<MultipartBody.Part>?`).
- UI : ajouter un **sélecteur de classe** (ECO / CONFORT / PREMIUM / VAN) — supprimer le défaut codé en dur `"ECONOMY"` (valeur invalide côté backend) — et un champ **prix**.
- Ajouter un **sélecteur de photos** (ActivityResultContracts.PickMultipleVisualMedia, max 3) pour la part `photos`.
- **AC** : création d'un véhicule avec 1–3 photos → 201 → visible dans la flotte.

### F1.9 — Rides : prix + statut « PENDING » (C8)
**Fichiers** : `AdminRidesScreen.kt`, `ClientRideHistoryScreen.kt`, `Dtos.kt` (`RideDto`)
- `RideDto` : le backend ajoute `price` (B3) — l'exposer dans l'UI (historique client, détail ride).
- `AdminRidesScreen.kt:118` : remplacer `"PENDING"` par `"REQUESTED"`.
- **AC** : le prix s'affiche après acceptation ; les courses REQUESTED acceptables.

---

# Phase 2 — Affichage des photos (après F1.6)

### F2.1 — Thumbnails Base64 dans les listes
- `VehiculeCard` : si `thumbnail` non vide → `AsyncImage` avec `data:image/jpeg;base64,<thumbnail>` (Coil supporte les data URIs) ; sinon icône placeholder. Supprimer l'usage de `imagePath`.
### F2.2 — Photos pleine résolution
- Pour la fiche détail du véhicule : `GET api/v1/vehicules/{id}/photo1|photo2|photo3` via Coil avec header `Authorization: Bearer <token>` (ImageRequest `.header("Authorization", ...)`) ou passer par un interceptor Coil dédié. Les images sont binaires avec le bon Content-Type.
- **AC** : les 3 photos d'un véhicule s'affichent dans le détail.

---

# Phase 3 — Temps réel : appels & notifications (C7, dépend de B2)

### F3.1 — Client STOMP
- Ajouter un client STOMP sur `/ws-notifications` (ex. `ua.naiksoftware.stomp:stomp-protocol-android` via JitPack, ou client OkHttp-WebSocket avec frames STOMP manuelles) — JWT dans le header `Authorization` au CONNECT, reconnexion automatique.
- Abonnements :
  - `/user/queue/calls` → événements `INCOMING_CALL`, `SIGNAL`, `CALL_STATUS`
  - topic notifications (déjà exposé par le backend) → rafraîchir badge unread
### F3.2 — Réception d'appel entrant
- `INCOMING_CALL` → écran sonnerie (réutiliser `CallScreen` en mode `isIncoming=true`) avec **bouton Refuser** (`callRepository.declineCall(callId)` — endpoint déjà câblé, jamais appelé à ce jour).
### F3.3 — Boucle WebRTC complète
- `SIGNAL` (offer du caller) → `webRtcManager.handleOffer(callId, signal)` puis envoyer l'answer via `sendSignalToBackend` (déjà implémenté).
- `SIGNAL` (answer du callee côté caller) → `webRtcManager.handleAnswer(...)` ; `SIGNAL` (ICE) → `webRtcManager.handleIceCandidate(...)` — ces deux méthodes existent déjà, il ne manque que le branchement.
- `CALL_STATUS` (DECLINED/ENDED/MISSED) → fermeture de l'écran, nettoyage WebRTC.
- **AC** : appel audio établi entre deux terminaux, refus fonctionnel, raccrochage propagé aux deux parties.

---

# Phase 4 — Fonctionnalités admin manquantes (endpoints backend déjà prêts)

### F4.1 — CRUD véhicules admin
- `GET /api/v1/vehicules/{id}`, `PUT /api/v1/vehicules/{id}` (JSON), `DELETE /api/v1/vehicules/{id}`, `PATCH /api/v1/vehicules/{id}/status?status=` : à câbler dans `VehiculeApiService` + UI `AdminVehiclesScreen` (menu par véhicule : éditer / supprimer / statut AVAILABLE↔MAINTENANCE).
### F4.2 — Rôles & permissions (SuperAdmin)
- Nouveau `RoleApiService` : `GET /api/v1/roles` (paginé, `PaginatedResponse`), `POST/PUT/DELETE /api/v1/roles/{id}`.
- UI : écran « Gestion des rôles » + depuis `SuperAdminUsersScreen`, assignation des rôles via `POST /api/v1/users/{id}/roles` (`roleIds`) et suppression d'utilisateur `DELETE /api/v1/users/{id}` (avec confirmation). Renseigner le `RoleDto` id/name depuis `GET /api/v1/roles`.
### F4.3 — Divers
- « Mot de passe oublié » : `POST /api/v1/auth/reset-password` + `confirm-reset-password` (écran à créer, basse priorité).
- `GET /api/v1/calls/missed/count` : badge d'appels manqués sur les dashboards.
- `AuthRepositoryImpl.getUserById` : à utiliser ou à supprimer (code mort).

---

# Phase 5 — Validation

```bash
./gradlew assembleDebug
```

**Checklist** :
- [ ] F1.1 refresh token via header → session renouvelée sans déconnexion
- [ ] F1.2 inscription via `/auth/register` (après B1)
- [ ] F1.3 initiation d'appel → `callId` exploitable
- [ ] F1.4 notifications listées + badge + markAsRead OK
- [ ] F1.5 liste utilisateurs SuperAdmin OK
- [ ] F1.6 dashboard client : classes/status corrects, badges conformes
- [ ] F1.7 flotte admin non vide
- [ ] F1.8 création véhicule multipart avec photos OK
- [ ] F1.9 prix ride affiché, plus de comparaison à "PENDING"
- [ ] F2 photos visibles (cards + détail)
- [ ] F3 appel audio de bout en bout entre 2 terminaux (après B2)
- [ ] F4 selon priorité métier
- [ ] F5 appels support de bout en bout (après B7) : résolution admin C10 + nom générique
- [ ] F6 inscription avec téléphone (après B8) : champ requis, format `^\+?[0-9]{8,15}$`

---

## 📌 Ordre recommandé
1. **F1.1–F1.7, F1.9** (aucune dépendance backend) — corrige 100 % des contrats cassés
2. **F2** (dépend F1.6)
3. **B1** (backend) puis **F1.2**
4. **B2** (backend) puis **F3**
5. **B3** (backend) puis **F1.9 prix** (affichage)
6. **F4** (backlog, endpoints déjà disponibles)