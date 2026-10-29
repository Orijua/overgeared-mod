# Plantilla Satisfy: guía breve

Plantilla de partida del mod: cuatro sistemas pequeños y funcionales que sirven de ejemplo para seguir creciendo. Base técnica: Minecraft 1.21.1, NeoForge 21.1.265 y Java 21.

## Cómo probarlo

| Qué | Cómo |
|---|---|
| Jugar en desarrollo | `gradlew runClient` (o la configuración *client* de IntelliJ) |
| Tests en juego | `gradlew runGameTestServer`: arranca un servidor real, ejecuta `SatisfyGameTests` y termina. Si todo va bien, debe decir *All 6 required tests passed*. |
| Subir de nivel rápido | `/satisfy xp 500` (requiere trucos/op) |
| Reiniciar progresión | `/satisfy reset` |

Teclas por defecto (se pueden cambiar en *Controles → Overgeared*):

- **K**: Status Window (nivel, XP, maná, puntos y estadísticas, con botones **+**).
- **R**: habilidad del arma en mano. Con cualquier espada lanza *Pagma's Swordsmanship: Link*.

Recorrido rápido para probarlo todo:

1. Fabrica el **Lingote de prueba**: lingote de hierro + lingote de cobre, sin forma. Necesitarás 4 para el yunque y 2 por espada.
2. Fabrica el **Yunque de herrero**: 3 bloques de hierro arriba, 1 lingote de prueba en el centro y 3 lingotes de prueba abajo.
3. En el yunque, pon 2 lingotes de prueba arriba y 1 palo abajo, y recoge la **Espada de Jaffa**. Su rango sale al azar, y el tooltip lo muestra.
4. Mata mobs o usa `/satisfy xp 500`, pulsa **K** y reparte los puntos (por ejemplo, en DEX para forjar mejor).
5. Con la espada en la mano, pulsa **R** delante de un mob.

## Arquitectura

```
com.orijua.overgearedmod
├── Overgearedmod          entrada común: conecta registros y eventos (como una composition root en .NET)
├── Config                 valores de equilibrio → config/overgearedmod-common.toml
├── registry/              un DeferredRegister por tipo (items, bloques, componentes, attachments, menús, recetas, pestañas)
├── rating/                rango de equipo
├── progression/           nivel, XP y estadísticas
├── skill/                 habilidades activas, maná y enfriamiento
├── smithing/              yunque, menú y receta de forja
├── network/               paquetes cliente → servidor
├── client/                solo cliente: teclas y pantallas (@Mod con dist = CLIENT)
└── gametest/              tests en juego
```

Ideas clave, comparadas con .NET:

- **Reglas puras frente a integración.** `RatingRoller` y `ProgressionRules` no tocan Minecraft: son la "capa de dominio", fácil de leer y de probar. Las clases `*Events` y `*Service` son la "capa de aplicación" que las conecta con el juego.
- **Datos inmutables.** `PlayerStats` y `SkillState` son `record`, el equivalente a los `record` de C#. Cada cambio crea una copia nueva y se guarda con `player.setData(...)`.
- **El servidor manda.** El cliente solo envía intenciones: "he pulsado R" (`UseSkillPayload`) o "+1 a DEX" (`AllocateStatPayload`). El servidor valida el enfriamiento, el maná y los puntos libres.
- **Sincronización automática.** Los *data attachments* llevan `.sync(...)`, así que cada `setData` envía el valor al cliente del jugador. La Status Window solo lee esos datos.
- **Dos formatos por dato.** `Codec` define cómo se guarda en disco (NBT/JSON) y `StreamCodec` cómo viaja por la red.

## Los 4 sistemas

| Sistema | Archivos | Dónde se guarda |
|---|---|---|
| Rango de equipo | `rating/ItemRating`, `RatingRoller`, `RatingEvents` | *data component* `overgearedmod:item_rating` en el `ItemStack` |
| Progresión | `progression/*`, `client/StatusScreen` | *attachment* `player_stats` (se conserva al morir) |
| Habilidad activa | `skill/*`, `network/UseSkillPayload` | *attachment* `skill_state` (maná y enfriamiento) |
| Herrería | `smithing/*`, `data/overgearedmod/recipe/forging/*.json` | El menú no guarda nada: es como la mesa de crafteo |

Equivalencias de las estadísticas (diseño propio, se ajustan en `StatEffects` y `ProgressionRules`):

| Estadística | Efecto por punto |
|---|---|
| STR | +0,25 de daño de ataque, y la habilidad escala un 3 % |
| STA | +0,5 de vida máxima |
| AGI | +0,4 % de velocidad de movimiento |
| INT | +5 de maná máximo y +0,2 de maná por segundo |
| DEX | +1 % de velocidad de ataque, mejor rango al forjar y la habilidad escala un 2 % |

## Cómo añadir cosas

**Una receta de forja nueva** (no hace falta tocar Java): crea `data/overgearedmod/recipe/forging/<nombre>.json`.

```json
{
  "type": "overgearedmod:forging",
  "material": { "item": "minecraft:gold_ingot" },
  "material_count": 2,
  "handle": { "item": "minecraft:stick" },
  "result": { "id": "minecraft:golden_sword", "count": 1 },
  "smithing_xp": 12
}
```

`handle` es opcional; si no lo pones, el hueco inferior tiene que estar vacío. `material` admite tags: `{ "tag": "c:ingots/iron" }`.

**Una habilidad nueva:**

1. Crea una clase que implemente `Skill`, con su coste, su enfriamiento, el objeto que la permite y el método `execute`.
2. Añádela a `Skills.ALL`.
3. Añade su nombre en `lang/en_us.json` (`skill.overgearedmod.<id>`).

Si dos habilidades admiten el mismo objeto, se usa la primera de la lista. Cuando haya varias, el siguiente paso natural es un paquete con el id de la habilidad, o una tecla por hueco.

**Un rango nuevo o distintos bonus:** edita el enum `ItemRating` y los pesos `BASE_WEIGHTS` de `RatingRoller`; los dos arrays deben tener la misma longitud. Añade la traducción `rating.overgearedmod.<nombre>`.

**Una estadística nueva:**

1. Añádela a `StatType`.
2. Añade un campo en `PlayerStats` (record y `CODEC`), y su caso en `get` y `with`.
3. Si modifica un atributo vanilla, añade su equivalencia en `StatEffects`.

## Canon, inspiración y diseño propio

Siguiendo el criterio de `Fuentes_Overgeared_Conocimiento.md` §4:

| Elemento | Clasificación | Pendiente |
|---|---|---|
| Rangos Normal → Myth | Canon (sistema de rangos de Satisfy) | Confirmar la lista exacta y el orden en la novela |
| Colores y bonus de cada rango | Diseño propio | — |
| STR/STA/AGI/INT/DEX | Canon (Satisfy tiene estas y muchas más) | Satisfy tiene más estadísticas (Dignidad, Insight…), sin implementar |
| Efecto de cada estadística | Diseño propio | — |
| Status Window | Inspiración (ventana de estado de Satisfy) | — |
| *Pagma's Swordsmanship: Link* | Inspiración: el nombre viene del canon, el efecto es propio | Verificar el nombre y el capítulo en Wuxiaworld |
| Forja con rango aleatorio según la destreza | Inspiración (la herrería de Grid) | — |
| Yunque de herrero, Lingote de prueba, Espada de Jaffa | Diseño propio | — |

## Limitaciones conocidas y siguientes pasos

- **Aspecto provisional:** el yunque usa el modelo del yunque vanilla y la interfaz de forja usa la textura del horno. Sustitúyelos con Blockbench y una textura propia en `textures/gui/`.
- **HUD:** la barra azul encima de la comida es el maná. La línea amarilla de debajo es el enfriamiento de la habilidad del arma que llevas, y se vacía hasta que puedes volver a usarla. Está en `client/ManaHudLayer` y se registra encima de la capa vanilla `FOOD_LEVEL`. Igual que la vida y la comida, se oculta en creativo.
- **Solo hay traducción en `en_us.json`,** con la mezcla de nombres elegida. Si juegas en español sin `es_es.json`, el juego usa esa misma.
- **La interfaz no tiene pruebas automáticas.** Los tests prueban la lógica en un servidor real. Las pantallas, el tooltip y las teclas hay que probarlos a mano con `runClient`.
