# Dealmaker KOB

Forge 1.20.1 integration addon for Dealmaker Core and Knights of Britannia.

## Dealmaker Ritual

Human and Demon KOB players can gain the Dealmaker ability by being on fire while a dropped Book and Quill burns within three blocks. The book is consumed and the player receives the persistent Core Dealmaker mark.

## Soul Authority

Soul holders can use these unrestricted commands:

- `/dealmaker kob soul take_mana <player> <amount>`
- `/dealmaker kob soul take_stamina <player> <amount>`
- `/dealmaker kob soul take_max_mana <player> <amount>`
- `/dealmaker kob soul take_max_stamina <player> <amount>`
- `/dealmaker kob soul drain_mana <player>`
- `/dealmaker kob soul drain_stamina <player>`
- `/dealmaker kob soul drain_max_mana <player>`
- `/dealmaker kob soul drain_max_stamina <player>`
- `/dealmaker kob soul extract_eye <player> <eye>`

Mana/Stamina and their permanent caps use KOB's `kob.mana`, `kob.stamina`, `kob.mana.max`, and `kob.stamina.max` scoreboards. `extract_eye` transfers only a portable KOB eye item in the soul owner's inventory. It deliberately does not strip an installed Palladium eye power, because KOB powers include coupled tags, scoreboards, and global pools.

## Contract Assets

The addon enables typed KOB deal assets using Dealmaker's extension kinds:

- `TRANSFER_RESOURCE_AMOUNT` or `TRANSFER_RESOURCE_PERCENT` with `assetId` `kob:mana`, `kob:stamina`, `kob:mana_max`, or `kob:stamina_max`.
- `DRAIN_RESOURCE_AMOUNT` or `DRAIN_RESOURCE_PERCENT` with the same asset IDs.
- `TRANSFER_SKILL` with `assetId` `kob:eye/<portable-eye-id>`.

Portable eye IDs are `left_sharingan`, `right_sharingan`, `left_rinnegan`, `right_rinnegan`, `byakugan_eye`, `six_eyes`, and `eye_of_balor`. These clauses move a physical KOB eye item; race, subclass, installed Palladium powers, and KOB progression are deliberately rejected.

Use `TRANSFER_SKILL` with `assetId` `kob:eyes` and amount `0` to transfer every supported portable eye item the source owns. This is the AI representation of wording such as “give me your eyes.”
