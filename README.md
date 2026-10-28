# Restless

Хоррор-мод для Minecraft 26.3: личные и территориальные духи устраивают случайные события,
которые становятся опаснее по мере прогресса игрока.

## Структура

| Путь | Что |
|---|---|
| `apps/mod/core` | Логика мода. Только vanilla Minecraft, без API загрузчика |
| `apps/mod/neoforge` | Адаптер NeoForge: точка входа, реализация `Platform`, сборка итогового jar |
| `apps/frontend` | Вики (VitePress) |

Всё, что зависит от загрузчика, `core` получает через `dev.keelfy.restless.platform.Platform`
(ServiceLoader). Порт на Fabric = новый модуль `apps/mod/fabric` со своей реализацией `Platform`.

## Запуск

Нужен [mise](https://mise.jdx.dev): `mise install` ставит Java 25, Node 24, pnpm 12.

```sh
mise run mod:build    # собрать jar → apps/mod/neoforge/build/libs
mise run mod:client   # dev-клиент
mise run mod:server   # dev-сервер
mise run wiki:dev     # вики локально
mise run wiki:build   # статическая сборка вики
```
