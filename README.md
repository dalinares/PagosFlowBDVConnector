# PagosFlow BDV Connector - Fase 1.5 (Guía para Compilar APK y Pruebas Reales)

Esta versión (Fase 1.5) está diseñada específicamente para generar un APK instalable en un teléfono Android real con **BDV Móvil** (Banco de Venezuela) instalado, para auditar y validar la captura de notificaciones bancarias en caliente antes de habilitar el envío HTTP hacia Base44.

---

## 🎯 Objetivos de la Fase 1.5 en Teléfono Real

1. **Pantalla inicial de configuración con estado de permisos**: Muestra de forma clara si el sistema Android autorizó a PagosFlow para leer las notificaciones.
2. **Botón directo de ajustes**: Abre con un solo toque la sección exacta de Android (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`) para habilitar el servicio sin perderse en menús.
3. **Historial local de los últimos 20 pagos**: Conserva en memoria y almacenamiento local (`SharedPreferences`) los últimos 20 pagos capturados para inspección inmediata.
4. **Registro interno de eventos y errores**: Registra en tiempo real los eventos del servicio, notificaciones filtradas, descartes de seguridad y posibles fallos de sintaxis.
5. **Botón "Probar captura BDV"**: Permite inyectar una prueba sintética interna para verificar el motor de expresiones regulares sin necesidad de hacer una transferencia real de inmediato.
6. **Indicador de 4 estados claros**:
   - 🟢 **Servicio activo**: Permiso concedido y listener enlazado.
   - 🔵 **Esperando notificación**: Servicio en reposo esperando un pago móvil entrante.
   - ✅ **Pago capturado**: Notificación de BDV procesada y almacenada con éxito.
   - 🔴 **Error**: Falta de permisos o servicio desconectado por el sistema operativo.

---

## 🛠️ Cómo Generar el APK de Prueba (3 Opciones Disponibles)

### Opción 1: Directo en Android Studio (Recomendado)
1. Abre **Android Studio** (Hedgehog, Iguana o posterior).
2. Selecciona **Open** y navega a la carpeta descomprimida `android/`.
3. Espera a que Gradle sincronice las dependencias automáticamente.
4. En el menú superior, selecciona:
   `Build` > `Build Bundle(s) / APK(s)` > `Build APK(s)`.
5. En 1-2 minutos, tu APK estará generado en:
   `app/build/outputs/apk/debug/app-debug.apk`

---

### Opción 2: Compilación Rápida por Terminal (CLI)
Si tienes Java 17 instalado en tu computadora, puedes compilar sin abrir la interfaz de Android Studio:

- **En Windows (PowerShell o CMD)**:
  ```powershell
  cd android
  .\gradlew.bat assembleDebug
  ```

- **En macOS o Linux (Terminal)**:
  ```bash
  cd android
  chmod +x gradlew
  ./gradlew assembleDebug
  ```

El binario compilado quedará listo en:
`app/build/outputs/apk/debug/app-debug.apk`

---

### Opción 3: Compilación 100% en la Nube (GitHub Actions CI/CD)
El proyecto incluye el archivo `.github/workflows/build-apk.yml`. Si prefieres compilar sin instalar nada en tu computadora:
1. Crea un repositorio en GitHub (público o privado) y sube el contenido de esta carpeta `android`.
2. Dirígete a la pestaña **Actions** de tu repositorio.
3. La acción **"Compilar APK PagosFlow BDV Connector"** se ejecutará de forma automática en los servidores de GitHub (Ubuntu con Java 17 y Android SDK).
4. Al finalizar la compilación (unos 2 minutos), haz clic sobre la ejecución y descarga el archivo **`PagosFlow-BDV-Connector-Debug-APK.zip`** desde la sección **Artifacts**. Descomprímelo y tendrás el `app-debug.apk` listo.

---

## 📱 Pasos para Realizar Pruebas Reales con BDV Móvil

1. **Abrir PagosFlow en el teléfono**:
   - Verás el banner naranja advirtiendo que el **Acceso a notificaciones es REQUERIDO**.
2. **Conceder el Permiso Especial**:
   - Toca el botón **"Abrir Ajustes de Notificaciones Android"**.
   - Busca en la lista **PagosFlow BDV Connector** y activa el interruptor.
   - Android mostrará una advertencia de seguridad del sistema; confirma la autorización.
3. **Regresar a PagosFlow**:
   - El indicador cambiará de inmediato a 🟢 **Servicio activo** / 🔵 **Esperando notificación BDV**.
4. **Validación Sintética Previa**:
   - Toca el botón **"Probar captura BDV"**. Verás cómo el parser procesa la prueba y el pago se almacena en el historial.
5. **Prueba Real en Campo con Banco de Venezuela**:
   - Mantén la app de PagosFlow en segundo plano o minimizada.
   - Abre **BDV Móvil** o solicita a un tercero que emita un **PagomóvilBDV** de prueba (ejemplo: Bs. 1,00 o Bs. 0,10) a tu cuenta.
   - Al recibir la notificación del Banco de Venezuela, PagosFlow interceptará el evento, extraerá el monto, la referencia, el pagador y la fecha/hora, registrándolo en la pestaña **Historial (20)** y en el **Registro de Logs**.

---

## 🛡️ Garantía de Seguridad y Privacidad

- **Sin permisos invasivos**: La app no solicita SMS (`READ_SMS`), contactos, llamadas telefónicas ni almacenamiento externo.
- **Filtro estricto en memoria**: Las notificaciones de WhatsApp, Telegram, llamadas u otras aplicaciones son ignoradas inmediatamente en la primera línea de código del servicio.
- **Sin llamadas de red externas en esta fase**: En esta Fase 1.5 no se envía ningún dato fuera del dispositivo; todo queda estrictamente en la memoria local para tu verificación.
