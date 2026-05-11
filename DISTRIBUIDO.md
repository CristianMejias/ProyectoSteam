# Sistema Steam Distribuido

## Procesos y puertos

- `server.Proxy`: puerto `8080`
- `server.svSesiones 8081` y `server.svSesiones 8181`
- `server.svJuegos 8082` y `server.svJuegos 8282`
- `server.svMensajeria 8083` y `server.svMensajeria 8383`
- `cliente.ClienteDistribuido`: cliente de consola contra el proxy

## Persistencia HA

Los archivos JSON se crean en `data/`:

- Sesiones: `SES_Main.txt` / `SES_Copy.txt`
- Juegos: `GME_Main.txt` / `GME_Copy.txt`
- Mensajeria: `MSG_Main.txt` / `MSG_Copy.txt`

`persistencia.GestorPersistencia` protege lectura/escritura con `synchronized`. Toda escritura guarda primero Main y replica inmediatamente Copy.

## Seguridad y concurrencia

- El protocolo usa `protocolo.MensajeProtocolo`, serializado con Gson, con `requestId` unico por mensaje.
- Las contrasenas se guardan como SHA-256 simple en `seguridad.HashUtil`.
- `svJuegos` y `svMensajeria` validan token consultando `svSesiones` antes de ejecutar operaciones protegidas.
- Cada servidor acepta multiples clientes con `ExecutorService`.
- El stock, reservas, billeteras y buzones se modifican en regiones criticas `synchronized`.
- `svJuegos` levanta un daemon `GestorDeLocks-Juegos` que expira reservas despues de 5 minutos y restaura stock.

## Ejecucion sugerida

Compilar con Ant si esta disponible:

```powershell
ant clean jar
```

En esta maquina tambien compila directo con `javac`:

```powershell
New-Item -ItemType Directory -Force build\classes | Out-Null
$files = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp lib\gson-2.11.0.jar -d build\classes $files
```

Levantar procesos en terminales separadas si compilaste con Ant:

```powershell
java -cp "dist/ProyectoSteam.jar;lib/gson-2.11.0.jar" server.svSesiones 8081
java -cp "dist/ProyectoSteam.jar;lib/gson-2.11.0.jar" server.svSesiones 8181
java -cp "dist/ProyectoSteam.jar;lib/gson-2.11.0.jar" server.svJuegos 8082
java -cp "dist/ProyectoSteam.jar;lib/gson-2.11.0.jar" server.svJuegos 8282
java -cp "dist/ProyectoSteam.jar;lib/gson-2.11.0.jar" server.svMensajeria 8083
java -cp "dist/ProyectoSteam.jar;lib/gson-2.11.0.jar" server.svMensajeria 8383
java -cp "dist/ProyectoSteam.jar;lib/gson-2.11.0.jar" server.Proxy
java -cp "dist/ProyectoSteam.jar;lib/gson-2.11.0.jar" cliente.ClienteDistribuido
```

O usar `build\classes` si compilaste directo con `javac`:

```powershell
java -cp "build\classes;lib/gson-2.11.0.jar" server.svSesiones 8081
java -cp "build\classes;lib/gson-2.11.0.jar" server.svSesiones 8181
java -cp "build\classes;lib/gson-2.11.0.jar" server.svJuegos 8082
java -cp "build\classes;lib/gson-2.11.0.jar" server.svJuegos 8282
java -cp "build\classes;lib/gson-2.11.0.jar" server.svMensajeria 8083
java -cp "build\classes;lib/gson-2.11.0.jar" server.svMensajeria 8383
java -cp "build\classes;lib/gson-2.11.0.jar" server.Proxy
java -cp "build\classes;lib/gson-2.11.0.jar" cliente.ClienteDistribuido
```

Usuarios demo:

- `comprador` / `1234`
- `vendedor` / `1234`
- `admin` / `admin`
