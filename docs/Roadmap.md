# Roadmap

Qué se hace, en qué orden y qué está hecho. El diseño (cómo se juega y por qué) está en `GDD.md`, y lo que viene de la novela, en `Canon.md`.

Marca con `[x]` lo que esté hecho. Dentro de cada fase, las tareas van en el orden recomendado.

Esfuerzo: **Bajo** (una tarde) · **Medio** (unos días) · **Alto** (una o dos semanas) · **Muy alto** (proyecto en sí mismo).

## Resumen

Objetivo: primero un servidor para amigos (5-10 jugadores) y después un MMO, con un mundo vivo, muchas clases y artesanía profunda. Cada fase termina en un **hito jugable**; si una fase se alarga, se puede parar ahí y el mod sigue siendo completo.

| Fase | Tema | Depende de | Hito |
|---|---|---|---|
| 0-2 | Historial | — | ✅ Plantilla con 4 sistemas y 5 sistemas nuevos |
| 3 | Fundamentos | — | Núcleo listo para escalar |
| 4 | Rebanada vertical | 3 | Un mini Satisfy jugable de principio a fin |
| 5 | Motor de clases | 3, 4 | Clases y habilidades con talentos, rangos y desbloqueos |
| 6 | Clases | 5 | 8-10 clases jugables |
| 7 | Artesanía profunda | 3, 5 | Diseñar un arma propia y engarzarle gemas |
| 8 | Servidor con amigos | 3 | 5-10 jugadores con economía y riesgo |
| 9 | Social | 8 | Grupos, gremios y mercado |
| 10 | Mundo y mazmorras | 4, 8 | Mazmorras con jefe por regiones |
| 11 | Mundo vivo | 10 | NPCs y reinos que reaccionan |
| 12 | Escala MMO *(aspiracional)* | 9 | Servidor abierto a muchos jugadores |
| 13 | Dimensión Satisfy *(aspiracional)* | 10, 11 | Contenido de final de juego |

Además, hay cuatro **pistas continuas** que avanzan en paralelo a las fases (ver §Pistas continuas).

---

## Historial (fases 0-2, hechas)

<details>
<summary>Ver lo ya hecho</summary>

### Inicio

- [x] **Fases 0-2.** Proyecto NeoForge 1.21.1 creado con el plugin Minecraft Development de IntelliJ y primer objeto registrado (`espada_jaffa`). Después, el proyecto se reestructuró como plantilla con 4 sistemas: equipo, estadísticas, habilidad activa y forja.

### Mejoras de la plantilla

**Base sólida**

- [x] **Probar en el juego y corregir** (Bajo).
- [x] **Requisitos coherentes** (Bajo). Si no cumples los requisitos, el objeto no da bonificaciones (`EquipmentBonuses`).
- [x] **Idiomas separados** (Bajo). `es_es.json` con la mezcla de nombres elegida y `en_us.json` todo en inglés.
- [x] **Opciones según el tipo de objeto** (Bajo). Campo `applies_to` y tags `overgearedmod:weapons` y `overgearedmod:armor`.
- [x] **Equilibrio en la configuración** (Bajo). El record `Balance` y una config de servidor por mundo.
- [x] **Lista con scroll reutilizable** (Medio).

**Sensación de juego**

- [x] **"+N" en el nombre y color por rango** (Bajo).
- [x] **Explicar las estadísticas en la ventana** (Bajo).
- [x] **Reasignar puntos y abandonar misiones** (Bajo).
- [x] **Equilibrar la XP** (Bajo-Medio). La XP cuenta vida, daño y armadura del mob; los mobs de spawner dan un 25 %. Falta probar la curva jugando.
- [x] **Iconos de habilidad** (Medio). Campo `icon` en el JSON.
- [x] **Misión activa en el HUD** (Medio).

**Más profundidad**

- [x] **Clases al estilo canon** (Alto). Básicas elegibles y avanzadas por misión (`selectable`, `required_class`, `class_reward`). Ejemplo: El legado de Pagma.
- [x] **Penalización por muerte** (Bajo-Medio). Se pierde un 10 % (configurable) de la XP del nivel actual, sin bajar de nivel.
- [x] **Espada legendaria de Grid** (Bajo). `grid_legendary_sword`, forjable, pide nivel 10 y 8 de STR.

### Sistemas nuevos (PR #6 a #10)

- [x] **Monstruos con nivel y élites** (Medio). Nivel del jugador más cercano (± 2), escalado de vida, daño y XP; un 5 % son élites.
- [x] **Mineral y material propios** (Medio). Mitril: menas, mitril en bruto y lingote.
- [x] **NPC herrero** (Alto). Comercia y, agachado, abre la pestaña Misiones.
- [x] **Efectos de estado propios** (Bajo). Sangrado (`overgearedmod:bleeding`).
- [x] **Generación de datos (datagen)** (Medio). `gradlew runData` genera modelos, blockstates, loot tables, tags y recetas.

</details>

> Dos tareas pendientes de esta etapa se han movido: **texturas propias**, a la fase 4 y a la pista Arte; **JEI**, a la fase 7.

---

## Fase 3: fundamentos

**Objetivo:** reforzar la base antes de añadir decenas de clases, profesiones y sistemas. Hacerlo ahora es barato; después obliga a rehacer contenido.
**Depende de:** —

- [ ] **Tests unitarios con JUnit** (Bajo). Probar las reglas puras (`ProgressionRules`, `EquipmentRules`, `RatingRoller`…) sin arrancar Minecraft. Los gametests quedan para la integración.
- [ ] **CI en GitHub Actions** (Bajo). En cada PR: compilar, tests unitarios y `runGameTestServer`.
- [ ] **Test de claves de traducción** (Bajo). Comprobar que `es_es.json` y `en_us.json` tienen las mismas claves.
- [ ] **Actualizar `Plantilla_Satisfy.md`** (Bajo). Quitar "Sin probar en el juego" de las limitaciones, corregir la ruta de la config en el árbol de archivos (dice `overgearedmod-common.toml`, pero es la config de servidor `overgearedmod-server.toml`) y no fijar el número de tests en el texto.
- [ ] **Eventos de dominio internos** (Medio). Eventos propios (`ItemForged`, `SkillUsed`, `MobKilled`, `LevelUp`…) a los que se suscriben títulos, misiones y contadores. Evita que `GoalTracker` crezca sin control (como MediatR en .NET).
- [ ] **Fuentes de estadísticas genéricas** (Medio). `StatModifier(source, stat, value, operation)` y una única función que agregue todas las fuentes (asignadas, clase, títulos, equipo y, más adelante, talentos, sets, buffs y gremio). El tooltip de cada estadística muestra de dónde sale.
- [ ] **Validador de contenido** (Bajo-Medio). Gametest o `/satisfy validate` que compruebe las referencias cruzadas entre JSON (clases → habilidades, misiones → clases, nodos de árbol huérfanos…). Va aquí porque el contenido empieza a crecer en la fase 4.

> **Hito:** el mod se comporta igual que ahora, pero con tests rápidos, CI y un núcleo (eventos y estadísticas) listo para escalar.

## Fase 4: rebanada vertical

**Objetivo:** un mini Satisfy completo antes de crecer en horizontal, para comprobar que es divertido. Cada pieza es una **versión mínima** que las fases siguientes generalizan.
**Depende de:** 3

- [ ] **Estructura base** (Medio). Pipeline de estructuras por datapack (plantilla NBT + `structure` + `structure_set`), reutilizable para pueblos y mazmorras. Se generaliza en la fase 10.
- [ ] **Pueblo pequeño** (Medio). Una estructura con el herrero y un segundo NPC de misiones.
- [ ] **Misiones en cadena** (Bajo-Medio). Campo `requires_quest` para que una misión desbloquee otra. Cadena de tres o cuatro misiones: pueblo → mazmorra → jefe.
- [ ] **Botín con rango** (Medio). Cofres, élites y jefes sueltan equipo con rango y opciones, como la forja. Loot function propia.
- [ ] **Mazmorra pequeña** (Medio). Un par de salas, élites y un cofre de botín con rango. Se generaliza en la fase 10.
- [ ] **Un jefe** (Medio-Alto). Barra de vida, dos fases y botín garantizado. Usa los tipos de efecto actuales (`SkillEffect`, efectos de estado); el motor único llega en la fase 5. Se generaliza en la fase 10.
- [ ] **Una clase oculta** (Medio). Se desbloquea al cumplir una condición secreta (vía eventos de dominio) y llega como misión. Versión mínima de *Clases ocultas por condiciones* (fase 5).
- [ ] **Notificaciones estilo Satisfy** (Medio). Ventanas de sistema para títulos, clases, misiones y subidas de nivel.
- [ ] **Texturas de lo que se ve** (Medio). PNG propios para lo que aparece en la rebanada (objetos, NPC, jefe, interfaz principal).

> **Hito:** una sesión de 1-2 horas que recorre pueblo → misiones → mazmorra → jefe → clase oculta. Si no es divertida, se ajusta antes de seguir.

## Fase 5: motor de clases

**Objetivo:** el sistema que soporta todas las clases y habilidades futuras.
**Depende de:** 3, 4

- [ ] **Motor de efectos único** (Alto). Un solo `Effect` con un `EffectContext` (quién lanza, objetivo, posición, fuente) reutilizado por habilidades, opciones de equipo y, más adelante, gemas, sets, talentos, jefes y consumibles. Sustituye a `SkillEffect` y `OptionEffect` como base común. Va aquí, y no en la fase 3, porque es cuando aparecen sus consumidores nuevos (habilidades v2, talentos); antes sería abstraer sin necesidad.
- [ ] **Habilidades v2** (Alto). Pasivas, lista de efectos por habilidad, condiciones (`requires_target`, `while_sneaking`…) y dominio (la habilidad sube de nivel con el uso, como en el canon).
- [ ] **Más huecos de habilidad** (Bajo). De 3 a 5-6, configurable; teclas nuevas.
- [ ] **Rangos de clase** (Medio). Común → rara → épica → única → legendaria (y oculta). Afecta a bonos y a qué habilidades da. *Confirmar la escala canon (`Canon.md`).*
- [ ] **Árbol de talentos** (Alto). JSON `skill_tree/<clase>.json` con nodos, requisitos y coste; puntos de habilidad por nivel; pestaña *Talentos* con el árbol dibujado. Usa `StatModifier` y el motor de efectos.
- [ ] **Clases ocultas por condiciones** (Medio). Generaliza la versión de la rebanada: campo `unlock` con uno o varios `goal` y goals nuevos (usar habilidad N veces, forjar X objetos de rango Y, matar sin recibir daño…).
- [ ] **Misión de cambio de clase al azar** (Bajo-Medio). Estilo canon: al llegar a cierto nivel, una misión ofrece una clase sorteada según tus estadísticas y contadores.

> **Hito:** las tres clases básicas actuales funcionan sobre el motor nuevo, con árbol de talentos, rangos y desbloqueo de clases ocultas.

## Fase 6: clases

**Objetivo:** llenar el motor de clases. Cada patrón introduce una técnica que luego reutilizan otras clases.
**Depende de:** 5

- [ ] **Elementos y marcas** (Medio). *Congelado* (ralentiza y bloquea el salto), *Quemadura*, *Marca del cazador* (más daño recibido) y combos entre ellos. Clases: *Mago de hielo*, *Cazador* (con trampas como bloque o entidad temporal).
- [ ] **Vida y producción** (Medio). Bonus a cultivos y animales, cosechas especiales con rango y materiales que alimentan la artesanía. Clase: *Granjero legendario* (*confirmar nombre canon*).
- [ ] **Invocaciones** (Alto). Entidades aliadas con propietario, límite, duración y órdenes (seguir, atacar, quieto). Clase: *Nigromante*. GeckoLib si llevan animación propia.
- [ ] **Copiar habilidades** (Alto). Guardar temporalmente una habilidad vista (de jugador o de élite) en un hueco especial, con usos limitados. Clase: *Duplicador* (*confirmar nombre canon*).
- [ ] **Estadísticas del canon** (Medio). Solo las que tengan un efecto claro en Minecraft (p. ej. *Insight* para ver más información de objetos y mobs). *Confirmar nombres y efectos (`Canon.md`).*
- [ ] **Efectos visuales de habilidades** (Medio). Partículas, sonidos y animación del jugador para que cada clase se sienta distinta.

> **Hito:** 8-10 clases jugables (básicas, avanzadas y ocultas), con al menos una de cada patrón.

## Fase 7: artesanía profunda

**Objetivo:** el pilar 1. Que forjar y diseñar equipo sea tan valioso como luchar.
**Depende de:** 3 y 5 (las gemas usan el motor de efectos); se beneficia de la 6 por los materiales del Granjero.

- [ ] **Firma del forjador** (Bajo). Cada objeto forjado guarda quién lo hizo y cuándo, y lo muestra en el tooltip.
- [ ] **Profesiones** (Alto). Nivel propio por profesión: herrería (la actual), alquimia, cocina, sastrería, joyería. Cada una con su estación de trabajo y recetas en JSON.
- [ ] **Sets de equipo** (Medio). JSON `item_set/` con bonus a 2, 4… piezas (vía `StatModifier`), visibles en el tooltip.
- [ ] **Gemas y runas** (Medio). Huecos en el objeto (según rango) donde engarzar gemas; reutiliza el motor de efectos.
- [ ] **Recetas en JEI** (Medio). Imprescindible con muchas profesiones y recetas.
- [ ] **Diseño de objetos estilo Grid** (Muy alto). Pantalla de diseño: material, forma, estadísticas objetivo y nombre. Al forjar, el resultado depende de la destreza y la profesión. El diseño queda guardado como receta del jugador.

> **Hito:** un herrero puede diseñar su propia arma, forjarla con su firma y engarzarle gemas. La venta llega con el mercado (fase 9).

## Fase 8: servidor con amigos

**Objetivo:** lo necesario para que 5-10 jugadores compartan un servidor sin romper nada.
**Depende de:** 3

- [ ] **Servidor dedicado de pruebas** (Bajo-Medio). Probar siempre en servidor real y medir con un perfilador (p. ej. *spark*).
- [ ] **Versionado de datos guardados** (Bajo). Campo `version` en los codecs de los attachments y migraciones simples, para cambios que rompan el formato sin borrar el progreso de los jugadores. Antes no hace falta: los campos opcionales con valor por defecto ya permiten añadir datos sin romper las partidas.
- [ ] **Datos globales del servidor** (Medio). `SavedData` para lo que no es de un jugador: rankings, gremios, subastas, objetos únicos.
- [ ] **Regiones por nivel** (Medio). El nivel de los mobs depende de la región (distancia al spawn o bioma), no del jugador más cercano, que es explotable en multijugador.
- [ ] **Oro virtual** (Bajo). Attachment de saldo, comandos de admin, pago con NPCs y saldo en la ventana.
- [ ] **Muerte v2: caída de equipo** (Medio). Probabilidad configurable de soltar un objeto equipado; objetos *ligados* o protegidos que nunca caen.
- [ ] **Permisos y comandos de admin** (Bajo). Niveles de permiso para `/satisfy` y para moderar.
- [ ] **Límite de paquetes** (Bajo). Evitar que un cliente sature el servidor con peticiones.
- [ ] **Sincronizar solo lo necesario** (Medio). Datos privados (misiones, contadores) solo a su dueño; el maná y otros valores que cambian a menudo, con menos frecuencia.

> **Hito:** servidor estable con 5-10 amigos, economía básica y muerte con riesgo real.

## Fase 9: social

**Objetivo:** el pilar 4. Jugar con otros y comerciar.
**Depende de:** 8

- [ ] **Grupos (party)** (Medio). Invitar, XP compartida por cercanía, reparto de botín y progreso de misiones `kill` compartido.
- [ ] **Chat de grupo y gremio** (Bajo).
- [ ] **Gremios** (Alto). Crear, rangos y permisos, almacén común, nivel de gremio con bonos. Pestaña *Gremio*.
- [ ] **PvP y rankings** (Medio). Duelos con petición, ranking de nivel, PvP y forja (pestaña *Ranking*). Aplica la caída de equipo de la fase 8.
- [ ] **Mercado entre jugadores** (Alto). Casa de subastas con oro virtual a través de un NPC subastador: publicar, pujar, comprar ya y comisión. Muestra la firma del forjador.

> **Hito:** jugar en grupo, pertenecer a un gremio y vender en el mercado lo que forjas.

## Fase 10: mundo y mazmorras (overworld)

**Objetivo:** generalizar lo de la rebanada a todo el overworld.
**Depende de:** 4, 8

- [ ] **Monstruos propios** (Alto). Modelos en Blockbench + GeckoLib, con habilidades que reutilizan el motor de efectos.
- [ ] **Estructuras propias** (Alto). Más pueblos con NPCs del mod, ruinas y torres, sobre el pipeline de la fase 4.
- [ ] **Mazmorras compartidas** (Alto). Varias mazmorras por región, con botín con rango y reinicio por tiempo.
- [ ] **Jefes** (Alto). Varios jefes con fases, ataques telegrafiados y botín garantizado de rango alto.

> **Hito:** explorar el overworld y encontrar mazmorras con jefe acordes a cada región.

## Fase 11: mundo vivo

**Objetivo:** el pilar 3. Un mundo que reacciona a los jugadores.
**Depende de:** 10

- [ ] **Afinidad con NPCs** (Medio). Cada NPC recuerda a cada jugador; la afinidad cambia precios y desbloquea misiones, habilidades y clases.
- [ ] **Misiones con rango** (Bajo-Medio). Rango de dificultad en las misiones. *Confirmar la escala canon (`Canon.md`).*
- [ ] **Rutinas de NPCs** (Alto). Horarios (trabajar, comer, dormir) y desplazamientos dentro de su pueblo.
- [ ] **Reinos y facciones** (Alto). Reputación por facción, territorios y relaciones entre ellas; las acciones de los jugadores las cambian.
- [ ] **Eventos de mundo** (Alto). Invasiones o jefes de mundo anunciados a todo el servidor.

> **Hito:** los NPCs reaccionan a lo que hace cada jugador y los reinos cambian con el tiempo.

## Fase 12: escala MMO (aspiracional)

**Objetivo:** abrir el servidor a muchos jugadores.
**Depende de:** 9

> Aspiracional: un servidor de Minecraft con mods pesados llega, como mucho, a unos cientos de jugadores. Esta fase y la 13 son el horizonte, no un compromiso.

- [ ] **Pruebas de carga** (Medio). Medir con muchos jugadores (o clientes simulados) y perfilar con *spark*.
- [ ] **Optimización** (Medio-Alto). Corregir los sistemas más costosos que salgan en las pruebas (ticks de NPCs, sincronización, búsquedas en `SavedData`).
- [ ] **Moderación a escala** (Medio). Herramientas de admin para gremios, mercado y reportes.

> **Hito:** el servidor aguanta muchos jugadores sin perder rendimiento.

## Fase 13: dimensión Satisfy y contenido final (aspiracional)

**Objetivo:** el destino de final de juego.
**Depende de:** 10, 11

- [ ] **Dimensión Satisfy** (Muy alto). Un continente propio con regiones y reinos, reutilizando todo lo de las fases 10 y 11.
- [ ] **Mazmorras instanciadas** (Muy alto). Una copia privada por grupo, creada al entrar y borrada al terminar.
- [ ] **Raids** (Alto). Jefes para gremios o grupos grandes dentro de las instancias.

> **Hito:** el mundo de Satisfy como destino de final de juego.

---

## Pistas continuas

Avanzan en paralelo a las fases y nunca se cierran.

- **Canon.** Resolver los pendientes de `Canon.md` antes de la fase que los necesita (p. ej. los rangos de clase antes de la fase 5).
- **Arte.** Texturas, modelos y sonidos propios para sustituir los provisionales.
- **Contenido.** Catálogo de clases (una ficha por clase en el GDD), misiones, objetos y títulos.
- **Publicación.** Solo si se decide publicar: mover el contenido con nombres canon a un datapack interno `overgeared_canon` y rellenar la columna *Nombre original* de `Canon.md`.

## Ideas

Sin compromiso. Cuando una idea se decida, pasa a la fase que le toque y se describe en el GDD.

- **Anuncios globales** (Bajo). "Primer jugador del servidor en…" (clase oculta, jefe, objeto legendario).
- **Simulador de equilibrio** (Medio). Programa Java que use las reglas puras para simular cientos de horas de juego y dibujar nivel, oro y probabilidades de mejora.
- **Guía dentro del juego** (Medio). Una pestaña que explique cada sistema.
- **Durabilidad y reparación por herreros** (Medio). Demanda constante para los artesanos.

## Reglas

- Cada sistema sigue el patrón de la plantilla: **Definición (JSON) → Estado → Reglas puras → Servicio → Eventos/Red/UI**, con sus tests.
- Antes de usar un nombre canon, añadirlo a `Canon.md` con capítulo y certeza.
- Al terminar una tarea, actualizar el estado del sistema en `GDD.md`.
- Desde la fase 8, probar cada fase en servidor dedicado, no solo en `runClient`.
