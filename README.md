# Sincronizado con los cambios de https://github.com/xtools-at/Android-PWA-Wrapper/ hasta el commit  f1bddca0f6c133a98db1c95b56f2b2d31f35709a (15 mayo 2019)

# Pasos a seguir para publicar una versión

1. `git pull origin master`
2. Abrir el proyecto con Android Studio
3. Asegurarse de que estamos compilando la app en modo `release` (Build -> Select build type -> Release)
4. Incrementar `Version Code`, `Version Name` y `Version Name Suffix` (File -> Project structure -> Flavors -> default)
5. Asegurarse de que `Min Sdk Version` es el adecuado (debería ser API 21, Android 5.0)
6. Asegurarse de que `Tarket Sdk Version` es el adecuado (debería ser API 26, Android 8.0)
7. Compilar (Build -> Generate Signed Bundle -> Android App Bundle)
8. Durante el proceso de build seleccionar el certificado `medika-android-cert` y usar las contraseñas correspondientes del `contraseña medika-android-cert.txt`
9. Subir a Google Play Console y sacar una release (`interna`, `alpha`, `beta` o `produccion`)
