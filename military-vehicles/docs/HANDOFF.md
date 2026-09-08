# Продолжение Military Vehicles

Ветка: `feature/military-vehicles`. Отдельный модуль: `military-vehicles/` в `ProkStudio/mine`. База: `83df29e`. Main, существующий Harvester и Military Arsenal не изменять ради сборки этого независимого модуля.

## Прочитать

README, ROADMAP, WORKLOG и QA в этой директории. Текущий этап — S1 + первый прототип грузовика S2. Не выдавать прототип за завершённый большой мод или за пройденную игровую приёмку.

## Проверить

Из корня: `bash military-vehicles/tests/run-core.sh`, затем `./gradlew -p military-vehicles --no-daemon --console=plain --no-build-cache --rerun-tasks clean build`, затем `python3 military-vehicles/tools/verify_build.py`. На Windows — gradlew.bat и python. Установочный JAR — в `military-vehicles/build/libs/`, не sources.

## Следующий шаг

Сначала игровая приёмка первого грузовика на отдельном тестовом мире: места/камера, сохранение груза/компонентов, подбор в creative, dedicated server и два клиента, столкновения и timeout. После исправлений — более точная коллизия, рабочая подвеска, двери, руки на руле, звук и эффекты. Только затем отдельный багги; боевые системы/танк/артиллерия — следующие самостоятельные этапы.

Нет разрешения на изменение активных миров/модов пользователя, автоматическое принятие EULA или публикацию логов с личными данными. Не делать force push и не перетирать чужие изменения. Результаты проверок читать в WORKLOG и CI, не выводить их из старого сообщения.

## Проверенная сборка этой итерации

`clean build` и `tools/verify_build.py`: PASS, 25 JUnit cases без failures/errors/skips. `coreSmoke`: 90041 assertion. Подробности и SHA-256 — BUILD-REPORT.json. Это не Minecraft runtime или визуальная приёмка.
