# StockScan — gestion de stock avec scan par téléphone

Application Android native : Kotlin, Jetpack Compose (Material 3), Room (SQLite),
CameraX et ML Kit Barcode Scanning (fonctionne hors ligne).

## Fonctionnalités
- Scan de codes-barres (EAN, UPC, Code 128, …) et QR codes, avec lampe torche
- Produit connu : entrée/sortie de stock en 2 touches ; produit inconnu : création de la fiche
- Fiche produit : nom, code-barres, quantité, seuil d'alerte, prix, emplacement
- Liste avec recherche, filtre « stock bas », totaux (références, unités, valeur)
- Historique de tous les mouvements (stock jamais négatif)
- Export CSV (Excel FR) partageable (mail, Drive, WhatsApp…)
- Données stockées localement sur le téléphone

## Compiler l'APK
1. Installer Android Studio (Koala 2024.1 ou plus récent, JDK 17 inclus).
2. File > Open > dossier `StockScan`, attendre la synchronisation Gradle
   (Android Studio télécharge Gradle 8.7 et les dépendances).
3. Brancher un téléphone (débogage USB activé) puis ▶ Run, ou :
   Build > Build Bundle(s) / APK(s) > Build APK(s).
   L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.
4. Pour un APK de production : Build > Generate Signed Bundle / APK.

En ligne de commande (après avoir généré le wrapper avec `gradle wrapper`) :
`./gradlew assembleDebug`
