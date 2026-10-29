# Plantilla Satisfy: guía

Plantilla de partida del mod: varios sistemas pequeños y funcionales, todos construidos con el mismo patrón, para elegir después cuál perfeccionar. Base técnica: Minecraft 1.21.1, NeoForge 21.1.265 y Java 21.

## Cómo probarlo

| Qué | Cómo |
|---|---|
| Jugar en desarrollo | `gradlew runClient` (o la configuración *client* de IntelliJ) |
| Tests en juego | `gradlew runGameTestServer`: arranca un servidor real, ejecuta los tests de `gametest/` y termina. Si todo va bien, debe decir *All 19 required tests passed*. |
| Ajustar el equilibrio | `saves/<mundo>/serverconfig/overgearedmod-server.toml` (curva de XP, XP por muerte, maná, probabilidades de mejora…). Se aplica por mundo y se sincroniza con los clientes |
| Subir de nivel rápido | `/satisfy xp 500` (requiere trucos/op) |
| Reiniciar todo (clase incluida) | `/satisfy reset` |

Teclas por defecto (se pueden cambiar en *Controles → Overgeared*):

| Tecla | Acción |
|---|---|
| **K** | Ventana Satisfy, con las pestañas Estado · Habilidades · Perfil · Misiones |
| **Z / X / C** | Habilidades de los huecos 1, 2 y 3 (se ven a la derecha de la barra de objetos) |

Recorrido rápido para probarlo todo:

1. **K → Perfil:** elige una clase. El Guerrero empieza con *Link*, el Herrero con *Guardia de hierro* y el Mago con *Saeta de llamas*.
2. **K → Misiones:** acepta *Primer encargo* y *Plaga de zombis*.
3. Fabrica **Lingotes de prueba** (lingote de hierro + lingote de cobre) y el **Yunque de herrero** (3 bloques de hierro arriba, 1 lingote de prueba en el centro y 3 abajo).
4. En el yunque, pon 2 lingotes de prueba arriba y 1 palo abajo, y recoge la **Espada de Jaffa**. Sale con rango, opciones al azar y requisitos: nivel 2 y 2 de STR. Si no los cumples, la espada no te deja atacar.
5. Pon la espada arriba y una **Piedra de mejora** abajo (lingote de prueba + fragmento de amatista), y pulsa **Mejorar**.
6. Mata zombis con Z/X/C. Ganarás XP, avanzarás en la misión y conseguirás el título *Cazador de zombis* a los 10.
7. Desde el inventario creativo (pestaña *Overgeared*) tienes un libro por habilidad y todos los objetos.

## Arquitectura: un mismo patrón para todo

Cada sistema se construye con las mismas piezas. Si entiendes uno, entiendes todos:

| Pieza | Qué es | Ejemplo |
|---|---|---|
| **Definición** | Qué existe. Va en JSON del datapack, sin tocar Java | `skill/pagma_link.json` → `SkillDefinition` |
| **Estado** | Qué tiene cada jugador u objeto. Es un `record` inmutable | `PlayerSkills` (attachment), `RolledOption` (componente) |
| **Reglas** | Cálculos puros, sin Minecraft, fáciles de probar | `EquipmentRules`, `ProgressionRules`, `RatingRoller` |
| **Servicio** | El único sitio que modifica el estado, siempre en el servidor | `SkillService`, `QuestService`, `EquipmentService` |
| **Eventos / Red / UI** | Cómo se conecta con el juego | `*Events`, `network/*Payload`, `client/tab/*Tab` |

```
com.orijua.overgearedmod
├── Overgearedmod          entrada común: registra todo (como una composition root en .NET)
├── Config                 valores de equilibrio → config/overgearedmod-common.toml
├── registry/              registros de Minecraft y de JSON (ModDatapackRegistries) y Definitions para leerlos
├── progression/           nivel, XP, estadísticas y estadísticas efectivas (asignadas + clase + títulos)
├── skill/                 habilidades: definición, efectos (skill/effect), huecos, maná y libros
├── equipment/             rango, opciones (equipment/option), requisitos y mejora
├── smithing/              yunque: forja y mejora
├── goal/                  objetivos (matar, forjar, nivel) y contadores; los usan títulos y misiones
├── playerclass/           clases
├── title/                 títulos
├── quest/                 misiones
├── network/               paquetes cliente → servidor y su único handler
├── client/                solo cliente: teclas, HUD, ventana (client/ui) y sus pestañas (client/tab)
└── gametest/              un archivo de tests por sistema
```

Ideas clave, comparadas con .NET:

- **El servidor manda.** El cliente solo envía intenciones (`UseSkillPayload`, `QuestActionPayload`…). `ServerPayloadHandler` las pasa al servicio, que vuelve a comprobar todas las reglas.
- **Sincronización automática.** Cada *attachment* lleva `.sync(...)`, así que `setData` envía el valor al cliente y las pantallas solo leen. Los registros JSON también llegan al cliente.
- **El JSON se valida al cargar.** Cada definición tiene un `Codec`. Un JSON mal escrito falla al arrancar el mundo con un error que señala el campo.
- **Polimorfismo por `type`.** Los efectos de habilidad, los efectos de opción y los objetivos usan un campo `"type"` en el JSON. Un enum (`SkillEffectType`, `OptionEffectType`, `GoalType`) asocia cada valor a su `record`, igual que un discriminador en `System.Text.Json`.

## Los sistemas

| Sistema | JSON (en `data/overgearedmod/overgearedmod/`) | Estado | UI |
|---|---|---|---|
| Progresión | — | attachment `player_stats` | Estado |
| Habilidades | `skill/*.json` | attachments `player_skills` y `mana` | Habilidades, HUD |
| Rango y opciones | `item_option/*.json` | componentes `item_rating`, `item_options` | tooltip |
| Requisitos y mejora | `requirements` en la receta de forja | componentes `requirements`, `enhancement` | tooltip, yunque |
| Herrería | `../recipe/forging/*.json` | nada (como la mesa de crafteo) | yunque |
| Clases | `player_class/*.json` | attachment `player_identity` | Perfil |
| Títulos | `title/*.json` | `player_identity` + `player_counters` | Perfil |
| Misiones | `quest/*.json` | attachment `player_quests` | Misiones |
| Efectos de estado | `MobEffect` en Java (`ModEffects`) | efecto vanilla sobre la entidad | iconos de efecto |
| Monstruos con nivel | — (números en la config, sección `mobs`) | attachment `mob_level` en el mob | nombre sobre la cabeza |

**Números del juego.** Todos los valores ajustables (XP, maná, mejora) están en el record `Balance`. Las reglas lo reciben como parámetro y así siguen siendo puras: los tests usan `Balance.DEFAULT` y el juego, `Balance.current()`, que viene de la configuración del servidor.

**Monstruos con nivel.** Al aparecer, cada mob hostil toma el nivel del jugador más cercano ± `levelSpread` (`MobLevelEvents`). Cada nivel suma un porcentaje de vida y daño base como modificadores permanentes (`MobLevelService`), así que la XP por matarlo ya crece sola. Un 5 % son élites: brillan, tienen +50 % de vida y daño y dan el triple de XP. Encima de la cabeza se ve "[Nv 5] Zombi" (`MobLevelNameTags`).

**Bonificaciones del equipo.** El rango, la mejora y las opciones no van "en el objeto", sino que se aplican al jugador que lo lleva (`EquipmentBonuses`), y solo si cumple sus requisitos. Se recalculan cada vez que cambia el equipo o las estadísticas.

Estadísticas (diseño propio, se ajustan en `StatEffects` y en `Balance`):

| Estadística | Efecto por punto |
|---|---|
| STR | +0,25 de daño de ataque; escala el daño de *area_damage* |
| STA | +0,5 de vida máxima |
| AGI | +0,4 % de velocidad de movimiento |
| INT | +5 de maná máximo y +0,2 de maná por segundo |
| DEX | +1 % de velocidad de ataque, mejor rango al forjar y más probabilidad de mejora |

## Cómo añadir contenido (solo JSON)

**Habilidad:** `skill/<id>.json`. Aparece sola como libro en la pestaña creativa.

```json
{
  "name": { "translate": "skill.overgearedmod.whirlwind" },
  "description": { "translate": "skill.overgearedmod.whirlwind.desc" },
  "icon": "minecraft:iron_axe",
  "mana_cost": 20,
  "cooldown_ticks": 200,
  "required_item": "minecraft:axes",
  "effect": { "type": "area_damage", "range": 3, "cone_degrees": 360, "strength_scaling": 0.05 }
}
```

`icon` es opcional: cualquier objeto, cuya textura se ve en el HUD y en la pestaña Habilidades. Tipos de efecto: `area_damage`, `self_buff` (`"effects": [{ "id": "minecraft:speed", "amplifier": 1, "duration": 200 }]`) y `projectile` (`count`, `speed`). `area_damage` acepta `target_effects` para dar efectos a cada enemigo golpeado (ejemplo: `rending_slash.json` con `overgearedmod:bleeding`).

**Opción de equipo:** `item_option/<id>.json`. El tipo es `attribute` (cualquier atributo vanilla, con `operation` opcional), `life_steal` o `apply_effect` (probabilidad al golpear de dar un efecto: `"effect": "overgearedmod:bleeding", "duration": 100`; el valor sorteado es la probabilidad). `applies_to` es opcional: limita la opción a un tag de objetos (`overgearedmod:weapons`, `overgearedmod:armor` o cualquier otro). Sin él, puede salir en cualquier objeto.

```json
{ "name": { "translate": "option.overgearedmod.knockback" }, "weight": 15, "min": 0.1, "max": 0.5,
  "applies_to": "overgearedmod:weapons",
  "effect": { "type": "attribute", "attribute": "minecraft:generic.attack_knockback" } }
```

**Receta de forja:** `data/overgearedmod/recipe/forging/<id>.json`. `handle` y `requirements` son opcionales.

```json
{
  "type": "overgearedmod:forging",
  "material": { "item": "minecraft:gold_ingot" }, "material_count": 2,
  "handle": { "item": "minecraft:stick" },
  "result": { "id": "minecraft:golden_sword" },
  "smithing_xp": 12,
  "requirements": { "level": 3, "stats": { "dexterity": 4 } }
}
```

**Clase:** `player_class/<id>.json`, con `bonus_stats`, `smithing_bonus` y `skills`. Las clases básicas se eligen desde Perfil; con `"selectable": false` es una clase avanzada que solo se consigue con una misión (`class_reward`).

**Título:** `title/<id>.json`, con un `goal` y `bonus_stats`.

**Misión:** `quest/<id>.json`, con un `goal`, `xp_reward`, `item_rewards` y `repeatable`. Opcionales: `required_class` (solo la acepta quien tenga esa clase) y `class_reward` (al reclamarla, cambia la clase y conserva las habilidades aprendidas). Ejemplo: `pagma_legacy.json`.

Los objetivos (`goal`) son de tres tipos:

| Tipo | Ejemplo |
|---|---|
| `kill` | `{ "type": "kill", "entity": "minecraft:skeleton", "count": 20 }` (sin `entity` cuenta cualquier mob) |
| `forge` | `{ "type": "forge", "min_rating": "unique", "count": 1 }` |
| `level` | `{ "type": "level", "level": 10 }` |

Recuerda añadir las traducciones en `assets/overgearedmod/lang/es_es.json` y `en_us.json`.

## Cómo añadir un tipo nuevo (Java)

- **Efecto de habilidad:** un `record` que implemente `SkillEffect`, con su `MapCodec`, y una entrada en `SkillEffectType`.
- **Efecto de opción:** un `record` que implemente `OptionEffect`. Sobrescribe solo los ganchos que necesites (`addModifiers`, `onHit`) y añade su entrada en `OptionEffectType`.
- **Efecto de estado:** una clase que extienda `MobEffect`, registrada en `ModEffects`, con su icono en `textures/mob_effect/<id>.png` y la clave `effect.overgearedmod.<id>`. Ejemplo: `BleedingEffect`, con su tipo de daño propio (`damage_type/bleeding.json`) que ignora la armadura.
- **Objetivo:** un `record` que implemente `Goal` y su entrada en `GoalType`. Si cuenta eventos nuevos, añade el método correspondiente en `GoalTracker`.
- **Pestaña de la ventana:** una clase que extienda `SatisfyTab` y su entrada en `SatisfyTabs`. Usa `UiStyle` para que se vea igual que el resto.
- **Sistema nuevo:** sigue la tabla del patrón. Si guarda datos del jugador, añade un attachment en `ModAttachments` (así `/satisfy reset` lo limpia solo). Si tiene contenido en JSON, añade una clave en `ModDatapackRegistries`.

## Canon, inspiración y diseño propio

Siguiendo el criterio de `Fuentes_Overgeared_Conocimiento.md` §4:

| Elemento | Clasificación | Pendiente |
|---|---|---|
| Rangos Normal → Myth | Canon (sistema de rangos de Satisfy) | Confirmar la lista exacta y el orden en la novela |
| STR/STA/AGI/INT/DEX | Canon (Satisfy tiene estas y muchas más) | Faltan estadísticas como Dignidad o Insight |
| Status Window y títulos | Inspiración (son sistemas de Satisfy) | — |
| Clases básicas elegibles y avanzadas por misión | Inspiración (en el canon las clases llegan por misiones o eventos) | La clase *Sucesor del herrero legendario* está inspirada en la de Grid: confirmar el nombre exacto en la novela |
| Pérdida de XP al morir | Diseño propio | Confirmar cómo castiga Satisfy la muerte en la novela |
| Mejora +1..+10 con fallo | Inspiración (la mejora de equipo de Satisfy) | Confirmar las reglas canon de fallo |
| *Pagma's Swordsmanship: Link* | Inspiración: el nombre viene del canon, el efecto es propio | Verificar el nombre y el capítulo en Wuxiaworld |
| Opciones, requisitos y forja según la destreza | Inspiración (los objetos de Grid) | — |
| Números, colores, misiones, títulos y objetos concretos | Diseño propio | — |

## Limitaciones conocidas

- **Aspecto provisional.** El yunque usa el modelo vanilla y la interfaz del horno, y los objetos reutilizan texturas vanilla. Los huecos de habilidad muestran la inicial, no un icono.
- **Listas sin desplazamiento.** Las pestañas Habilidades y Misiones muestran como mucho 7 filas, y Perfil caben unos 9 títulos. Con más contenido harán falta páginas o scroll.
- **Sin probar en el juego.** Los tests prueban la lógica en un servidor real, pero las pantallas, el HUD y las teclas hay que probarlos a mano con `runClient`.
- **Dos idiomas.** `es_es.json` lleva la mezcla de nombres elegida (Status Window, Rare/Epic…) y `en_us.json` está todo en inglés. Al añadir una clave, añádela en los dos archivos.
