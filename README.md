# Onyx Dungeons

Plugin para **Paper 1.21+** (Java 21) que convierte una zona de tu mundo en un sistema de **dungeons por niveles**:
plataforma de espera con cuenta atras, teletransporte, jefes, llaves, puertas de bloques, cofres con loot aleatorio
sin repetidos, hologramas, mensajes en pantalla y retorno automatico al lobby. Todo se configura **desde menus** dentro del juego.

> Idioma: espanol (los mensajes son 100 % editables).

## Caracteristicas

| | |
|---|---|
| **Plataforma de espera** | Seleccionas la zona con una varita. Cuenta cuantos jugadores hay dentro; al llegar al minimo empieza una **bossbar** con el tiempo restante y luego los teletransporta a la dungeon. Tiempo, minimo/maximo de jugadores y punto de teletransporte son editables. |
| **Hologramas** | Holograma propio (armor stands) o integracion con **DecentHolograms** y **HolographicDisplays**. Placeholders: `{name} {state} {players} {max} {min} {time} {level}`. |
| **Mapa de la dungeon** | Varita para marcar la region. Nadie entra por `/tp`, `/tpa`, `/back`... y los bloques quedan protegidos. |
| **Reglas dentro** | Sin `/tp`, `/tpa`, `/back`, `/home`, `/warp`, `/spawn`... (lista configurable), sin teletransportes por comando/plugin y **sin fly**. El modo de juego se fuerza a supervivencia. |
| **Muerte** | Al morir quedas **expulsado**: reapareces en el lobby y ves *"Moriste en la dungeon, quedas expulsado"*. Si mueren todos, la dungeon se reinicia. |
| **`/dungeon spawn`** | Lleva al lobby (dentro de una dungeon equivale a abandonarla). Se fija con `/dungeon setspawn`. |
| **Cofres de recompensa** | Tablas de loot con **probabilidad por item**, cantidad min/max y **sin repetir items**. Cofres/barriles que registras con una varita; se llenan al abrirse (o loot personal por jugador). |
| **Niveles y jefes** | Cada nivel tiene sus jefes (los crea el plugin, o los invocas tu con un *tag*). Al derrotar a todos se entrega la **llave del nivel**. |
| **Llaves personalizables** | Tu eliges el item (nombre, lore, material...). Se marca internamente para que nadie pueda falsificarlo. |
| **Puertas** | Seleccionas de punto a punto los bloques; se abren al hacer **clic derecho con la llave**. Se restauran solas al terminar. |
| **Mensajes** | Titulo, subtitulo, chat, actionbar y sonido para cada evento (*bajaste al nivel 2*, *abriste la puerta*, *derrotaron al jefe*...). Solo los ven los jugadores dentro. Editables en `messages.yml` o por menu, globales o **por dungeon**. |
| **Final** | Marcas cual es el ultimo nivel: al caer su jefe se entregan las recompensas finales (tabla + comandos) y todos vuelven a `/dungeon spawn`. |
| **API y eventos** | `OnyxDungeonsAPI` y 7 eventos de Bukkit para integrar otros plugins. |

## Compilar

```bash
mvn clean package
```

El jar queda en `target/OnyxDungeons-1.0.0.jar`. Copialo a `plugins/`.
En GitHub, el workflow `.github/workflows/build.yml` compila y sube el jar como artefacto en cada push.

Si tu servidor usa otra version de Paper, cambia `paper.version` en el `pom.xml`.

## Guia rapida: montar tu primera dungeon

1. `/dungeon` -> **Crear dungeon** y escribe un id (ej. `cripta`).
2. **Zona de espera**: recibes la *Varita de Zona*. Clic izquierdo = punto 1, clic derecho = punto 2, **agachado + clic derecho = guardar**.
3. **Teletransporte de entrada**: ponte donde quieres que aparezcan y pulsa el boton.
4. **Mapa**: marca todo el area de la dungeon con la *Varita de Mapa* (activa protecciones y bloqueo de entradas).
5. **Holograma** (opcional): ponte en la plataforma y pulsa el boton. Shift + clic izquierdo edita las lineas.
6. **Llaves**: en el menu principal -> *Llaves* -> *Crear llave desde mi mano* (sostén el item que quieras).
7. **Tablas de loot**: menu principal -> *Tablas de recompensas* -> crear tabla -> suelta items con el cursor (o usa *Anadir item de mi mano*) y ajusta la probabilidad de cada uno con clic.
8. **Niveles**: *Niveles, jefes y puertas* -> Nivel 1:
   - **Jefes** -> *Anadir jefe aqui* (tipo, nombre, vida, dano, posicion).
   - **Llave de este nivel**: la que se entrega al vencer a los jefes.
   - **Puertas** -> *Nueva puerta*: selecciona los bloques con la *Varita de Puerta*.
   - **Region del nivel** (opcional): con la *Varita de Nivel*; al entrar se muestra *"bajaste al nivel N"*.
   - Anade mas niveles y marca el **ultimo nivel**.
9. **Cofres**: *Cofres de recompensa* -> elige tabla -> *Varita de cofres* -> clic derecho en cada cofre/barril.
10. **Recompensa final**: elige una tabla y/o comandos (`%player%`).
11. Pon el spawn del lobby con `/dungeon setspawn` y prueba con **Iniciar ahora**.

## Comandos

| Comando | Descripcion | Permiso |
|---|---|---|
| `/dungeon` | Menu de administracion | `onyx.dungeons.admin` |
| `/dungeon spawn` | Ir al lobby / abandonar la dungeon | `onyx.dungeons.spawn` |
| `/dungeon leave` | Abandonar la dungeon | - |
| `/dungeon setspawn` | Fijar el spawn del lobby | admin |
| `/dungeon create\|delete\|list\|info <id>` | Gestionar dungeons | admin |
| `/dungeon start\|stop <id>` | Iniciar (con quien este en la plataforma) o detener | admin |
| `/dungeon wand <zone\|map\|level\|door\|chest> <dungeon> [nivel]` | Obtener varitas | admin |
| `/dungeon key <create\|delete\|list> <id>` | Llaves (create usa el item de tu mano) | admin |
| `/dungeon givekey <jugador> <llave> [n]` | Entregar una llave | admin |
| `/dungeon loot <create\|delete\|edit\|list> <id>` | Tablas de loot | admin |
| `/dungeon reload` | Recargar todo | admin |

Permisos: `onyx.dungeons.admin` (op), `onyx.dungeons.play` (todos), `onyx.dungeons.spawn` (todos), `onyx.dungeons.bypass` (op: ignora restricciones).

## Varitas

| Varita | Item | Uso |
|---|---|---|
| Zona de espera | Vara de blaze | Plataforma donde se paran los jugadores |
| Mapa | Palo | Region completa de la dungeon |
| Nivel | Fragmento de amatista | Region de un nivel |
| Puerta | Hacha dorada | Bloques de una puerta |
| Cofres | Fragmento de eco | Clic derecho en cofre/barril: registrar/quitar |

Clic izquierdo = punto 1 · Clic derecho = punto 2 · **Agachado + clic derecho = guardar**.

## Jefes

* **Jefes del plugin**: eliges tipo de mob, nombre, vida y dano; aparecen cuando el grupo llega a ese nivel y muestran una barra de vida.
* **Jefes externos** (MythicMobs, `/summon`, otro plugin): en el menu de jefes elige *Anadir jefe externo* y escribe un **tag**.
  Dale ese tag a tu mob (`/tag @e[...] add mi_jefe`, o la opcion `Tags` de tu plugin de mobs). Al morir, el plugin lo detecta igual que a uno propio.
  Tambien funciona el tag `onyxboss_<dungeon>_<nivel>` sin crear nada en el menu.
* Un nivel se considera **despejado** cuando mueren todos sus jefes. Cada nivel necesita al menos uno, y el ultimo nivel es el que termina la dungeon.

## Configuracion

* `config.yml`: modo de juego, tiempo limite, retraso al terminar, quien recibe la llave (`ALL`/`KILLER`/`DROP`), loot personal,
  listas de comandos bloqueados, causas de teletransporte bloqueadas, proveedor de hologramas.
* `messages.yml`: todos los mensajes (titulo, subtitulo, chat, actionbar, sonido). Placeholders: `{prefix} {dungeon} {player} {level} {levelname} {boss} {key} {items} {players} {min} {max} {time} {state}`.
* `dungeons.yml`, `loot.yml`, `keys.yml`, `data.yml`: datos generados por el plugin.

## API para desarrolladores

```java
import com.onyx.dungeons.api.OnyxDungeonsAPI;

if (OnyxDungeonsAPI.isInDungeon(player)) { /* ... */ }
OnyxDungeonsAPI.showHologram("mi-holo", location, List.of("&6Hola", "&7Mundo"));
OnyxDungeonsAPI.removeHologram("mi-holo");
ItemStack key = OnyxDungeonsAPI.createKey("llave_nivel1", 1);
List<ItemStack> loot = OnyxDungeonsAPI.rollLoot("cofre_nivel1");
```

Eventos (`com.onyx.dungeons.api.events`): `DungeonStartEvent`, `DungeonEndEvent`, `DungeonPlayerJoinEvent`,
`DungeonPlayerLeaveEvent`, `DungeonLevelReachEvent`, `DungeonBossDefeatEvent`, `DungeonDoorOpenEvent` (cancelable).

## Estado y limites conocidos

* El codigo compila y la logica del sorteo de loot esta probada, pero **no se ha probado en un servidor real**: prueba en un entorno de pruebas antes de usarlo en produccion.
* Solo hay una partida a la vez por dungeon (el mapa es compartido). Para varias partidas simultaneas hace falta clonar el mapa (ver hoja de ruta).
* Las puertas se restauran desde una copia de bloques; si cambias la construccion usa *Shift + clic izquierdo* en la puerta para volver a capturarla.

## Hoja de ruta (ideas)

Ver la lista completa en [ROADMAP.md](ROADMAP.md).

## Licencia

MIT
