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
- [ ] **Reasignar puntos y abandonar misiones** (Bajo). Ahora la única salida es `/satisfy reset`, que lo borra todo.
- [ ] **Equilibrar la XP** (Bajo-Medio). La XP depende solo de la vida del mob: se puede abusar con granjas y la curva no está probada jugando.
- [ ] **Iconos de habilidad** (Medio). Un campo `icon` en el JSON para que los huecos del HUD muestren algo mejor que una inicial.
- [ ] **Misión activa en el HUD** (Medio). Ver el progreso sin abrir la ventana.

### P3: más profundidad

- [ ] **Clases al estilo canon** (Alto). En la novela una clase se consigue con una misión, no se elige. Depende de que las misiones estén pulidas.
- [ ] **Penalización por muerte** (Bajo-Medio). Da peso a morir. Antes hay que confirmar en la novela cómo funciona.
- [ ] **Texturas e interfaz propias** (Medio). Sustituir las texturas vanilla provisionales por PNG propios.
- [ ] **Recetas de forja en JEI** (Medio). Mejora la experiencia, pero añade una dependencia externa que mantener.

### Extras

- [ ] **Espada legendaria de Grid** (Bajo). Usar la textura `espada_legendaria_grid.png` en un objeto nuevo (`grid_legendary_sword`, "Espada legendaria de Grid" / "Grid's Legendary Sword") que se forje en el yunque y pida nivel 10 y 8 de STR. Si hay un nombre canon, confirmarlo en la novela (capítulo) antes de usarlo.

## 2. Sistemas nuevos

Cada uno introduce un patrón de Minecraft que la plantilla todavía no tiene.

- [ ] **Monstruos con nivel y élites** (Medio). Modificar mobs vanilla al aparecer (attachment en la entidad), escalar su vida, daño y XP, y mostrar el nivel sobre la cabeza. Se conecta con la XP, las misiones (`kill`) y los títulos.
- [ ] **Mineral y material propios** (Medio). Generación del mundo por datapack (vetas), un bloque con loot table y la cadena mineral → lingote → forja. Sustituiría al "Lingote de prueba".
- [ ] **NPC herrero o de misiones** (Alto). Entidad propia con modelo, IA sencilla e interacción (dar misiones, comerciar). Mejor en una rama propia; valorar GeckoLib solo si hace falta animación.
- [ ] **Efectos de estado propios** (Bajo). Un `MobEffect` propio (p. ej. Sangrado) que puedan aplicar opciones y habilidades: un tipo nuevo de `OptionEffect` y otro de `SkillEffect`.
- [ ] **Generación de datos (datagen)** (Medio). Generar desde Java los JSON de traducciones, modelos, recetas y tags. No es una mecánica, pero facilita mantener la plantilla a medida que crece.

Orden sugerido: monstruos con nivel → mineral propio → efectos de estado → datagen → NPC.
