# Roadmap: sistemas nuevos pendientes

Ideas para después de cerrar la plantilla Satisfy. Cada una introduce un patrón nuevo de Minecraft que la plantilla todavía no tiene. Antes de empezar cualquiera, conviene que las mecánicas actuales estén probadas y refinadas.

| Sistema | Qué aporta a la plantilla | Coste | Notas |
|---|---|---|---|
| **Monstruos con nivel y élites** | Modificar mobs vanilla al aparecer (attachment en la entidad), escalar su vida, daño y XP, y mostrar el nivel sobre la cabeza | Medio | Encaja con Satisfy y hace que la progresión importe. Se conecta con XP, misiones (`kill`) y títulos |
| **Mineral y material propios** | Generación del mundo por datapack (vetas), bloque con loot table y la cadena mineral → lingote → forja | Medio | Base de cualquier material de Overgeared. Sustituiría al "Lingote de prueba" |
| **NPC herrero o de misiones** | Entidad propia con modelo, IA sencilla e interacción (dar misiones, comerciar) | Alto | Es lo que más se siente como Satisfy, pero conviene hacerlo en una rama propia. Valorar GeckoLib solo si hace falta animación |
| **Efectos de estado propios** | Un `MobEffect` propio (p. ej. Sangrado) que puedan aplicar opciones y habilidades | Bajo | Encaja con los tipos actuales: un tipo nuevo de `OptionEffect` y otro de `SkillEffect` |
| **Generación de datos (datagen)** | Generar desde Java los JSON de traducciones, modelos, recetas y tags | Medio | No es una mecánica, pero facilita mucho mantener la plantilla a medida que crezca |

Orden sugerido: monstruos con nivel → mineral propio → efectos de estado → datagen → NPC.
