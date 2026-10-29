# Roadmap

Orden de trabajo: primero mejorar lo que ya existe y después añadir sistemas nuevos. Marca con `[x]` lo que esté hecho.

## 1. Mejoras de lo que ya existe

### P1: base sólida

- [x] **Probar en el juego y corregir** (Bajo). Nadie había visto aún las pantallas ni el HUD, y todo lo demás se construye encima.
- [x] **Requisitos coherentes** (Bajo). Si no cumples los requisitos, el objeto no da bonificaciones. Ahora se aplican al jugador que lo lleva (`EquipmentBonuses`).
- [x] **Idiomas separados** (Bajo). `es_es.json` con la mezcla de nombres elegida y `en_us.json` todo en inglés.
- [x] **Opciones según el tipo de objeto** (Bajo). Campo `applies_to` y tags `overgearedmod:weapons` y `overgearedmod:armor`.
- [x] **Equilibrio en la configuración** (Bajo). El record `Balance` y una config de servidor por mundo.
- [x] **Lista con scroll reutilizable** (Medio). Habilidades y Misiones se cortan en 7 filas y Perfil en unos 9 títulos. Es una pieza de interfaz que aprovechan todas las pestañas.

### P2: sensación de juego

- [x] **"+N" en el nombre y color por rango** (Bajo). Es lo primero que ve el jugador y muy propio de Satisfy; hoy solo aparece en el tooltip.
- [x] **Explicar las estadísticas en la ventana** (Bajo). Al pasar el ratón por STR, DEX…, mostrar qué hace cada punto.
- [x] **Reasignar puntos y abandonar misiones** (Bajo). Pergamino de reasignación (devuelve los puntos asignados) y botón ✖ en las misiones activas.
- [x] **Equilibrar la XP** (Bajo-Medio). La XP cuenta vida, daño y armadura del mob, y los mobs de spawner dan un 25 %. La curva sigue siendo configurable y falta probarla jugando.
- [x] **Iconos de habilidad** (Medio). Un campo `icon` en el JSON para que los huecos del HUD muestren algo mejor que una inicial.
- [x] **Misión activa en el HUD** (Medio). Ver el progreso sin abrir la ventana.

### P3: más profundidad

- [x] **Clases al estilo canon** (Alto). Clases básicas elegibles y clases avanzadas solo por misión (`selectable`, `required_class`, `class_reward`). Ejemplo: El legado de Pagma.
- [x] **Penalización por muerte** (Bajo-Medio). Se pierde un 10 % (configurable) de la XP del nivel actual, sin bajar de nivel. Falta confirmar en la novela cómo funciona en el canon.
- [ ] **Texturas e interfaz propias** (Medio). Sustituir las texturas vanilla provisionales por PNG propios.
- [ ] **Recetas de forja en JEI** (Medio). Mejora la experiencia, pero añade una dependencia externa que mantener.

### Extras

- [x] **Espada legendaria de Grid** (Bajo). Usar la textura (ahora `grid_legendary_sword.png`) en un objeto nuevo (`grid_legendary_sword`, "Espada legendaria de Grid" / "Grid's Legendary Sword") que se forje en el yunque y pida nivel 10 y 8 de STR. Si hay un nombre canon, confirmarlo en la novela (capítulo) antes de usarlo.

## 2. Sistemas nuevos

Cada uno introduce un patrón de Minecraft que la plantilla todavía no tiene.

- [x] **Monstruos con nivel y élites** (Medio). Los mobs hostiles toman el nivel del jugador más cercano (± 2), escalan vida, daño y XP, y muestran el nivel sobre la cabeza. Un 5 % son élites. Se conecta con la XP, las misiones (`kill`) y los títulos.
- [ ] **Mineral y material propios** (Medio). Generación del mundo por datapack (vetas), un bloque con loot table y la cadena mineral → lingote → forja. Sustituiría al "Lingote de prueba".
- [ ] **NPC herrero o de misiones** (Alto). Entidad propia con modelo, IA sencilla e interacción (dar misiones, comerciar). Mejor en una rama propia; valorar GeckoLib solo si hace falta animación.
- [x] **Efectos de estado propios** (Bajo). Sangrado (`overgearedmod:bleeding`): daño por segundo que ignora la armadura. Lo aplican la opción de arma `apply_effect` y las habilidades con `target_effects` (Corte desgarrador).
- [ ] **Generación de datos (datagen)** (Medio). Generar desde Java los JSON de traducciones, modelos, recetas y tags. No es una mecánica, pero facilita mantener la plantilla a medida que crece.

Orden sugerido: monstruos con nivel → mineral propio → efectos de estado → datagen → NPC.
